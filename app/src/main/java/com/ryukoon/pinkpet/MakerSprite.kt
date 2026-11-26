package com.ryukoon.pinkpet

import android.graphics.Color

/** 사진에서 뽑은 겉모습 (모두 8비트 팔레트 색) */
data class Look(
    var hair: String = "short", var bangs: Boolean = true, var hairColor: Int = Color.parseColor("#2a1c18"),
    var skin: Int = Color.parseColor("#f6d2b4"),
    var top: String = "tshirt", var topColor: Int = Color.parseColor("#3b6fd8"), var top2Color: Int = Color.parseColor("#ffffff"),
    var bottom: String = "pants", var bottomColor: Int = Color.parseColor("#2b3d6b"), var shoeColor: Int = Color.parseColor("#f4f4f4"),
    var acc: String = "none", var female: Boolean = false
)

/** 8비트 팔레트와 부품 이름 */
object Palette {
    private fun c(h: String) = Color.parseColor(h)
    val skin = listOf("#ffe0c8", "#f6d2b4", "#f1c6a0", "#e0b090", "#c98d64", "#8d5a3b").map { c(it) }
    val hair = listOf("#141418", "#2a1c18", "#4a3020", "#7a4a24", "#b07a40", "#e0c070", "#c84a2a", "#9a9aa6", "#e8e8f0", "#ff7ab8", "#5b8cff", "#7a3fa0").map { c(it) }
    val cloth = listOf("#1c1c22", "#3a3a46", "#6a6a72", "#a8b4c4", "#e8e8f0", "#ffffff", "#e8312a", "#ff8fb0", "#f0a38a", "#ffcc33",
        "#f6d978", "#3fae6a", "#2b3d6b", "#3b5a8a", "#3b6fd8", "#5bc0ff", "#8a5a2b", "#c8935a", "#7a3fa0", "#b07aff").map { c(it) }
    val hairStyles = listOf("short" to "짧은 머리", "sidepart" to "옆가르마", "buzz" to "스포츠", "curly" to "곱슬", "bob" to "단발", "long" to "긴 생머리", "ponytail" to "묶은 머리")
    val tops = listOf("tshirt" to "티셔츠", "hoodie" to "후드티", "knit" to "니트", "padding" to "패딩", "vest" to "조끼+안쪽옷", "jacket" to "셔츠자켓")
    val bottoms = listOf("pants" to "긴바지", "shorts" to "반바지", "skirt" to "치마")
    val accs = listOf("none" to "없음", "glasses" to "안경", "sunglasses" to "선글라스", "cap" to "모자", "catears" to "고양이 머리띠", "headband" to "머리띠")

    /** 가장 가까운 팔레트 색으로 바꾸기 (8비트 느낌) */
    fun nearest(color: Int, pal: List<Int>): Int {
        var best = pal[0]; var bd = Long.MAX_VALUE
        val r = Color.red(color); val g = Color.green(color); val b = Color.blue(color)
        for (p in pal) {
            val dr = (r - Color.red(p)).toLong(); val dg = (g - Color.green(p)).toLong(); val db = (b - Color.blue(p)).toLong()
            val d = 3 * dr * dr + 4 * dg * dg + 2 * db * db
            if (d < bd) { bd = d; best = p }
        }
        return best
    }
    fun shade(c: Int, f: Float): Int = Color.rgb((Color.red(c) * f).toInt().coerceIn(0, 255), (Color.green(c) * f).toInt().coerceIn(0, 255), (Color.blue(c) * f).toInt().coerceIn(0, 255))
}

/** 부품을 조합해서 그리는 캐릭터 */
class MakerSprite(var look: Look) : PixelSprite() {
    private val WHITE = Color.parseColor("#f6f6f6")
    private val GLASS = Color.parseColor("#2a2a30")

