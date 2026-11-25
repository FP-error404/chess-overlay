package com.bot.chessoverlay

class MoveSynchronizer {

    data class SyncResult(
        val fen: String,
        val detectedMove: String,
        val outOfSync: Boolean = false,
        val assumedQueenPromotion: Boolean = false
    )

    private var pendingKey = ""
    private var pendingCount = 0

    private var lastAcceptedKey = ""
    private var consecutiveEmptyCount = 0
    private var lastTimestamp = 0L
    private var sequenceId = 0L

    fun observe(
        fen: String,
        highlights: List<String>
    ): SyncResult? {
        sequenceId++
        val now = System.currentTimeMillis()

        // Highlight kosong: naikkan consecutiveEmptyCount saja, jangan ubah FEN
        // dan jangan hapus lastAcceptedKey karena jeda frame kosong.
        if (highlights.isEmpty()) {
            pendingKey = ""
            pendingCount = 0
            consecutiveEmptyCount++
            return null
        }

        consecutiveEmptyCount = 0

        if (highlights.size != 2) {
            resetPending()
            return null
        }

        val squares = highlights.sorted()
        val key = squares.joinToString("")

        if (key == lastAcceptedKey) {
            return null
        }

        if (key != pendingKey) {
            pendingKey = key
            pendingCount = 1
            lastTimestamp = now
            return null
        }

        pendingCount++
        if (pendingCount < 2) return null

        pendingKey = ""
        pendingCount = 0

        // 1. Coba promosi ratu dulu jika legal untuk giliran aktif
        val promoMove = tryPromotionAsQueen(fen, squares[0], squares[1])
        if (promoMove != null) {
            lastAcceptedKey = key
            lastTimestamp = now
            return SyncResult(
                fen = promoMove.first,
                detectedMove = promoMove.second,
                outOfSync = false,
                assumedQueenPromotion = true
            )
        }

        // 2. Jika bukan promosi ratu legal, coba langkah biasa (termasuk non-pion di rank 7-8 / 1-2)
        val directMove = tryPair(fen, squares[0], squares[1])
        if (directMove != null) {
            lastAcceptedKey = key
            lastTimestamp = now
            return SyncResult(
                fen = directMove.first,
                detectedMove = directMove.second,
                outOfSync = false
            )
        }

        // 3. Cek alternatif castling jika highlight menyala pada kotak raja + benteng
        val castlingMove = tryCastlingPair(fen, squares[0], squares[1])
        if (castlingMove != null) {
            lastAcceptedKey = key
            lastTimestamp = now
            return SyncResult(
                fen = castlingMove.first,
                detectedMove = castlingMove.second,
                outOfSync = false
            )
        }

        // 4. Tidak cocok / ilegal / bukan giliran: jangan nebak, tahan FEN terakhir, tandai outOfSync
        lastAcceptedKey = key
        lastTimestamp = now
        return SyncResult(
            fen = fen,
            detectedMove = "${squares[0]}${squares[1]}",
            outOfSync = true
        )
    }

    private fun isPromotionSquarePair(a: String, b: String): Boolean {
        val ranks = setOf(a.getOrNull(1), b.getOrNull(1))
        val whitePromo = ('7' in ranks && '8' in ranks)
        val blackPromo = ('2' in ranks && '1' in ranks)
        return whitePromo || blackPromo
    }

    private fun tryPromotionAsQueen(fen: String, a: String, b: String): Pair<String, String>? {
        if (!isPromotionSquarePair(a, b)) return null

        val uci1 = a + b + "q"
        ChessEngine.applyMove(fen, uci1)?.let { nextFen ->
            return nextFen to uci1
        }

        val uci2 = b + a + "q"
        ChessEngine.applyMove(fen, uci2)?.let { nextFen ->
            return nextFen to uci2
        }

        return null
    }

    private fun tryPair(fen: String, a: String, b: String): Pair<String, String>? {
        val m1 = a + b
        ChessEngine.applyMove(fen, m1)?.let { return it to m1 }
        val m2 = b + a
        ChessEngine.applyMove(fen, m2)?.let { return it to m2 }
        return null
    }

    private fun tryCastlingPair(fen: String, a: String, b: String): Pair<String, String>? {
        val pairSet = setOf(a, b)
        val candidateUci = when (pairSet) {
            setOf("e1", "h1") -> "e1g1"
            setOf("e1", "a1") -> "e1c1"
            setOf("e8", "h8") -> "e8g8"
            setOf("e8", "a8") -> "e8c8"
            else -> return null
        }
        val nextFen = ChessEngine.applyMove(fen, candidateUci) ?: return null
        return nextFen to candidateUci
    }

    fun isBoardResetDetected(): Boolean {
        return consecutiveEmptyCount >= 8
    }

    fun resetPending() {
        pendingKey = ""
        pendingCount = 0
    }

    fun reset() {
        resetPending()
        lastAcceptedKey = ""
        consecutiveEmptyCount = 0
        lastTimestamp = 0L
    }
}
