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
// state: "walk" (đang đi), "bite" (dừng lại cắn rào), "climb" (khỉ đang leo
// qua rào), "burrow" (rắn đang chui dưới đất), "dead" (cáo giả chết).
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
    var frozenUntilMs: Long = 0, // gậy phép / gà kêu: đứng yên tới lúc này
    var burnUntilMs: Long = 0, // kiếm lửa: cháy tới lúc này (mất 1 máu/0,5s)
    var lastBurnTickMs: Long = 0,
    var dashUntilMs: Long = 0, // hành vi dash: đang lao nhanh tới lúc này
    var nextDashAtMs: Long = 0, // lần lao tiếp theo
    val spawnMs: Long = 0, // lúc sinh ra (để tính chu kỳ khiên)
    val isMini: Boolean = false, // con nhỏ tách ra khi quái split chết / boss triệu hồi
    val isBoss: Boolean = false, // boss khổng lồ (mỗi 10 level)
    val isBig: Boolean = false, // cáo to giữa đợt (máu gấp 5, chạy chậm)
    val isMiniBoss: Boolean = false, // cáo to đợt cuối (máu gấp 8)
    var state: String = "walk",
    var biteAtMs: Long = 0, // lần cắn rào tiếp theo
    var climbUntilMs: Long = 0, // leo qua rào xong lúc này
    var burrowUntilMs: Long = 0, // đang chui dưới đất tới lúc này
    var nextBurrowAtMs: Long = 0, // lần chui tiếp theo
    var stuckUntilMs: Long = 0, // sập hố: đứng yên tới lúc này
    var poisonUntilMs: Long = 0, // trúng độc tới lúc này
    var lastPoisonTickMs: Long = 0,
    var passedGate: Boolean = false, // đã qua cổng rào (cắn vỡ / leo / chui)
    var nextSummonAtMs: Long = 0, // boss triệu hồi quái con lần tiếp theo
    var lastSpikeTickMs: Long = 0, // lần dính gai gần nhất
    val biteOff: Float = 0f, // độ lệch ngang khi cắn rào (để quái không chồng nhau)
    var playDeadAt: Float = -1f, // quãng đường sẽ giả chết (-1 = không giả chết)
    var playedDead: Boolean = false,
    var deadUntilMs: Long = 0, // giả chết tới lúc này
    var weakOX: Float = 0f, // điểm yếu: độ lệch x so với tâm quái (px)
    var weakOY: Float = 0f, // điểm yếu: độ lệch y so với tâm quái (px)
    var weakUntilMs: Long = 0, // điểm yếu đổi vị trí lúc này
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

// Cổng rào: mỗi đường có 1 cổng ở chỗ đường cắt qua hàng rào. Quái tới cổng
// thì dừng lại cắn; rào vỡ thì quái tràn vào tổ. Sửa rào tốn xu trong ván.
class Gate(
    val pathIndex: Int,
    val x: Float,
    val y: Float,
    val gateDistance: Float, // quãng đường dọc đường tới cổng
    var hp: Int,
    var maxHp: Int,
    var lastHitMs: Long = 0, // lúc bị cắn gần nhất (để rung cổng)
) {
    val broken: Boolean get() = hp <= 0
}

// Bẫy người chơi đặt vào góc cua trên đường.
class Trap(
    val type: TrapType,
    val x: Float,
    val y: Float,
    var cooldownUntilMs: Long = 0, // hố sập: hồi lại sau khi sập
)

// Xu rơi ra đất khi quái chết: chạm để nhặt trong 4 giây, không nhặt thì mất.
class CoinDrop(
    val x: Float,
    val y: Float,
    val value: Int,
    val bornMs: Long,
    val lifeMs: Long = 4000,
)

