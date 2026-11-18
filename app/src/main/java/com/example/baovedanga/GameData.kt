package com.example.baovedanga

// ==================== VŨ KHÍ ====================
// damage: sát thương mỗi lần chạm. price: giá mua bằng xu (0 = có sẵn).
data class Weapon(val id: String, val name: String, val emoji: String, val damage: Int, val price: Int)

val WEAPONS = listOf(
    Weapon("swatter", "Vỉ ruồi", "\uD83E\uDEB0", 1, 0),
    Weapon("hammer", "Búa", "\uD83D\uDD28", 2, 150),
    Weapon("shovel", "Xẻng", "\uD83E\uDE8F", 3, 400),
    Weapon("axe", "Rìu", "\uD83E\uDE93", 5, 900),
    Weapon("sword", "Kiếm", "⚔️", 8, 2000),
    Weapon("thor", "Búa Thor", "⚡", 12, 4000),
)

fun weaponById(id: String): Weapon = WEAPONS.firstOrNull { it.id == id } ?: WEAPONS[0]

// ==================== QUÁI ====================
// hp: máu. speedDp: tốc độ di chuyển (dp/giây). coins: xu nhận khi giết.
// radiusScale: kích thước vòng tròn quái so với chuẩn. color: màu thân.
data class MonsterType(
    val id: String,
    val name: String,
    val emoji: String,
    val hp: Int,
    val speedDp: Float,
    val coins: Int,
    val radiusScale: Float,
    val color: Long,
)

