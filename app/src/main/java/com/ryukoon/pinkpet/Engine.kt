package com.ryukoon.pinkpet

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Plat(val x1: Float, val x2: Float, val y: Float, val floor: Boolean)

class Floater(val text: String, val kind: String, val x: Float, val y: Float) { var age = 0f }
class Crack(val path: Path, val dash: Path) { var age = 0f }
class Shard(var x: Float, var y: Float, var vx: Float, var vy: Float, var rot: Float, val vr: Float, val pts: FloatArray, val groundY: Float) {
    var age = 0f; var landed = false; var landedAge = 0f
}

class ActDef(val label: String, val d0: Float, val d1: Float, val w: Float, val move: Float = 0f,
             val says: List<String>? = null, val fx: String? = null, val fxEvery: Float = 0f, val fxKind: String = "")

/** 펫의 움직임·모션·효과를 모두 계산하는 두뇌 */
class Engine(private val density: Float, private val sfx: Sfx, val def: CharDef, private val openMenu: () -> Unit) {
    companion object { @Volatile var spicyOn = false }
    // 나중에 말하기 (상호작용용)
    private val later = ArrayList<Pair<Float, String>>()
    fun sayLater(text: String, delay: Float) { later.add(clock + delay to text) }
    private val D = density
    var SC = 2f * D
    private val G get() = 2600f * D
    var W = 1000f; var H = 2000f
    var floor = Plat(-1e6f, 1e6f, 1500f, true)
    private var minX = 0f; private var maxX = 0f

    // --- 펫 상태 ---
    var x = 300f; var y = -40f; private var vx = 0f; private var vy = 0f
    var dir = 1; var state = "air"; private var airKind = "fall"
    var act = "idle"; private var t = 0f; private var dur = 3f; private var plat: Plat? = null
    var pose = Pose(); private var rot = 0f
    private var landT = 0f; private var landA = 0f; private var impact = 0f; private var rebound = false; private var squash = 0f
    private var last = ""; private var fxFrame = -1; private var launched = false
    private var jvx = 0f; private var jvy = 0f; private var edge: Boolean? = null; private var fxT = 0f
    var form = Form.NORMAL; private var formT = 0f; private var charge = 0f; private var awayT = 0f
    // 모션 내부 변수
    private var ph = 0; private var kt = 0f; private var sw = -1; private var ki = 0; private var ks = 0f; private var ks2 = 0f; private var flipIdx = -1; private var kf = -1

    // --- 효과 ---
    private var bubbleText = ""; private var bubbleT = 0f
    private val floaters = ArrayList<Floater>(); private val cracks = ArrayList<Crack>(); private val shards = ArrayList<Shard>()
    private var flashA = 0f; private var shakeT = 0f; private var shakeA = 0f

    // --- 드래그 ---
    private var dragging = false; private var dOx = 0f; private var dOy = 0f; private var dTx = 0f; private var dTy = 0f
    private var dSx = 0f; private var dSy = 0f; private var dMoved = 0f; private var dTime = 0f; private var dPrevPlat: Plat? = null
    private var dSaid = false; private var longFired = false
    private val hist = ArrayDeque<FloatArray>() // [time, x, y]
    private var clock = 0f

    val acts: LinkedHashMap<String, ActDef> = linkedMapOf(
        "idle" to ActDef("가만히", 2f, 4f, 3f),
        "walk" to ActDef("걷기", 2.5f, 5f, 3f, move = 70f),
        "run" to ActDef("달리기", 1.5f, 3f, 1.2f, move = 180f, fx = "💦", fxEvery = .6f),
        "jump" to ActDef("점프", .3f, .3f, 1.6f),
        "sit" to ActDef("앉기", 3f, 6f, 1f),
        "sleep" to ActDef("잠자기", def.sleepMin, def.sleepMax, .9f, fx = "z", fxEvery = .8f, fxKind = "z"),
        "kick" to ActDef("공차기", 99f, 99f, 1f),
        "wave" to ActDef("손인사", 1.8f, 2.6f, 1f, says = listOf("안녕! 👋", "반가워!", "하이~")),
        "dance" to ActDef("춤추기", 3f, 5f, 1f, fx = "♪", fxEvery = .5f),
        "look" to ActDef("두리번", 2.5f, 4f, 1.4f, says = listOf("?", "뭐지?", "음…")),
        "stretch" to ActDef("기지개", 2.2f, 2.8f, 1f, says = listOf("하암~")),
        "pen" to ActDef("펜 칼싸움", 99f, 99f, .9f),
        "power" to ActDef("파워업 변신", 99f, 99f, .6f),
        "chicken" to ActDef("치킨 변신", 99f, 99f, .6f),
        "ramen" to ActDef("몰래 라면", 8f, 8f, .8f),
        "cfly" to ActDef("하늘 날기", 8f, 12f, 2.5f)
    )
    private val chickenOnly = setOf("cfly")
    private val allowed: Set<String> = def.specials.map { it.first }.toSet()
    // 비행·일반 모션용 변수
    private var tx = 0f; private var ty = 0f; private var fvx = 0f; private var fvy = 0f; private var loopT = 0f; private var sayT = 0f; private var evIdx = 0
    val drawY get() = y - pose.lift * SC
    // 공
    private var bState = "none"; private var bx = 0f; private var bY = 0f; private var bgy = 0f; private var bvx = 0f; private var bvy = 0f
    private var brot = 0f; private var bCrashed = false; private var bT = 0f; private var kct = -1f
    private val ballBmp: android.graphics.Bitmap = android.graphics.Bitmap.createBitmap(8, 8, android.graphics.Bitmap.Config.ARGB_8888).also { b ->
        val rows = arrayOf("..XXXX..", ".XWWWWX.", "XWWBBWWX", "XWBBBBWX", "XWWBBWWX", "XBWWWWBX", ".XBWWBX.", "..XXXX..")
        for (yy in 0 until 8) for (xx in 0 until 8) {
            val ch = rows[yy][xx]
            if (ch != '.') b.setPixel(xx, yy, when (ch) { 'X' -> Color.parseColor("#2a1f3d"); 'W' -> Color.WHITE; else -> Color.parseColor("#3a3a48") })
        }
    }

