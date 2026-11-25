package com.ryukoon.pinkpet

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/** 수아네 캐릭터 공통 그리기 도구 */
abstract class PixelSprite {
    protected fun c(hex: String) = Color.parseColor(hex)
    val bmp: Bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
    private val tmp: Bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
    private val bmpCanvas = Canvas(bmp)
    private val tmpCanvas = Canvas(tmp)
    private val paint = Paint().apply { isAntiAlias = false; style = Paint.Style.FILL }
    private var cv: Canvas = bmpCanvas

    protected fun px(x: Int, y: Int, w: Int, h: Int, col: Int) {
        paint.color = col
        cv.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
    }

    fun render(p: Pose): Bitmap {
        if (p.lying) {
            tmp.eraseColor(Color.TRANSPARENT); cv = tmpCanvas; cv.save(); cv.translate(2f, 0f); draw(p); cv.restore()
            bmp.eraseColor(Color.TRANSPARENT)
            bmpCanvas.save(); bmpCanvas.translate(24f, 2f); bmpCanvas.rotate(90f); bmpCanvas.drawBitmap(tmp, 0f, 0f, null); bmpCanvas.restore()
        } else {
            bmp.eraseColor(Color.TRANSPARENT); cv = bmpCanvas; cv.save(); cv.translate(2f, 0f); draw(p); cv.restore()
        }
        return bmp
    }

    protected abstract fun draw(p: Pose)

    private val LEGS = mapOf(
        "stand" to arrayOf(intArrayOf(6, 0, 3, 3), intArrayOf(12, 0, 3, 3)), "walkA" to arrayOf(intArrayOf(5, 0, 3, 3), intArrayOf(13, 0, 3, 3)),
        "walkB" to arrayOf(intArrayOf(7, 0, 3, 3), intArrayOf(11, 0, 3, 2)), "runA" to arrayOf(intArrayOf(5, 0, 3, 2), intArrayOf(13, 0, 3, 3)),
        "tuck" to arrayOf(intArrayOf(6, 0, 3, 2), intArrayOf(12, 0, 3, 2)), "dangle" to arrayOf(intArrayOf(7, 0, 3, 3), intArrayOf(11, 0, 3, 3)),
        "sit" to arrayOf(intArrayOf(5, 0, 4, 2), intArrayOf(12, 0, 4, 2)), "kick" to arrayOf(intArrayOf(6, 0, 3, 3), intArrayOf(13, -1, 3, 2))
    )
    private val SHOES = mapOf(
        "stand" to arrayOf(intArrayOf(5, 3, 4, 1), intArrayOf(12, 3, 4, 1)), "walkA" to arrayOf(intArrayOf(4, 3, 4, 1), intArrayOf(13, 3, 4, 1)),
        "walkB" to arrayOf(intArrayOf(7, 3, 4, 1), intArrayOf(11, 2, 4, 1)), "runA" to arrayOf(intArrayOf(3, 1, 3, 1), intArrayOf(13, 3, 4, 1)),
        "tuck" to arrayOf(intArrayOf(5, 2, 4, 1), intArrayOf(12, 2, 4, 1)), "dangle" to arrayOf(intArrayOf(7, 3, 3, 1), intArrayOf(11, 3, 3, 1)),
        "sit" to arrayOf(intArrayOf(3, 0, 2, 2), intArrayOf(16, 0, 2, 2)), "kick" to arrayOf(intArrayOf(5, 3, 4, 1), intArrayOf(16, -2, 2, 3))
    )
    protected fun legs(t: String, ly: Int, pants: Int, shoe: Int) {
        for (r in LEGS[t] ?: LEGS.getValue("stand")) px(r[0], ly + r[1], r[2], r[3], pants)
        for (r in SHOES[t] ?: SHOES.getValue("stand")) px(r[0], ly + r[1], r[2], r[3], shoe)
    }
    protected fun arm(side: Int, t: String, by: Int, sleeve: Int, hand: Int) {
        fun r(x: Int, y: Int, w: Int, h: Int, col: Int) = if (side < 0) px(x, y, w, h, col) else px(21 - x - w, y, w, h, col)
        when (t) {
            "down" -> { r(3, by + 1, 2, 4, sleeve); r(3, by + 5, 2, 1, hand) }
            "up" -> { r(3, by - 4, 2, 5, sleeve); r(3, by - 5, 2, 1, hand) }
            "out" -> { r(1, by + 1, 3, 2, sleeve); r(0, by + 1, 1, 2, hand) }
            "wave" -> { r(2, by - 3, 2, 4, sleeve); r(1, by - 4, 2, 1, hand) }
            "hip" -> { r(3, by + 1, 2, 2, sleeve); r(4, by + 3, 1, 1, hand) }
            "hold" -> { r(3, by + 1, 2, 2, sleeve); r(4, by + 3, 3, 1, sleeve); r(7, by + 2, 1, 1, hand) }
            "cross" -> { r(3, by + 1, 2, 2, sleeve); r(5, by + 2, 5, 1, sleeve); r(9, by + 2, 1, 1, hand) }
        }
    }
    protected val EYE = c("#1c1b24"); protected val M_IN = c("#6b2230"); protected val BLUSH = c("#f7a0a0")
    protected val SPARK = c("#ffe45e"); protected val TEAR = c("#5bc0ff"); protected val TONGUE = c("#ff7a9a")

