package com.example.logbook.data.exchange

import com.example.logbook.R
import com.example.logbook.localization.I18n.tr

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

data class RemoteCheckpoint(val revision: Long, val payload: String?)
data class CommitReply(val accepted: Boolean, val revision: Long)
interface SyncRemote {
    suspend fun pull(): RemoteCheckpoint
    suspend fun commit(change: String, expected: Long, payload: String): CommitReply
}
/** The local side of an exchange; [SyncStore] on the device, a fake in tests. */
interface SyncLocal {
    suspend fun checkpoint(endpoint: String, account: String): com.example.logbook.data.local.entity.SyncStateEntity
    suspend fun saveCheckpoint(checkpoint: com.example.logbook.data.local.entity.SyncStateEntity)
    suspend fun snapshot(): String
    /** Replaces local data with [payload] and returns the backup path. */
    suspend fun download(payload: String, expectedLocal: String, revision: Long, endpoint: String, account: String): String
}
data class SyncConflict(val local: String, val remote: String, val revision: Long)
data class SyncResult(val message: String, val conflict: SyncConflict? = null, val downloaded: Boolean = false,
    val direction: String = "Без изменений", val backup: String = "")

class SyncEngine(private val store: SyncLocal,private val remote: SyncRemote,private val endpoint: String,private val account: String) {
    companion object { private val lock=Mutex() }
    suspend fun synchronize(): SyncResult = lock.withLock {
        var state=store.checkpoint(endpoint,account)
        if(state.pendingId.isNotBlank() && state.pendingJson.isNotBlank()) {
            if(!state.pendingJson.contains("\"LogBookExchange\"")) {
                store.saveCheckpoint(state.copy(pendingId="",pendingJson="",pendingRevision=0))
            } else {
                try {
                    val reply=remote.commit(state.pendingId,state.pendingRevision,state.pendingJson)
                    if(reply.accepted) acknowledge(reply.revision,state.pendingJson)
                    else store.saveCheckpoint(store.checkpoint(endpoint,account).copy(pendingId="",pendingJson="",pendingRevision=0))
                } catch(error: Exception) {
                    if(error.message?.contains("Invalid sync packet", ignoreCase = true) == true) {
                        store.saveCheckpoint(store.checkpoint(endpoint,account).copy(pendingId="",pendingJson="",pendingRevision=0))
                    }
                    throw error
                }
            }
        } else if(state.pendingId.isNotBlank()) {
            store.saveCheckpoint(state.copy(pendingId="",pendingJson="",pendingRevision=0))
        }
        state=store.checkpoint(endpoint,account)
        val cloud=remote.pull()
        require(cloud.revision>=0 && ((cloud.revision==0L)==(cloud.payload==null))) { tr(R.string.msg_invalid_server_response_3ecf8d7) }
        val local=store.snapshot()
        if(cloud.revision==0L) {
            require(state.baseRevision==0L) { tr(R.string.msg_the_previous_server_version_is_missing_automat_fbb1f61) }
            return@withLock upload(local,0)
        }
        val payload=cloud.payload!!
        if(SyncStore.same(local,payload)) { acknowledge(cloud.revision,payload);return@withLock SyncResult(tr(R.string.msg_data_matches_synchronization_completed_ed8e867)) }
        if(SyncStore.same(local,state.baseJson) || (state.baseRevision==0L && !hasData(local))) {
            val backup=store.download(payload,local,cloud.revision,endpoint,account)
            return@withLock SyncResult(tr(R.string.msg_data_downloaded_from_the_server_5eea9c2),downloaded=true,direction="Получено",backup=backup)
        }
        if(cloud.revision==state.baseRevision && SyncStore.same(payload,state.baseJson)) return@withLock upload(local,cloud.revision)
        SyncResult(tr(R.string.msg_both_devices_have_changes_5cca551),SyncConflict(local,payload,cloud.revision))
    }
    private fun hasData(payload: String): Boolean = com.google.gson.JsonParser.parseString(payload).asJsonObject
        .getAsJsonObject("tables").entrySet().any { it.value.asJsonArray.size()>0 }
    suspend fun resolve(conflict: SyncConflict, useLocal: Boolean): SyncResult = lock.withLock {
        val local=store.snapshot();require(SyncStore.same(local,conflict.local)) { tr(R.string.msg_local_data_changed_retry_synchronization_1202ff5) }
        val fresh=remote.pull()
        if(fresh.revision!=conflict.revision || !SyncStore.same(fresh.payload.orEmpty(),conflict.remote))
            return@withLock SyncResult(tr(R.string.msg_the_server_version_changed_review_the_differen_9d5d72e),fresh.payload?.let { SyncConflict(local,it,fresh.revision) })
        if(useLocal) upload(local,fresh.revision)
        else {
            val backup=store.download(conflict.remote,local,conflict.revision,endpoint,account)
            SyncResult(tr(R.string.msg_server_version_selected_previous_data_was_save_b8c6e99),downloaded=true,direction="Получено",backup=backup)
        }
    }
    private suspend fun upload(payload: String,expected: Long): SyncResult {
        val id=UUID.randomUUID().toString()
        store.saveCheckpoint(store.checkpoint(endpoint,account).copy(pendingId=id,pendingJson=payload,pendingRevision=expected))
        val reply=remote.commit(id,expected,payload)
        if(reply.accepted) {
            acknowledge(reply.revision,payload)
            val changed=!SyncStore.same(store.snapshot(),payload)
            return SyncResult(if(changed) tr(R.string.msg_version_uploaded_new_local_edits_will_be_sent__60a4b22) else tr(R.string.msg_data_uploaded_synchronization_completed_dec2f83),direction="Отправлено")
        }
        store.saveCheckpoint(store.checkpoint(endpoint,account).copy(pendingId="",pendingJson="",pendingRevision=0))
        val fresh=remote.pull()
        return SyncResult(tr(R.string.msg_the_server_version_changed_0c2d195),fresh.payload?.let { SyncConflict(store.snapshot(),it,fresh.revision) })
    }
    private suspend fun acknowledge(revision: Long,payload: String) {
        require(revision>0) { tr(R.string.msg_invalid_server_revision_ddf52bb) }
        store.saveCheckpoint(store.checkpoint(endpoint,account).copy(baseRevision=revision,baseJson=payload,pendingId="",pendingJson="",pendingRevision=0,lastSuccess=System.currentTimeMillis()))
    }
}
