package com.example.baovedanga

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.onSizeChanged
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GameScreen(
    level: LevelDef,
    weapon: Weapon,
    save: SaveManager,
    onExit: () -> Unit,
    onNextLevel: () -> Unit,
) {
    val density = LocalDensity.current.density
    var restartKey by remember { mutableIntStateOf(0) }
    val game = remember(level.number, restartKey) { GameState(level, weapon, density) }
    var rewardsApplied by remember(level.number, restartKey) { mutableStateOf(false) }

    // Nhận xu + mở level mới khi thắng/thua (chỉ 1 lần mỗi ván).
    LaunchedEffect(game.status) {
        if (rewardsApplied) return@LaunchedEffect
        when (game.status) {
            GameStatus.WON -> {
                save.addCoins(game.coinsEarned + winBonus(level.number))
                save.unlockLevel(level.number + 1)
                rewardsApplied = true
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                    .pointerInput(game) {
                        detectTapGestures { offset -> game.tap(offset.x, offset.y) }
                    }
            ) {
                @Suppress("UNUSED_EXPRESSION")
                game.frame // đọc để vẽ lại mỗi khung hình
                val w = size.width
                val h = size.height
                val d = density
                val now = System.currentTimeMillis()

                // --- Rừng: nền xanh đậm + cây ---
                drawRect(Color(0xFF2E7D32))
                val treeRandom = Random(42)
                repeat(30) {
                    val tx = treeRandom.nextFloat() * w
                    val ty = treeRandom.nextFloat() * h
                    // Không trồng cây trong hàng rào.
                    if (tx > FENCE_LEFT * w - 20 * d && tx < FENCE_RIGHT * w + 20 * d &&
                        ty > FENCE_TOP * h - 20 * d && ty < FENCE_BOTTOM * h + 20 * d
                    ) return@repeat
                    val tr = (13f + treeRandom.nextFloat() * 9f) * d
                    drawCircle(Color(0xFF1B5E20), radius = tr, center = Offset(tx, ty))
                    drawCircle(Color(0xFF388E3C), radius = tr * 0.6f, center = Offset(tx - tr * 0.2f, ty - tr * 0.2f))
                }

                // --- Vườn rau trong hàng rào ---
                drawRoundRect(
                    color = Color(0xFFA5D6A7),
                    topLeft = Offset(FENCE_LEFT * w, FENCE_TOP * h),
                    size = Size((FENCE_RIGHT - FENCE_LEFT) * w, (FENCE_BOTTOM - FENCE_TOP) * h),
                    cornerRadius = CornerRadius(24f * d, 24f * d),
                )
                // Luống rau: chấm xanh xếp thành hàng (đường đất vẽ đè lên sau).
                val rowRandom = Random(7)
                var ry = FENCE_TOP * h + 22f * d
                while (ry < FENCE_BOTTOM * h - 10f * d) {
                    var rx = FENCE_LEFT * w + 18f * d
                    while (rx < FENCE_RIGHT * w - 10f * d) {
                        // Chừa chỗ cho tổ gà ở giữa.
                        val dx = rx - NEST_X * w
                        val dy = ry - NEST_Y * h
                        if (dx * dx + dy * dy > (70f * d) * (70f * d)) {
                            drawCircle(Color(0xFF66BB6A), radius = 6f * d, center = Offset(rx, ry))
                            drawCircle(Color(0xFF2E7D32), radius = 3f * d, center = Offset(rx, ry - 3f * d))
                        }
                        rx += 26f * d + rowRandom.nextFloat() * 6f * d
                    }
                    ry += 26f * d
                }

                // --- Đường đất (chỉ các đường đang dùng ở level này) ---
                for (i in 0 until level.pathCount) {
                    val path = game.paths.getOrNull(i) ?: continue
                    for (j in 1 until path.pts.size) {
                        drawLine(
                            color = Color(0xFFD7CCC8),
                            start = Offset(path.pts[j - 1].first, path.pts[j - 1].second),
                            end = Offset(path.pts[j].first, path.pts[j].second),
                            strokeWidth = 44f * d,
                            cap = Stroke.Cap.Round,
                        )
                    }
                    // Viền đường đậm hơn một chút ở giữa.
                    for (j in 1 until path.pts.size) {
                        drawLine(
                            color = Color(0xFFBCAAA4),
                            start = Offset(path.pts[j - 1].first, path.pts[j - 1].second),
                            end = Offset(path.pts[j].first, path.pts[j].second),
                            strokeWidth = 30f * d,
                            cap = Stroke.Cap.Round,
                        )
                    }
                }

                // --- Hàng rào màu cam, có khe hở ở chỗ đường đi qua ---
                val fenceColor = Color(0xFFFF8C42)
                val fw = 8f * d
                val fx0 = FENCE_LEFT * w
                val fx1 = FENCE_RIGHT * w
                val fy0 = FENCE_TOP * h
                val fy1 = FENCE_BOTTOM * h
                val gapX0 = 0.44f * w
                val gapX1 = 0.56f * w
                val gapY0 = 0.38f * h
                val gapY1 = 0.50f * h
                fun hLine(y: Float, x0: Float, x1: Float) {
                    if (x1 > x0) drawLine(fenceColor, Offset(x0, y), Offset(x1, y), fw, Stroke.Cap.Round)
                }
                fun vLine(x: Float, y0: Float, y1: Float) {
                    if (y1 > y0) drawLine(fenceColor, Offset(x, y0), Offset(x, y1), fw, Stroke.Cap.Round)
                }
                // Cạnh trên (đường 0 đi qua).
                if (level.pathCount >= 1) { hLine(fy0, fx0, gapX0); hLine(fy0, gapX1, fx1) } else hLine(fy0, fx0, fx1)
                // Cạnh dưới (đường 3 đi qua).
                if (level.pathCount >= 4) { hLine(fy1, fx0, gapX0); hLine(fy1, gapX1, fx1) } else hLine(fy1, fx0, fx1)
                // Cạnh trái (đường 1 đi qua).
                if (level.pathCount >= 2) { vLine(fx0, fy0, gapY0); vLine(fx0, gapY1, fy1) } else vLine(fx0, fy0, fy1)
                // Cạnh phải (đường 2 đi qua).
                if (level.pathCount >= 3) { vLine(fx1, fy0, gapY0); vLine(fx1, gapY1, fy1) } else vLine(fx1, fy0, fy1)
                // Cọc rào ở 4 góc.
                for ((cx, cy) in listOf(fx0 to fy0, fx1 to fy0, fx0 to fy1, fx1 to fy1)) {
                    drawCircle(Color(0xFFEF6C00), radius = 9f * d, center = Offset(cx, cy))
                }

                // --- Tổ gà ---
                val nx = NEST_X * w
                val ny = NEST_Y * h
                drawCircle(Color(0xFF8D6E63), radius = 54f * d, center = Offset(nx, ny))
                drawCircle(Color(0xFFFFE082), radius = 44f * d, center = Offset(nx, ny))
                // Rơm rạ.
                val hayRandom = Random(3)
                repeat(8) {
                    val a = hayRandom.nextFloat() * 360f
                    val r = 30f * d + hayRandom.nextFloat() * 12f * d
                    drawLine(
                        Color(0xFFFFCA28),
                        Offset(nx + cos(Math.toRadians(a.toDouble())).toFloat() * r, ny + sin(Math.toRadians(a.toDouble())).toFloat() * r),
                        Offset(nx + cos(Math.toRadians(a.toDouble())).toFloat() * (r + 10f * d), ny + sin(Math.toRadians(a.toDouble())).toFloat() * (r + 10f * d)),
                        strokeWidth = 3f * d,
                        cap = Stroke.Cap.Round,
                    )
                }
                // Gà còn sống xếp vòng quanh tổ.
                for (i in 0 until game.chickens) {
                    val a = Math.toRadians((i * 360.0 / game.chickens.coerceAtLeast(1)) - 90.0)
                    val cx = nx + cos(a).toFloat() * 24f * d
                    val cy = ny + sin(a).toFloat() * 24f * d
                    drawCircle(Color.White, radius = 11f * d, center = Offset(cx, cy))
                    drawCircle(Color(0xFFE53935), radius = 4f * d, center = Offset(cx, cy - 11f * d))
                    drawCircle(Color(0xFF212121), radius = 1.8f * d, center = Offset(cx + 3.5f * d, cy - 2f * d))
                    drawLine(Color(0xFFFF6D00), Offset(cx + 8f * d, cy + 1f * d), Offset(cx + 13f * d, cy + 3f * d), strokeWidth = 3f * d, cap = Stroke.Cap.Round)
                }

                // --- Quái ---
                for (m in game.monsters) {
                    val r = game.monsterRadiusPx(m.type)
                    drawCircle(Color(m.type.color), radius = r, center = Offset(m.x, m.y))
                    // Mặt quái bằng emoji.
                    drawIntoCanvas { canvas ->
                        emojiPaint.textSize = r * 1.15f
                        emojiPaint.alpha = 255
                        canvas.nativeCanvas.drawText(m.type.emoji, m.x, m.y + r * 0.4f, emojiPaint)
                    }
                    // Nhấp nháy trắng khi vừa bị đánh.
                    if (now < m.flashUntilMs) {
                        drawCircle(Color.White.copy(alpha = 0.55f), radius = r, center = Offset(m.x, m.y))
                    }
                    // Thanh máu.
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
                }

                // --- Hiệu ứng chữ nổi ---
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(weapon.emoji, fontSize = 28.sp)
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

    // ===== HỘP THOẠI THẮNG =====
    if (game.status == GameStatus.WON) {
        val total = game.coinsEarned + winBonus(level.number)
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🎉 Thắng!") },
            text = {
                Text(
                    "Bạn đã bảo vệ đàn gà ở level ${level.number}!\n" +
                        "Giết ${game.kills} quái: +${game.coinsEarned} xu\n" +
                        "Thưởng thắng level: +${winBonus(level.number)} xu\n" +
                        "Tổng: +$total xu"
                )
            },
            confirmButton = {
                Button(onClick = onNextLevel) { Text("Level tiếp") }
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
