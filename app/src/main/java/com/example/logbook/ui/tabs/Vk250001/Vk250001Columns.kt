package com.example.logbook.ui.tabs.vk250001

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Vk250001Cols {
    // ============================================================
    // Ширины колонок (21 колонка)
    // ============================================================
    val W0_DATE: Dp = 70.dp
    val W1_I: Dp = 60.dp
    val W2_EMPTY: Dp = 30.dp
    val W3_G: Dp = 55.dp
    val W4_EMPTY: Dp = 30.dp
    val W5_SNE: Dp = 70.dp
    val W6_C7: Dp = 70.dp
    val W7_C8: Dp = 50.dp
    val W8_B: Dp = 50.dp
    val W9_C10: Dp = 50.dp
    val W10_C11: Dp = 30.dp
    val W11_C12: Dp = 50.dp
    val W12_EMPTY: Dp = 30.dp
    val W13_D: Dp = 50.dp
    val W14_EMPTY: Dp = 30.dp
    val W15_E: Dp = 60.dp
    val W16_EMPTY: Dp = 30.dp
    val W17_H: Dp = 60.dp
    val W18_C19: Dp = 60.dp
    val W19_SIGN: Dp = 50.dp
    val W20_ACT: Dp = 30.dp

    val WIDTHS = listOf(
        W0_DATE, W1_I, W2_EMPTY, W3_G, W4_EMPTY,
        W5_SNE, W6_C7, W7_C8, W8_B, W9_C10,
        W10_C11, W11_C12, W12_EMPTY, W13_D, W14_EMPTY,
        W15_E, W16_EMPTY, W17_H, W18_C19,
        W19_SIGN, W20_ACT
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
    // Высоты уровней шапки (3 уровня + белая)
    // ============================================================
    val H_LEVEL1: Dp = 30.dp
    val H_LEVEL2: Dp = 60.dp
    val H_LEVEL3: Dp = 90.dp
    val H_LEVEL4: Dp = 20.dp   // белая строка

    val HEADER_HEIGHT: Dp = H_LEVEL1 + H_LEVEL2 + H_LEVEL3 + H_LEVEL4
    val ROW_HEIGHT: Dp = 26.dp

    val V_FONT_SIZE = 10
    val V_LINE_HEIGHT = 11

    // Цвета
    val HeaderColor: Color = Color(0xFFD6D6D6)
    val WhiteRowColor: Color = Color(0xFFEDEDED)
    val DataRowColor: Color = Color(0xFFFFFFFF)
    val TotalRowColor: Color = Color(0xFFDCE8F5)
    val GridColor: Color = Color(0xFF808080)
    val ErrorColor: Color = Color(0xFFB00020)
}