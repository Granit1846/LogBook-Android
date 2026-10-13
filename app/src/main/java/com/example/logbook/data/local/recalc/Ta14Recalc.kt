package com.example.logbook.data.local.recalc

import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.Ta14Entity
import com.example.logbook.data.local.entity.Ta14StartEntity
import java.time.LocalDate
import java.time.YearMonth

/**
 * Расчёты формуляра «Та-14».
 *
 * Порт из C# MainViewModel.Ta14.txt:
 *  - RecalculateTa14Records
 *  - EnsureMonthlyTotalsTa14 + AddMonthlyTotalTa14 + BuildMonthlyTotalTa14
 *  - RecalculateTa14MonthlyTotals
 *
 * Особенности:
 *  - LaunchCount = Count(vsu > 0)
 *  - Δналёт = Σ vsu
 *  - ColVremya = Σ vsu
 *  - ColSNE += Δ, ColSNE2 += Δ
 *  - Col7 += Δ (если col7Active), Col8 += Δzapuski (если col8Active)
 *  - LaunchCount2 += Δzapuski
 */
class Ta14Recalc(
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
        val start = db.ta14StartDao().getByProfile(profileId) ?: return

        val all = db.ta14Dao().getByProfile(profileId)
        val regular = all
            .filter { !it.isTotal }
            .sortedBy { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }

        val col7Active = start.col7 != 0
        val col8Active = start.col8 != 0

        var prevSNE = start.colSNE
        var prevSNE2 = start.colSNE2
        var prevCol7 = start.col7
        var prevCol8 = start.col8
        var prevLaunch2 = start.launchCount2

        val updated = ArrayList<Ta14Entity>(regular.size)

        for (rec in regular) {
            if (rec.isHistorical) {
                prevSNE = rec.colSNE; prevSNE2 = rec.colSNE2; prevCol7 = rec.col7; prevCol8 = rec.col8; prevLaunch2 = rec.launchCount2
                continue
            }
            val date = RecalcCommon.parseDate(rec.date)
            val day = if (date != null) db.dayDao().getByDate(profileId, rec.date) else null
            val launches = if (day != null) db.launchDao().getByDay(day.id) else emptyList()

            val deltaNarabotka: Int
            val deltaZapuski: Int
            val daySumVsu: Int
            val dayLaunchCount: Int

            if (day != null && launches.isNotEmpty()) {
                var sumVsu = 0
                var count = 0

                for (l in launches) {
                    if (l.vsu > 0) {
                        sumVsu += l.vsu
                        count++
                    }
                }

                deltaNarabotka = sumVsu
                deltaZapuski = count
                daySumVsu = sumVsu
                dayLaunchCount = count
            } else {
                // Ручной ввод
                deltaNarabotka = rec.colVremya
                deltaZapuski = rec.launchCount
                daySumVsu = rec.colVremya
                dayLaunchCount = rec.launchCount
            }

            // Накопительные
            prevSNE += deltaNarabotka
            val newSNE = prevSNE

            prevSNE2 += deltaNarabotka
            val newSNE2 = prevSNE2

            if (col7Active) {
                prevCol7 += deltaNarabotka
            }
            val newCol7 = if (col7Active) prevCol7 else 0

            if (col8Active) {
                prevCol8 += deltaZapuski
            }
            val newCol8 = if (col8Active) prevCol8 else 0

            prevLaunch2 += deltaZapuski
            val newLaunch2 = prevLaunch2

            updated += rec.copy(
                launchCount = dayLaunchCount,
                colVremya = daySumVsu,
                colSNE = newSNE,
                colSNE2 = newSNE2,
                col7 = newCol7,
                col8 = newCol8,
                launchCount2 = newLaunch2
            )
        }

        for (u in updated) db.ta14Dao().update(u)
    }

    private suspend fun ensureMonthlyTotalsInternal() {
        val start = db.ta14StartDao().getByProfile(profileId) ?: return
        val all = db.ta14Dao().getByProfile(profileId)

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
            db.ta14Dao().insert(
                total.copy(sortOrder = RecalcCommon.totalSortOrder(ym))
            )
        }
    }

    private suspend fun recalculateMonthlyTotalsInternal(historicalDate: String? = null) {
        val start = db.ta14StartDao().getByProfile(profileId) ?: return
        val all = db.ta14Dao().getByProfile(profileId)

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
                db.ta14Dao().delete(t)
                continue
            }
            // Archived rows retain their imported totals until a day in this month is removed.
            val deletedMonth = RecalcCommon.parseDate(historicalDate)?.let { YearMonth.of(it.year, it.monthValue) }
            if (monthRecords.all { it.isHistorical } && deletedMonth != ymObj) continue
            val rebuilt = buildMonthlyTotal(start, all, monthRecords, ymObj)
            db.ta14Dao().update(
                t.copy(
                    sortOrder = RecalcCommon.totalSortOrder(ymObj),
                    launchCount = rebuilt.launchCount,
                    colVremya = rebuilt.colVremya,
                    colSNE = rebuilt.colSNE,
                    col7 = rebuilt.col7,
                    col8 = rebuilt.col8,
                    colSNE2 = rebuilt.colSNE2,
                    launchCount2 = rebuilt.launchCount2,
                    needsManualRecalc = rebuilt.needsManualRecalc
                )
            )
        }
    }

    // ============================================================
    // Помощники
    // ============================================================

    private fun monthlyTotalExists(all: List<Ta14Entity>, ym: YearMonth): Boolean {
        return all.any { t ->
            t.isTotal && RecalcCommon.parseMonthKey(t.totalMonth)?.let { (y, m) ->
                y == ym.year && m == ym.monthValue
            } == true
        }
    }

    /**
     * Порт C# BuildMonthlyTotalTa14.
     *
     * Если предыдущего месяца нет:
     *   Sum: LaunchCount, ColVremya
     *   ColSNE = ColVremya, ColSNE2 = ColVremya, LaunchCount2 = LaunchCount
     *   Col7/Col8 — 0
     *   NeedsManualRecalc = true
     *
     * Если есть:
     *   Sum: LaunchCount, ColVremya
     *   Разница крайних: ColSNE, ColSNE2, Col7, Col8, LaunchCount2
     *   NeedsManualRecalc = false
     */
    private fun buildMonthlyTotal(
        start: Ta14StartEntity,
        allRecords: List<Ta14Entity>,
        monthRecords: List<Ta14Entity>,
        ym: YearMonth
    ): Ta14Entity {
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
        val sumVremya = monthRecords.sumOf { it.colVremya }

        // === Нет предыдущего месяца ===
        if (prevRecords.isEmpty()) {
            return Ta14Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true,
                launchCount = sumLaunch,
                colVremya = sumVremya,
                colSNE = sumVremya,
                colSNE2 = sumVremya,
                launchCount2 = sumLaunch,
                col7 = 0,
                col8 = 0
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
            return Ta14Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true
            )
        }

        val diffSNE = (lastCurrent.colSNE - lastPrev.colSNE).coerceAtLeast(0)
        val diffCol7 = (lastCurrent.col7 - lastPrev.col7).coerceAtLeast(0)
        val diffSNE2 = (lastCurrent.colSNE2 - lastPrev.colSNE2).coerceAtLeast(0)
        val diffCol8 = (lastCurrent.col8 - lastPrev.col8).coerceAtLeast(0)
        val diffLaunch2 = (lastCurrent.launchCount2 - lastPrev.launchCount2).coerceAtLeast(0)

        return Ta14Entity(
            profileId = profileId,
            date = label,
            isTotal = true,
            totalMonth = totalMonth,
            needsManualRecalc = false,
            launchCount = sumLaunch,
            colVremya = sumVremya,
            colSNE = diffSNE,
            colSNE2 = diffSNE2,
            col7 = diffCol7,
            col8 = diffCol8,
            launchCount2 = diffLaunch2
        )
    }
}
