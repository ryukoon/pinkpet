package com.ryukoon.pinkpet

import kotlin.math.abs
import kotlin.random.Random

/** 시아·정우 펫 */
object Roster {
    const val title = "시아·정우 펫 🏃‍♀️🥋"
    const val subtitle = "달리기 대장 시아와 태권도 3단 MC 정우가 홈 화면과 다른 앱 위를 뛰어다녀요. 둘 다 켜두면 만났을 때 같이 놀아요."
    const val notif = "시아·정우가 뛰어다니는 중"
    const val hints = "· 펫을 꾹 누르고 있으면 이 메뉴가 열려요.\n· 톡 누르면 인사해요. 잡고 던지면 떨어져서 띠용~ 해요.\n· 둘이 만나면 달리기 시합, 농구 패스, 하이파이브, 마라탕 vs 소고기 토론을 해요.\n· 정우가 격파로 화면을 깨면 시아가 \"야! 유정우!\" 해요."
    const val isMaker = false
    fun init(ctx: android.content.Context) { }
    const val hasSpicy = false
    const val hasToys = true

    // ================= 수영장 · 체육관 대결 =================
    private var contest = false
    private var contestCool = 12f
    private var announced = false
    private var winnerId = ""
    private var cKindNow = ""

    fun startContest(kind: String, pets: Map<String, Engine>, toys: List<Toy>, grid: Float) {
        val sia = pets["sia"] ?: return
        val woo = pets["woo"] ?: return
        val venue = toys.firstOrNull { it.type == kind } ?: run { sia.talk(if (kind == "pool") "수영장을 먼저 놓아주세요!" else "체육관을 먼저 놓아주세요!"); return }
        val left = venue.x - venue.wPx / 2; val right = venue.x + venue.wPx / 2
        cKindNow = if (kind == "pool") "swim" else listOf("run", "jumprope", "pushup", "shoot").random()
        when (cKindNow) {
            "swim" -> { sia.startContest("swim", left + 8 * grid, right - 6 * grid, 95f, 0f); woo.startContest("swim", left + 14 * grid, right - 6 * grid, 88f, 0f) }
            "run" -> { val l = left - 20 * grid; val r = right + 20 * grid; sia.startContest("run", l, r, 230f, 0f); woo.startContest("run", l + 6 * grid, r, 215f, 0f) }
            "jumprope" -> { sia.startContest("jumprope", venue.x - 14 * grid, venue.x - 14 * grid, 0f, .36f + Random.nextFloat() * .08f); woo.startContest("jumprope", venue.x + 14 * grid, venue.x + 14 * grid, 0f, .38f + Random.nextFloat() * .08f) }
            "pushup" -> { sia.startContest("pushup", venue.x - 14 * grid, venue.x - 14 * grid, 0f, .46f + Random.nextFloat() * .08f); woo.startContest("pushup", venue.x + 14 * grid, venue.x + 14 * grid, 0f, .40f + Random.nextFloat() * .08f) }
            else -> { sia.startContest("shoot", venue.x - 14 * grid, venue.x - 14 * grid, 0f, .7f); woo.startContest("shoot", venue.x + 14 * grid, venue.x + 14 * grid, 0f, .7f) }
        }
        val title = mapOf("swim" to "🏊 수영 대결!", "run" to "🏃 달리기 대결!", "jumprope" to "줄넘기 대결!", "pushup" to "💪 팔굽혀펴기 대결!", "shoot" to "🏀 슛 대결!")[cKindNow] ?: "대결!"
        woo.talk("$title 제가 MC 겸 선수입니다!")
        sia.sayLater("좋아, 누나가 이긴다!", 1.6f)
        contest = true; announced = false; winnerId = ""; contestCool = 30f
    }

    private fun runContest(sia: Engine, woo: Engine) {
        if (sia.actName != "contest" || woo.actName != "contest") { contest = false; return }
        if (!sia.cGo && sia.cReady && woo.cReady) {
            sia.cGo = true; woo.cGo = true
            sia.popAt("준비… 땅!", (sia.x + woo.x) / 2, minOf(sia.y, woo.y) - 60 * 3f)
        }
        val race = cKindNow == "swim" || cKindNow == "run"
        if (race && winnerId.isEmpty() && (sia.cDone || woo.cDone)) {
            val siaWins = sia.cDone && (!woo.cDone || Random.nextBoolean())
            winnerId = if (siaWins) "sia" else "woo"
            if (siaWins) { sia.contestResult(true, if (cKindNow == "swim") "1등! 수영은 내가 최고~" else "역시 내가 제일 빠르지~"); woo.contestResult(false, "누나 너무 빨라…! 다시 해!") }
            else { woo.contestResult(true, "이겼다!! 태권도 3단의 힘!"); sia.contestResult(false, "아깝다! 한 판 더!") }
        }
        if (!race && winnerId.isEmpty() && sia.cDone && woo.cDone) {
            val s = sia.cCount; val w = woo.cCount
            winnerId = if (s >= w) "sia" else "woo"
            if (s == w) { sia.contestResult(true, "${s}개! 무승부네~"); woo.contestResult(true, "${w}개! 비겼다!") }
            else if (s > w) { sia.contestResult(true, "${s}개! 내가 이겼다!"); woo.contestResult(false, "${w}개… 졌다! 다음엔 꼭!") }
            else { woo.contestResult(true, "${w}개! 정우 승리!"); sia.contestResult(false, "${s}개… 아깝다!") }
        }
        if (winnerId.isNotEmpty() && !announced) announced = true
    }
    val all: List<CharDef> = listOf(SiaDef, WooDef)
    fun byId(id: String?): CharDef? = all.firstOrNull { it.id == id }
    fun spawnFrac(id: String) = if (id == "sia") .3f else .7f

    fun hook(pets: Map<String, Engine>) {
        pets["woo"]?.onCrash = { cx -> pets["sia"]?.interact("angry", cx, "야! 유정우! 화면 깨졌잖아!") }
    }

    private var cool = 3f
    fun tick(pets: Map<String, Engine>, toys: List<Toy>, dt: Float, grid: Float) {
        val sia = pets["sia"] ?: return
        val woo = pets["woo"] ?: return
        // ---- 대결 진행 ----
        if (contest) { runContest(sia, woo); return }
        contestCool -= dt
        val venue = toys.firstOrNull { it.type == "pool" } ?: toys.firstOrNull { it.type == "gym" }
        if (venue != null && contestCool <= 0 && sia.canInteract && woo.canInteract && sia.stateName == "act" && woo.stateName == "act") {
            val inside = { e: Engine -> abs(e.x - venue.x) < venue.wPx / 2 && abs(e.y - venue.y) < 30 * grid }
            // 둘 다 수영장/체육관 위에 올려두면 바로, 아니면 가끔 스스로 대결
            if ((inside(sia) && inside(woo)) || Random.nextFloat() < dt / 25f) { startContest(if (venue.type == "pool") "pool" else "gym", pets, toys, grid); return }
        }
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
