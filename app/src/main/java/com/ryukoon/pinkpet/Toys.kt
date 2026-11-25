package com.ryukoon.pinkpet

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** 화면에 놓는 놀이 소품 (위치는 화면 좌표, x=가운데, y=바닥) */
class Toy(val type: String) {
    var homeX = 0f
    var x = 0f
    var y = 0f
    var wPx = 0f
    var hPx = 0f
    var rider: Engine? = null
    var dragging = false
}

object Toys {
    val types = listOf(
        "ladder" to "🪜 사다리", "slide" to "🛝 미끄럼틀", "drone" to "🚁 드론", "cloud" to "☁️ 근두운",
        "plant" to "🪴 난 화분", "tree" to "🌳 나무", "pool" to "🏊 수영장", "gym" to "🏟 체육관"
    )
    /** 도트 그리드 크기 (가로, 세로) */
    fun size(type: String): Pair<Int, Int> = when (type) {
        "ladder" -> 10 to 60
        "slide" -> 52 to 40
        "drone" -> 18 to 8
        "cloud" -> 24 to 10
        "plant" -> 12 to 20
        "tree" -> 34 to 50
        "pool" -> 80 to 16
        else -> 80 to 50
    }

    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    private fun c(hex: String) = Color.parseColor(hex)
    private lateinit var cv: Canvas
    private fun px(x: Int, y: Int, w: Int, h: Int, col: Int) {
        paint.color = col; cv.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
    }

