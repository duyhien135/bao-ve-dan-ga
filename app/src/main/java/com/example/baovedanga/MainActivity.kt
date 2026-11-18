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

private enum class Screen { MENU, LEVELS, SHOP, GAME }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val save = SaveManager(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var screen by remember { mutableStateOf(Screen.MENU) }
                    var playingLevel by remember { mutableStateOf(LEVELS[0]) }
                    // Đổi mỗi lần sang level tiếp theo để GameScreen tạo ván mới.
                    var gameKey by remember { mutableStateOf(0) }

                    when (screen) {
                        Screen.MENU -> MenuScreen(
                            save = save,
                            onPlay = { screen = Screen.LEVELS },
                            onShop = { screen = Screen.SHOP },
                        )
                        Screen.LEVELS -> {
                            BackHandler { screen = Screen.MENU }
                            LevelSelectScreen(
                                save = save,
                                onLevel = {
                                    playingLevel = it
                                    gameKey++
                                    screen = Screen.GAME
                                },
                                onBack = { screen = Screen.MENU },
                            )
                        }
                        Screen.SHOP -> {
                            BackHandler { screen = Screen.MENU }
                            ShopScreen(
                                save = save,
                                onBack = { screen = Screen.MENU },
                            )
                        }
                        Screen.GAME -> {
                            androidx.compose.runtime.key(gameKey) {
                                GameScreen(
                                    level = playingLevel,
                                    weapon = save.selectedWeapon,
                                    save = save,
                                    onExit = { screen = Screen.MENU },
                                    onNextLevel = {
                                        val next = LEVELS.getOrNull(playingLevel.number) // level.number là 1-based
                                        if (next != null) {
                                            playingLevel = next
                                            gameKey++
                                        } else {
                                            screen = Screen.MENU
                                        }
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