    private fun rnd(a: Float, b: Float) = a + Random.nextFloat() * (b - a)
    private fun fl(v: Float) = kotlin.math.floor(v).toInt()

    fun setScreen(w: Float, h: Float, floorY: Float, gridPx: Float) {
        W = w; H = h; SC = gridPx
        floor = Plat(-1e6f, 1e6f, floorY, true)
        minX = 12 * SC; maxX = W - 12 * SC
        if (state != "drag" && state != "away") {
            x = x.coerceIn(minX, maxX)
            if (state != "air") goAir(0f, 0f, "fall")
        }
    }

    fun spawn(frac: Float = .5f) { x = (W * frac).coerceIn(minX, maxX); y = -40 * D; goAir(0f, 0f, "fall") }

    // ================= 다른 펫과의 상호작용 =================
    var onCrash: ((Float) -> Unit)? = null
    val stateName get() = state
    val actName get() = act
    val isChicken get() = form == Form.CHICKEN
    private val busyActs = setOf("pen", "kick", "power", "chicken", "cfly", "ramen")
    /** 다른 펫이 끼어들어도 되는 상태인지 */
    val canInteract get() = (state == "act" && act !in busyActs && def.moves[act]?.events?.isEmpty() != false) || state == "land"
    fun has(name: String) = (acts.containsKey(name) && name in allowed) || def.moves.containsKey(name)
    fun interact(name: String, faceX: Float, text: String?) {
        if (!has(name) || (state != "act" && state != "land")) return
        if (plat == null) plat = floor
        y = plat?.y ?: floor.y
        startAct(name)
        dir = if (faceX >= x) 1 else -1
        if (text != null) say(text, 1.8f)
    }
    fun bump(fromX: Float) {
        if (state == "drag" || state == "away") return
        val d = if (x >= fromX) 1 else -1
        dir = -d
        goAir(d * 320 * D, -560 * D, "fall")
        floater("띠용!", "boing"); sfx.boing(.8f); sfx.vibrate(20)
    }
    fun talk(text: String) { if (state != "away") say(text, 1.6f) }
    fun popAt(text: String, px: Float, py: Float) { floaters.add(Floater(text, "boing", px, py)) }

    // ================= 말풍선 / 떠오르는 글자 =================
    private fun headX() = x
    private fun headY() = drawY - 21 * SC
    private fun say(text: String, sec: Float) { bubbleText = text; bubbleT = sec }
    private fun floater(text: String, kind: String = "", dx: Float = 0f) { floaters.add(Floater(text, kind, headX() + dx * D, headY() - 20 * D)) }
    private fun flash(a: Float) { flashA = max(flashA, a) }
    private fun shake(a: Float) { shakeA = a * D; shakeT = .26f }

    // ================= 모션 시작 =================
    fun force(name: String) {
        if (!(acts.containsKey(name) && name in allowed) && !def.moves.containsKey(name)) return
        if (state == "act" || state == "land") { if (plat == null) plat = floor; startAct(name) }
    }

    private fun startAct(name: String) {
        state = "act"; act = name; t = 0f; fxFrame = -1; launched = false; edge = null; last = name; evIdx = 0; fxT = 0f
        val needChicken = name in chickenOnly || def.moves[name]?.chicken == true
        if (needChicken && form != Form.CHICKEN) {
            form = Form.CHICKEN; formT = 40f; flash(.5f)
            repeat(4) { floater("💨", "", rnd(-24f, 24f)) }; floater("펑!", "boing"); sfx.tone("square", 300f, 150f, 600f, .35f, .22f)
        }
        val mv = def.moves[name]
        if (mv != null) {
            dur = mv.dur; ph = 0; kt = 0f
            mv.says?.let { say(it.random(), 1.6f) }
            return
        }
        val a = acts.getValue(name)
        dur = rnd(a.d0, a.d1)
        if (name == "walk" || name == "run") dir = if (Random.nextBoolean()) -1 else 1
        ph = 0; kt = 0f; kct = -1f; sw = -1; ki = 0; ks = 0f; ks2 = 0f; flipIdx = -1; kf = -1
        a.says?.let { say(it.random(), 1.6f) }
        if (name == "jump") planJump()
        if (name == "cfly") {
            plat = null; fvx = 0f; fvy = -260 * D; ki = 0; ks = 0f; ks2 = 0f; loopT = 0f; sayT = 0f
            say("날아오른다아~!!", 1.4f); floater("💨"); sfx.jump()
        }
    }

    private fun weightOf(k: String): Float {
        val ch = form == Form.CHICKEN
        val mv = def.moves[k]
        if (mv != null && mv.spicy && !spicyOn) return 0f
        if (mv != null) return if (mv.chicken) (if (ch) mv.w else 0f) else (if (ch) mv.w * .4f else mv.w)
        if (k in chickenOnly) return if (ch) acts.getValue(k).w else 0f
        return acts.getValue(k).w
    }

    private fun nextAct() {
        val keys = (acts.keys.filter { it in allowed } + def.moves.keys).filter { it != last && !(it == "kick" && (plat?.floor != true || bState != "none")) }
        val total = keys.sumOf { weightOf(it).toDouble() }.toFloat()
        var r = Random.nextFloat() * total
        for (k in keys) { r -= weightOf(k); if (r <= 0f) { startAct(k); return } }
        startAct("idle")
    }

    private fun planJump() {
        val tx = (x + dir * rnd(40f, 110f) * D).coerceIn(minX, maxX)
        val hgt = 55f * D * rnd(.8f, 1.6f)
        jvy = -sqrt(2 * G * hgt)
        val tt = 2 * sqrt(2 * hgt / G)
        jvx = (tx - x) / tt
        if (abs(jvx) > 5) dir = sign(jvx).toInt()
    }

    private fun goAir(nvx: Float, nvy: Float, kind: String) { state = "air"; vx = nvx; vy = nvy; plat = null; airKind = kind }

