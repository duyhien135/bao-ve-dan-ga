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
// splitCount con nhỏ), burrow (chui xuống đất, không đánh được, đi xuyên
// rào rồi trồi lên gần tổ), climb (leo qua rào thay vì cắn rào).
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
    MonsterType("fox", "Cáo", "\uD83E\uDD8A", 3, 160f, 5, 1.0f, 0xFFFF7043, 1, R.drawable.mon_fox, "dash"),
    MonsterType("snake", "Rắn", "🐍", 8, 240f, 10, 0.9f, 0xFF66BB6A, 6, R.drawable.mon_snake, "burrow"),
    MonsterType("wolf", "Sói", "\uD83D\uDC3A", 6, 200f, 8, 1.05f, 0xFF78909C, 10, R.drawable.mon_wolf),
    MonsterType("spider", "Nhện", "🕷️", 10, 190f, 12, 0.95f, 0xFF5D4037, 15, R.drawable.mon_spider, "split", 3),
    MonsterType("bear", "Gấu", "\uD83D\uDC3B", 12, 105f, 15, 1.25f, 0xFF8D6E63, 20, R.drawable.mon_bear, "shield"),
    MonsterType("monkey", "Khỉ", "🐒", 14, 220f, 18, 1.0f, 0xFFA1887F, 26, R.drawable.mon_monkey, "climb"),
    MonsterType("tiger", "Hổ", "\uD83D\uDC2F", 20, 175f, 25, 1.2f, 0xFFFFA000, 32, R.drawable.mon_tiger, "dash"),
    MonsterType("leopard", "Báo", "🐆", 16, 275f, 20, 1.1f, 0xFFFBC02D, 38, R.drawable.mon_leopard, "dash"),
    MonsterType("croc", "Cá sấu", "\uD83D\uDC0A", 30, 110f, 35, 1.3f, 0xFF558B2F, 45, R.drawable.mon_croc, "shield"),
    MonsterType("lion", "Sư tử", "🦁", 40, 160f, 45, 1.35f, 0xFFEF6C00, 52, R.drawable.mon_lion),
    MonsterType("rhino", "Tê giác", "🦏", 60, 85f, 60, 1.45f, 0xFF78909C, 60, R.drawable.mon_rhino, "shield"),
    MonsterType("elephant", "Voi", "\uD83D\uDC18", 45, 75f, 50, 1.5f, 0xFF90A4AE, 68, R.drawable.mon_elephant),
    MonsterType("dragon", "Rồng", "🐉", 90, 130f, 100, 1.6f, 0xFFC62828, 78, R.drawable.mon_dragon),
)

fun monsterTypeById(id: String): MonsterType = MONSTER_TYPES.firstOrNull { it.id == id } ?: MONSTER_TYPES[0]

// ==================== LEVEL ====================
// Một đợt (wave) gồm nhiều nhóm quái sinh ra nối tiếp nhau.
// gap: khoảng cách giữa 2 con trong nhóm (giây).
// isBoss: nhóm này là boss (chỉ 1 con, khổng lồ, máu gấp 10).
// isBig: cáo to giữa đợt (máu gấp 5, chạy chậm, xu gấp 5).
// isMiniBoss: cáo to ở đợt cuối (máu gấp 8, xu gấp 10).
data class SpawnGroup(val type: String, val count: Int, val gap: Float = 1.1f, val isBoss: Boolean = false, val isBig: Boolean = false, val isMiniBoss: Boolean = false)
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
    val gap = maxOf(0.55f, 1.2f - 0.02f * number)
    // Quái quen (mở được từ 5 level trước) làm chủ lực; quái mới nhất chỉ lác đác vài con.
    val common = unlocked.filter { it.minLevel <= number - 5 }.ifEmpty { unlocked }
    val newest = unlocked.filter { it.minLevel > number - 5 }
    var newestLeft = if (newest.isNotEmpty()) 3 else 0
    val perWaveBase = 40 + number * 3 // quái mỗi đợt, tăng theo level (giới hạn dưới)
    val waves = List(waveCount) { wi ->
        val groups = mutableListOf<SpawnGroup>()
        var remaining = minOf(perWaveBase + rng.nextInt(12), 100)
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
        // Cáo to giữa đợt: đợt thường thì cáo to, đợt cuối thì mini boss.
        val big = if (wi == waveCount - 1) SpawnGroup("fox", 1, gap, isMiniBoss = true)
        else SpawnGroup("fox", 1, gap, isBig = true)
        groups.add(groups.size / 2, big)
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
    Buff("layxu", "Gà đẻ trứng vàng", "Mỗi gà cho +5 xu mỗi đợt", "🥚"),
    Buff("fence", "Rào thép", "Rào +50% máu tối đa và hồi đầy ngay", "🛡️"),
)

