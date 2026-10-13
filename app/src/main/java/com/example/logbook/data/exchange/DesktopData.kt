package com.example.logbook.data.exchange

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import com.example.logbook.data.local.entity.*
import com.example.logbook.data.local.recalc.RecalcCommon
import com.example.logbook.data.model.*
import com.google.gson.*
import java.math.BigDecimal
import java.time.*
import java.util.UUID

object ExchangeIds {
    // Compatibility identities for files which predate persistent UUIDs.
    fun legacy(kind: String, key: String): String = UUID.nameUUIDFromBytes(
        "LogBook/legacy/v1/$kind/$key".toByteArray(Charsets.UTF_8)).toString()
}

internal data class DesktopDay(val day: DayEntity, val launches: List<LaunchEntity>, val snk: SnkEntity)
internal data class DesktopProfile(val sourceKey: String, val sharedId: String, val original: JsonObject,
    val profile: ProfileEntity, val days: List<DesktopDay>, val starts: Map<String, JsonObject>,
    val rows: Map<String, List<JsonObject>>, val warnings: List<String>)
internal data class DesktopArchive(val type: String, val number: String, val json: String)

internal object DesktopData {
    val types = listOf("Chast1", "Vr80", "Ta14", "Vk250001", "Vk250002")
    val gson = Gson()
    private val months = listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
    fun text(o: JsonObject, key: String): String = o.get(key)?.takeUnless { it.isJsonNull }?.let {
        require(it.isJsonPrimitive) { tr(R.string.msg_invalid_field_1_s_e1b7d0d, key) }; it.asString.trim()
    }.orEmpty()
    fun array(o: JsonObject, key: String): List<JsonObject> = o.get(key)?.takeUnless { it.isJsonNull }?.asJsonArray?.map { it.asJsonObject }.orEmpty()
    fun obj(o: JsonObject, key: String): JsonObject = o.get(key)?.takeUnless { it.isJsonNull }?.asJsonObject ?: JsonObject()
    private fun emptyNumber(value: String) = value.isBlank() || value in setOf("—", "–", "-")
    fun number(value: String): Double = if (emptyNumber(value)) 0.0 else BigDecimal(value.replace(',', '.')).toDouble().also { require(it.isFinite()) }
    fun integer(value: String): Int = if (emptyNumber(value) || value == "0:00") 0 else BigDecimal(value.replace(',', '.')).intValueExact()
    fun minutes(value: String): Int {
        if (emptyNumber(value)) return 0
        val match = Regex("(-?)(\\d+):(\\d{1,2})").matchEntire(value)
            ?: error(tr(R.string.msg_invalid_time_1_s_use_hh_mm_d2c83f0, value))
        val minute = match.groupValues[3].toInt(); require(minute in 0..59) { tr(R.string.msg_invalid_minutes_1_s_a26d8fc, value) }
        val result = Math.addExact(Math.multiplyExact(match.groupValues[2].toInt(), 60), minute)
        return if (match.groupValues[1] == "-") -result else result
    }
    fun date(value: String): String = RecalcCommon.formatFormularDate(
        RecalcCommon.parseDate(value) ?: error(tr(R.string.msg_invalid_date_1_s_b2d0f77, value)))
    private fun month(value: String): YearMonth? {
        val match = Regex("Итого за (\\S+) (\\d{4})", RegexOption.IGNORE_CASE).matchEntire(value) ?: return null
        val index = months.indexOf(match.groupValues[1].lowercase()); require(index >= 0) { tr(R.string.msg_unknown_month_1_s_85141bc, value) }
        return YearMonth.of(match.groupValues[2].toInt(), index + 1)
    }
    fun formular(type: String, source: JsonObject, start: Boolean, profile: JsonObject? = null): JsonObject {
        val defaults: Any = when (type) {
            "Chast1" -> if (start) Chast1StartEntity(profileId=0) else Chast1Entity(profileId=0)
            "Vr80" -> if (start) Vr80StartEntity(profileId=0) else Vr80Entity(profileId=0)
            "Ta14" -> if (start) Ta14StartEntity(profileId=0) else Ta14Entity(profileId=0)
            "Vk250001" -> if (start) Vk250001StartEntity(profileId=0) else Vk250001Entity(profileId=0)
            "Vk250002" -> if (start) Vk250002StartEntity(profileId=0) else Vk250002Entity(profileId=0)
            else -> error(tr(R.string.msg_unknown_logbook_1_s_57d78be, type))
        }
        val out = gson.toJsonTree(defaults).asJsonObject
        val times = when (type) {
            "Chast1" -> setOf("groundHours", "flightDuration", "col5", "colSNE")
            "Vr80" -> setOf("groundHours", "col6", "col7", "col24", "colSNE", "col26", "col27")
            "Ta14" -> setOf("colVremya", "colSNE", "col7", "colSNE2")
            else -> setOf("colSNE", "pprA")
        }
        val decimals = setOf("g", "col8", "b", "col10", "col11", "col12", "d", "e", "h", "pprH", "coeffA", "coeffH")
        val strings = setOf("flightType", "pilot", "mechanic", "col28")
        out.keySet().toList().filter { it !in setOf("id", "profileId", "date", "sortOrder", "isTotal", "totalMonth", "needsManualRecalc", "isHistorical") }.forEach { field ->
            val key = if (type.startsWith("Vk")) when(field) {
                "colSNE" -> "A"; "pprA" -> "A_PPR"; "pprH" -> "H_PPR"
                "coeffA" -> "${type}CoeffA"; "coeffH" -> "${type}CoeffH"
                else -> field.replaceFirstChar { it.uppercase() }
            } else field.replaceFirstChar { it.uppercase() }
            val raw = text(if (field.startsWith("coeff")) profile ?: JsonObject() else source, key)
            when {
                field in strings -> out.addProperty(field, raw)
                field in times -> out.addProperty(field, minutes(raw))
                type.startsWith("Vk") && field in decimals -> out.addProperty(field, number(raw))
                else -> out.addProperty(field, integer(raw))
            }
        }
        val rawDate = text(source, "Date")
        if (start) out.addProperty("date", if (rawDate.isBlank()) "" else date(rawDate))
        else {
            val month = month(rawDate)
            val day = month?.atEndOfMonth() ?: RecalcCommon.parseDate(rawDate) ?: error(tr(R.string.msg_invalid_date_in_1_s_2_s_6b73731, type, rawDate))
            out.addProperty("date", if (month == null) date(rawDate) else rawDate)
            out.addProperty("sortOrder", day.toEpochDay() * 2 + if (month != null) 1 else 0)
            out.addProperty("isTotal", month != null); out.addProperty("totalMonth", month?.toString().orEmpty())
            // Preserve the legacy cumulative values until a day is explicitly edited/reclosed.
            if (type != "Chast1") out.addProperty("isHistorical", true)
        }
        return out
    }

