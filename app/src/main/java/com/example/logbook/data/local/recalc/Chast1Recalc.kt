package com.example.logbook.data.local.recalc

import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.Chast1Entity
import com.example.logbook.data.local.entity.Chast1StartEntity
import java.time.LocalDate
import java.time.YearMonth
import com.example.logbook.data.local.entity.ProfileEntity

/**
 * Расчёты формуляра «Часть 1».
 *
 * Порт из C# MainViewModel.Chast1.txt:
 *  - RecalculateChast1Records
 *  - EnsureMonthlyTotals + AddMonthlyTotal + BuildMonthlyTotal
 *  - RecalculateMonthlyTotals
 *
 * Особенности:
 *  - Δналёт = round(Σ zemlya × 0.2) + Σ vozduh   ← единая формула (баг C# исправлен)
 *  - Δверт = Count(vozduh > 0)
 *  - Δсамол = Σ samolet
 *  - Δпосад = Δсамол + Δверт
 *  - Запуски с tp > 0 не дают налёта (по схеме ТП — обрыв цепочки)
 */
class Chast1Recalc(
    private val db: AppDatabase,
    private val profileId: Long
) {

    // ============================================================
    // Публичный API
    // ============================================================

    /** Полный пересчёт: обычные строки → итоги → порядок. Одной транзакцией. */
    suspend fun recalculateAll() {
        db.withTransaction {
            recalculateInternal()
            ensureMonthlyTotalsInternal()
            recalculateMonthlyTotalsInternal()
            refreshFuselageSneInternal()
        }
    }

    /** Пересчёт обычных (не итоговых) строк. */
    suspend fun recalculate() {
        db.withTransaction { recalculateInternal() }
    }

    /** Добавить недостающие итоговые строки. */
    suspend fun ensureMonthlyTotals() {
        db.withTransaction { ensureMonthlyTotalsInternal() }
    }

    /** Пересчитать все существующие итоги + пересобрать порядок. */
    suspend fun recalculateMonthlyTotals() {
        db.withTransaction { recalculateMonthlyTotalsInternal() }
    }

    // ============================================================
    // Внутренние (вызываются внутри withTransaction)
    // ============================================================

    private suspend fun recalculateInternal() {
        val start = db.chast1StartDao().getByProfile(profileId) ?: return

        val all = db.chast1Dao().getByProfile(profileId)
        val regular = all
            .filter { !it.isTotal }
            .sortedBy { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }

        val col5Active = start.col5 != 0
        val col7Active = start.col7 != 0

        var prevCol5 = start.col5
        var prevSNE = start.colSNE
        var prevCol7 = start.col7
        var prevLandings = start.landings

        val updated = ArrayList<Chast1Entity>(regular.size)

        for (rec in regular) {
            val date = RecalcCommon.parseDate(rec.date)
            val day = if (date != null) db.dayDao().getByDate(profileId, rec.date) else null
            val launches = if (day != null) db.launchDao().getByDay(day.id) else emptyList()

            val deltaNalet: Int
            val deltaVert: Int
            val deltaSamolet: Int
            val deltaPosad: Int
            val dayGroundMinutes: Int
            val dayAirMinutes: Int

            if (day != null && launches.isNotEmpty()) {
                var ground = 0
                var air = 0
                var vert = 0
                var samolet = 0

                for (l in launches) {
                    // ТП-запуск — обрыв: не даёт ни земли, ни воздуха
                    if (l.tp > 0) continue
                    ground += l.zemlya
                    air += l.vozduh
                    if (l.vozduh > 0) vert++
                    samolet += l.samolet
                }

                val groundMinutes = RecalcCommon.groundMinutesFromZemlya(ground)

                deltaNalet = groundMinutes + air
                deltaVert = vert
                deltaSamolet = samolet
                deltaPosad = samolet + vert
                dayGroundMinutes = groundMinutes
                dayAirMinutes = air
            } else {
                // Нет DayData — берём ручной ввод из самого формуляра.
                // groundHours уже в минутах (после ×0.2), flightDuration — воздух.
                deltaNalet = rec.groundHours + rec.flightDuration
                deltaVert = if (rec.col9 > 0) rec.col9 else rec.landings2
                deltaSamolet = if (rec.col11 > 0) rec.col11 else rec.col8
                deltaPosad = deltaSamolet + deltaVert
                dayGroundMinutes = rec.groundHours
                dayAirMinutes = rec.flightDuration
            }

            var newCol5: Int
            var newSNE: Int
            var newCol7: Int
            var newLandings: Int
            val newCol8: Int
            val newCol9: Int
            val newCol11: Int
            val newLandings2: Int

            if (col5Active) {
                prevCol5 += deltaNalet
                newCol5 = prevCol5
            } else newCol5 = 0

            prevSNE += deltaNalet
            newSNE = prevSNE

            if (col7Active) {
                prevCol7 += deltaPosad
                newCol7 = prevCol7
                newCol8 = deltaSamolet
                newCol9 = deltaVert
            } else {
                newCol7 = 0
                newCol8 = 0
                newCol9 = 0
            }

            prevLandings += deltaPosad
            newLandings = prevLandings

            newCol11 = deltaSamolet
            newLandings2 = deltaVert

            updated += rec.copy(
                pilot = day?.crew ?: rec.pilot,
                groundHours = dayGroundMinutes,
                flightDuration = dayAirMinutes,
                col5 = newCol5,
                colSNE = newSNE,
                col7 = newCol7,
                col8 = newCol8,
                col9 = newCol9,
                landings = newLandings,
                col11 = newCol11,
                landings2 = newLandings2
            )
        }

        for (u in updated) db.chast1Dao().update(u)
    }

    private suspend fun ensureMonthlyTotalsInternal() {
        val start = db.chast1StartDao().getByProfile(profileId) ?: return
        val all = db.chast1Dao().getByProfile(profileId)

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
            db.chast1Dao().insert(
                total.copy(sortOrder = RecalcCommon.totalSortOrder(ym))
            )
        }
    }

    private suspend fun recalculateMonthlyTotalsInternal() {
        val start = db.chast1StartDao().getByProfile(profileId) ?: return
        val all = db.chast1Dao().getByProfile(profileId)

        val regular = all.filter { !it.isTotal }
            .sortedBy { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }
        val totals = all.filter { it.isTotal }

        // Обновляем существующие итоги in-place. Обычные строки не трогаем.
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
                db.chast1Dao().delete(t)
                continue
            }
            val rebuilt = buildMonthlyTotal(start, all, monthRecords, ymObj)
            db.chast1Dao().update(
                t.copy(
                    sortOrder = RecalcCommon.totalSortOrder(ymObj),
                    groundHours = rebuilt.groundHours,
                    flightDuration = rebuilt.flightDuration,
                    col5 = rebuilt.col5,
                    colSNE = rebuilt.colSNE,
                    col7 = rebuilt.col7,
                    col8 = rebuilt.col8,
                    col9 = rebuilt.col9,
                    landings = rebuilt.landings,
                    col11 = rebuilt.col11,
                    landings2 = rebuilt.landings2,
                    needsManualRecalc = rebuilt.needsManualRecalc
                )
            )
        }
    }

    // ============================================================
    // Помощники
    // ============================================================

    private fun monthlyTotalExists(all: List<Chast1Entity>, ym: YearMonth): Boolean {
        return all.any { t ->
            t.isTotal && RecalcCommon.parseMonthKey(t.totalMonth)?.let { (y, m) ->
                y == ym.year && m == ym.monthValue
            } == true
        }
    }

    /**
     * Порт C# BuildMonthlyTotal.
     *
     *  - Нет предыдущего месяца: суммы + NeedsManualRecalc = true.
     *  - Есть: суммы для одних полей, разницы крайних — для других.
     *  - 0 → "—" для обязательных, "" для опциональных (col5/col7 не активны).
     *
     * Замечание: в C# при отсутствии предыдущего месяца ColSNE = sumSNE
     * (земля + воздух за месяц), а Col5 = sumSNE. Здесь так же.
     */
    private fun buildMonthlyTotal(
        start: Chast1StartEntity,
        allRecords: List<Chast1Entity>,
        monthRecords: List<Chast1Entity>,
        ym: YearMonth
    ): Chast1Entity {
        val label = RecalcCommon.totalLabel()
        val totalMonth = RecalcCommon.monthKey(ym.year, ym.monthValue)
        val startOfMonth = LocalDate.of(ym.year, ym.monthValue, 1)

        val col5Active = start.col5 != 0
        val col7Active = start.col7 != 0

        // Записи до текущего месяца
        val prevRecords = allRecords.asSequence()
            .filter { !it.isTotal }
            .filter {
                val d = RecalcCommon.parseDate(it.date) ?: return@filter false
                d.isBefore(startOfMonth)
            }
            .toList()

        // === Нет предыдущего месяца: только суммы ===
        if (prevRecords.isEmpty()) {
            val sumGround = monthRecords.sumOf { it.groundHours }
            val sumAir = monthRecords.sumOf { it.flightDuration }
            val sumCol8 = monthRecords.sumOf { it.col8 }
            val sumCol9 = monthRecords.sumOf { it.col9 }
            val sumCol11 = monthRecords.sumOf { it.col11 }
            val sumLandings2 = monthRecords.sumOf { it.landings2 }
            val sumSNE = sumGround + sumAir

            return Chast1Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true,
                groundHours = if (sumGround > 0) sumGround else 0,
                flightDuration = if (sumAir > 0) sumAir else 0,
                col5 = if (col5Active) sumSNE else 0,
                colSNE = sumSNE,
                col7 = if (col7Active) (sumCol8 + sumCol9) else 0,
                col8 = if (col7Active) sumCol8 else 0,
                col9 = if (col7Active) sumCol9 else 0,
                landings = sumCol8 + sumCol9,
                col11 = sumCol11,
                landings2 = sumLandings2
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
            return Chast1Entity(
                profileId = profileId,
                date = label,
                isTotal = true,
                totalMonth = totalMonth,
                needsManualRecalc = true
            )
        }

        val sumGround = monthRecords.sumOf { it.groundHours }
        val sumAir = monthRecords.sumOf { it.flightDuration }
        val sumCol8 = monthRecords.sumOf { it.col8 }
        val sumCol9 = monthRecords.sumOf { it.col9 }
        val sumCol11 = monthRecords.sumOf { it.col11 }
        val sumLandings2 = monthRecords.sumOf { it.landings2 }

        val diffCol5 = (lastCurrent.col5 - lastPrev.col5).coerceAtLeast(0)
        val diffSNE = (lastCurrent.colSNE - lastPrev.colSNE).coerceAtLeast(0)
        val diffCol7 = (lastCurrent.col7 - lastPrev.col7).coerceAtLeast(0)
        val diffLandings = (lastCurrent.landings - lastPrev.landings).coerceAtLeast(0)

        return Chast1Entity(
            profileId = profileId,
            date = label,
            isTotal = true,
            totalMonth = totalMonth,
            needsManualRecalc = false,
            groundHours = if (sumGround > 0) sumGround else 0,
            flightDuration = if (sumAir > 0) sumAir else 0,
            col5 = if (col5Active) diffCol5 else 0,
            colSNE = diffSNE,
            col7 = if (col7Active) diffCol7 else 0,
            col8 = if (col7Active) sumCol8 else 0,
            col9 = if (col7Active) sumCol9 else 0,
            landings = diffLandings,
            col11 = sumCol11,
            landings2 = sumLandings2
        )
    }
    /**
     * Обновляет ProfileEntity.fuselageSne значением последней ненулевой colSNE
     * из обычных записей Части 1.
     *
     * Используется вкладкой «Ресурсы» для автозаполнения СНЭ фюзеляжа
     * (первая строка таблицы «Агрегаты»).
     *
     * Если у профиля уже другое значение — обновляем.
     */
    private suspend fun refreshFuselageSneInternal() {
        val records = db.chast1Dao().getByProfile(profileId)

        // Идём по записям от самой поздней даты к самой ранней,
        // берём первую с colSNE > 0.
        val latestSne = records
            .filter { !it.isTotal }
            .sortedByDescending { RecalcCommon.parseDate(it.date) ?: LocalDate.MIN }
            .firstOrNull { it.colSNE > 0 }
            ?.colSNE
            ?: db.chast1StartDao().getByProfile(profileId)?.colSNE
            ?: 0

        val profile = db.profileDao().getById(profileId) ?: return
        if (profile.fuselageSne != latestSne) {
            db.profileDao().update(
                profile.copy(
                    fuselageSne = latestSne,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
