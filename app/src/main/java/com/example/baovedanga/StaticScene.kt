package com.example.baovedanga

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Vẽ toàn bộ cảnh tĩnh (cỏ, cây, rau, đường đá, hàng rào) MỘT LẦN vào bitmap,
// rồi GameScreen chỉ việc vẽ lại bitmap mỗi khung hình. Nhờ vậy cảnh có thể
// chi tiết tới hàng nghìn ngọn cỏ mà game vẫn mượt.
fun renderStaticScene(
    widthPx: Int,
    heightPx: Int,
    density: Float,
    paths: List<ScaledPath>,
    pathCount: Int,
): ImageBitmap {
    val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val d = density
    val w = widthPx.toFloat()
    val h = heightPx.toFloat()
    val fx0 = FENCE_LEFT * w
    val fx1 = FENCE_RIGHT * w
    val fy0 = FENCE_TOP * h
    val fy1 = FENCE_BOTTOM * h

    val fillPaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // ===== 1. NỀN CỎ: gradient + mảng màu loang =====
    fillPaint.shader = LinearGradient(
        0f, 0f, 0f, h,
        0xFF8BC34A.toInt(), 0xFF558B2F.toInt(), Shader.TileMode.CLAMP,
    )
    c.drawRect(0f, 0f, w, h, fillPaint)
    fillPaint.shader = null
    val blobRandom = Random(11)
    repeat(60) {
        val bx = blobRandom.nextFloat() * w
        val by = blobRandom.nextFloat() * h
        val br = (60f + blobRandom.nextFloat() * 180f) * d
        val col = if (blobRandom.nextBoolean()) Color.argb(28, 156, 204, 101) else Color.argb(30, 51, 105, 30)
        fillPaint.shader = RadialGradient(bx, by, br, col, Color.argb(0, 0, 0, 0), Shader.TileMode.CLAMP)
        c.drawCircle(bx, by, br, fillPaint)
    }
    fillPaint.shader = null

    // ===== 2. HÀNG NGHÌN NGỌN CỎ (cong nhẹ, đủ sắc xanh) =====
    val bladeColors = intArrayOf(
        0xFF2E7D32.toInt(), 0xFF388E3C.toInt(), 0xFF43A047.toInt(),
        0xFF558B2F.toInt(), 0xFF7CB342.toInt(), 0xFF689F38.toInt(), 0xFF33691E.toInt(),
    )
    val bladeRandom = Random(23)
    val bladePath = Path()
    val bladeCount = (w * h / (45f * d * d)).toInt().coerceIn(3000, 8000)
    repeat(bladeCount) {
        val bx = bladeRandom.nextFloat() * w
        val by = bladeRandom.nextFloat() * h
        val len = (5f + bladeRandom.nextFloat() * 9f) * d
        val lean = (bladeRandom.nextFloat() - 0.5f) * len * 0.7f
        val bend = (bladeRandom.nextFloat() - 0.5f) * len * 0.4f
        bladePath.reset()
        bladePath.moveTo(bx, by)
        bladePath.quadTo(bx + bend, by - len * 0.6f, bx + lean, by - len)
        strokePaint.color = bladeColors[bladeRandom.nextInt(bladeColors.size)]
        strokePaint.strokeWidth = (1.1f + bladeRandom.nextFloat() * 1.3f) * d
        c.drawPath(bladePath, strokePaint)
    }
    // Cụm cỏ: 5-7 ngọn xòe ra từ 1 gốc, xanh sáng hơn.
    val tuftRandom = Random(31)
    repeat(320) {
        val bx = tuftRandom.nextFloat() * w
        val by = tuftRandom.nextFloat() * h
        repeat(5 + tuftRandom.nextInt(3)) {
            val len = (9f + tuftRandom.nextFloat() * 9f) * d
            val lean = (tuftRandom.nextFloat() - 0.5f) * len * 1.1f
            bladePath.reset()
            bladePath.moveTo(bx, by)
            bladePath.quadTo(bx + lean * 0.4f, by - len * 0.6f, bx + lean, by - len)
            strokePaint.color = if (tuftRandom.nextBoolean()) 0xFF7CB342.toInt() else 0xFF9CCC65.toInt()
            strokePaint.strokeWidth = (1.3f + tuftRandom.nextFloat()) * d
            c.drawPath(bladePath, strokePaint)
        }
    }
    // Hoa dại nhỏ + cỏ 3 lá lác đác.
    val flowerRandom = Random(47)
    repeat(45) {
        val fx = flowerRandom.nextFloat() * w
        val fy = flowerRandom.nextFloat() * h
        val petal = if (flowerRandom.nextBoolean()) 0xFFFFFFFF.toInt() else 0xFFF48FB1.toInt()
        val pr = (2.2f + flowerRandom.nextFloat()) * d
        for (k in 0 until 5) {
            val a = k * 1.2566f
            fillPaint.color = petal
            c.drawCircle(fx + cos(a) * pr * 1.6f, fy + sin(a) * pr * 1.6f, pr, fillPaint)
        }
        fillPaint.color = 0xFFFFD54F.toInt()
        c.drawCircle(fx, fy, pr * 0.9f, fillPaint)
    }
    repeat(30) {
        val fx = flowerRandom.nextFloat() * w
        val fy = flowerRandom.nextFloat() * h
        fillPaint.color = 0xFF43A047.toInt()
        for (k in 0 until 3) {
            val a = k * 2.094f
            c.drawCircle(fx + cos(a) * 3.5f * d, fy + sin(a) * 3.5f * d, 3f * d, fillPaint)
        }
    }

    // ===== 3. CÂY (tránh hàng rào và đường) =====
    val treeRandom = Random(42)
    repeat(40) {
        val tx = treeRandom.nextFloat() * w
        val ty = treeRandom.nextFloat() * h
        if (tx > fx0 - 24f * d && tx < fx1 + 24f * d &&
            ty > fy0 - 24f * d && ty < fy1 + 24f * d
        ) return@repeat
        var onPath = false
        for (i in 0 until pathCount) {
            val path = paths.getOrNull(i) ?: continue
            for (j in 1 until path.pts.size) {
                if (distToSegment(tx, ty, path.pts[j - 1].first, path.pts[j - 1].second, path.pts[j].first, path.pts[j].second) < 36f * d) {
                    onPath = true
                    break
                }
            }
            if (onPath) break
        }
        if (onPath) return@repeat
        val tr = (12f + treeRandom.nextFloat() * 9f) * d
        fillPaint.color = 0xFF1B5E20.toInt()
        c.drawCircle(tx, ty, tr, fillPaint)
        fillPaint.color = 0xFF2E7D32.toInt()
        c.drawCircle(tx - tr * 0.15f, ty - tr * 0.15f, tr * 0.72f, fillPaint)
        fillPaint.color = 0xFF43A047.toInt()
        c.drawCircle(tx - tr * 0.3f, ty - tr * 0.3f, tr * 0.4f, fillPaint)
        fillPaint.color = 0xFF66BB6A.toInt()
        c.drawCircle(tx + tr * 0.25f, ty + tr * 0.2f, tr * 0.16f, fillPaint)
    }

    // ===== 4. VƯỜN RAU trong hàng rào: rau mọc trên cỏ, mỗi cây có ụ đất nhỏ =====
    val vegRandom = Random(7)
    var ry = fy0 + 26f * d
    while (ry < fy1 - 14f * d) {
        var rx = fx0 + 22f * d
        while (rx < fx1 - 14f * d) {
            val dx = rx - NEST_X * w
            val dy = ry - NEST_Y * h
            if (dx * dx + dy * dy > (85f * d) * (85f * d)) {
                fillPaint.color = 0xFF8D6E63.toInt()
                c.drawOval(RectF(rx - 11f * d, ry + 2f * d, rx + 11f * d, ry + 12f * d), fillPaint)
                when (vegRandom.nextInt(3)) {
                    0 -> { // Cà rốt: củ cam + lá xanh.
                        val p = Path().apply {
                            moveTo(rx - 6f * d, ry - 4f * d)
                            lineTo(rx + 6f * d, ry - 4f * d)
                            lineTo(rx, ry + 10f * d)
                            close()
                        }
                        fillPaint.color = 0xFFEF6C00.toInt()
                        c.drawPath(p, fillPaint)
                        strokePaint.color = 0xFF2E7D32.toInt()
                        strokePaint.strokeWidth = 2.5f * d
                        c.drawLine(rx, ry - 4f * d, rx - 5f * d, ry - 12f * d, strokePaint)
                        c.drawLine(rx, ry - 4f * d, rx, ry - 13f * d, strokePaint)
                        c.drawLine(rx, ry - 4f * d, rx + 5f * d, ry - 12f * d, strokePaint)
                    }
                    1 -> { // Bắp cải: 3 lớp xanh.
                        fillPaint.color = 0xFF2E7D32.toInt()
                        c.drawCircle(rx, ry, 9f * d, fillPaint)
                        fillPaint.color = 0xFF66BB6A.toInt()
                        c.drawCircle(rx, ry, 6.5f * d, fillPaint)
                        fillPaint.color = 0xFFA5D6A7.toInt()
                        c.drawCircle(rx, ry, 3.5f * d, fillPaint)
                    }
                    else -> { // Cà chua đỏ + đài xanh.
                        fillPaint.color = 0xFFE53935.toInt()
                        c.drawCircle(rx, ry, 8f * d, fillPaint)
                        fillPaint.color = 0xFFEF5350.toInt()
                        c.drawCircle(rx - 2f * d, ry - 2f * d, 4f * d, fillPaint)
                        strokePaint.color = 0xFF2E7D32.toInt()
                        strokePaint.strokeWidth = 2f * d
                        c.drawLine(rx, ry - 8f * d, rx - 5f * d, ry - 12f * d, strokePaint)
                        c.drawLine(rx, ry - 8f * d, rx + 5f * d, ry - 12f * d, strokePaint)
                    }
                }
            }
            rx += 30f * d + vegRandom.nextFloat() * 8f * d
        }
        ry += 30f * d
    }

    // ===== 5. ĐƯỜNG ĐẤT MÒN + ĐÁ CUỘI 3D (chỉ các đường đang dùng ở level này) =====
    val stoneColors = intArrayOf(
        0xFF9E9E9E.toInt(), 0xFF8D8D8D.toInt(), 0xFFA1887F.toInt(),
        0xFFB0A090.toInt(), 0xFF8D7B68.toInt(), 0xFFBCAAA4.toInt(),
    )
    for (i in 0 until pathCount) {
        val path = paths.getOrNull(i) ?: continue
        val p = Path().apply {
            moveTo(path.pts[0].first, path.pts[0].second)
            for (j in 1 until path.pts.size) lineTo(path.pts[j].first, path.pts[j].second)
        }
        // Đất mòn: viền nâu đậm + nền đất + vệt mòn sáng ở giữa.
        strokePaint.color = 0xFF4E342E.toInt()
        strokePaint.strokeWidth = 56f * d
        c.drawPath(p, strokePaint)
        strokePaint.color = 0xFF8D6E63.toInt()
        strokePaint.strokeWidth = 50f * d
        c.drawPath(p, strokePaint)
        strokePaint.color = 0xFFA1887F.toInt()
        strokePaint.strokeWidth = 30f * d
        c.drawPath(p, strokePaint)
        // Đá cuội tròn lồi: bóng đổ + thân đá + điểm sáng, xếp so le 3 cột.
        val stoneRandom = Random(100 + i)
        for (j in 1 until path.pts.size) {
            val (ax, ay) = path.pts[j - 1]
            val (bx, by) = path.pts[j]
            val segLen = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
            if (segLen < 1f) continue
            val ux = (bx - ax) / segLen
            val uy = (by - ay) / segLen
            val px = -uy
            val py = ux
            var dist = 8f * d
            var row = 0
            while (dist < segLen) {
                val t = dist / segLen
                val cx = ax + (bx - ax) * t
                val cy = ay + (by - ay) * t
                for (col in 0 until 3) {
                    val off = (col - 1) * 14f * d + (stoneRandom.nextFloat() - 0.5f) * 5f * d
                    val shift = if (row % 2 == 1 && col != 1) 8f * d else 0f
                    val sx = cx + px * off + ux * shift
                    val sy = cy + py * off + uy * shift
                    val sr = (5f + stoneRandom.nextFloat() * 3.5f) * d
                    fillPaint.color = Color.argb(70, 0, 0, 0)
                    c.drawCircle(sx + 1.5f * d, sy + 2f * d, sr, fillPaint)
                    fillPaint.color = stoneColors[stoneRandom.nextInt(stoneColors.size)]
                    c.drawCircle(sx, sy, sr, fillPaint)
                    fillPaint.color = Color.argb(80, 255, 255, 255)
                    c.drawCircle(sx - sr * 0.35f, sy - sr * 0.35f, sr * 0.45f, fillPaint)
                }
                dist += 16f * d
                row++
            }
        }
        // Lấm tấm đất: chấm tối/sáng lác đác trên mặt đường.
        val speckRandom = Random(300 + i)
        for (j in 1 until path.pts.size) {
            val (ax, ay) = path.pts[j - 1]
            val (bx, by) = path.pts[j]
            val segLen = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
            var dist = 12f * d
            while (dist < segLen) {
                val t = dist / segLen
                val sx = ax + (bx - ax) * t + (speckRandom.nextFloat() - 0.5f) * 40f * d
                val sy = ay + (by - ay) * t + (speckRandom.nextFloat() - 0.5f) * 40f * d
                fillPaint.color = if (speckRandom.nextBoolean()) Color.argb(110, 62, 39, 35) else Color.argb(100, 215, 204, 200)
                c.drawCircle(sx, sy, (1f + speckRandom.nextFloat() * 1.5f) * d, fillPaint)
                dist += 13f * d + speckRandom.nextFloat() * 10f * d
            }
        }
        // Đá to 2 mép đường.
        val edgeRandom = Random(200 + i)
        for (j in 1 until path.pts.size) {
            val (ax, ay) = path.pts[j - 1]
            val (bx, by) = path.pts[j]
            val segLen = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
            if (segLen < 1f) continue
            val ux = (bx - ax) / segLen
            val uy = (by - ay) / segLen
            val px = -uy
            val py = ux
            var dist = 15f * d
            while (dist < segLen) {
                val t = dist / segLen
                val side = if (edgeRandom.nextBoolean()) 1f else -1f
                val off = side * (26f + edgeRandom.nextFloat() * 3f) * d
                val ex = ax + (bx - ax) * t + px * off
                val ey = ay + (by - ay) * t + py * off
                val er = (4f + edgeRandom.nextFloat() * 3f) * d
                fillPaint.color = 0xFF78909C.toInt()
                c.drawCircle(ex, ey, er, fillPaint)
                fillPaint.color = 0xFF90A4AE.toInt()
                c.drawCircle(ex - er * 0.3f, ey - er * 0.3f, er * 0.55f, fillPaint)
                dist += 30f * d + edgeRandom.nextFloat() * 22f * d
            }
        }
        // Cỏ mọc lún phún 2 mép đường.
        val tuftRandom = Random(500 + i)
        for (j in 1 until path.pts.size) {
            val (ax, ay) = path.pts[j - 1]
            val (bx, by) = path.pts[j]
            val segLen = sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
            if (segLen < 1f) continue
            val px = -(by - ay) / segLen
            val py = (bx - ax) / segLen
            var dist = 30f * d
            while (dist < segLen) {
                val t = dist / segLen
                val side = if (tuftRandom.nextBoolean()) 1f else -1f
                val off = side * (24f + tuftRandom.nextFloat() * 4f) * d
                val tx = ax + (bx - ax) * t + px * off
                val ty = ay + (by - ay) * t + py * off
                strokePaint.color = 0xFF2E7D32.toInt()
                strokePaint.strokeWidth = 2f * d
                c.drawLine(tx, ty, tx - 4f * d, ty - 7f * d, strokePaint)
                c.drawLine(tx, ty, tx, ty - 8f * d, strokePaint)
                c.drawLine(tx, ty, tx + 4f * d, ty - 7f * d, strokePaint)
                dist += 44f * d + tuftRandom.nextFloat() * 30f * d
            }
        }
    }

    // ===== 6. HÀNG RÀO GỖ THÔ: khúc gỗ tròn xù xì, nghiêng lệch, có rêu =====
    // Tìm chỗ đường cắt qua từng cạnh rào để chừa khe hở (cổng rào vẽ động).
    val leftGaps = mutableListOf<Float>()
    val rightGaps = mutableListOf<Float>()
    val topGaps = mutableListOf<Float>()
    val bottomGaps = mutableListOf<Float>()
    for (i in 0 until pathCount) {
        val path = paths.getOrNull(i) ?: continue
        for (j in 1 until path.pts.size) {
            val (x0, y0) = path.pts[j - 1]
            val (x1, y1) = path.pts[j]
            if (y0 == y1) { // đoạn ngang: cắt cạnh trái/phải
                if ((x0 < fx0 && x1 >= fx0) || (x1 < fx0 && x0 >= fx0)) leftGaps.add(y0)
                if ((x0 < fx1 && x1 >= fx1) || (x1 < fx1 && x0 >= fx1)) rightGaps.add(y0)
            }
            if (x0 == x1) { // đoạn dọc: cắt cạnh trên/dưới
                if ((y0 < fy0 && y1 >= fy0) || (y1 < fy0 && y0 >= fy0)) topGaps.add(x0)
                if ((y0 < fy1 && y1 >= fy1) || (y1 < fy1 && y0 >= fy1)) bottomGaps.add(x0)
            }
        }
    }
    val gapR = 34f * d
    fun nearGap(v: Float, gaps: List<Float>) = gaps.any { kotlin.math.abs(v - it) < gapR }
    // Vệt đất mòn + bóng đổ mặt đất dọc theo toàn bộ hàng rào.
    strokePaint.color = Color.argb(100, 109, 76, 65)
    strokePaint.strokeWidth = 30f * d
    val fenceRect = Path().apply {
        moveTo(fx0, fy0); lineTo(fx1, fy0); lineTo(fx1, fy1); lineTo(fx0, fy1); close()
    }
    c.drawPath(fenceRect, strokePaint)
    strokePaint.color = Color.argb(35, 0, 0, 0)
    strokePaint.strokeWidth = 22f * d
    c.drawPath(fenceRect, strokePaint)
    // Tường đá chân: hàng đá cuội tròn xếp dọc rào, chừa khe hở.
    val baseStoneColors = intArrayOf(0xFF9E9E9E.toInt(), 0xFF8D8D8D.toInt(), 0xFFA1887F.toInt(), 0xFFB0A090.toInt())
    val baseRandom = Random(88)
    fun baseStonesH(y: Float, gaps: List<Float>) {
        var x = fx0 + 4f * d
        while (x < fx1) {
            if (!nearGap(x, gaps)) {
                val sr = (6f + baseRandom.nextFloat() * 2f) * d
                val jy = (baseRandom.nextFloat() - 0.5f) * 2f * d
                fillPaint.color = Color.argb(60, 0, 0, 0)
                c.drawOval(RectF(x - sr, y - sr * 0.72f + 2f * d + jy, x + sr, y + sr * 0.72f + 2f * d + jy), fillPaint)
                fillPaint.color = baseStoneColors[baseRandom.nextInt(baseStoneColors.size)]
                c.drawOval(RectF(x - sr, y - sr * 0.72f + jy, x + sr, y + sr * 0.72f + jy), fillPaint)
                fillPaint.color = Color.argb(70, 255, 255, 255)
                c.drawCircle(x - sr * 0.3f, y - sr * 0.3f + jy, sr * 0.3f, fillPaint)
            }
            x += 13f * d
        }
    }
    fun baseStonesV(x: Float, gaps: List<Float>) {
        var y = fy0 + 4f * d
        while (y < fy1) {
            if (!nearGap(y, gaps)) {
                val sr = (6f + baseRandom.nextFloat() * 2f) * d
                val jx = (baseRandom.nextFloat() - 0.5f) * 2f * d
                fillPaint.color = Color.argb(60, 0, 0, 0)
                c.drawOval(RectF(x - sr * 0.72f + 2f * d + jx, y - sr, x + sr * 0.72f + 2f * d + jx, y + sr), fillPaint)
                fillPaint.color = baseStoneColors[baseRandom.nextInt(baseStoneColors.size)]
                c.drawOval(RectF(x - sr * 0.72f + jx, y - sr, x + sr * 0.72f + jx, y + sr), fillPaint)
                fillPaint.color = Color.argb(70, 255, 255, 255)
                c.drawCircle(x - sr * 0.3f + jx, y - sr * 0.3f, sr * 0.3f, fillPaint)
            }
            y += 13f * d
        }
    }
    baseStonesH(fy0, topGaps); baseStonesH(fy1, bottomGaps)
    baseStonesV(fx0, leftGaps); baseStonesV(fx1, rightGaps)
    // Thanh ngang gỗ tròn sau lưng cọc, chừa khe hở.
    fun hRail(y: Float, x0: Float, x1: Float, gaps: List<Float>) {
        val sorted = (gaps.map { it - gapR } + gaps.map { it + gapR }).sorted()
        val cuts = listOf(x0) + sorted.filter { it > x0 && it < x1 } + listOf(x1)
        var k = 0
        while (k < cuts.size - 1) {
            val a = cuts[k]
            val b = cuts[k + 1]
            val mid = (a + b) / 2f
            if (!nearGap(mid, gaps) && b - a > 4f * d) {
                strokePaint.color = Color.argb(55, 0, 0, 0)
                strokePaint.strokeWidth = 9f * d
                c.drawLine(a + 2f * d, y + 3f * d, b + 2f * d, y + 3f * d, strokePaint)
                strokePaint.color = 0xFF5D4037.toInt()
                strokePaint.strokeWidth = 7f * d
                c.drawLine(a, y, b, y, strokePaint)
                strokePaint.color = Color.argb(110, 141, 110, 99)
                strokePaint.strokeWidth = 1.5f * d
                c.drawLine(a + 2f * d, y - 2f * d, b - 2f * d, y - 2f * d, strokePaint)
            }
            k++
        }
    }
    fun vRail(x: Float, y0: Float, y1: Float, gaps: List<Float>) {
        val sorted = (gaps.map { it - gapR } + gaps.map { it + gapR }).sorted()
        val cuts = listOf(y0) + sorted.filter { it > y0 && it < y1 } + listOf(y1)
        var k = 0
        while (k < cuts.size - 1) {
            val a = cuts[k]
            val b = cuts[k + 1]
            val mid = (a + b) / 2f
            if (!nearGap(mid, gaps) && b - a > 4f * d) {
                strokePaint.color = Color.argb(55, 0, 0, 0)
                strokePaint.strokeWidth = 9f * d
                c.drawLine(x + 2f * d, a + 3f * d, x + 2f * d, b + 3f * d, strokePaint)
                strokePaint.color = 0xFF5D4037.toInt()
                strokePaint.strokeWidth = 7f * d
                c.drawLine(x, a, x, b, strokePaint)
                strokePaint.color = Color.argb(110, 141, 110, 99)
                strokePaint.strokeWidth = 1.5f * d
                c.drawLine(x - 2f * d, a + 2f * d, x - 2f * d, b - 2f * d, strokePaint)
            }
            k++
        }
    }
    hRail(fy0 - 7f * d, fx0, fx1, topGaps); hRail(fy0 + 7f * d, fx0, fx1, topGaps)
    hRail(fy1 - 7f * d, fx0, fx1, bottomGaps); hRail(fy1 + 7f * d, fx0, fx1, bottomGaps)
    vRail(fx0 - 7f * d, fy0, fy1, leftGaps); vRail(fx0 + 7f * d, fy0, fy1, leftGaps)
    vRail(fx1 - 7f * d, fy0, fy1, rightGaps); vRail(fx1 + 7f * d, fy0, fy1, rightGaps)
    // Cọc gỗ tròn thô: nghiêng lệch ngẫu nhiên, cao thấp khác nhau, đầu bo
    // tròn, có sáng/tối 2 bên (khối 3D), vân gỗ sóng, mắt gỗ, rêu ở chân.
    val logShades = intArrayOf(0xFF8D6E63.toInt(), 0xFF795548.toInt(), 0xFF6D4C41.toInt(), 0xFFA1887F.toInt(), 0xFF7B5E57.toInt())
    val logRandom = Random(77)
    val grainPath = Path()
    fun picketH(x: Float, y: Float, up: Boolean) {
        val w2 = (5f + logRandom.nextFloat() * 1.5f) * d
        val len = (24f + logRandom.nextFloat() * 6f) * d
        val tilt = (logRandom.nextFloat() - 0.5f) * 8f
        val shade = logShades[logRandom.nextInt(logShades.size)]
        c.save()
        c.rotate(tilt, x, y)
        val top = if (up) y - len else y
        val bottom = if (up) y else y + len
        // Bóng đổ.
        fillPaint.color = Color.argb(55, 0, 0, 0)
        c.drawRoundRect(RectF(x - w2 + 2f * d, top + 3f * d, x + w2 + 2f * d, bottom + 3f * d), w2, w2, fillPaint)
        // Thân gỗ.
        fillPaint.color = shade
        c.drawRoundRect(RectF(x - w2, top, x + w2, bottom), w2, w2, fillPaint)
        // Sáng bên trái, tối bên phải (khối tròn 3D).
        fillPaint.color = Color.argb(55, 255, 255, 255)
        c.drawRoundRect(RectF(x - w2, top, x - w2 + 3.5f * d, bottom), w2, w2, fillPaint)
        fillPaint.color = Color.argb(65, 0, 0, 0)
        c.drawRoundRect(RectF(x + w2 - 3.5f * d, top, x + w2, bottom), w2, w2, fillPaint)
        // Viền tối quanh thân.
        strokePaint.color = Color.argb(150, 62, 39, 35)
        strokePaint.strokeWidth = 1.5f * d
        c.drawRoundRect(RectF(x - w2, top, x + w2, bottom), w2, w2, strokePaint)
        // Vân gỗ sóng: 2 đường cong dọc.
        strokePaint.color = Color.argb(90, 62, 39, 35)
        strokePaint.strokeWidth = 1.2f * d
        for (gv in listOf(-0.35f, 0.3f)) {
            val gx = x + gv * w2 * 2f
            grainPath.reset()
            grainPath.moveTo(gx, if (up) bottom - 4f * d else top + 4f * d)
            grainPath.quadTo(
                gx + 2.5f * d,
                (top + bottom) / 2f,
                gx - 1.5f * d,
                if (up) top + 6f * d else bottom - 6f * d,
            )
            c.drawPath(grainPath, strokePaint)
        }
        // Mắt gỗ: 1/4 cọc có.
        if (logRandom.nextFloat() < 0.25f) {
            val ky = (top + bottom) / 2f + (logRandom.nextFloat() - 0.5f) * len * 0.4f
            fillPaint.color = Color.argb(120, 62, 39, 35)
            c.drawCircle(x, ky, 2.5f * d, fillPaint)
            strokePaint.color = Color.argb(90, 62, 39, 35)
            strokePaint.strokeWidth = 1f * d
            c.drawCircle(x, ky, 4f * d, strokePaint)
        }
        // Đinh ở chỗ thanh ngang đi qua.
        fillPaint.color = 0xFF3E2723.toInt()
        c.drawCircle(x, if (up) y - 7f * d else y + 7f * d, 1.6f * d, fillPaint)
        // Rêu ở chân: 30% cọc có.
        if (logRandom.nextFloat() < 0.3f) {
            fillPaint.color = 0xFF558B2F.toInt()
            val my = if (up) bottom - 3f * d else top + 3f * d
            c.drawCircle(x - w2 * 0.5f, my, 2.2f * d, fillPaint)
            c.drawCircle(x + w2 * 0.4f, my - 2f * d, 1.8f * d, fillPaint)
            fillPaint.color = 0xFF7CB342.toInt()
            c.drawCircle(x, my - 1f * d, 1.5f * d, fillPaint)
        }
        c.restore()
    }
    fun picketV(x: Float, y: Float, left: Boolean) {
        val w2 = (5f + logRandom.nextFloat() * 1.5f) * d
        val len = (24f + logRandom.nextFloat() * 6f) * d
        val tilt = (logRandom.nextFloat() - 0.5f) * 8f
        val shade = logShades[logRandom.nextInt(logShades.size)]
        c.save()
        c.rotate(tilt, x, y)
        val near = if (left) x - len else x
        val far = if (left) x else x + len
        fillPaint.color = Color.argb(55, 0, 0, 0)
        c.drawRoundRect(RectF(near + 2f * d, y - w2 + 3f * d, far + 2f * d, y + w2 + 3f * d), w2, w2, fillPaint)
        fillPaint.color = shade
        c.drawRoundRect(RectF(near, y - w2, far, y + w2), w2, w2, fillPaint)
        fillPaint.color = Color.argb(55, 255, 255, 255)
        c.drawRoundRect(RectF(near, y - w2, far, y - w2 + 3.5f * d), w2, w2, fillPaint)
        fillPaint.color = Color.argb(65, 0, 0, 0)
        c.drawRoundRect(RectF(near, y + w2 - 3.5f * d, far, y + w2), w2, w2, fillPaint)
        strokePaint.color = Color.argb(150, 62, 39, 35)
        strokePaint.strokeWidth = 1.5f * d
        c.drawRoundRect(RectF(near, y - w2, far, y + w2), w2, w2, strokePaint)
        strokePaint.color = Color.argb(90, 62, 39, 35)
        strokePaint.strokeWidth = 1.2f * d
        for (gv in listOf(-0.35f, 0.3f)) {
            val gy = y + gv * w2 * 2f
            grainPath.reset()
            grainPath.moveTo(if (left) far - 4f * d else near + 4f * d, gy)
            grainPath.quadTo(
                (near + far) / 2f,
                gy + 2.5f * d,
                if (left) near + 6f * d else far - 6f * d,
                gy - 1.5f * d,
            )
            c.drawPath(grainPath, strokePaint)
        }
        if (logRandom.nextFloat() < 0.25f) {
            val kx = (near + far) / 2f + (logRandom.nextFloat() - 0.5f) * len * 0.4f
            fillPaint.color = Color.argb(120, 62, 39, 35)
            c.drawCircle(kx, y, 2.5f * d, fillPaint)
            strokePaint.color = Color.argb(90, 62, 39, 35)
            strokePaint.strokeWidth = 1f * d
            c.drawCircle(kx, y, 4f * d, strokePaint)
        }
        fillPaint.color = 0xFF3E2723.toInt()
        c.drawCircle(if (left) x - 7f * d else x + 7f * d, y, 1.6f * d, fillPaint)
        if (logRandom.nextFloat() < 0.3f) {
            fillPaint.color = 0xFF558B2F.toInt()
            val mx = if (left) far - 3f * d else near + 3f * d
            c.drawCircle(mx, y - w2 * 0.5f, 2.2f * d, fillPaint)
            c.drawCircle(mx - 2f * d, y + w2 * 0.4f, 1.8f * d, fillPaint)
            fillPaint.color = 0xFF7CB342.toInt()
            c.drawCircle(mx - 1f * d, y, 1.5f * d, fillPaint)
        }
        c.restore()
    }
    var px = fx0 + 8f * d
    while (px < fx1) {
        if (!nearGap(px, topGaps)) picketH(px, fy0, true)
        if (!nearGap(px, bottomGaps)) picketH(px, fy1, false)
        px += 16f * d
    }
    var py = fy0 + 8f * d
    while (py < fy1) {
        if (!nearGap(py, leftGaps)) picketV(fx0, py, true)
        if (!nearGap(py, rightGaps)) picketV(fx1, py, false)
        py += 16f * d
    }
    // Cột góc to + đèn lồng vàng phát sáng.
    for ((cx, cy) in listOf(fx0 to fy0, fx1 to fy0, fx0 to fy1, fx1 to fy1)) {
        fillPaint.shader = RadialGradient(cx, cy - 6f * d, 24f * d, Color.argb(110, 255, 213, 79), Color.argb(0, 255, 213, 79), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy - 6f * d, 24f * d, fillPaint)
        fillPaint.shader = null
        fillPaint.color = 0xFF4E342E.toInt()
        c.drawRoundRect(RectF(cx - 8f * d, cy - 8f * d, cx + 8f * d, cy + 8f * d), 2f * d, 2f * d, fillPaint)
        fillPaint.color = 0xFF6D4C41.toInt()
        c.drawRoundRect(RectF(cx - 6f * d, cy - 6f * d, cx + 6f * d, cy + 6f * d), 2f * d, 2f * d, fillPaint)
        strokePaint.color = 0xFF3E2723.toInt()
        strokePaint.strokeWidth = 1.5f * d
        c.drawLine(cx, cy - 8f * d, cx, cy - 14f * d, strokePaint)
        fillPaint.color = 0xFFFFD54F.toInt()
        c.drawRoundRect(RectF(cx - 5f * d, cy - 20f * d, cx + 5f * d, cy - 12f * d), 2f * d, 2f * d, fillPaint)
        fillPaint.color = 0xFFEF6C00.toInt()
        c.drawRect(RectF(cx - 5f * d, cy - 21f * d, cx + 5f * d, cy - 19f * d), fillPaint)
        fillPaint.color = 0xFFFFF59D.toInt()
        c.drawCircle(cx, cy - 16f * d, 2.5f * d, fillPaint)
    }

    return bmp.asImageBitmap()
}

// Khoảng cách từ điểm tới đoạn thẳng (để tránh trồng cây lên đường).
private fun distToSegment(
    px: Float, py: Float,
    x0: Float, y0: Float, x1: Float, y1: Float,
): Float {
    val dx = x1 - x0
    val dy = y1 - y0
    val lenSq = dx * dx + dy * dy
    val t = if (lenSq == 0f) 0f else (((px - x0) * dx + (py - y0) * dy) / lenSq).coerceIn(0f, 1f)
    val cx = x0 + t * dx
    val cy = y0 + t * dy
    val ddx = px - cx
    val ddy = py - cy
    return sqrt(ddx * ddx + ddy * ddy)
}