    private fun groundMove(sp: Float, dt: Float) {
        val nx = x + dir * sp * D * dt
        if (nx < minX || nx > maxX) { dir = -dir; return }
        x = nx
    }

    private fun land(p: Plat) {
        val imp = vy / D
        y = p.y
        if (imp > 1150 && !rebound) {
            rebound = true; vy = -vy * .33f; vx *= .5f; squash = .14f
            sfx.boing(1f); floater("띠용!", "boing"); sfx.vibrate(25); return
        }
        state = "land"; plat = p; vx = 0f; vy = 0f; landT = 0f
        impact = if (rebound) max(imp, 1200f) else imp
        landA = (imp / 2000f).coerceIn(.14f, .45f)
        if (imp > 420 || rebound) {
            floater(if (rebound) "띠요옹~" else "띠용~", "boing")
            sfx.boing(min(1f, imp / 1200f + .3f)); sfx.vibrate(18)
        }
        rebound = false
    }

    // ================= 터치 =================
    fun onDown(rx: Float, ry: Float) {
        dragging = true; dOx = x - rx; dOy = y - ry; dTx = x; dTy = y; dSx = rx; dSy = ry
        dMoved = 0f; dTime = 0f; dPrevPlat = plat; dSaid = false; longFired = false; hist.clear()
        state = "drag"; plat = null; rot = 0f; charge = 0f
        sfx.grab()
    }

    fun onMove(rx: Float, ry: Float) {
        if (!dragging || longFired) return
        dTx = rx + dOx; dTy = ry + dOy
        dMoved = max(dMoved, hypot(rx - dSx, ry - dSy))
        hist.addLast(floatArrayOf(clock, dTx, dTy)); if (hist.size > 12) hist.removeFirst()
        if (dMoved > 14 * D && !dSaid) { dSaid = true; say(listOf("으앗!", "어디 가?", "놔줘~", "높다아").random(), 1.2f) }
    }

    fun onUp() {
        if (!dragging) return
        dragging = false
        if (longFired) return
        val prev = dPrevPlat
        if (dMoved < 10 * D && dTime < .35f && prev != null) {
            plat = prev; y = prev.y; startAct("wave"); say(def.tapSay, 1.3f); floater("❤"); sfx.pop(); return
        }
        var nvx = 0f; var nvy = 0f
        val recent = hist.filter { clock - it[0] < .11f }
        if (recent.size > 1) {
            val a = recent.first(); val b = recent.last(); val dt = max(.016f, b[0] - a[0])
            nvx = (b[1] - a[1]) / dt; nvy = (b[2] - a[2]) / dt
        }
        nvx = nvx.coerceIn(-1500 * D, 1500 * D); nvy = nvy.coerceIn(-2000 * D, 1500 * D)
        if (nvy < -900 * D) say("으아아~", 1.2f)
        if (abs(nvx) > 30 * D) dir = sign(nvx).toInt()
        goAir(nvx, nvy, "fall")
    }

    // ================= 매 프레임 =================
    fun update(dtIn: Float, nowMs: Long) {
        val dt = min(.033f, dtIn)
        clock += dt
        var p: Pose? = null

        if (state == "act") {
            t += dt
            val mv = def.moves[act]
            if (mv != null) {
                moveUpdate(mv, dt)
                if (state == "act") {
                    p = movePose(mv, t)
                    if (t >= dur) nextAct()
                }
            } else {
                val a = acts.getValue(act)
                if (a.move > 0) groundMove(a.move, dt)
                actUpdate(dt)
                if (state == "act") {
                    p = actPose(t)
                    if (a.fx != null) { fxT += dt; if (fxT > a.fxEvery) { fxT = 0f; floater(a.fx, a.fxKind, rnd(-10f, 14f)) } }
                    if (state == "act" && t >= dur) nextAct()
                }
            }
        }
        if (state == "drag") {
            dTime += dt
            if (!longFired && dMoved < 10 * D && dTime > .6f) {
                longFired = true; dragging = false
                val prev = dPrevPlat ?: floor
                plat = prev; y = prev.y; startAct("idle"); say("메뉴 열게요!", 1f); openMenu()
            } else {
                val k = min(1f, dt * 22)
                val ox = x
                x += (dTx - x) * k; y += (dTy - y) * k
                x = x.coerceIn(minX, maxX)
                val v = (x - ox) / max(dt, .001f) / D
                rot += ((-v * .04f).coerceIn(-30f, 30f) * dir - rot) * min(1f, dt * 10)
                val f = ((nowMs / 260) % 2).toInt()
                p = Pose(legs = if (f == 1) "dangle" else "tuck", aL = "up", aR = "up", mouth = "o", eyes = "wide", sy = 1.06f, sx = .96f, rot = rot)
            }
        }
        if (state == "air") {
            vy += G * dt; x += vx * dt; y += vy * dt
            if (x < minX) { x = minX; vx = -vx * .5f; dir = 1 }
            if (x > maxX) { x = maxX; vx = -vx * .5f; dir = -1 }
            if (vy > 0 && y >= floor.y) land(floor)
            if (state == "air") {
                val f = ((nowMs / 90) % 2).toInt()
                p = if (airKind == "jump" && vy < 0) Pose(aL = "up", aR = "up", legs = "tuck", mouth = "smile", sy = 1.08f, sx = .94f)
                else Pose(aL = if (f == 1) "up" else "out", aR = if (f == 1) "out" else "up", legs = if (f == 1) "tuck" else "dangle",
                    mouth = "open", eyes = "wide", rot = (vx / D * .012f).coerceIn(-15f, 15f) * dir)
                if (squash > 0) { squash -= dt; p.sy = .7f; p.sx = 1.3f }
            }
        }
        if (state == "land") {
            landT += dt
            val e = exp(-5.5f * landT); val sy = 1 - landA * e * cos(21 * landT); val sx = 1 + (1 - sy) * .8f
            p = if (landT < .2f) Pose(legs = "walkA", aL = "out", aR = "out", eyes = "closed", sy = sy, sx = sx)
            else Pose(mouth = "smile", blush = true, sy = sy, sx = sx)
            if (landT > .95f) {
                if (impact > 1500) { startAct("look"); floater("💫"); say("어지러워…", 1.4f) } else nextAct()
            }
        }
        if (state == "away") {
            awayT -= dt; p = pose
            if (awayT <= 0) { x = rnd(minX, maxX); y = -40 * D; goAir(0f, 0f, "fall"); say("…휴", 1f) }
        }
        if (form != Form.NORMAL && state != "drag" && !(state == "act" && act == "cfly")) {
            formT -= dt
            if (formT <= 0) { form = Form.NORMAL; repeat(3) { floater("💨", "", rnd(-20f, 20f)) }; say("원래대로…", 1.2f) }
        }
        val pp = p ?: pose
        if (form == Form.CHICKEN && state == "act" && act in listOf("idle", "look", "sit", "chicken")) pp.box = true
        pose = pp

        ballUpdate(dt)
        // 효과 갱신
        if (later.isNotEmpty()) { val due = later.filter { it.first <= clock }; if (due.isNotEmpty()) { later.removeAll(due.toSet()); if (state != "away") say(due.last().second, 1.8f) } }
        if (bubbleT > 0) bubbleT -= dt
        floaters.forEach { it.age += dt }; floaters.removeAll { it.age > (if (it.kind == "z") 1.8f else 1.2f) }
        cracks.forEach { it.age += dt }; cracks.removeAll { it.age > 4.3f }
        for (s in shards) {
            s.age += dt
            if (!s.landed) {
                s.vy += G * .7f * dt; s.x += s.vx * dt; s.y += s.vy * dt; s.rot += s.vr * dt
                if (s.y >= s.groundY && s.vy > 0) { s.y = s.groundY; s.landed = true }
            } else s.landedAge += dt
        }
        shards.removeAll { it.landedAge > .5f || it.age > 3f }
        if (flashA > 0) flashA = max(0f, flashA - dt * 2.4f)
        if (shakeT > 0) shakeT -= dt
    }

