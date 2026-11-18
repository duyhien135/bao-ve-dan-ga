package com.example.baovedanga

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

// Lưu tiến trình vào SharedPreferences: xu, level đã mở, vũ khí đã mua/đang dùng.
class SaveManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("chicken_defense", Context.MODE_PRIVATE)

    var coins by mutableStateOf(prefs.getInt("coins", 0))
        private set

    var unlockedLevel by mutableStateOf(prefs.getInt("unlocked_level", 1))
        private set

    var ownedWeapons by mutableStateOf(
        prefs.getStringSet("owned_weapons", setOf("swatter")) ?: setOf("swatter")
    )
        private set

    var selectedWeaponId by mutableStateOf(prefs.getString("selected_weapon", "swatter") ?: "swatter")
        private set

    val selectedWeapon: Weapon get() = weaponById(selectedWeaponId)

    fun addCoins(amount: Int) {
        coins += amount
        prefs.edit().putInt("coins", coins).apply()
    }

    fun unlockLevel(level: Int) {
        if (level > unlockedLevel) {
            unlockedLevel = level
            prefs.edit().putInt("unlocked_level", unlockedLevel).apply()
        }
    }

    // Mua vũ khí: trừ xu, thêm vào kho, tự chọn làm vũ khí đang dùng.
    // Trả về true nếu mua được, false nếu không đủ xu hoặc đã mua rồi.
    fun buyWeapon(weapon: Weapon): Boolean {
        if (ownedWeapons.contains(weapon.id)) return false
        if (coins < weapon.price) return false
        coins -= weapon.price
        ownedWeapons = ownedWeapons + weapon.id
        selectedWeaponId = weapon.id
        prefs.edit()
            .putInt("coins", coins)
            .putStringSet("owned_weapons", ownedWeapons)
            .putString("selected_weapon", selectedWeaponId)
            .apply()
        return true
    }

    fun selectWeapon(weapon: Weapon) {
        if (!ownedWeapons.contains(weapon.id)) return
        selectedWeaponId = weapon.id
        prefs.edit().putString("selected_weapon", selectedWeaponId).apply()
    }
}
