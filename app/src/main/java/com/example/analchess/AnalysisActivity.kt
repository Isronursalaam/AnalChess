package com.example.analchess

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.analchess.data.ApiService
import com.example.analchess.data.BoardState
import com.example.analchess.data.ChessEngine
import com.example.analchess.data.GameAnalysisSummary
import com.example.analchess.data.GameItem
import com.example.analchess.data.MoveQuality
import com.example.analchess.data.SelectedGameHolder
import com.example.analchess.ui.ChessBoardView

class AnalysisActivity : AppCompatActivity() {

    private lateinit var rootAnalysis: FrameLayout
    private lateinit var scrollAnalysisContent: ScrollView
    private lateinit var contentAnalysisInner: LinearLayout
    private lateinit var layoutAnalysisLoading: LinearLayout
    private lateinit var tvLoadingProgress: TextView
    private lateinit var pbAnalysisHorizontal: ProgressBar

    private lateinit var ivTopPlayerAvatar: ImageView
    private lateinit var tvTopPlayerName: TextView
    private lateinit var tvTopPlayerRating: TextView
    private lateinit var tvTopCapturedPieces: TextView

    private lateinit var chessBoardView: ChessBoardView

    private lateinit var ivBottomPlayerAvatar: ImageView
    private lateinit var tvBottomPlayerName: TextView
    private lateinit var tvBottomPlayerRating: TextView
    private lateinit var tvBottomCapturedPieces: TextView

    private lateinit var btnPrevMove: FrameLayout
    private lateinit var hsvMoveStrip: HorizontalScrollView
    private lateinit var layoutMoveStripContainer: LinearLayout
    private lateinit var btnNextMove: FrameLayout

    private lateinit var tvColPlayer1: TextView
    private lateinit var tvColPlayer2: TextView
    private lateinit var ivColAvatar1: ImageView
    private lateinit var ivColAvatar2: ImageView

    private lateinit var tvAccuracyPlayer1: TextView
    private lateinit var tvAccuracyPlayer2: TextView

    private lateinit var layoutClassifications: LinearLayout
    private lateinit var tvBrilian1: TextView
    private lateinit var tvBrilian2: TextView
    private lateinit var tvBagus1: TextView
    private lateinit var tvBagus2: TextView
    private lateinit var tvTerbaik1: TextView
    private lateinit var tvTerbaik2: TextView
    private lateinit var tvSangatBaik1: TextView
    private lateinit var tvSangatBaik2: TextView
    private lateinit var tvBuku1: TextView
    private lateinit var tvBuku2: TextView
    private lateinit var tvKesalahan1: TextView
    private lateinit var tvKesalahan2: TextView
    private lateinit var tvTerlewat1: TextView
    private lateinit var tvTerlewat2: TextView
    private lateinit var tvBlunder1: TextView
    private lateinit var tvBlunder2: TextView

    private lateinit var btnToggleStats: ImageView
    private lateinit var tvPerfRating1: TextView
    private lateinit var tvPerfRating2: TextView

    private var boardStates: List<BoardState> = emptyList()
    private val moveTokenContainers = HashMap<Int, LinearLayout>()
    private val moveTokenTexts = HashMap<Int, TextView>()
    private var currentMoveIndex: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        bindViews()
        setupWindowInsets()

        val game = SelectedGameHolder.selectedGame
        if (game == null) {
            finish()
            return
        }

        // Show loading screen first until 100% of moves are analyzed
        layoutAnalysisLoading.visibility = View.VISIBLE
        scrollAnalysisContent.visibility = View.INVISIBLE

        setupPlayersHeader(game)
        boardStates = ChessEngine.parsePgnToBoardStates(game.pgn)
        currentMoveIndex = 0

        btnPrevMove.setOnClickListener {
            if (currentMoveIndex > 0) {
                currentMoveIndex--
                renderBoardAtMove(currentMoveIndex)
            }
        }

        btnNextMove.setOnClickListener {
            if (currentMoveIndex < boardStates.size - 1) {
                currentMoveIndex++
                renderBoardAtMove(currentMoveIndex)
            }
        }