    /** 그리드 좌표로 소품 그리기 */
    fun draw(canvas: Canvas, type: String, clock: Float) {
        cv = canvas
        when (type) {
            "ladder" -> {
                val rail = c("#8a5a2b"); val rung = c("#c08a4a")
                px(1, 0, 1, 60, rail); px(8, 0, 1, 60, rail); px(0, 0, 1, 60, c("#6a4020")); px(9, 0, 1, 60, c("#6a4020"))
                var y = 2; while (y < 60) { px(2, y, 6, 1, rung); y += 5 }
            }
            "slide" -> {
                val rail = c("#8a5a2b"); val rung = c("#c08a4a")
                px(1, 2, 1, 38, rail); px(6, 2, 1, 38, rail)
                var y = 5; while (y < 40) { px(2, y, 4, 1, rung); y += 5 }
                px(0, 0, 13, 3, c("#e8312a")); px(0, 0, 13, 1, c("#ff6a5a"))
                for (i in 0..40) {
                    val yy = 2 + i * 36 / 40
                    px(10 + i, yy, 3, 2, c("#ffcc33")); px(10 + i, yy + 2, 3, 1, c("#d89a10"))
                }
                px(28, 20, 1, 20, c("#8a8a96")); px(42, 31, 1, 9, c("#8a8a96"))
            }
            "drone" -> {
                val spin = ((clock * 20).toInt() % 2 == 0)
                px(1, 2, 16, 1, c("#55555f"))
                if (spin) { px(0, 1, 5, 1, c("#bdbdc8")); px(13, 1, 5, 1, c("#bdbdc8")) } else { px(1, 1, 3, 1, c("#e0e0ea")); px(14, 1, 3, 1, c("#e0e0ea")) }
                px(2, 2, 1, 1, c("#333333")); px(15, 2, 1, 1, c("#333333"))
                px(6, 3, 6, 3, c("#33343c")); px(7, 3, 4, 1, c("#4a4b56"))
                px(8, 5, 2, 1, if ((clock * 3).toInt() % 2 == 0) c("#e8312a") else c("#6a1010"))
                px(6, 6, 1, 2, c("#33343c")); px(11, 6, 1, 2, c("#33343c"))
            }
            "cloud" -> {
                val y1 = c("#ffd24a"); val y2 = c("#ffe98a"); val y3 = c("#e8a820")
                px(4, 4, 16, 5, y1); px(2, 5, 2, 3, y1); px(20, 5, 2, 3, y1); px(6, 2, 5, 3, y1); px(12, 1, 6, 4, y1)
                px(13, 2, 3, 1, c("#fff6c8")); px(7, 3, 2, 1, y2); px(4, 8, 16, 1, y3)
                val w = ((clock * 4).toInt() % 2)
                px(0, 6 + w, 2, 1, y2); px(22, 7 - w, 2, 1, y2)
            }
            "plant" -> {
                val leaf = c("#2f8a4a"); val leaf2 = c("#3fae5a")
                px(3, 14, 6, 6, c("#c8935a")); px(2, 13, 8, 2, c("#a87038")); px(4, 16, 4, 1, c("#e0b080"))
                px(5, 6, 1, 7, leaf); px(6, 4, 1, 9, leaf2); px(4, 8, 1, 2, leaf); px(3, 10, 1, 2, leaf); px(7, 7, 1, 2, leaf2); px(8, 9, 1, 3, leaf2)
                px(7, 2, 2, 2, c("#ff9ad8")); px(9, 4, 2, 2, c("#ffffff")); px(8, 3, 1, 1, c("#ffd24a")); px(10, 5, 1, 1, c("#ff9ad8"))
            }
            "tree" -> {
                px(14, 28, 6, 22, c("#7a4a24")); px(15, 30, 1, 18, c("#9a6234")); px(11, 48, 12, 2, c("#6a3a18"))
                px(4, 8, 26, 18, c("#3a9a4a")); px(8, 2, 18, 8, c("#3a9a4a")); px(2, 14, 30, 10, c("#2f8a40"))
                px(10, 5, 6, 3, c("#5cc06a")); px(5, 12, 5, 3, c("#5cc06a")); px(22, 9, 4, 3, c("#5cc06a")); px(6, 24, 22, 2, c("#2a7a38"))
                px(9, 16, 2, 2, c("#e8312a")); px(24, 18, 2, 2, c("#e8312a")); px(17, 11, 2, 2, c("#e8312a"))
            }
            "pool" -> {
                px(0, 4, 80, 12, c("#d8d8e4")); px(2, 6, 76, 9, c("#3ab0f0")); px(2, 6, 76, 1, c("#8ad8ff"))
                var x = 4; while (x < 76) { px(x, 10, 3, 1, c("#ffffff")); x += 6 }
                px(4, 0, 1, 6, c("#b0b0bc")); px(7, 0, 1, 6, c("#b0b0bc")); px(4, 1, 4, 1, c("#b0b0bc")); px(4, 3, 4, 1, c("#b0b0bc"))
                val w = ((clock * 3).toInt() % 3)
                px(20 + w * 4, 8, 4, 1, c("#ffffff")); px(52 - w * 3, 12, 4, 1, c("#ffffff"))
            }
            "gym" -> {
                px(0, 12, 80, 38, c("#e8e0d0")); px(-0, 6, 80, 7, c("#3b6fd8")); px(4, 3, 72, 4, c("#2a52be"))
                px(26, 14, 28, 8, c("#ffffff")); px(28, 16, 4, 4, c("#e8792a")); px(29, 16, 1, 4, c("#5a2a10"))
                px(34, 16, 2, 4, c("#2a52be")); px(37, 16, 2, 1, c("#2a52be")); px(37, 19, 2, 1, c("#2a52be")); px(41, 16, 2, 4, c("#e8312a")); px(45, 16, 6, 1, c("#ffcc33")); px(47, 16, 2, 4, c("#ffcc33"))
                px(34, 34, 12, 16, c("#7a5030")); px(39, 34, 1, 16, c("#5a3818"))
                px(6, 24, 16, 9, c("#9ad0ff")); px(58, 24, 16, 9, c("#9ad0ff")); px(13, 24, 1, 9, c("#ffffff")); px(65, 24, 1, 9, c("#ffffff"))
                px(0, 48, 80, 2, c("#b0a890"))
            }
        }
    }
}
