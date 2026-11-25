package com.ryukoon.pinkpet

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** 홈 화면에 떠 있는 앱 아이콘들의 위치 (화면 좌표) */
object IconMap {
    @Volatile var connected = false
    @Volatile var rects: List<Rect> = emptyList()
    @Volatile var version = 0
    fun set(list: List<Rect>) {
        if (list == rects) return
        rects = list; version++
    }
}

/**
 * 접근성 서비스: 홈 화면(런처)이 보일 때만 아이콘 버튼의 위치를 읽어요.
 * 글자 내용이나 다른 앱 화면은 저장하지 않고, 아이콘 위치(사각형)만 펫에게 전달해요.
 */
class IconWatcher : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var launchers: Set<String> = emptySet()
    private var lastScan = 0L
    private val rescan = Runnable { scan() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        launchers = packageManager.queryIntentActivities(home, PackageManager.MATCH_DEFAULT_ONLY).map { it.activityInfo.packageName }.toSet()
        IconMap.connected = true
        scan()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val now = SystemClock.uptimeMillis()
        handler.removeCallbacks(rescan)
        if (now - lastScan > 250) scan()
        handler.postDelayed(rescan, 350) // 페이지 넘김 애니메이션이 끝난 뒤 한 번 더
    }

    private fun scan() {
        lastScan = SystemClock.uptimeMillis()
        val root = try { rootInActiveWindow } catch (_: Exception) { null }
        if (root == null || root.packageName?.toString() !in launchers) { IconMap.set(emptyList()); return }
        val d = resources.displayMetrics.density
        val out = ArrayList<Rect>()
        collect(root, out, d, 0)
        IconMap.set(out)
    }

    private fun collect(n: AccessibilityNodeInfo, out: ArrayList<Rect>, d: Float, depth: Int) {
        if (depth > 25 || out.size > 60) return
        if (!n.isVisibleToUser) return
        val r = Rect(); n.getBoundsInScreen(r)
        val labeled = !n.text.isNullOrBlank() || !n.contentDescription.isNullOrBlank()
        val w = r.width() / d; val h = r.height() / d
        if ((n.isClickable || n.isLongClickable) && labeled && w in 40f..140f && h in 40f..160f && n.childCount <= 3) {
            out.add(Rect(r)); return
        }
        for (i in 0 until n.childCount) {
            val c = try { n.getChild(i) } catch (_: Exception) { null } ?: continue
            collect(c, out, d, depth + 1)
        }
    }

    override fun onInterrupt() { }

    override fun onDestroy() {
        IconMap.connected = false
        IconMap.set(emptyList())
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
