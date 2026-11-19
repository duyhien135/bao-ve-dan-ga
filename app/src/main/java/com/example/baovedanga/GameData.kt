package com.example.baovedanga

import kotlin.random.Random

// ==================== VŨ KHÍ ====================
// damage: sát thương mỗi lần chạm. price: giá mua bằng xu (0 = có sẵn).
// imageRes: ảnh vũ khí trong drawable-nodpi (0 = chưa có ảnh, dùng emoji).
// effect: mô tả hiệu ứng riêng của vũ khí ("" = chỉ gây sát thương).
data class Weapon(
    val id: String,
    val name: String,
    val emoji: String,
    val damage: Int,
    val price: Int,
    val imageRes: Int = 0,
    val effect: String = "",
)

val WEAPONS = listOf(
    Weapon("swatter", "Vỉ ruồi", "\uD83E\uDEB0", 1, 0, R.drawable.wpn_swatter),
    Weapon("hammer", "Búa", "\uD83D\uDD28", 2, 2000, R.drawable.wpn_hammer),
    Weapon("shovel", "Xẻng", "\uD83E\uDE8F", 3, 5000, R.drawable.wpn_shovel),
    Weapon("axe", "Rìu", "\uD83E\uDE93", 5, 12000, R.drawable.wpn_axe),
    Weapon("sword", "Kiếm", "⚔️", 8, 30000, R.drawable.wpn_sword),
    Weapon("thor", "Búa Thor", "⚡", 12, 70000, R.drawable.wpn_thor, "Giật điện lan sang 3 con gần nhất"),
    Weapon("trident", "Giáo", "🔱", 16, 150000, R.drawable.wpn_trident, "Đâm xuyên thêm 1 con gần nhất"),
    Weapon("bow", "Cung", "🏹", 20, 300000, R.drawable.wpn_bow, "Mũi tên xuyên nhiều con trên đường"),
    Weapon("watergun", "Súng nước", "🔫", 26, 600000, R.drawable.wpn_watergun, "Làm chậm quái 3 giây"),
    Weapon("bomb", "Bom", "💣", 35, 1200000, R.drawable.wpn_bomb, "Nổ lan diện rộng"),
    Weapon("fireblade", "Kiếm lửa", "🔥", 50, 2500000, R.drawable.wpn_fireblade, "Đốt cháy quái 3 giây"),
    Weapon("wand", "Gậy phép", "🪄", 70, 5000000, R.drawable.wpn_wand, "20% đóng băng quái 1,5 giây"),
)

fun weaponById(id: String): Weapon = WEAPONS.firstOrNull { it.id == id } ?: WEAPONS[0]

// ==================== QUÁI ====================
// hp: máu. speedDp: tốc độ di chuyển (dp/giây). coins: xu nhận khi giết.
// radiusScale: kích thước vòng tròn quái so với chuẩn. color: màu thân.
// minLevel: quái chỉ xuất hiện từ level này trở lên (mở rất chậm để chơi lâu).
// imageRes: ảnh quái trong drawable-nodpi.
// behavior: hành vi riêng — normal, dash (lao nhanh đột ngột), shield
// (khiên chặn sát thương theo chu kỳ 2s mở/2s đóng), split (chết tách ra
// splitCount con nhỏ), thief (cướp gà rồi chạy ngược về rừng).
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
    val behavior: String = "normal",
    val splitCount: Int = 0,
)