    /** 공통 얼굴 (눈·코·입) — 얼굴 기준 좌표 */
    protected fun face(p: Pose, lid: Int, mouthCol: Int, noseCol: Int, ey: Int = 7) {
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        val l = if (p.look < 0) 0 else 1
        when (p.eyes) {
            "closed" -> { h(7, ey, 2, 1, lid); h(12, ey, 2, 1, lid) }
            "wink" -> { h(7 + l, ey, 1, 1, EYE); h(12, ey, 2, 1, lid) }
            "sparkle" -> { h(7 + l, ey, 1, 1, SPARK); h(12 + l, ey, 1, 1, SPARK); h(7 + l, ey + 1, 1, 1, EYE); h(12 + l, ey + 1, 1, 1, EYE) }
            "angry" -> { h(7 + l, ey, 1, 1, EYE); h(12 + l, ey, 1, 1, EYE); h(8, ey - 1, 2, 1, lid); h(11, ey - 1, 2, 1, lid) }
            "wide" -> { h(7 + l, ey, 1, 2, EYE); h(12 + l, ey, 1, 2, EYE) }
            "half" -> { h(7, ey, 2, 1, lid); h(12, ey, 2, 1, lid); h(7 + l, ey + 1, 1, 1, EYE); h(12 + l, ey + 1, 1, 1, EYE) }
            else -> { h(7 + l, ey, 1, 1, EYE); h(12 + l, ey, 1, 1, EYE) }
        }
        if (p.eyes == "tear") { h(7 + l, ey + 1, 1, 2, TEAR); h(12 + l, ey + 1, 1, 2, TEAR) }
        h(10, 8, 1, 1, noseCol)
        when (p.mouth) {
            "smile" -> { h(9, 10, 3, 1, mouthCol); h(8, 9, 1, 1, mouthCol); h(12, 9, 1, 1, mouthCol) }
            "open" -> h(9, 9, 3, 2, M_IN)
            "o" -> h(10, 9, 1, 2, M_IN)
            "grin" -> { h(8, 9, 5, 2, M_IN); h(9, 9, 3, 1, Color.WHITE) }
            "tongue" -> { h(9, 9, 3, 1, M_IN); h(10, 10, 1, 1, TONGUE) }
            "pout" -> { h(9, 10, 3, 1, mouthCol); h(10, 9, 1, 1, mouthCol) }
            else -> h(9, 10, 3, 1, mouthCol)
        }
        if (p.blush) { h(6, 9, 2, 1, BLUSH); h(13, 9, 2, 1, BLUSH) }
    }

