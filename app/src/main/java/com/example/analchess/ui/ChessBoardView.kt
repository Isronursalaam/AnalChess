package com.example.analchess.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.example.analchess.R
import com.example.analchess.data.BoardState
import com.example.analchess.data.ChessEngine
import com.example.analchess.data.MoveQuality
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class ChessBoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var board: Array<CharArray> = ChessEngine.createInitialBoard()
    private var fromRow: Int = -1
    private var fromCol: Int = -1
    private var toRow: Int = -1
    private var toCol: Int = -1
    private var evalWhitePawns: Float = 0.0f
    private var moveQuality: MoveQuality = MoveQuality.NONE

    private val pieceDrawables = HashMap<Char, Drawable?>()
    private val badgeDrawables = HashMap<MoveQuality, Drawable?>()

    private val lightSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#EBECD0")
    }
    private val darkSquarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#779556")
    }
    private val highlightLightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#F5F682")
    }
    private val highlightDarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B9CA43")
    }

    private val coordTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#B881B64C")
    }

    private val evalBlackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#2B2A27")
    }
    private val evalWhitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#FFFFFF")
    }

    private val badgeShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#262522")
    }

    private val arrowPath = Path()

    init {
        pieceDrawables['P'] = AppCompatResources.getDrawable(context, R.drawable.piece_wp)
        pieceDrawables['N'] = AppCompatResources.getDrawable(context, R.drawable.piece_wn)
        pieceDrawables['B'] = AppCompatResources.getDrawable(context, R.drawable.piece_wb)
        pieceDrawables['R'] = AppCompatResources.getDrawable(context, R.drawable.piece_wr)
        pieceDrawables['Q'] = AppCompatResources.getDrawable(context, R.drawable.piece_wq)
        pieceDrawables['K'] = AppCompatResources.getDrawable(context, R.drawable.piece_wk)

        pieceDrawables['p'] = AppCompatResources.getDrawable(context, R.drawable.piece_bp)
        pieceDrawables['n'] = AppCompatResources.getDrawable(context, R.drawable.piece_bn)
        pieceDrawables['b'] = AppCompatResources.getDrawable(context, R.drawable.piece_bb)
        pieceDrawables['r'] = AppCompatResources.getDrawable(context, R.drawable.piece_br)
        pieceDrawables['q'] = AppCompatResources.getDrawable(context, R.drawable.piece_bq)
        pieceDrawables['k'] = AppCompatResources.getDrawable(context, R.drawable.piece_bk)

        badgeDrawables[MoveQuality.BRILIAN] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_brilian)
        badgeDrawables[MoveQuality.BAGUS] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_bagus)
        badgeDrawables[MoveQuality.TERBAIK] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_terbaik)
        badgeDrawables[MoveQuality.SANGAT_BAIK] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_sangat_baik)
        badgeDrawables[MoveQuality.BOOK] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_buku)
        badgeDrawables[MoveQuality.KESALAHAN] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_kesalahan)
        badgeDrawables[MoveQuality.TERLEWAT] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_terlewat)
        badgeDrawables[MoveQuality.BLUNDER] = AppCompatResources.getDrawable(context, R.drawable.ic_badge_blunder)
    }

    fun setBoardState(state: BoardState) {
        this.board = state.board
        this.fromRow = state.fromRow
        this.fromCol = state.fromCol
        this.toRow = state.toRow
        this.toCol = state.toCol
        this.evalWhitePawns = state.evalWhitePawns
        this.moveQuality = state.moveQuality
        invalidate()
    }

    fun updateEval(evalWhitePawns: Float) {
        this.evalWhitePawns = evalWhitePawns
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = MeasureSpec.getSize(widthMeasureSpec)
        val density = resources.displayMetrics.density
        val evalGap = (6f * density).toInt()
        val evalWidth = (12f * density).toInt()
        val boardSize = (totalWidth - evalGap - evalWidth).coerceAtLeast(100)
        setMeasuredDimension(totalWidth, boardSize)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val density = resources.displayMetrics.density
        val evalGap = 6f * density
        val evalWidth = 12f * density
        val boardSize = (width - evalGap - evalWidth).coerceAtLeast(100f)
        val sqSize = boardSize / 8f

        coordTextPaint.textSize = sqSize * 0.21f

        // 1. Draw 8x8 Board Squares & Chess.com Coordinates (8..1 and a..h)
        for (r in 0..7) {
            for (c in 0..7) {
                val left = c * sqSize
                val top = r * sqSize
                val right = left + sqSize
                val bottom = top + sqSize

                val isLight = (r + c) % 2 == 0
                val isHighlighted = (r == fromRow && c == fromCol) || (r == toRow && c == toCol)

                val sqPaint = when {
                    isHighlighted && isLight -> highlightLightPaint
                    isHighlighted && !isLight -> highlightDarkPaint
                    isLight -> lightSquarePaint
                    else -> darkSquarePaint
                }
                canvas.drawRect(left, top, right, bottom, sqPaint)

                // Rank numbers (8..1) on the left edge (col 0)
                if (c == 0) {
                    coordTextPaint.color = if (isLight) Color.parseColor("#779556") else Color.parseColor("#EBECD0")
                    coordTextPaint.textAlign = Paint.Align.LEFT
                    val rankStr = (8 - r).toString()
                    canvas.drawText(rankStr, left + sqSize * 0.06f, top + sqSize * 0.23f, coordTextPaint)
                }

                // File letters (a..h) on the bottom edge (row 7)
                if (r == 7) {
                    coordTextPaint.color = if (isLight) Color.parseColor("#779556") else Color.parseColor("#EBECD0")
                    coordTextPaint.textAlign = Paint.Align.RIGHT
                    val fileStr = ('a' + c).toString()
                    canvas.drawText(fileStr, right - sqSize * 0.06f, bottom - sqSize * 0.07f, coordTextPaint)
                }
            }
        }

        // 2. Draw Chess.com Neo Vector Pieces
        val pad = (sqSize * 0.03f).toInt()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board[r][c]
                if (piece != '.') {
                    val drawable = pieceDrawables[piece]
                    if (drawable != null) {
                        val left = (c * sqSize).toInt() + pad
                        val top = (r * sqSize).toInt() + pad
                        val right = ((c + 1) * sqSize).toInt() - pad
                        val bottom = ((r + 1) * sqSize).toInt() - pad
                        drawable.setBounds(left, top, right, bottom)
                        drawable.draw(canvas)
                    }
                }
            }
        }

        // 3. Draw Move Arrow from (fromRow, fromCol) to (toRow, toCol)
        if (fromRow in 0..7 && fromCol in 0..7 && toRow in 0..7 && toCol in 0..7) {
            drawMoveArrow(canvas, fromRow, fromCol, toRow, toCol, sqSize)
        }

        // 4. Draw Chess.com Move Quality Badge on Destination Square (toRow, toCol)
        if (toRow in 0..7 && toCol in 0..7 && moveQuality != MoveQuality.NONE) {
            val badgeDrawable = badgeDrawables[moveQuality]
            if (badgeDrawable != null) {
                val radius = sqSize * 0.22f
                val sqRight = (toCol + 1) * sqSize
                val sqTop = toRow * sqSize

                val cx = (sqRight - radius * 0.38f).coerceIn(radius + 2f, boardSize - radius - 2f)
                val cy = (sqTop + radius * 0.38f).coerceIn(radius + 2f, boardSize - radius - 2f)

                canvas.drawCircle(cx, cy, radius + 1.5f * density, badgeShadowPaint)
                badgeDrawable.setBounds(
                    (cx - radius).toInt(),
                    (cy - radius).toInt(),
                    (cx + radius).toInt(),
                    (cy + radius).toInt()
                )
                badgeDrawable.draw(canvas)
            }
        }

        // 5. Draw Right-Side Vertical Evaluation Bar
        val evalLeft = boardSize + evalGap
        val evalRight = evalLeft + evalWidth
        val clampedEval = evalWhitePawns.coerceIn(-10f, 10f)
        val whiteFraction = (0.5f + (clampedEval / 20f)).coerceIn(0.05f, 0.95f)
        val splitY = boardSize * (1f - whiteFraction)

        canvas.drawRect(evalLeft, 0f, evalRight, splitY, evalBlackPaint)
        canvas.drawRect(evalLeft, splitY, evalRight, boardSize, evalWhitePaint)
    }

    private fun drawMoveArrow(
        canvas: Canvas,
        r1: Int,
        c1: Int,
        r2: Int,
        c2: Int,
        sqSize: Float
    ) {
        val x1 = (c1 + 0.5f) * sqSize
        val y1 = (r1 + 0.5f) * sqSize
        val x2 = (c2 + 0.5f) * sqSize
        val y2 = (r2 + 0.5f) * sqSize

        val dx = x2 - x1
        val dy = y2 - y1
        val dist = hypot(dx, dy)
        if (dist < sqSize * 0.3f) return

        arrowPaint.color = when (moveQuality) {
            MoveQuality.BRILIAN -> Color.parseColor("#A61BACA6")
            MoveQuality.BAGUS -> Color.parseColor("#A65C8BB0")
            MoveQuality.TERBAIK, MoveQuality.SANGAT_BAIK -> Color.parseColor("#A673B845")
            MoveQuality.BOOK -> Color.parseColor("#A6A88865")
            MoveQuality.KESALAHAN -> Color.parseColor("#A6F09426")
            MoveQuality.TERLEWAT -> Color.parseColor("#A6E05347")
            MoveQuality.BLUNDER -> Color.parseColor("#A6CA3431")
            MoveQuality.NONE -> Color.parseColor("#A673B845")
        }

        val angle = atan2(dy, dx)
        val shaftHalfW = sqSize * 0.085f
        val headHalfW = sqSize * 0.22f
        val headLen = sqSize * 0.32f
        val endPullback = sqSize * 0.12f

        val tipX = x2 - endPullback * cos(angle)
        val tipY = y2 - endPullback * sin(angle)
        val neckX = tipX - headLen * cos(angle)
        val neckY = tipY - headLen * sin(angle)

        val perpX = -sin(angle)
        val perpY = cos(angle)

        arrowPath.reset()
        arrowPath.moveTo(x1 + perpX * shaftHalfW, y1 + perpY * shaftHalfW)
        arrowPath.lineTo(neckX + perpX * shaftHalfW, neckY + perpY * shaftHalfW)
        arrowPath.lineTo(neckX + perpX * headHalfW, neckY + perpY * headHalfW)
        arrowPath.lineTo(tipX, tipY)
        arrowPath.lineTo(neckX - perpX * headHalfW, neckY - perpY * headHalfW)
        arrowPath.lineTo(neckX - perpX * shaftHalfW, neckY - perpY * shaftHalfW)
        arrowPath.lineTo(x1 - perpX * shaftHalfW, y1 - perpY * shaftHalfW)
        arrowPath.close()

        canvas.drawPath(arrowPath, arrowPaint)
    }
}
