package com.ryukoon.pinkpet

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Choreographer
import android.view.Display
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** 다른 앱 위에 펫(여유곰, 부장님)을 띄우는 서비스 */
class PetService : Service() {
    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_ACT = "act"
        const val ACTION_PREFS = "prefs"
        const val CHANNEL = "pet"
        @Volatile var running = false
        fun isOn(ctx: Context, id: String) = ctx.getSharedPreferences("pet", MODE_PRIVATE).getBoolean("on_$id", true)
    }

    private class Pet(val engine: Engine, val view: View, val lp: WindowManager.LayoutParams)

    private lateinit var wm: WindowManager
    private lateinit var wctx: Context
    private lateinit var sfx: Sfx
    private lateinit var fxView: View
    private lateinit var fxLp: WindowManager.LayoutParams
    private val pets = LinkedHashMap<String, Pet>()
    private var lastFrameNs = 0L
    private var attached = false
    private var petW = 0; private var petH = 0; private var feetY = 0f
    private var screenW = 0f; private var screenH = 0f; private var floorY = 0f; private var gridPx = 0f
    private var interCool = 3f

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!attached) return
            val dt = if (lastFrameNs == 0L) 0.016f else (frameTimeNanos - lastFrameNs) / 1e9f
            lastFrameNs = frameTimeNanos
            val now = System.currentTimeMillis()
            for (p in pets.values) { p.engine.update(dt, now); movePet(p) }
            interactions(minOf(dt, .05f))
            fxView.invalidate()
            for (p in pets.values) p.view.invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        val display = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        wctx = createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        wm = wctx.getSystemService(WindowManager::class.java)
        wctx.registerComponentCallbacks(screenCallbacks)
        sfx = Sfx(this)
        applyPrefs()
        fxView = object : View(wctx) {
            override fun onDraw(canvas: Canvas) { for (p in pets.values) p.engine.drawFx(canvas) }
        }
        fxLp = baseLp(1, 1, false).apply { alpha = 0.8f } // 안드로이드 12+ 에서 아래 앱 터치가 막히지 않도록
        measure()
        wm.addView(fxView, fxLp)
        attached = true
        syncPets()
        running = true
        Choreographer.getInstance().postFrameCallback(frame)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_ACT -> if (attached) {
                val act = intent.getStringExtra("act")
                val id = intent.getStringExtra("pet")
                val p = pets[id] ?: pets.values.firstOrNull()
                if (act != null && p != null) p.engine.force(act)
            }
            ACTION_PREFS -> if (attached) { applyPrefs(); measure(); relayout(); syncPets() }
        }
        return START_STICKY
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "펫 실행 중", NotificationManager.IMPORTANCE_MIN))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, PetService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle("펫 친구들이 돌아다니는 중")
            .setContentText("눌러서 펫 켜기/끄기, 모션 메뉴 열기")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "모두 끄기", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun applyPrefs() {
        sfx.enabled = getSharedPreferences("pet", MODE_PRIVATE).getBoolean("sound", true)
    }

    private fun baseLp(w: Int, h: Int, touchable: Boolean): WindowManager.LayoutParams {
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            setFitInsetsTypes(0)
        }
    }

    /** 화면 크기·바닥 위치·도트 크기 계산 */
    private fun measure() {
        val m = wm.currentWindowMetrics
        val b = m.bounds
        val nav = m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
        val d = resources.displayMetrics.density
        gridPx = getSharedPreferences("pet", MODE_PRIVATE).getFloat("size", 2f) * d
        screenW = b.width().toFloat(); screenH = b.height().toFloat(); floorY = b.height() - nav.bottom - 6 * d
        fxLp.width = b.width(); fxLp.height = b.height(); fxLp.x = 0; fxLp.y = 0
        petW = (32 * gridPx).roundToInt(); petH = (38 * gridPx).roundToInt(); feetY = 29 * gridPx
    }

    private fun relayout() {
        try { wm.updateViewLayout(fxView, fxLp) } catch (_: Exception) { }
        for (p in pets.values) {
            p.engine.setScreen(screenW, screenH, floorY, gridPx)
            p.lp.width = petW; p.lp.height = petH
            computePos(p)
            try { wm.updateViewLayout(p.view, p.lp) } catch (_: Exception) { }
        }
    }

    /** 설정에 맞게 펫을 추가/제거. 하나도 없으면 서비스 종료 */
    @SuppressLint("ClickableViewAccessibility")
    private fun syncPets() {
        for (def in Chars.all) {
            val on = isOn(this, def.id)
            val existing = pets[def.id]
            if (on && existing == null) {
                lateinit var pet: Pet
                val engine = Engine(resources.displayMetrics.density, sfx, def) { openMenu(def.id) }
                val view = object : View(wctx) {
                    override fun onDraw(canvas: Canvas) { engine.drawPet(canvas, pet.lp.x.toFloat(), pet.lp.y.toFloat(), System.currentTimeMillis()) }
                }
                val lp = baseLp(petW, petH, true)
                pet = Pet(engine, view, lp)
                view.setOnTouchListener { _, e ->
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> engine.onDown(e.rawX, e.rawY)
                        MotionEvent.ACTION_MOVE -> engine.onMove(e.rawX, e.rawY)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> engine.onUp()
                    }
                    true
                }
                engine.setScreen(screenW, screenH, floorY, gridPx)
                engine.spawn(if (def.id == "bear") .3f else .7f)
                computePos(pet)
                wm.addView(view, lp)
                pets[def.id] = pet
            } else if (!on && existing != null) {
                try { wm.removeView(existing.view) } catch (_: Exception) { }
                pets.remove(def.id)
            }
        }
        // 서로 알아보기: 한쪽이 화면을 깨면 다른 쪽이 반응
        pets["bear"]?.engine?.onCrash = { cx -> pets["pink"]?.engine?.interact("run", cx, "누구야!! 유리 누가 깼어!!") }
        pets["pink"]?.engine?.onCrash = { cx -> pets["bear"]?.engine?.interact("look", cx, "부장님… 화면이…") }
        if (pets.isEmpty()) stopSelf()
    }

    private fun computePos(p: Pet): Boolean {
        val nx = (p.engine.x - petW / 2f).roundToInt()
        val ny = (p.engine.drawY - feetY).roundToInt()
        if (nx == p.lp.x && ny == p.lp.y) return false
        p.lp.x = nx; p.lp.y = ny
        return true
    }

    private fun movePet(p: Pet) {
        if (computePos(p)) try { wm.updateViewLayout(p.view, p.lp) } catch (_: Exception) { }
    }

    /** 두 펫이 만나면 서로 반응 */
    private fun interactions(dt: Float) {
        val bear = pets["bear"]?.engine ?: return
        val pink = pets["pink"]?.engine ?: return
        interCool -= dt
        if (interCool > 0) return
        if (bear.stateName == "away" || pink.stateName == "away") return
        val dx = abs(bear.x - pink.x); val dy = abs(bear.y - pink.y)
        if (dx > 13 * gridPx || dy > 18 * gridPx) return
        interCool = 7f
        val moving = setOf("air", "drag")
        if (bear.stateName in moving || pink.stateName in moving || bear.actName == "run" || pink.actName == "run") {
            // 부딪히면 둘 다 띠용~ 튕겨나감
            bear.bump(pink.x); pink.bump(bear.x)
            return
        }
        if (!bear.canInteract || !pink.canInteract) { interCool = 2f; return }
        if (pink.isChicken) {
            bear.interact("run", pink.x, "치킨이다!! 🍗")
            pink.interact("cfly", bear.x, "꼬꼬댁!! 살려줘~")
            return
        }
        val midX = (bear.x + pink.x) / 2; val topY = minOf(bear.y, pink.y) - 24 * gridPx
        when (Random.nextInt(6)) {
            0 -> { bear.interact("bow", pink.x, "부장님 안녕하세요~"); pink.interact("wave", bear.x, "어 왔어? 진행해줘!") }
            1 -> { bear.interact("pawWave", pink.x, "하이파이브!"); pink.interact("wave", bear.x, null); bear.popAt("짝!", midX, topY) }
            2 -> { bear.interact("dance", pink.x, "🎵 같이 춰요~"); pink.interact("dance", bear.x, "오 좋아!") }
            3 -> { pink.interact("boxing", bear.x, "덤벼라!"); bear.interact("kungfu", pink.x, "곰발 펀치!") }
            4 -> { pink.interact("chickenEat", bear.x, "치킨 먹을래?"); bear.interact("chickenEat", pink.x, "좋아요!! 🍗") }
            else -> { bear.bump(pink.x); pink.bump(bear.x); bear.talk("앗 죄송!"); pink.talk("어이쿠!") }
        }
    }

    private fun openMenu(id: String) {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("pet", id)
        try { startActivity(i) } catch (_: Exception) { }
    }

    private val screenCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            // 폴드 접기/펼치기, 화면 회전
            if (attached) fxView.postDelayed({ if (attached) try { measure(); relayout() } catch (_: Exception) { } }, 250)
        }
        @Deprecated("Deprecated in Java")
        override fun onLowMemory() { }
    }

    override fun onDestroy() {
        running = false
        if (attached) {
            attached = false
            Choreographer.getInstance().removeFrameCallback(frame)
            for (p in pets.values) try { wm.removeView(p.view) } catch (_: Exception) { }
            pets.clear()
            try { wm.removeView(fxView) } catch (_: Exception) { }
            sfx.release()
            try { wctx.unregisterComponentCallbacks(screenCallbacks) } catch (_: Exception) { }
        }
        super.onDestroy()
    }
}
