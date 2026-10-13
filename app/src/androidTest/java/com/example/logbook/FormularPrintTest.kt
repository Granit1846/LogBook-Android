package com.example.logbook

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.os.ParcelFileDescriptor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.logbook.data.local.AppDatabase
import com.example.logbook.data.local.entity.*
import com.example.logbook.printing.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class FormularPrintTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var db: AppDatabase
    @Before fun setup() { db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build() }
    @After fun cleanup() { db.close() }
    private suspend fun seed(count: Int = 70): Long {
        val id = db.profileDao().insert(ProfileEntity(name = "Проверка печати", boardNumber = "04008",
            aggregateAssignmentsJson = "{\"Chast1\":\"04008\",\"Vr80\":\"ВР-123\",\"Ta14\":\"ТА-456\",\"Vk250001\":\"ДВ-001\",\"Vk250002\":\"ДВ-002\"}"))
        db.chast1StartDao().insert(Chast1StartEntity(profileId = id, date = "01.07.2026", colSNE = 6000, pilot = "Начальные данные"))
        repeat(count) { index ->
            val date = LocalDate.of(2026, 8, 1).plusDays(index.toLong()).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            val order = index.toLong()
            db.chast1Dao().insert(Chast1Entity(profileId = id, date = date, sortOrder = order, flightType = "Облёт",
                groundHours = 25, flightDuration = 125, col5 = 7000 + index, colSNE = 8000 + index,
                landings = 5, pilot = if (index == 0) "Иванов И. И.\nПетров П. П." else "Иванов И. И.", mechanic = "Сидоров С. С."))
            db.vr80Dao().insert(Vr80Entity(profileId = id, date = date, sortOrder = order, launchCount = 2,
                groundHours = 25, col6 = 5, col7 = 125, col24 = 7200, colSNE = 8000 + index, col28 = "Сидоров С. С.", isHistorical = index == 0))
            db.ta14Dao().insert(Ta14Entity(profileId = id, date = date, sortOrder = order, launchCount = 3,
                colVremya = 5, colSNE = 8000 + index, colSNE2 = 7000, launchCount2 = 200))
            db.vk250001Dao().insert(Vk250001Entity(profileId = id, date = date, sortOrder = order, i = 3,
                g = 2.5, colSNE = 8000 + index, pprA = 6500, b = 3.4, h = 1.25, pprH = 4.55))
            db.vk250002Dao().insert(Vk250002Entity(profileId = id, date = date, sortOrder = order, i = 2,
                g = 2.5, colSNE = 8000 + index, b = 1.2, h = 2.25, pprH = 1.55))
        }
        return id
    }

    @Test fun snapshotPreservesAllFiveFormularsAndDatabase() = runBlocking {
        val id = seed(2)
        val before = db.chast1Dao().getByProfile(id)
        val snapshot = FormularPrintRepository(db).snapshot(id, PrintOptions(FORMULAR_TYPES.toSet()))
        assertEquals(5, snapshot.tables.size)
        assertEquals(listOf(16, 15, 12, 21, 21), snapshot.tables.map { it.widths.size })
        assertEquals("Иванов И. И.\nПетров П. П.", snapshot.tables[0].rows[1].values[12])
        assertEquals("1,3", snapshot.tables[3].rows[0].values[17])
        assertEquals("4,6", snapshot.tables[3].rows[0].values[18])
        assertTrue(snapshot.tables[1].title.contains("ВР-123"))
        assertTrue(snapshot.tables[0].rows.first().baseline)
        snapshot.tables.forEach { table -> table.rows.forEach { assertEquals(table.widths.size, it.values.size); assertEquals("", it.values.last()) } }
        assertEquals(before, db.chast1Dao().getByProfile(id))
        assertTrue(db.vr80Dao().getByProfile(id).first().isHistorical)
    }

    @Test fun periodDoesNotPrintMisleadingPartialMonthlyTotals() = runBlocking {
        val id = seed(2)
        db.chast1Dao().insert(Chast1Entity(profileId = id, date = "Итого август", sortOrder = 90,
            isTotal = true, totalMonth = "2026-08", needsManualRecalc = true))
        val repo = FormularPrintRepository(db)
        val partial = repo.snapshot(id, PrintOptions(setOf("Chast1"), LocalDate.of(2026,8,2), LocalDate.of(2026,8,10), false)).tables.single()
        assertEquals(1, partial.rows.size)
        assertFalse(partial.rows.single().isTotal)
        val full = repo.snapshot(id, PrintOptions(setOf("Chast1"), LocalDate.of(2026,8,1), LocalDate.of(2026,8,31), false)).tables.single()
        assertEquals(3, full.rows.size)
        assertTrue(full.rows.last().manual)
        assertTrue(full.rows.last().isTotal)
    }

    @Test fun paginationSelectedPagesCancellationAndRenderedArtifact() = runBlocking {
        val id = seed()
        val snapshot = FormularPrintRepository(db).snapshot(id, PrintOptions(FORMULAR_TYPES.toSet()))
        val pages = FormularPdf.pages(snapshot)
        assertTrue(pages.size > 5)
        snapshot.tables.forEach { table ->
            assertEquals(table.rows, pages.filter { it.table == table }.flatMap { it.rows })
            assertTrue(pages.count { it.table == table } > 1)
        }
        val directory = File(context.getExternalFilesDir(null), "print-verification").apply { mkdirs() }
        val pdf = File(directory, "all-formulars.pdf")
        pdf.outputStream().use { FormularPdf.write(snapshot, it) }
        PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertEquals(pages.size, renderer.pageCount)
            val firstPages = pages.mapIndexed { index, page -> page.table.title to index }.distinctBy { it.first }.map { it.second }
            (firstPages + pages.lastIndex).distinct().forEach { index ->
                renderer.openPage(index).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    File(directory, "page-${index + 1}.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
            }
        }
        val selected = File(context.cacheDir, "selected-test.pdf")
        try {
            selected.outputStream().use { FormularPdf.write(snapshot, it, selectedPages = setOf(1, pages.lastIndex)) }
            PdfRenderer(ParcelFileDescriptor.open(selected, ParcelFileDescriptor.MODE_READ_ONLY)).use { assertEquals(2, it.pageCount) }
        } finally { selected.delete() }
        val output = ByteArrayOutputStream()
        try {
            FormularPdf.write(snapshot, output, cancellation = CancellationSignal().apply { cancel() })
            fail("Cancelled PDF was written")
        } catch (_: OperationCanceledException) { assertEquals(0, output.size()) }
    }
    @Test fun systemPrintAdapterAsynchronouslyHonorsSelectedPages() = runBlocking {
        val snapshot = FormularPrintRepository(db).snapshot(seed(), PrintOptions(setOf("Chast1")))
        val adapter = FormularPrintAdapter(snapshot)
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val layoutLatch = java.util.concurrent.CountDownLatch(1)
        var layoutInfo: android.print.PrintDocumentInfo? = null
        var error: String? = null
        instrumentation.runOnMainSync {
            adapter.onLayout(null, android.print.PrintAttributes.Builder()
                .setMediaSize(android.print.PrintAttributes.MediaSize.ISO_A4.asLandscape()).build(), CancellationSignal(),
                object : android.print.TestLayoutCallback() {
                    override fun onLayoutFinished(info: android.print.PrintDocumentInfo, changed: Boolean) { layoutInfo = info; layoutLatch.countDown() }
                    override fun onLayoutFailed(message: CharSequence?) { error = message?.toString(); layoutLatch.countDown() }
                }, null)
        }
        assertTrue(layoutLatch.await(15, java.util.concurrent.TimeUnit.SECONDS))
        assertNull(error)
        assertTrue(layoutInfo!!.pageCount > 1)
        val file = File(context.cacheDir, "adapter-test.pdf")
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE)
        val writeLatch = java.util.concurrent.CountDownLatch(1)
        var written: Array<out android.print.PageRange>? = null
        try {
            instrumentation.runOnMainSync {
                adapter.onWrite(arrayOf(android.print.PageRange(1, 1)), descriptor, CancellationSignal(),
                    object : android.print.TestWriteCallback() {
                        override fun onWriteFinished(pages: Array<out android.print.PageRange>) { written = pages; writeLatch.countDown() }
                        override fun onWriteFailed(message: CharSequence?) { error = message?.toString(); writeLatch.countDown() }
                    })
            }
            assertTrue(writeLatch.await(15, java.util.concurrent.TimeUnit.SECONDS))
            assertNull(error)
            assertEquals(android.print.PageRange(1, 1), written!!.single())
            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { assertEquals(1, it.pageCount) }
        } finally { instrumentation.runOnMainSync { adapter.onFinish() }; descriptor.close(); file.delete() }
    }

}
