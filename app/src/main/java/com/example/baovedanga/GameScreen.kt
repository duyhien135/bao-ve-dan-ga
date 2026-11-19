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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

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
    val nestImage = remember { BitmapFactory.decodeResource(context.resources, R.drawable.nest_empty).asImageBitmap() }
    val chickImage = remember { BitmapFactory.decodeResource(context.resources, R.drawable.chick).asImageBitmap() }
    val weaponImages = remember { WEAPONS.associate { it.id to BitmapFactory.decodeResource(context.resources, it.imageRes).asImageBitmap() } }

    // Cảnh tĩnh (cỏ, cây, rau, đường đá, hàng rào) vẽ 1 lần vào bitmap ở
    // luồng nền, rồi mỗi khung hình chỉ việc vẽ lại bitmap cho mượt.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var bgBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(canvasSize, game) {
        val w = canvasSize.width
        val h = canvasSize.height
        if (w > 0 && h > 0) {
            if (game.paths.isEmpty()) game.configure(w.toFloat(), h.toFloat())
            bgBitmap = withContext(Dispatchers.Default) {
                renderStaticScene(w, h, density, game.paths, level.pathCount)
            }
        }
    }

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
                .background(Color(0xFF1B5E20))
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
        // Viền vàng dưới thanh HUD.
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFFFD54F)))

        // ===== VÙNG CHƠI =====
        Box(modifier = Modifier.weight(1f)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        canvasSize = it
                        game.configure(it.width.toFloat(), it.height.toFloat())
                    }
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

                // --- Cảnh tĩnh (đã vẽ sẵn vào bitmap) ---
                val bg = bgBitmap
                if (bg != null) {
                    drawImage(
                        bg,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bg.width, bg.height),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(w.roundToInt(), h.roundToInt()),
                    )
                } else {
                    drawRect(Color(0xFF558B2F))
                }

                // --- Tổ gà: tổ rơm + 5 gà con ngồi trong tổ, mất con nào thì vắng con đó ---
                val nx = NEST_X * w
                val ny = NEST_Y * h
                drawSprite(nestImage, nx, ny, 132f * d)
                // Vị trí 5 gà con trong tổ (con giữa sống sót cuối cùng).
                val chickSpots = listOf(
                    0f to 0.06f,
                    0f to -0.52f,
                    -0.52f to -0.08f,
                    0.52f to -0.16f,
                    0f to 0.48f,
                )
                val nestR = 66f * d
                val aliveSpots = chickSpots.take(game.chickens)
                for (k in aliveSpots.indices.reversed()) {
                    val (ox, oy) = aliveSpots[k]
                    drawSprite(chickImage, nx + ox * nestR, ny + oy * nestR, 38f * d)
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

        // Viền vàng trên thanh vũ khí.
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFFFD54F)))
        // ===== THANH VŨ KHÍ =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1B5E20))
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
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1B5E20),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFC8E6C9),
            title = { Text("⏸ Tạm dừng") },
            text = { Text("Level ${level.number} — Đợt ${game.waveIndex + 1}/${level.waves.size}\nGà còn lại: ${game.chickens}") },
            confirmButton = {
                Button(
                    onClick = { game.resume() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                ) { Text("Tiếp tục") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { restartKey++ }) { Text("Chơi lại", color = Color.White) }
                    TextButton(onClick = onExit) { Text("Về menu", color = Color.White) }
                }
            },
        )
    }

    // ===== HỘP THOẠI THUA =====
    if (game.status == GameStatus.LOST) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1B5E20),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFC8E6C9),
            title = { Text("😢 Thua rồi!") },
            text = {
                Text(
                    "Gà đã bị ăn hết ở level ${level.number}.\n" +
                        "Giết ${game.kills} quái: +${game.coinsEarned} xu (vẫn giữ xu)"
                )
            },
            confirmButton = {
                Button(
                    onClick = { restartKey++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                ) { Text("Chơi lại") }
            },
            dismissButton = {
                TextButton(onClick = onExit) { Text("Về menu", color = Color.White) }
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
