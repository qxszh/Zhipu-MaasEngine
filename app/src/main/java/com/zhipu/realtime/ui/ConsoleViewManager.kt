package com.zhipu.realtime.ui

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceView
import java.util.concurrent.ConcurrentLinkedQueue

data class ColoredText(val text: String, val color: Int)

class ConsoleViewManager(
    private val surfaceView: SurfaceView, private val lineHeight: Float = 30f
) {
    private val textQueue: ConcurrentLinkedQueue<ColoredText> = ConcurrentLinkedQueue()
    private val displayedTexts = mutableListOf<ColoredText>()
    private val handler = Handler(Looper.getMainLooper())
    private var scrollOffset = 0f

    fun startScrolling() {
        initScrollingTextMockData()
        initScrollingText()
    }

    fun stopScrolling() {
        handler.removeCallbacksAndMessages(null)
    }

    fun appendText(text: String, color: Int = Color.WHITE) {
        if (!surfaceView.isAttachedToWindow) {
            Log.w("ConsoleViewManager", "SurfaceView is not attached to window.")
            return
        }
        textQueue.offer(ColoredText(text, color))
    }

    private fun initScrollingTextMockData() {
        Thread {
            var count = 1
            while (true) {
                while (true) {
                    val message = "----- SEPARATOR AT ${count * 10} SECONDS -----"
                    textQueue.offer(ColoredText(message, Color.GRAY))
                    count++
                    Thread.sleep(10000)
                }
            }
        }.start()
    }

    private fun initScrollingText() {
        val updateRunnable = object : Runnable {
            override fun run() {
                val newText = textQueue.poll()
                if (newText != null) {
                    // Only add the new text if there is enough space for one more line
                    if ((displayedTexts.size + 1) * lineHeight - scrollOffset > surfaceView.height) {
                        // Space is not enough, remove the top line
                        if (displayedTexts.isNotEmpty()) {
                            displayedTexts.removeAt(0)
                            scrollOffset -= lineHeight
                        }
                    }
                    displayedTexts.add(newText)
                }
                drawTextOnSurfaceView()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(updateRunnable)
    }

    private fun drawTextOnSurfaceView() {
        val surfaceHolder = surfaceView.holder
        val canvas = surfaceHolder.lockCanvas()
        if (canvas != null) {
            try {
                canvas.drawColor(Color.BLACK)

                var y = lineHeight - scrollOffset
                displayedTexts.forEach { coloredText ->
                    val paint = Paint().apply {
                        this.color = coloredText.color
                        textSize = 20f
                        typeface = Typeface.MONOSPACE
                    }
                    canvas.drawText(coloredText.text, 10f, y, paint)
                    y += lineHeight
                }

                // Move text up by incrementing the scroll offset
                if (displayedTexts.size * lineHeight > surfaceView.height) {
                    // Shift offset to move the display upward
                    scrollOffset = lineHeight
                } else {
                    // Reset offset if not overflowing
                    scrollOffset = 0f
                }
            } finally {
                surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }
    }
}