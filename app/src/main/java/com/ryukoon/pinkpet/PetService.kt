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

/** 다른 앱 위에 펫을 띄우는 서비스 */
class PetService : Service() {
    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_ACT = "act"
        const val ACTION_PREFS = "prefs"
        const val CHANNEL = "pet"
        @Volatile var running = false
    }

    private lateinit var wm: WindowManager
    private lateinit var wctx: Context
    private lateinit var engine: Engine
    private lateinit var sfx: Sfx
    private lateinit var fxView: View
    private lateinit var petView: View
    private lateinit var fxLp: WindowManager.LayoutParams
    private lateinit var petLp: WindowManager.LayoutParams
    private var lastFrameNs = 0L
    private var attached = false
    private var petW = 0; private var petH = 0; private var feetY = 0f

    private val frame = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!attached) return
            val dt = if (lastFrameNs == 0L) 0.016f else (frameTimeNanos - lastFrameNs) / 1e9f
            lastFrameNs = frameTimeNanos
            engine.update(dt, System.currentTimeMillis())
            movePetWindow()
            fxView.invalidate(); petView.invalidate()
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        // 오버레이 전용 창 컨텍스트 (폴드 화면 전환도 여기로 알림이 옴)
        val display = getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        wctx = createWindowContext(display, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        wm = wctx.getSystemService(WindowManager::class.java)
        wctx.registerComponentCallbacks(screenCallbacks)
        sfx = Sfx(this)
        engine = Engine(resources.displayMetrics.density, sfx) { openMenu() }
        applyPrefs()
        createWindows()
        engine.spawn()
        running = true
        Choreographer.getInstance().postFrameCallback(frame)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> { stopSelf(); return START_NOT_STICKY }
            ACTION_ACT -> if (attached) intent.getStringExtra("act")?.let { engine.force(it) }
            ACTION_PREFS -> if (attached) { applyPrefs(); layoutWindows() }
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
            .setContentTitle("진행해줘 펫이 돌아다니는 중")
            .setContentText("눌러서 모션 메뉴 열기")
            .setContentIntent(open)
            .addAction(Notification.Action.Builder(null, "펫 끄기", stop).build())
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(1, n)
    }

    private fun applyPrefs() {
        val prefs = getSharedPreferences("pet", MODE_PRIVATE)
        sfx.enabled = prefs.getBoolean("sound", true)
    }

    private fun gridPx(): Float {
        val prefs = getSharedPreferences("pet", MODE_PRIVATE)
        val dp = prefs.getFloat("size", 2f)
        return dp * resources.displayMetrics.density
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

    @SuppressLint("ClickableViewAccessibility")
    private fun createWindows() {
        fxView = object : View(wctx) {
            override fun onDraw(canvas: Canvas) { engine.drawFx(canvas) }
        }
        petView = object : View(wctx) {
            override fun onDraw(canvas: Canvas) { engine.drawPet(canvas, petLp.x.toFloat(), petLp.y.toFloat(), System.currentTimeMillis()) }
        }
        petView.setOnTouchListener { _, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> engine.onDown(e.rawX, e.rawY)
                MotionEvent.ACTION_MOVE -> engine.onMove(e.rawX, e.rawY)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> engine.onUp()
            }
            true
        }
        fxLp = baseLp(1, 1, false).apply {
            // 안드로이드 12+ 에서 아래 앱 터치가 막히지 않도록 0.8 이하로
            alpha = 0.8f
        }
        petLp = baseLp(1, 1, true)
        layoutWindows(addNow = true)
        attached = true
    }

    private fun layoutWindows(addNow: Boolean = false) {
        val m = wm.currentWindowMetrics
        val b = m.bounds
        val nav = m.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
        val d = resources.displayMetrics.density
        val sc = gridPx()
        engine.setScreen(b.width().toFloat(), b.height().toFloat(), b.height() - nav.bottom - 6 * d, sc)
        fxLp.width = b.width(); fxLp.height = b.height(); fxLp.x = 0; fxLp.y = 0
        petW = (32 * sc).roundToInt(); petH = (38 * sc).roundToInt(); feetY = 29 * sc
        petLp.width = petW; petLp.height = petH
        computePetPos()
        if (addNow) {
            wm.addView(fxView, fxLp)
            wm.addView(petView, petLp)
        } else {
            wm.updateViewLayout(fxView, fxLp)
            wm.updateViewLayout(petView, petLp)
        }
    }

    private fun computePetPos(): Boolean {
        val nx = (engine.x - petW / 2f).roundToInt()
        val ny = (engine.drawY - feetY).roundToInt()
        if (nx == petLp.x && ny == petLp.y) return false
        petLp.x = nx; petLp.y = ny
        return true
    }

    private fun movePetWindow() {
        if (computePetPos()) {
            try { wm.updateViewLayout(petView, petLp) } catch (_: Exception) { }
        }
    }

    private fun openMenu() {
        val i = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        try { startActivity(i) } catch (_: Exception) { }
    }

    private val screenCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            // 폴드 접기/펼치기, 화면 회전
            if (attached) petView.postDelayed({ if (attached) try { layoutWindows() } catch (_: Exception) { } }, 250)
        }
        @Deprecated("Deprecated in Java")
        override fun onLowMemory() { }
    }

    override fun onDestroy() {
        running = false
        if (attached) {
            attached = false
            Choreographer.getInstance().removeFrameCallback(frame)
            try { wm.removeView(petView) } catch (_: Exception) { }
            try { wm.removeView(fxView) } catch (_: Exception) { }
            sfx.release()
            try { wctx.unregisterComponentCallbacks(screenCallbacks) } catch (_: Exception) { }
        }
        super.onDestroy()
    }
}
