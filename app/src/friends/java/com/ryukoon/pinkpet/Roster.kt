package com.ryukoon.pinkpet

import kotlin.math.abs
import kotlin.random.Random

/** 펫 친구들: 여유곰 + 진행해줘 부장님 */
object Roster {
    const val title = "펫 친구들 🐻🐔"
    const val subtitle = "여유곰과 진행해줘 부장님이 홈 화면과 다른 앱 위를 돌아다녀요. 둘 다 켜두면 만났을 때 서로 알아봐요."
    const val notif = "펫 친구들이 돌아다니는 중"
    const val hints = "· 펫을 꾹 누르고 있으면 이 메뉴가 열려요.\n· 톡 누르면 인사해요. 잡고 던지면 떨어져서 띠용~ 해요.\n· 둘이 부딪히면 튕겨 나가거나, 인사·하이파이브·춤·치킨 파티를 해요.\n· 곰이 유리를 깨면 부장님이 \"누구야!!\" 하고 달려와요.\n· 부장님이 닭이 되면 곰이 쫓아가요."
    const val hasToys = false
    fun startContest(kind: String, pets: Map<String, Engine>, toys: List<Toy>, grid: Float) { }
    const val hasSpicy = false
    val all: List<CharDef> = listOf(BearDef, PinkDef)
    fun byId(id: String?): CharDef? = all.firstOrNull { it.id == id }
    fun spawnFrac(id: String) = if (id == "bear") .3f else .7f

    fun hook(pets: Map<String, Engine>) {
        pets["bear"]?.onCrash = { cx -> pets["pink"]?.interact("run", cx, "누구야!! 유리 누가 깼어!!") }
        pets["pink"]?.onCrash = { cx -> pets["bear"]?.interact("look", cx, "부장님… 화면이…") }
    }

    private var cool = 3f
    fun tick(pets: Map<String, Engine>, toys: List<Toy>, dt: Float, grid: Float) {
        val bear = pets["bear"] ?: return
        val pink = pets["pink"] ?: return
        cool -= dt
        if (cool > 0) return
        if (bear.stateName == "away" || pink.stateName == "away") return
        if (abs(bear.x - pink.x) > 13 * grid || abs(bear.y - pink.y) > 18 * grid) return
        cool = 7f
        val moving = setOf("air", "drag")
        if (bear.stateName in moving || pink.stateName in moving || bear.actName == "run" || pink.actName == "run") { bear.bump(pink.x); pink.bump(bear.x); return }
        if (!bear.canInteract || !pink.canInteract) { cool = 2f; return }
        if (pink.isChicken) { bear.interact("run", pink.x, "치킨이다!! 🍗"); pink.interact("cfly", bear.x, "꼬꼬댁!! 살려줘~"); return }
        val midX = (bear.x + pink.x) / 2; val topY = minOf(bear.y, pink.y) - 24 * grid
        when (Random.nextInt(6)) {
            0 -> { bear.interact("bow", pink.x, "부장님 안녕하세요~"); pink.interact("wave", bear.x, "어 왔어? 진행해줘!") }
            1 -> { bear.interact("pawWave", pink.x, "하이파이브!"); pink.interact("wave", bear.x, null); bear.popAt("짝!", midX, topY) }
            2 -> { bear.interact("dance", pink.x, "🎵 같이 춰요~"); pink.interact("dance", bear.x, "오 좋아!") }
            3 -> { pink.interact("boxing", bear.x, "덤벼라!"); bear.interact("kungfu", pink.x, "곰발 펀치!") }
            4 -> { pink.interact("chickenEat", bear.x, "치킨 먹을래?"); bear.interact("chickenEat", pink.x, "좋아요!! 🍗") }
            else -> { bear.bump(pink.x); pink.bump(bear.x); bear.talk("앗 죄송!"); pink.talk("어이쿠!") }
        }
    }
}
