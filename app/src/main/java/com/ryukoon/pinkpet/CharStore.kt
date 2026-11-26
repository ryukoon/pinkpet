package com.ryukoon.pinkpet

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject

/** 직접 만든 캐릭터 (사진은 저장하지 않고, 뽑아낸 겉모습과 정보만 저장) */
class CustomChar(
    val id: String, var name: String, var female: Boolean, var kid: Boolean,
    var phrases: List<String>, var specs: List<String>, var look: Look
)

object CharStore {
    private var prefs: android.content.SharedPreferences? = null
    private var chars: List<CustomChar> = emptyList()
    private var defs: List<CustomDef> = emptyList()
    @Volatile var version = 0

    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences("chars", Context.MODE_PRIVATE)
        load()
        if (chars.isEmpty()) {
            // 처음 실행: 예시 캐릭터 하나
            save(CustomChar("c" + System.currentTimeMillis(), "도트", false, false, listOf("안녕!", "오늘도 화이팅!", "배고프다~"), listOf("달리기", "먹방·간식"), Look()))
        }
    }

    fun list(): List<CustomChar> = chars
    fun defs(): List<CustomDef> = defs
    fun get(id: String?): CustomChar? = chars.firstOrNull { it.id == id }

    fun save(c: CustomChar) {
        chars = if (chars.any { it.id == c.id }) chars.map { if (it.id == c.id) c else it } else chars + c
        persist()
    }
    fun delete(id: String) { chars = chars.filter { it.id != id }; persist() }

    private fun persist() {
        val arr = JSONArray()
        for (c in chars) {
            val l = c.look
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("female", c.female); put("kid", c.kid)
                put("phrases", JSONArray(c.phrases)); put("specs", JSONArray(c.specs))
                put("hair", l.hair); put("bangs", l.bangs); put("hairColor", l.hairColor); put("skin", l.skin)
                put("top", l.top); put("topColor", l.topColor); put("top2Color", l.top2Color)
                put("bottom", l.bottom); put("bottomColor", l.bottomColor); put("shoeColor", l.shoeColor); put("acc", l.acc)
            })
        }
        prefs?.edit()?.putString("list", arr.toString())?.apply()
        rebuild()
    }

    private fun load() {
        val s = prefs?.getString("list", null) ?: run { rebuild(); return }
        val out = ArrayList<CustomChar>()
        try {
            val arr = JSONArray(s)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                fun strs(k: String): List<String> { val a = o.optJSONArray(k) ?: return emptyList(); return (0 until a.length()).map { a.getString(it) } }
                val female = o.optBoolean("female")
                out.add(CustomChar(o.getString("id"), o.getString("name"), female, o.optBoolean("kid"), strs("phrases"), strs("specs"),
                    Look(o.optString("hair", "short"), o.optBoolean("bangs", true), o.optInt("hairColor"), o.optInt("skin"), o.optString("top", "tshirt"),
                        o.optInt("topColor"), o.optInt("top2Color"), o.optString("bottom", "pants"), o.optInt("bottomColor"), o.optInt("shoeColor"), o.optString("acc", "none"), female)))
            }
        } catch (_: Exception) { }
        chars = out
        rebuild()
    }

    private fun rebuild() { defs = chars.map { CustomDef(it) }; version++ }
}

/** 직접 만든 캐릭터를 펫 엔진이 쓰는 형태로 */
class CustomDef(val c: CustomChar) : CharDef {
    override val id = c.id
    override val name = c.name
    override val tapSay: String get() = if (c.phrases.isNotEmpty()) c.phrases.random() else "안녕!"
    override val sleepMin = 7f
    override val sleepMax = 11f
    override val scale = if (c.kid) .86f else 1f
    override val specials = listOf("idle" to "가만히", "walk" to "걷기", "run" to "달리기", "jump" to "점프", "sit" to "앉기",
        "sleep" to "자기", "wave" to "손인사", "dance" to "춤", "look" to "두리번", "stretch" to "기지개") + KidsExtras.specials
    private val built = MoveBook.build(c)
    override val moves get() = built
    private val sprite = MakerSprite(c.look.copy(female = c.female))
    override fun render(p: Pose, f: Form, nowMs: Long): Bitmap = sprite.render(p)
}

/** 특기·말버릇에 맞춰 모션 80개 정도를 자동으로 모으기 */
object MoveBook {
    val specs = listOf("달리기", "수영", "농구·체육", "태권도", "노래·MC", "게임", "먹방·간식", "요리·살림", "공부·일", "춤", "잠꾸러기", "술(어른)")

    private fun pick(src: Map<String, Move>, vararg keys: String): List<Pair<String, Move>> = keys.mapNotNull { k -> src[k]?.let { k to it } }

    private fun base(): List<Pair<String, Move>> =
        pick(SiaMoves.all, "laugh", "okCall", "shrug", "thumbsup", "coolWalk", "highfive", "wink", "peace", "selfie", "phone", "music", "spin", "trip", "sneeze",
            "yawn", "think", "clap", "bow", "robot", "frog", "airplane", "hide", "cola", "icecream", "cupramen", "pout") +
        pick(WooMoves.all, "proud", "idea", "hiccup", "shy", "dino", "tag", "cartwheel", "handstand") +
        pick(SuaMoves.all, "bubbles", "balloon", "peekaboo", "cat", "surprised", "giggle", "vpose", "snowman", "umbrella") +
        pick(MomMoves.all, "sigh", "coffee", "eyeroll", "calm", "tired", "flower")

