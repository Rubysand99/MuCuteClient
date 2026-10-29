package com.mucheng.mucute.client.game

/**
 * Thống kê đòn đánh: số đòn client đã gửi và số lần server xác nhận mục tiêu bị thương (gói EntityEvent HURT).
 *
 * Đây chỉ là ước lượng:
 *  - Một HURT được tính là "trúng" nếu ta vừa đánh đúng thực thể đó trong [MATCH_WINDOW_MS] gần đây
 *    (bù cho độ trễ của server), nên HURT do người khác gây ra ít khi bị tính nhầm.
 *  - Sau mỗi lần bị thương, thực thể có khoảng miễn thương ngắn nên số lần trúng mỗi giây vào một mục tiêu
 *    bị chặn trên khoảng 2/s dù ta gửi rất nhiều đòn. Vì vậy tỉ lệ trúng/đã gửi thấp là bình thường;
 *    hãy nhìn số trúng mỗi giây khi đổi cps.
 *  - Server không gửi HURT (hoặc chặn đòn mà không báo) thì không đếm được.
 */
object HitStats {

    const val MATCH_WINDOW_MS = 1000L
    private const val CAPACITY = 512

    private val attackTimes = LongArray(CAPACITY)
    private var attackCount = 0
    private var attackHead = 0

    private val hitTimes = LongArray(CAPACITY)
    private var hitCount = 0
    private var hitHead = 0

    private var totalAttacks = 0L
    private var totalHits = 0L

    // runtimeEntityId -> thời điểm ta đánh gần nhất.
    private val lastAttackOn = HashMap<Long, Long>()

    @Synchronized
    fun recordAttack(runtimeEntityId: Long) {
        val now = System.currentTimeMillis()
        attackHead = (attackHead + 1) % CAPACITY
        attackTimes[attackHead] = now
        if (attackCount < CAPACITY) attackCount++
        totalAttacks++

        lastAttackOn[runtimeEntityId] = now
        if (lastAttackOn.size > 64) {
            lastAttackOn.entries.removeIf { now - it.value > MATCH_WINDOW_MS * 2 }
        }
    }

    /** Gọi khi server báo thực thể [runtimeEntityId] bị thương. */
    @Synchronized
    fun onHurt(runtimeEntityId: Long) {
        val now = System.currentTimeMillis()
        val attackedAt = lastAttackOn[runtimeEntityId] ?: return
        if (now - attackedAt > MATCH_WINDOW_MS) return

        hitHead = (hitHead + 1) % CAPACITY
        hitTimes[hitHead] = now
        if (hitCount < CAPACITY) hitCount++
        totalHits++
    }

    /** Số đòn đã gửi trong [windowMs] gần nhất. */
    @Synchronized
    fun attacksIn(windowMs: Long): Int = countRecent(attackTimes, attackCount, attackHead, windowMs)

    /** Số lần trúng (được server xác nhận) trong [windowMs] gần nhất. */
    @Synchronized
    fun hitsIn(windowMs: Long): Int = countRecent(hitTimes, hitCount, hitHead, windowMs)

    @Synchronized
    fun totals(): Pair<Long, Long> = totalAttacks to totalHits

    @Synchronized
    fun reset() {
        attackCount = 0
        hitCount = 0
        totalAttacks = 0
        totalHits = 0
        lastAttackOn.clear()
    }

    private fun countRecent(times: LongArray, count: Int, head: Int, windowMs: Long): Int {
        val now = System.currentTimeMillis()
        var result = 0
        for (i in 0 until count) {
            val time = times[(head - i + CAPACITY) % CAPACITY]
            if (now - time > windowMs) break
            result++
        }
        return result
    }
}
