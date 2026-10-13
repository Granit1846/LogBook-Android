package com.example.logbook.ui.tabs.ta14

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Ta14Cols {
    // ============================================================
    // Ширины колонок (12 колонок)
    // ============================================================
    val W0_DATE: Dp = 70.dp
    val W1_LAUNCH: Dp = 60.dp
    val W2_C3: Dp = 60.dp
    val W3_VREMYA: Dp = 60.dp
    val W4_C5: Dp = 70.dp
    val W5_CSNE: Dp = 70.dp
    val W6_C7: Dp = 70.dp
    val W7_C8: Dp = 60.dp
    val W8_CSNE2: Dp = 70.dp
    val W9_LAUNCH2: Dp = 60.dp
    val W10_C11: Dp = 70.dp
    val W11_ACT: Dp = 30.dp

    val WIDTHS = listOf(
        W0_DATE, W1_LAUNCH, W2_C3, W3_VREMYA, W4_C5,
        W5_CSNE, W6_C7, W7_C8, W8_CSNE2, W9_LAUNCH2,
        W10_C11, W11_ACT
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
    // Высоты уровней шапки (2 уровня + белая)
    // ============================================================
    val H_LEVEL1: Dp = 50.dp   // все длинные подписи
    val H_LEVEL2: Dp = 80.dp    // "после последнего кап. ремонта" / "с начала эксплуатации"
    val H_LEVEL3: Dp = 20.dp    // белая строка

    val HEADER_HEIGHT: Dp = H_LEVEL1 + H_LEVEL2 + H_LEVEL3
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