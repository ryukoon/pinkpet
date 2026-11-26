package com.ryukoon.pinkpet

import android.content.Context
import kotlin.math.abs
import kotlin.random.Random

/** 도트펫 메이커: 사진으로 만든 내 캐릭터들 */
object Roster {
    const val title = "도트펫 메이커 🎨"
    const val subtitle = "사진 한 장으로 8비트 캐릭터를 만들어서 홈 화면에 키워요. 사진은 폰 밖으로 나가지 않고 저장도 안 해요."
    const val notif = "도트펫이 돌아다니는 중"
    const val hints = "· 펫을 꾹 누르면 이 메뉴가 열려요. 톡 누르면 말버릇을 말해요.\n· 잡고 던지면 떨어져서 띠용~ 해요.\n· 캐릭터끼리 만나면 하이파이브·춤·인사를 하고, 수영장·체육관에 같이 두면 대결해요."
    const val hasSpicy = false
    const val hasToys = true
    const val isMaker = true

    fun init(ctx: Context) = CharStore.init(ctx)
    val all: List<CharDef> get() = CharStore.defs()
    fun byId(id: String?): CharDef? = all.firstOrNull { it.id == id }
    fun spawnFrac(id: String): Float { val i = all.indexOfFirst { it.id == id }.coerceAtLeast(0); return (.2f + i * .23f) % .9f + .05f }

    fun hook(pets: Map<String, Engine>) { }

    private var cool = 3f
    private var contestCool = 15f
    private var contest = false
    private var aId = ""; private var bId = ""
    private var winner = ""
    private var kindNow = ""

    fun tick(pets: Map<String, Engine>, toys: List<Toy>, dt: Float, grid: Float) {
        if (pets.size < 2) return
        if (contest) { run(pets); return }
        cool -= dt; contestCool -= dt
        val list = pets.values.toList()
        // 가까운 두 캐릭터 찾기
        var a: Engine? = null; var b: Engine? = null
        loop@ for (i in list.indices) for (j in i + 1 until list.size) {
            val p = list[i]; val q = list[j]
            if (p.stateName == "away" || q.stateName == "away") continue
            if (abs(p.x - q.x) < 13 * grid && abs(p.y - q.y) < 18 * grid) { a = p; b = q; break@loop }
        }
        val venue = toys.firstOrNull { it.type == "pool" } ?: toys.firstOrNull { it.type == "gym" }
        if (venue != null && contestCool <= 0) {
            val inside = list.filter { abs(it.x - venue.x) < venue.wPx / 2 && it.canInteract && it.stateName == "act" }
            if (inside.size >= 2 || Random.nextFloat() < dt / 30f) {
                val pair = if (inside.size >= 2) inside.take(2) else list.filter { it.canInteract && it.stateName == "act" }.shuffled().take(2)
                if (pair.size == 2) { startPair(if (venue.type == "pool") "pool" else "gym", pair[0], pair[1], toys, grid); return }
            }
        }
        if (cool > 0 || a == null || b == null) return
        cool = 7f
        if (a.stateName in setOf("air", "drag") || b.stateName in setOf("air", "drag") || a.actName == "run" || b.actName == "run") {
            a.bump(b.x); b.bump(a.x); a.talk("앗 미안!"); b.talk("괜찮아~"); return
        }
        if (!a.canInteract || !b.canInteract) { cool = 2f; return }
        val midX = (a.x + b.x) / 2; val topY = minOf(a.y, b.y) - 24 * grid
        when (Random.nextInt(5)) {
            0 -> { a.interact("highfive", b.x, "하이파이브!"); b.interact("highfive", a.x, null); a.popAt("짝!", midX, topY) }
            1 -> { a.interact("dance", b.x, "같이 춤추자~"); b.interact("dance", a.x, "좋아!") }
            2 -> { a.interact("wave", b.x, "${b.def.name} 안녕!"); b.interact("wave", a.x, b.def.tapSay) }
            3 -> { a.interact("laugh", b.x, a.def.tapSay); b.interact("laugh", a.x, null) }
            else -> { a.bump(b.x); b.bump(a.x); a.talk("어이쿠!"); b.talk("앗!") }
        }
    }

