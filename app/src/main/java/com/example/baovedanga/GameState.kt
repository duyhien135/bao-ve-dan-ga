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
    val speedMul: Float = 1f, // hệ số tốc độ riêng của từng con (nhanh/chậm khác nhau)
    var distance: Float = 0f, // quãng đường đã đi dọc đường (px)
    var x: Float = 0f,
    var y: Float = 0f,
    var flashUntilMs: Long = 0, // nhấp nháy trắng khi vừa bị đánh
    var slowUntilMs: Long = 0, // súng nước: chậm 50% tới lúc này
    var frozenUntilMs: Long = 0, // gậy phép: đứng yên tới lúc này
    var burnUntilMs: Long = 0, // kiếm lửa: cháy tới lúc này (mất 1 máu/0,5s)
    var lastBurnTickMs: Long = 0,
    var dashUntilMs: Long = 0, // hành vi dash: đang lao nhanh tới lúc này
    var nextDashAtMs: Long = 0, // lần lao tiếp theo
    val spawnMs: Long = 0, // lúc sinh ra (để tính chu kỳ khiên)
    var carrying: Boolean = false, // khỉ cướp gà: đang cõng gà chạy ngược
    var reversing: Boolean = false, // đang chạy ngược về rừng
    val isMini: Boolean = false, // con nhỏ tách ra khi quái split chết
    val isBoss: Boolean = false, // boss khổng lồ (mỗi 10 level)
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

// Vụ nổ của bom: vòng lửa lan ra.
class ExplosionEffect(
    val x: Float,
    val y: Float,
    val radius: Float,
    val bornMs: Long,
    val lifeMs: Long = 400,
)

// Tia sét của Thor: đường gấp khúc qua các con quái bị giật.
class LightningEffect(
    val points: List<Pair<Float, Float>>,
    val bornMs: Long,
    val lifeMs: Long = 250,
)

