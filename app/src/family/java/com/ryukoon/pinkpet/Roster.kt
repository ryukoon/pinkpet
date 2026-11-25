package com.ryukoon.pinkpet

import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

/** 수아네 펫: 수아 + 수아 엄마 */
object Roster {
    const val title = "수아네 펫 👧👩"
    const val subtitle = "수아와 엄마가 홈 화면과 다른 앱 위를 돌아다녀요. 둘 다 켜두면 만났을 때 서로 반응해요."
    const val notif = "수아네 펫이 돌아다니는 중"
    const val hints = "· 펫을 꾹 누르고 있으면 이 메뉴가 열려요.\n· 톡 누르면 인사해요. 잡고 던지면 떨어져서 띠용~ 해요.\n· 수아가 엄마를 만나면 안아달라고 하거나, 카드 써도 되냐고 물어요.\n· 매일 오후 4시엔 수아가 간식 카드를 조르러 가요.\n· 엄마는 \"안돼.\" 해놓고 결국 사줘요."
    const val hasSpicy = true
    val all: List<CharDef> = listOf(SuaDef, MomDef)
    fun byId(id: String?): CharDef? = all.firstOrNull { it.id == id }
    fun spawnFrac(id: String) = if (id == "sua") .3f else .7f

    fun hook(pets: Map<String, Engine>) { }

    private var cool = 3f
    private var lastAskDay = -1
    fun tick(pets: Map<String, Engine>, dt: Float, grid: Float) {
        val sua = pets["sua"]
        val mom = pets["mom"]
        // 매일 오후 4시: 간식 카드 조르기
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_YEAR)
        if (sua != null && cal.get(Calendar.HOUR_OF_DAY) == 16 && cal.get(Calendar.MINUTE) < 10 && lastAskDay != day && sua.canInteract) {
            lastAskDay = day
            sua.interact("cardAsk", mom?.x ?: sua.x, "엄마! 4시야! 간식 카드 써도 돼?")
            if (mom != null && mom.canInteract) { mom.interact("no", sua.x, "안돼."); mom.sayLater("…하나만 사. 콜라는 작은 거.", 2.2f) }
            cool = 8f
            return
        }
        if (sua == null || mom == null) return
        cool -= dt
        if (cool > 0) return
        if (sua.stateName == "away" || mom.stateName == "away") return
        if (abs(sua.x - mom.x) > 13 * grid || abs(sua.y - mom.y) > 18 * grid) return
        cool = 7f
        val moving = setOf("air", "drag")
        if (sua.stateName in moving || mom.stateName in moving || sua.actName == "run" || mom.actName == "run") {
            sua.bump(mom.x); mom.bump(sua.x); sua.talk("엄마 미안~ 히히"); mom.talk("하지마.")
            return
        }
        if (!sua.canInteract || !mom.canInteract) { cool = 2f; return }
        val midX = (sua.x + mom.x) / 2; val topY = minOf(sua.y, mom.y) - 24 * grid
        when (Random.nextInt(7)) {
            0 -> { sua.interact("hugMe", mom.x, "엄마, 안아줘!"); mom.interact("hugKid", sua.x, "…이리 와."); mom.sayLater("아이고 내 새끼.", 1.8f) }
            1 -> { sua.interact("cardAsk", mom.x, "엄마 카드 써도 돼?"); mom.interact("no", sua.x, "안돼."); sua.sayLater("엄마아~ 한 번만~", 1.6f); mom.sayLater("…하나만 사.", 3.2f) }
            2 -> { mom.interact("goHome", sua.x, "집으로 빨리 가."); sua.interact("sulk", mom.x, "더 놀고 싶은데…") }
            3 -> { sua.interact("malatang", mom.x, "엄마 마라탕 먹자!"); mom.interact("hmm", sua.x, "음, 그렇군."); mom.sayLater("…가자. 엄마도 배고파.", 2f) }
            4 -> { mom.interact("cheers", sua.x, null); sua.interact("cola", mom.x, null); sua.popAt("짠!", midX, topY) }
            5 -> { mom.interact("wagFinger", sua.x, "하지마."); sua.interact("giggle", mom.x, "히히 안 했어~") }
            else -> { mom.interact("headpat", sua.x, "잘했네."); sua.interact("proud", mom.x, "나 잘했지?") }
        }
    }
}
