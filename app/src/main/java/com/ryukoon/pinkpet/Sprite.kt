package com.ryukoon.pinkpet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

enum class Form { NORMAL, POWER, CHICKEN }

/** 한 프레임의 자세. 좌표 단위는 24x24 도트 그리드. */
data class Pose(
    var hx: Int = 0, var hy: Int = 0, var by: Int = 12, var ly: Int = 18,
    var legs: String = "stand", var aL: String = "down", var aR: String = "down",
    var eyes: String = "open", var look: Int = 1, var mouth: String = "flat",
    var blush: Boolean = false, var lying: Boolean = false, var pot: Boolean = false,
    var eat: Boolean = false, var pen: String? = null, var box: Boolean = false,
    var sx: Float = 1f, var sy: Float = 1f, var rot: Float = 0f
)

/** 8bit 캐릭터를 24x24 비트맵에 그린다. */
object Sprite {
    private fun c(hex: String) = Color.parseColor(hex)

    private val HAIR = c("#1a1a20"); private val HAIR_HI = c("#3a3a46")
    private val SKIN = c("#f1c6a0"); private val SKIN_SH = c("#d49c78")
    private val EYE = c("#1c1b24"); private val LID = c("#5a3a30")
    private val MOUTH = c("#9a5040"); private val M_IN = c("#6b2230")
    private val SHIRT = c("#f0a38a"); private val SHIRT_SH = c("#d4826b"); private val SHIRT_HI = c("#fbc3ae")
    private val PANTS = c("#2b3d6b"); private val SHOE = c("#f4f4f4"); private val BLUSH = c("#f29a9a")
    private val PEN = c("#3b6fd8"); private val PEN_TIP = c("#e8e8e8"); private val PEN_CAP = c("#22222c")
    private val FEATHER = c("#f7ecd6"); private val FEATHER_SH = c("#e0cba6"); private val FEATHER_HI = c("#fffaf0")
    private val COMB = c("#e8312a"); private val BEAK = c("#f5c518"); private val BEAK_SH = c("#d99a0b")
    private val FOOT = c("#f2b705"); private val CLAW = c("#6b4a2b"); private val CH_LID = c("#b08a6a")
    private val BOX = c("#d93a2e"); private val BOX_IN = c("#fff6ee"); private val FRIED = c("#a8581e"); private val FRIED2 = c("#d88a34")
    private val POWER_A = c("#ff3d75"); private val POWER_B = c("#ff6f96"); private val POWER_HI = c("#ffe0ea")

    val bmp: Bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
    private val tmp: Bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
    private val bmpCanvas = Canvas(bmp)
    private val tmpCanvas = Canvas(tmp)
    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    private var cv: Canvas = bmpCanvas
    private var form = Form.NORMAL

    private fun px(x: Int, y: Int, w: Int, h: Int, col: Int) {
        paint.color = col
        cv.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
    }

    fun render(p: Pose, f: Form, nowMs: Long): Bitmap {
        form = f
        if (p.lying) {
            tmp.eraseColor(Color.TRANSPARENT)
            cv = tmpCanvas
            drawChar(p, nowMs)
            bmp.eraseColor(Color.TRANSPARENT)
            bmpCanvas.save()
            bmpCanvas.translate(24f, 2f)
            bmpCanvas.rotate(90f)
            bmpCanvas.drawBitmap(tmp, 0f, 0f, null)
            bmpCanvas.restore()
        } else {
            bmp.eraseColor(Color.TRANSPARENT)
            cv = bmpCanvas
            drawChar(p, nowMs)
        }
        return bmp
    }