    // ================= 모션별 동작 =================
    private fun actUpdate(dt: Float) {
        when (act) {
            "pen" -> {
                kt += dt
                when (ph) {
                    0 -> if (kt > .7f) { ph = 1; kt = 0f; say("얍! 얍!", 1.2f) }
                    1 -> {
                        val s = fl(kt / .22f)
                        if (s != sw) { sw = s; sfx.swish(); if (s % 2 == 0) floater("슉!", "", dir * 14f) }
                        if (kt > 2f) { ph = 2; kt = 0f; say("필살… 찌르기!", 1.1f) }
                    }
                    2 -> if (kt > .7f) { ph = 3; kt = 0f; sfx.stab(); shatter(x + dir * 9 * SC, y - 11 * SC, true); onCrash?.invoke(x) }
                    3 -> if (kt > .7f) { ph = 4; kt = 0f; say("어… 어떡해!!", 1.4f) }
                    4 -> {
                        ks2 += dt; if (ks2 > .45f) { ks2 = 0f; floater("💦", "", rnd(-12f, 12f)) }
                        val s = fl(kt / .18f); if (s != flipIdx) { flipIdx = s; dir = -dir }
                        if (kt > 1.6f) { ph = 5; kt = 0f; dir = if (x > W / 2) 1 else -1; say(listOf("내가 안 그랬어!", "도망쳐~!").random(), 1.4f) }
                    }
                    else -> {
                        x += dir * 340 * D * dt
                        if (x < -40 * D || x > W + 40 * D) { state = "away"; awayT = 1.4f; bubbleT = 0f }
                    }
                }
            }
            "power" -> {
                kt += dt
                if (ph == 0) {
                    charge = min(1f, kt / 1.8f); ks += dt
                    if (ks > .22f) { ks = 0f; shake(3f); sfx.tone("saw", 70 + kt * 60, 90 + kt * 80, 60 + kt * 60, .22f, .15f); if (kt > .6f) floater("⚡", "", rnd(-20f, 20f)) }
                    if (ki == 0) { ki = 1; say("흐으읍…", 1f) }
                    if (kt > 1f && ki == 1) { ki = 2; say("하아아아아!!", 1f) }
                    if (kt > 1.8f) {
                        ph = 1; kt = 0f; charge = 0f; form = Form.POWER; formT = 30f
                        flash(.8f); shake(9f); sfx.tone("square", 200f, 900f, 1200f, .5f, .25f); sfx.vibrate(120)
                        say("파워업 완료!", 1.4f); floater("💥")
                    }
                } else if (kt > 1.6f) nextAct()
            }
            "chicken" -> {
                kt += dt
                if (ph == 0) {
                    ks += dt
                    if (ks > .15f) { ks = 0f; floater("✨", "", rnd(-22f, 22f)); sfx.tone("tri", 600 + kt * 600, 900 + kt * 600, 700 + kt * 600, .12f, .1f) }
                    if (kt > 1.1f) {
                        ph = 1; kt = 0f; form = Form.CHICKEN; formT = 40f; flash(.6f)
                        repeat(4) { floater("💨", "", rnd(-24f, 24f)) }; floater("펑!", "boing")
                        sfx.tone("square", 300f, 150f, 600f, .35f, .22f); say("치킨 타임~ 🍗", 1.6f)
                    }
                } else if (kt > 2.4f) nextAct()
            }
            "ramen" -> {
                val ev = listOf(.05f to "쉿… 아무도 없지?", 2.1f to "후루룩", 4.6f to "…!", 5.5f to "후루룩 쩝쩝", 7.1f to "꺼억~ 😋")
                while (ki < ev.size && t >= ev[ki].first) { say(ev[ki].second, if (ki == 0) 1.6f else 1f); ki++ }
                if (t > 1.8f && t < 7.1f && !(t > 4.6f && t < 5.4f)) {
                    ks += dt; if (ks > .55f) { ks = 0f; floater("~", "z", rnd(-12f, 12f)) }
                    val f = fl(t * 3) % 3; if (f != kf) { kf = f; if (f == 0) sfx.slurp() }
                }
            }
            "kick" -> {
                kt += dt
                when (ph) {
                    0 -> {
                        if (kt > .35f && bState == "none") { bState = "rest"; bx = x + dir * 10 * SC; bY = plat?.y ?: floor.y; bgy = bY; bvx = 0f; bvy = 0f; bCrashed = false; bT = 0f }
                        if (kt > .75f) { ph = 1; kt = 0f }
                    }
                    1 -> { val nx = x - dir * 60 * D * dt; if (nx > minX && nx < maxX) x = nx; if (kt > .9f) { ph = 2; kt = 0f; say("간다~!", 1f) } }
                    2 -> {
                        x += dir * 230 * D * dt
                        if (abs(bx - x) <= 8 * SC || kt > 2f) { ph = 3; kt = 0f; bState = "fly"; bvx = dir * 1150 * D; bvy = -420 * D; sfx.stab() }
                    }
                    3 -> if (kt > .3f) { ph = 4; kt = 0f; kct = -1f }
                    4 -> {
                        if (bCrashed && kct < 0) { kct = 0f; say("헉!!", 1.2f) }
                        if (kct >= 0) kct += dt
                        if (kct > 1.2f) { ph = 5; kt = 0f; say(listOf("어떡해!! 유리가…", "내가 안 그랬어…!").random(), 1.4f) }
                        else if (kt > 4f) nextAct()
                    }
                    5 -> {
                        ks2 += dt; if (ks2 > .4f) { ks2 = 0f; floater("💦", "", rnd(-12f, 12f)) }
                        val s = fl(kt / .18f); if (s != flipIdx) { flipIdx = s; dir = -dir }
                        if (kt > 1.4f) { ph = 6; kt = 0f; dir = if (x > W / 2) 1 else -1; say("도망쳐~!! 🐻💨", 1.4f) }
                    }
                    else -> {
                        x += dir * 340 * D * dt
                        if (x < -40 * D || x > W + 40 * D) { state = "away"; awayT = 1.6f; bubbleT = 0f }
                    }
                }
            }
            "cfly" -> {
                ks += dt; ks2 += dt; sayT += dt
                if (ki == 0 || hypot(tx - x, ty - y) < 30 * D || ks > 2.6f) {
                    ki = 1; ks = 0f; tx = rnd(minX, maxX); ty = rnd(H * .12f, floor.y - 90 * D)
                }
                val dx = tx - x; val dy = ty - y; val d = max(1f, hypot(dx, dy)); val sp = 250 * D
                val k = min(1f, dt * 2.5f)
                fvx += (dx / d * sp - fvx) * k; fvy += (dy / d * sp - fvy) * k
                x += fvx * dt; y += fvy * dt + sin(clock * 9) * 30 * D * dt
                x = x.coerceIn(minX, maxX); y = y.coerceIn(H * .08f, floor.y)
                if (abs(fvx) > 25 * D) dir = sign(fvx).toInt()
                if (ks2 > .38f) { ks2 = 0f; floater(listOf("💕", "✨", "♪", "💖", "🌟", "🪶").random(), "", rnd(-16f, 16f)) }
                if (sayT > 2.6f) { sayT = 0f; say(listOf("날 수 있다아~!!", "꼬끼오~! 🐔", "행복해~", "하늘 최고!", "위이잉~", "치킨 아니고 비행기다!").random(), 1.6f) }
                if (loopT > 0) loopT -= dt else if (Random.nextFloat() < dt * .18f) { loopT = .9f; floater("빙그르르~", "boing") }
                if (t >= dur) { loopT = 0f; goAir(fvx * .5f, -150 * D, "fall"); say("착지할게~!", 1.1f) }
            }
            "jump" -> if (t > .18f && !launched) { launched = true; sfx.jump(); goAir(jvx, jvy, "jump") }
        }
    }

