package com.bot.chessoverlay

object BoardReader {

    class Result(val present: Boolean, val highlights: List<String>)

    // Titik sampling di strip bawah dalam petak agar tidak ketutup bidak 3D.
    private val DX = intArrayOf(22, 64, 105, 22, 64, 105)
    private val DY = intArrayOf(108, 108, 108, 116, 116, 116)

    private fun red(c: Int): Int = (c shr 16) and 255
    private fun green(c: Int): Int = (c shr 8) and 255
    private fun blue(c: Int): Int = c and 255

    private fun isGold(c: Int): Boolean {
        val r = red(c)
        val g = green(c)
        val b = blue(c)
        return r >= 135 && g >= 90 && b <= 35 && (r - b) >= 110 && (g - b) >= 70
    }

    private fun isCream(c: Int): Boolean {
        val r = red(c)
        val g = green(c)
        val b = blue(c)
        return r > 200 && g > 175 && b > 140 && (r - b) >= 20 && (r - b) < 75
    }

    private fun isDark(c: Int): Boolean {
        return red(c) < 50 && green(c) < 50 && blue(c) < 50
    }

    fun analyze(w: Int, h: Int, flipped: Boolean, px: (Int, Int) -> Int): Result {
        val sx = w / BoardGeometry.REF_W
        val sy = h / BoardGeometry.REF_H
        val hl = ArrayList<String>()
        var valid = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                var gold = 0
                var ok = 0
                for (i in 0 until 6) {
                    val x = ((BoardGeometry.LEFT + col * BoardGeometry.CELL + DX[i]) * sx).toInt()
                    val y = ((BoardGeometry.TOP + row * BoardGeometry.CELL + DY[i]) * sy).toInt()
                    if (x < 0 || y < 0 || x >= w || y >= h) continue
                    val c = px(x, y)
                    if (isGold(c)) {
                        gold += 1
                        ok += 1
                    } else if ((col + row) % 2 == 0) {
                        if (isCream(c)) ok += 1
                    } else {
                        if (isDark(c)) ok += 1
                    }
                }
                if (gold >= 4) hl.add(BoardGeometry.sqName(col, row, flipped))
                if (ok >= 4) valid += 1
            }
        }
        return Result(valid >= 52, hl)
    }
}
