package com.ryukoon.pinkpet

import android.graphics.Color

/** 박시아: 6학년, 긴 머리, 흰 후드 + 검정 패딩조끼, 크로스백, 청바지 */
object SiaSprite : PixelSprite() {
    private val HAIR = c("#2a1c18"); private val HAIR_HI = c("#4a342c"); private val SKIN = c("#f4cfb0"); private val SKIN_SH = c("#ddae8c")
    private val HOODIE = c("#ece8e0"); private val VEST = c("#1e1e24"); private val VEST_HI = c("#34343c"); private val BAG = c("#111114")
    private val JEANS = c("#3b5a8a"); private val SHOE = c("#f2f2f2"); private val LID = c("#5a3a30"); private val MOUTH = c("#c0605a")

    override fun draw(p: Pose) {
        val by = p.by
        val swim = p.prop == "swimcap" || p.prop == "goggles"
        legs(p.legs, p.ly, JEANS, SHOE)
        px(5, by, 11, 6, HOODIE); px(5, by, 4, 6, VEST); px(12, by, 4, 6, VEST); px(6, by + 1, 1, 3, VEST_HI); px(10, by + 2, 1, 1, c("#666666"))
        if (p.prop != "bib" && p.prop != "medal") {
            px(12, by + 1, 1, 1, BAG); px(11, by + 2, 1, 1, BAG); px(10, by + 3, 1, 1, BAG); px(9, by + 4, 1, 1, BAG)
            px(6, by + 4, 4, 2, BAG); px(7, by + 5, 1, 1, c("#dddddd")); px(8, by + 4, 1, 1, c("#dddddd"))
        }
        arm(-1, p.aL, by, HOODIE, SKIN); arm(1, p.aR, by, HOODIE, SKIN)
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        h(7, 0, 1, 1, HAIR); h(12, 0, 1, 1, HAIR); h(6, 1, 9, 1, HAIR); h(5, 2, 11, 3, HAIR); h(8, 2, 3, 1, HAIR_HI)
        h(5, 5, 11, 6, SKIN); h(6, 11, 9, 1, SKIN); h(5, 10, 1, 1, SKIN_SH); h(15, 10, 1, 1, SKIN_SH)
        h(5, 5, 3, 1, HAIR); h(12, 5, 4, 1, HAIR); h(5, 6, 1, 1, HAIR)
        h(4, 4, 1, 8, HAIR); h(16, 4, 1, 8, HAIR); h(3, 6, 1, 4, HAIR)
        face(p, LID, MOUTH, SKIN_SH)
        if (!p.lying && !swim) { h(4, 12, 1, 4, HAIR); h(16, 12, 1, 3, HAIR) }
        prop(p, by)
    }
}

/** 유정우: 4학년, 짧은 검은 머리, 검정 맨투맨·조거, 회색 크록스. 도복·선글라스·마이크 소품 */
object WooSprite : PixelSprite() {
    private val HAIR = c("#141418"); private val HAIR_HI = c("#34343c"); private val SKIN = c("#f2c8a6"); private val SKIN_SH = c("#d8a884")
    private val SHIRT = c("#1c1c22"); private val PRINT = c("#e8e8e8"); private val PANTS = c("#1c1c22"); private val CROCS = c("#6a6a72")
    private val DOBOK = c("#f6f6f6"); private val DOBOK_SH = c("#dcdce4"); private val BELT = c("#111111")
    private val LID = c("#5a3a30"); private val MOUTH = c("#b85a50")

    override fun draw(p: Pose) {
        val by = p.by
        val dobok = p.prop == "dobok" || p.prop == "board"
        val top = if (dobok) DOBOK else SHIRT
        legs(p.legs, p.ly, if (dobok) DOBOK else PANTS, if (dobok) SKIN else CROCS)
        px(5, by, 11, 6, top)
        if (dobok) { px(8, by, 2, 3, DOBOK_SH); px(11, by, 2, 3, DOBOK_SH); px(10, by + 1, 1, 2, DOBOK_SH); px(5, by + 4, 11, 1, BELT); px(9, by + 5, 1, 1, BELT); px(11, by + 5, 1, 1, BELT) }
        else { px(6, by + 1, 1, 1, PRINT); px(6, by + 3, 1, 1, PRINT); px(6, by + 5, 1, 1, PRINT); px(13, by + 1, 2, 2, c("#e8312a")); px(9, by, 3, 1, c("#2c2c34")) }
        arm(-1, p.aL, by, top, SKIN); arm(1, p.aR, by, top, SKIN)
        val hx = p.hx; val hy = p.hy
        fun h(x: Int, y: Int, w: Int, ht: Int, col: Int) = px(x + hx, y + hy, w, ht, col)
        h(6, 1, 9, 1, HAIR); h(5, 2, 11, 3, HAIR); h(7, 2, 3, 1, HAIR_HI); h(12, 1, 2, 1, HAIR)
        h(5, 5, 11, 6, SKIN); h(6, 11, 9, 1, SKIN); h(5, 10, 1, 1, SKIN_SH); h(15, 10, 1, 1, SKIN_SH)
        h(5, 5, 6, 1, HAIR); h(13, 5, 2, 1, HAIR); h(4, 3, 1, 4, HAIR); h(16, 3, 1, 3, HAIR)
        h(4, 7, 1, 2, SKIN); h(16, 7, 1, 2, SKIN)
        h(7, 6, 2, 1, HAIR); h(12, 6, 2, 1, HAIR)
        face(p, LID, MOUTH, SKIN_SH)
        if (p.prop != "dobok") prop(p, by)
        if (p.prop == "mic") { h(6, 7, 4, 2, Color.parseColor("#111111")); h(11, 7, 4, 2, Color.parseColor("#111111")); h(10, 7, 1, 1, Color.parseColor("#111111")) }
    }
}
