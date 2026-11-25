package com.bot.chessoverlay

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

object GameController {

    private val main = Handler(Looper.getMainLooper())
    private val engineExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var searchVersion = 0

    @Volatile
    private var activeTask: Future<*>? = null

    @Volatile
    private var activeCancelled = AtomicBoolean(false)

    fun isMyTurn(): Boolean {
        val parts = GameState.fen.split(" ")
        val whiteToMove = parts.size > 1 && parts[1] == "w"
        return whiteToMove == GameState.myIsWhite
    }

    fun notifyChange() {
        OverlayService.instance?.sync()
        GameState.onChange?.invoke()
    }

    private fun cancelCurrentSearch() {
        searchVersion++
        activeCancelled.set(true)
        activeTask?.cancel(false)
        activeTask = null
    }

    fun markOutOfSync(moveCandidate: String) {
        cancelCurrentSearch()
        GameState.syncError = true
        GameState.suggestion = ""
        GameState.statusMsg = "Posisi tidak sinkron ($moveCandidate) — perlu sinkronisasi ulang."
        notifyChange()
    }

    fun resetSync() {
        cancelCurrentSearch()
        GameState.syncError = false
        GameState.suggestion = ""
        OverlayService.instance?.resetSynchronizerOnly()
        recompute()
    }

    fun startTrackingFromInitialPosition() {
        cancelCurrentSearch()
        GameState.fen = GameState.START_FEN
        GameState.history.clear()
        GameState.suggestion = ""
        GameState.lastDetectedMove = ""
        GameState.syncError = false
        GameState.trackingEnabled = true

        OverlayService.instance?.resetTracking()
        recompute()
    }

    fun stopTracking() {
        cancelCurrentSearch()
        GameState.trackingEnabled = false
        GameState.suggestion = ""
        GameState.syncError = false
        GameState.statusMsg = "Pelacakan dihentikan"
        OverlayService.instance?.resetTracking()
        notifyChange()
    }

    fun commit(newFen: String) {
        cancelCurrentSearch()
        GameState.history.add(GameState.fen)
        GameState.fen = newFen
        GameState.suggestion = ""
        GameState.syncError = false
        recompute()
    }

    fun undo(): Boolean {
        if (GameState.history.isEmpty()) return false
        cancelCurrentSearch()
        GameState.fen = GameState.history.removeAt(GameState.history.size - 1)
        GameState.suggestion = ""
        GameState.syncError = false
        OverlayService.instance?.resetTracking()
        recompute()
        return true
    }

    fun resetGame() {
        cancelCurrentSearch()
        GameState.fen = GameState.START_FEN
        GameState.history.clear()
        GameState.suggestion = ""
        GameState.lastDetectedMove = ""
        GameState.syncError = false
        OverlayService.instance?.resetTracking()
        recompute()
    }

    // Harus dipanggil dari thread utama.
    fun recompute() {
        if (GameState.syncError) {
            GameState.suggestion = ""
            notifyChange()
            return
        }

        cancelCurrentSearch()
        val fenNow = GameState.fen
        val version = searchVersion
        val cancelledFlag = AtomicBoolean(false)
        activeCancelled = cancelledFlag

        val end = try {
            ChessEngine.statusText(fenNow)
        } catch (e: Exception) {
            "Posisi rusak, tekan Partai baru"
        }
        if (end.isNotEmpty()) {
            GameState.statusMsg = end
            GameState.suggestion = ""
            notifyChange()
            return
        }
        if (!isMyTurn()) {
            val lastInfo = if (GameState.lastDetectedMove.isNotEmpty()) {
                " (terakhir: ${GameState.lastDetectedMove})"
            } else ""
            GameState.statusMsg = "Giliran lawan...$lastInfo"
            GameState.suggestion = ""
            notifyChange()
            return
        }

        GameState.statusMsg = "Menghitung..."
        GameState.suggestion = ""
        notifyChange()

        activeTask = engineExecutor.submit {
            if (cancelledFlag.get() || Thread.currentThread().isInterrupted) return@submit
            val move = try {
                ChessEngine.bestMoveRaw(fenNow, depth = 3)
            } catch (e: Exception) {
                ""
            }
            if (cancelledFlag.get() || Thread.currentThread().isInterrupted) return@submit

            main.post {
                if (cancelledFlag.get()) return@post
                if (version != searchVersion) return@post
                if (GameState.fen != fenNow || GameState.syncError) return@post

                GameState.suggestion = move
                GameState.statusMsg = if (move.isNotEmpty()) {
                    "Saran langkah: $move"
                } else {
                    "Tidak ada langkah tersedia"
                }
                notifyChange()
            }
        }
    }

    fun shutdown() {
        cancelCurrentSearch()
        engineExecutor.shutdownNow()
    }
}