val MONSTER_TYPES = listOf(
    MonsterType("fox", "Cáo", "\uD83E\uDD8A", 3, 130f, 5, 1.0f, 0xFFFF7043, 1, R.drawable.mon_fox, "dash"),
    MonsterType("snake", "Rắn", "🐍", 8, 195f, 10, 0.9f, 0xFF66BB6A, 6, R.drawable.mon_snake, "split", 2),
    MonsterType("wolf", "Sói", "\uD83D\uDC3A", 6, 160f, 8, 1.05f, 0xFF78909C, 10, R.drawable.mon_wolf),
    MonsterType("spider", "Nhện", "🕷️", 10, 150f, 12, 0.95f, 0xFF5D4037, 15, R.drawable.mon_spider, "split", 3),
    MonsterType("bear", "Gấu", "\uD83D\uDC3B", 12, 85f, 15, 1.25f, 0xFF8D6E63, 20, R.drawable.mon_bear, "shield"),
    MonsterType("monkey", "Khỉ", "🐒", 14, 175f, 18, 1.0f, 0xFFA1887F, 26, R.drawable.mon_monkey, "thief"),
    MonsterType("tiger", "Hổ", "\uD83D\uDC2F", 20, 140f, 25, 1.2f, 0xFFFFA000, 32, R.drawable.mon_tiger, "dash"),
    MonsterType("leopard", "Báo", "🐆", 16, 220f, 20, 1.1f, 0xFFFBC02D, 38, R.drawable.mon_leopard, "dash"),
    MonsterType("croc", "Cá sấu", "\uD83D\uDC0A", 30, 90f, 35, 1.3f, 0xFF558B2F, 45, R.drawable.mon_croc, "shield"),
    MonsterType("lion", "Sư tử", "🦁", 40, 130f, 45, 1.35f, 0xFFEF6C00, 52, R.drawable.mon_lion),
    MonsterType("rhino", "Tê giác", "🦏", 60, 70f, 60, 1.45f, 0xFF78909C, 60, R.drawable.mon_rhino, "shield"),
    MonsterType("elephant", "Voi", "\uD83D\uDC18", 45, 60f, 50, 1.5f, 0xFF90A4AE, 68, R.drawable.mon_elephant),
    MonsterType("dragon", "Rồng", "🐉", 90, 105f, 100, 1.6f, 0xFFC62828, 78, R.drawable.mon_dragon),
)

fun monsterTypeById(id: String): MonsterType = MONSTER_TYPES.firstOrNull { it.id == id } ?: MONSTER_TYPES[0]

// ==================== LEVEL ====================
// Một đợt (wave) gồm nhiều nhóm quái sinh ra nối tiếp nhau.
// gap: khoảng cách giữa 2 con trong nhóm (giây).
// isBoss: nhóm này là boss (chỉ 1 con, khổng lồ, máu gấp 10).
data class SpawnGroup(val type: String, val count: Int, val gap: Float = 1.1f, val isBoss: Boolean = false)
data class LevelDef(
    val number: Int,
    val pathCount: Int, // số đường quái đi vào (1..4)
    val waves: List<List<SpawnGroup>>,
    val chickens: Int = 5,
    val bossType: String? = null, // loại quái làm boss ở màn này (mỗi 10 level mới có)
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
// Mỗi 10 level có 1 boss ở cuối đợt 3 (loại quái mới nhất, khổng lồ).
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
    // Boss mỗi 10 level: con cuối cùng của đợt 3, là loại quái mới nhất.
    val bossType = if (number % 10 == 0) unlocked.last().id else null
    val finalWaves = if (bossType != null) {
        waves.toMutableList().also { list ->
            val last = list.last().toMutableList()
            last += SpawnGroup(bossType, 1, 1.0f, isBoss = true)
            list[list.lastIndex] = last
        }
    } else waves
    return LevelDef(number, pathCount, finalWaves, bossType = bossType)
}

// ==================== BUFF GIỮA CÁC ĐỢT (kiểu roguelike) ====================
// Giữa 2 đợt, người chơi chọn 1 trong 3 buff ngẫu nhiên. Buff chỉ có tác
// dụng trong ván hiện tại, chọn trùng thì cộng dồn.
data class Buff(val id: String, val name: String, val desc: String, val emoji: String)

val BUFF_POOL = listOf(
    Buff("damage", "Sức mạnh +1", "Vũ khí +1 sát thương", "\uD83D\uDCAA"),
    Buff("chicken", "Gà mới", "+1 gà (tối đa 8 con)", "\uD83D\uDC14"),
    Buff("crit", "Mắt diều hâu", "+15% tỉ lệ chí mạng", "\uD83C\uDFAF"),
    Buff("slow", "Bùn lầy", "Tất cả quái chậm đi 10%", "\uD83D\uDFE4"),
    Buff("coins", "Túi vàng", "+30% xu khi giết quái", "\uD83D\uDCB0"),
    Buff("shield", "Khiên tổ", "Mỗi đợt chặn 1 lần quái tới tổ", "\uD83D\uDEE1"),
    Buff("splash", "Đánh lan", "Cú đánh lan sang 1 con gần nhất (50% sát thương)", "\uD83D\uDCA5"),
    Buff("heal", "Hồi gà", "Hồi 1 gà mỗi khi sang đợt mới", "❤️"),
)

fun randomBuffs(rng: Random, count: Int = 3): List<Buff> = BUFF_POOL.shuffled(rng).take(count)

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