val MONSTER_TYPES = listOf(
    MonsterType("fox", "Cáo", "\uD83E\uDD8A", 3, 55f, 5, 1.0f, 0xFFFF7043),
    MonsterType("wolf", "Sói", "\uD83D\uDC3A", 6, 70f, 8, 1.05f, 0xFF78909C),
    MonsterType("bear", "Gấu", "\uD83D\uDC3B", 12, 35f, 15, 1.25f, 0xFF8D6E63),
    MonsterType("tiger", "Hổ", "\uD83D\uDC2F", 20, 60f, 25, 1.2f, 0xFFFFA000),
    MonsterType("croc", "Cá sấu", "\uD83D\uDC0A", 30, 40f, 35, 1.3f, 0xFF558B2F),
    MonsterType("elephant", "Voi", "\uD83D\uDC18", 45, 26f, 50, 1.5f, 0xFF90A4AE),
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

val LEVELS = listOf(
    LevelDef(1, 1, listOf(
        listOf(SpawnGroup("fox", 5)),
        listOf(SpawnGroup("fox", 7)),
    )),
    LevelDef(2, 1, listOf(
        listOf(SpawnGroup("fox", 6)),
        listOf(SpawnGroup("fox", 4), SpawnGroup("wolf", 3)),
        listOf(SpawnGroup("fox", 8), SpawnGroup("wolf", 2)),
    )),
    LevelDef(3, 2, listOf(
        listOf(SpawnGroup("fox", 6), SpawnGroup("wolf", 2)),
        listOf(SpawnGroup("wolf", 5)),
        listOf(SpawnGroup("fox", 6), SpawnGroup("bear", 2)),
    )),
    LevelDef(4, 2, listOf(
        listOf(SpawnGroup("wolf", 4), SpawnGroup("bear", 1)),
        listOf(SpawnGroup("fox", 8), SpawnGroup("wolf", 4)),
        listOf(SpawnGroup("bear", 3), SpawnGroup("wolf", 4)),
    )),
    LevelDef(5, 3, listOf(
        listOf(SpawnGroup("wolf", 5), SpawnGroup("bear", 2)),
        listOf(SpawnGroup("tiger", 2), SpawnGroup("fox", 6)),
        listOf(SpawnGroup("bear", 3), SpawnGroup("tiger", 2)),
        listOf(SpawnGroup("wolf", 6), SpawnGroup("bear", 2), SpawnGroup("tiger", 1)),
    )),
    LevelDef(6, 3, listOf(
        listOf(SpawnGroup("tiger", 3), SpawnGroup("wolf", 4)),
        listOf(SpawnGroup("bear", 4), SpawnGroup("tiger", 2)),
        listOf(SpawnGroup("croc", 2), SpawnGroup("wolf", 6)),
        listOf(SpawnGroup("tiger", 3), SpawnGroup("bear", 3)),
    )),
    LevelDef(7, 4, listOf(
        listOf(SpawnGroup("croc", 2), SpawnGroup("tiger", 2)),
        listOf(SpawnGroup("bear", 4), SpawnGroup("wolf", 6)),
        listOf(SpawnGroup("tiger", 4), SpawnGroup("croc", 2)),
        listOf(SpawnGroup("wolf", 8), SpawnGroup("bear", 3), SpawnGroup("tiger", 2)),
    )),
    LevelDef(8, 4, listOf(
        listOf(SpawnGroup("tiger", 3), SpawnGroup("croc", 2)),
        listOf(SpawnGroup("elephant", 1), SpawnGroup("wolf", 6)),
        listOf(SpawnGroup("croc", 3), SpawnGroup("tiger", 3)),
        listOf(SpawnGroup("bear", 4), SpawnGroup("tiger", 3), SpawnGroup("croc", 1)),
        listOf(SpawnGroup("elephant", 1), SpawnGroup("tiger", 2), SpawnGroup("wolf", 6)),
    )),
    LevelDef(9, 4, listOf(
        listOf(SpawnGroup("elephant", 1), SpawnGroup("croc", 2)),
        listOf(SpawnGroup("tiger", 5), SpawnGroup("wolf", 6)),
        listOf(SpawnGroup("croc", 3), SpawnGroup("bear", 4)),
        listOf(SpawnGroup("elephant", 2), SpawnGroup("tiger", 3)),
        listOf(SpawnGroup("croc", 2), SpawnGroup("tiger", 4), SpawnGroup("bear", 3)),
    )),
    LevelDef(10, 4, listOf(
        listOf(SpawnGroup("elephant", 2), SpawnGroup("croc", 2)),
        listOf(SpawnGroup("tiger", 5), SpawnGroup("bear", 4)),
        listOf(SpawnGroup("croc", 4), SpawnGroup("wolf", 8)),
        listOf(SpawnGroup("elephant", 2), SpawnGroup("tiger", 4)),
        listOf(SpawnGroup("elephant", 2), SpawnGroup("croc", 3), SpawnGroup("tiger", 3)),
    )),
)

// Xu thưởng khi thắng level.
fun winBonus(level: Int): Int = 10 + 15 * level

// ==================== ĐƯỜNG QUÁI ĐI ====================
// Tọa độ chuẩn hóa (0..1) theo kích thước vùng chơi: x = ngang, y = dọc.
// 4 đường: trên, trái, phải, dưới — đều chạy từ rừng vào tổ gà ở giữa.
val PATH_POINTS = listOf(
    // Trên
    listOf(0.50f to -0.05f, 0.42f to 0.14f, 0.56f to 0.28f, 0.50f to 0.44f),
    // Trái
    listOf(-0.05f to 0.44f, 0.16f to 0.38f, 0.30f to 0.50f, 0.50f to 0.44f),
    // Phải
    listOf(1.05f to 0.44f, 0.84f to 0.50f, 0.70f to 0.38f, 0.50f to 0.44f),
    // Dưới
    listOf(0.50f to 1.05f, 0.58f to 0.82f, 0.44f to 0.68f, 0.50f to 0.44f),
)

// Tổ gà ở giữa map (tọa độ chuẩn hóa).
const val NEST_X = 0.50f
const val NEST_Y = 0.44f

// Hàng rào (tọa độ chuẩn hóa): hình chữ nhật bo góc quanh vườn rau.
const val FENCE_LEFT = 0.10f
const val FENCE_TOP = 0.26f
const val FENCE_RIGHT = 0.90f
const val FENCE_BOTTOM = 0.62f
