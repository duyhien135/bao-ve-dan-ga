package com.example.baovedanga

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

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
    soundManager: SoundManager,
    onExit: () -> Unit,
    onWin: (Int) -> Unit, // thắng: chuyển sang cửa hàng, kèm tổng xu kiếm được ở màn này
) {
    val density = LocalDensity.current.density
    var restartKey by remember { mutableIntStateOf(0) }
    val game = remember(level.number, restartKey) {
        GameState(level, weapon, density, playSound = { soundManager.play(it) })
    }
    var rewardsApplied by remember(level.number, restartKey) { mutableStateOf(false) }

    // Chế độ đặt bẫy: game tạm dừng, chạm vào góc cua để đặt bẫy đã chọn.
    var trapMode by remember(level.number, restartKey) { mutableStateOf(false) }
    var selectedTrap by remember(level.number, restartKey) { mutableStateOf<TrapType?>(null) }
    var trapPulse by remember(level.number, restartKey) { mutableIntStateOf(0) }
    // Nhịp nhấp nháy cho điểm đặt bẫy khi đang đặt bẫy.
    LaunchedEffect(trapMode) {
        while (trapMode) {
            delay(120)
            trapPulse++
        }
    }

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
            if (!trapMode) game.update(dt) // đang đặt bẫy: game đứng yên
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
        // ===== HUD (thanh gỗ) =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0xFF8D6E63))))
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
            if (game.combo >= 5) {
                Spacer(modifier = Modifier.width(12.dp))
                Text("🔥 x${game.combo} · xu x${game.comboCoinMul()}", color = Color(0xFFFFAB40), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("\uD83E\uDE99 ${game.coinsEarned}", color = Color(0xFFFFD54F), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = { soundManager.muted = !soundManager.muted }) {
                Text(if (soundManager.muted) "🔇" else "🔊", fontSize = 20.sp)
            }
            TextButton(onClick = { soundManager.play("click"); game.pause() }) {
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
                    // Đang đặt bẫy thì chạm để đặt bẫy; chạm nút 🔧 thì sửa rào.
                    .pointerInput(game, trapMode, selectedTrap) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    for (change in event.changes) {
                                        if (trapMode) {
                                            val t = selectedTrap
                                            if (t != null) game.tryPlaceTrapAt(change.position.x, change.position.y, t)
                                        } else {
                                            if (!game.tryRepairAt(change.position.x, change.position.y)) {
                                                game.tap(change.position.x, change.position.y)
                                            }
                                        }
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

                // --- Bẫy đã đặt ---
                for (t in game.traps) {
                    when (t.type.id) {
                        "spikes" -> {
                            drawCircle(Color(0xFF4E342E), radius = 20f * d, center = Offset(t.x, t.y))
                            for (k in 0 until 5) {
                                val a = k * 1.2566f - 1.5708f
                                val sx = t.x + cos(a) * 12f * d
                                val sy = t.y + sin(a) * 12f * d
                                val spike = Path().apply {
                                    moveTo(sx - 5f * d, sy + 6f * d)
                                    lineTo(sx, sy - 8f * d)
                                    lineTo(sx + 5f * d, sy + 6f * d)
                                    close()
                                }
                                drawPath(spike, Color(0xFFB0BEC5))
                            }
                        }
                        "poison" -> {
                            drawOval(Color(0xFF7CB342).copy(alpha = 0.7f), topLeft = Offset(t.x - 22f * d, t.y - 14f * d), size = Size(44f * d, 28f * d))
                            drawOval(Color(0xFF33691E).copy(alpha = 0.8f), topLeft = Offset(t.x - 14f * d, t.y - 9f * d), size = Size(28f * d, 18f * d))
                            drawCircle(Color(0xFFC5E1A5), radius = 3f * d, center = Offset(t.x - 8f * d, t.y - 4f * d))
                            drawCircle(Color(0xFFC5E1A5), radius = 2f * d, center = Offset(t.x + 7f * d, t.y + 3f * d))
                        }
                        "pit" -> {
                            val armed = now >= t.cooldownUntilMs
                            drawOval(Color(if (armed) 0xFF3E2723 else 0xFF6D4C41), topLeft = Offset(t.x - 22f * d, t.y - 14f * d), size = Size(44f * d, 28f * d))
                            drawOval(Color(0xFF8D6E63), topLeft = Offset(t.x - 22f * d, t.y - 14f * d), size = Size(44f * d, 28f * d), style = Stroke(width = 2.5f * d))
                            if (!armed) {
                                drawCircle(Color(0xFF8D6E63), radius = 10f * d, center = Offset(t.x, t.y))
                            }
                        }
                    }
                }

                // --- Điểm đặt bẫy (chỉ hiện khi đang đặt bẫy) ---
                if (trapMode) {
                    @Suppress("UNUSED_EXPRESSION")
                    trapPulse // đọc để vẽ lại nhịp nhấp nháy
                    val pulseA = 0.5f + 0.3f * sin(trapPulse / 2f)
                    for (s in game.trapSpots) {
                        val occupied = game.traps.any { t ->
                            val dx = t.x - s.first
                            val dy = t.y - s.second
                            dx * dx + dy * dy < (40f * d) * (40f * d)
                        }
                        if (occupied) continue
                        drawCircle(Color(0xFFFFEB3B).copy(alpha = pulseA * 0.3f), radius = 30f * d, center = Offset(s.first, s.second))
                        drawCircle(Color(0xFFFFEB3B).copy(alpha = pulseA), radius = 30f * d, center = Offset(s.first, s.second), style = Stroke(width = 3f * d))
                    }
                }

                // --- Cổng rào (có máu, quái cắn rào ở đây) ---
                for (g in game.gates) {
                    val verticalGap = g.pathIndex <= 1 // cổng ở cạnh trái/phải: khe rào chạy dọc
                    val shake = if (now - g.lastHitMs < 150) sin(now / 18f) * 2f * d else 0f
                    val gx = g.x + shake
                    val gy = g.y
                    if (g.broken) {
                        // Rào vỡ: ván gãy nghiêng + mảnh vụn.
                        withTransform({ rotate(-28f, pivot = Offset(gx, gy)) }) {
                            drawRoundRect(Color(0xFF6D4C41), topLeft = Offset(gx - 22f * d, gy - 6f * d), size = Size(44f * d, 12f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * d, 3f * d))
                        }
                        withTransform({ rotate(24f, pivot = Offset(gx, gy)) }) {
                            drawRoundRect(Color(0xFF795548), topLeft = Offset(gx - 18f * d, gy - 5f * d), size = Size(36f * d, 10f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * d, 3f * d))
                        }
                        drawCircle(Color(0xFF5D4037), radius = 4f * d, center = Offset(gx - 14f * d, gy + 12f * d))
                        drawCircle(Color(0xFF5D4037), radius = 3f * d, center = Offset(gx + 16f * d, gy - 10f * d))
                    } else {
                        // 2 cột + 3 ván + chéo.
                        val postC = Color(0xFF4E342E)
                        val plankC = Color(0xFF8D6E63)
                        val plankDark = Color(0xFF6D4C41)
                        if (verticalGap) {
                            drawRoundRect(postC, topLeft = Offset(gx - 7f * d, gy - 40f * d), size = Size(14f * d, 12f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * d, 2f * d))
                            drawRoundRect(postC, topLeft = Offset(gx - 7f * d, gy + 28f * d), size = Size(14f * d, 12f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * d, 2f * d))
                            for (k in -1..1) {
                                val py = gy + k * 20f * d
                                drawRoundRect(if (k == 0) plankDark else plankC, topLeft = Offset(gx - 13f * d, py - 6f * d), size = Size(26f * d, 12f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * d, 2f * d))
                            }
                            drawLine(plankDark, Offset(gx - 10f * d, gy - 28f * d), Offset(gx + 10f * d, gy + 28f * d), 4f * d, StrokeCap.Round)
                        } else {
                            drawRoundRect(postC, topLeft = Offset(gx - 40f * d, gy - 7f * d), size = Size(12f * d, 14f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * d, 2f * d))
                            drawRoundRect(postC, topLeft = Offset(gx + 28f * d, gy - 7f * d), size = Size(12f * d, 14f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * d, 2f * d))
                            for (k in -1..1) {
                                val px = gx + k * 20f * d
                                drawRoundRect(if (k == 0) plankDark else plankC, topLeft = Offset(px - 6f * d, gy - 13f * d), size = Size(12f * d, 26f * d), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * d, 2f * d))
                            }
                            drawLine(plankDark, Offset(gx - 28f * d, gy - 10f * d), Offset(gx + 28f * d, gy + 10f * d), 4f * d, StrokeCap.Round)
                        }
                        // Thanh máu rào.
                        val barW = 64f * d
                        val barH = 6f * d
                        val barX = gx - barW / 2f
                        val barY = gy - 48f * d
                        val frac = (g.hp.toFloat() / g.maxHp).coerceIn(0f, 1f)
                        drawRect(Color(0xFF212121), topLeft = Offset(barX, barY), size = Size(barW, barH))
                        val hpColor = when {
                            frac > 0.5f -> Color(0xFF66BB6A)
                            frac > 0.25f -> Color(0xFFFFA726)
                            else -> Color(0xFFEF5350)
                        }
                        drawRect(hpColor, topLeft = Offset(barX, barY), size = Size(barW * frac, barH))
                        drawIntoCanvas { canvas ->
                            hpFillPaint.textSize = 10f * d
                            hpStrokePaint.textSize = 10f * d
                            val label = "🛡 ${g.hp}/${g.maxHp}"
                            canvas.nativeCanvas.drawText(label, gx, barY - 3f * d, hpStrokePaint)
                            canvas.nativeCanvas.drawText(label, gx, barY - 3f * d, hpFillPaint)
                        }
                        // Nút sửa rào 🔧 khi bị hư (chạm để sửa, tốn xu).
                        if (g.hp < g.maxHp) {
                            val (bx, by) = game.repairButtonPos(g)
                            drawCircle(Color(0xFFFFD54F), radius = 20f * d, center = Offset(bx, by))
                            drawCircle(Color(0xFFBF360C), radius = 20f * d, center = Offset(bx, by), style = Stroke(width = 2f * d))
                            drawIntoCanvas { canvas ->
                                emojiPaint.textSize = 20f * d
                                emojiPaint.alpha = 255
                                emojiPaint.color = android.graphics.Color.BLACK
                                canvas.nativeCanvas.drawText("🔧", bx, by + 7f * d, emojiPaint)
                                hpFillPaint.textSize = 10f * d
                                hpStrokePaint.textSize = 10f * d
                                val price = "${repairCost(level.number)} xu"
                                canvas.nativeCanvas.drawText(price, bx, by + 32f * d, hpStrokePaint)
                                canvas.nativeCanvas.drawText(price, bx, by + 32f * d, hpFillPaint)
                            }
                        }
                    }
                    if (g.broken) {
                        // Cổng vỡ vẫn hiện nút sửa.
                        val (bx, by) = game.repairButtonPos(g)
                        drawCircle(Color(0xFFFFD54F), radius = 20f * d, center = Offset(bx, by))
                        drawCircle(Color(0xFFBF360C), radius = 20f * d, center = Offset(bx, by), style = Stroke(width = 2f * d))
                        drawIntoCanvas { canvas ->
                            emojiPaint.textSize = 20f * d
                            emojiPaint.alpha = 255
                            emojiPaint.color = android.graphics.Color.BLACK
                            canvas.nativeCanvas.drawText("🔧", bx, by + 7f * d, emojiPaint)
                            hpFillPaint.textSize = 10f * d
                            hpStrokePaint.textSize = 10f * d
                            val price = "${repairCost(level.number)} xu"
                            canvas.nativeCanvas.drawText(price, bx, by + 32f * d, hpStrokePaint)
                            canvas.nativeCanvas.drawText(price, bx, by + 32f * d, hpFillPaint)
                        }
                    }
                }

                // --- Thanh máu boss (nếu đang có boss) ---
                val boss = game.bossMonster()
                if (boss != null) {
                    val barW = w * 0.5f
                    val barH = 12f * d
                    val barX = (w - barW) / 2f
                    val barY = 10f * d
                    drawRoundRect(Color(0xAA000000), topLeft = Offset(barX, barY), size = Size(barW, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * d, 6f * d))
                    val frac = (boss.hp.toFloat() / boss.maxHp).coerceIn(0f, 1f)
                    drawRoundRect(Color(0xFFD32F2F), topLeft = Offset(barX, barY), size = Size(barW * frac, barH), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * d, 6f * d))
                    drawIntoCanvas { canvas ->
                        hpFillPaint.textSize = 14f * d
                        hpStrokePaint.textSize = 14f * d
                        val label = "👑 BOSS ${boss.type.name} ${boss.hp}/${boss.maxHp}"
                        canvas.nativeCanvas.drawText(label, w / 2f, barY - 6f * d, hpStrokePaint)
                        canvas.nativeCanvas.drawText(label, w / 2f, barY - 6f * d, hpFillPaint)
                    }
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
                    val r = game.monsterRadiusPx(m)
                    val size = r * 2f
                    // Rắn chui dưới đất: chỉ vẽ ụ đất + bụi, không vẽ quái.
                    if (m.state == "burrow") {
                        drawOval(Color(0xFF6D4C41), topLeft = Offset(m.x - r, m.y - r * 0.6f), size = Size(r * 2f, r * 1.2f))
                        drawOval(Color(0xFF8D6E63), topLeft = Offset(m.x - r * 0.7f, m.y - r * 0.45f), size = Size(r * 1.4f, r * 0.8f))
                        drawCircle(Color(0xFFA1887F).copy(alpha = 0.7f), radius = 4f * d, center = Offset(m.x - r * 0.5f, m.y - r * 0.5f))
                        drawCircle(Color(0xFFA1887F).copy(alpha = 0.5f), radius = 3f * d, center = Offset(m.x + r * 0.4f, m.y - r * 0.6f))
                        continue
                    }
                    // Vị trí vẽ: cắn rào thì lao tới lao lui, leo rào thì nhô lên dần.
                    var mx = m.x
                    var my = m.y
                    if (m.state == "bite") {
                        val dx = nx - m.x
                        val dy = ny - m.y
                        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                        val lunge = sin(now / 150f + m.id) * 3f * d
                        mx += dx / len * lunge
                        my += dy / len * lunge
                    }
                    if (m.state == "climb") {
                        val progress = (1f - (m.climbUntilMs - now).toFloat() / 1500f).coerceIn(0f, 1f)
                        my -= progress * 24f * d
                    }
                    // Hào quang đỏ đậm nhấp nháy dưới mọi con quái (ghê rợn).
                    val pulse = 0.22f + 0.08f * sin(now / 280f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFB71C1C).copy(alpha = pulse), Color.Transparent),
                            center = Offset(mx, my),
                            radius = r + 12f * d,
                        ),
                        radius = r + 12f * d,
                        center = Offset(mx, my),
                    )
                    // Hào quang boss / lửa cháy / độc (vẽ dưới quái).
                    if (m.isBoss) {
                        drawCircle(Color(0xFFD32F2F).copy(alpha = 0.25f), radius = r + 8f * d, center = Offset(mx, my))
                    }
                    if (now < m.burnUntilMs) {
                        drawCircle(Color(0xFFFF6D00).copy(alpha = 0.3f), radius = r + 2f * d, center = Offset(mx, my))
                    }
                    if (now < m.poisonUntilMs) {
                        drawCircle(Color(0xFF7CB342).copy(alpha = 0.3f), radius = r + 2f * d, center = Offset(mx, my))
                    }
                    // Nhấp nháy trắng khi vừa bị đánh.
                    val flash = if (now < m.flashUntilMs) {
                        ColorFilter.tint(Color.White.copy(alpha = 0.6f), BlendMode.SrcAtop)
                    } else null
                    // Cáo giả chết: nằm nghiêng 90 độ, mờ hơn.
                    if (m.state == "dead") {
                        withTransform({ rotate(90f, pivot = Offset(mx, my)) }) {
                            drawSprite(img, mx, my, size, alpha = 0.85f, colorFilter = flash)
                        }
                    } else {
                        drawSprite(img, mx, my, size, colorFilter = flash)
                    }
                    // Tối viền quanh quái + mắt đỏ phát sáng (quái yếu thì
                    // hiện 2 chữ X đỏ thay, quái đóng băng thì không vẽ mắt).
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
                            center = Offset(mx, my),
                            radius = r,
                        ),
                        radius = r,
                        center = Offset(mx, my),
                    )
                    if (m.hp > m.maxHp / 4 && now >= m.frozenUntilMs && m.state != "dead") {
                        val ex = 0.17f * size
                        val ey = -0.14f * size
                        for (sx in listOf(mx - ex, mx + ex)) {
                            val cy = my + ey
                            drawCircle(Color(0xFFFF1744).copy(alpha = 0.5f), radius = 0.075f * size, center = Offset(sx, cy))
                            drawCircle(Color(0xFFFF5252), radius = 0.038f * size, center = Offset(sx, cy))
                            drawCircle(Color.White, radius = 0.015f * size, center = Offset(sx, cy))
                        }
                    }
                    // Điểm yếu: chấm vàng nhấp nháy, chạm đúng thì chí mạng.
                    if (m.state == "walk" || m.state == "bite" || m.state == "climb") {
                        val wPulse = 0.6f + 0.4f * sin(now / 120f)
                        val wx = mx + m.weakOX
                        val wy = my + m.weakOY
                        drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.4f * wPulse), radius = 10f * d, center = Offset(wx, wy))
                        drawCircle(Color(0xFFFFD54F).copy(alpha = wPulse), radius = 5.5f * d, center = Offset(wx, wy))
                        drawCircle(Color.White.copy(alpha = wPulse), radius = 2.5f * d, center = Offset(wx, wy))
                    }
                    // Đóng băng: phủ lớp băng trắng xanh.
                    if (now < m.frozenUntilMs) {
                        drawCircle(Color(0xFFB3E5FC).copy(alpha = 0.45f), radius = r, center = Offset(mx, my))
                    }
                    // Sập hố: mờ đi (chìm một nửa trong hố).
                    if (now < m.stuckUntilMs) {
                        drawCircle(Color(0xFF3E2723).copy(alpha = 0.35f), radius = r, center = Offset(mx, my))
                    }
                    // Khiên đang mở: bong bóng xanh.
                    if (game.isShieldUp(m, now)) {
                        drawCircle(Color(0xFF4FC3F7).copy(alpha = 0.18f), radius = r + 6f * d, center = Offset(mx, my))
                        drawCircle(Color(0xFF4FC3F7).copy(alpha = 0.7f), radius = r + 6f * d, center = Offset(mx, my), style = Stroke(width = 3f * d))
                    }
                    // Bị làm chậm: vòng xanh.
                    if (now < m.slowUntilMs) {
                        drawCircle(Color(0xFF29B6F6).copy(alpha = 0.8f), radius = r + 4f * d, center = Offset(mx, my), style = Stroke(width = 3f * d))
                    }
                    // Đang lao nhanh: vệt tốc độ phía sau (hướng ngược về phía tổ).
                    if (now < m.dashUntilMs) {
                        val dx = nx - mx
                        val dy = ny - my
                        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                        val ux = dx / len
                        val uy = dy / len
                        val px = -uy
                        val py = ux
                        for (k in -1..1) {
                            val ox = px * k * 10f * d
                            val oy = py * k * 10f * d
                            drawLine(
                                Color.White.copy(alpha = 0.55f),
                                Offset(mx - ux * (r + 4f * d) + ox, my - uy * (r + 4f * d) + oy),
                                Offset(mx - ux * (r + 20f * d) + ox, my - uy * (r + 20f * d) + oy),
                                3f * d, StrokeCap.Round,
                            )
                        }
                    }
                    // Biểu tượng trạng thái phía trên quái.
                    val statusIcon = when {
                        m.state == "dead" -> "💤"
                        m.isBoss -> "👑"
                        m.isMiniBoss -> "😤"
                        m.state == "bite" -> "💢"
                        now < m.frozenUntilMs -> "❄️"
                        now < m.burnUntilMs -> "🔥"
                        now < m.stuckUntilMs -> "💫"
                        now < m.slowUntilMs -> "❄️"
                        else -> null
                    }
                    if (statusIcon != null) {
                        drawIntoCanvas { canvas ->
                            emojiPaint.textSize = 18f * d
                            emojiPaint.alpha = 255
                            emojiPaint.color = android.graphics.Color.WHITE
                            canvas.nativeCanvas.drawText(statusIcon, mx, my - r - 24f * d, emojiPaint)
                        }
                    }
                    // Quái yếu (còn <= 1/4 máu): mắt hiện 2 chữ X đỏ.
                    if (m.hp <= m.maxHp / 4 && m.state != "dead") {
                        val ex = 0.17f * size
                        val ey = -0.14f * size
                        val s = 0.07f * size
                        for (sx in listOf(mx - ex, mx + ex)) {
                            val cy = my + ey
                            drawLine(Color(0xFFD32F2F), Offset(sx - s, cy - s), Offset(sx + s, cy + s), 3.5f * d, StrokeCap.Round)
                            drawLine(Color(0xFFD32F2F), Offset(sx - s, cy + s), Offset(sx + s, cy - s), 3.5f * d, StrokeCap.Round)
                        }
                    }
                    // Thanh máu + chữ máu "3/3".
                    val barW = if (m.isBoss || m.isMiniBoss) r * 2.4f else if (m.isBig) r * 2.2f else r * 2f
                    val barH = 5f * d
                    val barX = mx - barW / 2f
                    val barY = my - r - 12f * d
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
                        canvas.nativeCanvas.drawText(label, mx, barY - 4f * d, hpStrokePaint)
                        canvas.nativeCanvas.drawText(label, mx, barY - 4f * d, hpFillPaint)
                    }
                }

                // --- Xu rơi ra đất (chạm để nhặt trong 4 giây, không nhặt thì mất) ---
                for (c in game.coinDrops) {
                    val remain = c.lifeMs - (now - c.bornMs)
                    val alpha = if (remain < 1000) (if ((now / 150) % 2 == 0L) 1f else 0.35f) else 1f
                    val scaleIn = ((now - c.bornMs).toFloat() / 150f).coerceIn(0f, 1f)
                    val r = (12f * d) * (0.5f + 0.5f * scaleIn)
                    drawCircle(Color(0xFFBF8F00).copy(alpha = alpha), radius = r, center = Offset(c.x, c.y))
                    drawCircle(Color(0xFFFFD54F).copy(alpha = alpha), radius = r * 0.8f, center = Offset(c.x, c.y))
                    drawCircle(Color(0xFFFFECB3).copy(alpha = alpha), radius = r * 0.35f, center = Offset(c.x - r * 0.2f, c.y - r * 0.2f))
                    drawIntoCanvas { canvas ->
                        hpFillPaint.textSize = 11f * d
                        hpStrokePaint.textSize = 11f * d
                        canvas.nativeCanvas.drawText("${c.value}", c.x, c.y + r + 12f * d, hpStrokePaint)
                        canvas.nativeCanvas.drawText("${c.value}", c.x, c.y + r + 12f * d, hpFillPaint)
                    }
                }

                // --- Vụ nổ bom ---
                for (e in game.explosions) {
                    val progress = ((now - e.bornMs).toFloat() / e.lifeMs).coerceIn(0f, 1f)
                    drawCircle(Color(0xFFFF6D00).copy(alpha = (1f - progress) * 0.5f), radius = progress * e.radius, center = Offset(e.x, e.y))
                    drawCircle(Color(0xFFFFD54F).copy(alpha = 1f - progress), radius = progress * e.radius, center = Offset(e.x, e.y), style = Stroke(width = 6f * d))
                    drawCircle(Color.White.copy(alpha = (1f - progress) * 0.8f), radius = (1f - progress) * 20f * d, center = Offset(e.x, e.y))
                }

                // --- Tia sét Thor ---
                for (l in game.lightnings) {
                    val progress = ((now - l.bornMs).toFloat() / l.lifeMs).coerceIn(0f, 1f)
                    val alpha = 1f - progress
                    for (j in 1 until l.points.size) {
                        val (x0, y0) = l.points[j - 1]
                        val (x1, y1) = l.points[j]
                        drawLine(Color(0xFFFFEB3B).copy(alpha = alpha), Offset(x0, y0), Offset(x1, y1), 4.5f * d, StrokeCap.Round)
                        drawLine(Color.White.copy(alpha = alpha), Offset(x0, y0), Offset(x1, y1), 2f * d, StrokeCap.Round)
                    }
                }

                // --- Mũi tên cung xuyên ---
                for (a in game.arrows) {
                    val progress = ((now - a.bornMs).toFloat() / a.lifeMs).coerceIn(0f, 1f)
                    val alpha = 1f - progress
                    drawLine(Color(0xFF8D6E63).copy(alpha = alpha), Offset(a.x0, a.y0), Offset(a.x1, a.y1), 3.5f * d, StrokeCap.Round)
                    val dx = a.x1 - a.x0
                    val dy = a.y1 - a.y0
                    val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val ux = dx / len
                    val uy = dy / len
                    val head = Path().apply {
                        moveTo(a.x1, a.y1)
                        lineTo(a.x1 - ux * 12f * d - uy * 6f * d, a.y1 - uy * 12f * d + ux * 6f * d)
                        lineTo(a.x1 - ux * 12f * d + uy * 6f * d, a.y1 - uy * 12f * d - ux * 6f * d)
                        close()
                    }
                    drawPath(head, Color(0xFFBCAAA4).copy(alpha = alpha))
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

            // Chọn buff giữa các đợt.
            val choices = game.buffChoices
            if (choices != null && game.status == GameStatus.PLAYING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xAA000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎁 Chọn 1 buff!", color = Color(0xFFFFD54F), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Đợt ${game.waveIndex + 1} xong — chọn buff rồi đánh tiếp",
                            color = Color.White,
                            fontSize = 14.sp,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            for (buff in choices) {
                                Card(
                                    onClick = { game.pickBuff(buff) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                                    border = BorderStroke(2.dp, Color(0xFFFFD54F)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    modifier = Modifier.width(190.dp),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(buff.emoji, fontSize = 36.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(buff.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(buff.desc, color = Color(0xFFC8E6C9), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bảng chọn bẫy khi đang đặt bẫy.
            if (trapMode && game.status == GameStatus.PLAYING) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .background(Color(0xDD1B5E20), shape = RoundedCornerShape(16.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (selectedTrap == null) "Chọn 1 loại bẫy, rồi chạm vào vòng vàng ở góc cua"
                        else "Chạm vào vòng vàng để đặt ${selectedTrap!!.name} (${selectedTrap!!.price} xu)",
                        color = Color.White,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (t in TRAP_TYPES) {
                            val sel = selectedTrap?.id == t.id
                            Card(
                                onClick = { soundManager.play("click"); selectedTrap = if (sel) null else t },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = if (sel) Color(0xFFFF8C42) else Color(0xFF2E7D32)),
                                border = if (sel) BorderStroke(2.dp, Color(0xFFFFD54F)) else null,
                                modifier = Modifier.width(150.dp),
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text("${t.emoji} ${t.name}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Text("${t.price} xu", color = Color(0xFFFFD54F), fontSize = 13.sp)
                                    Text(t.desc, color = Color(0xFFC8E6C9), fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        Button(
                            onClick = { soundManager.play("click"); trapMode = false; selectedTrap = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                            shape = RoundedCornerShape(12.dp),
                        ) { Text("▶ Đánh tiếp", fontSize = 14.sp) }
                    }
                }
            }
        }

        // Viền vàng trên thanh vũ khí.
        Box(modifier = Modifier.fillMaxWidth().height(3.dp).background(Color(0xFFFFD54F)))
        // ===== THANH VŨ KHÍ =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0xFF8D6E63))))
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
                Text(
                    "Sát thương: ${weapon.damage + game.damageBonus}" + if (weapon.effect.isNotEmpty()) " • ${weapon.effect}" else "",
                    color = Color(0xFFFFE0B2),
                    fontSize = 13.sp,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("Đã giết: ${game.kills}", color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { soundManager.play("click"); trapMode = !trapMode; selectedTrap = null },
                colors = ButtonDefaults.buttonColors(containerColor = if (trapMode) Color(0xFFFF8C42) else Color(0xFF43A047)),
                shape = RoundedCornerShape(12.dp),
            ) { Text(if (trapMode) "▶ Đánh tiếp" else "🪤 Bẫy", fontSize = 14.sp) }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { game.useChickenSkill() },
                enabled = !game.skillUsed,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F), disabledContainerColor = Color(0xFF616161)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    if (game.skillUsed) "🐔 Đã dùng" else "🐔 Kêu! (1 lần)",
                    fontSize = 14.sp,
                    color = if (game.skillUsed) Color.White else Color.Black,
                )
            }
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
            text = { Text("Level ${level.number} — Đợt ${game.waveIndex + 1}/${level.waves.size}\nGà còn lại: ${game.chickens}\nCombo cao nhất: x${game.comboBest}") },
            confirmButton = {
                Button(
                    onClick = { soundManager.play("click"); game.resume() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                ) { Text("Tiếp tục") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { soundManager.play("click"); restartKey++ }) { Text("Chơi lại", color = Color.White) }
                    TextButton(onClick = { soundManager.play("click"); onExit() }) { Text("Về menu", color = Color.White) }
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
                        "Giết ${game.kills} quái: +${game.coinsEarned} xu (vẫn giữ xu)\n" +
                        "Combo cao nhất: x${game.comboBest}"
                )
            },
            confirmButton = {
                Button(
                    onClick = { soundManager.play("click"); restartKey++ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                ) { Text("Chơi lại") }
            },
            dismissButton = {
                TextButton(onClick = { soundManager.play("click"); onExit() }) { Text("Về menu", color = Color.White) }
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