    private fun drawChar(p: Pose, nowMs: Long) {
        cv.save()
        cv.translate(2f, 0f)
        val by = p.by
        legs(p.legs, p.ly)
        if (form == Form.CHICKEN) {
            px(1, by + 1, 4, 4, FEATHER); px(0, by - 1, 2, 3, FEATHER_HI); px(2, by, 2, 2, FEATHER_HI); px(1, by + 4, 3, 1, FEATHER_SH)
        }
        // 꽈배기 니트
        px(5, by, 11, 6, SHIRT); px(5, by + 5, 11, 1, SHIRT_SH); px(8, by, 5, 1, SHIRT_SH)
        for (r in 1..4) {
            px(if (r % 2 == 1) 7 else 6, by + r, 1, 1, SHIRT_HI)
            px(10, by + r, 1, 1, if (r % 2 == 1) SHIRT_HI else SHIRT_SH)
            px(if (r % 2 == 1) 13 else 14, by + r, 1, 1, SHIRT_HI)
        }
        arm(-1, p.aL, by); arm(1, p.aR, by)
        if (form == Form.CHICKEN) headChicken(p) else head(p, nowMs)
        if (form == Form.CHICKEN && p.box) {
            px(10, by + 1, 9, 5, BOX); px(13, by, 3, 1, BOX); px(14, by, 1, 1, BOX_IN); px(11, by + 2, 7, 3, BOX_IN)
            px(12, by + 3, 2, 1, FRIED); px(14, by + 3, 1, 1, FRIED2); px(15, by + 3, 2, 1, FRIED); px(13, by + 4, 3, 1, FRIED2); px(11, by + 2, 1, 1, BOX)
            px(9, by + 3, 2, 2, FEATHER_HI); px(18, by + 3, 1, 2, FEATHER_HI)
        }
        when (p.pen) {
            "up" -> { px(16, by - 9, 1, 4, PEN); px(16, by - 10, 1, 1, PEN_TIP); px(16, by - 6, 1, 1, PEN_CAP) }
            "out" -> { px(20, by - 3, 1, 4, PEN); px(20, by - 4, 1, 1, PEN_TIP) }
            "down" -> { px(20, by + 3, 1, 1, PEN); px(21, by + 4, 1, 1, PEN); px(21, by + 5, 1, 1, PEN_TIP) }
            "stab" -> { px(20, by + 1, 1, 1, PEN_TIP); px(19, by + 1, 1, 1, PEN) }
        }
        if (p.pot) {
            px(5, 16, 11, 1, c("#f6d978")); px(6, 16, 9, 1, c("#d8642c")); px(7, 16, 1, 1, c("#fbe9a0")); px(10, 16, 2, 1, c("#fbe9a0")); px(13, 16, 1, 1, c("#fbe9a0"))
            px(5, 17, 11, 4, c("#e0b54a")); px(5, 20, 11, 1, c("#b88a2a")); px(7, 18, 1, 1, c("#f6d978")); px(11, 19, 1, 1, c("#f6d978"))
            px(3, 17, 2, 1, c("#b88a2a")); px(16, 17, 2, 1, c("#b88a2a"))
        }
        if (p.eat) {
            px(10, by - 1, 1, 3, c("#fbe9a0")); px(16, by + 1, 2, 2, SHIRT); px(14, by - 1, 2, 2, SHIRT)
            px(12, by - 2, 2, 1, if (form == Form.CHICKEN) FEATHER_HI else SKIN); px(11, by - 1, 1, 3, c("#7a4a24"))
        }
        cv.restore()
    }

    private val LEGS = mapOf(
        "stand" to arrayOf(intArrayOf(6, 0, 3, 3), intArrayOf(12, 0, 3, 3)),
        "walkA" to arrayOf(intArrayOf(5, 0, 3, 3), intArrayOf(13, 0, 3, 3)),
        "walkB" to arrayOf(intArrayOf(7, 0, 3, 3), intArrayOf(11, 0, 3, 2)),
        "runA" to arrayOf(intArrayOf(5, 0, 3, 2), intArrayOf(13, 0, 3, 3)),
        "tuck" to arrayOf(intArrayOf(6, 0, 3, 2), intArrayOf(12, 0, 3, 2)),
        "dangle" to arrayOf(intArrayOf(7, 0, 3, 3), intArrayOf(11, 0, 3, 3)),
        "sit" to arrayOf(intArrayOf(5, 0, 4, 2), intArrayOf(12, 0, 4, 2))
    )
    private val SHOES = mapOf(
        "stand" to arrayOf(intArrayOf(5, 3, 4, 1), intArrayOf(12, 3, 4, 1)),
        "walkA" to arrayOf(intArrayOf(4, 3, 4, 1), intArrayOf(13, 3, 4, 1)),
        "walkB" to arrayOf(intArrayOf(7, 3, 4, 1), intArrayOf(11, 2, 4, 1)),
        "runA" to arrayOf(intArrayOf(3, 1, 3, 1), intArrayOf(13, 3, 4, 1)),
        "tuck" to arrayOf(intArrayOf(5, 2, 4, 1), intArrayOf(12, 2, 4, 1)),
        "dangle" to arrayOf(intArrayOf(7, 3, 3, 1), intArrayOf(11, 3, 3, 1)),
        "sit" to arrayOf(intArrayOf(3, 0, 2, 2), intArrayOf(16, 0, 2, 2))
    )

    private fun legs(t: String, ly: Int) {
        val ch = form == Form.CHICKEN
        val pc = if (ch) FOOT else PANTS
        val sc = if (ch) FOOT else SHOE
        for (r in LEGS[t] ?: LEGS.getValue("stand")) px(r[0] + (if (ch) 1 else 0), ly + r[1], if (ch) 1 else r[2], r[3], pc)
        for (r in SHOES[t] ?: SHOES.getValue("stand")) {
            px(r[0], ly + r[1], r[2], r[3], sc)
            if (ch) px(r[0] + r[2] - 1, ly + r[1] + r[3] - 1, 1, 1, CLAW)
        }
        if (ch && t != "sit") { px(5, ly, 11, 1, FEATHER); px(6, ly + 1, 9, 1, FEATHER_SH) }
    }

