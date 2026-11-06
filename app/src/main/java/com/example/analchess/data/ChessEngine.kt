package com.example.analchess.data

import kotlin.math.abs
import kotlin.math.min

object ChessEngine {

    data class EngineMove(
        val fromR: Int,
        val fromC: Int,
        val toR: Int,
        val toC: Int,
        val piece: Char,
        val captured: Char = '.',
        val promotion: Char = '.',
        var score: Int = 0
    ) {
        fun toUci(): String {
            val fCol = ('a' + fromC)
            val fRank = ('8' - fromR)
            val tCol = ('a' + toC)
            val tRank = ('8' - toR)
            val promoStr = if (promotion != '.') promotion.lowercaseChar().toString() else ""
            return "$fCol$fRank$tCol$tRank$promoStr"
        }
    }

    data class TacticalMoveAnalysis(
        val bestEvalWhite: Float,
        val secondBestEvalWhite: Float,
        val playedEvalWhite: Float,
        val bestMoveUci: String,
        val isBestMove: Boolean,
        val isBookMove: Boolean,
        val isTrueSacrifice: Boolean,
        val isRoutineRecaptureOrFreeTake: Boolean
    )

    private val pawnTable = arrayOf(
        floatArrayOf(0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f),
        floatArrayOf(0.55f, 0.60f, 0.60f, 0.65f, 0.65f, 0.60f, 0.60f, 0.55f),
        floatArrayOf(0.15f, 0.15f, 0.25f, 0.35f, 0.35f, 0.25f, 0.15f, 0.15f),
        floatArrayOf(0.05f, 0.05f, 0.12f, 0.28f, 0.28f, 0.12f, 0.05f, 0.05f),
        floatArrayOf(0.0f,  0.0f,  0.02f, 0.24f, 0.24f, 0.02f, 0.0f,  0.0f),
        floatArrayOf(0.05f, -0.05f,-0.08f, 0.0f,  0.0f, -0.08f,-0.05f, 0.05f),
        floatArrayOf(0.05f, 0.10f, 0.10f, -0.20f,-0.20f, 0.10f, 0.10f, 0.05f),
        floatArrayOf(0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f,  0.0f)
    )

    private val knightTable = arrayOf(
        floatArrayOf(-0.50f, -0.40f, -0.30f, -0.30f, -0.30f, -0.30f, -0.40f, -0.50f),
        floatArrayOf(-0.40f, -0.20f,  0.00f,  0.00f,  0.00f,  0.00f, -0.20f, -0.40f),
        floatArrayOf(-0.30f,  0.00f,  0.12f,  0.18f,  0.18f,  0.12f,  0.00f, -0.30f),
        floatArrayOf(-0.30f,  0.05f,  0.18f,  0.22f,  0.22f,  0.18f,  0.05f, -0.30f),
        floatArrayOf(-0.30f,  0.00f,  0.18f,  0.22f,  0.22f,  0.18f,  0.00f, -0.30f),
        floatArrayOf(-0.30f,  0.06f,  0.12f,  0.15f,  0.15f,  0.12f,  0.06f, -0.30f),
        floatArrayOf(-0.40f, -0.20f,  0.00f,  0.05f,  0.05f,  0.00f, -0.20f, -0.40f),
        floatArrayOf(-0.50f, -0.40f, -0.30f, -0.30f, -0.30f, -0.30f, -0.40f, -0.50f)
    )

    private val bishopTable = arrayOf(
        floatArrayOf(-0.20f, -0.10f, -0.10f, -0.10f, -0.10f, -0.10f, -0.10f, -0.20f),
        floatArrayOf(-0.10f,  0.00f,  0.00f,  0.00f,  0.00f,  0.00f,  0.00f, -0.10f),
        floatArrayOf(-0.10f,  0.00f,  0.06f,  0.10f,  0.10f,  0.06f,  0.00f, -0.10f),
        floatArrayOf(-0.10f,  0.06f,  0.06f,  0.12f,  0.12f,  0.06f,  0.06f, -0.10f),
        floatArrayOf(-0.10f,  0.00f,  0.12f,  0.12f,  0.12f,  0.12f,  0.00f, -0.10f),
        floatArrayOf(-0.10f,  0.10f,  0.10f,  0.10f,  0.10f,  0.10f,  0.10f, -0.10f),
        floatArrayOf(-0.10f,  0.08f,  0.00f,  0.00f,  0.00f,  0.00f,  0.08f, -0.10f),
        floatArrayOf(-0.20f, -0.10f, -0.10f, -0.10f, -0.10f, -0.10f, -0.10f, -0.20f)
    )

    private val commonBookMoves = setOf(
        "e4", "e5", "d4", "d5", "c4", "c5", "c6", "e6", "d6", "g6", "b6",
        "Nf3", "Nc3", "Nf6", "Nc6", "Bc4", "Bc5", "Bb5", "Bf4", "Bg5", "Be2", "Be7", "Bg7", "Bb7", "Bd3", "Bd6",
        "O-O", "0-0", "exd5", "cxd4", "Nxd4", "Nxd5", "a6", "h6", "a3", "h3", "c3", "Re1", "Qe2"
    )

    fun createInitialBoard(): Array<CharArray> {
        return arrayOf(
            charArrayOf('r', 'n', 'b', 'q', 'k', 'b', 'n', 'r'),
            charArrayOf('p', 'p', 'p', 'p', 'p', 'p', 'p', 'p'),
            charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
            charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
            charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
            charArrayOf('.', '.', '.', '.', '.', '.', '.', '.'),
            charArrayOf('P', 'P', 'P', 'P', 'P', 'P', 'P', 'P'),
            charArrayOf('R', 'N', 'B', 'Q', 'K', 'B', 'N', 'R')
        )
    }