    private fun actPose(t: Float): Pose {
        when (act) {
            "idle" -> { val b = fl(t * 1.6f) % 2; return Pose(by = 12 + b, hy = b, eyes = if (t % 2.7f < .13f) "closed" else "open", look = if (t > dur * .6f) -1 else 1) }
            "walk" -> { val f = fl(t * 8) % 4; return Pose(legs = arrayOf("walkA", "stand", "walkB", "stand")[f], hy = f % 2, by = 12 + f % 2, aL = if (f == 0) "hip" else "down", aR = if (f == 2) "hip" else "down") }
            "run" -> { val f = fl(t * 12) % 2; return Pose(legs = if (f == 1) "runA" else "walkB", hy = f, by = 12 + f, aL = if (f == 1) "out" else "hip", aR = if (f == 1) "hip" else "out", mouth = "open", hx = 1) }
            "jump" -> return Pose(by = 13, hy = 2, legs = "tuck", aL = "out", aR = "out", sy = .85f, sx = 1.1f)
            "sit" -> return Pose(by = 14, hy = 2, ly = 20, legs = "sit", eyes = if (t % 3f < .13f) "closed" else "open", mouth = if (t > 1.5f) "smile" else "flat", blush = t > 1.5f)
            "sleep" -> return Pose(lying = true, eyes = "closed", mouth = if (fl(t * 1.2f) % 2 == 1) "o" else "flat")
            "wave" -> { val f = fl(t * 5) % 2; return Pose(aR = if (f == 1) "wave" else "up", mouth = "smile", blush = true) }
            "dance" -> {
                val f = fl(t * 4) % 4
                if (f != fxFrame) { fxFrame = f; if (f % 2 == 0) dir = -dir }
                val o = f % 2 == 1
                return Pose(aL = if (o) "up" else "out", aR = if (o) "out" else "up", legs = if (o) "walkB" else "walkA", by = 12 + f % 2, hy = f % 2,
                    mouth = "smile", eyes = if (f == 3) "closed" else "open", sy = if (o) .94f else 1.04f)
            }
            "look" -> { val s = fl(t / .7f) % 2; return Pose(look = if (s == 1) -1 else 1, hx = if (s == 1) -1 else 1, eyes = "wide", mouth = "o") }
            "stretch" -> {
                if (t < 1.5f) { val q = sin(min(t / 1.5f, 1f) * PI.toFloat()); return Pose(aL = "up", aR = "up", eyes = "closed", mouth = "open", sy = 1 + .1f * q, sx = 1 - .05f * q, legs = "dangle") }
                return Pose(mouth = "smile", blush = true)
            }
            "pen" -> {
                val f = fl(kt * 9) % 4
                return when (ph) {
                    0 -> Pose(aR = "out", pen = "out", mouth = "smile")
                    1 -> when (((sw % 3) + 3) % 3) {
                        0 -> Pose(aR = "up", pen = "up", mouth = "open", legs = "walkA", hx = 1)
                        1 -> Pose(aR = "out", pen = "out")
                        else -> Pose(aR = "out", pen = "down", mouth = "open", legs = "walkA", by = 13, hy = 1, hx = 1)
                    }
                    2 -> Pose(aR = "hip", aL = "out", by = 13, hy = 1, sx = .9f, sy = 1.05f, eyes = "wide", hx = -1)
                    3 -> Pose(aR = "out", pen = "stab", legs = "walkA", mouth = "open", eyes = "wide", sx = 1.3f, sy = 1.3f, hx = 1)
                    4 -> { val j = fl(kt * 14) % 2; Pose(aL = if (j == 1) "up" else "out", aR = if (j == 1) "out" else "up", eyes = "wide", mouth = "open", hx = if (j == 1) 1 else -1, legs = if (j == 1) "walkA" else "walkB", by = 12 + j, hy = j) }
                    else -> Pose(legs = if (f % 2 == 1) "runA" else "walkB", hy = f % 2, by = 12 + f % 2, aL = "up", aR = "up", mouth = "open", eyes = "wide", hx = 1)
                }
            }
            "power" -> {
                if (ph == 0) { val j = fl(t * 20) % 2; return Pose(by = 13, hy = 1, aL = "hip", aR = "hip", eyes = "closed", mouth = "open", hx = if (j == 1) 1 else -1, sx = 1 + .05f * j) }
                return Pose(aL = "out", aR = "out", mouth = "smile", legs = "walkA", sy = 1.06f)
            }
            "chicken" -> {
                if (ph == 0) return Pose(sx = cos(t * 22) * (1 - t * .3f), aL = "up", aR = "up", eyes = "closed", mouth = "open")
                return Pose(box = true, mouth = "smile", hy = fl(t * 3) % 2)
            }
            "kick" -> {
                val f = fl(kt * 10) % 4
                return when (ph) {
                    0 -> Pose(by = 13, hy = 2, aR = "out", look = 1)
                    1 -> Pose(legs = arrayOf("walkB", "stand", "walkA", "stand")[f], mouth = "smile", look = 1)
                    2 -> Pose(legs = if (f % 2 == 1) "runA" else "walkB", hy = f % 2, by = 12 + f % 2, aL = if (f % 2 == 1) "out" else "hip", aR = if (f % 2 == 1) "hip" else "out", mouth = "open", hx = 1)
                    3 -> Pose(legs = "kick", aL = "out", aR = "up", mouth = "open", hx = 1, sx = 1.06f)
                    4 -> if (kct < 0) Pose(eyes = "wide", mouth = "o") else Pose(eyes = "wide", mouth = "open", aL = "up", aR = "up", sy = 1.06f, sx = .95f, hx = fl(kt * 14) % 2)
                    5 -> { val j = fl(kt * 14) % 2; Pose(aL = if (j == 1) "up" else "out", aR = if (j == 1) "out" else "up", eyes = "wide", mouth = "open", hx = if (j == 1) 1 else -1, legs = if (j == 1) "walkA" else "walkB") }
                    else -> Pose(legs = if (f % 2 == 1) "runA" else "walkB", hy = f % 2, by = 12 + f % 2, aL = "up", aR = "up", mouth = "open", eyes = "wide", hx = 1)
                }
            }
            "cfly" -> {
                val f = ((clock * 10).toInt()) % 2
                var r = (fvx / D * .03f).coerceIn(-15f, 15f) * dir
                if (loopT > 0) r += (1 - loopT / .9f) * 360f
                return Pose(aL = if (f == 1) "up" else "out", aR = if (f == 1) "out" else "up", legs = "tuck",
                    eyes = if ((clock * .7f).toInt() % 3 == 2) "sparkle" else "closed", mouth = if (f == 1) "open" else "smile", blush = true, rot = r, pivot = 11f)
            }
            "ramen" -> {
                if (t < 1.8f) { val s = fl(t / .35f) % 2; return Pose(look = if (s == 1) -1 else 1, hx = if (s == 1) -1 else 1, eyes = "wide", by = 13, hy = 1) }
                if (t > 4.6f && t < 5.4f) return Pose(by = 14, hy = 2, ly = 20, legs = "sit", pot = true, look = if (fl(t / .2f) % 2 == 1) -1 else 1, eyes = "wide", mouth = "o")
                if (t >= 7.1f) return Pose(by = 14, hy = 2, ly = 20, legs = "sit", pot = true, eyes = "closed", mouth = "smile", blush = true)
                val f = fl(t * 3) % 3
                return if (f == 0) Pose(by = 14, hy = 2, ly = 20, legs = "sit", pot = true, eat = true, aR = "none", mouth = "open", eyes = "closed")
                else Pose(by = 14, hy = if (f == 1) 2 else 3, ly = 20, legs = "sit", pot = true, blush = true)
            }
        }
        return Pose()
    }

