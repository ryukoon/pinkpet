package com.ryukoon.pinkpet

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.media.FaceDetector
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 폰 안에서만 사진을 분석해요. (인터넷 사용 없음, 사진 저장 없음)
 * 1) 안드로이드 기본 얼굴 찾기(FaceDetector)로 눈 사이 위치를 찾고
 * 2) 얼굴·머리·옷 위치의 색을 뽑아서
 * 3) 8비트 팔레트와 기본 부품에 맞춰요.
 */
object PhotoAnalyzer {
    class Result(val look: Look, val faceFound: Boolean)

    fun analyze(src: Bitmap, female: Boolean): Result {
        // 분석용으로 작게 (얼굴 찾기는 RGB_565, 짝수 가로 필요)
        val scale = min(1f, 640f / max(src.width, src.height))
        var w = (src.width * scale).toInt().coerceAtLeast(2); if (w % 2 == 1) w -= 1
        val h = (src.height * scale).toInt().coerceAtLeast(2)
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val face565 = small.copy(Bitmap.Config.RGB_565, false)
        val faces = arrayOfNulls<FaceDetector.Face>(3)
        val n = try { FaceDetector(w, h, 3).findFaces(face565, faces) } catch (_: Exception) { 0 }
        var cx = w / 2f; var cy = h * .32f; var ed = w * .11f
        var found = false
        if (n > 0) {
            // 가장 큰 얼굴
            val f = faces.filterNotNull().maxByOrNull { it.eyesDistance() }
            if (f != null) { val p = PointF(); f.getMidPoint(p); cx = p.x; cy = p.y; ed = f.eyesDistance(); found = true }
        }
        val px = IntArray(w * h); small.getPixels(px, 0, w, 0, 0, w, h)
        fun region(x0: Float, x1: Float, y0: Float, y1: Float): List<Int> {
            val out = ArrayList<Int>()
            val ax = x0.toInt().coerceIn(0, w - 1); val bx = x1.toInt().coerceIn(0, w - 1)
            val ay = y0.toInt().coerceIn(0, h - 1); val byy = y1.toInt().coerceIn(0, h - 1)
            if (bx <= ax || byy <= ay) return out
            val stepX = max(1, (bx - ax) / 18); val stepY = max(1, (byy - ay) / 18)
            var y = ay; while (y <= byy) { var x = ax; while (x <= bx) { out.add(px[y * w + x]); x += stepX }; y += stepY }
            return out
        }
        fun median(list: List<Int>, fallback: Int): Int {
            if (list.isEmpty()) return fallback
            val r = list.map { Color.red(it) }.sorted(); val g = list.map { Color.green(it) }.sorted(); val b = list.map { Color.blue(it) }.sorted()
            val m = list.size / 2
            return Color.rgb(r[m], g[m], b[m])
        }
        fun dist(a: Int, b: Int): Float {
            val dr = (Color.red(a) - Color.red(b)).toFloat(); val dg = (Color.green(a) - Color.green(b)).toFloat(); val db = (Color.blue(a) - Color.blue(b)).toFloat()
            return sqrt(dr * dr + dg * dg + db * db)
        }
        fun ratioNear(list: List<Int>, c: Int, th: Float): Float = if (list.isEmpty()) 0f else list.count { dist(it, c) < th } / list.size.toFloat()
        fun lum(c: Int) = (Color.red(c) * 30 + Color.green(c) * 59 + Color.blue(c) * 11) / 100

        val skin = median(region(cx - .55f * ed, cx + .55f * ed, cy + .35f * ed, cy + .85f * ed), Color.parseColor("#f1c6a0"))
        val hair = median(region(cx - .6f * ed, cx + .6f * ed, cy - 1.55f * ed, cy - 1.15f * ed), Color.parseColor("#2a1c18"))
        val hairIsHair = dist(hair, skin) > 45
        // 머리 길이: 턱 아래 얼굴 양옆이 머리색이면 긴 머리
        val beside = region(cx - 1.45f * ed, cx - 1.05f * ed, cy + 1.0f * ed, cy + 2.4f * ed) + region(cx + 1.05f * ed, cx + 1.45f * ed, cy + 1.0f * ed, cy + 2.4f * ed)
        val cheekSide = region(cx - 1.35f * ed, cx - 1.05f * ed, cy - .2f * ed, cy + .7f * ed) + region(cx + 1.05f * ed, cx + 1.35f * ed, cy - .2f * ed, cy + .7f * ed)
        val longR = ratioNear(beside, hair, 55f); val sideR = ratioNear(cheekSide, hair, 55f)
        val forehead = region(cx - .5f * ed, cx + .5f * ed, cy - .9f * ed, cy - .6f * ed)
        val bangs = hairIsHair && ratioNear(forehead, hair, 50f) > .45f
        val hairStyle = when {
            !hairIsHair -> "buzz"
            longR > .42f -> "long"
            sideR > .45f -> if (female) "bob" else "sidepart"
            female -> "ponytail"
            else -> "short"
        }
        // 옷: 턱 아래 몸통
        val torso = region(cx - 1.6f * ed, cx + 1.6f * ed, cy + 2.5f * ed, cy + 3.8f * ed)
        val center = region(cx - .25f * ed, cx + .25f * ed, cy + 2.5f * ed, cy + 3.6f * ed)
        val sides = region(cx - 1.6f * ed, cx - .7f * ed, cy + 2.5f * ed, cy + 3.8f * ed) + region(cx + .7f * ed, cx + 1.6f * ed, cy + 2.5f * ed, cy + 3.8f * ed)
        val topC = median(if (sides.isNotEmpty()) sides else torso, Color.parseColor("#3b6fd8"))
        val innerC = median(center, topC)
        val open = dist(innerC, topC) > 85
        // 안경: 콧등(눈 사이)이 피부보다 확실히 어두우면 안경 테
        val bridge = median(region(cx - .08f * ed, cx + .08f * ed, cy - .12f * ed, cy + .05f * ed), skin)
        val glasses = found && lum(skin) - lum(bridge) > 42
        // 바지: 사진 아래쪽
        val legsC = median(region(cx - 1.2f * ed, cx + 1.2f * ed, cy + 6f * ed, cy + 8f * ed), Color.parseColor("#2b3d6b"))
        val legsFound = cy + 6f * ed < h

        val look = Look(
            hair = hairStyle, bangs = bangs,
            hairColor = Palette.nearest(if (hairIsHair) hair else Palette.shade(skin, .6f), Palette.hair),
            skin = Palette.nearest(skin, Palette.skin),
            top = if (open) "vest" else if (lum(topC) < 70) "padding" else "tshirt",
            topColor = Palette.nearest(topC, Palette.cloth),
            top2Color = Palette.nearest(innerC, Palette.cloth),
            bottom = if (female && !legsFound) "pants" else "pants",
            bottomColor = if (legsFound) Palette.nearest(legsC, Palette.cloth) else Palette.cloth[12],
            shoeColor = Palette.cloth[5],
            acc = if (glasses) "glasses" else "none",
            female = female
        )
        if (small != src) small.recycle()
        face565.recycle()
        return Result(look, found)
    }
}
