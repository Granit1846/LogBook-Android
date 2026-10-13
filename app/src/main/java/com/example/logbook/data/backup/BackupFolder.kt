package com.example.logbook.data.backup

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.OutputStream

/** The persisted SAF grant is device-specific and is not part of the portable backup. */
class BackupFolder(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val preferences = context.applicationContext.getSharedPreferences("backup_folder", Context.MODE_PRIVATE)

    fun hasAccess(): Boolean {
        val tree = savedTree() ?: return false
        return resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission && it.isWritePermission }
    }

    fun initialUri(): Uri = if (hasAccess()) preferences.getString("directory", null)?.let(Uri::parse)
        ?: DOCUMENTS_URI else DOCUMENTS_URI

    fun select(tree: Uri) {
        require(tree.authority == AUTHORITY) { tr(R.string.msg_choose_documents_in_your_device_s_internal_sto_f6ca1ae) }
        val documentId = DocumentsContract.getTreeDocumentId(tree)
        require(documentId in listOf("primary:", "primary:Documents", "primary:Documents/LogBook",
            "primary:Documents/LogBook/BackUp")) {
            tr(R.string.msg_choose_documents_or_documents_logbook_backup_i_5c191ca)
        }
        resolver.takePersistableUriPermission(tree,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val directory = ensureDirectory(tree)
        check(preferences.edit().putString("tree", tree.toString())
            .putString("directory", directory.toString()).commit()) { tr(R.string.msg_could_not_save_the_backup_folder_a1bcfed) }
    }

    suspend fun save(name: String, write: suspend (OutputStream) -> Unit) {
        check(hasAccess()) { tr(R.string.msg_access_to_documents_was_lost_choose_the_folder_ce8b75c) }
        val directory = ensureDirectory(checkNotNull(savedTree()))
        val document = DocumentsContract.createDocument(resolver, directory, "application/json", name)
            ?: error(tr(R.string.msg_could_not_create_a_file_in_1_s_de683de, PATH))
        try {
            resolver.openOutputStream(document, "wt").use { output ->
                checkNotNull(output) { tr(R.string.msg_could_not_open_the_backup_file_3f819a5) }
                write(output)
            }
        } catch (e: Exception) {
            // Never leave an incomplete file looking like a usable backup.
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
            throw e
        }
        preferences.edit().putString("directory", directory.toString()).apply()
    }

    private fun savedTree(): Uri? = preferences.getString("tree", null)?.let(Uri::parse)

    private fun ensureDirectory(tree: Uri): Uri {
        val id = DocumentsContract.getTreeDocumentId(tree)
        var parent = DocumentsContract.buildDocumentUriUsingTree(tree, id)
        val existing = id.substringAfter(':').split('/').filter { it.isNotEmpty() }
        listOf("Documents", "LogBook", "BackUp").drop(existing.size).forEach { name ->
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree,
                DocumentsContract.getDocumentId(parent))
            var found: Uri? = null
            resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == name) {
                        check(cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR) {
                            tr(R.string.msg_1_s_exists_but_is_not_a_folder_581eef9, name)
                        }
                        found = DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0))
                        break
                    }
                }
            } ?: error(tr(R.string.msg_could_not_read_the_documents_folder_728abe9))
            parent = found ?: DocumentsContract.createDocument(resolver, parent,
                DocumentsContract.Document.MIME_TYPE_DIR, name) ?: error(tr(R.string.msg_could_not_create_folder_1_s_89d8d27, name))
        }
        return parent
    }

    companion object {
        const val PATH = "Documents/LogBook/BackUp/"
        private const val AUTHORITY = "com.android.externalstorage.documents"
        val DOCUMENTS_URI: Uri = DocumentsContract.buildDocumentUri(AUTHORITY, "primary:Documents")
    }
}
