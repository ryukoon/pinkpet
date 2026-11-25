package com.ryukoon.pinkpet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** 여유곰: 곰 귀, 검은 머리, 동그란 안경, 검정 패딩 */
object BearSprite {
    private fun c(hex: String) = Color.parseColor(hex)

    private val FUR = c("#b9783f"); private val FUR_SH = c("#9a5f2e"); private val EAR_IN = c("#e3b07a")
    private val MUZ = c("#f1ddb8"); private val NOSE = c("#3a2420"); private val MOUTH = c("#7a4a3a")
    private val HAIR = c("#1a1a20"); private val HAIR_HI = c("#3a3a46")
    private val GLASS = c("#2a2a30"); private val LENS = c("#e3f0fb"); private val EYE = c("#1c1b24"); private val LID = c("#6a4020")
    private val JACKET = c("#2c2c34"); private val JACKET_LN = c("#33333c"); private val JACKET_HI = c("#3e3e4a"); private val ZIP = c("#5c5c6a")
    private val PANTS = c("#3a3f58"); private val SHOE = c("#4a3426"); private val PAD = c("#f2a0a8")
    private val M_IN = c("#6b2230"); private val BLUSH = c("#f29a9a"); private val SPARK = c("#ffe45e"); private val TEAR = c("#5bc0ff"); private val TONGUE = c("#ff7a9a")

    val bmp: Bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
    private val tmp: Bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
    private val bmpCanvas = Canvas(bmp)
    private val tmpCanvas = Canvas(tmp)
    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    private var cv: Canvas = bmpCanvas

    private fun px(x: Int, y: Int, w: Int, h: Int, col: Int) {
        paint.color = col
        cv.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
    }

    fun render(p: Pose, f: Form, nowMs: Long): Bitmap {
        if (p.lying) {
            tmp.eraseColor(Color.TRANSPARENT); cv = tmpCanvas; drawChar(p)
            bmp.eraseColor(Color.TRANSPARENT)
            bmpCanvas.save(); bmpCanvas.translate(24f, 2f); bmpCanvas.rotate(90f); bmpCanvas.drawBitmap(tmp, 0f, 0f, null); bmpCanvas.restore()
        } else {
            bmp.eraseColor(Color.TRANSPARENT); cv = bmpCanvas; drawChar(p)
        }
        return bmp
    }

    private fun drawChar(p: Pose) {
        cv.save(); cv.translate(2f, 0f)
        val by = p.by
        if (p.prop == "bike") bike()
        legs(p.legs, p.ly)
        // 검정 패딩
        px(4, by, 13, 6, JACKET)
        px(4, by + 2, 13, 1, JACKET_LN); px(4, by + 4, 13, 1, JACKET_LN); px(4, by, 1, 1, Color.TRANSPARENT); px(16, by, 1, 1, Color.TRANSPARENT)
        px(6, by, 9, 1, JACKET_HI); px(8, by, 5, 1, FUR); px(10, by, 1, 6, ZIP)
        px(6, by + 2, 1, 2, ZIP); px(14, by + 2, 1, 2, ZIP)
        arm(-1, p.aL, by); arm(1, p.aR, by)
        head(p)
        if (p.aR == "paw") { px(15, by - 3, 2, 2, FUR); px(16, by - 3, 1, 1, PAD) }
        drawProp(p, by)
        cv.restore()
    }

