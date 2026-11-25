package com.ryukoon.pinkpet

import android.graphics.Bitmap

enum class Form { NORMAL, POWER, CHICKEN }

/** 한 프레임의 자세. 좌표 단위는 24x24 도트 그리드. */
data class Pose(
    var hx: Int = 0, var hy: Int = 0, var by: Int = 12, var ly: Int = 18,
    var legs: String = "stand", var aL: String = "down", var aR: String = "down",
    var eyes: String = "open", var look: Int = 1, var mouth: String = "flat",
    var blush: Boolean = false, var lying: Boolean = false, var pot: Boolean = false,
    var eat: Boolean = false, var pen: String? = null, var box: Boolean = false,
    var prop: String? = null, var lift: Float = 0f, var pivot: Float = 0f,
    var sx: Float = 1f, var sy: Float = 1f, var rot: Float = 0f
)


/** 프레임 목록으로 만드는 일반 모션. events: (시간초, 명령) — "say:글", "boing:글", "fl:글", "shake", "flash", "pop" */
class Move(
    val label: String, val dur: Float, val fps: Float, val frames: List<Pose>,
    val w: Float = .45f, val move: Float = 0f, val back: Boolean = false,
    val says: List<String>? = null, val fx: String? = null, val fxEvery: Float = 0f, val fxKind: String = "",
    val spin: Float = 0f, val roll: Float = 0f, val jitter: Boolean = false,
    val hop: Float = 0f, val hopRate: Float = 0f, val float: Float = 0f,
    val switchAt: Float = 99f, val frames2: List<Pose>? = null,
    val events: List<Pair<Float, String>> = emptyList(), val chicken: Boolean = false, val spicy: Boolean = false
)


/** 캐릭터 정의 (부장님 / 여유곰) */
interface CharDef {
    val id: String
    val name: String
    val tapSay: String
    val sleepMin: Float
    val sleepMax: Float
    val specials: List<Pair<String, String>>
    val moves: LinkedHashMap<String, Move>
    fun render(p: Pose, f: Form, nowMs: Long): Bitmap
    /** 캐릭터 크기 배율 (수아는 아이라서 작게) */
    val scale: Float get() = 1f
    val menu: List<Pair<String, String>> get() = specials + moves.filter { !it.value.spicy || Engine.spicyOn }.map { (k, m) -> k to (if (m.chicken) "🐔 " else "") + m.label }
}

object PinkDef : CharDef {
    override val id = "pink"
    override val name = "진행해줘 부장님"
    override val tapSay = "진행해줘!"
    override val sleepMin = 6f
    override val sleepMax = 9f
    override val specials = listOf("idle" to "가만히", "walk" to "걷기", "run" to "달리기", "jump" to "점프", "sit" to "앉기",
        "sleep" to "잠자기", "wave" to "손인사", "dance" to "춤추기", "look" to "두리번", "stretch" to "기지개",
        "pen" to "펜 칼싸움", "ramen" to "몰래 라면", "power" to "파워업 변신", "chicken" to "치킨 변신", "cfly" to "🐔 하늘 날기")
    override val moves get() = PinkMoves.all
    override fun render(p: Pose, f: Form, nowMs: Long): Bitmap = PinkSprite.render(p, f, nowMs)
}

object BearDef : CharDef {
    override val id = "bear"
    override val name = "여유곰"
    override val tapSay = "곰발바닥~ 🐾"
    override val sleepMin = 10f
    override val sleepMax = 16f
    override val specials = listOf("idle" to "여유롭게 서있기", "walk" to "어슬렁 걷기", "run" to "달리기", "jump" to "점프", "sit" to "앉기",
        "sleep" to "푹 자기", "wave" to "손인사", "dance" to "곰 댄스", "look" to "두리번", "stretch" to "기지개",
        "kick" to "⚽ 공차기 (유리 깸)")
    override val moves get() = BearMoves.all
    override fun render(p: Pose, f: Form, nowMs: Long): Bitmap = BearSprite.render(p, f, nowMs)
}

object SuaDef : CharDef {
    override val id = "sua"
    override val name = "수아"
    override val tapSay = "엄마 안아줘!"
    override val sleepMin = 8f
    override val sleepMax = 12f
    override val scale = .82f
    override val specials = listOf("idle" to "가만히", "walk" to "콩콩 걷기", "run" to "뛰어다니기", "jump" to "점프", "sit" to "앉기",
        "sleep" to "쿨쿨 자기", "wave" to "손인사", "dance" to "신나는 춤", "look" to "두리번", "stretch" to "기지개")
    override val moves get() = SuaMoves.all
    override fun render(p: Pose, f: Form, nowMs: Long): Bitmap = SuaSprite.render(p)
}

object MomDef : CharDef {
    override val id = "mom"
    override val name = "수아 엄마"
    override val tapSay = "음, 그렇군."
    override val sleepMin = 7f
    override val sleepMax = 11f
    override val specials = listOf("idle" to "가만히", "walk" to "걷기", "run" to "뛰기", "jump" to "점프", "sit" to "앉기",
        "sleep" to "자기", "wave" to "손인사", "dance" to "춤", "look" to "두리번", "stretch" to "기지개")
    override val moves get() = MomMoves.all
    override fun render(p: Pose, f: Form, nowMs: Long): Bitmap = MomSprite.render(p)
}
