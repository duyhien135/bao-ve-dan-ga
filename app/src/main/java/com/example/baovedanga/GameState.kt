package com.example.baovedanga

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

enum class GameStatus { PLAYING, PAUSED, WON, LOST }

// Một con quái đang đi trên đường.
class Monster(
    val id: Int,
    val type: MonsterType,
    val pathIndex: Int,
    var hp: Int,
    val maxHp: Int, // máu tối đa (đã nhân hệ số theo level) để vẽ thanh máu
    val coins: Int, // xu nhận khi giết (đã cộng thưởng theo level)
    var distance: Float = 0f, // quãng đường đã đi dọc đường (px)
    var x: Float = 0f,
    var y: Float = 0f,
    var flashUntilMs: Long = 0, // nhấp nháy trắng khi vừa bị đánh
)

// Quái đã chết: giữ lại một lúc để vẽ mờ dần rồi biến mất.
class DyingMonster(
    val type: MonsterType,
    val x: Float,
    val y: Float,
    val radius: Float,
    val bornMs: Long,
    val lifeMs: Long = 450,
)

// Hiệu ứng chữ nổi (xu khi giết quái, mất gà).
class HitEffect(
    val x: Float,
    val y: Float,
    val text: String,
    val color: Long,
    val bornMs: Long,
    val lifeMs: Long = 800,
    val big: Boolean = false,
)

// Hoạt ảnh vũ khí gõ xuống tại điểm chạm.
class SwingEffect(
    val x: Float,
    val y: Float,
    val weapon: Weapon,
    val bornMs: Long,
    val lifeMs: Long = 180,
)

// Đạn nước của súng nước: bay từ dưới lên tới quái.
class Projectile(
    val startX: Float,
    val startY: Float,
    val targetX: Float,
    val targetY: Float,
    val bornMs: Long,
    val lifeMs: Long = 120,
)

// Vệt nước bắn tung khi đạn tới nơi.
class SplashEffect(
    val x: Float,
    val y: Float,
    val bornMs: Long,
    val lifeMs: Long = 300,
)

// Một đoạn đường đã đổi sang tọa độ px, kèm độ dài tích lũy từng điểm
// để tính vị trí quái theo quãng đường đã đi.
class ScaledPath(points: List<Pair<Float, Float>>) {
    val pts: List<Pair<Float, Float>> = points
    val cumLen: List<Float>
    val totalLen: Float

    init {
        val cum = mutableListOf(0f)
        var total = 0f
        for (i in 1 until points.size) {
            val dx = points[i].first - points[i - 1].first
            val dy = points[i].second - points[i - 1].second
            total += sqrt(dx * dx + dy * dy)
            cum.add(total)
        }
        cumLen = cum
        totalLen = total
    }

    // Vị trí tại quãng đường d dọc đường (nội suy tuyến tính theo đoạn).
    fun pointAt(d: Float): Pair<Float, Float> {
        if (d <= 0f) return pts.first()
        if (d >= totalLen) return pts.last()
        var i = 1
        while (i < cumLen.size && cumLen[i] < d) i++
        val segLen = cumLen[i] - cumLen[i - 1]
        val t = if (segLen > 0f) (d - cumLen[i - 1]) / segLen else 0f
        val (x0, y0) = pts[i - 1]
        val (x1, y1) = pts[i]
        return (x0 + (x1 - x0) * t) to (y0 + (y1 - y0) * t)
    }
}

