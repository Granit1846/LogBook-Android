package com.example.logbook.data.local.recalc

import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.Vk250001Entity
import com.example.logbook.data.local.entity.Vk250001StartEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/**
 * Расчёты формуляра «ВК-2500-01».
 *
 * Порт из C# MainViewModel.Vk2500.txt (RecalculateVkRecords с useLeftSnk = true).
 *
 * Особенности:
 *  - A (colSNE) = prevA + round(rawDelta × CoeffA)   — накопительно, минуты
 *  - I (i) = prevI + Count(zemlya > 0)               — накопительно, штуки
 *  - H (h) = prevH + rawDelta × CoeffH               — накопительно, 1 знак
 *  - A_PPR (pprA) = colSNE - start.pprA              — ППР по A, если pprA != 0
 *  - H_PPR (pprH) = h - start.pprH                   — ППР по H, если pprH != 0
 *  - B (b) = prevB                                    — статичное / СНК
 *  - G (g), E (e) = СНК, если задано, иначе prev      — статичные
 *  - D (d) = 0.0                                      — исключён
 *  - СНК имеет приоритет: если для дня есть значение СНК — берём его,
 *    и оно становится «предыдущим» для следующего дня.
 *  - rawDelta = round(Σ zemlya × 0.2) + Σ vozduh
 */
