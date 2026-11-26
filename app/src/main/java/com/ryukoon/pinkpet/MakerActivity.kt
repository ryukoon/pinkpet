package com.ryukoon.pinkpet

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** 사진 → 8비트 캐릭터 만들기 / 고치기 */
class MakerActivity : Activity() {
    private val dp get() = resources.displayMetrics.density
    private val purple = Color.parseColor("#5d3f82")
    private val brown = Color.parseColor("#8a5a2b")
    private val grey = Color.parseColor("#9a96a8")
    private val darkC = Color.parseColor("#2a1f3d")

    private var editId: String? = null
    private var look = Look()
    private var female = false
    private var kid = false
    private val specs = LinkedHashSet<String>()
    private lateinit var preview: ImageView
    private lateinit var status: TextView
    private lateinit var nameEt: EditText
    private lateinit var phraseEt: EditText
    private val genderBtns = HashMap<Boolean, Button>()
    private lateinit var kidBtn: Button
    private val specBtns = HashMap<String, Button>()
    private val rowUpdaters = ArrayList<() -> Unit>()
    private var poseIdx = 0
    private lateinit var root: LinearLayout
    private lateinit var header: LinearLayout
    private val palettes = ArrayList<GridLayout>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Roster.init(this)
        editId = intent?.getStringExtra("id")
        CharStore.get(editId)?.let { c -> look = c.look.copy(); female = c.female; kid = c.kid; specs.addAll(c.specs) }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * dp).toInt(), (20 * dp).toInt(), (18 * dp).toInt(), (40 * dp).toInt())
            setBackgroundColor(Color.parseColor("#f7eef2"))
        }
        root.addView(text("사진은 이 폰 안에서만 색을 분석하고 바로 버려요. 어디에도 저장하거나 보내지 않아요.", 12f, false, Color.DKGRAY))

        // 위에 고정되는 미리보기 (스크롤해도 항상 보임)
        header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((18 * dp).toInt(), (12 * dp).toInt(), (18 * dp).toInt(), (6 * dp).toInt())
            setBackgroundColor(Color.parseColor("#efe3f2"))
            elevation = 6 * dp
        }
        header.addView(text(if (editId == null) "새 캐릭터 만들기 🎨" else "캐릭터 고치기 ✏️", 18f, true, darkC).apply { setPadding(0, 0, 0, (4 * dp).toInt()) })
        preview = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = GradientDrawable().apply { setColor(Color.parseColor("#b9a6d8")); cornerRadius = 16 * dp }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (150 * dp).toInt())
            setOnClickListener { poseIdx++; updatePreview() }
        }
        header.addView(preview)
        status = text("미리보기를 누르면 다른 자세를 보여줘요", 13f, true, purple).apply { gravity = Gravity.CENTER; setPadding(0, (4 * dp).toInt(), 0, 0) }
        header.addView(status)

        root.addView(section("1. 기본 정보"))
        val gRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for ((f, label) in listOf(false to "👦 남자", true to "👧 여자")) {
            val b = button(label, grey) { female = f; look.female = f; updateAll() }
            b.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { setMargins((3 * dp).toInt(), 0, (3 * dp).toInt(), 0) }
            genderBtns[f] = b; gRow.addView(b)
        }
        root.addView(gRow)
        kidBtn = button("", grey) { kid = !kid; updateAll() }.also { root.addView(it) }

        root.addView(section("2. 사진으로 만들기"))
        root.addView(button("📷 사진 고르기", brown) {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 7)
        })
        root.addView(text("얼굴이 정면으로 크게 나오고, 윗옷까지 보이는 사진이 잘 맞아요. 성별을 먼저 고르면 머리 모양이 더 잘 맞아요.", 12f, false, Color.DKGRAY))

        root.addView(section("3. 이름 · 말버릇"))
        nameEt = EditText(this).apply { hint = "이름 (예: 민준)"; setSingleLine(true); textSize = 16f }
        root.addView(nameEt)
        phraseEt = EditText(this).apply {
            hint = "자주 하는 말 (한 줄에 하나씩)\n예) 배고파\n오늘도 화이팅!"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 3; gravity = Gravity.TOP or Gravity.START; textSize = 15f
        }
        root.addView(phraseEt)

        root.addView(section("4. 특기 (여러 개 고르기)"))
        val sg = GridLayout(this).apply { columnCount = 3 }
        for (s in MoveBook.specs) {
            val b = button(s, grey) { if (!specs.remove(s)) specs.add(s); updateAll() }
            b.textSize = 13f
            b.layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                width = 0; setMargins((2 * dp).toInt(), (2 * dp).toInt(), (2 * dp).toInt(), (2 * dp).toInt())
            }
            specBtns[s] = b; sg.addView(b)
        }
        root.addView(sg)

        root.addView(section("5. 겉모습 고치기"))
        root.addView(text("사진 분석이 안 맞는 부분은 ◀ ▶ 로 바꿔주세요.", 12f, false, Color.DKGRAY))
        choiceRow("머리 모양", Palette.hairStyles, { look.hair }) { look.hair = it }
        choiceRow("앞머리", listOf("yes" to "있음", "no" to "없음"), { if (look.bangs) "yes" else "no" }) { look.bangs = it == "yes" }
        colorRow("머리색", Palette.hair, { look.hairColor }) { look.hairColor = it }
        colorRow("피부색", Palette.skin, { look.skin }) { look.skin = it }
        choiceRow("윗옷", Palette.tops, { look.top }) { look.top = it }
        colorRow("윗옷 색", Palette.cloth, { look.topColor }) { look.topColor = it }
        colorRow("안쪽 옷 색", Palette.cloth, { look.top2Color }) { look.top2Color = it }
        choiceRow("아래옷", Palette.bottoms, { look.bottom }) { look.bottom = it }
        colorRow("아래옷 색", Palette.cloth, { look.bottomColor }) { look.bottomColor = it }
        colorRow("신발 색", Palette.cloth, { look.shoeColor }) { look.shoeColor = it }
        choiceRow("소품", Palette.accs, { look.acc }) { look.acc = it }

        root.addView(button("💾 저장하고 홈 화면에 내보내기", brown) { save() }.apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (60 * dp).toInt()).apply { setMargins(0, (24 * dp).toInt(), 0, 0) }
            textSize = 17f
        })

        CharStore.get(editId)?.let { c -> nameEt.setText(c.name); phraseEt.setText(c.phrases.joinToString("\n")) }
        val outer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#f7eef2")) }
        outer.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        outer.addView(ScrollView(this).apply { addView(root) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(outer)
        updateAll()
    }

    // ---------- 사진 받기 ----------
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != 7 || resultCode != RESULT_OK || uri == null) return
        status.text = "사진 분석 중…"
        Thread {
            val bmp = try { loadBitmap(uri) } catch (_: Exception) { null }
            val res = bmp?.let { PhotoAnalyzer.analyze(it, female) }
            bmp?.recycle()
            runOnUiThread {
                if (res == null) { status.text = "사진을 열지 못했어요. 다른 사진으로 해볼까요?"; return@runOnUiThread }
                look = res.look
                status.text = if (res.faceFound) "얼굴을 찾았어요! 아래에서 다른 부분을 고칠 수 있어요." else "얼굴을 못 찾아서 사진 가운데로 맞췄어요. 아래에서 고쳐주세요."
                updateAll()
            }
        }.start()
    }

    private fun loadBitmap(uri: Uri): Bitmap? {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (maxOf(opts.outWidth, opts.outHeight) / sample > 1400) sample *= 2
        val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val rot = try {
            contentResolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f; ExifInterface.ORIENTATION_ROTATE_180 -> 180f; ExifInterface.ORIENTATION_ROTATE_270 -> 270f; else -> 0f
                }
            } ?: 0f
        } catch (_: Exception) { 0f }
        if (rot == 0f) return bmp
        val r = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rot) }, true)
        if (r != bmp) bmp.recycle()
        return r
    }

    // ---------- 미리보기 ----------
    private val previewSprite = MakerSprite(Look())
    private fun updatePreview() {
        previewSprite.look = look.copy(female = female)
        val poses = listOf(Pose(mouth = "smile"), Pose(aR = "wave", mouth = "grin", eyes = "closed"), Pose(aL = "up", aR = "up", mouth = "open", eyes = "sparkle"),
            Pose(legs = "walkA", aL = "hip", aR = "out", mouth = "smile"), Pose(by = 14, hy = 2, ly = 20, legs = "sit", mouth = "smile", blush = true))
        val small = previewSprite.render(poses[poseIdx % poses.size])
        preview.setImageBitmap(Bitmap.createScaledBitmap(small, 240, 240, false))
    }

    private fun updateAll() {
        genderBtns.forEach { (f, b) -> (b.background as GradientDrawable).setColor(if (f == female) brown else grey) }
        kidBtn.text = if (kid) "🧒 어린이 (키 작게): 켜짐" else "🧒 어린이 (키 작게): 꺼짐"
        (kidBtn.background as GradientDrawable).setColor(if (kid) brown else grey)
        specBtns.forEach { (s, b) -> (b.background as GradientDrawable).setColor(if (s in specs) brown else grey) }
        rowUpdaters.forEach { it() }
        updatePreview()
    }

    private fun save() {
        val name = nameEt.text.toString().trim()
        if (name.isEmpty()) { status.text = "이름을 넣어주세요!"; nameEt.requestFocus(); return }
        val phrases = phraseEt.text.toString().lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("안녕!") }
        val id = editId ?: ("c" + System.currentTimeMillis())
        CharStore.save(CustomChar(id, name, female, kid, phrases, specs.toList().ifEmpty { listOf("달리기") }, look.copy(female = female)))
        getSharedPreferences("pet", MODE_PRIVATE).edit().putBoolean("on_$id", true).apply()
        if (PetService.running) try { startService(Intent(this, PetService::class.java).setAction(PetService.ACTION_PREFS)) } catch (_: Exception) { }
        finish()
    }

    // ---------- 고르기 줄 ----------
    private fun choiceRow(label: String, opts: List<Pair<String, String>>, get: () -> String, set: (String) -> Unit) {
        val tv = TextView(this).apply { textSize = 15f; gravity = Gravity.CENTER; setTextColor(darkC) }
        fun step(d: Int) { val i = opts.indexOfFirst { it.first == get() }.coerceAtLeast(0); set(opts[(i + d + opts.size) % opts.size].first); updateAll() }
        addRow(label, tv) { step(it) }
        rowUpdaters.add { tv.text = opts.firstOrNull { it.first == get() }?.second ?: get() }
    }

    private fun colorRow(label: String, pal: List<Int>, get: () -> Int, set: (Int) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, (3 * dp).toInt(), 0, (3 * dp).toInt()) }
        row.addView(TextView(this).apply { text = label; textSize = 14f; setTextColor(darkC); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f) })
        val sw = TextView(this).apply { text = "눌러서 색 고르기 ▾"; textSize = 13f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); setShadowLayer(3f, 0f, 0f, Color.BLACK) }
        row.addView(sw, LinearLayout.LayoutParams(0, (44 * dp).toInt(), 2.5f))
        root.addView(row)
        // 펼쳐지는 팔레트
        val grid = GridLayout(this).apply { columnCount = 6; visibility = android.view.View.GONE; setPadding(0, (4 * dp).toInt(), 0, (10 * dp).toInt()) }
        val cells = ArrayList<Pair<Int, android.view.View>>()
        for (col in pal) {
            val cell = android.view.View(this)
            cell.setOnClickListener { set(col); grid.visibility = android.view.View.GONE; updateAll() }
            cell.layoutParams = GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f)).apply {
                width = 0; height = (44 * dp).toInt(); setMargins((3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt(), (3 * dp).toInt())
            }
            cells.add(col to cell); grid.addView(cell)
        }
        palettes.add(grid)
        root.addView(grid)
        sw.setOnClickListener {
            val open = grid.visibility != android.view.View.VISIBLE
            palettes.forEach { it.visibility = android.view.View.GONE }
            if (open) grid.visibility = android.view.View.VISIBLE
        }
        rowUpdaters.add {
            sw.background = GradientDrawable().apply { setColor(get()); cornerRadius = 10 * dp; setStroke((2 * dp).toInt(), darkC) }
            for ((col, cell) in cells) cell.background = GradientDrawable().apply {
                setColor(col); cornerRadius = 10 * dp
                if (col == get()) setStroke((4 * dp).toInt(), Color.parseColor("#ffcc33")) else setStroke((1 * dp).toInt(), Color.parseColor("#55506a"))
            }
        }
    }

    private fun addRow(label: String, mid: android.view.View, onStep: (Int) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, (3 * dp).toInt(), 0, (3 * dp).toInt()) }
        row.addView(TextView(this).apply { text = label; textSize = 14f; setTextColor(darkC); layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.3f) })
        row.addView(button("◀", purple) { onStep(-1) }.apply { layoutParams = LinearLayout.LayoutParams((52 * dp).toInt(), (44 * dp).toInt()) })
        mid.layoutParams = LinearLayout.LayoutParams(0, (36 * dp).toInt(), 1.4f).apply { setMargins((6 * dp).toInt(), 0, (6 * dp).toInt(), 0) }
        row.addView(mid)
        row.addView(button("▶", purple) { onStep(1) }.apply { layoutParams = LinearLayout.LayoutParams((52 * dp).toInt(), (44 * dp).toInt()) })
        root.addView(row)
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
