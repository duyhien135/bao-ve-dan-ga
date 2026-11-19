package com.example.baovedanga

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Nền gradient xanh lá dùng chung cho menu + cửa hàng.
private val menuBackground = Brush.verticalGradient(
    listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF43A047))
)

// ==================== MENU CHÍNH ====================
// 3 nút: Chơi (tiếp tục màn đang chơi), Cửa hàng, Thoát.
@Composable
fun MenuScreen(
    save: SaveManager,
    onPlay: () -> Unit,
    onShop: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val weapon = save.selectedWeapon
    val level = save.unlockedLevel // màn đang chơi (chỉ chơi màn này, không chơi lại màn đã qua)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(menuBackground)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🐔", fontSize = 56.sp)
        Text(
            "BẢO VỆ ĐÀN GÀ",
            color = Color(0xFFFFD54F),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            style = TextStyle(
                shadow = Shadow(color = Color(0xFF000000), offset = Offset(2f, 3f), blurRadius = 6f)
            ),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Chạm liên tục lên quái để đập chết,\nđừng để chúng ăn hết gà!",
            color = Color(0xFFC8E6C9),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        // Bảng thông tin: xu, màn, vũ khí.
        Column(
            modifier = Modifier
                .background(Color(0x55000000), shape = RoundedCornerShape(14.dp))
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
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8C42)),
        ) {
            Text("▶ Chơi — Level $level", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(
            onClick = onShop,
            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
        ) {
            Text("🛒 Cửa hàng", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = { showExit = true },
            modifier = Modifier.fillMaxWidth(0.7f).height(48.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text("🚪 Thoát", fontSize = 18.sp, color = Color.White)
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { showHelp = true }) {
            Text("❓ Cách chơi", color = Color(0xFFC8E6C9), fontSize = 14.sp)
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
                        "• Máu quái hiện dạng số, ví dụ cáo 3/3. Quái còn ít máu thì mắt hiện X đỏ.\n" +
                        "• Quái tới tổ sẽ ăn 1 con gà. Hết 5 con gà là thua, nhưng vẫn giữ xu đã giết quái.\n" +
                        "• Thắng màn thì sang cửa hàng mua vũ khí, bấm Tiếp theo để chơi màn kế.\n" +
                        "• Mỗi màn chỉ 3 đợt, quái chạy rất nhanh và càng về đợt sau càng nhanh, nên phải đánh nhanh tay.\n" +
                        "• Quái mới chỉ xuất hiện sau nhiều màn chơi. Chỉ được chơi màn đang chơi, không chơi lại màn đã qua để cày xu.\n" +
                        "• Thoát app ra vào lại vẫn giữ nguyên tiến trình."
                )
            },
            confirmButton = {
                Button(
                    onClick = { showHelp = false },
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
                TextButton(onClick = { showExit = false }) { Text("Không", color = Color.White) }
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
    onBack: () -> Unit,
    showNext: Boolean = false,
    winInfo: String? = null,
    onNext: (() -> Unit)? = null,
) {
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(menuBackground)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) { Text("← Quay lại", color = Color.White, fontSize = 16.sp) }
            Spacer(modifier = Modifier.weight(1f))
            if (showNext) {
                Button(
                    onClick = { onNext?.invoke() },
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
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = if (isSelected) BorderStroke(2.dp, Color(0xFFFFD54F)) else null,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFFF8C42) else Color(0xFF2E7D32)
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
                            modifier = Modifier.size(52.dp),
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
                        Spacer(modifier = Modifier.height(4.dp))
                        when {
                            isSelected -> Text("✔ Đang dùng", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            isOwned -> Button(
                                onClick = {
                                    save.selectWeapon(weapon)
                                    message = null
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047)),
                            ) { Text("Chọn", fontSize = 13.sp) }
                            else -> Button(
                                onClick = {
                                    if (save.buyWeapon(weapon)) {
                                        message = "Đã mua ${weapon.name}!"
                                    } else {
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