        btnToggleStats.setOnClickListener {
            if (layoutClassifications.visibility == View.VISIBLE) {
                layoutClassifications.visibility = View.GONE
                btnToggleStats.rotation = 180f
            } else {
                layoutClassifications.visibility = View.VISIBLE
                btnToggleStats.rotation = 0f
            }
        }

        // Perform full game analysis during loading phase
        ApiService.analyzeGameFullBeforeDisplay(
            game = game,
            states = boardStates,
            onProgress = { percent, step, total ->
                pbAnalysisHorizontal.progress = percent
                tvLoadingProgress.text = "Stockfish sedang menghitung langkah $step/$total ($percent%)"
            },
            onComplete = { summary, whiteAvatarUrl, blackAvatarUrl ->
                ApiService.loadImage(blackAvatarUrl, ivTopPlayerAvatar, R.drawable.ic_pawn_avatar)
                ApiService.loadImage(whiteAvatarUrl, ivBottomPlayerAvatar, R.drawable.ic_sukiwir_avatar)
                ApiService.loadImage(blackAvatarUrl, ivColAvatar1, R.drawable.ic_pawn_avatar)
                ApiService.loadImage(whiteAvatarUrl, ivColAvatar2, R.drawable.ic_pawn_avatar)

                buildFullMoveStrip()
                bindAnalysisSummary(summary)
                renderBoardAtMove(0)

                layoutAnalysisLoading.visibility = View.GONE
                scrollAnalysisContent.visibility = View.VISIBLE
            }
        )
    }

    private fun setupWindowInsets() {
        val density = resources.displayMetrics.density
        val horizontalPad = (18 * density).toInt()
        val extraTopPad = (14 * density).toInt()
        val extraBotPad = (18 * density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(rootAnalysis) { _, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            contentAnalysisInner.setPadding(
                horizontalPad,
                sysBars.top + extraTopPad,
                horizontalPad,
                sysBars.bottom + extraBotPad
            )
            insets
        }
    }

    private fun bindViews() {
        rootAnalysis = findViewById(R.id.rootAnalysis)
        scrollAnalysisContent = findViewById(R.id.scrollAnalysisContent)
        contentAnalysisInner = findViewById(R.id.contentAnalysisInner)
        layoutAnalysisLoading = findViewById(R.id.layoutAnalysisLoading)
        tvLoadingProgress = findViewById(R.id.tvLoadingProgress)
        pbAnalysisHorizontal = findViewById(R.id.pbAnalysisHorizontal)

        ivTopPlayerAvatar = findViewById(R.id.ivTopPlayerAvatar)
        tvTopPlayerName = findViewById(R.id.tvTopPlayerName)
        tvTopPlayerRating = findViewById(R.id.tvTopPlayerRating)
        tvTopCapturedPieces = findViewById(R.id.tvTopCapturedPieces)

        chessBoardView = findViewById(R.id.chessBoardView)

        ivBottomPlayerAvatar = findViewById(R.id.ivBottomPlayerAvatar)
        tvBottomPlayerName = findViewById(R.id.tvBottomPlayerName)
        tvBottomPlayerRating = findViewById(R.id.tvBottomPlayerRating)
        tvBottomCapturedPieces = findViewById(R.id.tvBottomCapturedPieces)

        btnPrevMove = findViewById(R.id.btnPrevMove)
        hsvMoveStrip = findViewById(R.id.hsvMoveStrip)
        layoutMoveStripContainer = findViewById(R.id.layoutMoveStripContainer)
        btnNextMove = findViewById(R.id.btnNextMove)

        tvColPlayer1 = findViewById(R.id.tvColPlayer1)
        tvColPlayer2 = findViewById(R.id.tvColPlayer2)
        ivColAvatar1 = findViewById(R.id.ivColAvatar1)
        ivColAvatar2 = findViewById(R.id.ivColAvatar2)

        tvAccuracyPlayer1 = findViewById(R.id.tvAccuracyPlayer1)
        tvAccuracyPlayer2 = findViewById(R.id.tvAccuracyPlayer2)

        layoutClassifications = findViewById(R.id.layoutClassifications)
        tvBrilian1 = findViewById(R.id.tvBrilian1)
        tvBrilian2 = findViewById(R.id.tvBrilian2)
        tvBagus1 = findViewById(R.id.tvBagus1)
        tvBagus2 = findViewById(R.id.tvBagus2)
        tvTerbaik1 = findViewById(R.id.tvTerbaik1)
        tvTerbaik2 = findViewById(R.id.tvTerbaik2)
        tvSangatBaik1 = findViewById(R.id.tvSangatBaik1)
        tvSangatBaik2 = findViewById(R.id.tvSangatBaik2)
        tvBuku1 = findViewById(R.id.tvBuku1)
        tvBuku2 = findViewById(R.id.tvBuku2)
        tvKesalahan1 = findViewById(R.id.tvKesalahan1)
        tvKesalahan2 = findViewById(R.id.tvKesalahan2)
        tvTerlewat1 = findViewById(R.id.tvTerlewat1)
        tvTerlewat2 = findViewById(R.id.tvTerlewat2)
        tvBlunder1 = findViewById(R.id.tvBlunder1)
        tvBlunder2 = findViewById(R.id.tvBlunder2)

        btnToggleStats = findViewById(R.id.btnToggleStats)
        tvPerfRating1 = findViewById(R.id.tvPerfRating1)
        tvPerfRating2 = findViewById(R.id.tvPerfRating2)
    }

    private fun setupPlayersHeader(game: GameItem) {
        tvTopPlayerName.text = game.blackUsername
        tvTopPlayerRating.text = " (${game.blackRating})"

        tvBottomPlayerName.text = game.whiteUsername
        tvBottomPlayerRating.text = " (${game.whiteRating})"

        tvColPlayer1.text = game.blackUsername
        tvColPlayer2.text = game.whiteUsername

        ivColAvatar1.setImageResource(R.drawable.ic_pawn_avatar)
        ivColAvatar2.setImageResource(R.drawable.ic_pawn_avatar)
    }

    /**
     * Builds the full horizontal Chess.com-style move notation strip with mini move-quality badges.
     */
    private fun buildFullMoveStrip() {
        layoutMoveStripContainer.removeAllViews()
        moveTokenContainers.clear()
        moveTokenTexts.clear()

        val density = resources.displayMetrics.density
        val totalHalfMoves = boardStates.size - 1
        if (totalHalfMoves <= 0) return

        val totalFullMoves = (totalHalfMoves + 1) / 2
        for (fullMove in 1..totalFullMoves) {
            val numTv = TextView(this).apply {
                text = "$fullMove."
                setTextColor("#8E8D8A".toColorInt())
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                gravity = Gravity.CENTER_VERTICAL
                val startMargin = if (fullMove == 1) (2 * density).toInt() else (10 * density).toInt()
                setPadding(startMargin, 0, (3 * density).toInt(), 0)
            }
            layoutMoveStripContainer.addView(numTv)

            // White move token
            val whiteIdx = (fullMove - 1) * 2 + 1
            if (whiteIdx <= totalHalfMoves) {
                val wView = createMoveTokenChip(whiteIdx, boardStates[whiteIdx], density)
                layoutMoveStripContainer.addView(wView)
            }

            // Black move token
            val blackIdx = whiteIdx + 1
            if (blackIdx <= totalHalfMoves) {
                val bView = createMoveTokenChip(blackIdx, boardStates[blackIdx], density)
                layoutMoveStripContainer.addView(bView)
            }
        }
    }

    private fun createMoveTokenChip(moveIdx: Int, state: BoardState, density: Float): LinearLayout {
        val hPad = (5 * density).toInt()
        val vPad = (3 * density).toInt()

        val chip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(hPad, vPad, hPad, vPad)
            setOnClickListener {
                currentMoveIndex = moveIdx
                renderBoardAtMove(currentMoveIndex)
            }
        }

        val badgeRes = when (state.moveQuality) {
            MoveQuality.BRILIAN -> R.drawable.ic_badge_brilian
            MoveQuality.BAGUS -> R.drawable.ic_badge_bagus
            MoveQuality.TERBAIK -> R.drawable.ic_badge_terbaik
            MoveQuality.SANGAT_BAIK -> R.drawable.ic_badge_sangat_baik
            MoveQuality.BOOK -> R.drawable.ic_badge_buku
            MoveQuality.KESALAHAN -> R.drawable.ic_badge_kesalahan
            MoveQuality.TERLEWAT -> R.drawable.ic_badge_terlewat
            MoveQuality.BLUNDER -> R.drawable.ic_badge_blunder
            MoveQuality.NONE -> 0
        }

        if (badgeRes != 0) {
            val iconSize = (13 * density).toInt()
            val iv = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                    marginEnd = (3 * density).toInt()
                }
                setImageResource(badgeRes)
            }
            chip.addView(iv)
        }

        val tv = TextView(this).apply {
            text = state.sanMove
            setTextColor("#CCCCCC".toColorInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
        }
        chip.addView(tv)

        moveTokenContainers[moveIdx] = chip
        moveTokenTexts[moveIdx] = tv
        return chip
    }

    private fun renderBoardAtMove(index: Int) {
        if (boardStates.isEmpty()) return
        val safeIndex = index.coerceIn(0, boardStates.size - 1)
        val state = boardStates[safeIndex]

        chessBoardView.setBoardState(state)
        tvTopCapturedPieces.text = state.topCapturedText
        tvBottomCapturedPieces.text = state.bottomCapturedText

        highlightAndScrollMoveStrip(safeIndex)
    }

    private fun highlightAndScrollMoveStrip(activeMoveIndex: Int) {
        val focusIdx = if (activeMoveIndex == 0) 1 else activeMoveIndex

        for ((idx, chip) in moveTokenContainers) {
            val tv = moveTokenTexts[idx]
            if (idx == focusIdx) {
                chip.setBackgroundResource(R.drawable.bg_nav_btn)
                tv?.setTextColor("#FFFFFF".toColorInt())
                tv?.setTypeface(null, Typeface.BOLD)
                tv?.paintFlags = (tv?.paintFlags ?: 0) or Paint.UNDERLINE_TEXT_FLAG
            } else {
                chip.background = null
                tv?.setTextColor("#CCCCCC".toColorInt())
                tv?.setTypeface(null, Typeface.NORMAL)
                tv?.paintFlags = (tv?.paintFlags ?: 0) and Paint.UNDERLINE_TEXT_FLAG.inv()
            }
        }

        val targetView = moveTokenContainers[focusIdx] ?: return
        hsvMoveStrip.post {
            val scrollX = (targetView.left + targetView.width / 2) - (hsvMoveStrip.width / 2)
            hsvMoveStrip.smoothScrollTo(scrollX.coerceAtLeast(0), 0)
        }
    }

    private fun bindAnalysisSummary(summary: GameAnalysisSummary) {
        tvAccuracyPlayer1.text = summary.accuracy1.toString()
        tvAccuracyPlayer2.text = summary.accuracy2.toString()

        tvBrilian1.text = summary.brilian1.toString()
        tvBrilian2.text = summary.brilian2.toString()

        tvBagus1.text = summary.bagus1.toString()
        tvBagus2.text = summary.bagus2.toString()

        tvTerbaik1.text = summary.terbaik1.toString()
        tvTerbaik2.text = summary.terbaik2.toString()

        tvSangatBaik1.text = summary.sangatBaik1.toString()
        tvSangatBaik2.text = summary.sangatBaik2.toString()

        tvBuku1.text = summary.buku1.toString()
        tvBuku2.text = summary.buku2.toString()

        tvKesalahan1.text = summary.kesalahan1.toString()
        tvKesalahan2.text = summary.kesalahan2.toString()

        tvTerlewat1.text = summary.terlewat1.toString()
        tvTerlewat2.text = summary.terlewat2.toString()

        tvBlunder1.text = summary.blunder1.toString()
        tvBlunder2.text = summary.blunder2.toString()

        tvPerfRating1.text = summary.perfRating1.toString()
        tvPerfRating2.text = summary.perfRating2.toString()
    }
}
