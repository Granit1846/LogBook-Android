package com.example.logbook.printing

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.CancellationSignal
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import java.io.OutputStream
import kotlin.math.*

data class PrintPage(val table: PrintTable, val rows: List<PrintRow>, val heights: List<Float>)

object FormularPdf {
    private fun textPaint(size: Float = 10f, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; textSize = size
        typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
    }
    private fun layout(text: String, width: Float, size: Float = 10f, bold: Boolean = false): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, textPaint(size, bold), max(1, (width - 6).toInt()))
            .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build()

    fun pages(snapshot: PrintSnapshot, width: Int = 842, height: Int = 595, margin: Float = 24f): List<PrintPage> = buildList {
        require(width > margin * 2 + 100 && height > margin * 2 + 150)
        snapshot.tables.forEach { table ->
            require(table.widths.isNotEmpty() && table.widths.all { it > 0 })
            val scale = (width - margin * 2) / table.widths.sum()
            val available = (height - margin * 2 - 58) / scale - table.headerHeight
            require(available > 26) { tr(R.string.msg_the_selected_paper_size_is_too_small_for_this__5329bb8) }
            var rows = mutableListOf<PrintRow>(); var heights = mutableListOf<Float>(); var used = 0f
            table.rows.forEach { row ->
                require(row.values.size == table.widths.size)
                val rowHeight = max(26f, row.values.mapIndexed { i, value -> layout(value, table.widths[i]).height + 8f }.max()) +
                    if (row.manual) 16f else 0f
                require(rowHeight <= available) { tr(R.string.msg_the_entry_is_too_long_1_s_choose_a_larger_pape_7e19395, row.date) }
                if (used + rowHeight > available && rows.isNotEmpty()) {
                    add(PrintPage(table, rows.toList(), heights.toList())); rows = mutableListOf(); heights = mutableListOf(); used = 0f
                }
                rows.add(row); heights.add(rowHeight); used += rowHeight
            }
            add(PrintPage(table, rows.toList(), heights.toList()))
        }
    }

    fun write(snapshot: PrintSnapshot, output: OutputStream, width: Int = 842, height: Int = 595,
        margin: Float = 24f, selectedPages: Set<Int>? = null, cancellation: CancellationSignal = CancellationSignal()) {
        val pages = pages(snapshot, width, height, margin)
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, page ->
                cancellation.throwIfCanceled()
                if (selectedPages != null && index !in selectedPages) return@forEachIndexed
                val pdfPage = document.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                try {
                val canvas = pdfPage.canvas
                canvas.drawColor(Color.WHITE)
                canvas.drawText("LogBook / ${page.table.title}", margin, margin + 14, textPaint(13f, true))
                val details = tr(R.string.msg_profile_1_s_aircraft_2_s_7bbbf87, snapshot.profileName, snapshot.boardNumber)
                drawText(canvas, details, margin, margin + 20, width - margin * 2, 22f, false, 10f)
                val scale = (width - margin * 2) / page.table.widths.sum()
                canvas.save(); canvas.translate(margin, margin + 46); canvas.scale(scale, scale)
                page.table.headers.forEach { cell ->
                    drawCell(canvas, cell.text, cell.x, cell.y, cell.width, cell.height, true,
                        if (cell.white) Color.WHITE else Color.rgb(226, 226, 226), cell.vertical)
                }
                var y = page.table.headerHeight
                if (page.rows.isEmpty()) {
                    drawCell(canvas, tr(R.string.msg_no_entries_for_the_selected_period_4309f88), 0f, y, page.table.widths.sum(), 30f, false, Color.WHITE)
                }
                page.rows.forEachIndexed { rowIndex, row ->
                    cancellation.throwIfCanceled()
                    val totalHeight = page.heights[rowIndex]
                    val cellsHeight = totalHeight - if (row.manual) 16f else 0f
                    var x = 0f
                    row.values.forEachIndexed { col, value ->
                        drawCell(canvas, value, x, y, page.table.widths[col], cellsHeight, row.isTotal,
                            if (row.isTotal || row.baseline) Color.rgb(243, 243, 243) else Color.WHITE)
                        x += page.table.widths[col]
                    }
                    if (row.manual) drawText(canvas, tr(R.string.msg_manual_recalculation_is_required_722f7f5), 0f, y + cellsHeight,
                        page.table.widths.sum(), 16f, true, 10f)
                    y += totalHeight
                }
                canvas.restore()
                canvas.drawText(tr(R.string.msg_page_1_s_2_s_0894db3, index + 1, pages.size), margin, height - margin + 2, textPaint(9f))
                } finally { document.finishPage(pdfPage) }
            }
            cancellation.throwIfCanceled()
            document.writeTo(output)
        } finally { document.close() }
    }

    private fun drawCell(canvas: Canvas, text: String, x: Float, y: Float, width: Float, height: Float,
        bold: Boolean, background: Int, vertical: Boolean = false) {
        val rect = RectF(x, y, x + width, y + height)
        canvas.drawRect(rect, Paint().apply { color = background })
        canvas.drawRect(rect, Paint().apply { color = Color.DKGRAY; style = Paint.Style.STROKE; strokeWidth = 0.5f })
        if (vertical) {
            canvas.save(); canvas.translate(x + width / 2, y + height / 2); canvas.rotate(-90f)
            drawText(canvas, text, -height / 2, -width / 2, height, width, bold, 10f)
            canvas.restore()
        } else drawText(canvas, text, x, y, width, height, bold, 10f)
    }

    private fun drawText(canvas: Canvas, text: String, x: Float, y: Float, width: Float, height: Float,
        bold: Boolean, initialSize: Float) {
        var size = initialSize
        var lines = layout(text, width, size, bold)
        while (lines.height > height - 4 && size > 5f) {
            size -= 0.5f; lines = layout(text, width, size, bold)
        }
        canvas.save(); canvas.clipRect(x + 1, y + 1, x + width - 1, y + height - 1)
        canvas.translate(x + 3, y + (height - lines.height) / 2); lines.draw(canvas); canvas.restore()
    }
}