    private val LEGS = mapOf(
        "stand" to arrayOf(intArrayOf(6, 0, 3, 3), intArrayOf(12, 0, 3, 3)),
        "walkA" to arrayOf(intArrayOf(5, 0, 3, 3), intArrayOf(13, 0, 3, 3)),
        "walkB" to arrayOf(intArrayOf(7, 0, 3, 3), intArrayOf(11, 0, 3, 2)),
        "runA" to arrayOf(intArrayOf(5, 0, 3, 2), intArrayOf(13, 0, 3, 3)),
        "tuck" to arrayOf(intArrayOf(6, 0, 3, 2), intArrayOf(12, 0, 3, 2)),
        "dangle" to arrayOf(intArrayOf(7, 0, 3, 3), intArrayOf(11, 0, 3, 3)),
        "sit" to arrayOf(intArrayOf(5, 0, 4, 2), intArrayOf(12, 0, 4, 2)),
        "kick" to arrayOf(intArrayOf(6, 0, 3, 3), intArrayOf(13, -1, 3, 2))
    )
    private val SHOES = mapOf(
        "stand" to arrayOf(intArrayOf(5, 3, 4, 1), intArrayOf(12, 3, 4, 1)),
        "walkA" to arrayOf(intArrayOf(4, 3, 4, 1), intArrayOf(13, 3, 4, 1)),
        "walkB" to arrayOf(intArrayOf(7, 3, 4, 1), intArrayOf(11, 2, 4, 1)),
        "runA" to arrayOf(intArrayOf(3, 1, 3, 1), intArrayOf(13, 3, 4, 1)),
        "tuck" to arrayOf(intArrayOf(5, 2, 4, 1), intArrayOf(12, 2, 4, 1)),
        "dangle" to arrayOf(intArrayOf(7, 3, 3, 1), intArrayOf(11, 3, 3, 1)),
        "sit" to arrayOf(intArrayOf(3, 0, 2, 2), intArrayOf(16, 0, 2, 2)),
        "kick" to arrayOf(intArrayOf(5, 3, 4, 1), intArrayOf(16, -2, 2, 3))
    )

    private fun legs(t: String, ly: Int) {
        for (r in LEGS[t] ?: LEGS.getValue("stand")) px(r[0], ly + r[1], r[2], r[3], PANTS)
        for (r in SHOES[t] ?: SHOES.getValue("stand")) px(r[0], ly + r[1], r[2], r[3], SHOE)
    }

    private fun arm(side: Int, t: String, by: Int) {
        fun r(x: Int, y: Int, w: Int, h: Int, col: Int) = if (side < 0) px(x, y, w, h, col) else px(21 - x - w, y, w, h, col)
        when (t) {
            "down" -> { r(2, by + 1, 2, 4, JACKET); r(2, by + 2, 1, 2, JACKET_LN) } // 주머니에 손
            "paws" -> { r(2, by + 1, 2, 4, JACKET); r(2, by + 5, 2, 1, FUR) }
            "up" -> { r(2, by - 4, 2, 5, JACKET); r(2, by - 5, 2, 1, FUR) }
            "out" -> { r(0, by + 1, 3, 2, JACKET); r(-1, by + 1, 1, 2, FUR) }
            "wave" -> { r(1, by - 3, 2, 4, JACKET); r(0, by - 4, 2, 1, FUR); r(0, by - 4, 1, 1, PAD) }
            "hip" -> { r(2, by + 1, 2, 2, JACKET); r(3, by + 3, 1, 1, FUR) }
            "hold" -> { r(2, by + 1, 2, 2, JACKET); r(3, by + 3, 3, 1, JACKET); r(6, by + 2, 1, 1, FUR) }
            "paw" -> { r(2, by - 1, 2, 2, JACKET); r(3, by - 2, 1, 1, JACKET) }
        }
    }