    override fun draw(p: Pose) {
        val L = look; val by = p.by
        val dob = p.prop == "dobok" || p.prop == "board"
        val swim = p.prop == "swimcap" || p.prop == "goggles"
        val top = if (dob) WHITE else L.topColor
        val topSh = Palette.shade(top, .8f); val topHi = Palette.shade(top, 1.2f)
        val skinSh = Palette.shade(L.skin, .86f)
        val lid = Palette.shade(L.skin, .5f)
        val bottom = if (dob) WHITE else L.bottomColor
        // 다리
        when {
            !dob && L.bottom == "shorts" -> { legs(p.legs, p.ly, L.skin, L.shoeColor); if (p.legs != "sit") px(5, p.ly, 11, 1, bottom) else px(5, p.ly, 11, 1, bottom) }
            !dob && L.bottom == "skirt" -> { legs(p.legs, p.ly, L.skin, L.shoeColor); if (p.legs != "sit") { px(5, p.ly, 11, 1, bottom); px(4, p.ly + 1, 13, 1, bottom) } else px(4, p.ly, 13, 1, bottom) }
            else -> legs(p.legs, p.ly, bottom, if (dob) L.skin else L.shoeColor)
        }
        // 윗옷
        px(5, by, 11, 6, top)
        var sleeve = top
        if (dob) {
            px(8, by, 2, 3, topSh); px(11, by, 2, 3, topSh); px(10, by + 1, 1, 2, topSh)
            px(5, by + 4, 11, 1, Color.parseColor("#111111")); px(9, by + 5, 1, 1, Color.parseColor("#111111")); px(11, by + 5, 1, 1, Color.parseColor("#111111"))
        } else when (L.top) {
            "tshirt" -> { px(9, by, 3, 1, L.skin); px(5, by + 5, 11, 1, topSh); sleeve = L.skin }
            "hoodie" -> { px(8, by, 5, 1, topSh); px(9, by + 1, 1, 2, WHITE); px(11, by + 1, 1, 2, WHITE); px(7, by + 3, 7, 2, topSh) }
            "knit" -> {
                px(5, by + 5, 11, 1, topSh); px(8, by, 5, 1, topSh)
                for (r in 1..4) { px(if (r % 2 == 1) 7 else 6, by + r, 1, 1, topHi); px(10, by + r, 1, 1, if (r % 2 == 1) topHi else topSh); px(if (r % 2 == 1) 13 else 14, by + r, 1, 1, topHi) }
            }
            "padding" -> { px(5, by + 2, 11, 1, topSh); px(5, by + 4, 11, 1, topSh); px(6, by, 9, 1, topHi); px(10, by, 1, 6, Palette.shade(top, 1.5f)) }
            "vest" -> { px(9, by, 3, 6, L.top2Color); sleeve = L.top2Color; px(6, by + 1, 1, 3, topHi); px(14, by + 1, 1, 3, topHi) }
            "jacket" -> { px(9, by, 3, 2, L.top2Color); px(10, by + 2, 1, 1, L.top2Color); px(7, by + 1, 1, 4, topHi); px(13, by + 1, 1, 4, topHi); px(5, by + 5, 11, 1, topSh); px(11, by + 3, 3, 1, topSh) }
        }
        arm(-1, p.aL, by, sleeve, L.skin); arm(1, p.aR, by, sleeve, L.skin)
        if (!dob && L.top == "tshirt") {
            val down = setOf("down", "hip", "hold", "cross")
            if (p.aL in down) px(3, by + 1, 2, 1, top)
            if (p.aR in down) px(16, by + 1, 2, 1, top)
        }
        head(p, L, swim, skinSh, lid)
        if (p.prop != "dobok") prop(p, by)
    }