    /** 소품 (오른손 위치 기준) */
    protected fun prop(p: Pose, by: Int) {
        val pr = p.prop ?: return
        val (ax, ay) = when (p.aR) {
            "up" -> 16 to by - 5; "out" -> 20 to by + 1; "wave" -> 18 to by - 4
            "hip" -> 16 to by + 3; "hold" -> 13 to by + 2; "cross" -> 12 to by + 2; else -> 16 to by + 5
        }
        val dark = c("#22222c"); val w = Color.WHITE
        val hx = p.hx; val hy = p.hy
        when (pr) {
            "phone" -> { px(ax, ay - 2, 2, 3, dark); px(ax, ay - 2, 2, 2, c("#6fd3ff")) }
            "roblox" -> { px(ax - 2, ay - 3, 4, 3, dark); px(ax - 1, ay - 2, 2, 1, c("#ff4a4a")) }
            "coffee" -> { px(ax - 1, ay - 2, 3, 3, w); px(ax - 1, ay - 2, 3, 1, c("#6b3e1e")); px(ax + 2, ay - 1, 1, 1, w) }
            "cola" -> { px(ax, ay - 3, 2, 3, c("#d8343a")); px(ax, ay - 2, 2, 1, w); px(ax, ay - 3, 2, 1, c("#bbbbbb")) }
            "chips" -> { px(ax - 2, ay - 3, 4, 4, c("#ffcc33")); px(ax - 2, ay - 2, 4, 1, c("#e8312a")); px(ax - 1, ay - 4, 2, 1, c("#ffcc33")) }
            "malatang" -> { px(ax - 3, ay - 1, 6, 2, w); px(ax - 2, ay - 2, 4, 1, c("#e0402a")); px(ax - 1, ay - 2, 1, 1, c("#3a9a4a")); px(ax - 2, ay + 1, 4, 1, c("#dddddd")) }
            "card" -> { px(ax - 1, ay - 2, 3, 2, c("#3b6fd8")); px(ax - 1, ay - 1, 3, 1, c("#ffd54a")) }
            "lollipop" -> { px(ax, ay - 1, 1, 3, w); px(ax - 1, ay - 4, 3, 3, c("#ff6fb0")); px(ax, ay - 3, 1, 1, c("#ffd0e8")) }
            "jelly" -> { px(ax - 1, ay - 2, 1, 2, c("#ff4a4a")); px(ax, ay - 2, 1, 2, c("#4ad06a")); px(ax + 1, ay - 2, 1, 2, c("#ffcc33")) }
            "milk" -> { px(ax - 1, ay - 3, 3, 4, c("#8b5a2b")); px(ax - 1, ay - 3, 3, 1, w); px(ax, ay - 4, 1, 1, w) }
            "gimbap" -> { px(ax - 1, ay - 3, 3, 3, w); px(ax, ay - 4, 1, 1, w); px(ax - 1, ay - 1, 3, 1, dark) }
            "icecream" -> { px(ax, ay - 3, 2, 2, c("#ffb6c8")); px(ax, ay - 1, 2, 1, c("#d9a24a")) }
            "cupramen" -> { px(ax - 1, ay - 3, 3, 3, w); px(ax - 1, ay - 2, 3, 1, c("#e8312a")); px(ax - 1, ay - 3, 3, 1, c("#f5c518")) }
            "wand" -> { px(ax, ay - 4, 1, 4, c("#ffd54a")); px(ax - 1, ay - 5, 3, 1, SPARK); px(ax, ay - 6, 1, 3, SPARK) }
            "bubble" -> { px(ax, ay - 2, 1, 3, c("#ff8fd0")); px(ax - 1, ay - 5, 3, 1, c("#ff8fd0")); px(ax - 1, ay - 4, 1, 1, c("#ff8fd0")); px(ax + 1, ay - 4, 1, 1, c("#ff8fd0")) }
            "crayon" -> { px(ax, ay - 3, 1, 3, c("#ff6f96")); px(ax, ay - 4, 1, 1, c("#d04070")); px(4, 19, 12, 2, w); px(6, 19, 3, 1, c("#ffcc33")); px(11, 20, 3, 1, c("#5b8cff")) }
            "toothbrush" -> { px(ax, ay - 3, 1, 4, c("#5bc0ff")); px(ax - 1, ay - 4, 2, 1, w) }
            "book" -> { px(ax - 3, ay - 2, 5, 4, c("#ff8fb0")); px(ax - 2, ay - 1, 3, 2, w) }
            "beer" -> { px(ax - 1, ay - 3, 3, 4, c("#f0b42a")); px(ax - 1, ay - 4, 3, 1, w); px(ax + 2, ay - 2, 1, 2, c("#dddddd")) }
            "soju" -> { px(ax, ay - 5, 1, 2, c("#3fae6a")); px(ax - 1, ay - 3, 2, 4, c("#4fc07a")); px(ax - 1, ay - 2, 2, 1, w) }
            "shot" -> { px(ax, ay - 2, 2, 2, c("#e8f4ff")); px(ax, ay - 1, 2, 1, c("#cfe6ff")) }
            "wine" -> { px(ax - 1, ay - 4, 3, 2, c("#a01830")); px(ax, ay - 2, 1, 2, c("#e8e8f0")); px(ax - 1, ay, 3, 1, c("#e8e8f0")) }
            "makgeolli" -> { px(ax - 2, ay - 2, 4, 2, c("#f4f0e6")); px(ax - 1, ay - 2, 2, 1, c("#e8e0c8")) }
            "pot" -> { px(4, by + 4, 13, 2, c("#f2a0b0")); px(5, by + 3, 11, 1, c("#2a2a30")); px(7, by + 3, 2, 1, w); px(11, by + 3, 2, 1, c("#e0a060")); px(2, by + 4, 2, 1, c("#e07090")); px(17, by + 4, 2, 1, c("#e07090")) }
            "parcel" -> { px(ax - 3, ay - 3, 6, 4, c("#c8935a")); px(ax - 3, ay - 2, 6, 1, c("#e8c08a")) }
            "mirror" -> { px(ax - 1, ay - 3, 3, 3, c("#cfe6ff")); px(ax, ay, 1, 2, c("#ff8fb0")) }
            "keys" -> { px(ax, ay - 1, 2, 2, c("#c8c8d0")); px(ax + 1, ay + 1, 1, 1, c("#ffcc33")) }
            "laptop" -> { px(4, 19, 12, 1, c("#bfbfc8")); px(5, 15, 10, 4, c("#3a3a46")); px(6, 16, 8, 2, c("#6fd3ff")) }
            "vacuum" -> { px(ax, ay - 1, 1, 7, c("#888899")); px(ax - 1, 20, 4, 2, c("#e8312a")) }
            "umbrella" -> { px(ax, ay - 7, 1, 8, dark); px(ax - 4, ay - 9, 9, 2, c("#ffcc33")); px(ax - 3, ay - 10, 7, 1, c("#ffcc33")) }
            "flower" -> { px(ax, ay - 1, 1, 3, c("#3a9a4a")); px(ax - 1, ay - 3, 3, 2, c("#ff6f96")); px(ax, ay - 3, 1, 1, SPARK) }
            "heart" -> { val hc = c("#ff4f7a"); px(ax - 2, ay - 4, 1, 1, hc); px(ax, ay - 4, 1, 1, hc); px(ax - 2, ay - 3, 3, 1, hc); px(ax - 1, ay - 2, 1, 1, hc) }
            "witch" -> { px(4 + hx, 2 + hy, 13, 1, dark); px(7 + hx, 0 + hy, 7, 2, dark); px(9 + hx, -1 + hy, 3, 1, dark); px(7 + hx, 1 + hy, 7, 1, c("#ff8a2a")) }
            "catears" -> { px(5 + hx, 0 + hy, 2, 2, c("#ffe0ec")); px(14 + hx, 0 + hy, 2, 2, c("#ffe0ec")); px(5 + hx, 1 + hy, 1, 1, c("#ff9ac0")); px(15 + hx, 1 + hy, 1, 1, c("#ff9ac0")); px(6 + hx, 2 + hy, 9, 1, w) }
            "headset" -> { px(5 + hx, 1 + hy, 11, 1, dark); px(3 + hx, 6 + hy, 2, 3, c("#4ad06a")); px(16 + hx, 6 + hy, 2, 3, c("#4ad06a")) }
            "backpack" -> { px(2, by + 1, 3, 5, c("#ff8fb0")); px(2, by + 2, 3, 1, c("#ffd0e0")) }
        }
    }
}