fun randomBuffs(rng: Random, count: Int = 3): List<Buff> = BUFF_POOL.shuffled(rng).take(count)

// ==================== BẪY ĐẶT TRÊN ĐƯỜNG ====================
// Người chơi dùng xu trong ván mua bẫy, đặt vào các góc cua trên đường.
// Bẫy ở lại hết ván. Gai gây sát thương liên tục, hố giữ chân quái,
// bã độc làm quái mất máu dần + chậm lại.
data class TrapType(val id: String, val name: String, val emoji: String, val price: Int, val desc: String)

val TRAP_TYPES = listOf(
    TrapType("spikes", "Bẫy gai", "📌", 80, "Gây 1 sát thương mỗi 0,6s cho quái đứng trên bẫy"),
    TrapType("poison", "Bã độc", "☠️", 120, "Quái đi qua bị độc: mất 1 máu/giây trong 3s, chậm 25%"),
    TrapType("pit", "Hố sập", "🕳️", 180, "Quái sập hố, đứng yên 3s (hố hồi lại sau 6s)"),
)

fun trapTypeById(id: String): TrapType = TRAP_TYPES.firstOrNull { it.id == id } ?: TRAP_TYPES[0]

// ==================== CỔNG RÀO ====================
// Mỗi đường có 1 cổng rào ở chỗ đường cắt qua hàng rào. Quái tới cổng thì
// dừng lại cắn rào; rào vỡ thì quái tràn vào tổ. Sửa rào tốn xu trong ván.
fun gateMaxHp(level: Int): Int = 25 + 5 * level
fun repairCost(level: Int): Int = 40 + 10 * level

// Vị trí cổng (tọa độ chuẩn hóa) theo từng đường: trái, phải, trên, dưới.
fun gatePointForPath(pathIndex: Int): Pair<Float, Float> = when (pathIndex) {
    1 -> FENCE_RIGHT to 0.50f
    2 -> 0.50f to FENCE_TOP
    3 -> 0.50f to FENCE_BOTTOM
    else -> FENCE_LEFT to 0.50f
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
    // Đường dài ngoằn ngoèo: nhiều đoạn gấp khúc ngoài rào rồi mới có 1 đoạn
    // thẳng cuối đi qua cổng rào vào tổ. Mỗi đường chỉ cắt rào đúng 1 lần.
    // Đường 0: vào từ trái, tới tổ theo hướng ngang từ trái.
    if (pathCount >= 1) {
        val y0 = randY(rng, 0.12f, 0.88f)
        val x1 = randY(rng, 0.08f, 0.15f)
        val y1 = randYOffCenter(rng)
        val x2 = randY(rng, 0.18f, 0.25f)
        val y2 = randYOffCenter(rng)
        val x3 = randY(rng, 0.27f, 0.31f)
        paths += listOf(
            -0.05f to y0, x1 to y0, x1 to y1, x2 to y1, x2 to y2, x3 to y2, x3 to 0.50f, 0.50f to 0.50f,
        )
    }
    // Đường 1: vào từ phải, tới tổ theo hướng ngang từ phải.
    if (pathCount >= 2) {
        val y0 = randY(rng, 0.12f, 0.88f)
        val x1 = randY(rng, 0.85f, 0.92f)
        val y1 = randYOffCenter(rng)
        val x2 = randY(rng, 0.75f, 0.82f)
        val y2 = randYOffCenter(rng)
        val x3 = randY(rng, 0.69f, 0.73f)
        paths += listOf(
            1.05f to y0, x1 to y0, x1 to y1, x2 to y1, x2 to y2, x3 to y2, x3 to 0.50f, 0.50f to 0.50f,
        )
    }
    // Đường 2: vào từ trái, tới tổ theo hướng dọc từ trên.
    if (pathCount >= 3) {
        val y0 = randY(rng, 0.15f, 0.85f)
        val x1 = randY(rng, 0.08f, 0.15f)
        val y1 = randY(rng, 0.10f, 0.30f)
        val x2 = randY(rng, 0.20f, 0.30f)
        val y2 = randY(rng, 0.08f, 0.13f)
        paths += listOf(
            -0.05f to y0, x1 to y0, x1 to y1, x2 to y1, x2 to y2, 0.50f to y2, 0.50f to 0.50f,
        )
    }
    // Đường 3: vào từ phải, tới tổ theo hướng dọc từ dưới.
    if (pathCount >= 4) {
        val y0 = randY(rng, 0.15f, 0.85f)
        val x1 = randY(rng, 0.85f, 0.92f)
        val y1 = randY(rng, 0.70f, 0.90f)
        val x2 = randY(rng, 0.70f, 0.80f)
        val y2 = randY(rng, 0.87f, 0.92f)
        paths += listOf(
            1.05f to y0, x1 to y0, x1 to y1, x2 to y1, x2 to y2, 0.50f to y2, 0.50f to 0.50f,
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
