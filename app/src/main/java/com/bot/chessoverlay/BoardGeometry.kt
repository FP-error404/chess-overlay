package com.bot.chessoverlay

object BoardGeometry {
    // Geometri papan game di layar 1080 x 2400 (diukur dari screenshot Chess Club).
    const val REF_W = 1080f
    const val REF_H = 2400f
    const val LEFT = 30f
    const val TOP = 662f
    const val CELL = 127f

    fun colOf(sq: Int, flipped: Boolean): Int = if (flipped) 7 - (sq % 8) else sq % 8
    fun rowOf(sq: Int, flipped: Boolean): Int = if (flipped) sq / 8 else 7 - (sq / 8)

    fun sqName(col: Int, row: Int, flipped: Boolean): String {
        val f = if (flipped) 7 - col else col
        val r = if (flipped) row else 7 - row
        return "${'a' + f}${r + 1}"
    }
}