/** 수아: 초1, 반묶음 머리 + 반짝 핀, 검정 맨투맨(태극기 패치), 목걸이 */
object SuaSprite : PixelSprite() {
    private val HAIR = c("#2a1d1a"); private val HAIR_HI = c("#4a3530"); private val SKIN = c("#f6d2b4"); private val SKIN_SH = c("#e0b090")
    private val CLIP1 = c("#ff7ab8"); private val CLIP2 = c("#b07aff"); private val SHIRT = c("#1f1f26"); private val SHIRT_HI = c("#34343e")
    private val SILVER = c("#d0d0d8"); private val PANTS = c("#7a7a86"); private val SHOE = c("#ff8fb0"); private val LID = c("#5a3a30"); private val MOUTH = c("#c0605a")

    override fun draw(p: Pose) {
        val by = p.by
        if (p.prop == "backpack") prop(p, by)
        legs(p.legs, p.ly, PANTS, SHOE)
        px(5, by, 11, 6, SHIRT); px(5, by + 5, 11, 1, SHIRT_HI); px(9, by, 3, 1, SKIN)
        px(9, by + 1, 1, 1, SILVER); px(11, by + 1, 1, 1, SILVER); px(10, by + 2, 1, 2, SILVER)
        px(6, by + 3, 1, 1, SHIRT_HI); px(7, by + 3, 1, 1, c("#cccccc"))
        arm(-1, p.aL, by, SHIRT, SKIN); arm(1, p.aR, by, SHIRT, SKIN)
        // 태극기 패치
        val fx = if (p.aR == "down" || p.aR == "hip") 15 else 13
        px(fx, by + 2, 2, 2, Color.WHITE); px(fx, by + 2, 1, 1, c("#d8343a")); px(fx + 1, by + 3, 1, 1, c("#2a52be"))
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        // 머리
        h(9, 0, 3, 1, HAIR); h(8, 1, 5, 1, HAIR); h(8, 1, 1, 1, CLIP1); h(12, 1, 1, 1, CLIP2); h(9, 0, 1, 1, CLIP1)
        h(5, 2, 11, 2, HAIR); h(4, 3, 1, 3, HAIR); h(16, 3, 1, 3, HAIR); h(7, 2, 3, 1, HAIR_HI)
        h(5, 4, 11, 7, SKIN); h(6, 11, 9, 1, SKIN); h(4, 6, 1, 4, SKIN); h(16, 6, 1, 4, SKIN)
        h(5, 4, 2, 1, HAIR); h(8, 4, 1, 1, HAIR); h(13, 4, 3, 1, HAIR); h(5, 5, 1, 1, HAIR)
        h(3, 5, 1, 7, HAIR); h(17, 5, 1, 7, HAIR); h(4, 10, 1, 2, HAIR); h(16, 10, 1, 2, HAIR)
        h(5, 9, 1, 1, BLUSH); h(15, 9, 1, 1, BLUSH)
        face(p, LID, MOUTH, SKIN_SH)
        if (p.prop != "backpack") prop(p, by)
    }
}

