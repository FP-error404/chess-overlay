package com.bot.chessoverlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.sqrt

class ArrowView(context: Context) : View(context) {

    companion object {
        val OVERLAY_GREEN = Color.argb(230, 0, 230, 85)
    }

    var fromSq = -1
    var toSq = -1
    var flipped = false
    var show = true

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OVERLAY_GREEN
        style = Paint.Style.STROKE
        strokeWidth = 7f
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OVERLAY_GREEN
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = OVERLAY_GREEN
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!show || fromSq < 0 || toSq < 0) return

        val sx = width / BoardGeometry.REF_W
        val sy = height / BoardGeometry.REF_H
        val left = BoardGeometry.LEFT * sx
        val top = BoardGeometry.TOP * sy
        val cw = BoardGeometry.CELL * sx
        val ch = BoardGeometry.CELL * sy

        val fx = left + BoardGeometry.colOf(fromSq, flipped) * cw + cw / 2f
        val fy = top + BoardGeometry.rowOf(fromSq, flipped) * ch + ch / 2f
        val tx = left + BoardGeometry.colOf(toSq, flipped) * cw + cw / 2f
        val ty = top + BoardGeometry.rowOf(toSq, flipped) * ch + ch / 2f

        val dx = tx - fx
        val dy = ty - fy
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1f) return
        val ux = dx / len
        val uy = dy / len

        // Mulai garis panah sedikit dari tepi lingkaran asal agar rapi
        val circleRadius = cw * 0.34f
        val startX = fx + ux * (circleRadius * 0.85f)
        val startY = fy + uy * (circleRadius * 0.85f)

        val headLen = cw * 0.48f
        val bx = tx - ux * headLen
        val by = ty - uy * headLen

        linePaint.strokeWidth = cw * 0.15f
        canvas.drawLine(startX, startY, bx, by, linePaint)

        val px = -uy
        val py = ux
        val hw = cw * 0.27f
        val path = Path()
        path.moveTo(tx, ty)
        path.lineTo(bx + px * hw, by + py * hw)
        path.lineTo(bx - px * hw, by - py * hw)
        path.close()
        canvas.drawPath(path, headPaint)

        // Lingkaran hijau pada bidak asal
        circlePaint.strokeWidth = (cw * 0.065f).coerceAtLeast(6f)
        canvas.drawCircle(fx, fy, circleRadius, circlePaint)
    }
}
