package com.example.analchess.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt

object ApiService {

    data class OnlineStockfishEval(
        val evalWhitePawns: Float,
        val bestMoveUci: String?
    )

    private val executor = Executors.newFixedThreadPool(6)
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val avatarCache = HashMap<String, Bitmap>()
    private val fenEvalCache = HashMap<String, OnlineStockfishEval>()

    private fun httpGet(urlStr: String, timeoutMs: Int = 7000): Pair<Int, String> {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "AnalChessAndroid/1.0 (Kotlin; ChessAnalysisApp)")
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            Pair(code, body)
        } catch (e: Exception) {
            Pair(0, e.message ?: "Network error")
        } finally {
            conn?.disconnect()
        }
    }

    fun fetchChessComUser(
        rawUsername: String,
        callback: (isSuccess: Boolean, profile: PlayerProfile?, errorMessage: String?) -> Unit
    ) {
        val usernameClean = rawUsername.trim().lowercase(Locale.ROOT)
        if (usernameClean.isEmpty()) {
            callback(false, null, "Masukkan username terlebih dahulu")
            return
        }

        executor.execute {
            try {
                // 1. Fetch Player Profile
                val (profCode, profBody) = httpGet("https://api.chess.com/pub/player/$usernameClean")
                if (profCode !in 200..299 || profBody.isBlank()) {
                    mainHandler.post {
                        callback(false, null, "Username '$rawUsername' tidak ditemukan di Chess.com")
                    }
                    return@execute
                }

                val profJson = JSONObject(profBody)
                val displayUsername = profJson.optString("username", rawUsername.trim())
                val avatarUrl = profJson.optString("avatar", "").takeIf { it.isNotBlank() }

                // 2. Fetch Player Stats
                val (statsCode, statsBody) = httpGet("https://api.chess.com/pub/player/$usernameClean/stats")
                var rapidRating = 0
                var bulletRating = 0
                var blitzRating = 0
                var totalWins = 0
                var totalLosses = 0
                var totalDraws = 0

                if (statsCode in 200..299 && statsBody.isNotBlank()) {
                    val statsJson = JSONObject(statsBody)

                    fun parseMode(key: String): Int {
                        val modeObj = statsJson.optJSONObject(key) ?: return 0
                        val lastObj = modeObj.optJSONObject("last")
                        val rating = lastObj?.optInt("rating", 0) ?: 0
                        val recordObj = modeObj.optJSONObject("record")
                        if (recordObj != null) {
                            totalWins += recordObj.optInt("win", 0)
                            totalLosses += recordObj.optInt("loss", 0)
                            totalDraws += recordObj.optInt("draw", 0)
                        }
                        return rating
                    }

                    rapidRating = parseMode("chess_rapid")
                    bulletRating = parseMode("chess_bullet")
                    blitzRating = parseMode("chess_blitz")
                }

                val totalTrackedGames = totalWins + totalLosses + totalDraws
                val winratePercent = if (totalTrackedGames > 0) {
                    ((totalWins.toDouble() / totalTrackedGames.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
                } else {
                    0
                }

                // 3. Fetch Recent Games Archive
                val gamesList = mutableListOf<GameItem>()
                val (archCode, archBody) = httpGet("https://api.chess.com/pub/player/$usernameClean/games/archives")
                if (archCode in 200..299 && archBody.isNotBlank()) {
                    val archJson = JSONObject(archBody)
                    val archivesArr = archJson.optJSONArray("archives")
                    if (archivesArr != null && archivesArr.length() > 0) {
                        val latestArchiveUrl = archivesArr.optString(archivesArr.length() - 1)
                        if (latestArchiveUrl.isNotBlank()) {
                            val (gamesCode, gamesBody) = httpGet(latestArchiveUrl, timeoutMs = 10000)
                            if (gamesCode in 200..299 && gamesBody.isNotBlank()) {
                                val gamesRoot = JSONObject(gamesBody)
                                val gamesArr = gamesRoot.optJSONArray("games")
                                if (gamesArr != null) {
                                    val count = gamesArr.length()
                                    val startIdx = (count - 20).coerceAtLeast(0)
                                    for (i in (count - 1) downTo startIdx) {
                                        val gObj = gamesArr.optJSONObject(i) ?: continue
                                        val timeClass = gObj.optString("time_class", "rapid")
                                        val pgn = gObj.optString("pgn", "")
                                        if (pgn.isBlank()) continue

                                        val whiteObj = gObj.optJSONObject("white")
                                        val blackObj = gObj.optJSONObject("black")
                                        if (whiteObj == null || blackObj == null) continue

                                        val whiteUser = whiteObj.optString("username", "White")
                                        val whiteRating = whiteObj.optInt("rating", 800)
                                        val whiteResult = whiteObj.optString("result", "")

                                        val blackUser = blackObj.optString("username", "Black")
                                        val blackRating = blackObj.optInt("rating", 800)
                                        val blackResult = blackObj.optString("result", "")

                                        val isUserWhite = whiteUser.equals(usernameClean, ignoreCase = true)
                                        val opponentName = if (isUserWhite) blackUser else whiteUser
                                        val userResult = if (isUserWhite) whiteResult else blackResult
                                        val isWin = userResult == "win"

                                        val accObj = gObj.optJSONObject("accuracies")
                                        val offWhiteAcc = accObj?.optDouble("white", -1.0)?.takeIf { it > 0.0 }
                                        val offBlackAcc = accObj?.optDouble("black", -1.0)?.takeIf { it > 0.0 }
                                        val userOffAcc = if (isUserWhite) offWhiteAcc else offBlackAcc

                                        val finalAcc = userOffAcc ?: estimateInitialAccuracy(isWin, whiteRating, blackRating, i)
                                        val accFormatted = String.format(Locale.GERMAN, "%.1f", finalAcc)

                                        gamesList.add(
                                            GameItem(
                                                id = gObj.optString("url", "game_$i"),
                                                timeClass = timeClass,
                                                opponentName = opponentName,
                                                accuracyFormatted = accFormatted,
                                                isPositive = isWin,
                                                whiteUsername = whiteUser,
                                                whiteRating = whiteRating,
                                                whiteAvatarUrl = if (isUserWhite) avatarUrl else null,
                                                blackUsername = blackUser,
                                                blackRating = blackRating,
                                                blackAvatarUrl = if (!isUserWhite) avatarUrl else null,
                                                pgn = pgn,
                                                isUserWhite = isUserWhite,
                                                officialWhiteAccuracy = offWhiteAcc,
                                                officialBlackAccuracy = offBlackAcc,
                                                isDefaultSample = false
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                val profile = PlayerProfile(
                    username = displayUsername,
                    avatarUrl = avatarUrl,
                    winratePercent = winratePercent,
                    rapidRating = rapidRating,
                    bulletRating = bulletRating,
                    blitzRating = blitzRating,
                    totalGamesCount = gamesList.size,
                    games = gamesList
                )

                mainHandler.post {
                    callback(true, profile, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post {
                    callback(false, null, "Gagal memuat data: ${e.message}")
                }
            }
        }
    }

    private fun estimateInitialAccuracy(isWin: Boolean, wRating: Int, bRating: Int, seed: Int): Double {
        val base = if (isWin) 78.4 else 52.3
        val offset = ((wRating + bRating + seed * 17) % 160) / 10.0
        return (base + offset).coerceIn(35.0, 98.5)
    }

    /**
     * Complete pre-analysis of the entire game before showing the Analysis Screen.
     * Uses Stockfish Online API (Lichess Cloud + Stockfish 16 depth 11) + local Alpha-Beta/Quiescence engine.
     */
    fun analyzeGameFullBeforeDisplay(
        game: GameItem,
        states: List<BoardState>,
        onProgress: (percent: Int, currentStep: Int, totalSteps: Int) -> Unit,
        onComplete: (summary: GameAnalysisSummary, whiteAvatarUrl: String?, blackAvatarUrl: String?) -> Unit
    ) {
        executor.execute {
            val totalMoves = (states.size - 1).coerceAtLeast(1)
            val completedCount = AtomicInteger(0)

            var resolvedWhiteAvatar = game.whiteAvatarUrl
            var resolvedBlackAvatar = game.blackAvatarUrl

            if (resolvedWhiteAvatar == null && game.whiteUsername.isNotBlank()) {
                resolvedWhiteAvatar = fetchPlayerAvatarSync(game.whiteUsername)
            }
            if (resolvedBlackAvatar == null && game.blackUsername.isNotBlank()) {
                resolvedBlackAvatar = fetchPlayerAvatarSync(game.blackUsername)
            }

            val tacticalResults = arrayOfNulls<ChessEngine.TacticalMoveAnalysis>(states.size)
            val onlineResults = arrayOfNulls<OnlineStockfishEval>(states.size)
            onlineResults[0] = queryStockfishOnlineSync(states[0].fen, timeoutMs = 2000)
                ?: OnlineStockfishEval(0.20f, "e2e4")

            val tasks = (1 until states.size).map { idx ->
                Callable {
                    val prevState = states[idx - 1]
                    val currState = states[idx]
                    val isWhiteMove = (idx % 2 == 1)

                    val tactical = ChessEngine.analyzeMoveTactical(prevState, currState, isWhiteMove)
                    tacticalResults[idx] = tactical

                    val online = queryStockfishOnlineSync(currState.fen, timeoutMs = 2600)
                    onlineResults[idx] = online

                    currState.evalWhitePawns = online?.evalWhitePawns ?: tactical.playedEvalWhite

                    val done = completedCount.incrementAndGet()
                    val pct = ((done.toFloat() / totalMoves.toFloat()) * 100f).roundToInt().coerceIn(5, 99)
                    mainHandler.post {
                        onProgress(pct, done, totalMoves)
                    }
                }
            }

            try {
                executor.invokeAll(tasks)
            } catch (_: Exception) {
            }

            val summary = buildSummaryFromResults(game, states, tacticalResults, onlineResults)

            mainHandler.post {
                onProgress(100, totalMoves, totalMoves)
                onComplete(summary, resolvedWhiteAvatar, resolvedBlackAvatar)
            }
        }
    }

    fun buildSummaryFromResults(
        game: GameItem,
        states: List<BoardState>,
        tacticalResults: Array<ChessEngine.TacticalMoveAnalysis?>,
        onlineResults: Array<OnlineStockfishEval?>
    ): GameAnalysisSummary {
        val summary = GameAnalysisSummary(
            player1Name = game.blackUsername,
            player2Name = game.whiteUsername,
            accuracy1 = 80,
            accuracy2 = 80,
            perfRating1 = game.blackRating.coerceAtLeast(150),
            perfRating2 = game.whiteRating.coerceAtLeast(150)
        )

        var p1MoveAccuracySum = 0.0
        var p1MovesCount = 0
        var p2MoveAccuracySum = 0.0
        var p2MovesCount = 0

        for (idx in 1 until states.size) {
            val prevState = states[idx - 1]
            val state = states[idx]
            val isWhiteMove = (idx % 2 == 1)
            val tactical = tacticalResults[idx] ?: ChessEngine.analyzeMoveTactical(prevState, state, isWhiteMove)

            val prevOnline = onlineResults[idx - 1]
            val currOnline = onlineResults[idx]

            val playedUci = if (state.fromRow in 0..7 && state.fromCol in 0..7 && state.toRow in 0..7 && state.toCol in 0..7) {
                "${'a' + state.fromCol}${'8' - state.fromRow}${'a' + state.toCol}${'8' - state.toRow}"
            } else ""

            val isCheckmateDelivered = state.sanMove.contains("#")
            val matchedOnlineBest = isCheckmateDelivered || (
                prevOnline?.bestMoveUci != null &&
                    playedUci.isNotEmpty() &&
                    prevOnline.bestMoveUci.startsWith(playedUci)
                )

            val tacticalBestMover = if (isWhiteMove) tactical.bestEvalWhite else -tactical.bestEvalWhite
            val tacticalSecondMover = if (isWhiteMove) tactical.secondBestEvalWhite else -tactical.secondBestEvalWhite
            val tacticalPlayedMover = if (isWhiteMove) tactical.playedEvalWhite else -tactical.playedEvalWhite
            val tacticalCpLoss = (tacticalBestMover - tacticalPlayedMover).coerceAtLeast(0f)

            val hasBothOnline = (prevOnline != null && currOnline != null)
            val onlineBestMover = if (prevOnline != null) {
                if (isWhiteMove) prevOnline.evalWhitePawns else -prevOnline.evalWhitePawns
            } else {
                tacticalBestMover
            }
            val onlinePlayedMover = if (currOnline != null) {
                if (isWhiteMove) currOnline.evalWhitePawns else -currOnline.evalWhitePawns
            } else {
                tacticalPlayedMover
            }

            val onlineCpLoss = if (hasBothOnline) {
                (onlineBestMover - onlinePlayedMover).coerceAtLeast(0f)
            } else {
                null
            }

            val effectiveCpLoss = when {
                isCheckmateDelivered -> 0.0f
                matchedOnlineBest -> 0.0f
                onlineCpLoss != null -> {
                    // Trust Stockfish Online directly, with tactical safeguard if a piece was hung
                    if (tacticalCpLoss >= 2.20f && onlineCpLoss < 1.0f) {
                        max(onlineCpLoss, tacticalCpLoss * 0.8f)
                    } else {
                        onlineCpLoss
                    }
                }
                else -> tacticalCpLoss
            }

            val effectiveBestMover = if (hasBothOnline) onlineBestMover else tacticalBestMover
            val effectivePlayedMover = if (hasBothOnline) onlinePlayedMover else tacticalPlayedMover
            val isBestMove = matchedOnlineBest || (tactical.isBestMove && effectiveCpLoss <= 0.12f) || effectiveCpLoss <= 0.08f

            // Win-percentage loss from the mover's perspective (already clamped smoothly in -10..+10 pawns)
            val bestWinPct = evalToWinPercentage(effectiveBestMover)
            val playedWinPct = evalToWinPercentage(effectiveBestMover - effectiveCpLoss)
            val winPctLoss = (bestWinPct - playedWinPct).coerceAtLeast(0.0)

            val moveAcc = (103.1668 * exp(-0.04354 * winPctLoss) - 3.1669).coerceIn(10.0, 100.0)
            val bestAdvantageOverSecond = (tacticalBestMover - tacticalSecondMover).coerceAtLeast(0f)

            // Classify using Win-Probability Loss + Centipawn Loss (just like Chess.com Game Review!)
            // Why Win-Probability Loss matters: when a player is already winning by +6.0 and plays a +4.5 move,
            // centipawn loss is 1.5, but winPctLoss is only ~2% (still 98% winning!), which on Chess.com is Sangat Baik / Bagus, NOT a Blunder!
            val quality = when {
                tactical.isBookMove && effectiveCpLoss <= 0.18f -> MoveQuality.BOOK

                idx > 10 &&
                    !isCheckmateDelivered &&
                    isBestMove &&
                    effectiveCpLoss <= 0.05f &&
                    tactical.isTrueSacrifice &&
                    !tactical.isRoutineRecaptureOrFreeTake &&
                    effectivePlayedMover >= 1.25f &&
                    effectiveBestMover <= 4.20f &&
                    bestAdvantageOverSecond >= 0.75f -> MoveQuality.BRILIAN

                idx > 6 &&
                    !isCheckmateDelivered &&
                    isBestMove &&
                    effectiveCpLoss <= 0.08f &&
                    !tactical.isBookMove &&
                    !tactical.isRoutineRecaptureOrFreeTake &&
                    bestAdvantageOverSecond >= 1.50f &&
                    effectivePlayedMover >= -0.50f &&
                    effectiveBestMover <= 4.80f -> MoveQuality.BAGUS

                isBestMove || winPctLoss <= 2.2 || effectiveCpLoss <= 0.12f -> MoveQuality.TERBAIK

                winPctLoss <= 5.5 || effectiveCpLoss <= 0.35f -> MoveQuality.SANGAT_BAIK

                winPctLoss <= 10.5 || effectiveCpLoss <= 0.70f -> MoveQuality.BAGUS

                // Missed a clear winning tactical opportunity while remaining in a playable position
                winPctLoss > 14.0 &&
                    effectiveBestMover >= 1.80f &&
                    bestAdvantageOverSecond >= 1.15f &&
                    effectivePlayedMover >= -0.80f -> MoveQuality.TERLEWAT

                winPctLoss <= 20.0 && effectiveCpLoss <= 1.60f -> MoveQuality.KESALAHAN

                else -> MoveQuality.BLUNDER
            }

            state.moveQuality = quality

            if (isWhiteMove) {
                p2MovesCount++
                p2MoveAccuracySum += moveAcc
                when (quality) {
                    MoveQuality.BRILIAN -> summary.brilian2++
                    MoveQuality.TERBAIK -> summary.terbaik2++
                    MoveQuality.SANGAT_BAIK -> summary.sangatBaik2++
                    MoveQuality.BAGUS -> summary.bagus2++
                    MoveQuality.BOOK -> summary.buku2++
                    MoveQuality.KESALAHAN -> summary.kesalahan2++
                    MoveQuality.TERLEWAT -> summary.terlewat2++
                    MoveQuality.BLUNDER -> summary.blunder2++
                    MoveQuality.NONE -> {}
                }
            } else {
                p1MovesCount++
                p1MoveAccuracySum += moveAcc
                when (quality) {
                    MoveQuality.BRILIAN -> summary.brilian1++
                    MoveQuality.TERBAIK -> summary.terbaik1++
                    MoveQuality.SANGAT_BAIK -> summary.sangatBaik1++
                    MoveQuality.BAGUS -> summary.bagus1++
                    MoveQuality.BOOK -> summary.buku1++
                    MoveQuality.KESALAHAN -> summary.kesalahan1++
                    MoveQuality.TERLEWAT -> summary.terlewat1++
                    MoveQuality.BLUNDER -> summary.blunder1++
                    MoveQuality.NONE -> {}
                }
            }
        }

        if (p1MovesCount > 0) {
            val computedBlackAcc = (p1MoveAccuracySum / p1MovesCount).roundToInt().coerceIn(15, 99)
            summary.accuracy1 = game.officialBlackAccuracy?.roundToInt()?.coerceIn(1, 100) ?: computedBlackAcc
            summary.perfRating1 = (game.blackRating + (summary.accuracy1 - 74) * 8).coerceIn(100, 3000)
        }
        if (p2MovesCount > 0) {
            val computedWhiteAcc = (p2MoveAccuracySum / p2MovesCount).roundToInt().coerceIn(15, 99)
            summary.accuracy2 = game.officialWhiteAccuracy?.roundToInt()?.coerceIn(1, 100) ?: computedWhiteAcc
            summary.perfRating2 = (game.whiteRating + (summary.accuracy2 - 74) * 8).coerceIn(100, 3000)
        }

        // Update the game item's formatted accuracy if it wasn't pre-populated by Chess.com
        if (game.officialWhiteAccuracy == null && game.officialBlackAccuracy == null) {
            val userAcc = if (game.isUserWhite) summary.accuracy2.toDouble() else summary.accuracy1.toDouble()
            game.accuracyFormatted = String.format(Locale.GERMAN, "%.1f", userAcc)
        }

        return summary
    }

    private fun fetchPlayerAvatarSync(username: String): String? {
        return try {
            val clean = username.trim().lowercase(Locale.ROOT)
            val (code, body) = httpGet("https://api.chess.com/pub/player/$clean", timeoutMs = 3000)
            if (code in 200..299 && body.isNotBlank()) {
                JSONObject(body).optString("avatar", "").takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun evalToWinPercentage(evalPawns: Float): Double {
        val centipawns = evalPawns.coerceIn(-12f, 12f) * 100.0
        return 50.0 + 50.0 * (2.0 / (1.0 + exp(-0.00368208 * centipawns)) - 1.0)
    }

    private fun normalizeUciMove(rawUci: String?): String? {
        if (rawUci.isNullOrBlank() || rawUci.length < 4) return null
        return when (rawUci.trim()) {
            "e1h1" -> "e1g1"
            "e1a1" -> "e1c1"
            "e8h8" -> "e8g8"
            "e8a8" -> "e8c8"
            else -> rawUci.trim()
        }
    }

    private fun mateToWhiteEval(mate: Int, fen: String): Float {
        return when {
            mate > 0 -> 12.0f
            mate < 0 -> -12.0f
            else -> {
                // mate == 0 means the side to move in FEN is currently checkmated
                val isWhiteToMove = fen.contains(" w ")
                if (isWhiteToMove) -12.0f else 12.0f
            }
        }
    }

    fun queryStockfishOnlineSync(fen: String, timeoutMs: Int = 2600): OnlineStockfishEval? {
        synchronized(fenEvalCache) {
            fenEvalCache[fen]?.let { return it }
        }
        try {
            val encodedFen = URLEncoder.encode(fen, "UTF-8")

            // 1. Try Lichess Cloud Eval first for fast cloud opening/middlegame evaluations
            val lichessUrl = "https://lichess.org/api/cloud-eval?fen=$encodedFen&multiPv=1"
            val (lCode, lBody) = httpGet(lichessUrl, timeoutMs = 1200)
            if (lCode in 200..299 && lBody.isNotBlank()) {
                val lJson = JSONObject(lBody)
                val pvs = lJson.optJSONArray("pvs")
                if (pvs != null && pvs.length() > 0) {
                    val firstPv = pvs.optJSONObject(0)
                    if (firstPv != null) {
                        val movesStr = firstPv.optString("moves", "")
                        val bestUci = normalizeUciMove(movesStr.split(" ").firstOrNull())
                        if (firstPv.has("cp")) {
                            val eval = (firstPv.optInt("cp", 0) / 100f).coerceIn(-12.0f, 12.0f)
                            val res = OnlineStockfishEval(eval, bestUci)
                            synchronized(fenEvalCache) { fenEvalCache[fen] = res }
                            return res
                        } else if (firstPv.has("mate")) {
                            val mate = firstPv.optInt("mate", 0)
                            val eval = mateToWhiteEval(mate, fen)
                            val res = OnlineStockfishEval(eval, bestUci)
                            synchronized(fenEvalCache) { fenEvalCache[fen] = res }
                            return res
                        }
                    }
                }
            }

            // 2. Query Stockfish Online API (depth 11) with 1 automatic retry if timeout occurs
            val url = "https://stockfish.online/api/s/v2.php?fen=$encodedFen&depth=11"
            for (attempt in 1..2) {
                val (code, body) = httpGet(url, timeoutMs = timeoutMs)
                if (code in 200..299 && body.isNotBlank()) {
                    val json = JSONObject(body)
                    if (json.optBoolean("success", false)) {
                        val rawBest = json.optString("bestmove", "")
                        val parts = rawBest.split(" ")
                        val bestUci = if (parts.size >= 2 && parts[0] == "bestmove" && parts[1] != "(none)") {
                            normalizeUciMove(parts[1])
                        } else null

                        if (!json.isNull("mate")) {
                            val mateStr = json.optString("mate", "0")
                            val mate = mateStr.toIntOrNull() ?: json.optInt("mate", 0)
                            val eval = mateToWhiteEval(mate, fen)
                            val res = OnlineStockfishEval(eval, bestUci)
                            synchronized(fenEvalCache) { fenEvalCache[fen] = res }
                            return res
                        }
                        if (!json.isNull("evaluation")) {
                            val eval = json.optDouble("evaluation", 0.0).toFloat().coerceIn(-12.0f, 12.0f)
                            val res = OnlineStockfishEval(eval, bestUci)
                            synchronized(fenEvalCache) { fenEvalCache[fen] = res }
                            return res
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    fun loadImage(urlStr: String?, imageView: ImageView, defaultResId: Int) {
        if (urlStr.isNullOrBlank()) {
            imageView.setImageResource(defaultResId)
            return
        }

        val cached = synchronized(avatarCache) { avatarCache[urlStr] }
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        executor.execute {
            var conn: HttpURLConnection? = null
            var bmp: Bitmap? = null
            try {
                val url = URL(urlStr)
                conn = (url.openConnection() as HttpURLConnection).apply {
                    doInput = true
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "AnalChessAndroid/1.0")
                }
                conn.connect()
                if (conn.responseCode in 200..299) {
                    bmp = BitmapFactory.decodeStream(conn.inputStream)
                }
            } catch (_: Exception) {
            } finally {
                conn?.disconnect()
            }

            if (bmp != null) {
                synchronized(avatarCache) {
                    avatarCache[urlStr] = bmp
                }
            }

            mainHandler.post {
                if (bmp != null) {
                    imageView.setImageBitmap(bmp)
                } else {
                    imageView.setImageResource(defaultResId)
                }
            }
        }
    }
}
