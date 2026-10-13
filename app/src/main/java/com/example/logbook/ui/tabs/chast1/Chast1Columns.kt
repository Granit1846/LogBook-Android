package com.example.logbook.ui.tabs.chast1

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Chast1Cols {
    // ============================================================
    // Ширины колонок
    // ============================================================
    val W0_DATE: Dp = 70.dp
    val W1_TYPE: Dp = 25.dp
    val W2_GROUND: Dp = 60.dp
    val W3_AIR: Dp = 60.dp
    val W4_C5: Dp = 65.dp
    val W5_CSNE: Dp = 70.dp
    val W6_C7: Dp = 60.dp
    val W7_C8: Dp = 40.dp
    val W8_C9: Dp = 40.dp
    val W9_LAND: Dp = 60.dp
    val W10_C11: Dp = 40.dp
    val W11_LAND2: Dp = 40.dp
    val W12_PILOT: Dp = 80.dp
    val W13_MECH: Dp = 80.dp
    val W14_C15: Dp = 40.dp
    val W15_ACT: Dp = 40.dp

    val WIDTHS = listOf(
        W0_DATE, W1_TYPE, W2_GROUND, W3_AIR,
        W4_C5, W5_CSNE, W6_C7, W7_C8, W8_C9,
        W9_LAND, W10_C11, W11_LAND2,
        W12_PILOT, W13_MECH, W14_C15, W15_ACT
    )

    val TOTAL_WIDTH: Dp = WIDTHS.fold(0.dp) { acc, w -> acc + w }

    val XS: List<Dp> = run {
        val result = mutableListOf<Dp>()
        var acc = 0.dp
        for (w in WIDTHS) {
            result.add(acc)
            acc += w
        }
        result
    }

    // ============================================================
    // Высоты уровней шапки
    // ============================================================
    val H_LEVEL1: Dp = 25.dp
    val H_LEVEL2: Dp = 30.dp
    val H_LEVEL3: Dp = 30.dp
    val H_LEVEL4: Dp = 80.dp
    val H_LEVEL5: Dp = 20.dp

    val HEADER_HEIGHT: Dp = H_LEVEL1 + H_LEVEL2 + H_LEVEL3 + H_LEVEL4 + H_LEVEL5
    val ROW_HEIGHT: Dp = 26.dp

    // ============================================================
    // Цвета — вынесены в formularColors() (зависит от темы).
    // Оставлены для обратной совместимости @Preview и старых ссылок.
    // ============================================================
    val HeaderColor: Color = Color(0xFFD6D6D6)
    val WhiteRowColor: Color = Color(0xFFEDEDED)
    val DataRowColor: Color = Color(0xFFFFFFFF)
    val TotalRowColor: Color = Color(0xFFDCE8F5)
    val GridColor: Color = Color(0xFF808080)
    val ErrorColor: Color = Color(0xFFB00020)
}