    fun copyBoard(src: Array<CharArray>): Array<CharArray> {
        return Array(8) { r -> src[r].clone() }
    }

    fun pieceBaseValue(ch: Char): Float {
        return when (ch.uppercaseChar()) {
            'P' -> 1.00f
            'N' -> 3.05f
            'B' -> 3.25f
            'R' -> 5.00f
            'Q' -> 9.00f
            'K' -> 40.0f
            else -> 0.0f
        }
    }

    private fun pieceOrderValue(ch: Char): Int {
        return when (ch.uppercaseChar()) {
            'P' -> 100
            'N' -> 305
            'B' -> 325
            'R' -> 500
            'Q' -> 900
            'K' -> 10000
            else -> 0
        }
    }

    fun parsePgnToBoardStates(pgn: String): List<BoardState> {
        val states = mutableListOf<BoardState>()
        var currentBoard = createInitialBoard()
        var wK = true
        var wQ = true
        var bK = true
        var bQ = true
        var halfMoveClock = 0

        states.add(
            BoardState(
                board = copyBoard(currentBoard),
                moveIndex = 0,
                sanMove = "",
                fen = boardToFen(
                    board = currentBoard,
                    isWhiteTurn = true,
                    fullMoveNumber = 1,
                    castlingRights = "KQkq",
                    enPassantSquare = "-",
                    halfMoveClock = 0
                ),
                evalWhitePawns = 0.0f,
                topCapturedText = "-",
                bottomCapturedText = "-"
            )
        )

        val moves = extractSanMovesFromPgn(pgn)
        var isWhite = true
        var moveIndex = 0

        for (san in moves) {
            moveIndex++
            val prevBoard = currentBoard
            val nextBoard = copyBoard(currentBoard)
            val coords = applySanMove(nextBoard, san, isWhite)
            val fromR = coords[0]
            val fromC = coords[1]
            val toR = coords[2]
            val toC = coords[3]

            val movedPiece = if (fromR in 0..7 && fromC in 0..7) prevBoard[fromR][fromC] else '.'
            val wasCapture = san.contains("x") || (toR in 0..7 && toC in 0..7 && prevBoard[toR][toC] != '.')

            // Update castling rights accurately
            if (fromR == 7 && fromC == 4) { wK = false; wQ = false }
            if (fromR == 0 && fromC == 4) { bK = false; bQ = false }
            if ((fromR == 7 && fromC == 7) || (toR == 7 && toC == 7)) wK = false
            if ((fromR == 7 && fromC == 0) || (toR == 7 && toC == 0)) wQ = false
            if ((fromR == 0 && fromC == 7) || (toR == 0 && toC == 7)) bK = false
            if ((fromR == 0 && fromC == 0) || (toR == 0 && toC == 0)) bQ = false

            val castlingStr = buildString {
                if (wK) append('K')
                if (wQ) append('Q')
                if (bK) append('k')
                if (bQ) append('q')
            }.ifEmpty { "-" }

            // En-passant square if double pawn push
            val epSquare = if (movedPiece.uppercaseChar() == 'P' && abs(toR - fromR) == 2 && toC in 0..7) {
                val epRow = (fromR + toR) / 2
                "${'a' + toC}${'8' - epRow}"
            } else {
                "-"
            }

            if (movedPiece.uppercaseChar() == 'P' || wasCapture) {
                halfMoveClock = 0
            } else {
                halfMoveClock++
            }

            currentBoard = nextBoard
            isWhite = !isWhite
            val fullMove = (moveIndex / 2) + 1
            val fen = boardToFen(
                board = currentBoard,
                isWhiteTurn = isWhite,
                fullMoveNumber = fullMove,
                castlingRights = castlingStr,
                enPassantSquare = epSquare,
                halfMoveClock = halfMoveClock
            )
            val (topCap, botCap) = computeCapturedStrings(currentBoard)
            val staticEval = evaluateBoardPositionalPawns(currentBoard)

            states.add(
                BoardState(
                    board = copyBoard(currentBoard),
                    moveIndex = moveIndex,
                    sanMove = san,
                    fen = fen,
                    fromRow = fromR,
                    fromCol = fromC,
                    toRow = toR,
                    toCol = toC,
                    evalWhitePawns = staticEval,
                    topCapturedText = topCap,
                    bottomCapturedText = botCap
                )
            )
        }

        if (states.size > 1) {
            val lastState = states.last()
            states[0] = states[0].copy(
                topCapturedText = lastState.topCapturedText,
                bottomCapturedText = lastState.bottomCapturedText
            )
        }

        return states
    }

    private fun extractSanMovesFromPgn(pgn: String): List<String> {
        if (pgn.isBlank()) return emptyList()
        val withoutHeaders = pgn.lines()
            .filter { !it.trim().startsWith("[") }
            .joinToString(" ")

        val noCurly = withoutHeaders.replace(Regex("\\{[^}]*\\}"), " ")
        val noParen = noCurly.replace(Regex("\\([^)]*\\)"), " ")

        val tokens = noParen.split(Regex("\\s+")).map { it.trim() }.filter { it.isNotEmpty() }
        val sanMoves = mutableListOf<String>()

        for (token in tokens) {
            if (token in listOf("1-0", "0-1", "1/2-1/2", "*")) continue
            if (token.matches(Regex("^\\d+\\.+$"))) continue
            val cleaned = token.replace(Regex("^\\d+\\.+"), "").trim()
            if (cleaned.isEmpty() || cleaned in listOf("1-0", "0-1", "1/2-1/2", "*")) continue
            sanMoves.add(cleaned)
        }
        return sanMoves
    }

