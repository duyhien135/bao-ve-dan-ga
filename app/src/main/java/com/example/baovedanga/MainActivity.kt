package com.example.baovedanga

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class Screen { MENU, SHOP, GAME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val save = SaveManager(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf(Screen.MENU) }
                    var playingLevel by remember { mutableStateOf(levelDef(1)) }
                    // Đổi mỗi lần vào màn mới để GameScreen tạo ván mới.
                    var gameKey by remember { mutableStateOf(0) }
                    // Thắng màn thì sang cửa hàng, kèm nút "Tiếp theo" và thông tin xu.
                    var shopFromWin by remember { mutableStateOf(false) }
                    var winInfo by remember { mutableStateOf<String?>(null) }

                    when (screen) {
                        Screen.MENU -> MenuScreen(
                            save = save,
                            // Chỉ chơi màn đang chơi (màn đã mở cao nhất); chơi mới thì là màn 1.
                            onPlay = {
                                playingLevel = levelDef(save.unlockedLevel)
                                gameKey++
                                screen = Screen.GAME
                            },
                            onShop = {
                                shopFromWin = false
                                winInfo = null
                                screen = Screen.SHOP
                            },
                        )
                        Screen.SHOP -> {
                            BackHandler { screen = Screen.MENU }
                            ShopScreen(
                                save = save,
                                onBack = { screen = Screen.MENU },
                                showNext = shopFromWin,
                                winInfo = winInfo,
                                onNext = {
                                    playingLevel = levelDef(save.unlockedLevel)
                                    gameKey++
                                    screen = Screen.GAME
                                },
                            )
                        }
                        Screen.GAME -> {
                            androidx.compose.runtime.key(gameKey) {
                                GameScreen(
                                    level = playingLevel,
                                    weapon = save.selectedWeapon,
                                    save = save,
                                    onExit = { screen = Screen.MENU },
                                    onWin = { totalCoins ->
                                        winInfo = "🎉 Thắng level ${playingLevel.number}! +$totalCoins xu"
                                        shopFromWin = true
                                        screen = Screen.SHOP
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