    private fun ballUpdate(dt: Float) {
        if (bState == "none") return
        val r = 4 * SC
        if (bState == "rest") {
            if (act != "kick" || state != "act") { bT += dt; if (bT > 3f) bState = "none" }
            return
        }
        bvy += G * dt; bx += bvx * dt; bY += bvy * dt; brot += bvx / D * dt * 1.6f
        if (bY >= bgy) { bY = bgy; bvy = -bvy * .45f; bvx *= .85f; if (abs(bvy) < 70 * D) bvy = 0f }
        if (!bCrashed && (bx >= W - r || bx <= r)) {
            val right = bx >= W - r
            bx = if (right) W - r else r; bCrashed = true
            shatter(if (right) W else 0f, bY - r, false)
            onCrash?.invoke(bx)
            bvx = -bvx * .22f; bvy = -180 * D
        }
        if (bCrashed) { bx = bx.coerceIn(r, W - r); bT += dt; if (bT > 2.8f) bState = "none" }
    }

    // ================= 일반 모션 =================
    private fun moveUpdate(mv: Move, dt: Float) {
        if (mv.move > 0f) {
            val sd = if (mv.back) -dir else dir
            val nx = x + sd * mv.move * D * dt
            if (nx < minX || nx > maxX) dir = -dir else x = nx
        }
        while (evIdx < mv.events.size && t >= mv.events[evIdx].first) {
            val cmd = mv.events[evIdx].second; evIdx++
            when {
                cmd.startsWith("say:") -> say(cmd.substring(4), 1.4f)
                cmd.startsWith("boing:") -> { floater(cmd.substring(6), "boing"); sfx.boing(.6f) }
                cmd.startsWith("fl:") -> floater(cmd.substring(3), "", rnd(-10f, 10f))
                cmd == "shake" -> { shake(5f); sfx.vibrate(30) }
                cmd == "flash" -> flash(.5f)
                cmd == "pop" -> sfx.pop()
            }
        }
        val fx = mv.fx
        if (fx != null) { fxT += dt; if (fxT > mv.fxEvery) { fxT = 0f; floater(fx, mv.fxKind, rnd(-12f, 14f)) } }
    }

