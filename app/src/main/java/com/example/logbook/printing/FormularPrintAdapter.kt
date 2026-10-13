package com.example.logbook.printing

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.os.*
import android.print.*
import kotlinx.coroutines.*

class FormularPrintAdapter(private val snapshot: PrintSnapshot) : PrintDocumentAdapter() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var width = 842
    private var height = 595
    private var margin = 24f
    private var count = 0

    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback, extras: Bundle?) {
        if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
        val media = newAttributes.mediaSize ?: PrintAttributes.MediaSize.ISO_A4.asLandscape()
        val nextWidth = (media.widthMils * 72f / 1000).toInt()
        val nextHeight = (media.heightMils * 72f / 1000).toInt()
        val margins = newAttributes.minMargins ?: PrintAttributes.Margins.NO_MARGINS
        val nextMargin = maxOf(24f, maxOf(margins.leftMils, margins.rightMils, margins.topMils, margins.bottomMils) * 72f / 1000)
        scope.launch {
            try {
                val pageCount = FormularPdf.pages(snapshot, nextWidth, nextHeight, nextMargin).size
                withContext(Dispatchers.Main) {
                    if (cancellationSignal.isCanceled) callback.onLayoutCancelled()
                    else {
                        width = nextWidth; height = nextHeight; margin = nextMargin; count = pageCount
                        callback.onLayoutFinished(PrintDocumentInfo.Builder("LogBook.pdf")
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(count).build(), oldAttributes != newAttributes)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                withContext(Dispatchers.Main) {
                    if (cancellationSignal.isCanceled) callback.onLayoutCancelled() else callback.onLayoutFailed(error.message)
                }
            }
        }
    }

    override fun onWrite(pages: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal,
        callback: WriteResultCallback) {
        val selected = (0 until count).filter { index -> pages.any { index >= it.start && index <= it.end } }.toSet()
        val pageWidth = width; val pageHeight = height; val pageMargin = margin
        scope.launch {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(destination).use {
                    FormularPdf.write(snapshot, it, pageWidth, pageHeight, pageMargin, selected, cancellationSignal)
                }
                withContext(Dispatchers.Main) { callback.onWriteFinished(selected.sorted().map { PageRange(it, it) }.toTypedArray()) }
            } catch (_: OperationCanceledException) {
                withContext(Dispatchers.Main) { callback.onWriteCancelled() }
            } catch (error: Exception) {
                withContext(Dispatchers.Main) { callback.onWriteFailed(error.message ?: tr(R.string.msg_could_not_prepare_printing_ba2044b)) }
            }
        }
    }
    override fun onFinish() { scope.cancel() }
}
