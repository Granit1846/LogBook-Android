package com.example.logbook.data.local.recalc

import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.Vr80Entity
import com.example.logbook.data.local.entity.Vr80StartEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Расчёты формуляра «ВР-80».
 *
 * Порт из C# MainViewModel.Vr80.txt:
 *  - RecalculateVr80Records
 *  - EnsureMonthlyTotalsVr80 + AddMonthlyTotalVr80 + BuildMonthlyTotalVr80
 *  - RecalculateVr80MonthlyTotals
 *
 * Особенности:
 *  - LaunchCount = Count(zemlya > 0 || tp > 0)
 *  - ΔГР = round(Σ zemlya × 0.2) + Σ vozduh
 *  - ΔТП = Σ tp
 *  - GroundHours = Σ zemlya (полное время, БЕЗ ×0.2)
 *  - Col6 = Σ tp
 *  - Col7 = Σ vozduh
 *  - ColSNE += ΔГР, Col24 += ΔГР (если col24Active)
 *  - Col27 += ΔТП, Col26 += ΔТП (если col26Active)
 */
class Vr80Recalc(
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
        val start = db.vr80StartDao().getByProfile(profileId) ?: return

        val all = db.vr80Dao().getByProfile(profileId)
        val regular = all
            .filter { !it.isTotal }
            .sortedBy { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }

        val col24Active = start.col24 != 0
        val col26Active = start.col26 != 0

        var prevSNE = start.colSNE
        var prevCol24 = start.col24
        var prevCol26 = start.col26
        var prevCol27 = start.col27

        val updated = ArrayList<Vr80Entity>(regular.size)

        for (rec in regular) {
            if (rec.isHistorical) {
                prevSNE = rec.colSNE; prevCol24 = rec.col24; prevCol26 = rec.col26; prevCol27 = rec.col27
                continue
            }
            val date = RecalcCommon.parseDate(rec.date)
            val day = if (date != null) db.dayDao().getByDate(profileId, rec.date) else null
            val launches = if (day != null) db.launchDao().getByDay(day.id) else emptyList()

            val deltaGR: Int
            val deltaTP: Int
            val daySumZemlya: Int
            val daySumVozduh: Int
            val daySumTP: Int
            val dayLaunchCount: Int

            if (day != null && launches.isNotEmpty()) {
                var sumZemlya = 0
                var sumVozduh = 0
                var sumTP = 0
                var launchCount = 0

                for (l in launches) {
                    if (l.tp <= 0) {
                        sumZemlya += l.zemlya
                        sumVozduh += l.vozduh
                    }
                    sumTP += l.tp
                    // LaunchCount = Count(zemlya > 0 || tp > 0)
                    if (l.zemlya > 0 || l.tp > 0) launchCount++
                }

                deltaGR = RecalcCommon.naletMinutes(sumZemlya, sumVozduh)
                deltaTP = sumTP
                daySumZemlya = sumZemlya
                daySumVozduh = sumVozduh
                daySumTP = sumTP
                dayLaunchCount = launchCount
            } else {
                // Ручной ввод: GroundHours — это уже полное время земли (не ×0.2).
                // Для накопительного используем его как есть (C# тоже так: recZemlya),
                // но × 0.2 в дельте.
                val recZemlya = rec.groundHours
                val recVozduh = rec.col7
                val recTP = rec.col6

                deltaGR = RecalcCommon.naletMinutes(recZemlya, recVozduh)
                deltaTP = recTP
                daySumZemlya = recZemlya
                daySumVozduh = recVozduh
                daySumTP = recTP
                dayLaunchCount = rec.launchCount
            }

            // Накопительные
            prevSNE += deltaGR
            val newSNE = prevSNE

            if (col24Active) {
                prevCol24 += deltaGR
            }
            val newCol24 = if (col24Active) prevCol24 else 0

            prevCol27 += deltaTP
            val newCol27 = prevCol27

            if (col26Active) {
                prevCol26 += deltaTP
            }
            val newCol26 = if (col26Active) prevCol26 else 0

            updated += rec.copy(
                launchCount = dayLaunchCount,
                groundHours = daySumZemlya,
                col6 = daySumTP,
                col7 = daySumVozduh,
                colSNE = newSNE,
                col24 = newCol24,
                col26 = newCol26,
                col27 = newCol27
            )
        }

        for (u in updated) db.vr80Dao().update(u)
    }

    private suspend fun ensureMonthlyTotalsInternal() {
        val start = db.vr80StartDao().getByProfile(profileId) ?: return
        val all = db.vr80Dao().getByProfile(profileId)

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
            db.vr80Dao().insert(
                total.copy(sortOrder = RecalcCommon.totalSortOrder(ym))
            )
        }
    }

    private suspend fun recalculateMonthlyTotalsInternal(historicalDate: String? = null) {
        val start = db.vr80StartDao().getByProfile(profileId) ?: return
        val all = db.vr80Dao().getByProfile(profileId)

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
                db.vr80Dao().delete(t)
                continue
            }
            // Archived rows retain their imported totals until a day in this month is removed.
            val deletedMonth = RecalcCommon.parseDate(historicalDate)?.let { YearMonth.of(it.year, it.monthValue) }
            if (monthRecords.all { it.isHistorical } && deletedMonth != ymObj) continue
            val rebuilt = buildMonthlyTotal(start, all, monthRecords, ymObj)
            db.vr80Dao().update(
                t.copy(
                    sortOrder = RecalcCommon.totalSortOrder(ymObj),
                    launchCount = rebuilt.launchCount,
                    groundHours = rebuilt.groundHours,
                    col6 = rebuilt.col6,
                    col7 = rebuilt.col7,
                    col24 = rebuilt.col24,
                    colSNE = rebuilt.colSNE,
                    col26 = rebuilt.col26,
                    col27 = rebuilt.col27,
                    needsManualRecalc = rebuilt.needsManualRecalc
                )
            )
        }
    }

    // ============================================================
    // Помощники
    // ============================================================

    private fun monthlyTotalExists(all: List<Vr80Entity>, ym: YearMonth): Boolean {
        return all.any { t ->
            t.isTotal && RecalcCommon.parseMonthKey(t.totalMonth)?.let { (y, m) ->
                y == ym.year && m == ym.monthValue
            } == true
        }
    }

    /**
     * Порт C# BuildMonthlyTotalVr80.
     *
     * Если предыдущего месяца нет:
     *   Sum: LaunchCount, GroundHours, Col6, Col7
     *   Col24, ColSNE, Col26, Col27 — 0 (нельзя сложить из-за ×0.2)
     *   NeedsManualRecalc = true
     *
     * Если есть:
     *   Sum: LaunchCount, GroundHours, Col6, Col7
     *   Разница крайних: Col24, ColSNE, Col26, Col27
     *   NeedsManualRecalc = false
     */
    private fun buildMonthlyTotal(
        start: Vr80StartEntity,
        allRecords: List<Vr80Entity>,
        monthRecords: List<Vr80Entity>,
        ym: YearMonth
    ): Vr80Entity {
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

        val sumLaunch = monthRecords.sumOf { it.launchCount }
        val sumGround = monthRecords.sumOf { it.groundHours }
        val sumTp = monthRecords.sumOf { it.col6 }
        val sumAir = monthRecords.sumOf { it.col7 }

        // === Нет предыдущего месяца ===
        if (prevRecords.isEmpty()) {
            return Vr80Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true,
                launchCount = sumLaunch,
                groundHours = sumGround,
                col6 = sumTp,
                col7 = sumAir,
                col24 = 0,
                colSNE = 0,
                col26 = 0,
                col27 = 0
            )
        }

        // === Есть предыдущий месяц ===
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
            return Vr80Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true
            )
        }

        val diffCol24 = (lastCurrent.col24 - lastPrev.col24).coerceAtLeast(0)
        val diffSNE = (lastCurrent.colSNE - lastPrev.colSNE).coerceAtLeast(0)
        val diffCol26 = (lastCurrent.col26 - lastPrev.col26).coerceAtLeast(0)
        val diffCol27 = (lastCurrent.col27 - lastPrev.col27).coerceAtLeast(0)

        return Vr80Entity(
            profileId = profileId,
            date = label,
            isTotal = true,
            totalMonth = totalMonth,
            needsManualRecalc = false,
            launchCount = sumLaunch,
            groundHours = sumGround,
            col6 = sumTp,
            col7 = sumAir,
            col24 = diffCol24,
            colSNE = diffSNE,
            col26 = diffCol26,
            col27 = diffCol27
        )
    }
}