    private fun head(p: Pose) {
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        // 귀
        // 동그란 곰 귀
        h(3, 1, 4, 3, FUR); h(4, 0, 2, 1, FUR); h(4, 1, 2, 2, EAR_IN)
        h(14, 1, 4, 3, FUR); h(15, 0, 2, 1, FUR); h(15, 1, 2, 2, EAR_IN)
        // 둥근 얼굴 털
        h(5, 3, 11, 1, FUR); h(4, 4, 13, 7, FUR); h(5, 11, 11, 1, FUR); h(3, 5, 1, 5, FUR); h(17, 5, 1, 5, FUR)
        h(3, 9, 1, 1, FUR_SH); h(17, 9, 1, 1, FUR_SH); h(4, 10, 1, 1, FUR_SH); h(16, 10, 1, 1, FUR_SH)
        // 부스스한 검은 머리 (정수리)
        h(7, 0, 1, 1, HAIR); h(10, 0, 1, 1, HAIR); h(13, 0, 1, 1, HAIR)
        h(6, 1, 9, 2, HAIR); h(6, 3, 2, 1, HAIR); h(9, 3, 3, 1, HAIR); h(13, 3, 2, 1, HAIR); h(8, 1, 3, 1, HAIR_HI)
        h(10, 4, 1, 1, HAIR)
        // 큰 주둥이 + 코
        h(8, 8, 5, 1, MUZ); h(7, 9, 7, 2, MUZ); h(8, 11, 5, 1, MUZ)
        h(9, 8, 3, 1, NOSE); h(10, 9, 1, 1, NOSE)
        // 동그란 얇은 안경
        for (gx in intArrayOf(6, 11)) {
            h(gx + 1, 5, 2, 1, GLASS); h(gx + 1, 7, 2, 1, GLASS); h(gx, 6, 1, 1, GLASS); h(gx + 3, 6, 1, 1, GLASS); h(gx + 1, 6, 2, 1, LENS)
        }
        h(10, 6, 1, 1, GLASS)
        val l = if (p.look < 0) 0 else 1
        when (p.eyes) {
            "closed" -> { h(7, 6, 2, 1, LID); h(12, 6, 2, 1, LID) }
            "wink" -> { h(7 + l, 6, 1, 1, EYE); h(12, 6, 2, 1, LID) }
            "sparkle" -> { h(7 + l, 6, 1, 1, SPARK); h(12 + l, 6, 1, 1, SPARK) }
            "angry" -> { h(7 + l, 6, 1, 1, EYE); h(12 + l, 6, 1, 1, EYE); h(8, 4, 2, 1, HAIR); h(11, 4, 2, 1, HAIR) }
            "wide" -> { h(7, 6, 2, 1, EYE); h(12, 6, 2, 1, EYE) }
            else -> { h(7 + l, 6, 1, 1, EYE); h(12 + l, 6, 1, 1, EYE) }
        }
        if (p.eyes == "tear") { h(7 + l, 8, 1, 2, TEAR); h(13, 8, 1, 2, TEAR) }
        when (p.mouth) {
            "smile" -> { h(8, 9, 1, 1, MOUTH); h(12, 9, 1, 1, MOUTH); h(9, 10, 3, 1, MOUTH) }
            "open" -> h(9, 10, 3, 2, M_IN)
            "o" -> h(10, 10, 1, 2, M_IN)
            "grin" -> { h(8, 10, 5, 2, M_IN); h(9, 10, 3, 1, Color.WHITE) }
            "tongue" -> { h(9, 10, 3, 1, M_IN); h(10, 11, 1, 1, TONGUE) }
            else -> { h(9, 10, 1, 1, MOUTH); h(11, 10, 1, 1, MOUTH) }
        }
        if (p.blush) { h(5, 9, 2, 1, BLUSH); h(14, 9, 2, 1, BLUSH) }
        if (p.prop == "santa") { h(5, 0, 11, 2, c("#e8312a")); h(4, 2, 13, 1, Color.WHITE); h(15, -1, 2, 2, Color.WHITE) }
        if (p.prop == "headphones") { h(5, 0, 11, 1, c("#333344")); h(3, 5, 2, 3, c("#ffb000")); h(16, 5, 2, 3, c("#ffb000")) }
    }

    private fun bike() {
        val frame = c("#3b6fd8"); val dark = c("#22222c")
        for (cx in intArrayOf(3, 16)) { px(cx - 2, 17, 5, 1, dark); px(cx - 2, 21, 5, 1, dark); px(cx - 3, 18, 1, 3, dark); px(cx + 2, 18, 1, 3, dark); px(cx, 19, 1, 1, c("#999999")) }
        px(3, 19, 13, 1, frame); px(9, 16, 1, 3, frame); px(15, 14, 1, 5, frame); px(14, 13, 3, 1, dark); px(7, 16, 4, 1, dark)
    }

