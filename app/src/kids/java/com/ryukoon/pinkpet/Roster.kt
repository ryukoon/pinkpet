package com.ryukoon.pinkpet

import kotlin.math.abs
import kotlin.random.Random

/** 시아·정우 펫 */
object Roster {
    const val title = "시아·정우 펫 🏃‍♀️🥋"
    const val subtitle = "달리기 대장 시아와 태권도 3단 MC 정우가 홈 화면과 다른 앱 위를 뛰어다녀요. 둘 다 켜두면 만났을 때 같이 놀아요."
    const val notif = "시아·정우가 뛰어다니는 중"
    const val hints = "· 펫을 꾹 누르고 있으면 이 메뉴가 열려요.\n· 톡 누르면 인사해요. 잡고 던지면 떨어져서 띠용~ 해요.\n· 둘이 만나면 달리기 시합, 농구 패스, 하이파이브, 마라탕 vs 소고기 토론을 해요.\n· 정우가 격파로 화면을 깨면 시아가 \"야! 유정우!\" 해요."
    const val hasSpicy = false
    val all: List<CharDef> = listOf(SiaDef, WooDef)
    fun byId(id: String?): CharDef? = all.firstOrNull { it.id == id }
    fun spawnFrac(id: String) = if (id == "sia") .3f else .7f

    fun hook(pets: Map<String, Engine>) {
        pets["woo"]?.onCrash = { cx -> pets["sia"]?.interact("angry", cx, "야! 유정우! 화면 깨졌잖아!") }
    }

    private var cool = 3f
    fun tick(pets: Map<String, Engine>, dt: Float, grid: Float) {
        val sia = pets["sia"] ?: return
        val woo = pets["woo"] ?: return
        cool -= dt
        if (cool > 0) return
        if (sia.stateName == "away" || woo.stateName == "away") return
        if (abs(sia.x - woo.x) > 13 * grid || abs(sia.y - woo.y) > 18 * grid) return
        cool = 7f
        val moving = setOf("air", "drag")
        if (sia.stateName in moving || woo.stateName in moving || sia.actName == "run" || woo.actName == "run") {
            sia.bump(woo.x); woo.bump(sia.x); sia.talk("앗 미안!"); woo.talk("괜찮아 누나!")
            return
        }
        if (!sia.canInteract || !woo.canInteract) { cool = 2f; return }
        val midX = (sia.x + woo.x) / 2; val topY = minOf(sia.y, woo.y) - 24 * grid
        when (Random.nextInt(7)) {
            0 -> { sia.interact("sprint", woo.x, "달리기 시합 할래?"); woo.interact("dash", sia.x, "좋아! 누가 이기나 보자!"); sia.sayLater("내가 이겼지~ ㅋㅋ", 2.6f); woo.sayLater("누나 너무 빨라!!", 3.2f) }
            1 -> { sia.interact("shoot", woo.x, "패스!"); woo.interact("ballHold", sia.x, "나이스 패스!"); sia.popAt("🏀", midX, topY) }
            2 -> { sia.interact("highfive", woo.x, "하이파이브!"); woo.interact("highfive", sia.x, null); sia.popAt("짝!", midX, topY) }
            3 -> { sia.interact("malaDream", woo.x, "마라탕 먹자!"); woo.interact("beefDream", sia.x, "아니야 소고기 먹자!"); sia.sayLater("…둘 다 먹자!", 2.4f); woo.sayLater("콜!!", 3f) }
            4 -> { woo.interact("mcInterview", sia.x, "박시아 선수, 우승 소감은요?"); sia.interact("laugh", woo.x, null); sia.sayLater("히히 다 같이 열심히 했어요!", 1.6f) }
            5 -> { woo.interact("frontKick", sia.x, "누나 내 앞차기 봐!"); sia.interact("cheerWoo", woo.x, "오~ 태권도 3단!") }
            else -> { sia.interact("okCall", woo.x, "정우야 편의점 갈래?"); woo.interact("thumbsup", sia.x, "라면 사자!") }
        }
    }
}