    fun profile(key: String, source: JsonObject): DesktopProfile {
        require(key.isNotBlank()) { tr(R.string.msg_empty_profile_key_42d5144) }
        val warnings = mutableListOf<String>()
        val starts = types.mapNotNull { type -> source.get("${type}StartRecord")?.takeUnless { it.isJsonNull }?.let {
            type to formular(type, it.asJsonObject, true, source)
        } }.toMap()
        val rows = types.associateWith { type -> array(source, "${type}Records").map { formular(type, it, false) } }
        val rawAssignments = obj(source, "AggregateAssignments")
        val assignments = JsonObject().apply { rawAssignments.keySet().forEach { addProperty(it, text(rawAssignments,it)) } }
        require(assignments.keySet().all { it in types }) { tr(R.string.msg_unknown_component_type_a7c1b8b) }
        val activeNumbers = types.drop(1).filter { text(assignments,it).isNotBlank() }.map {
            (if (it.startsWith("Vk")) "Vk2500" else it) to text(assignments,it)
        }
        require(activeNumbers.distinct().size == activeNumbers.size) { tr(R.string.msg_one_engine_is_assigned_to_both_positions_f423521) }
        types.forEach { type ->
            val unsupported = if (type == "Chast1") listOf("Col15") else if (type == "Vr80") listOf("Col10", "Col15") else emptyList()
            if ((array(source,"${type}Records") + obj(source,"${type}StartRecord")).any { row -> unsupported.any { text(row,it).isNotBlank() } })
                warnings.add(tr(R.string.msg_1_s_extra_columns_were_preserved_in_the_source_1e905bd, type))
        }
        val resources = array(source, "ResourceRecords").mapIndexed { i, r -> ResourceItem(
            name=text(r,"Name"), sne=minutes(text(r,"SNE")), extendedTo1=minutes(text(r,"ExtendedTo1")),
            yearsResource=text(r,"YearsResource"), issueDate=text(r,"IssueDate"), extendedTo2=text(r,"ExtendedTo2"),
            remaining1=minutes(text(r,"Remaining1")), remaining2=text(r,"Remaining2"),
            installedAtSne1=minutes(text(r,"InstalledAtSNE1")), installedAtSne2=minutes(text(r,"InstalledAtSNE2")),
            coefficient=text(r,"Coefficient"), order=i) }
        val oils = array(source, "OilRecords").mapIndexed { i, o -> OilItem(name=text(o,"Name"),
            byHours=minutes(text(o,"ByHours")), byYears=text(o,"ByYears"), coefficient=minutes(text(o,"Coefficient")), term=text(o,"Term"), order=i) }
        val history = array(source,"AggregateHistory").map { h ->
            val stamp = runCatching { LocalDateTime.parse(text(h,"ReplacedAt")).takeIf { it.year >= 1900 }?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0 }.getOrDefault(0)
            AggregateReplacement(type=text(h,"Type"),oldNumber=text(h,"OldNumber"),newNumber=text(h,"NewNumber"),reason=text(h,"Reason"),date=stamp)
        }
        val days = obj(source,"Days").entrySet().map { (keyDate, value) ->
            val d = value.asJsonObject
            val dayDate = date(keyDate)
            require(text(d,"Date").isBlank() || date(text(d,"Date")) == dayDate) { tr(R.string.msg_the_day_s_date_does_not_match_its_key_a47c136) }
            val snk = gson.toJsonTree(SnkEntity(dayId=0)).asJsonObject
            val labels = mutableSetOf<String>()
            array(d,"SnkValues").forEach { s ->
                val label = text(s,"Label").uppercase(); require(labels.add(label)) { tr(R.string.msg_duplicate_snk_1_s_71a5e1f, label) }
                if (label in listOf("A","B","E","G","H","I")) {
                    for (side in listOf("Left","Right")) snk.addProperty(label.lowercase()+side, number(text(s,"${side}Value")))
                } else if (text(s,"LeftValue").isNotBlank() || text(s,"RightValue").isNotBlank()) warnings.add(tr(R.string.msg_snk_1_s_for_2_s_was_preserved_in_the_source_an_68c93cb, label, dayDate))
            }
            val launches = array(d,"Launches").mapIndexed { i, l -> LaunchEntity(dayId=0,order=i,
                vsu=integer(text(l,"Vsu")), zemlya=integer(text(l,"Zemlya")), vozduh=integer(text(l,"Vozduh")), tp=integer(text(l,"TP")), samolet=integer(text(l,"SamoletLandings"))) }
            DesktopDay(DayEntity(profileId=0,date=dayDate,crew=text(d,"Crew"),isClosed=rows.getValue("Chast1").any { text(it,"date") == dayDate }), launches, gson.fromJson(snk,SnkEntity::class.java))
        }
        require(days.map { it.day.date }.distinct().size == days.size) { tr(R.string.msg_duplicate_flight_date_8d7e3fe) }
        val board = text(assignments,"Chast1")
        val name = text(source,"ProfileName").ifBlank { key }
        val identity = if (board.isNotBlank()) "board:$board" else "profile:$key"
        val last = rows.getValue("Chast1").filter { !it.get("isTotal").asBoolean }.maxByOrNull { it.get("sortOrder").asLong }
        val sne = last?.get("colSNE")?.asInt ?: starts["Chast1"]?.get("colSNE")?.asInt ?: 0
        if (history.any { it.date <= 0 }) warnings.add(tr(R.string.msg_some_replacement_history_dates_are_unknown_ins_ce265bd))
        warnings.add(tr(R.string.msg_recorded_operating_time_will_be_transferred_wi_f6bd8a4))
        val mw = if (source.has("MaintenanceWorks")) com.example.logbook.data.model.MaintenanceJournal.normalizeWorks(source.get("MaintenanceWorks").toString()) else "[]"
        val dt = if (source.has("DefectTasks")) com.example.logbook.data.model.MaintenanceJournal.normalizeTasks(source.get("DefectTasks").toString()) else "[]"
        return DesktopProfile(identity,ExchangeIds.legacy("profile",identity),source.deepCopy(),
            ProfileEntity(name=name,boardNumber=board,aggregateAssignmentsJson=gson.toJson(assignments),resourcesJson=gson.toJson(resources),
                oilsJson=gson.toJson(oils),aggregateHistoryJson=gson.toJson(history),fuselageSne=sne,
                maintenanceWorksJson=mw,defectTasksJson=dt),days,starts,rows,warnings)
    }

    fun archive(source: JsonObject): DesktopArchive {
        val type = text(source,"Type"); require(type in types && type != "Chast1") { tr(R.string.msg_unknown_component_archive_1_s_e79327c, type) }
        val number = text(source,"Number"); require(number.isNotBlank()) { tr(R.string.msg_archive_has_no_serial_number_9286438) }
        val json = JsonObject().apply {
            add("start",formular(type,obj(source,"${type}StartRecord"),true,source))
            add("records",gson.toJsonTree(array(source,"${type}Records").map { formular(type,it,false) }))
            add("legacySource",source.deepCopy())
            addProperty("sharedId",ExchangeIds.legacy("aggregate",(if(type.startsWith("Vk")) "Vk2500" else type)+":"+number))
        }
        return DesktopArchive(if(type.startsWith("Vk")) "Vk2500" else type,number,json.toString())
    }
}
