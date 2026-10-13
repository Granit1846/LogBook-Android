package com.example.logbook.ui.tabs.vr80

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Vr80Cols {
    // ============================================================
    // Ширины колонок (15 колонок)
    // ============================================================
    val W0_DATE: Dp = 70.dp
    val W1_LAUNCH: Dp = 50.dp
    val W2_GROUND: Dp = 50.dp
    val W3_C4: Dp = 40.dp
    val W4_C5: Dp = 40.dp
    val W5_TP: Dp = 50.dp
    val W6_AIR: Dp = 50.dp
    val W7_C8: Dp = 40.dp
    val W8_C9: Dp = 40.dp
    val W9_C24: Dp = 85.dp
    val W10_CSNE: Dp = 85.dp
    val W11_C26: Dp = 70.dp
    val W12_C27: Dp = 70.dp
    val W13_C28: Dp = 60.dp
    val W14_ACT: Dp = 30.dp

    val WIDTHS = listOf(
        W0_DATE, W1_LAUNCH, W2_GROUND, W3_C4, W4_C5,
        W5_TP, W6_AIR, W7_C8, W8_C9,
        W9_C24, W10_CSNE, W11_C26, W12_C27, W13_C28,
        W14_ACT
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
    val H_LEVEL1: Dp = 40.dp
    val H_LEVEL2: Dp = 30.dp
    val H_LEVEL3: Dp = 30.dp
    val H_LEVEL4: Dp = 60.dp
    val H_LEVEL5: Dp = 20.dp   // белая строка

    val HEADER_HEIGHT: Dp = H_LEVEL1 + H_LEVEL2 + H_LEVEL3 + H_LEVEL4 + H_LEVEL5
    val ROW_HEIGHT: Dp = 26.dp

    // Шрифт
    val V_FONT_SIZE = 10
    val V_LINE_HEIGHT = 11

    // ============================================================
    // Цвета
    // ============================================================
    val HeaderColor: Color = Color(0xFFD6D6D6)
    val WhiteRowColor: Color = Color(0xFFEDEDED)
    val DataRowColor: Color = Color(0xFFFFFFFF)
    val TotalRowColor: Color = Color(0xFFDCE8F5)
    val GridColor: Color = Color(0xFF808080)
    val ErrorColor: Color = Color(0xFFB00020)
}