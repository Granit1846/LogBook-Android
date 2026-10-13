package com.example.logbook.printing

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.room.withTransaction
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.example.logbook.data.local.recalc.RecalcCommon
import com.example.logbook.ui.util.displayDate
import com.example.logbook.ui.util.minutesToTime
import java.time.LocalDate
import java.time.YearMonth
import java.math.BigDecimal
import java.math.RoundingMode
import com.example.logbook.ui.tabs.chast1.Chast1Cols
import com.example.logbook.ui.tabs.vr80.Vr80Cols
import com.example.logbook.ui.tabs.ta14.Ta14Cols
import com.example.logbook.ui.tabs.vk250001.Vk250001Cols
import com.example.logbook.ui.tabs.vk250002.Vk250002Cols

data class PrintHeaderCell(val x: Float, val y: Float, val width: Float, val height: Float,
    val text: String, val vertical: Boolean = false, val white: Boolean = false)
data class PrintRow(val date: String, val values: List<String>, val isTotal: Boolean = false,
    val totalMonth: String = "", val manual: Boolean = false, val baseline: Boolean = false)
data class PrintTable(val title: String, val widths: List<Float>, val headerHeight: Float,
    val headers: List<PrintHeaderCell>, val rows: List<PrintRow>)
data class PrintSnapshot(val profileName: String, val boardNumber: String, val tables: List<PrintTable>)
data class PrintOptions(val types: Set<String>, val from: LocalDate? = null, val to: LocalDate? = null,
    val includeBaseline: Boolean = true, val includeTotals: Boolean = true) {
    init { require(types.isNotEmpty()); require(from == null || to == null || from <= to) }
    fun includes(row: PrintRow): Boolean {
        if (row.baseline) return includeBaseline
        if (row.isTotal) {
            if (!includeTotals) return false
            val month = runCatching { YearMonth.parse(row.totalMonth) }.getOrNull() ?: return false
            return (from == null || from <= month.atDay(1)) && (to == null || to >= month.atEndOfMonth())
        }
        val date = RecalcCommon.parseDate(row.date) ?: return from == null && to == null
        return (from == null || date >= from) && (to == null || date <= to)
    }
}