    fun startContest(kind: String, pets: Map<String, Engine>, toys: List<Toy>, grid: Float) {
        val pair = pets.values.filter { it.stateName == "act" || it.stateName == "land" }.take(2)
        if (pair.size < 2) { pets.values.firstOrNull()?.talk("대결하려면 캐릭터 2명을 켜주세요!"); return }
        startPair(kind, pair[0], pair[1], toys, grid)
    }

    private fun startPair(kind: String, a: Engine, b: Engine, toys: List<Toy>, grid: Float) {
        val venue = toys.firstOrNull { it.type == kind } ?: run { a.talk(if (kind == "pool") "수영장을 먼저 놓아주세요!" else "체육관을 먼저 놓아주세요!"); return }
        val left = venue.x - venue.wPx / 2; val right = venue.x + venue.wPx / 2
        kindNow = if (kind == "pool") "swim" else listOf("run", "jumprope", "pushup", "shoot").random()
        when (kindNow) {
            "swim" -> { a.startContest("swim", left + 8 * grid, right - 6 * grid, rnd(85f, 100f), 0f); b.startContest("swim", left + 14 * grid, right - 6 * grid, rnd(85f, 100f), 0f) }
            "run" -> { val l = left - 20 * grid; val r = right + 20 * grid; a.startContest("run", l, r, rnd(200f, 235f), 0f); b.startContest("run", l + 6 * grid, r, rnd(200f, 235f), 0f) }
            else -> { a.startContest(kindNow, venue.x - 14 * grid, venue.x - 14 * grid, 0f, rnd(.36f, .5f)); b.startContest(kindNow, venue.x + 14 * grid, venue.x + 14 * grid, 0f, rnd(.36f, .5f)) }
        }
        val title = mapOf("swim" to "🏊 수영 대결!", "run" to "🏃 달리기 대결!", "jumprope" to "줄넘기 대결!", "pushup" to "💪 팔굽혀펴기 대결!", "shoot" to "🏀 슛 대결!")[kindNow] ?: "대결!"
        a.talk(title); b.sayLater("지지 않을 거야!", 1.5f)
        aId = a.def.id; bId = b.def.id; winner = ""; contest = true; contestCool = 30f
    }

    private fun rnd(a: Float, b: Float) = a + Random.nextFloat() * (b - a)

    private fun run(pets: Map<String, Engine>) {
        val a = pets[aId]; val b = pets[bId]
        if (a == null || b == null || a.actName != "contest" || b.actName != "contest") { contest = false; return }
        if (!a.cGo && a.cReady && b.cReady) { a.cGo = true; b.cGo = true; a.popAt("준비… 땅!", (a.x + b.x) / 2, minOf(a.y, b.y) - 150f) }
        val race = kindNow == "swim" || kindNow == "run"
        if (winner.isNotEmpty()) return
        if (race && (a.cDone || b.cDone)) {
            val aw = a.cDone && (!b.cDone || Random.nextBoolean())
            winner = if (aw) aId else bId
            val (w, l) = if (aw) a to b else b to a
            w.contestResult(true, "이겼다!! ${w.def.tapSay}"); l.contestResult(false, "아깝다! 한 판 더!")
        } else if (!race && a.cDone && b.cDone) {
            winner = "done"
            when {
                a.cCount == b.cCount -> { a.contestResult(true, "${a.cCount}개! 무승부~"); b.contestResult(true, "${b.cCount}개! 비겼다!") }
                a.cCount > b.cCount -> { a.contestResult(true, "${a.cCount}개! 내가 이겼다!"); b.contestResult(false, "${b.cCount}개… 다음엔 꼭!") }
                else -> { b.contestResult(true, "${b.cCount}개! 내가 이겼다!"); a.contestResult(false, "${a.cCount}개… 다음엔 꼭!") }
            }
        }
    }
}