    private fun arm(side: Int, t: String, by: Int) {
        val hand = if (form == Form.CHICKEN) FEATHER_HI else SKIN
        fun r(x: Int, y: Int, w: Int, h: Int, col: Int) = if (side < 0) px(x, y, w, h, col) else px(21 - x - w, y, w, h, col)
        when (t) {
            "down" -> { r(3, by + 1, 2, 4, SHIRT); r(3, by + 5, 2, 1, hand) }
            "up" -> { r(3, by - 4, 2, 5, SHIRT); r(3, by - 5, 2, 1, hand) }
            "out" -> { r(1, by + 1, 3, 2, SHIRT); r(0, by + 1, 1, 2, hand) }
            "wave" -> { r(2, by - 3, 2, 4, SHIRT); r(1, by - 4, 2, 1, hand) }
            "hip" -> { r(3, by + 1, 2, 2, SHIRT); r(4, by + 3, 1, 1, hand) }
        }
    }

    private fun head(p: Pose, nowMs: Long) {
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        val pw = form == Form.POWER
        val hr = if (pw) (if ((nowMs / 90) % 2 == 0L) POWER_A else POWER_B) else HAIR
        val hh = if (pw) POWER_HI else HAIR_HI
        h(6, 1, 9, 1, hr); h(4, 2, 13, 3, hr); h(9, 2, 5, 1, hh); h(7, 2, 1, 2, hh)
        if (pw) {
            for (sx in intArrayOf(5, 8, 11, 14)) h(sx, 0, 1, 1, hr)
            h(3, 2, 1, 2, hr); h(17, 2, 1, 2, hr); h(2, 1, 1, 1, hr); h(18, 1, 1, 1, hr)
        }
        h(5, 5, 11, 6, SKIN); h(6, 11, 9, 1, SKIN); h(5, 10, 1, 1, SKIN_SH); h(15, 10, 1, 1, SKIN_SH)
        h(4, 5, 1, 2, hr); h(16, 5, 1, 1, hr); h(8, 5, 6, 1, hr); h(14, 5, 1, 1, hh)
        h(4, 7, 1, 2, SKIN); h(16, 7, 1, 2, SKIN); h(4, 8, 1, 1, SKIN_SH); h(16, 8, 1, 1, SKIN_SH)
        h(7, 6, 2, 1, HAIR); h(12, 6, 2, 1, HAIR)
        if (pw) { h(9, 7, 1, 1, HAIR); h(11, 7, 1, 1, HAIR) }
        if (p.eyes == "closed") { h(7, 7, 2, 1, LID); h(12, 7, 2, 1, LID) }
        else {
            val l = if (p.look < 0) 0 else 1
            h(7 + l, 7, 1, 1, EYE); h(12 + l, 7, 1, 1, EYE)
            if (p.eyes == "wide") { h(7 + l, 8, 1, 1, EYE); h(12 + l, 8, 1, 1, EYE); h(7, 5, 2, 1, HAIR); h(12, 5, 2, 1, HAIR) }
        }
        h(10, 8, 1, 2, SKIN_SH)
        when (p.mouth) {
            "smile" -> { h(9, 10, 3, 1, MOUTH); h(8, 9, 1, 1, MOUTH); h(12, 9, 1, 1, MOUTH) }
            "open" -> h(9, 9, 3, 2, M_IN)
            "o" -> h(10, 9, 1, 2, M_IN)
            else -> h(9, 10, 3, 1, MOUTH)
        }
        if (p.blush) { h(6, 9, 1, 1, BLUSH); h(14, 9, 1, 1, BLUSH) }
    }

    private fun headChicken(p: Pose) {
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        h(8, 0, 2, 1, COMB); h(11, 0, 2, 1, COMB); h(7, 1, 8, 1, COMB); h(14, 0, 1, 1, COMB)
        h(4, 2, 13, 3, HAIR); h(8, 2, 4, 1, HAIR_HI); h(3, 3, 1, 2, HAIR); h(17, 3, 1, 2, HAIR)
        h(4, 5, 13, 6, FEATHER); h(5, 11, 11, 1, FEATHER); h(3, 7, 1, 3, FEATHER_HI); h(17, 7, 1, 3, FEATHER_HI)
        h(4, 10, 1, 1, FEATHER_SH); h(16, 10, 1, 1, FEATHER_SH)
        h(4, 5, 2, 1, HAIR); h(15, 5, 2, 1, HAIR)
        h(7, 6, 2, 1, HAIR); h(12, 6, 3, 1, HAIR)
        val l = if (p.look < 0) 0 else 1
        if (p.eyes == "closed") { h(7, 7, 2, 1, LID); h(12, 7, 2, 1, LID) }
        else {
            h(7, 7, 2, 1, CH_LID); h(12, 7, 2, 1, CH_LID); h(7 + l, 8, 1, 1, EYE); h(12 + l, 8, 1, 1, EYE)
            if (p.eyes == "wide") { h(7 + l, 7, 1, 1, EYE); h(12 + l, 7, 1, 1, EYE) }
        }
        h(9, 8, 4, 1, BEAK); h(13, 8, 1, 1, BEAK_SH)
        val open = p.mouth == "open" || p.mouth == "o"
        if (open) { h(10, 9, 3, 1, M_IN); h(10, 10, 3, 1, BEAK_SH) } else h(10, 9, 3, 1, BEAK_SH)
        h(10, if (open) 11 else 10, 2, 2, COMB); h(12, 10, 1, 1, COMB)
        h(6, 9, 2, 1, BLUSH); h(14, 9, 2, 1, BLUSH)
    }
}