class Vk250001Recalc(
    private val db: AppDatabase,
    private val profileId: Long
) {

    // ============================================================
    // Публичный API
    // ============================================================

    suspend fun recalculateAll(historicalDate: String? = null) {
        db.withTransaction {
            recalculateInternal()
            ensureMonthlyTotalsInternal()
            recalculateMonthlyTotalsInternal(historicalDate)
        }
    }

    suspend fun recalculate() {
        db.withTransaction { recalculateInternal() }
    }

    suspend fun ensureMonthlyTotals() {
        db.withTransaction { ensureMonthlyTotalsInternal() }
    }

    suspend fun recalculateMonthlyTotals() {
        db.withTransaction { recalculateMonthlyTotalsInternal() }
    }

    // ============================================================
    // Внутренние
    // ============================================================

    private suspend fun recalculateInternal() {
        val start = db.vk250001StartDao().getByProfile(profileId) ?: return

        val all = db.vk250001Dao().getByProfile(profileId)
        val regular = all
            .filter { !it.isTotal }
            .sortedBy { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }

        val coeffA = if (start.coeffA != 0.0) start.coeffA else 1.0
        val coeffH = if (start.coeffH != 0.0) start.coeffH else 1.0

        val aPprActive = start.pprA != 0
        val hPprActive = start.pprH != 0.0
        val aPprBase = start.pprA
        val hPprBase = start.pprH

        // Накопительные от Start
        var prevA: Int = start.colSNE
        var prevI: Int = start.i
        var prevH: Double = start.h
        var prevB: Double = start.b
        var prevE: Double = start.e
        var prevG: Double = start.g

        val updated = ArrayList<Vk250001Entity>(regular.size)

        for (rec in regular) {
            if (rec.isHistorical) {
                prevA = rec.colSNE; prevI = rec.i; prevH = rec.h; prevB = rec.b; prevE = rec.e; prevG = rec.g
                continue
            }
            val date = RecalcCommon.parseDate(rec.date)
            val day = if (date != null) db.dayDao().getByDate(profileId, rec.date) else null
            val launches = if (day != null) db.launchDao().getByDay(day.id) else emptyList()
            val snk = if (day != null) db.snkDao().getByDay(day.id) else null

            // Суммы за день
            var ground = 0
            var air = 0
            var zemlyaLaunches = 0
            for (l in launches) {
                if (l.tp > 0) continue
                ground += l.zemlya
                air += l.vozduh
                if (l.zemlya > 0) zemlyaLaunches++
            }
            val rawDelta = RecalcCommon.naletMinutes(ground, air)

            // ===== A =====
            val newA: Int = when {
                snk?.aLeft?.takeIf { it != 0.0 } != null ->
                    RecalcCommon.roundedProduct(snk.aLeft, 60.0).also { prevA = it }
                day != null -> {
                    val delta = RecalcCommon.roundedProduct(rawDelta.toDouble(), coeffA)
                    (prevA + delta).also { prevA = it }
                }
                else -> rec.colSNE.also { prevA = it }
            }

            // ===== I =====
            val newI: Int = when {
                snk?.iLeft?.takeIf { it != 0.0 } != null ->
                    snk.iLeft.roundToInt().also { prevI = it }
                day != null -> (prevI + zemlyaLaunches).also { prevI = it }
                else -> rec.i.also { prevI = it }
            }

            // ===== H =====
            val newH: Double = when {
                snk?.hLeft?.takeIf { it != 0.0 } != null ->
                    round1(snk.hLeft).also { prevH = it }
                day != null -> RecalcCommon.roundedSumProduct(prevH, rawDelta, coeffH, 1).also { prevH = it }
                else -> rec.h.also { prevH = it }
            }

            // ===== B, E, G =====
            val newB: Double = snk?.bLeft?.takeIf { it != 0.0 }?.let {
                round3(it).also { v -> prevB = v }
            } ?: prevB

            val newE: Double = snk?.eLeft?.takeIf { it != 0.0 }?.let {
                round2(it).also { v -> prevE = v }
            } ?: prevE

            val newG: Double = snk?.gLeft?.takeIf { it != 0.0 }?.let {
                round2(it).also { v -> prevG = v }
            } ?: prevG

            // ===== A_PPR =====
            val newAPpr: Int = if (aPprActive) {
                (newA - aPprBase).coerceAtLeast(0)
            } else 0

            // ===== H_PPR =====
            val newHPpr: Double = if (hPprActive) {
                RecalcCommon.roundedDifference(newH, hPprBase, 1)
            } else 0.0

            updated += rec.copy(
                i = newI,
                g = newG,
                colSNE = newA,
                pprA = newAPpr,
                b = newB,
                d = 0.0,
                e = newE,
                h = newH,
                pprH = newHPpr
            )
        }

        for (u in updated) db.vk250001Dao().update(u)
    }

    private suspend fun ensureMonthlyTotalsInternal() {
        val start = db.vk250001StartDao().getByProfile(profileId) ?: return
        val all = db.vk250001Dao().getByProfile(profileId)

        val months = all.asSequence()
            .filter { !it.isTotal }
            .mapNotNull { RecalcCommon.parseDate(it.date) }
            .map { YearMonth.of(it.year, it.monthValue) }
            .distinct()
            .sorted()
            .toList()

        for (ym in months) {
            if (monthlyTotalExists(all, ym)) continue
            val monthRecords = all.filter { r ->
                !r.isTotal && RecalcCommon.parseDate(r.date)?.let { d ->
                    d.year == ym.year && d.monthValue == ym.monthValue
                } == true
            }
            if (monthRecords.isEmpty()) continue

            val total = buildMonthlyTotal(start, all, monthRecords, ym)
            db.vk250001Dao().insert(
                total.copy(sortOrder = RecalcCommon.totalSortOrder(ym))
            )
        }
    }

    private suspend fun recalculateMonthlyTotalsInternal(historicalDate: String? = null) {
        val start = db.vk250001StartDao().getByProfile(profileId) ?: return
        val all = db.vk250001Dao().getByProfile(profileId)

        val regular = all.filter { !it.isTotal }
            .sortedBy { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }
        val totals = all.filter { it.isTotal }

        for (t in totals) {
            val ym = RecalcCommon.parseMonthKey(t.totalMonth)
            if (ym == null) continue
            val ymObj = YearMonth.of(ym.first, ym.second)
            val monthRecords = regular.filter { r ->
                RecalcCommon.parseDate(r.date)?.let { d ->
                    d.year == ymObj.year && d.monthValue == ymObj.monthValue
                } == true
            }
            if (monthRecords.isEmpty()) {
                db.vk250001Dao().delete(t)
                continue
            }
            // Archived rows retain their imported totals until a day in this month is removed.
            val deletedMonth = RecalcCommon.parseDate(historicalDate)?.let { YearMonth.of(it.year, it.monthValue) }
            if (monthRecords.all { it.isHistorical } && deletedMonth != ymObj) continue
            val rebuilt = buildMonthlyTotal(start, all, monthRecords, ymObj)
            db.vk250001Dao().update(
                t.copy(
                    sortOrder = RecalcCommon.totalSortOrder(ymObj),
                    i = rebuilt.i,
                    g = rebuilt.g,
                    colSNE = rebuilt.colSNE,
                    pprA = rebuilt.pprA,
                    b = rebuilt.b,
                    d = rebuilt.d,
                    e = rebuilt.e,
                    h = rebuilt.h,
                    pprH = rebuilt.pprH,
                    needsManualRecalc = rebuilt.needsManualRecalc
                )
            )
        }
    }

    // ============================================================
    // Помощники
    // ============================================================

    private fun monthlyTotalExists(all: List<Vk250001Entity>, ym: YearMonth): Boolean =
        all.any { t ->
            t.isTotal && RecalcCommon.parseMonthKey(t.totalMonth)?.let { (y, m) ->
                y == ym.year && m == ym.monthValue
            } == true
        }

    /**
     * Порт C# BuildMonthlyTotalVk2500.
     *
     *  - Нет предыдущего месяца: I = sumI; остальное — 0; NeedsManualRecalc = true.
     *  - Есть: разница крайних по всем накопительным; I — разница.
     *  - 0 → 0 (UI покажет "—" через форматирование).
     */
    private fun buildMonthlyTotal(
        start: Vk250001StartEntity,
        allRecords: List<Vk250001Entity>,
        monthRecords: List<Vk250001Entity>,
        ym: YearMonth
    ): Vk250001Entity {
        val label = RecalcCommon.totalLabel()
        val totalMonth = RecalcCommon.monthKey(ym.year, ym.monthValue)
        val startOfMonth = LocalDate.of(ym.year, ym.monthValue, 1)

        val prevRecords = allRecords.asSequence()
            .filter { !it.isTotal }
            .filter {
                val d = RecalcCommon.parseDate(it.date) ?: return@filter false
                d.isBefore(startOfMonth)
            }
            .toList()

        if (prevRecords.isEmpty()) {
            return Vk250001Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true
                // все поля — 0
            )
        }

        val lastPrevDate = prevRecords
            .mapNotNull { RecalcCommon.parseDate(it.date) }
            .maxOrNull() ?: startOfMonth.minusDays(1)
        val prevYM = YearMonth.of(lastPrevDate.year, lastPrevDate.monthValue)

        val lastCurrent = monthRecords.maxByOrNull {
            RecalcCommon.parseDate(it.date) ?: LocalDate.MIN
        }
        val lastPrev = prevRecords
            .filter {
                val d = RecalcCommon.parseDate(it.date) ?: return@filter false
                d.year == prevYM.year && d.monthValue == prevYM.monthValue
            }
            .maxByOrNull { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }

        if (lastCurrent == null || lastPrev == null) {
            return Vk250001Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true
            )
        }

        return Vk250001Entity(
            profileId = profileId,
            date = label,
            isTotal = true,
            totalMonth = totalMonth,
            needsManualRecalc = false,
            i = (lastCurrent.i - lastPrev.i).coerceAtLeast(0),
            g = RecalcCommon.roundedDifference(lastCurrent.g, lastPrev.g, 2),
            colSNE = (lastCurrent.colSNE - lastPrev.colSNE).coerceAtLeast(0),
            pprA = (lastCurrent.pprA - lastPrev.pprA).coerceAtLeast(0),
            b = RecalcCommon.roundedDifference(lastCurrent.b, lastPrev.b, 3),
            d = 0.0,
            e = RecalcCommon.roundedDifference(lastCurrent.e, lastPrev.e, 2),
            h = RecalcCommon.roundedDifference(lastCurrent.h, lastPrev.h, 1),
            pprH = RecalcCommon.roundedDifference(lastCurrent.pprH, lastPrev.pprH, 1)
        )
    }

    private fun round1(v: Double): Double =
        BigDecimal.valueOf(v).setScale(1, RoundingMode.HALF_UP).toDouble()

    private fun round2(v: Double): Double =
        BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toDouble()

    private fun round3(v: Double): Double =
        BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP).toDouble()
}
