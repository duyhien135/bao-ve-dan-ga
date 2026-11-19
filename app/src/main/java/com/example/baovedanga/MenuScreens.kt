package com.example.baovedanga

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

// Nền cảnh vườn (cỏ, cây, rau, đường đá, hàng rào) vẽ sẵn 1 lần, phủ lớp
// tối để chữ dễ đọc. Dùng chung cho menu + cửa hàng.
@Composable
fun FarmBackground(content: @Composable () -> Unit) {
    val density = LocalDensity.current.density
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var bg by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(boxSize) {
        val w = boxSize.width
        val h = boxSize.height
        if (w > 0 && h > 0) {
            val paths = randomPathSet(Random(1), 1).map { pts ->
                ScaledPath(pts.map { (nx, ny) -> nx * w to ny * h })
            }
            bg = withContext(Dispatchers.Default) { renderStaticScene(w, h, density, paths, 1) }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { boxSize = it }
    ) {
        val bitmap = bg
        if (bitmap != null) {
            val dstW = boxSize.width
            val dstH = boxSize.height
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawImage(
                    bitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bitmap.width, bitmap.height),
                    dstOffset = IntOffset.Zero,
                    dstSize = IntSize(dstW, dstH),
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2E7D32)))
        }
        Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)))
        content()
    }
}

