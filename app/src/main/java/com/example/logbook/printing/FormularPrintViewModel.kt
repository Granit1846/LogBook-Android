package com.example.logbook.printing

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logbook.data.local.AppDatabase
import kotlinx.coroutines.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FormularPrintViewModel(private val context: Context, db: AppDatabase, private val profileId: Long) : ViewModel() {
    private val repository = FormularPrintRepository(db)
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf<String?>(null); private set
    private var prepared: File? = null

    fun prepare(options: PrintOptions, a3: Boolean, save: Boolean, ready: (PrintSnapshot, String) -> Unit) {
        if (busy) return
        busy = true; message = null
        viewModelScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { repository.snapshot(profileId, options) }
                val filename = "LogBook_${profileId}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.pdf"
                if (save) withContext(Dispatchers.IO) {
                    prepared?.delete()
                    val file = File.createTempFile("formular_", ".pdf", context.cacheDir)
                    prepared = file
                    file.outputStream().use { FormularPdf.write(snapshot, it, if (a3) 1191 else 842, if (a3) 842 else 595) }
                }
                ready(snapshot, filename)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                prepared?.delete(); prepared = null
                message = error.message ?: tr(R.string.msg_could_not_prepare_logbooks_8352370)
            } finally { busy = false }
        }
    }

    fun save(uri: Uri?) {
        val file = prepared
        if (uri == null) { file?.delete(); prepared = null; return }
        if (file == null) { message = tr(R.string.msg_prepare_the_pdf_again_d91f198); return }
        busy = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    checkNotNull(context.contentResolver.openOutputStream(uri, "w")) { tr(R.string.msg_could_not_open_the_file_651ac1a) }
                        .use { output -> file.inputStream().use { it.copyTo(output) } }
                }
                message = tr(R.string.msg_pdf_saved_671d8cb)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) { message = error.message ?: tr(R.string.msg_could_not_save_the_pdf_8ae0e0c)
            } finally { file.delete(); prepared = null; busy = false }
        }
    }
    override fun onCleared() { prepared?.delete() }
}