    private fun applySanMove(board: Array<CharArray>, rawSan: String, isWhite: Boolean): IntArray {
        val san = rawSan.replace("+", "").replace("#", "").replace("!", "").replace("?", "")

        if (san == "O-O" || san == "0-0") {
            val row = if (isWhite) 7 else 0
            board[row][4] = '.'
            board[row][7] = '.'
            board[row][6] = if (isWhite) 'K' else 'k'
            board[row][5] = if (isWhite) 'R' else 'r'
            return intArrayOf(row, 4, row, 6)
        }
        if (san == "O-O-O" || san == "0-0-0") {
            val row = if (isWhite) 7 else 0
            board[row][4] = '.'
            board[row][0] = '.'
            board[row][2] = if (isWhite) 'K' else 'k'
            board[row][3] = if (isWhite) 'R' else 'r'
            return intArrayOf(row, 4, row, 2)
        }

        var promotionPiece: Char? = null
        var workSan = san
        if (workSan.contains("=")) {
            val parts = workSan.split("=")
            workSan = parts[0]
            val pChar = parts.getOrNull(1)?.firstOrNull() ?: 'Q'
            promotionPiece = if (isWhite) pChar.uppercaseChar() else pChar.lowercaseChar()
        }

        if (workSan.length < 2) return intArrayOf(-1, -1, -1, -1)

        val destFileChar = workSan[workSan.length - 2]
        val destRankChar = workSan[workSan.length - 1]
        val toCol = destFileChar - 'a'
        val toRow = 8 - (destRankChar - '0')
        if (toRow !in 0..7 || toCol !in 0..7) return intArrayOf(-1, -1, -1, -1)

        val firstChar = workSan[0]
        val isPieceMove = firstChar in listOf('K', 'Q', 'R', 'B', 'N')
        val pieceTypeUpper = if (isPieceMove) firstChar else 'P'
        val targetPieceChar = if (isWhite) pieceTypeUpper else pieceTypeUpper.lowercaseChar()

        val prefix = workSan.substring(0, workSan.length - 2).replace("x", "")
        var disambigCol: Int? = null
        var disambigRow: Int? = null

        val disambigStr = if (isPieceMove && prefix.isNotEmpty()) prefix.substring(1) else if (!isPieceMove) prefix else ""
        for (ch in disambigStr) {
            if (ch in 'a'..'h') disambigCol = ch - 'a'
            if (ch in '1'..'8') disambigRow = 8 - (ch - '0')
        }

        var foundFromRow = -1
        var foundFromCol = -1
        var fallbackFromRow = -1
        var fallbackFromCol = -1

        for (r in 0..7) {
            for (c in 0..7) {
                if (board[r][c] != targetPieceChar) continue
                if (disambigCol != null && c != disambigCol) continue
                if (disambigRow != null && r != disambigRow) continue
                if (canPieceReach(board, r, c, toRow, toCol, pieceTypeUpper, isWhite, workSan.contains("x"))) {
                    if (fallbackFromRow == -1) {
                        fallbackFromRow = r
                        fallbackFromCol = c
                    }
                    // Verify move does not leave own king in check (handles pinned pieces)
                    val testBoard = copyBoard(board)
                    if (pieceTypeUpper == 'P' && c != toCol && testBoard[toRow][toCol] == '.') {
                        testBoard[r][toCol] = '.'
                    }
                    testBoard[toRow][toCol] = promotionPiece ?: targetPieceChar
                    testBoard[r][c] = '.'
                    if (!isKingInCheck(testBoard, isWhite)) {
                        foundFromRow = r
                        foundFromCol = c
                        break
                    }
                }
            }
            if (foundFromRow != -1) break
        }

        if (foundFromRow == -1) {
            foundFromRow = fallbackFromRow
            foundFromCol = fallbackFromCol
        }

        if (foundFromRow != -1 && foundFromCol != -1) {
            if (pieceTypeUpper == 'P' && foundFromCol != toCol && board[toRow][toCol] == '.') {
                board[foundFromRow][toCol] = '.'
            }
            board[toRow][toCol] = promotionPiece ?: targetPieceChar
            board[foundFromRow][foundFromCol] = '.'
        }

        return intArrayOf(foundFromRow, foundFromCol, toRow, toCol)
    }

    private fun canPieceReach(
        board: Array<CharArray>,
        r1: Int,
        c1: Int,
        r2: Int,
        c2: Int,
        pieceUpper: Char,
        isWhite: Boolean,
        isCapture: Boolean
    ): Boolean {
        if (r1 == r2 && c1 == c2) return false
        val dr = r2 - r1
        val dc = c2 - c1
        when (pieceUpper) {
            'P' -> {
                val dir = if (isWhite) -1 else 1
                val startRow = if (isWhite) 6 else 1
                return if (!isCapture) {
                    if (dc != 0) false
                    else if (dr == dir && board[r2][c2] == '.') true
                    else r1 == startRow && dr == 2 * dir && board[r1 + dir][c1] == '.' && board[r2][c2] == '.'
                } else {
                    abs(dc) == 1 && dr == dir
                }
            }
            'N' -> return (abs(dr) == 2 && abs(dc) == 1) || (abs(dr) == 1 && abs(dc) == 2)
            'K' -> return abs(dr) <= 1 && abs(dc) <= 1
            'B' -> {
                if (abs(dr) != abs(dc)) return false
                return isPathClear(board, r1, c1, r2, c2)
            }
            'R' -> {
                if (dr != 0 && dc != 0) return false
                return isPathClear(board, r1, c1, r2, c2)
            }
            'Q' -> {
                if (dr != 0 && dc != 0 && abs(dr) != abs(dc)) return false
                return isPathClear(board, r1, c1, r2, c2)
            }
        }
        return false
    }