    private fun drawProp(p: Pose, by: Int) {
        val pr = p.prop ?: return
        val (ax, ay) = when (p.aR) {
            "up" -> 17 to by - 5; "out" -> 20 to by + 1; "wave" -> 19 to by - 4
            "hip" -> 17 to by + 3; "hold" -> 14 to by + 2; "paw" -> 15 to by - 3; else -> 17 to by + 5
        }
        val dark = c("#22222c"); val white = Color.WHITE
        when (pr) {
            "phone" -> { px(ax, ay - 2, 2, 3, dark); px(ax, ay - 2, 2, 2, c("#6fd3ff")) }
            "coffee" -> { px(ax - 1, ay - 2, 3, 3, white); px(ax - 1, ay - 2, 3, 1, c("#6b3e1e")); px(ax + 2, ay - 1, 1, 1, white) }
            "book" -> { px(ax - 3, ay - 2, 5, 4, c("#3b6fd8")); px(ax - 2, ay - 1, 3, 2, white) }
            "mic" -> { px(ax, ay - 3, 2, 2, c("#666677")); px(ax, ay - 1, 1, 2, dark) }
            "drumstick" -> { px(ax - 1, ay - 2, 3, 2, c("#c8782e")); px(ax + 1, ay, 1, 1, c("#a8581e")); px(ax + 2, ay + 1, 1, 1, white) }
            "icecream" -> { px(ax, ay - 3, 2, 2, c("#ffb6c8")); px(ax, ay - 1, 2, 1, c("#d9a24a")) }
            "balloon" -> { px(ax, ay - 6, 1, 6, c("#dddddd")); px(ax - 1, ay - 9, 3, 3, c("#ff4f7a")); px(ax - 1, ay - 9, 1, 1, c("#ffc0d0")) }
            "camera" -> { px(ax - 2, ay - 2, 4, 3, c("#333344")); px(ax - 1, ay - 1, 2, 1, c("#6fd3ff")); px(ax + 1, ay - 3, 1, 1, c("#333344")) }
            "flower" -> { px(ax, ay - 1, 1, 3, c("#3a9a4a")); px(ax - 1, ay - 3, 3, 2, c("#ff6f96")); px(ax, ay - 3, 1, 1, SPARK) }
            "heart" -> { val hc = c("#ff4f7a"); px(ax - 2, ay - 4, 1, 1, hc); px(ax, ay - 4, 1, 1, hc); px(ax - 2, ay - 3, 3, 1, hc); px(ax - 1, ay - 2, 1, 1, hc) }
            "dumbbell" -> { px(0, by - 6, 22, 1, c("#777788")); px(-1, by - 8, 2, 4, c("#333344")); px(20, by - 8, 2, 4, c("#333344")) }
            "dumbbellDown" -> { px(0, by + 5, 21, 1, c("#777788")); px(-1, by + 3, 2, 4, c("#333344")); px(20, by + 3, 2, 4, c("#333344")) }
            "guitar" -> { px(9, by + 2, 6, 4, c("#b5651d")); px(10, by + 1, 4, 1, c("#b5651d")); px(11, by + 3, 2, 2, c("#3a2410")); px(15, by + 2, 5, 1, c("#5a3a1e")); px(20, by + 1, 1, 2, c("#3a2410")) }
            "laptop" -> { px(4, 19, 12, 1, c("#bfbfc8")); px(5, 15, 10, 4, c("#3a3a46")); px(6, 16, 8, 2, c("#6fd3ff")) }
            "honey" -> { px(ax - 2, ay - 2, 4, 3, c("#d9a24a")); px(ax - 2, ay - 3, 4, 1, c("#f5c518")); px(ax - 1, ay - 1, 2, 1, c("#fff1b0")); px(ax + 1, ay + 1, 1, 1, c("#f5c518")) }
            "salmon" -> { px(ax - 3, ay - 1, 6, 2, c("#f08a6a")); px(ax + 3, ay - 2, 1, 4, c("#e07050")); px(ax - 3, ay - 1, 1, 1, dark); px(ax - 1, ay, 3, 1, c("#ffc0a8")) }
            "berry" -> { px(ax - 1, ay - 2, 2, 2, c("#d93a4e")); px(ax, ay - 3, 1, 1, c("#3a9a4a")); px(ax - 1, ay - 2, 1, 1, c("#ff8a9a")) }
            "snowball" -> { px(ax - 1, ay - 2, 3, 3, c("#f4f8ff")); px(ax + 1, ay, 1, 1, c("#cfe0f5")) }
            "lunchbox" -> { px(ax - 3, ay - 1, 6, 3, c("#e8312a")); px(ax - 3, ay - 1, 6, 1, c("#ffb0a0")) }
            "cupramen" -> { px(ax - 1, ay - 3, 3, 3, white); px(ax - 1, ay - 2, 3, 1, c("#e8312a")); px(ax - 1, ay - 3, 3, 1, c("#f5c518")) }
            "pillow" -> { px(10, 18, 8, 3, white); px(11, 19, 6, 1, c("#dde6f5")) }
        }
    }
}