    private fun head(p: Pose, L: Look, swim: Boolean, skinSh: Int, lid: Int) {
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        val hc = L.hairColor; val hh = Palette.shade(hc, 1.45f)
        if (L.hair == "ponytail" && !swim) { h(16, 4, 2, 5, hc); h(17, 9, 1, 2, hc) }
        // 얼굴
        h(5, 5, 11, 6, L.skin); h(6, 11, 9, 1, L.skin); h(5, 10, 1, 1, skinSh); h(15, 10, 1, 1, skinSh)
        val ears = L.hair in setOf("short", "sidepart", "buzz", "curly", "ponytail")
        if (ears) { h(4, 7, 1, 2, L.skin); h(16, 7, 1, 2, L.skin) }
        if (!swim) when (L.hair) {
            "buzz" -> { h(6, 2, 9, 1, hc); h(5, 3, 11, 2, hc); h(7, 3, 3, 1, hh) }
            "short" -> { h(6, 1, 9, 1, hc); h(5, 2, 11, 3, hc); h(7, 2, 3, 1, hh); h(4, 3, 1, 4, hc); h(16, 3, 1, 3, hc) }
            "sidepart" -> { h(6, 1, 9, 1, hc); h(4, 2, 13, 3, hc); h(9, 2, 5, 1, hh); h(7, 2, 1, 2, hh); h(4, 5, 1, 2, hc); h(16, 5, 1, 1, hc) }
            "curly" -> {
                h(5, 1, 11, 4, hc); var x = 5; while (x <= 15) { h(x, 0, 1, 1, hc); x += 2 }
                h(4, 2, 1, 4, hc); h(16, 2, 1, 4, hc); h(6, 2, 1, 1, hh); h(9, 1, 1, 1, hh); h(12, 2, 1, 1, hh); h(14, 3, 1, 1, hh)
            }
            "bob" -> { h(6, 1, 9, 1, hc); h(5, 2, 11, 3, hc); h(4, 3, 1, 8, hc); h(16, 3, 1, 8, hc); h(5, 5, 1, 5, hc); h(15, 5, 1, 5, hc); h(7, 2, 3, 1, hh) }
            "long" -> { h(6, 1, 9, 1, hc); h(5, 2, 11, 3, hc); h(5, 5, 1, 6, hc); h(15, 5, 1, 6, hc); h(10, 2, 1, 1, hh); h(7, 2, 2, 1, hh); h(12, 2, 2, 1, hh) }
            "ponytail" -> { h(9, 0, 3, 1, hc); h(8, 1, 5, 1, hc); h(5, 2, 11, 3, hc); h(4, 3, 1, 3, hc); h(16, 3, 1, 3, hc); h(7, 2, 3, 1, hh) }
        }
        if (!swim) {
            if (L.bangs) { h(5, 5, 4, 1, hc); h(10, 5, 4, 1, hc); h(5, 6, 1, 1, hc) }
            else { h(5, 5, 1, 1, hc); h(15, 5, 1, 1, hc) }
        }
        face(p, lid, Palette.shade(L.skin, .62f), skinSh)
        if (L.female) { h(6, 7, 1, 1, lid); h(14, 7, 1, 1, lid) }
        when (L.acc) {
            "glasses" -> { for (gx in intArrayOf(6, 11)) { h(gx, 6, 4, 1, GLASS); h(gx, 8, 4, 1, GLASS); h(gx, 7, 1, 1, GLASS); h(gx + 3, 7, 1, 1, GLASS) }; h(10, 7, 1, 1, GLASS) }
            "sunglasses" -> { val d = Color.parseColor("#111111"); h(6, 7, 4, 2, d); h(11, 7, 4, 2, d); h(10, 7, 1, 1, d) }
            "cap" -> { val cc = Palette.shade(L.topColor, .9f); h(5, 1, 11, 3, cc); h(6, 0, 9, 1, cc); h(9, 4, 9, 1, Palette.shade(cc, .75f)) }
            "catears" -> { h(5, 0, 2, 2, Color.parseColor("#ffe0ec")); h(14, 0, 2, 2, Color.parseColor("#ffe0ec")); h(5, 1, 1, 1, Color.parseColor("#ff9ac0")); h(15, 1, 1, 1, Color.parseColor("#ff9ac0")); h(6, 2, 9, 1, Color.WHITE) }
            "headband" -> h(5, 3, 11, 1, Color.parseColor("#ff8fb0"))
        }
        if (!p.lying && !swim && L.hair == "long") { h(4, 6, 1, 10, hc); h(16, 6, 1, 10, hc); h(5, 11, 1, 5, hc); h(15, 11, 1, 5, hc) }
    }
}