/** 수아 엄마: 긴 생머리, 연하늘 코듀로이 셔츠자켓, 흰 티 */
object MomSprite : PixelSprite() {
    private val HAIR = c("#1c1618"); private val HAIR_HI = c("#3a2c2a"); private val SKIN = c("#f3d0b5"); private val SKIN_SH = c("#dcae90")
    private val JACKET = c("#a8b4c4"); private val J_SH = c("#8c98aa"); private val J_LN = c("#bcc7d6"); private val TEE = c("#f4f2f0")
    private val PANTS = c("#2a2a30"); private val SHOE = c("#f4f4f4"); private val LID = c("#4a3a36"); private val MOUTH = c("#c06060")

    override fun draw(p: Pose) {
        val by = p.by
        legs(p.legs, p.ly, PANTS, SHOE)
        px(5, by, 11, 6, JACKET); px(9, by, 3, 2, TEE); px(10, by + 2, 1, 1, TEE)
        px(7, by + 1, 1, 4, J_LN); px(13, by + 1, 1, 4, J_LN); px(11, by + 3, 3, 1, J_SH); px(8, by + 2, 1, 1, c("#556070")); px(5, by + 5, 11, 1, J_SH)
        arm(-1, p.aL, by, JACKET, SKIN); arm(1, p.aR, by, JACKET, SKIN)
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        h(6, 1, 9, 1, HAIR); h(5, 2, 11, 3, HAIR); h(10, 2, 1, 1, HAIR_HI); h(7, 2, 2, 1, HAIR_HI); h(12, 2, 2, 1, HAIR_HI)
        h(6, 5, 9, 6, SKIN); h(7, 11, 7, 1, SKIN); h(6, 10, 1, 1, SKIN_SH); h(14, 10, 1, 1, SKIN_SH)
        h(5, 5, 1, 6, HAIR); h(15, 5, 1, 6, HAIR); h(6, 5, 2, 1, HAIR); h(13, 5, 2, 1, HAIR)
        h(7, 6, 2, 1, LID); h(12, 6, 2, 1, LID)
        face(p, LID, MOUTH, SKIN_SH)
        // 어깨 아래로 내려오는 긴 머리
        if (!p.lying) { h(4, 6, 1, 10, HAIR); h(16, 6, 1, 10, HAIR); h(5, 11, 1, 5, HAIR); h(15, 11, 1, 5, HAIR) }
        prop(p, by)
    }
}