    private fun pack(spec: String): List<Pair<String, Move>> = when (spec) {
        "달리기" -> pick(SiaMoves.all, "sprint", "marathon", "startLine", "finish", "relay", "medal", "pant", "legStretch", "warmup", "stopwatch", "water", "lap")
        "수영" -> pick(SiaMoves.all, "swim", "backstroke", "dive", "goggles", "floatWater", "splash", "dryHair")
        "농구·체육" -> pick(SiaMoves.all, "dribble", "shoot", "ballSpin", "dodge", "longjump", "jumprope", "situp", "pe", "pushup", "badminton") + pick(WooMoves.all, "soccer", "ballHold")
        "태권도" -> pick(WooMoves.all, "frontKick", "sideKick", "spinKick", "jumpKick", "breakBoard", "poomsae", "sparring", "blackBelt", "goldMedal", "kiai", "tkdBow", "punch", "stance", "dobokOn")
        "노래·MC" -> pick(WooMoves.all, "mcIntro", "mcHype", "mcCount", "mcJoke", "mcRap", "mcInterview", "mcApplause", "mcBow", "sunglasses", "coolPose", "speech") + pick(SuaMoves.all, "sing")
        "게임" -> pick(SuaMoves.all, "roblox", "robloxWin", "robloxLose", "obby", "robloxDance", "oof", "youtube") + pick(WooMoves.all, "game")
        "먹방·간식" -> pick(SuaMoves.all, "chips", "conveni", "jelly", "lollipop", "chocomilk", "gimbap", "snackDance") + pick(SiaMoves.all, "malatang", "malaLevel", "spicy", "tanghulu") +
            pick(WooMoves.all, "beef", "beefDream", "ramen", "ramenCook", "brothShot", "full")
        "요리·살림" -> pick(MomMoves.all, "cook", "yasik", "dishes", "vacuum", "grocery", "shopping", "lunchbox")
        "공부·일" -> pick(MomMoves.all, "laptop", "phone", "drama") + pick(SuaMoves.all, "reading", "homework", "draw") + pick(SiaMoves.all, "bagCheck")
        "춤" -> pick(SuaMoves.all, "kpop", "robloxDance", "princess", "ballet") + pick(MomMoves.all, "momDance", "karaoke") + pick(SiaMoves.all, "kpop")
        "잠꾸러기" -> pick(SuaMoves.all, "bedtime", "sleepy") + pick(MomMoves.all, "nap") + pick(SiaMoves.all, "nap") + pick(WooMoves.all, "nap")
        "술(어른)" -> pick(MomMoves.all, "beer", "soju", "sojuBottle", "somaek", "wine", "makgeolli", "cheers", "tipsy", "hangover", "drunkSing", "fridge", "chimaek", "highball")
        else -> emptyList()
    }

    /** 다른 캐릭터 이름이 나오는 대사는 내 캐릭터 이름으로 */
    private fun Move.renamed(name: String): Move {
        fun r(s: String) = s.replace("MC 정우", "MC $name").replace("정우", name).replace("유정우", name)
        return Move(r(label), dur, fps, frames, w, move, back, says?.map { r(it) }, fx, fxEvery, fxKind, spin, roll, jitter, hop, hopRate, float, switchAt, frames2,
            events.map { it.first to r(it.second) }, chicken, spicy)
    }

    fun build(c: CustomChar): LinkedHashMap<String, Move> {
        val out = linkedMapOf<String, Move>()
        // 1) 말버릇 모션
        val poses = listOf(
            Pose(aR = "wave", mouth = "smile"), Pose(aL = "hip", aR = "hip", mouth = "open"), Pose(aR = "hold", eyes = "half", mouth = "flat"),
            Pose(aL = "out", aR = "out", mouth = "grin"), Pose(aL = "cross", aR = "cross", eyes = "closed", mouth = "smile"), Pose(aR = "up", mouth = "open", eyes = "sparkle")
        )
        c.phrases.filter { it.isNotBlank() }.take(12).forEachIndexed { i, ph ->
            val p1 = poses[i % poses.size]; val p2 = p1.copy(hy = 1, by = 13)
            out["say$i"] = Move("💬 $ph", 3f, 1.5f, listOf(p1, p2), w = 1.3f, says = listOf(ph))
        }
        // 2) 특기 모션
        for (s in c.specs) for ((k, m) in pack(s)) if (k !in out) out[k] = m.renamed(c.name)
        // 3) 기본 생활 모션
        for ((k, m) in base()) if (k !in out) out[k] = m.renamed(c.name)
        val extra = if (c.female) pick(SuaMoves.all, "princess", "shy") + pick(MomMoves.all, "makeup", "catEars") else pick(WooMoves.all, "flex", "salute")
        for ((k, m) in extra) { if (k !in out) out[k] = m.renamed(c.name) }
        // 4) 80개(기본 동작 포함)가 안 되면 다른 특기에서 채우기 (술은 제외)
        val target = 80 - 10 - KidsExtras.specials.size
        for (s in specs.filter { it != "술(어른)" && it !in c.specs }) {
            if (out.size >= target) break
            for ((k, m) in pack(s)) { if (out.size >= target) break; if (k !in out) out[k] = m.renamed(c.name) }
        }
        return out
    }
}
