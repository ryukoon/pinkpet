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
    private lateinit var status: TextView
    private lateinit var soundBtn: Button
    private var spicyBtn: Button? = null
    private lateinit var grid: GridLayout
    private lateinit var motionTitle: TextView
    private val toggles = HashMap<String, Button>()
    private val pickers = HashMap<String, Button>()
    private var target: CharDef = Roster.all.first()
    private val dp get() = resources.displayMetrics.density
    private val purple = Color.parseColor("#5d3f82")
    private val brown = Color.parseColor("#8a5a2b")
    private val grey = Color.parseColor("#9a96a8")
    private val darkC = Color.parseColor("#2a1f3d")
    private val prefs get() = getSharedPreferences("pet", MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Roster.byId(intent?.getStringExtra("pet"))?.let { target = it }
        Engine.spicyOn = prefs.getBoolean("spicy", false)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((20 * dp).toInt(), (24 * dp).toInt(), (20 * dp).toInt(), (32 * dp).toInt())
            setBackgroundColor(Color.parseColor("#f7eef2"))
        }
        root.addView(text(Roster.title, 26f, true, darkC))
        root.addView(text(Roster.subtitle, 14f, false, Color.DKGRAY))
        status = text("", 14f, true, purple); root.addView(status)

        root.addView(section("1. 처음 한 번만 설정"))
        root.addView(button("다른 앱 위에 표시 권한 켜기", purple) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        if (Build.VERSION.SDK_INT >= 33) root.addView(button("알림 권한 허용", purple) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        })
        root.addView(button("배터리 제한 해제 (꺼짐 방지)", purple) { askBattery() })

        root.addView(section("2. 펫 켜기 / 끄기"))
        for (def in Roster.all) {
            val b = button("", brown) { togglePet(def) }
            toggles[def.id] = b; root.addView(b)
        }
        root.addView(button("모두 끄기", grey) {
            for (def in Roster.all) prefs.edit().putBoolean("on_${def.id}", false).apply()
            if (PetService.running) startService(Intent(this, PetService::class.java).setAction(PetService.ACTION_STOP))
            status.postDelayed({ refresh() }, 400)
        })

        motionTitle = section(""); root.addView(motionTitle)
        val pickRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (def in Roster.all) {
            val b = button(def.name, brown) { target = def; buildGrid(); refresh() }
            b.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0) }
            pickers[def.id] = b; pickRow.addView(b)
        }
        root.addView(pickRow)
        root.addView(button("🎲 아무거나 랜덤!", purple) { sendAct(target.menu.random().first) })
        grid = GridLayout(this).apply { columnCount = 2 }
        root.addView(grid)

        root.addView(section("4. 설정"))
        soundBtn = button("", purple) {
            prefs.edit().putBoolean("sound", !prefs.getBoolean("sound", true)).apply(); refresh(); notifyPrefs()
        }.also { root.addView(it) }
        val sizeRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((v, l) in listOf(2f to "작게", 3f to "보통", 4f to "크게")) {
            sizeRow.addView(button("크기 $l", purple) { prefs.edit().putFloat("size", v).apply(); notifyPrefs() }.apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0) }
            })
        }
        root.addView(sizeRow)
        if (Roster.hasSpicy) {
            spicyBtn = button("", purple) {
                prefs.edit().putBoolean("spicy", !prefs.getBoolean("spicy", false)).apply()
                Engine.spicyOn = prefs.getBoolean("spicy", false); buildGrid(); refresh(); notifyPrefs()
            }.also { root.addView(it) }
        }
        root.addView(text(Roster.hints, 13f, false, Color.DKGRAY))

        setContentView(ScrollView(this).apply { addView(root) })
        buildGrid()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Roster.byId(intent.getStringExtra("pet"))?.let { target = it; buildGrid(); refresh() }
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun buildGrid() {
        grid.removeAllViews()
        for ((key, label) in target.menu) {
            val b = button(label, purple) { sendAct(key) }
            b.textSize = 14f
            b.layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                width = 0; setMargins((3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt())
            }
            grid.addView(b)
        }
    }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        status.text = if (!overlay) "⚠ 먼저 '다른 앱 위에 표시' 권한을 켜주세요" else if (PetService.running) "● 펫이 돌아다니는 중" else "○ 펫이 꺼져 있어요"
        for (def in Roster.all) {
            val on = PetService.running && PetService.isOn(this, def.id)
            toggles[def.id]?.text = if (on) "${def.name}: 켜짐 ● (눌러서 끄기)" else "${def.name}: 꺼짐 (눌러서 켜기)"
            (toggles[def.id]?.background as? GradientDrawable)?.setColor(if (on) brown else grey)
            (pickers[def.id]?.background as? GradientDrawable)?.setColor(if (def == target) brown else grey)
        }
        motionTitle.text = "3. ${target.name} 모션 (${target.menu.size}종)"
        spicyBtn?.text = if (prefs.getBoolean("spicy", false)) "🌶 엄마 매운맛 대사: 켜짐 (눌러서 끄기)" else "🌶 엄마 매운맛 대사: 꺼짐 (아이 앞에선 끄기 추천)"
        soundBtn.text = if (prefs.getBoolean("sound", true)) "소리 켜짐 🔊 (눌러서 끄기)" else "소리 꺼짐 🔇 (눌러서 켜기)"
    }

    private fun togglePet(def: CharDef) {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))); return
        }
        val nowOn = PetService.running && PetService.isOn(this, def.id)
        if (!PetService.running) {
            // 서비스가 꺼져 있으면: 이 펫만 켜고 시작
            for (d in Roster.all) prefs.edit().putBoolean("on_${d.id}", d == def).apply()
            startForegroundService(Intent(this, PetService::class.java).setAction(PetService.ACTION_START))
        } else {
            prefs.edit().putBoolean("on_${def.id}", !nowOn).apply()
            notifyPrefs()
        }
        status.postDelayed({ refresh() }, 500)
    }

    private fun sendAct(key: String) {
        if (!Settings.canDrawOverlays(this)) { togglePet(target); return }
        if (!PetService.running || !PetService.isOn(this, target.id)) {
            togglePet(target)
            status.postDelayed({ if (PetService.running) sendAct(key) }, 1500)
            return
        }
        try { startService(Intent(this, PetService::class.java).setAction(PetService.ACTION_ACT).putExtra("act", key).putExtra("pet", target.id)) } catch (_: Exception) { }
        moveTaskToBack(true)
    }

    private fun notifyPrefs() {
        if (PetService.running) try { startService(Intent(this, PetService::class.java).setAction(PetService.ACTION_PREFS)) } catch (_: Exception) { }
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

    private fun button(label: String, color: Int, onClick: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        minHeight = (48 * dp).toInt()
        background = GradientDrawable().apply { setColor(color); cornerRadius = 14 * dp }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { setMargins(0, (4 * dp).toInt(), 0, (4 * dp).toInt()) }
        setOnClickListener { onClick() }
    }
}
