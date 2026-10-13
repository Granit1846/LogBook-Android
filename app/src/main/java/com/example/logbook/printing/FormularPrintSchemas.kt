package com.example.logbook.printing

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.logbook.ui.tabs.chast1.Chast1Cols
import com.example.logbook.ui.tabs.vr80.Vr80Cols
import com.example.logbook.ui.tabs.ta14.Ta14Cols
import com.example.logbook.ui.tabs.vk250001.Vk250001Cols
import com.example.logbook.ui.tabs.vk250002.Vk250002Cols

/** Print geometry mirrors the five formular headers; units are independent of screen zoom. */
object FormularPrintSchemas {
    fun chast1(): List<PrintHeaderCell> = buildList {
        fun HCellH(x: Dp, y: Dp, width: Dp, height: Dp, text: String, white: Boolean = false) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, white = white))
        }
        fun HCellV(x: Dp, y: Dp, width: Dp, height: Dp, text: String) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, vertical = true))
        }
        val y1 = 0.dp
        val y2 = Chast1Cols.H_LEVEL1
        val y3 = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2
        val y4 = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3
        val y5 = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4

        HCellV(x = Chast1Cols.XS[0], y = 0.dp, width = Chast1Cols.WIDTHS[0], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_date_a5b49d2))
        HCellV(x = Chast1Cols.XS[1], y = 0.dp, width = Chast1Cols.WIDTHS[1], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_flight_type_224b7b5))
        HCellV(x = Chast1Cols.XS[2], y = 0.dp, width = Chast1Cols.WIDTHS[2], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_ground_operating_time_including_20_of_flight_t_dc38bce))
        HCellV(x = Chast1Cols.XS[3], y = 0.dp, width = Chast1Cols.WIDTHS[3], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_flight_duration_h_min_aad3cea))
        HCellV(x = Chast1Cols.XS[14], y = 0.dp, width = Chast1Cols.WIDTHS[14], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_logbook_keeper_s_signature_1ee8168))
        HCellH(x = Chast1Cols.XS[15], y = 0.dp, width = Chast1Cols.WIDTHS[15], height = Chast1Cols.H_LEVEL1 + Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = "")

        HCellV(x = Chast1Cols.XS[12], y = y2, width = Chast1Cols.WIDTHS[12],
            height = Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_pilot_instructor_d07c5b8))
        HCellV(x = Chast1Cols.XS[13], y = y2, width = Chast1Cols.WIDTHS[13],
            height = Chast1Cols.H_LEVEL2 + Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4,
            text = tr(R.string.msg_technician_mechanic_d0a9b58))

        val wTotal1 = Chast1Cols.WIDTHS[4] + Chast1Cols.WIDTHS[5] + Chast1Cols.WIDTHS[6] +
                Chast1Cols.WIDTHS[7] + Chast1Cols.WIDTHS[8] + Chast1Cols.WIDTHS[9] +
                Chast1Cols.WIDTHS[10] + Chast1Cols.WIDTHS[11]
        HCellH(x = Chast1Cols.XS[4], y = y1, width = wTotal1, height = Chast1Cols.H_LEVEL1, text = tr(R.string.msg_total_operating_time_f693e7c))
        HCellH(x = Chast1Cols.XS[12], y = y1, width = Chast1Cols.WIDTHS[12] + Chast1Cols.WIDTHS[13],
            height = Chast1Cols.H_LEVEL1, text = tr(R.string.msg_surname_5aa7f89))

        HCellH(x = Chast1Cols.XS[4], y = y2, width = Chast1Cols.WIDTHS[4] + Chast1Cols.WIDTHS[5],
            height = Chast1Cols.H_LEVEL2, text = tr(R.string.msg_by_flight_time_h_min_90cde71))
        HCellH(x = Chast1Cols.XS[6], y = y2,
            width = Chast1Cols.WIDTHS[6] + Chast1Cols.WIDTHS[7] + Chast1Cols.WIDTHS[8] +
                    Chast1Cols.WIDTHS[9] + Chast1Cols.WIDTHS[10] + Chast1Cols.WIDTHS[11],
            height = Chast1Cols.H_LEVEL2, text = tr(R.string.msg_by_landings_b8bf40d))

        HCellV(x = Chast1Cols.XS[4], y = y3, width = Chast1Cols.WIDTHS[4],
            height = Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_since_last_overhaul_a1cdda4))
        HCellV(x = Chast1Cols.XS[5], y = y3, width = Chast1Cols.WIDTHS[5],
            height = Chast1Cols.H_LEVEL3 + Chast1Cols.H_LEVEL4, text = tr(R.string.msg_since_new_10ee563))
        HCellH(x = Chast1Cols.XS[6], y = y3,
            width = Chast1Cols.WIDTHS[6] + Chast1Cols.WIDTHS[7] + Chast1Cols.WIDTHS[8],
            height = Chast1Cols.H_LEVEL3, text = tr(R.string.msg_since_last_overhaul_a1cdda4))
        HCellH(x = Chast1Cols.XS[9], y = y3,
            width = Chast1Cols.WIDTHS[9] + Chast1Cols.WIDTHS[10] + Chast1Cols.WIDTHS[11],
            height = Chast1Cols.H_LEVEL3, text = tr(R.string.msg_since_new_10ee563))

        HCellV(x = Chast1Cols.XS[6], y = y4, width = Chast1Cols.WIDTHS[6], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_total_3b51439))
        HCellV(x = Chast1Cols.XS[7], y = y4, width = Chast1Cols.WIDTHS[7], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_aeroplane_mode_c51981c))
        HCellV(x = Chast1Cols.XS[8], y = y4, width = Chast1Cols.WIDTHS[8], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_helicopter_mode_41b9feb))
        HCellV(x = Chast1Cols.XS[9], y = y4, width = Chast1Cols.WIDTHS[9], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_total_3b51439))
        HCellV(x = Chast1Cols.XS[10], y = y4, width = Chast1Cols.WIDTHS[10], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_aeroplane_mode_c51981c))
        HCellV(x = Chast1Cols.XS[11], y = y4, width = Chast1Cols.WIDTHS[11], height = Chast1Cols.H_LEVEL4, text = tr(R.string.msg_helicopter_mode_41b9feb))

        HCellH(x = Chast1Cols.XS[0], y = y5, width = Chast1Cols.WIDTHS[0], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(x = Chast1Cols.XS[1], y = y5, width = Chast1Cols.WIDTHS[1], height = Chast1Cols.H_LEVEL5, text = "2", white = true)
        HCellH(x = Chast1Cols.XS[2], y = y5, width = Chast1Cols.WIDTHS[2], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_ground_42ff10b), white = true)
        HCellH(x = Chast1Cols.XS[3], y = y5, width = Chast1Cols.WIDTHS[3], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_flight_62a693a), white = true)
        HCellH(x = Chast1Cols.XS[4], y = y5, width = Chast1Cols.WIDTHS[4], height = Chast1Cols.H_LEVEL5, text = "5", white = true)
        HCellH(x = Chast1Cols.XS[5], y = y5, width = Chast1Cols.WIDTHS[5], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_sn_835d66a), white = true)
        HCellH(x = Chast1Cols.XS[6], y = y5, width = Chast1Cols.WIDTHS[6], height = Chast1Cols.H_LEVEL5, text = "7", white = true)
        HCellH(x = Chast1Cols.XS[7], y = y5, width = Chast1Cols.WIDTHS[7], height = Chast1Cols.H_LEVEL5, text = "8", white = true)
        HCellH(x = Chast1Cols.XS[8], y = y5, width = Chast1Cols.WIDTHS[8], height = Chast1Cols.H_LEVEL5, text = "9", white = true)
        HCellH(x = Chast1Cols.XS[9], y = y5, width = Chast1Cols.WIDTHS[9], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_landings_c4f82d2), white = true)
        HCellH(x = Chast1Cols.XS[10], y = y5, width = Chast1Cols.WIDTHS[10], height = Chast1Cols.H_LEVEL5, text = "11", white = true)
        HCellH(x = Chast1Cols.XS[11], y = y5, width = Chast1Cols.WIDTHS[11], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_landings_2_5693047), white = true)
        HCellH(x = Chast1Cols.XS[12], y = y5, width = Chast1Cols.WIDTHS[12], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_pilot_857680d), white = true)
        HCellH(x = Chast1Cols.XS[13], y = y5, width = Chast1Cols.WIDTHS[13], height = Chast1Cols.H_LEVEL5, text = tr(R.string.msg_technician_bda968f), white = true)
        HCellH(x = Chast1Cols.XS[14], y = y5, width = Chast1Cols.WIDTHS[14], height = Chast1Cols.H_LEVEL5, text = "15", white = true)
        HCellH(x = Chast1Cols.XS[15], y = y5, width = Chast1Cols.WIDTHS[15], height = Chast1Cols.H_LEVEL5, text = "", white = true)
    }
    fun vr80(): List<PrintHeaderCell> = buildList {
        fun HCellH(x: Dp, y: Dp, width: Dp, height: Dp, text: String, white: Boolean = false) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, white = white))
        }
        fun HCellV(x: Dp, y: Dp, width: Dp, height: Dp, text: String) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, vertical = true))
        }
        val y1 = 0.dp
        val y2 = Vr80Cols.H_LEVEL1
        val y3 = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2
        val y4 = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3
        val y5 = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4
        val fullH = Vr80Cols.H_LEVEL1 + Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4
        val h234 = Vr80Cols.H_LEVEL2 + Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4
        val h34 = Vr80Cols.H_LEVEL3 + Vr80Cols.H_LEVEL4

        // ============================================================
        // Rowspan 4: Col0, Col1, Col13, Col14
        // ============================================================
        HCellV(Vr80Cols.XS[0], y1, Vr80Cols.WIDTHS[0], fullH, tr(R.string.msg_date_a5b49d2))
        HCellV(Vr80Cols.XS[1], y1, Vr80Cols.WIDTHS[1], fullH, tr(R.string.msg_number_of_starts_6882d64))
        HCellV(Vr80Cols.XS[13], y1, Vr80Cols.WIDTHS[13], fullH, tr(R.string.msg_logbook_keeper_s_signature_1ee8168))
        HCellH(Vr80Cols.XS[14], y1, Vr80Cols.WIDTHS[14], fullH, "")

        // ============================================================
        // Col2-5 — Продолжительность работы ГР на земле
        // ============================================================
        // У1: Col2-5
        val wOnGround = Vr80Cols.WIDTHS[2] + Vr80Cols.WIDTHS[3] +
                Vr80Cols.WIDTHS[4] + Vr80Cols.WIDTHS[5]
        HCellH(Vr80Cols.XS[2], y1, wOnGround, Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_ground_operating_time_04f64fa))

        // У2: Col2-4 — "С приводом от двух двигателей"
        val wTwoEngines = Vr80Cols.WIDTHS[2] + Vr80Cols.WIDTHS[3] + Vr80Cols.WIDTHS[4]
        HCellH(Vr80Cols.XS[2], y2, wTwoEngines, Vr80Cols.H_LEVEL2,
            tr(R.string.msg_driven_by_both_engines_990e953))

        // У2-4: Col5 — "От турбопривода" (rowspan 3)
        HCellV(Vr80Cols.XS[5], y2, Vr80Cols.WIDTHS[5], h234, tr(R.string.msg_turbine_drive_2c2ef82))

        // У3-4: Col2 — "Полное время" (rowspan 2)
        HCellV(Vr80Cols.XS[2], y3, Vr80Cols.WIDTHS[2], h34, tr(R.string.msg_total_time_5bae5f0))

        // У3: Col3-4 — "На режимах"
        HCellH(Vr80Cols.XS[3], y3, Vr80Cols.WIDTHS[3] + Vr80Cols.WIDTHS[4],
            Vr80Cols.H_LEVEL3, tr(R.string.msg_by_mode_a3a328e))

        // У4: Col3 — "Номинальный", Col4 — "Взлётный"
        HCellV(Vr80Cols.XS[3], y4, Vr80Cols.WIDTHS[3], Vr80Cols.H_LEVEL4, tr(R.string.msg_nominal_63e315b))
        HCellV(Vr80Cols.XS[4], y4, Vr80Cols.WIDTHS[4], Vr80Cols.H_LEVEL4, tr(R.string.msg_takeoff_4235534))

        // ============================================================
        // Col6-8 — Продолжительность работы ГР в полёте
        // ============================================================
        // У1: Col6-8
        val wInFlight = Vr80Cols.WIDTHS[6] + Vr80Cols.WIDTHS[7] + Vr80Cols.WIDTHS[8]
        HCellH(Vr80Cols.XS[6], y1, wInFlight, Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_flight_operating_time_8e15cbb))

        // У2-4: Col6 — "Полное время" (rowspan 3)
        HCellV(Vr80Cols.XS[6], y2, Vr80Cols.WIDTHS[6], h234, tr(R.string.msg_total_time_5bae5f0))

        // У2: Col7-8 — "На режимах"
        HCellH(Vr80Cols.XS[7], y2, Vr80Cols.WIDTHS[7] + Vr80Cols.WIDTHS[8],
            Vr80Cols.H_LEVEL2, tr(R.string.msg_by_mode_a3a328e))

        // У3-4: Col7 — "Номинальный" (rowspan 2)
        HCellV(Vr80Cols.XS[7], y3, Vr80Cols.WIDTHS[7], h34, tr(R.string.msg_nominal_63e315b))

        // У3-4: Col8 — "Взлётный" (rowspan 2)
        HCellV(Vr80Cols.XS[8], y3, Vr80Cols.WIDTHS[8], h34, tr(R.string.msg_takeoff_4235534))

        // ============================================================
        // Col9-10 — Сумма работы ГР от двигателей
        // ============================================================
        // У1: Col9-10
        HCellH(Vr80Cols.XS[9], y1, Vr80Cols.WIDTHS[9] + Vr80Cols.WIDTHS[10],
            Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_engine_driven_time_in_all_modes_i_57599f1))

        // У2-4: Col9 — "После последнего ремонта" (rowspan 3)
        HCellV(Vr80Cols.XS[9], y2, Vr80Cols.WIDTHS[9], h234,
            tr(R.string.msg_since_last_overhaul_a1cdda4))

        // У2-4: Col10 — "С начала эксплуатации" (rowspan 3)
        HCellV(Vr80Cols.XS[10], y2, Vr80Cols.WIDTHS[10], h234,
            tr(R.string.msg_since_new_10ee563))

        // ============================================================
        // Col11-12 — Сумма работы ГР от турбопривода
        // ============================================================
        // У1: Col11-12
        HCellH(Vr80Cols.XS[11], y1, Vr80Cols.WIDTHS[11] + Vr80Cols.WIDTHS[12],
            Vr80Cols.H_LEVEL1,
            tr(R.string.msg_main_gearbox_turbine_driven_operating_time_4478b34))

        // У2-4: Col11 — "После последнего ремонта" (rowspan 3)
        HCellV(Vr80Cols.XS[11], y2, Vr80Cols.WIDTHS[11], h234,
            tr(R.string.msg_since_last_overhaul_a1cdda4))

        // У2-4: Col12 — "С начала эксплуатации" (rowspan 3)
        HCellV(Vr80Cols.XS[12], y2, Vr80Cols.WIDTHS[12], h234,
            tr(R.string.msg_since_new_10ee563))

        // ============================================================
        // Уровень 5 — белая строка
        // ============================================================
        HCellH(Vr80Cols.XS[0], y5, Vr80Cols.WIDTHS[0], Vr80Cols.H_LEVEL5, tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(Vr80Cols.XS[1], y5, Vr80Cols.WIDTHS[1], Vr80Cols.H_LEVEL5, tr(R.string.msg_starts_c001fc7), white = true)
        HCellH(Vr80Cols.XS[2], y5, Vr80Cols.WIDTHS[2], Vr80Cols.H_LEVEL5, tr(R.string.msg_ground_42ff10b), white = true)
        HCellH(Vr80Cols.XS[3], y5, Vr80Cols.WIDTHS[3], Vr80Cols.H_LEVEL5, "4", white = true)
        HCellH(Vr80Cols.XS[4], y5, Vr80Cols.WIDTHS[4], Vr80Cols.H_LEVEL5, "5", white = true)
        HCellH(Vr80Cols.XS[5], y5, Vr80Cols.WIDTHS[5], Vr80Cols.H_LEVEL5, "6", white = true)
        HCellH(Vr80Cols.XS[6], y5, Vr80Cols.WIDTHS[6], Vr80Cols.H_LEVEL5, tr(R.string.msg_flight_62a693a), white = true)
        HCellH(Vr80Cols.XS[7], y5, Vr80Cols.WIDTHS[7], Vr80Cols.H_LEVEL5, "8", white = true)
        HCellH(Vr80Cols.XS[8], y5, Vr80Cols.WIDTHS[8], Vr80Cols.H_LEVEL5, "9", white = true)
        HCellH(Vr80Cols.XS[9], y5, Vr80Cols.WIDTHS[9], Vr80Cols.H_LEVEL5, "24", white = true)
        HCellH(Vr80Cols.XS[10], y5, Vr80Cols.WIDTHS[10], Vr80Cols.H_LEVEL5, tr(R.string.msg_sn_835d66a), white = true)
        HCellH(Vr80Cols.XS[11], y5, Vr80Cols.WIDTHS[11], Vr80Cols.H_LEVEL5, "26", white = true)
        HCellH(Vr80Cols.XS[12], y5, Vr80Cols.WIDTHS[12], Vr80Cols.H_LEVEL5, "27", white = true)
        HCellH(Vr80Cols.XS[13], y5, Vr80Cols.WIDTHS[13], Vr80Cols.H_LEVEL5, "28", white = true)
        HCellH(Vr80Cols.XS[14], y5, Vr80Cols.WIDTHS[14], Vr80Cols.H_LEVEL5, "", white = true)
    }
    fun ta14(): List<PrintHeaderCell> = buildList {
        fun HCellH(x: Dp, y: Dp, width: Dp, height: Dp, text: String, white: Boolean = false) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, white = white))
        }
        fun HCellV(x: Dp, y: Dp, width: Dp, height: Dp, text: String) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, vertical = true))
        }
        val y1 = 0.dp
        val y2 = Ta14Cols.H_LEVEL1
        val y3 = Ta14Cols.H_LEVEL1 + Ta14Cols.H_LEVEL2
        val fullH = Ta14Cols.H_LEVEL1 + Ta14Cols.H_LEVEL2

        // Rowspan 2: Col0, Col1, Col2, Col3, Col4, Col5, Col10, Col11
        HCellV(Ta14Cols.XS[0], y1, Ta14Cols.WIDTHS[0], fullH, tr(R.string.msg_date_a5b49d2))
        HCellV(Ta14Cols.XS[1], y1, Ta14Cols.WIDTHS[1], fullH, tr(R.string.msg_number_of_starts_6882d64))
        HCellV(Ta14Cols.XS[2], y1, Ta14Cols.WIDTHS[2], fullH,
            tr(R.string.msg_operating_time_and_start_counter_readings_freq_e31b879))
        HCellV(Ta14Cols.XS[3], y1, Ta14Cols.WIDTHS[3], fullH,
            tr(R.string.msg_engine_ground_operating_time_h_min_4eff8e6))
        HCellV(Ta14Cols.XS[4], y1, Ta14Cols.WIDTHS[4], fullH,
            tr(R.string.msg_engine_flight_operating_time_h_min_ad084d1))
        HCellV(Ta14Cols.XS[5], y1, Ta14Cols.WIDTHS[5], fullH,
            tr(R.string.msg_total_engine_operating_time_h_min_e7bc036))
        HCellV(Ta14Cols.XS[10], y1, Ta14Cols.WIDTHS[10], fullH,
            tr(R.string.msg_cycle_counter_reading_98189da))
        HCellH(Ta14Cols.XS[11], y1, Ta14Cols.WIDTHS[11], fullH, "")

        // Col6-9 — группа "Суммарная наработка двигателя, ч.мин/запуски"
        val wGroup = Ta14Cols.WIDTHS[6] + Ta14Cols.WIDTHS[7] +
                Ta14Cols.WIDTHS[8] + Ta14Cols.WIDTHS[9]
        HCellH(Ta14Cols.XS[6], y1, wGroup, Ta14Cols.H_LEVEL1,
            tr(R.string.msg_total_engine_operating_time_h_min_starts_560f455))

        // Уровень 2: Col6-7 — "после последнего капитального ремонта"
        val wAfterRepair = Ta14Cols.WIDTHS[6] + Ta14Cols.WIDTHS[7]
        HCellH(Ta14Cols.XS[6], y2, wAfterRepair, Ta14Cols.H_LEVEL2,
            tr(R.string.msg_since_last_overhaul_00c7522))

        // Уровень 2: Col8-9 — "с начала эксплуатации"
        val wSinceOp = Ta14Cols.WIDTHS[8] + Ta14Cols.WIDTHS[9]
        HCellH(Ta14Cols.XS[8], y2, wSinceOp, Ta14Cols.H_LEVEL2,
            tr(R.string.msg_since_new_f8324b5))

        // Белая строка (уровень 3)
        HCellH(Ta14Cols.XS[0], y3, Ta14Cols.WIDTHS[0], Ta14Cols.H_LEVEL3, tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(Ta14Cols.XS[1], y3, Ta14Cols.WIDTHS[1], Ta14Cols.H_LEVEL3, tr(R.string.msg_starts_8338da8), white = true)
        HCellH(Ta14Cols.XS[2], y3, Ta14Cols.WIDTHS[2], Ta14Cols.H_LEVEL3, "3", white = true)
        HCellH(Ta14Cols.XS[3], y3, Ta14Cols.WIDTHS[3], Ta14Cols.H_LEVEL3, tr(R.string.msg_time_c80d7e8), white = true)
        HCellH(Ta14Cols.XS[4], y3, Ta14Cols.WIDTHS[4], Ta14Cols.H_LEVEL3, "5", white = true)
        HCellH(Ta14Cols.XS[5], y3, Ta14Cols.WIDTHS[5], Ta14Cols.H_LEVEL3, tr(R.string.msg_sn_835d66a), white = true)
        HCellH(Ta14Cols.XS[6], y3, Ta14Cols.WIDTHS[6], Ta14Cols.H_LEVEL3, "7", white = true)
        HCellH(Ta14Cols.XS[7], y3, Ta14Cols.WIDTHS[7], Ta14Cols.H_LEVEL3, "8", white = true)
        HCellH(Ta14Cols.XS[8], y3, Ta14Cols.WIDTHS[8], Ta14Cols.H_LEVEL3, tr(R.string.msg_sn_2_e8543ee), white = true)
        HCellH(Ta14Cols.XS[9], y3, Ta14Cols.WIDTHS[9], Ta14Cols.H_LEVEL3, tr(R.string.msg_starts_2_0264acb), white = true)
        HCellH(Ta14Cols.XS[10], y3, Ta14Cols.WIDTHS[10], Ta14Cols.H_LEVEL3, "11", white = true)
        HCellH(Ta14Cols.XS[11], y3, Ta14Cols.WIDTHS[11], Ta14Cols.H_LEVEL3, "", white = true)
    }
    fun vk250001(): List<PrintHeaderCell> = buildList {
        fun HCellH(x: Dp, y: Dp, width: Dp, height: Dp, text: String, white: Boolean = false) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, white = white))
        }
        fun HCellV(x: Dp, y: Dp, width: Dp, height: Dp, text: String) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, vertical = true))
        }
        val y1 = 0.dp
        val y2 = Vk250001Cols.H_LEVEL1
        val y3 = Vk250001Cols.H_LEVEL1 + Vk250001Cols.H_LEVEL2
        val y4 = Vk250001Cols.H_LEVEL1 + Vk250001Cols.H_LEVEL2 + Vk250001Cols.H_LEVEL3
        val fullH = Vk250001Cols.H_LEVEL1 + Vk250001Cols.H_LEVEL2 + Vk250001Cols.H_LEVEL3
        val h23 = Vk250001Cols.H_LEVEL2 + Vk250001Cols.H_LEVEL3

        // Rowspan 3: Col0 (Дата), Col1 (Запуск), Col19 (Подпись), Col20 (✕)
        HCellV(Vk250001Cols.XS[0], y1, Vk250001Cols.WIDTHS[0], fullH, tr(R.string.msg_date_a5b49d2))
        HCellV(Vk250001Cols.XS[1], y1, Vk250001Cols.WIDTHS[1], fullH, tr(R.string.msg_number_of_starts_6882d64))
        HCellV(Vk250001Cols.XS[19], y1, Vk250001Cols.WIDTHS[19], fullH, tr(R.string.msg_logbook_keeper_s_signature_1ee8168))
        HCellH(Vk250001Cols.XS[20], y1, Vk250001Cols.WIDTHS[20], fullH, "")

        // У1: Col2-18 — общая группа "Наработка двигателя, ч:мин"
        val wEngine = Vk250001Cols.WIDTHS[2] + Vk250001Cols.WIDTHS[3] + Vk250001Cols.WIDTHS[4] +
                Vk250001Cols.WIDTHS[5] + Vk250001Cols.WIDTHS[6] + Vk250001Cols.WIDTHS[7] +
                Vk250001Cols.WIDTHS[8] + Vk250001Cols.WIDTHS[9] + Vk250001Cols.WIDTHS[10] +
                Vk250001Cols.WIDTHS[11] + Vk250001Cols.WIDTHS[12] + Vk250001Cols.WIDTHS[13] +
                Vk250001Cols.WIDTHS[14] + Vk250001Cols.WIDTHS[15] + Vk250001Cols.WIDTHS[16] +
                Vk250001Cols.WIDTHS[17] + Vk250001Cols.WIDTHS[18]
        HCellH(Vk250001Cols.XS[2], y1, wEngine, Vk250001Cols.H_LEVEL1,
            tr(R.string.msg_engine_operating_time_h_min_d10ebec))

        // У2: Col2-3 — "Эквивалентная наработка (G)"
        HCellH(Vk250001Cols.XS[2], y2, Vk250001Cols.WIDTHS[2] + Vk250001Cols.WIDTHS[3],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_equivalent_operating_time_g_5cf9acc))

        // У2: Col4-6 — "Суммарная (общая) наработка (A)"
        HCellH(Vk250001Cols.XS[4], y2,
            Vk250001Cols.WIDTHS[4] + Vk250001Cols.WIDTHS[5] + Vk250001Cols.WIDTHS[6],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_total_operating_time_a_5345b4f))

        // У2: Col7-8 — "Наработка на взлётном режиме (B)"
        HCellH(Vk250001Cols.XS[7], y2, Vk250001Cols.WIDTHS[7] + Vk250001Cols.WIDTHS[8],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_takeoff_operating_time_b_e171014))

        // У2: Col9-11 — "Наработка на максимально продолжительном режиме"
        HCellH(Vk250001Cols.XS[9], y2,
            Vk250001Cols.WIDTHS[9] + Vk250001Cols.WIDTHS[10] + Vk250001Cols.WIDTHS[11],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_maximum_continuous_mode_operating_time_9a36288))

        // У2: Col12-13 — "Наработка при повышенной температуре газов (D)"
        HCellH(Vk250001Cols.XS[12], y2, Vk250001Cols.WIDTHS[12] + Vk250001Cols.WIDTHS[13],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_high_exhaust_gas_temperature_operating_time_d_0c852d0))

        // У2: Col14-15 — "Наработка при отключенном БАРК (E)"
        HCellH(Vk250001Cols.XS[14], y2, Vk250001Cols.WIDTHS[14] + Vk250001Cols.WIDTHS[15],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_operating_time_with_bark_disabled_e_b57a145))

        // У2: Col16-18 — "Циклическая наработка (H)"
        HCellH(Vk250001Cols.XS[16], y2,
            Vk250001Cols.WIDTHS[16] + Vk250001Cols.WIDTHS[17] + Vk250001Cols.WIDTHS[18],
            Vk250001Cols.H_LEVEL2, tr(R.string.msg_cycle_based_operating_time_h_e4ab175))

        // У3: подписи
        // Col2 — пусто
        // Col3 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[3], y3, Vk250001Cols.WIDTHS[3], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col4 — пусто
        // Col5 — "С начала эксплуатации"
        HCellH(Vk250001Cols.XS[5], y3, Vk250001Cols.WIDTHS[5], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_new_10ee563))
        // Col6 — "После последнего кап. ремонта" (вертикально)
        HCellV(Vk250001Cols.XS[6], y3, Vk250001Cols.WIDTHS[6], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_last_overhaul_e8d220d))
        // Col7 — "От 6 до 15 мин"
        HCellH(Vk250001Cols.XS[7], y3, Vk250001Cols.WIDTHS[7], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_6_to_15_min_24673b0))
        // Col8 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[8], y3, Vk250001Cols.WIDTHS[8], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col9 — "Общая" (вертикально)
        HCellV(Vk250001Cols.XS[9], y3, Vk250001Cols.WIDTHS[9], Vk250001Cols.H_LEVEL3, tr(R.string.msg_overall_d7672c5))
        // Col10 — "На земле" (вертикально)
        HCellV(Vk250001Cols.XS[10], y3, Vk250001Cols.WIDTHS[10], Vk250001Cols.H_LEVEL3, tr(R.string.msg_on_ground_267312c))
        // Col11 — "В полёте" (вертикально)
        HCellV(Vk250001Cols.XS[11], y3, Vk250001Cols.WIDTHS[11], Vk250001Cols.H_LEVEL3, tr(R.string.msg_in_flight_2b66de6))
        // Col12 — пусто
        // Col13 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[13], y3, Vk250001Cols.WIDTHS[13], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col14 — пусто
        // Col15 — "Суммарное время работы"
        HCellH(Vk250001Cols.XS[15], y3, Vk250001Cols.WIDTHS[15], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col16 — пусто
        // Col17 — "С начала эксплуатации"
        HCellH(Vk250001Cols.XS[17], y3, Vk250001Cols.WIDTHS[17], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_new_10ee563))
        // Col18 — "После последнего кап. ремонта" (вертикально)
        HCellV(Vk250001Cols.XS[18], y3, Vk250001Cols.WIDTHS[18], Vk250001Cols.H_LEVEL3,
            tr(R.string.msg_since_last_overhaul_e8d220d))

        // Белая строка (уровень 4)
        HCellH(Vk250001Cols.XS[0], y4, Vk250001Cols.WIDTHS[0], Vk250001Cols.H_LEVEL4, tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(Vk250001Cols.XS[1], y4, Vk250001Cols.WIDTHS[1], Vk250001Cols.H_LEVEL4, tr(R.string.msg_starts_c001fc7), white = true)
        HCellH(Vk250001Cols.XS[2], y4, Vk250001Cols.WIDTHS[2], Vk250001Cols.H_LEVEL4, "3", white = true)
        HCellH(Vk250001Cols.XS[3], y4, Vk250001Cols.WIDTHS[3], Vk250001Cols.H_LEVEL4, "G", white = true)
        HCellH(Vk250001Cols.XS[4], y4, Vk250001Cols.WIDTHS[4], Vk250001Cols.H_LEVEL4, "5", white = true)
        HCellH(Vk250001Cols.XS[5], y4, Vk250001Cols.WIDTHS[5], Vk250001Cols.H_LEVEL4, tr(R.string.msg_sn_835d66a), white = true)
        HCellH(Vk250001Cols.XS[6], y4, Vk250001Cols.WIDTHS[6], Vk250001Cols.H_LEVEL4, tr(R.string.msg_so_a921cdf), white = true)
        HCellH(Vk250001Cols.XS[7], y4, Vk250001Cols.WIDTHS[7], Vk250001Cols.H_LEVEL4, "8", white = true)
        HCellH(Vk250001Cols.XS[8], y4, Vk250001Cols.WIDTHS[8], Vk250001Cols.H_LEVEL4, "B", white = true)
        HCellH(Vk250001Cols.XS[9], y4, Vk250001Cols.WIDTHS[9], Vk250001Cols.H_LEVEL4, "10", white = true)
        HCellH(Vk250001Cols.XS[10], y4, Vk250001Cols.WIDTHS[10], Vk250001Cols.H_LEVEL4, "11", white = true)
        HCellH(Vk250001Cols.XS[11], y4, Vk250001Cols.WIDTHS[11], Vk250001Cols.H_LEVEL4, "12", white = true)
        HCellH(Vk250001Cols.XS[12], y4, Vk250001Cols.WIDTHS[12], Vk250001Cols.H_LEVEL4, "13", white = true)
        HCellH(Vk250001Cols.XS[13], y4, Vk250001Cols.WIDTHS[13], Vk250001Cols.H_LEVEL4, "D", white = true)
        HCellH(Vk250001Cols.XS[14], y4, Vk250001Cols.WIDTHS[14], Vk250001Cols.H_LEVEL4, "15", white = true)
        HCellH(Vk250001Cols.XS[15], y4, Vk250001Cols.WIDTHS[15], Vk250001Cols.H_LEVEL4, "E", white = true)
        HCellH(Vk250001Cols.XS[16], y4, Vk250001Cols.WIDTHS[16], Vk250001Cols.H_LEVEL4, "17", white = true)
        HCellH(Vk250001Cols.XS[17], y4, Vk250001Cols.WIDTHS[17], Vk250001Cols.H_LEVEL4, "H", white = true)
        HCellH(Vk250001Cols.XS[18], y4, Vk250001Cols.WIDTHS[18], Vk250001Cols.H_LEVEL4, tr(R.string.msg_so_h_e7084ac), white = true)
        HCellH(Vk250001Cols.XS[19], y4, Vk250001Cols.WIDTHS[19], Vk250001Cols.H_LEVEL4, "", white = true)
        HCellH(Vk250001Cols.XS[20], y4, Vk250001Cols.WIDTHS[20], Vk250001Cols.H_LEVEL4, "", white = true)
    }
    fun vk250002(): List<PrintHeaderCell> = buildList {
        fun HCellH(x: Dp, y: Dp, width: Dp, height: Dp, text: String, white: Boolean = false) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, white = white))
        }
        fun HCellV(x: Dp, y: Dp, width: Dp, height: Dp, text: String) {
            add(PrintHeaderCell(x.value, y.value, width.value, height.value, text, vertical = true))
        }
        val y1 = 0.dp
        val y2 = Vk250002Cols.H_LEVEL1
        val y3 = Vk250002Cols.H_LEVEL1 + Vk250002Cols.H_LEVEL2
        val y4 = Vk250002Cols.H_LEVEL1 + Vk250002Cols.H_LEVEL2 + Vk250002Cols.H_LEVEL3
        val fullH = Vk250002Cols.H_LEVEL1 + Vk250002Cols.H_LEVEL2 + Vk250002Cols.H_LEVEL3

        // Rowspan 3: Col0 (Дата), Col1 (Запуск), Col19 (Подпись), Col20 (✕)
        HCellV(Vk250002Cols.XS[0], y1, Vk250002Cols.WIDTHS[0], fullH, tr(R.string.msg_date_a5b49d2))
        HCellV(Vk250002Cols.XS[1], y1, Vk250002Cols.WIDTHS[1], fullH, tr(R.string.msg_number_of_starts_6882d64))
        HCellV(Vk250002Cols.XS[19], y1, Vk250002Cols.WIDTHS[19], fullH, tr(R.string.msg_logbook_keeper_s_signature_1ee8168))
        HCellH(Vk250002Cols.XS[20], y1, Vk250002Cols.WIDTHS[20], fullH, "")

        // У1: Col2-18 — общая группа "Наработка двигателя, ч:мин"
        val wEngine = Vk250002Cols.WIDTHS[2] + Vk250002Cols.WIDTHS[3] + Vk250002Cols.WIDTHS[4] +
                Vk250002Cols.WIDTHS[5] + Vk250002Cols.WIDTHS[6] + Vk250002Cols.WIDTHS[7] +
                Vk250002Cols.WIDTHS[8] + Vk250002Cols.WIDTHS[9] + Vk250002Cols.WIDTHS[10] +
                Vk250002Cols.WIDTHS[11] + Vk250002Cols.WIDTHS[12] + Vk250002Cols.WIDTHS[13] +
                Vk250002Cols.WIDTHS[14] + Vk250002Cols.WIDTHS[15] + Vk250002Cols.WIDTHS[16] +
                Vk250002Cols.WIDTHS[17] + Vk250002Cols.WIDTHS[18]
        HCellH(Vk250002Cols.XS[2], y1, wEngine, Vk250002Cols.H_LEVEL1,
            tr(R.string.msg_engine_operating_time_h_min_d10ebec))

        // У2: Col2-3 — "Эквивалентная наработка (G)"
        HCellH(Vk250002Cols.XS[2], y2, Vk250002Cols.WIDTHS[2] + Vk250002Cols.WIDTHS[3],
            Vk250002Cols.H_LEVEL2, tr(R.string.msg_equivalent_operating_time_g_5cf9acc))

        // У2: Col4-6 — "Суммарная (общая) наработка (A)"
        HCellH(Vk250002Cols.XS[4], y2,
            Vk250002Cols.WIDTHS[4] + Vk250002Cols.WIDTHS[5] + Vk250002Cols.WIDTHS[6],
            Vk250002Cols.H_LEVEL2, tr(R.string.msg_total_operating_time_a_5345b4f))

        // У2: Col7-8 — "Наработка на взлётном режиме (B)"
        HCellH(Vk250002Cols.XS[7], y2, Vk250002Cols.WIDTHS[7] + Vk250002Cols.WIDTHS[8],
            Vk250002Cols.H_LEVEL2, tr(R.string.msg_takeoff_operating_time_b_e171014))

        // У2: Col9-11 — "Наработка на максимально продолжительном режиме"
        HCellH(Vk250002Cols.XS[9], y2,
            Vk250002Cols.WIDTHS[9] + Vk250002Cols.WIDTHS[10] + Vk250002Cols.WIDTHS[11],
            Vk250002Cols.H_LEVEL2, tr(R.string.msg_maximum_continuous_mode_operating_time_9a36288))

        // У2: Col12-13 — "Наработка при повышенной температуре газов (D)"
        HCellH(Vk250002Cols.XS[12], y2, Vk250002Cols.WIDTHS[12] + Vk250002Cols.WIDTHS[13],
            Vk250002Cols.H_LEVEL2, tr(R.string.msg_high_exhaust_gas_temperature_operating_time_d_0c852d0))

        // У2: Col14-15 — "Наработка при отключенном БАРК (E)"
        HCellH(Vk250002Cols.XS[14], y2, Vk250002Cols.WIDTHS[14] + Vk250002Cols.WIDTHS[15],
            Vk250002Cols.H_LEVEL2, tr(R.string.msg_operating_time_with_bark_disabled_e_b57a145))

        // У2: Col16-18 — "Циклическая наработка (H)"
        HCellH(Vk250002Cols.XS[16], y2,
            Vk250002Cols.WIDTHS[16] + Vk250002Cols.WIDTHS[17] + Vk250002Cols.WIDTHS[18],
            Vk250002Cols.H_LEVEL2, tr(R.string.msg_cycle_based_operating_time_h_e4ab175))

        // У3: подписи
        // Col2 — пусто
        // Col3 — "Суммарное время работы"
        HCellH(Vk250002Cols.XS[3], y3, Vk250002Cols.WIDTHS[3], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col4 — пусто
        // Col5 — "С начала эксплуатации"
        HCellH(Vk250002Cols.XS[5], y3, Vk250002Cols.WIDTHS[5], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_since_new_10ee563))
        // Col6 — "После последнего кап. ремонта" (вертикально)
        HCellV(Vk250002Cols.XS[6], y3, Vk250002Cols.WIDTHS[6], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_since_last_overhaul_e8d220d))
        // Col7 — "От 6 до 15 мин"
        HCellH(Vk250002Cols.XS[7], y3, Vk250002Cols.WIDTHS[7], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_6_to_15_min_24673b0))
        // Col8 — "Суммарное время работы"
        HCellH(Vk250002Cols.XS[8], y3, Vk250002Cols.WIDTHS[8], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col9 — "Общая" (вертикально)
        HCellV(Vk250002Cols.XS[9], y3, Vk250002Cols.WIDTHS[9], Vk250002Cols.H_LEVEL3, tr(R.string.msg_overall_d7672c5))
        // Col10 — "На земле" (вертикально)
        HCellV(Vk250002Cols.XS[10], y3, Vk250002Cols.WIDTHS[10], Vk250002Cols.H_LEVEL3, tr(R.string.msg_on_ground_267312c))
        // Col11 — "В полёте" (вертикально)
        HCellV(Vk250002Cols.XS[11], y3, Vk250002Cols.WIDTHS[11], Vk250002Cols.H_LEVEL3, tr(R.string.msg_in_flight_2b66de6))
        // Col12 — пусто
        // Col13 — "Суммарное время работы"
        HCellH(Vk250002Cols.XS[13], y3, Vk250002Cols.WIDTHS[13], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col14 — пусто
        // Col15 — "Суммарное время работы"
        HCellH(Vk250002Cols.XS[15], y3, Vk250002Cols.WIDTHS[15], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_total_operating_time_672db4c))
        // Col16 — пусто
        // Col17 — "С начала эксплуатации"
        HCellH(Vk250002Cols.XS[17], y3, Vk250002Cols.WIDTHS[17], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_since_new_10ee563))
        // Col18 — "После последнего кап. ремонта" (вертикально)
        HCellV(Vk250002Cols.XS[18], y3, Vk250002Cols.WIDTHS[18], Vk250002Cols.H_LEVEL3,
            tr(R.string.msg_since_last_overhaul_e8d220d))

        // Белая строка (уровень 4)
        HCellH(Vk250002Cols.XS[0], y4, Vk250002Cols.WIDTHS[0], Vk250002Cols.H_LEVEL4, tr(R.string.msg_date_a5b49d2), white = true)
        HCellH(Vk250002Cols.XS[1], y4, Vk250002Cols.WIDTHS[1], Vk250002Cols.H_LEVEL4, tr(R.string.msg_starts_c001fc7), white = true)
        HCellH(Vk250002Cols.XS[2], y4, Vk250002Cols.WIDTHS[2], Vk250002Cols.H_LEVEL4, "3", white = true)
        HCellH(Vk250002Cols.XS[3], y4, Vk250002Cols.WIDTHS[3], Vk250002Cols.H_LEVEL4, "G", white = true)
        HCellH(Vk250002Cols.XS[4], y4, Vk250002Cols.WIDTHS[4], Vk250002Cols.H_LEVEL4, "5", white = true)
        HCellH(Vk250002Cols.XS[5], y4, Vk250002Cols.WIDTHS[5], Vk250002Cols.H_LEVEL4, tr(R.string.msg_sn_835d66a), white = true)
        HCellH(Vk250002Cols.XS[6], y4, Vk250002Cols.WIDTHS[6], Vk250002Cols.H_LEVEL4, tr(R.string.msg_so_a921cdf), white = true)
        HCellH(Vk250002Cols.XS[7], y4, Vk250002Cols.WIDTHS[7], Vk250002Cols.H_LEVEL4, "8", white = true)
        HCellH(Vk250002Cols.XS[8], y4, Vk250002Cols.WIDTHS[8], Vk250002Cols.H_LEVEL4, "B", white = true)
        HCellH(Vk250002Cols.XS[9], y4, Vk250002Cols.WIDTHS[9], Vk250002Cols.H_LEVEL4, "10", white = true)
        HCellH(Vk250002Cols.XS[10], y4, Vk250002Cols.WIDTHS[10], Vk250002Cols.H_LEVEL4, "11", white = true)
        HCellH(Vk250002Cols.XS[11], y4, Vk250002Cols.WIDTHS[11], Vk250002Cols.H_LEVEL4, "12", white = true)
        HCellH(Vk250002Cols.XS[12], y4, Vk250002Cols.WIDTHS[12], Vk250002Cols.H_LEVEL4, "13", white = true)
        HCellH(Vk250002Cols.XS[13], y4, Vk250002Cols.WIDTHS[13], Vk250002Cols.H_LEVEL4, "D", white = true)
        HCellH(Vk250002Cols.XS[14], y4, Vk250002Cols.WIDTHS[14], Vk250002Cols.H_LEVEL4, "15", white = true)
        HCellH(Vk250002Cols.XS[15], y4, Vk250002Cols.WIDTHS[15], Vk250002Cols.H_LEVEL4, "E", white = true)
        HCellH(Vk250002Cols.XS[16], y4, Vk250002Cols.WIDTHS[16], Vk250002Cols.H_LEVEL4, "17", white = true)
        HCellH(Vk250002Cols.XS[17], y4, Vk250002Cols.WIDTHS[17], Vk250002Cols.H_LEVEL4, "H", white = true)
        HCellH(Vk250002Cols.XS[18], y4, Vk250002Cols.WIDTHS[18], Vk250002Cols.H_LEVEL4, tr(R.string.msg_so_h_e7084ac), white = true)
        HCellH(Vk250002Cols.XS[19], y4, Vk250002Cols.WIDTHS[19], Vk250002Cols.H_LEVEL4, "", white = true)
        HCellH(Vk250002Cols.XS[20], y4, Vk250002Cols.WIDTHS[20], Vk250002Cols.H_LEVEL4, "", white = true)
    }

    fun journalWorks(): List<PrintHeaderCell> = listOf(
        PrintHeaderCell(0f, 0f, 80f, 28f, tr(R.string.msg_date_a5b49d2)),
        PrintHeaderCell(80f, 0f, 150f, 28f, tr(R.string.journal_kind)),
        PrintHeaderCell(230f, 0f, 110f, 28f, tr(R.string.journal_counter)),
        PrintHeaderCell(340f, 0f, 380f, 28f, tr(R.string.journal_description))
    )

    fun journalDefects(): List<PrintHeaderCell> = listOf(
        PrintHeaderCell(0f, 0f, 80f, 28f, tr(R.string.msg_date_a5b49d2)),
        PrintHeaderCell(80f, 0f, 320f, 28f, tr(R.string.journal_defects)),
        PrintHeaderCell(400f, 0f, 120f, 28f, tr(R.string.journal_completed)),
        PrintHeaderCell(520f, 0f, 200f, 28f, tr(R.string.journal_scheduled_works))
    )
}
