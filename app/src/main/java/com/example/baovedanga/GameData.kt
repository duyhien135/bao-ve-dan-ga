package com.example.baovedanga

import kotlin.random.Random

// ==================== VŨ KHÍ ====================
// damage: sát thương mỗi lần chạm. price: giá mua bằng xu (0 = có sẵn).
// imageRes: ảnh vũ khí trong drawable-nodpi (0 = chưa có ảnh, dùng emoji).
data class Weapon(
    val id: String,
    val name: String,
    val emoji: String,
    val damage: Int,
    val price: Int,
    val imageRes: Int = 0,
)

val WEAPONS = listOf(
    Weapon("swatter", "Vỉ ruồi", "\uD83E\uDEB0", 1, 0, R.drawable.wpn_swatter),
    Weapon("hammer", "Búa", "\uD83D\uDD28", 2, 2000, R.drawable.wpn_hammer),
    Weapon("shovel", "Xẻng", "\uD83E\uDE8F", 3, 5000, R.drawable.wpn_shovel),
    Weapon("axe", "Rìu", "\uD83E\uDE93", 5, 12000, R.drawable.wpn_axe),
    Weapon("sword", "Kiếm", "⚔️", 8, 30000, R.drawable.wpn_sword),
    Weapon("thor", "Búa Thor", "⚡", 12, 70000, R.drawable.wpn_thor),
    Weapon("trident", "Giáo", "🔱", 16, 150000, R.drawable.wpn_trident),
    Weapon("bow", "Cung", "🏹", 20, 300000, R.drawable.wpn_bow),
    Weapon("watergun", "Súng nước", "🔫", 26, 600000, R.drawable.wpn_watergun),
    Weapon("bomb", "Bom", "💣", 35, 1200000, R.drawable.wpn_bomb),
    Weapon("fireblade", "Kiếm lửa", "🔥", 50, 2500000, R.drawable.wpn_fireblade),
    Weapon("wand", "Gậy phép", "🪄", 70, 5000000, R.drawable.wpn_wand),
)

fun weaponById(id: String): Weapon = WEAPONS.firstOrNull { it.id == id } ?: WEAPONS[0]

// ==================== QUÁI ====================
// hp: máu. speedDp: tốc độ di chuyển (dp/giây). coins: xu nhận khi giết.
// radiusScale: kích thước vòng tròn quái so với chuẩn. color: màu thân.
// minLevel: quái chỉ xuất hiện từ level này trở lên (mở rất chậm để chơi lâu).
// imageRes: ảnh quái trong drawable-nodpi.
data class MonsterType(
    val id: String,
    val name: String,
    val emoji: String,
    val hp: Int,
    val speedDp: Float,
    val coins: Int,
    val radiusScale: Float,
    val color: Long,
    val minLevel: Int = 1,
    val imageRes: Int = 0,
)