    private fun movePose(mv: Move, t: Float): Pose {
        val list = if (t >= mv.switchAt && mv.frames2 != null) mv.frames2 else mv.frames
        val p = list[fl(t * mv.fps) % list.size].copy()
        if (mv.spin != 0f) p.sx *= cos(t * mv.spin)
        if (mv.roll != 0f) { p.rot = t * mv.roll; p.pivot = 11f }
        if (mv.jitter) p.hx = Random.nextInt(-1, 2)
        if (mv.hop > 0f) p.lift = abs(sin(t * mv.hopRate * PI.toFloat())) * mv.hop
        if (mv.float > 0f) p.lift = mv.float * (.5f - .5f * cos(min(t, 2f) * PI.toFloat() / 2f)) + sin(t * 2f) * 1.2f
        return p
    }

    // ================= 화면 깨짐 =================
    private fun shatter(cx: Float, cy: Float, big: Boolean) {
        val r = if (big) min(300 * D, W * .6f) else min(170 * D, W * .42f)
        val path = Path(); val dash = Path()
        val nc = if (big) 16 else 12
        for (i in 0 until nc) {
            val a = i.toFloat() / nc * 2 * PI.toFloat() + Random.nextFloat() * .4f
            path.moveTo(cx, cy)
            for (j in 1..4) {
                val l = r * j / 4 * (.65f + Random.nextFloat() * .5f); val aa = a + (Random.nextFloat() - .5f) * .35f
                path.lineTo(cx + cos(aa) * l, cy + sin(aa) * l)
            }
        }
        for (rr in if (big) floatArrayOf(22f, 55f, 100f, 160f, 230f) else floatArrayOf(18f, 42f, 78f, 120f)) {
            val rd = rr * D
            for (i in 0 until 12) {
                if (i % 2 == 1) continue
                val a0 = i / 12f * 2 * PI.toFloat(); val a1 = (i + 1) / 12f * 2 * PI.toFloat()
                val q0 = rd * (.8f + Random.nextFloat() * .4f); val q1 = rd * (.8f + Random.nextFloat() * .4f)
                dash.moveTo(cx + cos(a0) * q0, cy + sin(a0) * q0); dash.lineTo(cx + cos(a1) * q1, cy + sin(a1) * q1)
            }
        }
        cracks.add(Crack(path, dash))
        repeat(if (big) 26 else 16) {
            val sz = rnd(6f, 16f) * D
            val pts = floatArrayOf(rnd(0f, .4f) * sz, 0f, sz, rnd(.1f, .6f) * sz, rnd(.2f, .8f) * sz, sz, 0f, rnd(.3f, .9f) * sz)
            shards.add(Shard(cx, cy, rnd(-260f, 260f) * D, -rnd(150f, 600f) * D, 0f, rnd(-500f, 500f), pts, floor.y - rnd(0f, 10f) * D))
        }
        flash(.55f); shake(6f); sfx.crash(); sfx.vibrate(40, 30, 90)
    }

    // ================= 그리기 =================
    private val bmpPaint = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    private val tmpRect = RectF()

