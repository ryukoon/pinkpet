package com.ryukoon.pinkpet

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 권한 설정 + 펫 켜기/끄기 + 모션 메뉴 */
class MainActivity : Activity() {
    private val acts = listOf(
        "idle" to "가만히", "walk" to "걷기", "run" to "달리기", "jump" to "점프", "sit" to "앉기",
        "sleep" to "잠자기", "wave" to "손인사", "dance" to "춤추기", "look" to "두리번", "stretch" to "기지개",
        "pen" to "펜 칼싸움", "ramen" to "몰래 라면", "power" to "파워업 변신", "chicken" to "치킨 변신"
    )
    private lateinit var status: TextView
    private lateinit var toggle: Button
    private lateinit var soundBtn: Button
    private val dp get() = resources.displayMetrics.density
    private val purple = Color.parseColor("#5d3f82")
    private val darkC = Color.parseColor("#2a1f3d")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = darkC
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (24 * dp).toInt(), (20 * dp).toInt(), (32 * dp).toInt())
            setBackgroundColor(Color.parseColor("#f7eef2"))
        }
        root.addView(text("진행해줘 펫 🐔", 26f, true, darkC))
        root.addView(text("핑크 스웨터 아저씨가 홈 화면과 다른 앱 위를 돌아다녀요.", 14f, false, Color.DKGRAY))
        status = text("", 14f, true, purple); root.addView(status)

        root.addView(section("1. 처음 한 번만 설정"))
        root.addView(button("다른 앱 위에 표시 권한 켜기") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        if (Build.VERSION.SDK_INT >= 33) root.addView(button("알림 권한 허용") {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        root.addView(button("배터리 제한 해제 (꺼짐 방지)") { askBattery() })

        root.addView(section("2. 펫 켜기"))
        toggle = button("펫 시작!") { togglePet() }.also { root.addView(it) }

        root.addView(section("3. 모션 골라서 시키기"))
        val grid = GridLayout(this).apply { columnCount = 2 }
        for ((key, label) in acts) {
            val b = button(label) { sendAct(key) }
            b.layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED, 1f), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                width = 0; setMargins((3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt())
            }
            grid.addView(b)
        }
        root.addView(grid)

        root.addView(section("4. 설정"))
        val prefs = getSharedPreferences("pet", MODE_PRIVATE)
        soundBtn = button("") {
            prefs.edit().putBoolean("sound", !prefs.getBoolean("sound", true)).apply(); refresh(); notifyPrefs()
        }.also { root.addView(it) }
        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((v, l) in listOf(2f to "작게", 3f to "보통", 4f to "크게")) {
            sizeRow.addView(button("크기 $l") { prefs.edit().putFloat("size", v).apply(); refresh(); notifyPrefs() }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0) }
            })
        }
        root.addView(sizeRow)
        root.addView(text("· 펫을 꾹 누르고 있으면 이 메뉴가 열려요.\n· 톡 누르면 \"진행해줘!\" 하고 인사해요.\n· 잡고 위로 던지면 떨어져서 띠용~ 해요.\n· 알림창의 [펫 끄기]로도 끌 수 있어요.", 13f, false, Color.DKGRAY))

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        status.text = if (!overlay) "⚠ 먼저 '다른 앱 위에 표시' 권한을 켜주세요" else if (PetService.running) "● 펫이 돌아다니는 중" else "○ 펫이 꺼져 있어요"
        toggle.text = if (PetService.running) "펫 끄기" else "펫 시작!"
        val prefs = getSharedPreferences("pet", MODE_PRIVATE)
        soundBtn.text = if (prefs.getBoolean("sound", true)) "소리 켜짐 🔊 (눌러서 끄기)" else "소리 꺼짐 🔇 (눌러서 켜기)"
    }

    private fun togglePet() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return
        }
        if (PetService.running) {
            startService(Intent(this, PetService::class.java).setAction(PetService.ACTION_STOP))
        } else {
            startForegroundService(Intent(this, PetService::class.java).setAction(PetService.ACTION_START))
            moveTaskToBack(true)
        }
        status.postDelayed({ refresh() }, 400)
    }

    private fun sendAct(key: String) {
        if (!Settings.canDrawOverlays(this)) { togglePet(); return }
        if (!PetService.running) { togglePet(); status.postDelayed({ if (PetService.running) sendAct(key) }, 1500); return }
        try { startService(Intent(this, PetService::class.java).setAction(PetService.ACTION_ACT).putExtra("act", key)) } catch (_: Exception) { }
        moveTaskToBack(true)
    }

    private fun notifyPrefs() {
        if (PetService.running) startService(Intent(this, PetService::class.java).setAction(PetService.ACTION_PREFS))
    }

    @Suppress("BatteryLife")
    private fun askBattery() {
        val pm = getSystemService(PowerManager::class.java)
        try {
            if (!pm.isIgnoringBatteryOptimizations(packageName))
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            else startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) refresh()
    }

    private fun text(s: String, size: Float, bold: Boolean, color: Int) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
    }

    private fun section(s: String) = text(s, 17f, true, darkC).apply { setPadding(0, (18 * dp).toInt(), 0, (6 * dp).toInt()) }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        minHeight = (48 * dp).toInt()
        background = GradientDrawable().apply { setColor(purple); cornerRadius = 14 * dp }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, (4 * dp).toInt(), 0, (4 * dp).toInt()) }
        setOnClickListener { onClick() }
    }
}