val MONSTER_TYPES = listOf(
    MonsterType("fox", "Cáo", "\uD83E\uDD8A", 3, 100f, 5, 1.0f, 0xFFFF7043, 1, R.drawable.mon_fox),
    MonsterType("snake", "Rắn", "🐍", 8, 150f, 10, 0.9f, 0xFF66BB6A, 6, R.drawable.mon_snake),
    MonsterType("wolf", "Sói", "\uD83D\uDC3A", 6, 125f, 8, 1.05f, 0xFF78909C, 10, R.drawable.mon_wolf),
    MonsterType("spider", "Nhện", "🕷️", 10, 115f, 12, 0.95f, 0xFF5D4037, 15, R.drawable.mon_spider),
    MonsterType("bear", "Gấu", "\uD83D\uDC3B", 12, 65f, 15, 1.25f, 0xFF8D6E63, 20, R.drawable.mon_bear),
    MonsterType("monkey", "Khỉ", "🐒", 14, 135f, 18, 1.0f, 0xFFA1887F, 26, R.drawable.mon_monkey),
    MonsterType("tiger", "Hổ", "\uD83D\uDC2F", 20, 110f, 25, 1.2f, 0xFFFFA000, 32, R.drawable.mon_tiger),
    MonsterType("leopard", "Báo", "🐆", 16, 170f, 20, 1.1f, 0xFFFBC02D, 38, R.drawable.mon_leopard),
    MonsterType("croc", "Cá sấu", "\uD83D\uDC0A", 30, 70f, 35, 1.3f, 0xFF558B2F, 45, R.drawable.mon_croc),
    MonsterType("lion", "Sư tử", "🦁", 40, 100f, 45, 1.35f, 0xFFEF6C00, 52, R.drawable.mon_lion),
    MonsterType("rhino", "Tê giác", "🦏", 60, 55f, 60, 1.45f, 0xFF78909C, 60, R.drawable.mon_rhino),
    MonsterType("elephant", "Voi", "\uD83D\uDC18", 45, 45f, 50, 1.5f, 0xFF90A4AE, 68, R.drawable.mon_elephant),
    MonsterType("dragon", "Rồng", "🐉", 90, 80f, 100, 1.6f, 0xFFC62828, 78, R.drawable.mon_dragon),
)

fun monsterTypeById(id: String): MonsterType = MONSTER_TYPES.firstOrNull { it.id == id } ?: MONSTER_TYPES[0]

// ==================== LEVEL ====================
// Một đợt (wave) gồm nhiều nhóm quái sinh ra nối tiếp nhau.
// gap: khoảng cách giữa 2 con trong nhóm (giây).
data class SpawnGroup(val type: String, val count: Int, val gap: Float = 1.1f)
data class LevelDef(
    val number: Int,
    val pathCount: Int, // số đường quái đi vào (1..4)
    val waves: List<List<SpawnGroup>>,
    val chickens: Int = 5,
)

// Số đường theo level: đường 2 mãi mới xuất hiện, ít đường, tăng quái + tốc độ thay vào đó.
fun pathCountForLevel(number: Int): Int = when {
    number < 15 -> 1
    number < 30 -> 2
    number < 45 -> 3
    else -> 4
}

// Level vô hạn, sinh tự động theo số level (Random gieo theo số level nên cùng
// 1 level luôn ra cùng 1 đội hình). Mỗi màn chỉ 3 đợt, quái chạy rất nhanh.
// Quái mới mở rất chậm; loại mới nhất mỗi màn chỉ xuất hiện 1-3 con.
fun levelDef(number: Int): LevelDef {
    val rng = Random(number * 7919L)
    val unlocked = MONSTER_TYPES.filter { it.minLevel <= number }
    val pathCount = pathCountForLevel(number)
    val waveCount = 3
    val gap = maxOf(0.7f, 1.4f - 0.02f * number)
    // Quái quen (mở được từ 5 level trước) làm chủ lực; quái mới nhất chỉ lác đác vài con.
    val common = unlocked.filter { it.minLevel <= number - 5 }.ifEmpty { unlocked }
    val newest = unlocked.filter { it.minLevel > number - 5 }
    var newestLeft = if (newest.isNotEmpty()) 3 else 0
    val perWaveBase = 25 + number * 2 // quái mỗi đợt, tăng theo level (giới hạn dưới)
    val waves = List(waveCount) { _ ->
        val groups = mutableListOf<SpawnGroup>()
        var remaining = minOf(perWaveBase + rng.nextInt(10), 70)
        while (remaining > 0) {
            // Thỉnh thoảng nhét 1-2 con quái mới nhất vào giữa đợt.
            if (newestLeft > 0 && rng.nextFloat() < 0.25f) {
                val n = minOf(newestLeft, 1 + rng.nextInt(2))
                groups += SpawnGroup(newest.random(rng).id, n, gap)
                newestLeft -= n
                remaining -= n
            } else {
                val type = common.random(rng)
                val count = minOf(remaining, 8 + rng.nextInt(14))
                groups += SpawnGroup(type.id, count, gap)
                remaining -= count
            }
        }
        groups
    }
    return LevelDef(number, pathCount, waves)
}

