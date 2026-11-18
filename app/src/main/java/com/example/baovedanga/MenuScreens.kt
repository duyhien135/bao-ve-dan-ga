package com.example.baovedanga

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GreenDark = Color(0xFF1B5E20)
private val GreenMid = Color(0xFF2E7D32)
private val Cream = Color(0xFFFFF8E1)

// ==================== MENU CHÍNH ====================
@Composable
fun MenuScreen(
    save: SaveManager,
    onPlay: () -> Unit,
    onShop: () -> Unit,
) {
    var showHelp by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("\uD83D\uDC14", fontSize = 72.sp)
        Text(
            "Bảo vệ đàn gà",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            "Chạm liên tục để đập quái,\nđừng để chúng ăn hết gà!",
            color = Color(0xFFC8E6C9),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "\uD83E\uDE99 ${save.coins} xu",
            color = Color(0xFFFFD54F),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "Vũ khí: ${save.selectedWeapon.emoji} ${save.selectedWeapon.name} (sát thương ${save.selectedWeapon.damage})",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(modifier = Modifier.height(28.dp))
        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
        ) { Text("▶️  Chơi", fontSize = 20.sp) }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onShop,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp),
        ) { Text("🛒  Cửa hàng", fontSize = 20.sp) }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = { showHelp = true }) {
            Text("❓ Cách chơi", color = Color(0xFFC8E6C9), fontSize = 16.sp)
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Cách chơi") },
            text = {
                Text(
                    "• Quái đi theo đường đất từ rừng vào tổ gà. Chạm liên tục lên quái để đập chết chúng.\n\n" +
                        "• Quái tới tổ sẽ ăn 1 con gà. Hết gà là thua ván.\n\n" +
                        "• Giết quái được xu. Thắng level được thêm xu thưởng và mở level tiếp theo.\n\n" +
                        "• Vào Cửa hàng mua vũ khí mạnh hơn: đập càng đau, giết quái càng nhanh.\n\n" +
                        "• Cáo yếu nhất (3 máu), càng lên level càng gặp quái mạnh: rắn, sói, nhện, gấu, khỉ, hổ, báo, cá sấu, sư tử, tê giác, voi, rồng.\n\n" +
                        "• Level vô hạn: thắng là mở level tiếp, quái càng lên cao càng trâu và càng đông."
                )
            },
            confirmButton = {
                Button(onClick = { showHelp = false }) { Text("Đã hiểu") }
            },
        )
    }
}

// ==================== CHỌN LEVEL ====================
@Composable
fun LevelSelectScreen(
    save: SaveManager,
    onLevel: (LevelDef) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenDark)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Quay lại", color = Color.White, fontSize = 16.sp) }
            Spacer(modifier = Modifier.weight(1f))
            Text("\uD83E\uDE99 ${save.coins}", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Chọn level",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Level vô hạn: chỉ hiện các level đã mở + level tiếp theo (đang khóa).
            items((1..save.unlockedLevel + 1).toList()) { n ->
                val level = levelDef(n)
                val unlocked = level.number <= save.unlockedLevel
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (unlocked) Cream else Color(0xFF424242)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = unlocked) { onLevel(level) },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (unlocked) "Level ${level.number}" else "🔒 Level ${level.number}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (unlocked) Color(0xFF212121) else Color(0xFF9E9E9E),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "${level.waves.size} đợt · ${level.pathCount} đường",
                            fontSize = 13.sp,
                            color = if (unlocked) Color(0xFF616161) else Color(0xFF757575),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "🐔 × ${level.chickens}",
                            fontSize = 13.sp,
                            color = if (unlocked) Color(0xFF616161) else Color(0xFF757575),
                        )
                    }
                }
            }
        }
    }
}

// ==================== CỬA HÀNG ====================
@Composable
fun ShopScreen(
    save: SaveManager,
    onBack: () -> Unit,
) {
    var message by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenDark)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Quay lại", color = Color.White, fontSize = 16.sp) }
            Spacer(modifier = Modifier.weight(1f))
            Text("\uD83E\uDE99 ${save.coins} xu", color = Color(0xFFFFD54F), fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Cửa hàng vũ khí",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp),
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(WEAPONS) { weapon ->
                val owned = save.ownedWeapons.contains(weapon.id)
                val selected = save.selectedWeaponId == weapon.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFFC8E6C9) else Cream
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(weapon.emoji, fontSize = 36.sp)
                        Spacer(modifier = Modifier.size(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(weapon.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Sát thương: ${weapon.damage}", fontSize = 14.sp, color = Color(0xFF616161))
                            if (!owned) {
                                Text("Giá: ${weapon.price} xu", fontSize = 14.sp, color = Color(0xFFF57F17), fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!owned) {
                            Button(
                                onClick = {
                                    if (save.buyWeapon(weapon)) {
                                        message = "Đã mua ${weapon.name}!"
                                    } else {
                                        message = "Không đủ xu! Cần ${weapon.price} xu."
                                    }
                                },
                                enabled = save.coins >= weapon.price,
                            ) { Text("Mua") }
                        } else if (selected) {
                            Text("✔ Đang dùng", fontSize = 14.sp, color = GreenMid, fontWeight = FontWeight.Bold)
                        } else {
                            Button(onClick = { save.selectWeapon(weapon) }) { Text("Chọn") }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Mẹo: giết quái và thắng level để tích xu mua vũ khí mạnh hơn.",
            color = Color(0xFFC8E6C9),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    message?.let { msg ->
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text("Cửa hàng") },
            text = { Text(msg) },
            confirmButton = {
                Button(onClick = { message = null }) { Text("OK") }
            },
        )
    }
}