// Một con quái chờ sinh ra.
private data class SpawnItem(
    val typeId: String,
    val gap: Float,
    val isBoss: Boolean,
    val isBig: Boolean,
    val isMiniBoss: Boolean,
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

    // Quãng đường dọc đường tới điểm (x, y) nằm trên đường; -1 nếu không
    // tìm thấy (đường chỉ đi ngang/dọc nên kiểm tra theo từng đoạn).
    fun distanceAtPoint(x: Float, y: Float): Float {
        for (i in 1 until pts.size) {
            val (x0, y0) = pts[i - 1]
            val (x1, y1) = pts[i]
            if (y0 == y1 && kotlin.math.abs(y - y0) < 1f) {
                val lo = minOf(x0, x1)
                val hi = maxOf(x0, x1)
                if (x in lo..hi) return cumLen[i - 1] + kotlin.math.abs(x - x0)
            }
            if (x0 == x1 && kotlin.math.abs(x - x0) < 1f) {
                val lo = minOf(y0, y1)
                val hi = maxOf(y0, y1)
                if (y in lo..hi) return cumLen[i - 1] + kotlin.math.abs(y - y0)
            }
        }
        return -1f
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
    val gates = mutableStateListOf<Gate>()
    val traps = mutableStateListOf<Trap>()
    val coinDrops = mutableStateListOf<CoinDrop>()

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
    // chí mạng càng lớn và xu nhận được càng nhiều (x2 từ combo 15, x3 từ 30...).
    var combo by mutableStateOf(0)
        private set
    var comboBest by mutableStateOf(0)
        private set
    // Chọn buff giữa các đợt: khác null thì game đứng yên chờ người chơi chọn.
    var buffChoices by mutableStateOf<List<Buff>?>(null)
        private set
    // Kỹ năng gà kêu: 1 lần mỗi ván, quái đứng yên 3 giây.
    var skillUsed by mutableStateOf(false)
        private set

    // Hệ số nhân xu theo combo: 1 + combo/15, tối đa x5.
    fun comboCoinMul(): Int = minOf(1 + combo / 15, 5)

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
    var eggStacks = 0
        private set
    var gateMaxHpMul = 1f
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
    // Điểm đặt bẫy: các góc cua trên đường (tọa độ px).
    var trapSpots: List<Pair<Float, Float>> = emptyList()
        private set

    private var nextMonsterId = 0
    private var configured = false
    private var spawnQueue: ArrayDeque<SpawnItem> = ArrayDeque()
    private var spawnTimer = 0.5f // chờ nửa giây rồi mới sinh con đầu tiên
    private val random = Random(System.currentTimeMillis())
    private var lastBiteSoundMs = 0L

    // Quái càng lên level càng trâu (tối đa gấp 5) và chạy nhanh hơn (tối đa gấp 3,5).
    private val hpFactor: Float = minOf(1f + 0.08f * (level.number - 1), 5f)
    private val speedFactor: Float = minOf(1f + 0.06f * (level.number - 1), 3.5f)

    fun monsterRadiusPx(m: Monster): Float {
        var r = 26f * density * m.type.radiusScale
        if (m.isBoss) r *= 1.8f
        if (m.isMini) r *= 0.55f
        if (m.isBig) r *= 1.4f
        if (m.isMiniBoss) r *= 1.6f
        return r
    }

    // Khiên của gấu/cá sấu/tê giác: 2 giây mở, 2 giây đóng, lặp lại.
    fun isShieldUp(m: Monster, now: Long): Boolean {
        if (m.type.behavior != "shield") return false
        return ((now - m.spawnMs) / 2000) % 2 == 0L
    }

    fun bossMonster(): Monster? = monsters.firstOrNull { it.isBoss }

    fun gateForPath(pathIndex: Int): Gate? = gates.firstOrNull { it.pathIndex == pathIndex }

    // Vị trí nút sửa rào của 1 cổng (vẽ phía trên cổng).
    fun repairButtonPos(gate: Gate): Pair<Float, Float> = gate.x to (gate.y - 68f * density)

    fun configure(widthPx: Float, heightPx: Float) {
        paths = pathDefs.map { pts ->
            ScaledPath(pts.map { (nx, ny) -> nx * widthPx to ny * heightPx })
        }
        nestX = NEST_X * widthPx
        nestY = NEST_Y * heightPx
        if (gates.isEmpty()) {
            val baseHp = gateMaxHp(level.number)
            for (i in 0 until level.pathCount) {
                val (gx, gy) = gatePointForPath(i)
                val px = gx * widthPx
                val py = gy * heightPx
                val path = paths.getOrNull(i)
                var gd = path?.distanceAtPoint(px, py) ?: -1f
                if (gd < 0f) gd = (path?.totalLen ?: 0f) * 0.85f
                val maxHp = (baseHp * gateMaxHpMul).roundToInt()
                gates.add(Gate(i, px, py, gd, maxHp, maxHp))
            }
            // Điểm đặt bẫy: các điểm giữa của mỗi đường (bỏ điểm đầu ngoài
            // màn hình và điểm cuối ở tổ).
            val spots = mutableListOf<Pair<Float, Float>>()
            for (p in paths) {
                for (i in 1 until p.pts.size - 1) spots += p.pts[i]
            }
            trapSpots = spots
        }
        if (!configured) {
            configured = true
            loadWave(0)
        }
    }

    private fun loadWave(index: Int) {
        spawnQueue.clear()
        for (group in level.waves[index]) {
            repeat(group.count) {
                spawnQueue.add(SpawnItem(group.type, group.gap, group.isBoss, group.isBig, group.isMiniBoss))
            }
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
            "layxu" -> eggStacks += 1
            "fence" -> {
                gateMaxHpMul *= 1.5f
                val baseHp = gateMaxHp(level.number)
                for (g in gates) {
                    g.maxHp = (baseHp * gateMaxHpMul).roundToInt()
                    g.hp = g.maxHp
                }
            }
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

    // Kỹ năng gà kêu: 1 lần mỗi ván, tất cả quái đứng yên 3 giây.
    fun useChickenSkill(): Boolean {
        if (skillUsed || status != GameStatus.PLAYING || buffChoices != null) return false
        skillUsed = true
        val now = System.currentTimeMillis()
        for (m in monsters) m.frozenUntilMs = now + 3000
        bannerText = "🐔 GÀ KÊU! Quái đứng yên 3 giây!"
        bannerUntilMs = now + 2000
        playSound("chicken")
        return true
    }

    // Trừ xu trong ván (mua bẫy, sửa rào). Trả về false nếu không đủ xu.
    private fun spendCoins(amount: Int): Boolean {
        if (coinsEarned < amount) return false
        coinsEarned -= amount
        return true
    }

    // Đặt bẫy vào điểm góc cua gần nhất (trong 44dp). Trả về false nếu xa
    // điểm đặt bẫy quá, chỗ đó có bẫy rồi, hoặc không đủ xu.
    fun tryPlaceTrapAt(x: Float, y: Float, type: TrapType): Boolean {
        if (status != GameStatus.PLAYING || buffChoices != null) return false
        var best: Pair<Float, Float>? = null
        var bestD = Float.MAX_VALUE
        for (s in trapSpots) {
            val dx = s.first - x
            val dy = s.second - y
            val d2 = dx * dx + dy * dy
            if (d2 < bestD) {
                bestD = d2
                best = s
            }
        }
        val spot = best ?: return false
        val maxD = 44f * density
        if (bestD > maxD * maxD) {
            addEffect(x, y, "Đặt bẫy vào góc cua!", 0xFFFFD54F)
            return false
        }
        val occupied = traps.any { t ->
            val dx = t.x - spot.first
            val dy = t.y - spot.second
            dx * dx + dy * dy < (40f * density) * (40f * density)
        }
        if (occupied) {
            addEffect(spot.first, spot.second, "Chỗ này có bẫy rồi!", 0xFFFFD54F)
            return false
        }
        if (!spendCoins(type.price)) {
            addEffect(spot.first, spot.second, "Không đủ xu!", 0xFFEF5350)
            return false
        }
        traps.add(Trap(type, spot.first, spot.second))
        addEffect(spot.first, spot.second - 20f * density, "Đã đặt ${type.name}!", 0xFF66BB6A, big = true)
        playSound("click")
        return true
    }

    // Chạm vào nút sửa rào (vòng tròn 🔧 phía trên cổng bị hư). Trả về true
    // nếu chạm trúng nút (dù đủ xu hay không).
    fun tryRepairAt(x: Float, y: Float): Boolean {
        if (status != GameStatus.PLAYING || buffChoices != null) return false
        for (g in gates) {
            if (g.hp >= g.maxHp) continue
            val (bx, by) = repairButtonPos(g)
            val dx = x - bx
            val dy = y - by
            val r = 24f * density
            if (dx * dx + dy * dy <= r * r) {
                val cost = repairCost(level.number)
                if (!spendCoins(cost)) {
                    addEffect(bx, by - 20f * density, "Không đủ xu sửa rào!", 0xFFEF5350)
                    return true
                }
                g.hp = g.maxHp
                addEffect(g.x, g.y - 40f * density, "Đã sửa rào! 🔧", 0xFF66BB6A, big = true)
                playSound("coin")
                return true
            }
        }
        return false
    }

    private fun checkLost(): Boolean {
        if (chickens <= 0) {
            chickens = 0
            status = GameStatus.LOST
            playSound("lose")
            return true
        }
        return false
    }

    // Đổi vị trí điểm yếu của quái (ngẫu nhiên trong vòng tròn quái, 2,5s/lần).
    private fun randomizeWeakSpot(m: Monster, now: Long) {
        val r = monsterRadiusPx(m)
        m.weakOX = (random.nextFloat() - 0.5f) * r
        m.weakOY = (random.nextFloat() - 0.5f) * r
        m.weakUntilMs = now + 2500
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
                val item = spawnQueue.removeFirst()
                spawnMonster(item.typeId, item.isBoss, item.isBig, item.isMiniBoss)
                spawnTimer = item.gap
            }
        }

        // Quái đang cháy: mất 1 máu mỗi 0,5 giây.
        val burning = monsters.toList().filter { now < it.burnUntilMs && now - it.lastBurnTickMs >= 500 }
        for (m in burning) {
            m.lastBurnTickMs = now
            damageMonster(m, 1, showShield = false)
        }
        // Quái trúng độc: mất 1 máu mỗi 1 giây.
        val poisoned = monsters.toList().filter { now < it.poisonUntilMs && now - it.lastPoisonTickMs >= 1000 }
        for (m in poisoned) {
            m.lastPoisonTickMs = now
            damageMonster(m, 1, showShield = false)
        }

        // Bẫy gai: quái đứng trên bẫy mất 1 máu mỗi 0,6s.
        for (t in traps.filter { it.type.id == "spikes" }) {
            for (m in monsters.toList()) {
                if (m.state == "burrow") continue
                val dx = m.x - t.x
                val dy = m.y - t.y
                val r = 45f * density
                if (dx * dx + dy * dy <= r * r && now - m.lastSpikeTickMs >= 600) {
                    m.lastSpikeTickMs = now
                    damageMonster(m, 1, showShield = false)
                }
            }
        }
        // Hố sập: quái đầu tiên đi qua thì sập hố 3s, hố hồi sau 6s.
        for (t in traps.filter { it.type.id == "pit" && now >= it.cooldownUntilMs }) {
            val victim = monsters.firstOrNull { m ->
                m.state == "walk" && now >= m.stuckUntilMs &&
                    run {
                        val dx = m.x - t.x
                        val dy = m.y - t.y
                        dx * dx + dy * dy <= (30f * density) * (30f * density)
                    }
            }
            if (victim != null) {
                victim.stuckUntilMs = now + 3000
                t.cooldownUntilMs = now + 6000
                addEffect(t.x, t.y - 20f * density, "Sập hố! 🕳️", 0xFF8D6E63, big = true)
                playSound("thud")
            }
        }
        // Bã độc: quái đi qua bị độc 3s.
        for (t in traps.filter { it.type.id == "poison" }) {
            for (m in monsters) {
                if (m.state == "burrow") continue
                val dx = m.x - t.x
                val dy = m.y - t.y
                val r = 50f * density
                if (dx * dx + dy * dy <= r * r && now >= m.poisonUntilMs) {
                    m.poisonUntilMs = now + 3000
                    m.lastPoisonTickMs = now
                }
            }
        }

        // Di chuyển quái dọc đường.
        val arrived = mutableListOf<Monster>()
        for (m in monsters) {
            val path = paths.getOrNull(m.pathIndex) ?: continue
            val gate = gateForPath(m.pathIndex)
            // Điểm yếu đổi vị trí mỗi 2,5s.
            if (now >= m.weakUntilMs) randomizeWeakSpot(m, now)
            // Cáo giả chết: nằm im 2,5s rồi chạy tiếp.
            if (m.state == "dead") {
                if (now >= m.deadUntilMs) m.state = "walk"
                continue
            }
            // Khỉ leo rào: đứng yên ở cổng 1,5s rồi qua rào.
            if (m.state == "climb") {
                if (now >= m.climbUntilMs) {
                    m.state = "walk"
                    m.passedGate = true
                    val g = gate
                    if (g != null) m.distance = g.gateDistance + 40f * density
                }
                continue
            }
            // Rắn chui đất: hết thời gian thì trồi lên.
            if (m.state == "burrow" && now >= m.burrowUntilMs) m.state = "walk"
            // Hành vi dash: thỉnh thoảng lao nhanh gấp 2,5 lần trong 1 giây.
            if (m.type.behavior == "dash" && now >= m.nextDashAtMs && now >= m.dashUntilMs) {
                m.dashUntilMs = now + 1000
                m.nextDashAtMs = now + 4000 + random.nextLong(4000)
            }
            // Boss triệu hồi 2 quái con mỗi 5 giây.
            if (m.isBoss && now >= m.nextSummonAtMs && monsters.size < 60) {
                m.nextSummonAtMs = now + 5000
                repeat(2) { i ->
                    monsters.add(
                        Monster(
                            id = nextMonsterId++,
                            type = m.type,
                            pathIndex = m.pathIndex,
                            hp = 1,
                            maxHp = 1,
                            coins = 1,
                            speedMul = m.speedMul,
                            distance = maxOf(0f, m.distance - (i + 1) * 20f * density),
                            x = m.x,
                            y = m.y,
                            spawnMs = now,
                            nextDashAtMs = now + 3000 + random.nextLong(4000),
                            isMini = true,
                            passedGate = m.passedGate,
                        )
                    )
                }
                addEffect(m.x, m.y - 50f * density, "👑 Triệu hồi!", 0xFFD32F2F, big = true)
            }
            var mul = m.speedMul * slowAll
            if (now < m.slowUntilMs) mul *= 0.5f
            if (now < m.frozenUntilMs) mul = 0f
            if (now < m.stuckUntilMs) mul = 0f
            if (now < m.poisonUntilMs) mul *= 0.75f
            if (now < m.dashUntilMs) mul *= 2.5f
            val step = m.type.speedDp * density * speedFactor * mul * dt
            // Quái đang cắn rào: đứng yên tại cổng.
            if (m.state == "bite") {
                if (gate != null) {
                    if (m.pathIndex <= 1) {
                        m.x = gate.x
                        m.y = gate.y + m.biteOff
                    } else {
                        m.x = gate.x + m.biteOff
                        m.y = gate.y
                    }
                    // Cắn rào mỗi 0,8s (boss cắn mạnh gấp 3).
                    if (now >= m.biteAtMs) {
                        m.biteAtMs = now + 800
                        gate.hp -= if (m.isBoss) 3 else 1
                        gate.lastHitMs = now
                        if (now - lastBiteSoundMs > 400) {
                            lastBiteSoundMs = now
                            playSound("thud")
                        }
                        if (gate.hp <= 0) {
                            gate.hp = 0
                            addEffect(gate.x, gate.y - 40f * density, "Rào vỡ! 💥", 0xFFD32F2F, big = true)
                            playSound("boom")
                            // Quái đang cắn cổng này tràn vào.
                            for (other in monsters) {
                                if (other.pathIndex == gate.pathIndex && other.state == "bite") {
                                    other.state = "walk"
                                    other.passedGate = true
                                }
                            }
                        }
                    }
                }
                continue
            }
            m.distance += step
            val (x, y) = path.pointAt(m.distance)
            m.x = x
            m.y = y
            // Rắn chui đất: thỉnh thoảng chui xuống 1,8s (không đánh được,
            // đi xuyên rào), chỉ chui khi chưa qua cổng.
            if (m.type.behavior == "burrow" && m.state == "walk" && !m.passedGate &&
                m.distance > 80f * density && now >= m.nextBurrowAtMs
            ) {
                m.state = "burrow"
                m.burrowUntilMs = now + 1800
                m.nextBurrowAtMs = now + 4500 + random.nextLong(3000)
            }
            // Cáo giả chết: tới điểm đã định thì nằm im 2,5s.
            if (m.type.id == "fox" && !m.isMini && !m.isBoss && !m.isBig && !m.isMiniBoss &&
                m.state == "walk" && !m.playedDead && m.playDeadAt > 0f && m.distance >= m.playDeadAt
            ) {
                m.state = "dead"
                m.playedDead = true
                m.deadUntilMs = now + 2500
                continue
            }
            // Tới cổng rào: rắn chui thì đi xuyên, khỉ thì leo, còn lại cắn rào.
            if (!m.passedGate && gate != null && m.distance >= gate.gateDistance) {
                if (gate.broken) {
                    m.passedGate = true
                } else if (m.state == "burrow") {
                    m.passedGate = true
                } else if (m.type.behavior == "climb") {
                    m.state = "climb"
                    m.climbUntilMs = now + 1500
                    m.distance = gate.gateDistance
                    if (m.pathIndex <= 1) {
                        m.x = gate.x
                        m.y = gate.y
                    } else {
                        m.x = gate.x
                        m.y = gate.y
                    }
                } else {
                    m.state = "bite"
                    m.distance = gate.gateDistance
                    m.biteAtMs = now + 400
                }
            }
            if (m.distance >= path.totalLen) arrived.add(m)
        }
        // Quái tới tổ.
        for (m in arrived) {
            monsters.remove(m)
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

        // Hết đợt: mở chọn buff rồi sang đợt tiếp, hoặc thắng nếu hết đợt.
        if (spawnQueue.isEmpty() && monsters.isEmpty()) {
            if (waveIndex >= level.waves.size - 1) {
                status = GameStatus.WON
                playSound("win")
                return
            }
            if (buffChoices == null) {
                // Gà đẻ trứng vàng: mỗi gà cho +5 xu mỗi đợt.
                if (eggStacks > 0) {
                    val bonus = chickens * 5 * eggStacks
                    coinsEarned += bonus
                    addEffect(nestX, nestY - 60f * density, "Gà đẻ trứng: +$bonus xu 🥚", 0xFFFFD54F, big = true)
                }
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
        coinDrops.removeAll { now - it.bornMs > it.lifeMs }
    }

    private fun spawnMonster(typeId: String, isBoss: Boolean = false, isBig: Boolean = false, isMiniBoss: Boolean = false) {
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
        if (isBig) {
            // Cáo to: máu gấp 5, xu gấp 5, chạy chậm hơn (0,6 lần).
            scaledHp *= 5
            coins *= 5
            speedMul *= 0.6f
        }
        if (isMiniBoss) {
            // Mini boss đợt cuối: máu gấp 8, xu gấp 10, chạy chậm hơn (0,5 lần).
            scaledHp *= 8
            coins *= 10
            speedMul *= 0.5f
            bannerText = "😤 CÁO TO XUẤT HIỆN!"
            bannerUntilMs = now + 2500
            playSound("growl")
        }
        val monster = Monster(
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
            nextBurrowAtMs = now + 3000 + random.nextLong(3000),
            nextSummonAtMs = now + 5000,
            isBoss = isBoss,
            isBig = isBig,
            isMiniBoss = isMiniBoss,
            biteOff = (random.nextFloat() - 0.5f) * 56f * density,
        )
        randomizeWeakSpot(monster, now)
        // 20% cáo giả chết 1 lần giữa đường (trước khi tới cổng rào).
        if (type.id == "fox" && random.nextFloat() < 0.2f) {
            val gate = gateForPath(pathIndex)
            if (gate != null) {
                monster.playDeadAt = gate.gateDistance * (0.25f + random.nextFloat() * 0.4f)
            }
        }
        monsters.add(monster)
    }

    // Gây sát thương lên 1 con quái (khiên có thể chặn). Chết thì xử lý
    // giết quái (rơi xu, tách con nhỏ).
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
        // Xu rơi ra đất: chia thành 1-3 đồng (boss 5 đồng), chạm để nhặt.
        val value = (m.coins * coinMul).roundToInt() * comboCoinMul()
        val n = if (m.isBoss) 5 else when {
            value <= 6 -> 1
            value <= 15 -> 2
            else -> 3
        }
        val now = System.currentTimeMillis()
        val each = maxOf(1, value / n)
        repeat(n) { i ->
            val v = if (i == 0) value - each * (n - 1) else each
            if (v > 0) {
                coinDrops.add(
                    CoinDrop(
                        m.x + (random.nextFloat() - 0.5f) * 48f * density,
                        m.y + (random.nextFloat() - 0.5f) * 48f * density,
                        v,
                        now,
                    )
                )
            }
        }
        dying.add(DyingMonster(m.type, m.x, m.y, monsterRadiusPx(m), now))
        playSound("die")
        // Quái split chết: tách thành nhiều con nhỏ (con nhỏ không tách nữa).
        if (m.type.splitCount > 0 && !m.isMini) {
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
                        passedGate = m.passedGate,
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

    // Chạm vào màn hình: ưu tiên nhặt xu rơi, rồi đánh quái. Trả về true nếu
    // chạm trúng cái gì đó. Chạm trượt (không trúng gì) thì mất combo.
    // Chạm vào cáo giả chết cũng mất combo. Rắn đang chui thì không đánh được.
    fun tap(x: Float, y: Float): Boolean {
        if (status != GameStatus.PLAYING) return false
        if (buffChoices != null) return false
        val now = System.currentTimeMillis()
        // Nhặt xu rơi (không ảnh hưởng combo).
        val coin = coinDrops.firstOrNull { c ->
            val dx = c.x - x
            val dy = c.y - y
            dx * dx + dy * dy <= (26f * density) * (26f * density)
        }
        if (coin != null) {
            coinDrops.remove(coin)
            coinsEarned += coin.value
            addEffect(coin.x, coin.y - 10f * density, "+${coin.value} xu", 0xFFFFD54F)
            playSound("coin")
            return true
        }
        // Chạm vào cáo giả chết: mất combo, không gây sát thương.
        val faker = monsters.firstOrNull { m ->
            if (m.state != "dead") return@firstOrNull false
            val r = monsterRadiusPx(m) + 10f * density
            val dx = m.x - x
            val dy = m.y - y
            dx * dx + dy * dy <= r * r
        }
        if (faker != null) {
            combo = 0
            addEffect(faker.x, faker.y - 40f * density, "Giả chết! Mất combo 😤", 0xFFEF5350, big = true)
            playSound("thud")
            return false
        }
        var best: Monster? = null
        var bestDist = -1f
        for (m in monsters) {
            if (m.state == "burrow") continue // rắn chui dưới đất: không đánh được
            if (m.state == "dead") continue
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
        // Chạm trúng điểm yếu: chắc chắn chí mạng + thưởng xu ngay.
        val weakDx = x - (m.x + m.weakOX)
        val weakDy = y - (m.y + m.weakOY)
        val weakR = 12f * density
        val hitWeak = weakDx * weakDx + weakDy * weakDy <= weakR * weakR
        val critChance = minOf(0.05f + combo * 0.02f + critBonus, 0.6f)
        val isCrit = hitWeak || random.nextFloat() < critChance
        var dmg = weapon.damage + damageBonus
        if (isCrit) {
            dmg *= 2
            addEffect(m.x, m.y - 50f * density, "💥 CHÍ MẠNG!", 0xFFFF7043, big = true)
            playSound("crit")
        }
        if (hitWeak) {
            val bonus = maxOf(2, m.coins / 2)
            coinsEarned += bonus
            addEffect(m.x, m.y - 70f * density, "Điểm yếu! +$bonus xu", 0xFFFFD54F, big = true)
            randomizeWeakSpot(m, now)
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
                if (other.state == "burrow") continue
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
                    .filter { it != m && it.hp > 0 && it.state != "burrow" }
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
                    .filter { it != m && it.hp > 0 && it.state != "burrow" && it.pathIndex == m.pathIndex && it.distance < m.distance && m.distance - it.distance <= 120f * density }
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
                    .filter { it != m && it.hp > 0 && it.state != "burrow" }
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
                .filter { it != m && it.hp > 0 && it.state != "burrow" }
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