// Mũi tên của cung: đường thẳng xuyên qua các con quái.
class ArrowEffect(
    val x0: Float,
    val y0: Float,
    val x1: Float,
    val y1: Float,
    val bornMs: Long,
    val lifeMs: Long = 200,
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
    private val playSound: (String) -> Unit = {},
) {
    val monsters = mutableStateListOf<Monster>()
    val dying = mutableStateListOf<DyingMonster>()
    val effects = mutableStateListOf<HitEffect>()
    val swings = mutableStateListOf<SwingEffect>()
    val projectiles = mutableStateListOf<Projectile>()
    val splashes = mutableStateListOf<SplashEffect>()
    val explosions = mutableStateListOf<ExplosionEffect>()
    val lightnings = mutableStateListOf<LightningEffect>()
    val arrows = mutableStateListOf<ArrowEffect>()

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
    // Combo: chạm trúng liên tiếp, chạm trượt thì mất. Combo càng cao tỉ lệ
    // chí mạng càng lớn.
    var combo by mutableStateOf(0)
        private set
    var comboBest by mutableStateOf(0)
        private set
    // Chọn buff giữa các đợt: khác null thì game đứng yên chờ người chơi chọn.
    var buffChoices by mutableStateOf<List<Buff>?>(null)
        private set

    // Buff đã chọn trong ván này (cộng dồn nếu chọn trùng).
    var damageBonus by mutableStateOf(0)
        private set
    var critBonus = 0f
        private set
    var coinMul = 1f
        private set
    var slowAll = 1f
        private set
    var shieldSaves = 0
        private set
    var shieldUsedThisWave = 0
        private set
    var splashHits = 0
        private set
    var healPerWave = 0
        private set

    // Đường đi đã đổi sang px (GameScreen gọi configure khi biết kích thước).
    // Đường sinh ngẫu nhiên mỗi trận (khác nhau hoàn toàn, không cố định).
    private val pathDefs = randomPathSet(Random(System.currentTimeMillis()), level.pathCount)
    var paths: List<ScaledPath> = emptyList()
        private set
    var nestX: Float = 0f
        private set
    var nestY: Float = 0f
        private set

    private var nextMonsterId = 0
    private var configured = false
    private var spawnQueue: ArrayDeque<Triple<String, Float, Boolean>> = ArrayDeque() // (loại quái, chờ bao lâu rồi sinh, có phải boss)
    private var spawnTimer = 0.5f // chờ nửa giây rồi mới sinh con đầu tiên
    private val random = Random(System.currentTimeMillis())

    // Quái càng lên level càng trâu (tối đa gấp 5) và chạy nhanh hơn (tối đa gấp 3).
    private val hpFactor: Float = minOf(1f + 0.08f * (level.number - 1), 5f)
    private val speedFactor: Float = minOf(1f + 0.06f * (level.number - 1), 3.5f)

    fun monsterRadiusPx(m: Monster): Float {
        var r = 26f * density * m.type.radiusScale
        if (m.isBoss) r *= 1.8f
        if (m.isMini) r *= 0.55f
        return r
    }

    // Khiên của gấu/cá sấu/tê giác: 2 giây mở, 2 giây đóng, lặp lại.
    fun isShieldUp(m: Monster, now: Long): Boolean {
        if (m.type.behavior != "shield") return false
        return ((now - m.spawnMs) / 2000) % 2 == 0L
    }

    fun bossMonster(): Monster? = monsters.firstOrNull { it.isBoss }

    fun configure(widthPx: Float, heightPx: Float) {
        paths = pathDefs.map { pts ->
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
            repeat(group.count) { spawnQueue.add(Triple(group.type, group.gap, group.isBoss)) }
        }
        spawnTimer = 0.5f
        bannerText = "Đợt ${index + 1}/${level.waves.size}"
        bannerUntilMs = System.currentTimeMillis() + 2000
        playSound("growl")
    }

    // Người chơi chọn 1 buff giữa các đợt: áp dụng rồi sang đợt tiếp theo.
    fun pickBuff(buff: Buff) {
        when (buff.id) {
            "damage" -> damageBonus += 1
            "chicken" -> chickens = minOf(chickens + 1, 8)
            "crit" -> critBonus += 0.15f
            "slow" -> slowAll *= 0.9f
            "coins" -> coinMul *= 1.3f
            "shield" -> shieldSaves += 1
            "splash" -> splashHits += 1
            "heal" -> healPerWave += 1
        }
        playSound("buff")
        buffChoices = null
        shieldUsedThisWave = 0
        if (healPerWave > 0) chickens = minOf(chickens + healPerWave, 8)
        waveIndex++
        loadWave(waveIndex)
    }

    fun pause() {
        if (status == GameStatus.PLAYING) status = GameStatus.PAUSED
    }

    fun resume() {
        if (status == GameStatus.PAUSED) status = GameStatus.PLAYING
    }

    // Thua khi hết gà VÀ không còn khỉ nào đang cõng gà (vẫn còn cơ hội cứu).
    private fun checkLost(): Boolean {
        if (chickens <= 0 && monsters.none { it.carrying }) {
            chickens = 0
            status = GameStatus.LOST
            playSound("lose")
            return true
        }
        return false
    }

    // Vòng lặp game, gọi mỗi khung hình với dt = giây từ khung trước.
    fun update(dt: Float) {
        if (status != GameStatus.PLAYING) return
        if (buffChoices != null) return // đang chọn buff: game đứng yên
        if (paths.isEmpty()) return // chưa biết kích thước màn hình thì chưa chạy
        frame++
        val now = System.currentTimeMillis()

        // Sinh quái theo hàng đợi.
        if (spawnQueue.isNotEmpty()) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                val (typeId, gap, isBoss) = spawnQueue.removeFirst()
                spawnMonster(typeId, isBoss)
                spawnTimer = gap
            }
        }

        // Quái đang cháy: mất 1 máu mỗi 0,5 giây.
        val burning = monsters.toList().filter { now < it.burnUntilMs && now - it.lastBurnTickMs >= 500 }
        for (m in burning) {
            m.lastBurnTickMs = now
            damageMonster(m, 1, showShield = false)
        }

        // Di chuyển quái dọc đường.
        val arrived = mutableListOf<Monster>()
        for (m in monsters) {
            val path = paths.getOrNull(m.pathIndex) ?: continue
            // Hành vi dash: thỉnh thoảng lao nhanh gấp 2,5 lần trong 1 giây.
            if (m.type.behavior == "dash" && now >= m.nextDashAtMs && now >= m.dashUntilMs) {
                m.dashUntilMs = now + 1000
                m.nextDashAtMs = now + 4000 + random.nextLong(4000)
            }
            var mul = m.speedMul * slowAll
            if (now < m.slowUntilMs) mul *= 0.5f
            if (now < m.frozenUntilMs) mul = 0f
            if (now < m.dashUntilMs) mul *= 2.5f
            if (m.carrying) mul *= 0.8f // cõng gà chạy chậm hơn
            val step = m.type.speedDp * density * speedFactor * mul * dt
            m.distance += if (m.reversing) -step else step
            val (x, y) = path.pointAt(m.distance)
            m.x = x
            m.y = y
            if (!m.reversing && m.distance >= path.totalLen) arrived.add(m)
        }
        // Quái tới tổ.
        for (m in arrived) {
            monsters.remove(m)
            if (m.type.behavior == "thief" && !m.reversing) {
                // Khỉ cướp gà rồi chạy ngược về rừng: giết nó trên đường về thì cứu được gà.
                chickens--
                m.carrying = true
                m.reversing = true
                m.distance = paths[m.pathIndex].totalLen
                monsters.add(m)
                addEffect(nestX, nestY - 60f * density, "🐒 Cướp gà!", 0xFFD32F2F, big = true)
                playSound("chicken")
                if (checkLost()) return
                continue
            }
            // Khiên tổ (buff): chặn 1 lần quái tới tổ mỗi đợt, quái chết luôn.
            if (shieldSaves > shieldUsedThisWave) {
                shieldUsedThisWave++
                addEffect(nestX, nestY - 60f * density, "🛡 Khiên tổ chặn!", 0xFF4FC3F7, big = true)
                playSound("shield")
                continue
            }
            chickens--
            addEffect(nestX, nestY - 60f * density, "-1 \uD83D\uDC14", 0xFFD32F2F, big = true)
            playSound("chicken")
            if (checkLost()) return
        }
        // Khỉ cõng gà chạy thoát khỏi mép đường: mất gà luôn.
        val escaped = monsters.toList().filter { it.reversing && it.distance <= 0f }
        for (m in escaped) {
            monsters.remove(m)
            addEffect(m.x, m.y, "Gà bị cướp mất!", 0xFFD32F2F, big = true)
            if (checkLost()) return
        }

        // Hết đợt: mở chọn buff rồi sang đợt tiếp, hoặc thắng nếu hết đợt.
        if (spawnQueue.isEmpty() && monsters.isEmpty()) {
            if (waveIndex >= level.waves.size - 1) {
                status = GameStatus.WON
                playSound("win")
                return
            }
            if (buffChoices == null) {
                buffChoices = randomBuffs(random)
            }
            return
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
        explosions.removeAll { now - it.bornMs > it.lifeMs }
        lightnings.removeAll { now - it.bornMs > it.lifeMs }
        arrows.removeAll { now - it.bornMs > it.lifeMs }
    }

    private fun spawnMonster(typeId: String, isBoss: Boolean = false) {
        val type = monsterTypeById(typeId)
        val pathIndex = random.nextInt(level.pathCount)
        val path = paths.getOrNull(pathIndex) ?: return
        val (x, y) = path.pointAt(0f)
        val now = System.currentTimeMillis()
        var scaledHp = (type.hp * hpFactor).roundToInt()
        var coins = type.coins + level.number / 2
        // Mỗi con tốc độ khác nhau (0,85-1,25 lần) và càng về đợt sau càng nhanh.
        val waveSpeed = minOf(1f + 0.18f * waveIndex, 2.4f)
        var speedMul = (0.85f + random.nextFloat() * 0.4f) * waveSpeed
        if (isBoss) {
            // Boss: máu gấp 10, xu gấp 20, chạy chậm hơn (0,7 lần).
            scaledHp *= 10
            coins *= 20
            speedMul *= 0.7f
            bannerText = "👑 BOSS XUẤT HIỆN!"
            bannerUntilMs = now + 2500
            playSound("growl")
        }
        monsters.add(
            Monster(
                id = nextMonsterId++,
                type = type,
                pathIndex = pathIndex,
                hp = scaledHp,
                maxHp = scaledHp,
                coins = coins,
                speedMul = speedMul,
                x = x,
                y = y,
                spawnMs = now,
                nextDashAtMs = now + 3000 + random.nextLong(4000),
                isBoss = isBoss,
            )
        )
    }

    // Gây sát thương lên 1 con quái (khiên có thể chặn). Chết thì xử lý
    // giết quái (xu, tách con nhỏ, cứu gà nếu nó đang cõng gà).
    private fun damageMonster(m: Monster, dmg: Int, showShield: Boolean = true) {
        if (m.hp <= 0) return
        val now = System.currentTimeMillis()
        if (isShieldUp(m, now)) {
            if (showShield) {
                addEffect(m.x, m.y - 30f * density, "🛡", 0xFF4FC3F7)
                playSound("shield")
            }
            return
        }
        m.hp -= dmg
        m.flashUntilMs = now + 120
        if (m.hp <= 0) killMonster(m)
    }

    private fun killMonster(m: Monster) {
        monsters.remove(m)
        kills++
        val coins = (m.coins * coinMul).roundToInt()
        coinsEarned += coins
        dying.add(DyingMonster(m.type, m.x, m.y, monsterRadiusPx(m), System.currentTimeMillis()))
        addEffect(m.x, m.y, "+$coins xu", 0xFFFFD54F, big = true)
        playSound("die")
        // Giết khỉ đang cõng gà: cứu được gà.
        if (m.carrying) {
            chickens = minOf(chickens + 1, 8)
            addEffect(m.x, m.y - 40f * density, "Cứu được gà! 🐔", 0xFF66BB6A, big = true)
            playSound("buff")
        }
        // Quái split chết: tách thành nhiều con nhỏ (con nhỏ không tách nữa).
        if (m.type.splitCount > 0 && !m.isMini) {
            val now = System.currentTimeMillis()
            repeat(m.type.splitCount) { i ->
                monsters.add(
                    Monster(
                        id = nextMonsterId++,
                        type = m.type,
                        pathIndex = m.pathIndex,
                        hp = 1,
                        maxHp = 1,
                        coins = 1,
                        speedMul = m.speedMul * 1.1f,
                        distance = maxOf(0f, m.distance - i * 12f * density),
                        x = m.x,
                        y = m.y,
                        spawnMs = now,
                        nextDashAtMs = now + 3000 + random.nextLong(4000),
                        isMini = true,
                    )
                )
            }
        }
    }

    private fun weaponSound(id: String): String = when (id) {
        "hammer" -> "thud"
        "axe", "sword", "trident" -> "slash"
        "thor" -> "zap"
        "bow" -> "arrow"
        "watergun" -> "water"
        "bomb" -> "boom"
        "fireblade" -> "fire"
        "wand" -> "magic"
        else -> "hit"
    }

    // Chạm vào quái: gây sát thương theo vũ khí đang dùng + hiệu ứng riêng.
    // Trả về true nếu đánh trúng con quái nào đó. Gọi riêng cho từng ngón tay
    // (chạm đa điểm) nên nhiều ngón chạm cùng lúc vẫn đánh trúng nhiều con.
    // Chạm trượt (không trúng con nào) thì mất combo.
    fun tap(x: Float, y: Float): Boolean {
        if (status != GameStatus.PLAYING) return false
        if (buffChoices != null) return false
        val now = System.currentTimeMillis()
        var best: Monster? = null
        var bestDist = -1f
        for (m in monsters) {
            val r = monsterRadiusPx(m) + 10f * density
            val dx = m.x - x
            val dy = m.y - y
            if (dx * dx + dy * dy <= r * r && m.distance > bestDist) {
                best = m
                bestDist = m.distance
            }
        }
        val m = best ?: run {
            combo = 0 // chạm trượt: mất combo
            return false
        }
        // Combo + chí mạng: combo càng cao tỉ lệ chí mạng càng lớn.
        combo++
        if (combo > comboBest) comboBest = combo
        if (combo == 20 || combo == 50 || combo == 100) {
            val bonus = combo / 2
            coinsEarned += bonus
            addEffect(m.x, m.y - 40f * density, "Combo x$combo! +$bonus xu", 0xFFFFD54F, big = true)
            playSound("coin")
        }
        val critChance = minOf(0.05f + combo * 0.02f + critBonus, 0.6f)
        val isCrit = random.nextFloat() < critChance
        var dmg = weapon.damage + damageBonus
        if (isCrit) {
            dmg *= 2
            addEffect(m.x, m.y - 50f * density, "💥 CHÍ MẠNG!", 0xFFFF7043, big = true)
            playSound("crit")
        }
        playSound(weaponSound(weapon.id))
        // Hoạt ảnh vũ khí gõ xuống tại điểm chạm (súng nước thì bắn đạn thay vào đó).
        if (weapon.id == "watergun") {
            projectiles.add(
                Projectile(x, y + 110f * density, m.x, m.y, now)
            )
        } else {
            swings.add(SwingEffect(x, y, weapon, now))
        }

        if (weapon.id == "bomb") {
            // Bom: nổ lan diện rộng, tất cả quái trong bán kính 100dp đều dính.
            val radius = 100f * density
            explosions.add(ExplosionEffect(m.x, m.y, radius, now))
            for (other in monsters.toList()) {
                val dx = other.x - m.x
                val dy = other.y - m.y
                if (dx * dx + dy * dy <= radius * radius) {
                    damageMonster(other, dmg)
                }
            }
            return true
        }

        damageMonster(m, dmg)
        when (weapon.id) {
            "watergun" -> m.slowUntilMs = now + 3000 // chậm 50% trong 3 giây
            "fireblade" -> {
                m.burnUntilMs = now + 3000
                m.lastBurnTickMs = now
            }
            "wand" -> if (random.nextFloat() < 0.2f) m.frozenUntilMs = now + 1500
            "thor" -> {
                // Giật điện lan sang tối đa 3 con gần nhất (50% sát thương).
                val chain = monsters.toList()
                    .filter { it != m && it.hp > 0 }
                    .sortedBy { val dx = it.x - m.x; val dy = it.y - m.y; dx * dx + dy * dy }
                    .take(3)
                    .filter {
                        val dx = it.x - m.x
                        val dy = it.y - m.y
                        dx * dx + dy * dy <= (140f * density) * (140f * density)
                    }
                if (chain.isNotEmpty()) {
                    // Đường sét gấp khúc từ con trúng đòn qua từng con bị giật.
                    val pts = mutableListOf(m.x to m.y)
                    var px = m.x
                    var py = m.y
                    for (c in chain) {
                        val midX = (px + c.x) / 2f + (random.nextFloat() - 0.5f) * 30f * density
                        val midY = (py + c.y) / 2f + (random.nextFloat() - 0.5f) * 30f * density
                        pts += midX to midY
                        pts += c.x to c.y
                        px = c.x
                        py = c.y
                    }
                    lightnings.add(LightningEffect(pts, now))
                    for (c in chain) damageMonster(c, maxOf(1, dmg / 2))
                }
            }
            "bow" -> {
                // Xuyên: trúng thêm tối đa 2 con trên cùng đường, phía sau con trúng đòn.
                val pierced = monsters.toList()
                    .filter { it != m && it.hp > 0 && it.pathIndex == m.pathIndex && it.distance < m.distance && m.distance - it.distance <= 120f * density }
                    .sortedByDescending { it.distance }
                    .take(2)
                if (pierced.isNotEmpty()) {
                    val last = pierced.last()
                    arrows.add(ArrowEffect(m.x, m.y, last.x, last.y, now))
                    for (p in pierced) damageMonster(p, dmg)
                }
            }
            "trident" -> {
                // Đâm xuyên thêm 1 con gần nhất.
                val near = monsters.toList()
                    .filter { it != m && it.hp > 0 }
                    .minByOrNull { val dx = it.x - m.x; val dy = it.y - m.y; dx * dx + dy * dy }
                if (near != null) {
                    val dx = near.x - m.x
                    val dy = near.y - m.y
                    if (dx * dx + dy * dy <= (60f * density) * (60f * density)) {
                        damageMonster(near, dmg)
                    }
                }
            }
        }
        // Buff đánh lan: mỗi cú đánh lan sang 1 con gần nhất (50% sát thương).
        if (splashHits > 0) {
            val near = monsters.toList()
                .filter { it != m && it.hp > 0 }
                .minByOrNull { val dx = it.x - m.x; val dy = it.y - m.y; dx * dx + dy * dy }
            if (near != null) {
                val dx = near.x - m.x
                val dy = near.y - m.y
                if (dx * dx + dy * dy <= (70f * density) * (70f * density)) {
                    damageMonster(near, maxOf(1, dmg / 2))
                }
            }
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