    private fun isPathClear(board: Array<CharArray>, r1: Int, c1: Int, r2: Int, c2: Int): Boolean {
        val stepR = (r2 - r1).coerceIn(-1, 1)
        val stepC = (c2 - c1).coerceIn(-1, 1)
        var r = r1 + stepR
        var c = c1 + stepC
        while (r != r2 || c != c2) {
            if (board[r][c] != '.') return false
            r += stepR
            c += stepC
        }
        return true
    }

    fun isSquareAttackedBy(board: Array<CharArray>, targetR: Int, targetC: Int, byWhite: Boolean): Boolean {
        val pawnSourceRow = if (byWhite) targetR + 1 else targetR - 1
        val attackerPawn = if (byWhite) 'P' else 'p'
        if (pawnSourceRow in 0..7) {
            if (targetC - 1 >= 0 && board[pawnSourceRow][targetC - 1] == attackerPawn) return true
            if (targetC + 1 <= 7 && board[pawnSourceRow][targetC + 1] == attackerPawn) return true
        }

        val attackerKnight = if (byWhite) 'N' else 'n'
        val knightMoves = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1),
            intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2),
            intArrayOf(2, -1), intArrayOf(2, 1)
        )
        for (d in knightMoves) {
            val nr = targetR + d[0]
            val nc = targetC + d[1]
            if (nr in 0..7 && nc in 0..7 && board[nr][nc] == attackerKnight) return true
        }

        val attackerKing = if (byWhite) 'K' else 'k'
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val kr = targetR + dr
                val kc = targetC + dc
                if (kr in 0..7 && kc in 0..7 && board[kr][kc] == attackerKing) return true
            }
        }

        val attackerRook = if (byWhite) 'R' else 'r'
        val attackerQueen = if (byWhite) 'Q' else 'q'
        val orthoDirs = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        for (d in orthoDirs) {
            var r = targetR + d[0]
            var c = targetC + d[1]
            while (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != '.') {
                    if (p == attackerRook || p == attackerQueen) return true
                    break
                }
                r += d[0]
                c += d[1]
            }
        }

        val attackerBishop = if (byWhite) 'B' else 'b'
        val diagDirs = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
        for (d in diagDirs) {
            var r = targetR + d[0]
            var c = targetC + d[1]
            while (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != '.') {
                    if (p == attackerBishop || p == attackerQueen) return true
                    break
                }
                r += d[0]
                c += d[1]
            }
        }

        return false
    }

    fun isKingInCheck(board: Array<CharArray>, isWhiteKing: Boolean): Boolean {
        val kingChar = if (isWhiteKing) 'K' else 'k'
        for (r in 0..7) {
            for (c in 0..7) {
                if (board[r][c] == kingChar) {
                    return isSquareAttackedBy(board, r, c, !isWhiteKing)
                }
            }
        }
        return false
    }

    private fun lowestAttackerValue(board: Array<CharArray>, targetR: Int, targetC: Int, byWhite: Boolean): Float {
        val pawnSourceRow = if (byWhite) targetR + 1 else targetR - 1
        val attackerPawn = if (byWhite) 'P' else 'p'
        if (pawnSourceRow in 0..7) {
            if (targetC - 1 >= 0 && board[pawnSourceRow][targetC - 1] == attackerPawn) return 1.0f
            if (targetC + 1 <= 7 && board[pawnSourceRow][targetC + 1] == attackerPawn) return 1.0f
        }
        val attackerKnight = if (byWhite) 'N' else 'n'
        val knightMoves = arrayOf(
            intArrayOf(-2, -1), intArrayOf(-2, 1),
            intArrayOf(-1, -2), intArrayOf(-1, 2),
            intArrayOf(1, -2), intArrayOf(1, 2),
            intArrayOf(2, -1), intArrayOf(2, 1)
        )
        for (d in knightMoves) {
            val nr = targetR + d[0]
            val nc = targetC + d[1]
            if (nr in 0..7 && nc in 0..7 && board[nr][nc] == attackerKnight) return 3.05f
        }
        var minRay = 99f
        val attackerBishop = if (byWhite) 'B' else 'b'
        val attackerRook = if (byWhite) 'R' else 'r'
        val attackerQueen = if (byWhite) 'Q' else 'q'
        val diagDirs = arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
        for (d in diagDirs) {
            var r = targetR + d[0]
            var c = targetC + d[1]
            while (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != '.') {
                    if (p == attackerBishop) minRay = min(minRay, 3.25f)
                    else if (p == attackerQueen) minRay = min(minRay, 9.0f)
                    break
                }
                r += d[0]
                c += d[1]
            }
        }
        if (minRay <= 3.25f) return minRay

        val orthoDirs = arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
        for (d in orthoDirs) {
            var r = targetR + d[0]
            var c = targetC + d[1]
            while (r in 0..7 && c in 0..7) {
                val p = board[r][c]
                if (p != '.') {
                    if (p == attackerRook) minRay = min(minRay, 5.0f)
                    else if (p == attackerQueen) minRay = min(minRay, 9.0f)
                    break
                }
                r += d[0]
                c += d[1]
            }
        }
        if (minRay < 99f) return minRay

        val attackerKing = if (byWhite) 'K' else 'k'
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val kr = targetR + dr
                val kc = targetC + dc
                if (kr in 0..7 && kc in 0..7 && board[kr][kc] == attackerKing) return 10.0f
            }
        }
        return 99f
    }

    private fun generateMoves(board: Array<CharArray>, isWhite: Boolean, capturesOnly: Boolean): List<EngineMove> {
        val list = ArrayList<EngineMove>(40)
        for (r in 0..7) {
            for (c in 0..7) {
                val p = board[r][c]
                if (p == '.') continue
                if (p.isUpperCase() != isWhite) continue

                when (p.uppercaseChar()) {
                    'P' -> {
                        val dir = if (isWhite) -1 else 1
                        val startRow = if (isWhite) 6 else 1
                        val promoRow = if (isWhite) 0 else 7
                        val nr = r + dir
                        if (nr in 0..7) {
                            if (!capturesOnly && board[nr][c] == '.') {
                                val promo = if (nr == promoRow) (if (isWhite) 'Q' else 'q') else '.'
                                val sc = if (promo != '.') 800 else 10
                                list.add(EngineMove(r, c, nr, c, p, '.', promo, sc))
                                val nnr = r + 2 * dir
                                if (r == startRow && nnr in 0..7 && board[nnr][c] == '.') {
                                    list.add(EngineMove(r, c, nnr, c, p, '.', '.', 15))
                                }
                            } else if (capturesOnly && nr == promoRow && board[nr][c] == '.') {
                                val promo = if (isWhite) 'Q' else 'q'
                                list.add(EngineMove(r, c, nr, c, p, '.', promo, 800))
                            }
                            for (dc in intArrayOf(-1, 1)) {
                                val nc = c + dc
                                if (nc in 0..7) {
                                    val target = board[nr][nc]
                                    if (target != '.' && target.isUpperCase() != isWhite) {
                                        val promo = if (nr == promoRow) (if (isWhite) 'Q' else 'q') else '.'
                                        val sc = 1000 + pieceOrderValue(target) * 10 - 100 + (if (promo != '.') 800 else 0)
                                        list.add(EngineMove(r, c, nr, nc, p, target, promo, sc))
                                    }
                                }
                            }
                        }
                    }
                    'N' -> {
                        val jumps = arrayOf(
                            intArrayOf(-2, -1), intArrayOf(-2, 1),
                            intArrayOf(-1, -2), intArrayOf(-1, 2),
                            intArrayOf(1, -2), intArrayOf(1, 2),
                            intArrayOf(2, -1), intArrayOf(2, 1)
                        )
                        for (d in jumps) {
                            val nr = r + d[0]
                            val nc = c + d[1]
                            if (nr in 0..7 && nc in 0..7) {
                                val target = board[nr][nc]
                                if (target == '.') {
                                    if (!capturesOnly) list.add(EngineMove(r, c, nr, nc, p, '.', '.', 20))
                                } else if (target.isUpperCase() != isWhite) {
                                    val sc = 1000 + pieceOrderValue(target) * 10 - 305
                                    list.add(EngineMove(r, c, nr, nc, p, target, '.', sc))
                                }
                            }
                        }
                    }
                    'B', 'R', 'Q' -> {
                        val dirs = when (p.uppercaseChar()) {
                            'B' -> arrayOf(intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1))
                            'R' -> arrayOf(intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1))
                            else -> arrayOf(
                                intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1),
                                intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1)
                            )
                        }
                        val selfVal = pieceOrderValue(p)
                        for (d in dirs) {
                            var nr = r + d[0]
                            var nc = c + d[1]
                            while (nr in 0..7 && nc in 0..7) {
                                val target = board[nr][nc]
                                if (target == '.') {
                                    if (!capturesOnly) list.add(EngineMove(r, c, nr, nc, p, '.', '.', 15))
                                } else {
                                    if (target.isUpperCase() != isWhite) {
                                        val sc = 1000 + pieceOrderValue(target) * 10 - selfVal
                                        list.add(EngineMove(r, c, nr, nc, p, target, '.', sc))
                                    }
                                    break
                                }
                                nr += d[0]
                                nc += d[1]
                            }
                        }
                    }
                    'K' -> {
                        for (dr in -1..1) {
                            for (dc in -1..1) {
                                if (dr == 0 && dc == 0) continue
                                val nr = r + dr
                                val nc = c + dc
                                if (nr in 0..7 && nc in 0..7) {
                                    val target = board[nr][nc]
                                    if (target == '.') {
                                        if (!capturesOnly) list.add(EngineMove(r, c, nr, nc, p, '.', '.', 5))
                                    } else if (target.isUpperCase() != isWhite) {
                                        val sc = 1000 + pieceOrderValue(target) * 10 - 900
                                        list.add(EngineMove(r, c, nr, nc, p, target, '.', sc))
                                    }
                                }
                            }
                        }
                        if (!capturesOnly) {
                            val homeRow = if (isWhite) 7 else 0
                            val rookChar = if (isWhite) 'R' else 'r'
                            if (r == homeRow && c == 4 && !isKingInCheck(board, isWhite)) {
                                if (board[homeRow][7] == rookChar && board[homeRow][5] == '.' && board[homeRow][6] == '.') {
                                    if (!isSquareAttackedBy(board, homeRow, 5, !isWhite) && !isSquareAttackedBy(board, homeRow, 6, !isWhite)) {
                                        list.add(EngineMove(homeRow, 4, homeRow, 6, p, '.', '.', 60))
                                    }
                                }
                                if (board[homeRow][0] == rookChar && board[homeRow][1] == '.' && board[homeRow][2] == '.' && board[homeRow][3] == '.') {
                                    if (!isSquareAttackedBy(board, homeRow, 3, !isWhite) && !isSquareAttackedBy(board, homeRow, 2, !isWhite)) {
                                        list.add(EngineMove(homeRow, 4, homeRow, 2, p, '.', '.', 55))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        list.sortByDescending { it.score }
        return list
    }

    private fun generateLegalMoves(board: Array<CharArray>, isWhite: Boolean, capturesOnly: Boolean = false): List<EngineMove> {
        val pseudo = generateMoves(board, isWhite, capturesOnly)
        val legal = ArrayList<EngineMove>(pseudo.size)
        for (mv in pseudo) {
            val savedFrom = board[mv.fromR][mv.fromC]
            val savedTo = board[mv.toR][mv.toC]
            val isCastleKingside = (mv.piece.uppercaseChar() == 'K' && mv.fromC == 4 && mv.toC == 6)
            val isCastleQueenside = (mv.piece.uppercaseChar() == 'K' && mv.fromC == 4 && mv.toC == 2)

            board[mv.toR][mv.toC] = if (mv.promotion != '.') mv.promotion else mv.piece
            board[mv.fromR][mv.fromC] = '.'
            if (isCastleKingside) {
                board[mv.fromR][5] = if (isWhite) 'R' else 'r'
                board[mv.fromR][7] = '.'
            } else if (isCastleQueenside) {
                board[mv.fromR][3] = if (isWhite) 'R' else 'r'
                board[mv.fromR][0] = '.'
            }

            val inCheck = isKingInCheck(board, isWhite)

            board[mv.fromR][mv.fromC] = savedFrom
            board[mv.toR][mv.toC] = savedTo
            if (isCastleKingside) {
                board[mv.fromR][7] = if (isWhite) 'R' else 'r'
                board[mv.fromR][5] = '.'
            } else if (isCastleQueenside) {
                board[mv.fromR][0] = if (isWhite) 'R' else 'r'
                board[mv.fromR][3] = '.'
            }

            if (!inCheck) {
                legal.add(mv)
            }
        }
        return legal
    }

    private inline fun <T> withMoveApplied(board: Array<CharArray>, mv: EngineMove, isWhite: Boolean, block: () -> T): T {
        val savedFrom = board[mv.fromR][mv.fromC]
        val savedTo = board[mv.toR][mv.toC]
        val isCastleKingside = (mv.piece.uppercaseChar() == 'K' && mv.fromC == 4 && mv.toC == 6)
        val isCastleQueenside = (mv.piece.uppercaseChar() == 'K' && mv.fromC == 4 && mv.toC == 2)

        board[mv.toR][mv.toC] = if (mv.promotion != '.') mv.promotion else mv.piece
        board[mv.fromR][mv.fromC] = '.'
        if (isCastleKingside) {
            board[mv.fromR][5] = if (isWhite) 'R' else 'r'
            board[mv.fromR][7] = '.'
        } else if (isCastleQueenside) {
            board[mv.fromR][3] = if (isWhite) 'R' else 'r'
            board[mv.fromR][0] = '.'
        }

        return try {
            block()
        } finally {
            board[mv.fromR][mv.fromC] = savedFrom
            board[mv.toR][mv.toC] = savedTo
            if (isCastleKingside) {
                board[mv.fromR][7] = if (isWhite) 'R' else 'r'
                board[mv.fromR][5] = '.'
            } else if (isCastleQueenside) {
                board[mv.fromR][0] = if (isWhite) 'R' else 'r'
                board[mv.fromR][3] = '.'
            }
        }
    }

    private fun quiescenceWhite(
        board: Array<CharArray>,
        isWhiteTurn: Boolean,
        alphaIn: Float,
        betaIn: Float,
        qDepth: Int
    ): Float {
        val inCheck = isKingInCheck(board, isWhiteTurn)
        var alpha = alphaIn
        var beta = betaIn

        val standPat = evaluateBoardPositionalPawns(board)
        if (!inCheck) {
            if (qDepth <= 0) return standPat
            if (isWhiteTurn) {
                if (standPat >= beta) return beta
                if (standPat > alpha) alpha = standPat
            } else {
                if (standPat <= alpha) return alpha
                if (standPat < beta) beta = standPat
            }
        } else if (qDepth <= 0) {
            return standPat
        }

        val moves = generateLegalMoves(board, isWhiteTurn, capturesOnly = !inCheck)
        if (moves.isEmpty()) {
            return if (inCheck) {
                if (isWhiteTurn) -12.0f - qDepth * 0.2f else 12.0f + qDepth * 0.2f
            } else {
                standPat
            }
        }

        if (isWhiteTurn) {
            var best = alpha
            for (mv in moves) {
                val score = withMoveApplied(board, mv, true) {
                    quiescenceWhite(board, false, best, beta, qDepth - 1)
                }
                if (score > best) best = score
                if (best >= beta) return beta
            }
            return best
        } else {
            var best = beta
            for (mv in moves) {
                val score = withMoveApplied(board, mv, false) {
                    quiescenceWhite(board, true, alpha, best, qDepth - 1)
                }
                if (score < best) best = score
                if (best <= alpha) return alpha
            }
            return best
        }
    }

    fun searchEvalWhite(
        board: Array<CharArray>,
        isWhiteTurn: Boolean,
        depth: Int = 2,
        alphaIn: Float = -20.0f,
        betaIn: Float = 20.0f
    ): Float {
        if (depth <= 0) {
            return quiescenceWhite(board, isWhiteTurn, alphaIn, betaIn, qDepth = 5)
        }

        val moves = generateLegalMoves(board, isWhiteTurn, capturesOnly = false)
        if (moves.isEmpty()) {
            return if (isKingInCheck(board, isWhiteTurn)) {
                if (isWhiteTurn) -12.0f - depth * 0.5f else 12.0f + depth * 0.5f
            } else {
                0.0f
            }
        }

        var alpha = alphaIn
        var beta = betaIn

        if (isWhiteTurn) {
            var best = -20.0f
            for (mv in moves) {
                val eval = withMoveApplied(board, mv, true) {
                    searchEvalWhite(board, false, depth - 1, alpha, beta)
                }
                if (eval > best) best = eval
                if (best > alpha) alpha = best
                if (alpha >= beta) break
            }
            return best
        } else {
            var best = 20.0f
            for (mv in moves) {
                val eval = withMoveApplied(board, mv, false) {
                    searchEvalWhite(board, true, depth - 1, alpha, beta)
                }
                if (eval < best) best = eval
                if (best < beta) beta = best
                if (alpha >= beta) break
            }
            return best
        }
    }

    fun analyzeMoveTactical(
        prevState: BoardState,
        currState: BoardState,
        isWhiteMove: Boolean
    ): TacticalMoveAnalysis {
        val workPrev = copyBoard(prevState.board)
        val legalMoves = generateLegalMoves(workPrev, isWhiteMove, capturesOnly = false)

        var bestEvalMover = -20.0f
        var secondBestEvalMover = -20.0f
        var bestMove: EngineMove? = null
        var playedEvalWhiteFromSearch: Float? = null

        val searchChildDepth = 2

        for (mv in legalMoves) {
            val childEvalWhite = withMoveApplied(workPrev, mv, isWhiteMove) {
                searchEvalWhite(workPrev, !isWhiteMove, depth = searchChildDepth)
            }
            val moverEval = if (isWhiteMove) childEvalWhite else -childEvalWhite

            if (mv.fromR == currState.fromRow && mv.fromC == currState.fromCol &&
                mv.toR == currState.toRow && mv.toC == currState.toCol
            ) {
                playedEvalWhiteFromSearch = childEvalWhite
            }

            if (moverEval > bestEvalMover) {
                secondBestEvalMover = bestEvalMover
                bestEvalMover = moverEval
                bestMove = mv
            } else if (moverEval > secondBestEvalMover) {
                secondBestEvalMover = moverEval
            }
        }

        val workCurr = copyBoard(currState.board)
        val finalPlayedEvalWhite = playedEvalWhiteFromSearch
            ?: searchEvalWhite(workCurr, !isWhiteMove, depth = searchChildDepth)

        val finalBestEvalWhite = if (bestMove != null) {
            if (isWhiteMove) bestEvalMover else -bestEvalMover
        } else {
            finalPlayedEvalWhite
        }

        val finalSecondBestEvalWhite = if (secondBestEvalMover > -19.0f) {
            if (isWhiteMove) secondBestEvalMover else -secondBestEvalMover
        } else {
            finalBestEvalWhite
        }

        val isExactBestCoord = bestMove != null &&
            bestMove.fromR == currState.fromRow &&
            bestMove.fromC == currState.fromCol &&
            bestMove.toR == currState.toRow &&
            bestMove.toC == currState.toCol

        val playedMoverEval = if (isWhiteMove) finalPlayedEvalWhite else -finalPlayedEvalWhite
        val cpLoss = (bestEvalMover - playedMoverEval).coerceAtLeast(0f)
        val isBestMove = isExactBestCoord || cpLoss <= 0.06f

        val cleanSan = currState.sanMove.replace("+", "").replace("#", "")
        val isBookMove = currState.moveIndex <= 8 &&
            cpLoss <= 0.16f &&
            (currState.moveIndex <= 4 || cleanSan in commonBookMoves)

        val movedPiece = if (currState.fromRow in 0..7 && currState.fromCol in 0..7) {
            prevState.board[currState.fromRow][currState.fromCol]
        } else '.'
        val capturedPiece = if (currState.toRow in 0..7 && currState.toCol in 0..7) {
            prevState.board[currState.toRow][currState.toCol]
        } else '.'

        val movedVal = pieceBaseValue(movedPiece)
        val capturedVal = pieceBaseValue(capturedPiece)

        val isDestAttackedAfter = if (currState.toRow in 0..7 && currState.toCol in 0..7) {
            isSquareAttackedBy(currState.board, currState.toRow, currState.toCol, !isWhiteMove)
        } else false

        val isRoutineRecaptureOrFreeTake = capturedVal >= 1.0f && (
            !isDestAttackedAfter || capturedVal >= movedVal - 0.3f
        )

        // Checkmate (#) is a Best Move, not a piece sacrifice.
        var isTrueSacrifice = false
        val isCheckmateMove = currState.sanMove.contains("#")
        if (!isCheckmateMove && currState.moveIndex > 10 && isBestMove && playedMoverEval >= 1.25f) {
            if (currState.toRow in 0..7 && currState.toCol in 0..7 && movedVal >= 3.0f) {
                val enemyLowestAttacker = lowestAttackerValue(currState.board, currState.toRow, currState.toCol, !isWhiteMove)
                val friendlyDefended = isSquareAttackedBy(currState.board, currState.toRow, currState.toCol, isWhiteMove)
                if (enemyLowestAttacker < movedVal - 0.5f && (movedVal - capturedVal) >= 2.0f) {
                    if (!friendlyDefended || enemyLowestAttacker + 1.5f < movedVal) {
                        isTrueSacrifice = true
                    }
                }
            }
            if (!isTrueSacrifice && !currState.sanMove.contains("+") && capturedVal < 3.0f) {
                for (r in 0..7) {
                    for (c in 0..7) {
                        if (r == currState.toRow && c == currState.toCol) continue
                        val p = currState.board[r][c]
                        if (p == '.' || p.isUpperCase() != isWhiteMove) continue
                        val pVal = pieceBaseValue(p)
                        if (pVal in 3.0f..9.5f) {
                            val attackedByEnemy = isSquareAttackedBy(currState.board, r, c, !isWhiteMove)
                            val defendedByFriend = isSquareAttackedBy(currState.board, r, c, isWhiteMove)
                            val wasAlreadyHanging = isSquareAttackedBy(prevState.board, r, c, !isWhiteMove) &&
                                !isSquareAttackedBy(prevState.board, r, c, isWhiteMove)
                            if (attackedByEnemy && !defendedByFriend && !wasAlreadyHanging && (pVal - capturedVal) >= 2.0f) {
                                isTrueSacrifice = true
                                break
                            }
                        }
                    }
                }
            }
        }

        return TacticalMoveAnalysis(
            bestEvalWhite = finalBestEvalWhite.coerceIn(-12.0f, 12.0f),
            secondBestEvalWhite = finalSecondBestEvalWhite.coerceIn(-12.0f, 12.0f),
            playedEvalWhite = finalPlayedEvalWhite.coerceIn(-12.0f, 12.0f),
            bestMoveUci = bestMove?.toUci() ?: "",
            isBestMove = isBestMove,
            isBookMove = isBookMove,
            isTrueSacrifice = isTrueSacrifice,
            isRoutineRecaptureOrFreeTake = isRoutineRecaptureOrFreeTake
        )
    }

    fun boardToFen(
        board: Array<CharArray>,
        isWhiteTurn: Boolean,
        fullMoveNumber: Int,
        castlingRights: String = "KQkq",
        enPassantSquare: String = "-",
        halfMoveClock: Int = 0
    ): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var emptyCount = 0
            for (c in 0..7) {
                val ch = board[r][c]
                if (ch == '.') {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        sb.append(emptyCount)
                        emptyCount = 0
                    }
                    sb.append(ch)
                }
            }
            if (emptyCount > 0) sb.append(emptyCount)
            if (r < 7) sb.append('/')
        }
        sb.append(if (isWhiteTurn) " w " else " b ")
        sb.append(castlingRights).append(' ')
        sb.append(enPassantSquare).append(' ')
        sb.append(halfMoveClock.coerceAtLeast(0)).append(' ')
        sb.append(fullMoveNumber.coerceAtLeast(1))
        return sb.toString()
    }

    fun evaluateBoardPositionalPawns(board: Array<CharArray>): Float {
        var score = 0f
        var whiteBishops = 0
        var blackBishops = 0

        for (r in 0..7) {
            for (c in 0..7) {
                val ch = board[r][c]
                if (ch == '.') continue
                val isW = ch.isUpperCase()
                val tableRow = if (isW) r else (7 - r)
                val pieceScore = when (ch.uppercaseChar()) {
                    'P' -> 1.00f + pawnTable[tableRow][c]
                    'N' -> 3.05f + knightTable[tableRow][c]
                    'B' -> {
                        if (isW) whiteBishops++ else blackBishops++
                        3.25f + bishopTable[tableRow][c]
                    }
                    'R' -> 5.00f + (if (tableRow == 1) 0.25f else 0.0f) + (if (c == 3 || c == 4) 0.08f else 0.0f)
                    'Q' -> 9.00f + (bishopTable[tableRow][c] * 0.4f)
                    'K' -> if (tableRow == 7 && (c == 6 || c == 2)) 0.28f else if (tableRow in 3..5) -0.25f else 0.0f
                    else -> 0f
                }
                if (isW) score += pieceScore else score -= pieceScore
            }
        }
        if (whiteBishops >= 2) score += 0.25f
        if (blackBishops >= 2) score -= 0.25f
        return score
    }

    private fun computeCapturedStrings(board: Array<CharArray>): Pair<String, String> {
        var wP = 8; var wN = 2; var wB = 2; var wR = 2; var wQ = 1
        var bP = 8; var bN = 2; var bB = 2; var bR = 2; var bQ = 1

        var matBalance = 0
        for (r in 0..7) {
            for (c in 0..7) {
                when (board[r][c]) {
                    'P' -> { wP--; matBalance += 1 }
                    'N' -> { wN--; matBalance += 3 }
                    'B' -> { wB--; matBalance += 3 }
                    'R' -> { wR--; matBalance += 5 }
                    'Q' -> { wQ--; matBalance += 9 }
                    'p' -> { bP--; matBalance -= 1 }
                    'n' -> { bN--; matBalance -= 3 }
                    'b' -> { bB--; matBalance -= 3 }
                    'r' -> { bR--; matBalance -= 5 }
                    'q' -> { bQ--; matBalance -= 9 }
                }
            }
        }

        val topCaptured = buildString {
            repeat(wP.coerceAtLeast(0)) { append("♟") }
            repeat(wB.coerceAtLeast(0)) { append("♝") }
            repeat(wN.coerceAtLeast(0)) { append("♞") }
            repeat(wR.coerceAtLeast(0)) { append("♜") }
            repeat(wQ.coerceAtLeast(0)) { append("♛") }
        }
        val botCaptured = buildString {
            repeat(bP.coerceAtLeast(0)) { append("♟") }
            repeat(bB.coerceAtLeast(0)) { append("♝") }
            repeat(bN.coerceAtLeast(0)) { append("♞") }
            repeat(bR.coerceAtLeast(0)) { append("♜") }
            repeat(bQ.coerceAtLeast(0)) { append("♛") }
        }

        val topText = when {
            topCaptured.isEmpty() && matBalance < 0 -> "+${-matBalance}"
            topCaptured.isNotEmpty() && matBalance < 0 -> "$topCaptured +${-matBalance}"
            topCaptured.isEmpty() -> "-"
            else -> topCaptured
        }
        val botText = when {
            botCaptured.isEmpty() && matBalance > 0 -> "+$matBalance"
            botCaptured.isNotEmpty() && matBalance > 0 -> "$botCaptured +$matBalance"
            botCaptured.isEmpty() -> "-"
            else -> botCaptured
        }

        return Pair(topText, botText)
    }
}