class FormularPrintRepository(private val db: AppDatabase) {
    suspend fun snapshot(profileId: Long, options: PrintOptions): PrintSnapshot = db.withTransaction {
        val profile = checkNotNull(db.profileDao().getById(profileId)) { tr(R.string.msg_profile_not_found_0e520dc) }
        val assignments = com.example.logbook.data.local.AggregateManager.numbers(profile.aggregateAssignmentsJson)
        val tables = mutableListOf<PrintTable>()
        if ("Chast1" in options.types) {
            val rows = mutableListOf<PrintRow>()
            db.chast1StartDao().getByProfile(profileId)?.let { start ->
                rows.add(row(Chast1Entity(profileId = profileId, date = start.date, flightType = start.flightType, groundHours = start.groundHours, flightDuration = start.flightDuration, col5 = start.col5, colSNE = start.colSNE, col7 = start.col7, col8 = start.col8, col9 = start.col9, landings = start.landings, col11 = start.col11, landings2 = start.landings2, pilot = start.pilot, mechanic = start.mechanic)).copy(baseline = true))
            }
            rows.addAll(db.chast1Dao().getByProfile(profileId).map(::row))
            tables.add(PrintTable(com.example.logbook.localization.I18n.display("Часть 1") + assignments["Chast1"]?.takeIf { it.isNotBlank() }?.let { " / $it" }.orEmpty(),
                Chast1Cols.WIDTHS.map { it.value }, Chast1Cols.HEADER_HEIGHT.value,
                FormularPrintSchemas.chast1(), rows.filter(options::includes)))
        }
        if ("Vr80" in options.types) {
            val rows = mutableListOf<PrintRow>()
            db.vr80StartDao().getByProfile(profileId)?.let { start ->
                rows.add(row(Vr80Entity(profileId = profileId, date = start.date, launchCount = start.launchCount, groundHours = start.groundHours, col4 = start.col4, col5 = start.col5, col6 = start.col6, col7 = start.col7, col8 = start.col8, col9 = start.col9, col24 = start.col24, colSNE = start.colSNE, col26 = start.col26, col27 = start.col27, col28 = start.col28)).copy(baseline = true))
            }
            rows.addAll(db.vr80Dao().getByProfile(profileId).map(::row))
            tables.add(PrintTable(com.example.logbook.localization.I18n.display("ВР-80") + assignments["Vr80"]?.takeIf { it.isNotBlank() }?.let { " / $it" }.orEmpty(),
                Vr80Cols.WIDTHS.map { it.value }, Vr80Cols.HEADER_HEIGHT.value,
                FormularPrintSchemas.vr80(), rows.filter(options::includes)))
        }
        if ("Ta14" in options.types) {
            val rows = mutableListOf<PrintRow>()
            db.ta14StartDao().getByProfile(profileId)?.let { start ->
                rows.add(row(Ta14Entity(profileId = profileId, date = start.date, launchCount = start.launchCount, col3 = start.col3, colVremya = start.colVremya, col5 = start.col5, colSNE = start.colSNE, col7 = start.col7, col8 = start.col8, colSNE2 = start.colSNE2, launchCount2 = start.launchCount2, col11 = start.col11)).copy(baseline = true))
            }
            rows.addAll(db.ta14Dao().getByProfile(profileId).map(::row))
            tables.add(PrintTable(com.example.logbook.localization.I18n.display("Та-14") + assignments["Ta14"]?.takeIf { it.isNotBlank() }?.let { " / $it" }.orEmpty(),
                Ta14Cols.WIDTHS.map { it.value }, Ta14Cols.HEADER_HEIGHT.value,
                FormularPrintSchemas.ta14(), rows.filter(options::includes)))
        }
        if ("Vk250001" in options.types) {
            val rows = mutableListOf<PrintRow>()
            db.vk250001StartDao().getByProfile(profileId)?.let { start ->
                rows.add(row(Vk250001Entity(profileId = profileId, date = start.date, i = start.i, g = start.g, colSNE = start.colSNE, pprA = start.pprA, col8 = start.col8, b = start.b, col10 = start.col10, col11 = start.col11, col12 = start.col12, d = start.d, e = start.e, h = start.h, pprH = start.pprH)).copy(baseline = true))
            }
            rows.addAll(db.vk250001Dao().getByProfile(profileId).map(::row))
            tables.add(PrintTable(com.example.logbook.localization.I18n.display("ВК-2500-01") + assignments["Vk250001"]?.takeIf { it.isNotBlank() }?.let { " / $it" }.orEmpty(),
                Vk250001Cols.WIDTHS.map { it.value }, Vk250001Cols.HEADER_HEIGHT.value,
                FormularPrintSchemas.vk250001(), rows.filter(options::includes)))
        }
        if ("Vk250002" in options.types) {
            val rows = mutableListOf<PrintRow>()
            db.vk250002StartDao().getByProfile(profileId)?.let { start ->
                rows.add(row(Vk250002Entity(profileId = profileId, date = start.date, i = start.i, g = start.g, colSNE = start.colSNE, pprA = start.pprA, col8 = start.col8, b = start.b, col10 = start.col10, col11 = start.col11, col12 = start.col12, d = start.d, e = start.e, h = start.h, pprH = start.pprH)).copy(baseline = true))
            }
            rows.addAll(db.vk250002Dao().getByProfile(profileId).map(::row))
            tables.add(PrintTable(com.example.logbook.localization.I18n.display("ВК-2500-02") + assignments["Vk250002"]?.takeIf { it.isNotBlank() }?.let { " / $it" }.orEmpty(),
                Vk250002Cols.WIDTHS.map { it.value }, Vk250002Cols.HEADER_HEIGHT.value,
                FormularPrintSchemas.vk250002(), rows.filter(options::includes)))
        }
        if ("Journal" in options.types) {
            val works = try { com.example.logbook.data.model.MaintenanceJournal.works(profile) } catch (_: Exception) { emptyList() }
            val workRows = works.map { w ->
                val kindStr = when (w.kind) {
                    com.example.logbook.data.model.MaintenanceJournal.PERIODIC -> tr(R.string.journal_periodic)
                    com.example.logbook.data.model.MaintenanceJournal.HOURS_50 -> tr(R.string.journal_50)
                    com.example.logbook.data.model.MaintenanceJournal.HOURS_100 -> tr(R.string.journal_100)
                    com.example.logbook.data.model.MaintenanceJournal.HOURS_300 -> tr(R.string.journal_300)
                    com.example.logbook.data.model.MaintenanceJournal.REG_12 -> tr(R.string.journal_12)
                    com.example.logbook.data.model.MaintenanceJournal.REG_24 -> tr(R.string.journal_24)
                    else -> if (w.kind.isNotBlank()) w.kind else tr(R.string.journal_other)
                }
                val counterStr = if (w.counterMinutes > 0) minutesToTime(w.counterMinutes) else ""
                PrintRow(
                    date = w.date,
                    values = listOf(displayDate(w.date), kindStr, counterStr, w.description)
                )
            }
            tables.add(
                PrintTable(
                    title = tr(R.string.journal_title) + " / " + tr(R.string.journal_works),
                    widths = listOf(80f, 150f, 110f, 380f),
                    headerHeight = 28f,
                    headers = FormularPrintSchemas.journalWorks(),
                    rows = workRows.filter(options::includes)
                )
            )

            val tasks = try { com.example.logbook.data.model.MaintenanceJournal.tasks(profile) } catch (_: Exception) { emptyList() }
            val taskRows = tasks.map { t ->
                val createdDate = java.time.Instant.ofEpochMilli(t.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                val completedStr = if (t.completedAt > 0L) {
                    val d = java.time.Instant.ofEpochMilli(t.completedAt)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                    displayDate(d)
                } else tr(R.string.journal_due_now)
                PrintRow(
                    date = createdDate,
                    values = listOf(displayDate(createdDate), t.description, completedStr, "")
                )
            }
            tables.add(
                PrintTable(
                    title = tr(R.string.journal_title) + " / " + tr(R.string.journal_defects),
                    widths = listOf(80f, 320f, 120f, 200f),
                    headerHeight = 28f,
                    headers = FormularPrintSchemas.journalDefects(),
                    rows = taskRows.filter(options::includes)
                )
            )
        }
        PrintSnapshot(profile.name, assignments["Chast1"] ?: profile.boardNumber, tables)
    }
    private fun row(record: Chast1Entity): PrintRow = PrintRow(record.date, listOf(
        displayDate(record.date),
        record.flightType,
        minutesToTime(record.groundHours),
        minutesToTime(record.flightDuration),
        minutesToTime(record.col5),
        minutesToTime(record.colSNE),
        if (record.col7 == 0) "" else record.col7.toString(),
        if (record.col8 == 0) "" else record.col8.toString(),
        if (record.col9 == 0) "" else record.col9.toString(),
        if (record.landings == 0) "" else record.landings.toString(),
        if (record.col11 == 0) "" else record.col11.toString(),
        if (record.landings2 == 0) "" else record.landings2.toString(),
        record.pilot,
        record.mechanic,
        "",
        ""
    ), record.isTotal, record.totalMonth, record.needsManualRecalc)
    private fun row(record: Vr80Entity): PrintRow = PrintRow(record.date, listOf(
        displayDate(record.date),
        if (record.launchCount == 0) "" else record.launchCount.toString(),
        minutesToTime(record.groundHours),
        if (record.col4 == 0) "" else record.col4.toString(),
        if (record.col5 == 0) "" else record.col5.toString(),
        minutesToTime(record.col6),
        minutesToTime(record.col7),
        if (record.col8 == 0) "" else record.col8.toString(),
        if (record.col9 == 0) "" else record.col9.toString(),
        minutesToTime(record.col24),
        minutesToTime(record.colSNE),
        minutesToTime(record.col26),
        minutesToTime(record.col27),
        record.col28,
        ""
    ), record.isTotal, record.totalMonth, record.needsManualRecalc)
    private fun row(record: Ta14Entity): PrintRow = PrintRow(record.date, listOf(
        displayDate(record.date),
        if (record.launchCount == 0) "" else record.launchCount.toString(),
        if (record.col3 == 0) "" else record.col3.toString(),
        minutesToTime(record.colVremya),
        if (record.col5 == 0) "" else record.col5.toString(),
        minutesToTime(record.colSNE),
        minutesToTime(record.col7),
        if (record.col8 == 0) "" else record.col8.toString(),
        minutesToTime(record.colSNE2),
        if (record.launchCount2 == 0) "" else record.launchCount2.toString(),
        if (record.col11 == 0) "" else record.col11.toString(),
        ""
    ), record.isTotal, record.totalMonth, record.needsManualRecalc)
    private fun row(record: Vk250001Entity): PrintRow = PrintRow(record.date, listOf(
        displayDate(record.date),
        if (record.i == 0) "" else record.i.toString(),
        "",
        formatDouble(record.g),
        "",
        minutesToTime(record.colSNE),
        if (record.pprA == 0) "" else minutesToTime(record.pprA),
        formatDouble(record.col8),
        formatDouble(record.b),
        formatDouble(record.col10),
        formatDouble(record.col11),
        formatDouble(record.col12),
        "",
        formatDouble(record.d),
        "",
        formatDouble(record.e),
        "",
        formatDouble1(record.h),
        formatDouble1(record.pprH),
        "",
        ""
    ), record.isTotal, record.totalMonth, record.needsManualRecalc)
    private fun row(record: Vk250002Entity): PrintRow = PrintRow(record.date, listOf(
        displayDate(record.date),
        if (record.i == 0) "" else record.i.toString(),
        "",
        formatDouble(record.g),
        "",
        minutesToTime(record.colSNE),
        if (record.pprA == 0) "" else minutesToTime(record.pprA),
        formatDouble(record.col8),
        formatDouble(record.b),
        formatDouble(record.col10),
        formatDouble(record.col11),
        formatDouble(record.col12),
        "",
        formatDouble(record.d),
        "",
        formatDouble(record.e),
        "",
        formatDouble1(record.h),
        formatDouble1(record.pprH),
        "",
        ""
    ), record.isTotal, record.totalMonth, record.needsManualRecalc)
    private fun formatDouble(value: Double): String = if (value == 0.0) "" else
        BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',')
    private fun formatDouble1(value: Double): String = if (value == 0.0) "" else
        BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',')
}