    /** 펫 창 안에 캐릭터를 그린다. (ox, oy) = 창의 화면 좌표 */
    fun drawPet(c: Canvas, ox: Float, oy: Float, nowMs: Long) {
        val p = pose
        val bmp = def.render(p, form, nowMs)
        c.save()
        c.translate(x - ox, drawY - oy)
        c.scale(dir * p.sx, p.sy)
        if (p.pivot != 0f) { c.translate(0f, -p.pivot * SC); c.rotate(p.rot); c.translate(0f, p.pivot * SC) } else c.rotate(p.rot)
        c.scale(SC, SC)
        c.translate(-12f, -22f)
        tmpRect.set(0f, 0f, 24f, 24f)
        c.drawBitmap(bmp, null, tmpRect, bmpPaint)
        c.restore()
    }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
    private val textOut = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND; color = Color.parseColor("#2a1f3d") }
    private val dark = Color.parseColor("#2a1f3d")

    /** 화면 전체 효과(그림자, 오라, 금, 파편, 글자, 말풍선) */
    fun drawFx(c: Canvas) {
        c.save()
        if (shakeT > 0) c.translate(rnd(-shakeA, shakeA), rnd(-shakeA, shakeA))
        // 금
        for (k in cracks) {
            val a = if (k.age < 3.2f) 1f else max(0f, 1 - (k.age - 3.2f))
            stroke.strokeWidth = 3.2f * D; stroke.color = Color.argb((140 * a).toInt(), 30, 20, 60)
            c.drawPath(k.path, stroke); c.drawPath(k.dash, stroke)
            stroke.strokeWidth = 1.3f * D; stroke.color = Color.argb((255 * a).toInt(), 255, 255, 255)
            c.drawPath(k.path, stroke); c.drawPath(k.dash, stroke)
            fill.color = Color.argb((230 * a).toInt(), 255, 255, 255)
        }
        // 파편
        val sp = Path()
        for (s in shards) {
            val a = if (s.landed) max(0f, 1 - s.landedAge / .5f) else 1f
            sp.reset(); sp.moveTo(s.pts[0], s.pts[1]); var i = 2
            while (i < s.pts.size) { sp.lineTo(s.pts[i], s.pts[i + 1]); i += 2 }
            sp.close()
            c.save(); c.translate(s.x, s.y); c.rotate(s.rot)
            fill.color = Color.argb((235 * a).toInt(), 215, 235, 255); c.drawPath(sp, fill)
            stroke.strokeWidth = 1f * D; stroke.color = Color.argb((150 * a).toInt(), 30, 20, 60); c.drawPath(sp, stroke)
            c.restore()
        }
        // 그림자
        if (state != "away") {
            val gy = floor.y; val dist = max(0f, gy - y)
            val w = max(16 * D, (if (pose.lying) 17 else 12) * SC - dist * .08f)
            fill.color = Color.argb((90 * max(.2f, 1 - dist / (400 * D))).toInt(), 30, 10, 40)
            tmpRect.set(x - w / 2, gy - 4 * D, x + w / 2, gy + 4 * D); c.drawOval(tmpRect, fill)
        }
        // 공
        if (bState != "none") {
            val r = 4 * SC
            bmpPaint.alpha = if (bCrashed && bT > 2.2f) (255 * max(0f, 1 - (bT - 2.2f) / .6f)).toInt() else 255
            c.save(); c.translate(bx, bY - r); c.rotate(brot)
            tmpRect.set(-r, -r, r, r); c.drawBitmap(ballBmp, null, tmpRect, bmpPaint)
            c.restore(); bmpPaint.alpha = 255
        }
        // 오라
        val ac = if (form == Form.POWER) 1f else charge
        if (ac > 0 && state != "away") {
            val w = 26 * SC * (.5f + ac * .6f); val h = 32 * SC * (.5f + ac * .65f)
            val flick = if ((clock * 18).toInt() % 2 == 0) 1f else .8f
            val cy = y - h * .28f
            fill.shader = RadialGradient(x, cy, max(w, h) / 2, intArrayOf(Color.argb((230 * flick).toInt(), 255, 235, 245), Color.argb((180 * flick).toInt(), 255, 110, 165), Color.argb((80 * flick).toInt(), 255, 50, 110), Color.TRANSPARENT), floatArrayOf(0f, .3f, .55f, 1f), Shader.TileMode.CLAMP)
            c.save(); c.scale(w / max(w, h), h / max(w, h), x, cy)
            c.drawCircle(x, cy, max(w, h) / 2, fill); c.restore()
            fill.shader = null
        }
        // 떠오르는 글자
        for (f in floaters) {
            val life = if (f.kind == "z") 1.8f else 1.2f
            val q = f.age / life
            val alpha = if (f.age < .18f) f.age / .18f else max(0f, 1 - (f.age - .18f) / (life - .18f))
            var size = if (f.kind == "boing") 26f else if (f.kind == "z") 16f else 18f
            var sx = 1f; var sy = 1f
            if (f.kind == "boing") { val e = exp(-6 * f.age); sx = 1 + .35f * e * cos(f.age * 26); sy = 1 - .3f * e * cos(f.age * 26) } else if (f.age < .18f) size *= .6f + .4f * (f.age / .18f)
            val dy = -(if (f.kind == "z") 50f else 46f) * D * q
            val dx = if (f.kind == "z") 10 * D * q else 0f
            text.textSize = size * D; textOut.textSize = size * D; textOut.strokeWidth = 4 * D
            text.color = when (f.kind) { "boing" -> Color.parseColor("#ffe45e"); "z" -> Color.parseColor("#cfe3ff"); else -> Color.WHITE }
            text.alpha = (255 * alpha).toInt(); textOut.alpha = (255 * alpha).toInt()
            c.save(); c.translate(f.x + dx, f.y + dy); c.scale(sx, sy)
            c.drawText(f.text, 0f, 0f, textOut); c.drawText(f.text, 0f, 0f, text)
            c.restore()
        }
        // 말풍선
        if (bubbleT > 0 && bubbleText.isNotEmpty() && state != "away") {
            text.textSize = 14 * D; text.color = dark; text.alpha = 255
            val tw = text.measureText(bubbleText)
            val bw = tw + 20 * D; val bh = 28 * D
            val bx = (headX() - bw / 2).coerceIn(4 * D, W - bw - 4 * D); val bb = headY() - 14 * D
            fill.color = Color.argb(60, 0, 0, 0); c.drawRect(bx + 3 * D, bb - bh + 3 * D, bx + bw + 3 * D, bb + 3 * D, fill)
            fill.color = Color.WHITE; c.drawRect(bx, bb - bh, bx + bw, bb, fill)
            stroke.strokeWidth = 3 * D; stroke.color = dark; c.drawRect(bx, bb - bh, bx + bw, bb, stroke)
            val tail = Path(); tail.moveTo(headX() - 5 * D, bb); tail.lineTo(headX() + 5 * D, bb); tail.lineTo(headX(), bb + 7 * D); tail.close()
            fill.color = dark; c.drawPath(tail, fill)
            c.drawText(bubbleText, bx + bw / 2, bb - bh / 2 + 5 * D, text)
        }
        c.restore()
        if (flashA > 0) { fill.color = Color.argb((255 * flashA).toInt(), 255, 255, 255); c.drawRect(0f, 0f, W, H, fill) }
    }
}