class GameState(
    val level: LevelDef,
    val weapon: Weapon,
    private val density: Float,
) {
    val monsters = mutableStateListOf<Monster>()
    val dying = mutableStateListOf<DyingMonster>()
    val effects = mutableStateListOf<HitEffect>()
    val swings = mutableStateListOf<SwingEffect>()
    val projectiles = mutableStateListOf<Projectile>()
    val splashes = mutableStateListOf<SplashEffect>()

    var chickens by mutableStateOf(level.chickens)
        private set
    var coinsEarned by mutableStateOf(0)
        private set
    var kills by mutableStateOf(0)
        private set
    var waveIndex by mutableStateOf(0) // đợt hiện tại (0-based)
        private set
    var status by mutableStateOf(GameStatus.PLAYING)
        private set
    var frame by mutableStateOf(0)
        private set
    var bannerText by mutableStateOf("Đợt 1/${level.waves.size}")
        private set
    var bannerUntilMs by mutableStateOf(System.currentTimeMillis() + 2000)
        private set

    // Đường đi đã đổi sang px (GameScreen gọi configure khi biết kích thước).
    var paths: List<ScaledPath> = emptyList()
        private set
    var nestX: Float = 0f
        private set
    var nestY: Float = 0f
        private set

    private var nextMonsterId = 0
    private var configured = false
    private var spawnQueue: ArrayDeque<Pair<String, Float>> = ArrayDeque() // (loại quái, chờ bao lâu rồi sinh)
    private var spawnTimer = 0.5f // chờ nửa giây rồi mới sinh con đầu tiên
    private var waveBreakTimer = 0f // thời gian nghỉ giữa 2 đợt
    private val random = Random(System.currentTimeMillis())

    // Quái càng lên level càng trâu (tối đa gấp 5) và chạy nhanh hơn (tối đa gấp 2).
    private val hpFactor: Float = minOf(1f + 0.08f * (level.number - 1), 5f)
    private val speedFactor: Float = minOf(1f + 0.03f * (level.number - 1), 2f)

    fun monsterRadiusPx(type: MonsterType): Float = 26f * density * type.radiusScale

    fun configure(widthPx: Float, heightPx: Float) {
        paths = PATH_POINTS.map { pts ->
            ScaledPath(pts.map { (nx, ny) -> nx * widthPx to ny * heightPx })
        }
        nestX = NEST_X * widthPx
        nestY = NEST_Y * heightPx
        if (!configured) {
            configured = true
            loadWave(0)
        }
    }

    private fun loadWave(index: Int) {
        spawnQueue.clear()
        for (group in level.waves[index]) {
            repeat(group.count) { spawnQueue.add(group.type to group.gap) }
        }
        spawnTimer = 0.5f
        bannerText = "Đợt ${index + 1}/${level.waves.size}"
        bannerUntilMs = System.currentTimeMillis() + 2000
    }

    fun pause() {
        if (status == GameStatus.PLAYING) status = GameStatus.PAUSED
    }

    fun resume() {
        if (status == GameStatus.PAUSED) status = GameStatus.PLAYING
    }

    // Vòng lặp game, gọi mỗi khung hình với dt = giây từ khung trước.
    fun update(dt: Float) {
        if (status != GameStatus.PLAYING) return
        if (paths.isEmpty()) return // chưa biết kích thước màn hình thì chưa chạy
        frame++
        val now = System.currentTimeMillis()

        // Sinh quái theo hàng đợi.
        if (spawnQueue.isNotEmpty()) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                val (typeId, gap) = spawnQueue.removeFirst()
                spawnMonster(typeId)
                spawnTimer = gap
            }
        }

        // Di chuyển quái dọc đường.
        val arrived = mutableListOf<Monster>()
        for (m in monsters) {
            val path = paths.getOrNull(m.pathIndex) ?: continue
            m.distance += m.type.speedDp * density * speedFactor * dt
            val (x, y) = path.pointAt(m.distance)
            m.x = x
            m.y = y
            if (m.distance >= path.totalLen) arrived.add(m)
        }
        // Quái tới tổ: ăn 1 con gà rồi biến mất.
        for (m in arrived) {
            monsters.remove(m)
            chickens--
            addEffect(nestX, nestY - 60f * density, "-1 \uD83D\uDC14", 0xFFD32F2F, big = true)
            if (chickens <= 0) {
                chickens = 0
                status = GameStatus.LOST
                return
            }
        }

        // Hết đợt: nghỉ 2.5s rồi sang đợt tiếp, hoặc thắng nếu hết đợt.
        if (spawnQueue.isEmpty() && monsters.isEmpty()) {
            if (waveIndex >= level.waves.size - 1) {
                status = GameStatus.WON
                return
            }
            if (waveBreakTimer <= 0f) waveBreakTimer = 2.5f
            waveBreakTimer -= dt
            if (waveBreakTimer <= 0f) {
                waveIndex++
                loadWave(waveIndex)
            }
        }

        // Đạn nước tới nơi thì bắn tung vệt nước.
        val arrivedShots = projectiles.filter { now - it.bornMs > it.lifeMs }
        for (p in arrivedShots) {
            projectiles.remove(p)
            splashes.add(SplashEffect(p.targetX, p.targetY, now))
        }

        // Dọn hiệu ứng hết hạn.
        effects.removeAll { now - it.bornMs > it.lifeMs }
        swings.removeAll { now - it.bornMs > it.lifeMs }
        dying.removeAll { now - it.bornMs > it.lifeMs }
        splashes.removeAll { now - it.bornMs > it.lifeMs }
    }

    private fun spawnMonster(typeId: String) {
        val type = monsterTypeById(typeId)
        val pathIndex = random.nextInt(level.pathCount)
        val path = paths.getOrNull(pathIndex) ?: return
        val (x, y) = path.pointAt(0f)
        val scaledHp = (type.hp * hpFactor).roundToInt()
        monsters.add(
            Monster(
                id = nextMonsterId++,
                type = type,
                pathIndex = pathIndex,
                hp = scaledHp,
                maxHp = scaledHp,
                coins = type.coins + level.number / 2,
                x = x,
                y = y,
            )
        )
    }

    // Chạm vào quái: gây sát thương theo vũ khí đang dùng.
    // Trả về true nếu đánh trúng con quái nào đó. Gọi riêng cho từng ngón tay
    // (chạm đa điểm) nên nhiều ngón chạm cùng lúc vẫn đánh trúng nhiều con.
    fun tap(x: Float, y: Float): Boolean {
        if (status != GameStatus.PLAYING) return false
        val now = System.currentTimeMillis()
        var best: Monster? = null
        var bestDist = -1f
        for (m in monsters) {
            val r = monsterRadiusPx(m.type) + 10f * density
            val dx = m.x - x
            val dy = m.y - y
            if (dx * dx + dy * dy <= r * r && m.distance > bestDist) {
                best = m
                bestDist = m.distance
            }
        }
        val m = best ?: return false
        m.hp -= weapon.damage
        m.flashUntilMs = now + 120
        // Hoạt ảnh vũ khí gõ xuống tại điểm chạm (súng nước thì bắn đạn thay vào đó).
        if (weapon.id == "watergun") {
            projectiles.add(
                Projectile(x, y + 110f * density, m.x, m.y, now)
            )
        } else {
            swings.add(SwingEffect(x, y, weapon, now))
        }
        if (m.hp <= 0) {
            monsters.remove(m)
            kills++
            coinsEarned += m.coins
            dying.add(DyingMonster(m.type, m.x, m.y, monsterRadiusPx(m.type), now))
            addEffect(m.x, m.y, "+${m.coins} xu", 0xFFFFD54F, big = true)
        }
        return true
    }

    private fun addEffect(
        x: Float,
        y: Float,
        text: String,
        color: Long,
        lifeMs: Long = 800,
        big: Boolean = false,
    ) {
        effects.add(HitEffect(x, y, text, color, System.currentTimeMillis(), lifeMs, big))
    }
}