// Xu thưởng khi thắng level.
fun winBonus(level: Int): Int = 25 + 20 * level

// ==================== ĐƯỜNG QUÁI ĐI ====================
// Màn hình ngang: đường chỉ đi ngang/dọc (gấp khúc 90 độ, không chéo),
// các góc được bo tròn khi vẽ. Tọa độ chuẩn hóa (0..1) theo kích thước vùng chơi.
// Mỗi trận sinh đường ngẫu nhiên nên không trận nào giống trận nào.
// 4 đường có 4 hướng vào tổ khác nhau: trái, phải, trên, dưới.
private fun randY(rng: Random, lo: Float, hi: Float): Float = lo + rng.nextFloat() * (hi - lo)

// y ngẫu nhiên nhưng tránh vùng giữa (để đoạn gấp khúc không bị ngắn quá).
private fun randYOffCenter(rng: Random): Float {
    var y = randY(rng, 0.10f, 0.90f)
    if (y > 0.40f && y < 0.60f) y = if (y < 0.50f) y - 0.22f else y + 0.22f
    return y
}

fun randomPathSet(rng: Random, pathCount: Int): List<List<Pair<Float, Float>>> {
    val paths = mutableListOf<List<Pair<Float, Float>>>()
    // Đường 0: vào từ trái, tới tổ theo hướng ngang từ trái.
    if (pathCount >= 1) {
        val y0 = randY(rng, 0.12f, 0.88f)
        val x1 = randY(rng, 0.08f, 0.25f)
        val y1 = randYOffCenter(rng)
        val x2 = randY(rng, maxOf(x1 + 0.08f, 0.27f), 0.40f)
        paths += listOf(
            -0.05f to y0, x1 to y0, x1 to y1, x2 to y1, x2 to 0.50f, 0.50f to 0.50f,
        )
    }
    // Đường 1: vào từ phải, tới tổ theo hướng ngang từ phải.
    if (pathCount >= 2) {
        val y0 = randY(rng, 0.12f, 0.88f)
        val x1 = randY(rng, 0.75f, 0.92f)
        val y1 = randYOffCenter(rng)
        val x2 = randY(rng, 0.60f, minOf(x1 - 0.08f, 0.73f))
        paths += listOf(
            1.05f to y0, x1 to y0, x1 to y1, x2 to y1, x2 to 0.50f, 0.50f to 0.50f,
        )
    }
    // Đường 2: vào từ trái, tới tổ theo hướng dọc từ trên.
    if (pathCount >= 3) {
        val y0 = randY(rng, 0.15f, 0.85f)
        val x1 = randY(rng, 0.08f, 0.30f)
        val y1 = randY(rng, 0.10f, 0.32f)
        paths += listOf(
            -0.05f to y0, x1 to y0, x1 to y1, 0.50f to y1, 0.50f to 0.50f,
        )
    }
    // Đường 3: vào từ phải, tới tổ theo hướng dọc từ dưới.
    if (pathCount >= 4) {
        val y0 = randY(rng, 0.15f, 0.85f)
        val x1 = randY(rng, 0.70f, 0.92f)
        val y1 = randY(rng, 0.68f, 0.90f)
        paths += listOf(
            1.05f to y0, x1 to y0, x1 to y1, 0.50f to y1, 0.50f to 0.50f,
        )
    }
    return paths
}

// Tổ gà ở giữa map (tọa độ chuẩn hóa).
const val NEST_X = 0.50f
const val NEST_Y = 0.50f

// Hàng rào (tọa độ chuẩn hóa): hình chữ nhật quanh vườn rau ở giữa màn hình ngang.
const val FENCE_LEFT = 0.33f
const val FENCE_TOP = 0.15f
const val FENCE_RIGHT = 0.67f
const val FENCE_BOTTOM = 0.85f