// ==================== MENU CHÍNH ====================
// 3 nút: Chơi (tiếp tục màn đang chơi), Cửa hàng, Thoát.
@Composable
fun MenuScreen(
    save: SaveManager,
    soundManager: SoundManager,
    onPlay: () -> Unit,
    onShop: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val weapon = save.selectedWeapon
    val level = save.unlockedLevel // màn đang chơi (chỉ chơi màn này, không chơi lại màn đã qua)

    FarmBackground {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🐔", fontSize = 56.sp)
                Text(
                    "BẢO VỆ ĐÀN GÀ",
                    color = Color(0xFFFFD54F),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(color = Color(0xFF000000), offset = Offset(3f, 4f), blurRadius = 8f)
                    ),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Chạm liên tục lên quái để đập chết,\nđừng để chúng ăn hết gà!",
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        shadow = Shadow(color = Color(0xFF000000), offset = Offset(1f, 2f), blurRadius = 4f)
                    ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Bảng thông tin: xu, màn, vũ khí.
                Column(
                    modifier = Modifier
                        .background(Color(0x88000000), shape = RoundedCornerShape(14.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🪙 ${save.coins} xu", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Màn đang chơi: $level", color = Color.White, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(weapon.imageRes),
                            contentDescription = weapon.name,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vũ khí: ${weapon.name} (sát thương ${weapon.damage})", color = Color.White, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { soundManager.play("click"); onPlay() },
                    modifier = Modifier.fillMaxWidth(0.7f).height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                ) {
                    Text("▶ Chơi — Level $level", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { soundManager.play("click"); onShop() },
                    modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                ) {
                    Text("🛒 Cửa hàng", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { soundManager.play("click"); showExit = true },
                    modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("🚪 Thoát", fontSize = 18.sp, color = Color.White)
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = { soundManager.play("click"); showHelp = true }) {
                    Text("❓ Cách chơi", color = Color.White, fontSize = 14.sp)
                }
            }
            // Công tắc âm thanh góc phải trên.
            TextButton(
                onClick = { soundManager.muted = !soundManager.muted },
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Text(if (soundManager.muted) "🔇" else "🔊", fontSize = 26.sp)
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1B5E20),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFC8E6C9),
            title = { Text("Cách chơi") },
            text = {
                Text(
                    "• Quái đi từ rừng 2 bên theo đường đá vào tổ gà ở giữa. Mỗi trận đường đi khác nhau!\n" +
                        "• Chạm liên tục lên quái để đập chết bằng vũ khí. Chạm nhiều ngón cùng lúc được!\n" +
                        "• Chạm trúng liên tiếp để lên combo — combo càng cao càng dễ chí mạng (sát thương gấp đôi). Chạm trượt là mất combo.\n" +
                        "• Vũ khí xịn có hiệu ứng riêng: bom nổ lan, súng nước làm chậm, Thor giật điện lan, cung xuyên nhiều con, kiếm lửa đốt cháy, gậy phép đóng băng.\n" +
                        "• Quái có hành vi riêng: cáo/báo lao nhanh đột ngột, rắn/nhện chết tách thành con nhỏ, gấu/cá sấu/tê giác có khiên (chỉ đánh được lúc khiên đóng), khỉ cướp gà chạy ngược (giết nó trên đường về để cứu gà).\n" +
                        "• Mỗi 10 level có 1 boss khổng lồ ở cuối đợt 3.\n" +
                        "• Giữa các đợt được chọn 1 trong 3 buff ngẫu nhiên (tăng sát thương, thêm gà, chí mạng, làm chậm quái...).\n" +
                        "• Quái tới tổ sẽ ăn 1 con gà. Hết 5 con gà là thua, nhưng vẫn giữ xu đã giết quái.\n" +
                        "• Thắng màn thì sang cửa hàng mua vũ khí, bấm Tiếp theo để chơi màn kế.\n" +
                        "• Chỉ được chơi màn đang chơi, không chơi lại màn đã qua để cày xu.\n" +
                        "• Thoát app ra vào lại vẫn giữ nguyên tiến trình."
                )
            },
            confirmButton = {
                Button(
                    onClick = { soundManager.play("click"); showHelp = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                ) { Text("Đã hiểu") }
            },
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color(0xFF1B5E20),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFC8E6C9),
            title = { Text("Cút ok?") },
            text = { Text("Thoát khỏi game nhé? Tiến trình đã được lưu rồi.") },
            confirmButton = {
                Button(
                    onClick = { (context as? Activity)?.finish() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { soundManager.play("click"); showExit = false }) { Text("Không", color = Color.White) }
            },
        )
    }
}

// ==================== CỬA HÀNG ====================
// Vũ khí bày theo lưới ngang 4 cột, nhìn một lần thấy hết, không cần kéo xuống.
// Khi vừa thắng 1 màn (showNext = true): hiện nút "Tiếp theo ▶" ở góc phải
// để sang màn kế, kèm dòng chúc mừng + xu kiếm được.
@Composable
fun ShopScreen(
    save: SaveManager,
    soundManager: SoundManager,
    onBack: () -> Unit,
    showNext: Boolean = false,
    winInfo: String? = null,
    onNext: (() -> Unit)? = null,
) {
    var message by remember { mutableStateOf<String?>(null) }

    FarmBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { soundManager.play("click"); onBack() }) { Text("← Quay lại", color = Color.White, fontSize = 16.sp) }
                Spacer(modifier = Modifier.weight(1f))
                if (showNext) {
                    Button(
                        onClick = { soundManager.play("click"); onNext?.invoke() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                    ) {
                        Text("Tiếp theo ▶", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                "🛒 Cửa hàng vũ khí",
                color = Color(0xFFFFD54F),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
                style = TextStyle(
                    shadow = Shadow(color = Color(0xFF000000), offset = Offset(1f, 2f), blurRadius = 4f)
                ),
            )
            if (winInfo != null) {
                Text(
                    winInfo,
                    color = Color(0xFFFFD54F),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "🪙 ${save.coins} xu",
                color = Color(0xFFFFD54F),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (message != null) {
                Text(
                    message!!,
                    color = Color(0xFFFFCDD2),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(WEAPONS) { weapon ->
                    val isOwned = save.ownedWeapons.contains(weapon.id)
                    val isSelected = save.selectedWeaponId == weapon.id
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = if (isSelected) BorderStroke(2.dp, Color(0xFFFFD54F)) else null,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFFF8C42) else Color(0xDD2E7D32)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Image(
                                painter = painterResource(weapon.imageRes),
                                contentDescription = weapon.name,
                                modifier = Modifier.size(64.dp),
                            )
                            Text(
                                weapon.name,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Sát thương: ${weapon.damage}",
                                color = Color(0xFFC8E6C9),
                                fontSize = 12.sp,
                            )
                            if (weapon.effect.isNotEmpty()) {
                                Text(
                                    weapon.effect,
                                    color = Color(0xFFFFD54F),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            when {
                                isSelected -> Text("✔ Đang dùng", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                isOwned -> Button(
                                    onClick = {
                                        soundManager.play("click")
                                        save.selectWeapon(weapon)
                                        message = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                                ) { Text("Chọn", fontSize = 13.sp) }
                                else -> Button(
                                    onClick = {
                                        if (save.buyWeapon(weapon)) {
                                            soundManager.play("coin")
                                            message = "Đã mua ${weapon.name}!"
                                        } else {
                                            soundManager.play("click")
                                            message = "Chưa đủ xu mua ${weapon.name} (cần ${weapon.price} xu)."
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
                                ) { Text("Mua ${weapon.price} xu", fontSize = 12.sp) }
                            }
                        }
                    }
                }
            }
        }
    }
}
