package com.example.baovedanga

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// Vẽ ảnh vừa khít trong ô vuông size×size, giữ nguyên tỉ lệ.
private fun DrawScope.drawSprite(
    image: ImageBitmap,
    centerX: Float,
    centerY: Float,
    size: Float,
    alpha: Float = 1f,
    colorFilter: ColorFilter? = null,
) {
    val iw = image.width.toFloat()
    val ih = image.height.toFloat()
    val scale = size / max(iw, ih)
    val dw = iw * scale
    val dh = ih * scale
    drawImage(
        image,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(image.width, image.height),
        dstOffset = IntOffset((centerX - dw / 2f).roundToInt(), (centerY - dh / 2f).roundToInt()),
        dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
        alpha = alpha,
        colorFilter = colorFilter,
    )
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
    return kotlin.math.sqrt(ddx * ddx + ddy * ddy)
}

@Composable
fun GameScreen(
    level: LevelDef,
    weapon: Weapon,
    save: SaveManager,
    onExit: () -> Unit,
    onWin: (Int) -> Unit, // thắng: chuyển sang cửa hàng, kèm tổng xu kiếm được ở màn này
) {
    val density = LocalDensity.current.density
    var restartKey by remember { mutableIntStateOf(0) }
    val game = remember(level.number, restartKey) { GameState(level, weapon, density) }
    var rewardsApplied by remember(level.number, restartKey) { mutableStateOf(false) }

    // Ảnh quái, gà, vũ khí: nạp 1 lần bằng BitmapFactory rồi đổi sang ImageBitmap.
    val context = LocalContext.current
    val monsterImages = remember { MONSTER_TYPES.associate { it.id to BitmapFactory.decodeResource(context.resources, it.imageRes).asImageBitmap() } }
    val chickenImage = remember { BitmapFactory.decodeResource(context.resources, R.drawable.chicken).asImageBitmap() }
    val weaponImages = remember { WEAPONS.associate { it.id to BitmapFactory.decodeResource(context.resources, it.imageRes).asImageBitmap() } }

    // Nhận xu + mở level mới khi thắng/thua (chỉ 1 lần mỗi ván).
    // Thắng thì chuyển sang cửa hàng ngay.
    LaunchedEffect(game.status) {
        if (rewardsApplied) return@LaunchedEffect
        when (game.status) {
            GameStatus.WON -> {
                val total = game.coinsEarned + winBonus(level.number)
                save.addCoins(total)
                save.unlockLevel(level.number + 1)
                rewardsApplied = true
                onWin(total)
            }
            GameStatus.LOST -> {
                save.addCoins(game.coinsEarned)
                rewardsApplied = true
            }
            else -> {}
        }
    }

    // Vòng lặp game theo từng khung hình.
    LaunchedEffect(game) {
        var last = withFrameNanos { it }
        while (true) {
            val t = withFrameNanos { it }
            val dt = ((t - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
            last = t
            game.update(dt)
        }
    }

    BackHandler(enabled = game.status == GameStatus.PLAYING || game.status == GameStatus.PAUSED) {
        if (game.status == GameStatus.PLAYING) game.pause() else game.resume()
    }

    val emojiPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    }
    // Chữ máu "3/3": vẽ viền đen rồi tô trắng cho dễ đọc trên mọi nền.
    val hpFillPaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            color = android.graphics.Color.WHITE
        }
    }
    val hpStrokePaint = remember {
        Paint().apply {
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = android.graphics.Color.BLACK
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20))
    ) {
        // ===== HUD =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Level ${level.number}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            Text("\uD83D\uDC14 × ${game.chickens}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "Đợt ${game.waveIndex + 1}/${level.waves.size}",
                color = Color.White,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text("\uD83E\uDE99 ${game.coinsEarned}", color = Color(0xFFFFD54F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = { game.pause() }) {
                Text("⏸", fontSize = 22.sp)
            }
        }

        // ===== VÙNG CHƠI =====
        Box(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { game.configure(it.width.toFloat(), it.height.toFloat()) }
                    // Chạm đa điểm: mỗi ngón tay chạm xuống là 1 cú đánh riêng,
                    // không chờ nhấc tay nên đánh nhanh nhiều chỗ cùng lúc được.
                    .pointerInput(game) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    for (change in event.changes) {
                                        game.tap(change.position.x, change.position.y)
                                    }
                                }
                                // Hết cử chỉ khi nhấc hết các ngón tay.
                                if (event.type == PointerEventType.Release && event.changes.none { it.pressed }) break
                            }
                        }
                    }
            ) {
                @Suppress("UNUSED_EXPRESSION")
                game.frame // đọc để vẽ lại mỗi khung hình
                val w = size.width
                val h = size.height
                val d = density
                val now = System.currentTimeMillis()

                val fx0 = FENCE_LEFT * w
                val fx1 = FENCE_RIGHT * w
                val fy0 = FENCE_TOP * h
                val fy1 = FENCE_BOTTOM * h

                // --- Rừng: nền xanh đậm + cây (tránh hàng rào và đường) ---
                drawRect(Color(0xFF2E7D32))
                val treeRandom = Random(42)
                repeat(40) {
                    val tx = treeRandom.nextFloat() * w
                    val ty = treeRandom.nextFloat() * h
                    if (tx > fx0 - 24f * d && tx < fx1 + 24f * d &&
                        ty > fy0 - 24f * d && ty < fy1 + 24f * d
                    ) return@repeat
                    var onPath = false
                    for (i in 0 until level.pathCount) {
                        val path = game.paths.getOrNull(i) ?: continue
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
                    drawCircle(Color(0xFF1B5E20), radius = tr, center = Offset(tx, ty))
                    drawCircle(Color(0xFF388E3C), radius = tr * 0.6f, center = Offset(tx - tr * 0.2f, ty - tr * 0.2f))
                }

                // --- Vườn rau trong hàng rào ---
                drawRoundRect(
                    color = Color(0xFFA5D6A7),
                    topLeft = Offset(fx0, fy0),
                    size = Size(fx1 - fx0, fy1 - fy0),
                    cornerRadius = CornerRadius(24f * d, 24f * d),
                )
                // Luống rau đẹp: cà rốt, bắp cải, cà chua xen kẽ.
                val vegRandom = Random(7)
                var ry = fy0 + 26f * d
                while (ry < fy1 - 14f * d) {
                    var rx = fx0 + 22f * d
                    while (rx < fx1 - 14f * d) {
                        val dx = rx - NEST_X * w
                        val dy = ry - NEST_Y * h
                        if (dx * dx + dy * dy > (85f * d) * (85f * d)) {
                            when (vegRandom.nextInt(3)) {
                                0 -> { // Cà rốt: củ cam + lá xanh.
                                    val p = Path().apply {
                                        moveTo(rx - 6f * d, ry - 4f * d)
                                        lineTo(rx + 6f * d, ry - 4f * d)
                                        lineTo(rx, ry + 10f * d)
                                        close()
                                    }
                                    drawPath(p, Color(0xFFEF6C00))
                                    drawLine(Color(0xFF2E7D32), Offset(rx, ry - 4f * d), Offset(rx - 5f * d, ry - 12f * d), 2.5f * d, StrokeCap.Round)
                                    drawLine(Color(0xFF2E7D32), Offset(rx, ry - 4f * d), Offset(rx, ry - 13f * d), 2.5f * d, StrokeCap.Round)
                                    drawLine(Color(0xFF2E7D32), Offset(rx, ry - 4f * d), Offset(rx + 5f * d, ry - 12f * d), 2.5f * d, StrokeCap.Round)
                                }
                                1 -> { // Bắp cải: 3 lớp xanh.
                                    drawCircle(Color(0xFF2E7D32), radius = 9f * d, center = Offset(rx, ry))
                                    drawCircle(Color(0xFF66BB6A), radius = 6.5f * d, center = Offset(rx, ry))
                                    drawCircle(Color(0xFFA5D6A7), radius = 3.5f * d, center = Offset(rx, ry))
                                }
                                else -> { // Cà chua đỏ + đài xanh.
                                    drawCircle(Color(0xFFE53935), radius = 8f * d, center = Offset(rx, ry))
                                    drawCircle(Color(0xFFEF5350), radius = 4f * d, center = Offset(rx - 2f * d, ry - 2f * d))
                                    drawLine(Color(0xFF2E7D32), Offset(rx, ry - 8f * d), Offset(rx - 5f * d, ry - 12f * d), 2f * d, StrokeCap.Round)
                                    drawLine(Color(0xFF2E7D32), Offset(rx, ry - 8f * d), Offset(rx + 5f * d, ry - 12f * d), 2f * d, StrokeCap.Round)
                                }
                            }
                        }
                        rx += 30f * d + vegRandom.nextFloat() * 8f * d
                    }
                    ry += 30f * d
                }

                // --- Đường đất gấp khúc 90° (chỉ các đường đang dùng ở level này) ---
                // Vẽ bằng Path + Stroke join tròn nên các góc gấp khúc được bo tròn.
                for (i in 0 until level.pathCount) {
                    val path = game.paths.getOrNull(i) ?: continue
                    val p = Path().apply {
                        moveTo(path.pts[0].first, path.pts[0].second)
                        for (j in 1 until path.pts.size) lineTo(path.pts[j].first, path.pts[j].second)
                    }
                    drawPath(p, Color(0xFF8D6E63), style = Stroke(width = 46f * d, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(p, Color(0xFFD7CCC8), style = Stroke(width = 34f * d, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Sỏi đá lấm tấm trên đường.
                    val pebbleRandom = Random(100 + i)
                    for (j in 1 until path.pts.size) {
                        val (ax, ay) = path.pts[j - 1]
                        val (bx, by) = path.pts[j]
                        val segLen = kotlin.math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay))
                        var dist = 20f * d
                        while (dist < segLen) {
                            val t = dist / segLen
                            val px = ax + (bx - ax) * t + (pebbleRandom.nextFloat() - 0.5f) * 20f * d
                            val py = ay + (by - ay) * t + (pebbleRandom.nextFloat() - 0.5f) * 20f * d
                            drawCircle(Color(0xFFBCAAA4), radius = (1.5f + pebbleRandom.nextFloat() * 2f) * d, center = Offset(px, py))
                            dist += 26f * d + pebbleRandom.nextFloat() * 20f * d
                        }
                    }
                }

                // --- Hàng rào cọc gỗ màu cam, có khe hở ở chỗ đường đi qua ---
                // Tìm chỗ đường cắt qua từng cạnh rào để chừa khe hở.
                val leftGaps = mutableListOf<Float>()
                val rightGaps = mutableListOf<Float>()
                val topGaps = mutableListOf<Float>()
                val bottomGaps = mutableListOf<Float>()
                for (i in 0 until level.pathCount) {
                    val path = game.paths.getOrNull(i) ?: continue
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
                val picketColor = Color(0xFFFF8C42)
                val railColor = Color(0xFFEF6C00)
                val gapR = 34f * d
                fun nearGap(v: Float, gaps: List<Float>) = gaps.any { kotlin.math.abs(v - it) < gapR }
                // Thanh ngang của rào: vẽ thành đoạn, chừa khe hở.
                fun hRail(y: Float, x0: Float, x1: Float, gaps: List<Float>) {
                    val sorted = (gaps.map { it - gapR } + gaps.map { it + gapR }).sorted()
                    val cuts = listOf(x0) + sorted.filter { it > x0 && it < x1 } + listOf(x1)
                    var k = 0
                    while (k < cuts.size - 1) {
                        val a = cuts[k]
                        val b = cuts[k + 1]
                        val mid = (a + b) / 2f
                        if (!nearGap(mid, gaps) && b - a > 4f * d) {
                            drawLine(railColor, Offset(a, y), Offset(b, y), 5f * d, StrokeCap.Round)
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
                            drawLine(railColor, Offset(x, a), Offset(x, b), 5f * d, StrokeCap.Round)
                        }
                        k++
                    }
                }
                // 2 thanh ngang mỗi cạnh (lệch vào/ra 7dp so với mép rào).
                hRail(fy0 - 7f * d, fx0, fx1, topGaps); hRail(fy0 + 7f * d, fx0, fx1, topGaps)
                hRail(fy1 - 7f * d, fx0, fx1, bottomGaps); hRail(fy1 + 7f * d, fx0, fx1, bottomGaps)
                vRail(fx0 - 7f * d, fy0, fy1, leftGaps); vRail(fx0 + 7f * d, fy0, fy1, leftGaps)
                vRail(fx1 - 7f * d, fy0, fy1, rightGaps); vRail(fx1 + 7f * d, fy0, fy1, rightGaps)
                // Cọc rào: thanh dọc đầu nhọn hướng ra ngoài, cách nhau 16dp.
                fun picketH(x: Float, y: Float, up: Boolean) {
                    val w2 = 5f * d
                    val len = 20f * d
                    val tip = 8f * d
                    if (up) {
                        drawRoundRect(picketColor, Offset(x - w2, y - len), Size(w2 * 2, len), CornerRadius(3f * d, 3f * d))
                        val p = Path().apply {
                            moveTo(x - w2, y - len)
                            lineTo(x + w2, y - len)
                            lineTo(x, y - len - tip)
                            close()
                        }
                        drawPath(p, picketColor)
                    } else {
                        drawRoundRect(picketColor, Offset(x - w2, y), Size(w2 * 2, len), CornerRadius(3f * d, 3f * d))
                        val p = Path().apply {
                            moveTo(x - w2, y + len)
                            lineTo(x + w2, y + len)
                            lineTo(x, y + len + tip)
                            close()
                        }
                        drawPath(p, picketColor)
                    }
                }
                fun picketV(x: Float, y: Float, left: Boolean) {
                    val w2 = 5f * d
                    val len = 20f * d
                    val tip = 8f * d
                    if (left) {
                        drawRoundRect(picketColor, Offset(x - len, y - w2), Size(len, w2 * 2), CornerRadius(3f * d, 3f * d))
                        val p = Path().apply {
                            moveTo(x - len, y - w2)
                            lineTo(x - len, y + w2)
                            lineTo(x - len - tip, y)
                            close()
                        }
                        drawPath(p, picketColor)
                    } else {
                        drawRoundRect(picketColor, Offset(x, y - w2), Size(len, w2 * 2), CornerRadius(3f * d, 3f * d))
                        val p = Path().apply {
                            moveTo(x + len, y - w2)
                            lineTo(x + len, y + w2)
                            lineTo(x + len + tip, y)
                            close()
                        }
                        drawPath(p, picketColor)
                    }
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

                // --- Tổ gà: ảnh gà + gà còn sống xếp hàng dưới tổ ---
                val nx = NEST_X * w
                val ny = NEST_Y * h
                drawSprite(chickenImage, nx, ny, 118f * d)
                for (i in 0 until game.chickens) {
                    val totalW = (game.chickens - 1) * 30f * d
                    val cx = nx - totalW / 2f + i * 30f * d
                    drawSprite(chickenImage, cx, ny + 74f * d, 26f * d)
                }

                // --- Quái đã chết: mờ dần rồi biến mất ---
                for (dm in game.dying) {
                    val img = monsterImages[dm.type.id] ?: continue
                    val progress = ((now - dm.bornMs).toFloat() / dm.lifeMs).coerceIn(0f, 1f)
                    drawSprite(img, dm.x, dm.y, dm.radius * 2f, alpha = 1f - progress)
                }

                // --- Quái ---
                for (m in game.monsters) {
                    val img = monsterImages[m.type.id] ?: continue
                    val r = game.monsterRadiusPx(m.type)
                    val size = r * 2f
                    // Nhấp nháy trắng khi vừa bị đánh.
                    val flash = if (now < m.flashUntilMs) {
                        ColorFilter.tint(Color.White.copy(alpha = 0.6f), BlendMode.SrcAtop)
                    } else null
                    drawSprite(img, m.x, m.y, size, colorFilter = flash)
                    // Quái yếu (còn <= 1/4 máu): mắt hiện 2 chữ X đỏ.
                    if (m.hp <= m.maxHp / 4) {
                        val ex = 0.17f * size
                        val ey = -0.14f * size
                        val s = 0.07f * size
                        for (sx in listOf(m.x - ex, m.x + ex)) {
                            val cy = m.y + ey
                            drawLine(Color(0xFFD32F2F), Offset(sx - s, cy - s), Offset(sx + s, cy + s), 3.5f * d, StrokeCap.Round)
                            drawLine(Color(0xFFD32F2F), Offset(sx - s, cy + s), Offset(sx + s, cy - s), 3.5f * d, StrokeCap.Round)
                        }
                    }
                    // Thanh máu + chữ máu "3/3".
                    val barW = r * 2f
                    val barH = 5f * d
                    val barX = m.x - barW / 2f
                    val barY = m.y - r - 12f * d
                    val frac = (m.hp.toFloat() / m.maxHp).coerceIn(0f, 1f)
                    drawRect(Color(0xFF212121), topLeft = Offset(barX, barY), size = Size(barW, barH))
                    val hpColor = when {
                        frac > 0.5f -> Color(0xFF66BB6A)
                        frac > 0.25f -> Color(0xFFFFA726)
                        else -> Color(0xFFEF5350)
                    }
                    drawRect(hpColor, topLeft = Offset(barX, barY), size = Size(barW * frac, barH))
                    drawIntoCanvas { canvas ->
                        hpFillPaint.textSize = 11f * d
                        hpStrokePaint.textSize = 11f * d
                        val label = "${m.hp}/${m.maxHp}"
                        canvas.nativeCanvas.drawText(label, m.x, barY - 4f * d, hpStrokePaint)
                        canvas.nativeCanvas.drawText(label, m.x, barY - 4f * d, hpFillPaint)
                    }
                }

                // --- Hoạt ảnh vũ khí gõ xuống ---
                for (s in game.swings) {
                    val img = weaponImages[s.weapon.id] ?: continue
                    val progress = ((now - s.bornMs).toFloat() / s.lifeMs).coerceIn(0f, 1f)
                    val degrees = -50f + 95f * progress
                    val alpha = if (progress > 0.7f) (1f - (progress - 0.7f) / 0.3f) else 1f
                    withTransform({ rotate(degrees, pivot = Offset(s.x, s.y)) }) {
                        drawSprite(img, s.x, s.y - 10f * d, 52f * d, alpha = alpha)
                    }
                }

                // --- Đạn nước bay + vệt nước bắn tung ---
                for (p in game.projectiles) {
                    val progress = ((now - p.bornMs).toFloat() / p.lifeMs).coerceIn(0f, 1f)
                    val cx = p.startX + (p.targetX - p.startX) * progress
                    val cy = p.startY + (p.targetY - p.startY) * progress
                    drawLine(Color(0xFF4FC3F7), Offset(p.startX, p.startY), Offset(cx, cy), 4f * d, StrokeCap.Round)
                    drawCircle(Color(0xFF29B6F6), radius = 7f * d, center = Offset(cx, cy))
                    drawCircle(Color(0xFFB3E5FC), radius = 3f * d, center = Offset(cx - 2f * d, cy - 2f * d))
                }
                for (sp in game.splashes) {
                    val progress = ((now - sp.bornMs).toFloat() / sp.lifeMs).coerceIn(0f, 1f)
                    drawCircle(
                        Color(0xFF4FC3F7).copy(alpha = 1f - progress),
                        radius = 8f * d + progress * 26f * d,
                        center = Offset(sp.x, sp.y),
                        style = Stroke(width = 3.5f * d),
                    )
                }

                // --- Hiệu ứng chữ nổi (xu, mất gà) ---
                for (e in game.effects) {
                    val progress = ((now - e.bornMs).toFloat() / e.lifeMs).coerceIn(0f, 1f)
                    val alpha = (255 * (1f - progress)).toInt()
                    val ey = e.y - progress * 34f * d
                    drawIntoCanvas { canvas ->
                        emojiPaint.textSize = (if (e.big) 24f else 16f) * d
                        emojiPaint.alpha = alpha
                        emojiPaint.color = android.graphics.Color.rgb(
                            ((e.color shr 16) and 0xFF).toInt(),
                            ((e.color shr 8) and 0xFF).toInt(),
                            (e.color and 0xFF).toInt(),
                        )
                        canvas.nativeCanvas.drawText(e.text, e.x, ey, emojiPaint)
                    }
                }
            }

            // Banner đổi đợt.
            WaveBanner(game)
        }

        // ===== THANH VŨ KHÍ =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E7D32))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(weapon.imageRes),
                contentDescription = weapon.name,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(weapon.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Sát thương: ${weapon.damage}", color = Color(0xFFC8E6C9), fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("Đã giết: ${game.kills}", color = Color.White, fontSize = 14.sp)
        }
    }

    // ===== HỘP THOẠI TẠM DỪNG =====
    if (game.status == GameStatus.PAUSED) {
        AlertDialog(
            onDismissRequest = { game.resume() },
            title = { Text("⏸ Tạm dừng") },
            text = { Text("Level ${level.number} — Đợt ${game.waveIndex + 1}/${level.waves.size}\nGà còn lại: ${game.chickens}") },
            confirmButton = {
                Button(onClick = { game.resume() }) { Text("Tiếp tục") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { restartKey++ }) { Text("Chơi lại") }
                    TextButton(onClick = onExit) { Text("Về menu") }
                }
            },
        )
    }

    // ===== HỘP THOẠI THUA =====
    if (game.status == GameStatus.LOST) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("😢 Thua rồi!") },
            text = {
                Text(
                    "Gà đã bị ăn hết ở level ${level.number}.\n" +
                        "Giết ${game.kills} quái: +${game.coinsEarned} xu (vẫn giữ xu)"
                )
            },
            confirmButton = {
                Button(onClick = { restartKey++ }) { Text("Chơi lại") }
            },
            dismissButton = {
                TextButton(onClick = onExit) { Text("Về menu") }
            },
        )
    }
}

// Banner "Đợt X/Y" hiện 2 giây khi đổi đợt. Đọc game.frame để tự vẽ lại
// mỗi khung hình (nếu không banner sẽ không tự biến mất đúng giờ).
@Composable
fun androidx.compose.foundation.layout.BoxScope.WaveBanner(game: GameState) {
    @Suppress("UNUSED_EXPRESSION")
    game.frame
    if (System.currentTimeMillis() < game.bannerUntilMs && game.status == GameStatus.PLAYING) {
        Text(
            text = game.bannerText,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .background(Color(0xAA000000), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}
