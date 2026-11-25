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
import kotlin.math.roundToInt

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

    private class Pet(val engine: Engine, val view: View, val lp: WindowManager.LayoutParams) { var w = 0; var h = 0; var feet = 0f }

    private lateinit var wm: WindowManager
    private lateinit var wctx: Context
    private lateinit var sfx: Sfx
    private lateinit var fxView: View
    private lateinit var fxLp: WindowManager.LayoutParams
    private val pets = LinkedHashMap<String, Pet>()
    private var lastFrameNs = 0L
    private var attached = false
    private var screenW = 0f; private var screenH = 0f; private var floorY = 0f; private var gridPx = 0f

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!attached) return
            val dt = if (lastFrameNs == 0L) 0.016f else (frameTimeNanos - lastFrameNs) / 1e9f
            lastFrameNs = frameTimeNanos
            val now = System.currentTimeMillis()
            for (p in pets.values) { p.engine.update(dt, now); movePet(p) }
            Roster.tick(pets.mapValues { it.value.engine }, minOf(dt, .05f), gridPx)
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
            .setContentTitle(Roster.notif)
            .setContentText("눌러서 펫 켜기/끄기, 모션 메뉴 열기")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "모두 끄기", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun applyPrefs() {
        val prefs = getSharedPreferences("pet", MODE_PRIVATE)
        sfx.enabled = prefs.getBoolean("sound", true)
        Engine.spicyOn = prefs.getBoolean("spicy", false)
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
    }

    private fun relayout() {
        try { wm.updateViewLayout(fxView, fxLp) } catch (_: Exception) { }
        for (p in pets.values) {
            sizePet(p)
            computePos(p)
            try { wm.updateViewLayout(p.view, p.lp) } catch (_: Exception) { }
        }
    }

    /** 설정에 맞게 펫을 추가/제거. 하나도 없으면 서비스 종료 */
    @SuppressLint("ClickableViewAccessibility")
    private fun syncPets() {
        for (def in Roster.all) {
            val on = isOn(this, def.id)
            val existing = pets[def.id]
            if (on && existing == null) {
                lateinit var pet: Pet
                val engine = Engine(resources.displayMetrics.density, sfx, def) { openMenu(def.id) }
                val view = object : View(wctx) {
                    override fun onDraw(canvas: Canvas) { engine.drawPet(canvas, pet.lp.x.toFloat(), pet.lp.y.toFloat(), System.currentTimeMillis()) }
                }
                val lp = baseLp(1, 1, true)
                pet = Pet(engine, view, lp)
                sizePet(pet)
                view.setOnTouchListener { _, e ->
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> engine.onDown(e.rawX, e.rawY)
                        MotionEvent.ACTION_MOVE -> engine.onMove(e.rawX, e.rawY)
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> engine.onUp()
                    }
                    true
                }
                engine.spawn(Roster.spawnFrac(def.id))
                computePos(pet)
                wm.addView(view, lp)
                pets[def.id] = pet
            } else if (!on && existing != null) {
                try { wm.removeView(existing.view) } catch (_: Exception) { }
                pets.remove(def.id)
            }
        }
        Roster.hook(pets.mapValues { it.value.engine })
        if (pets.isEmpty()) stopSelf()
    }

    /** 캐릭터 배율에 맞춰 펫 창 크기 계산 */
    private fun sizePet(p: Pet) {
        val g = gridPx * p.engine.def.scale
        p.engine.setScreen(screenW, screenH, floorY, g)
        p.w = (32 * g).roundToInt(); p.h = (38 * g).roundToInt(); p.feet = 29 * g
        p.lp.width = p.w; p.lp.height = p.h
    }

    private fun computePos(p: Pet): Boolean {
        val nx = (p.engine.x - p.w / 2f).roundToInt()
        val ny = (p.engine.drawY - p.feet).roundToInt()
        if (nx == p.lp.x && ny == p.lp.y) return false
        p.lp.x = nx; p.lp.y = ny
        return true
    }

    private fun movePet(p: Pet) {
        if (computePos(p)) try { wm.updateViewLayout(p.view, p.lp) } catch (_: Exception) { }
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
