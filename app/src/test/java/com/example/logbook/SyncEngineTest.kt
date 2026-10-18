package com.example.logbook

import com.example.logbook.data.exchange.CommitReply
import com.example.logbook.data.exchange.RemoteCheckpoint
import com.example.logbook.data.exchange.SyncEngine
import com.example.logbook.data.exchange.SyncLocal
import com.example.logbook.data.exchange.SyncRemote
import com.example.logbook.data.local.entity.SyncStateEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SyncEngineTest {
    private fun data(vararg days: String) =
        """{"format":"LogBookExchange","tables":{"days":[${days.joinToString(",") { """{"id":1,"date":"$it"}""" }}]}}"""

    private class Cloud(var revision: Long = 0, var payload: String? = null) : SyncRemote {
        var commits = 0
        override suspend fun pull() = RemoteCheckpoint(revision, payload)
        override suspend fun commit(change: String, expected: Long, payload: String): CommitReply {
            commits++
            if (expected != revision) return CommitReply(false, revision)
            revision++; this.payload = payload
            return CommitReply(true, revision)
        }
    }

    private class Device(var data: String) : SyncLocal {
        var state = SyncStateEntity(endpoint = "e", accountId = "a")
        override suspend fun checkpoint(endpoint: String, account: String) = state
        override suspend fun saveCheckpoint(checkpoint: SyncStateEntity) { state = checkpoint }
        override suspend fun snapshot() = data
        override suspend fun download(payload: String, expectedLocal: String, revision: Long, endpoint: String, account: String): String {
            data = payload
            state = state.copy(baseRevision = revision, baseJson = payload, pendingId = "", pendingJson = "")
            return "backup"
        }
    }

    private fun sync(device: Device, cloud: Cloud) = runBlocking { SyncEngine(device, cloud, "e", "a").synchronize() }

    @Test fun firstSyncUploadsToEmptyCloud() {
        val cloud = Cloud(); val phone = Device(data("2026-10-01"))
        sync(phone, cloud)
        assertEquals(1L, cloud.revision)
        assertEquals(phone.data, cloud.payload)
        assertEquals(1L, phone.state.baseRevision)
    }

    @Test fun unchangedDeviceDownloadsAnotherDevicesChange() {
        val cloud = Cloud(); val phone = Device(data("2026-10-01")); val pc = Device("")
        sync(phone, cloud)
        pc.data = data("2026-10-01"); pc.state = phone.state
        pc.data = data("2026-10-01", "2026-10-02"); sync(pc, cloud)
        val result = sync(phone, cloud)
        assertTrue(result.downloaded)
        assertEquals(cloud.payload, phone.data)
    }

    @Test fun localEditUploadsWhenCloudIsUnchanged() {
        val cloud = Cloud(); val phone = Device(data("2026-10-01"))
        sync(phone, cloud)
        phone.data = data("2026-10-01", "2026-10-03")
        sync(phone, cloud)
        assertEquals(2L, cloud.revision)
        assertEquals(phone.data, cloud.payload)
    }

    @Test fun editsOnBothSidesBecomeConflictAndChangeNothing() {
        val cloud = Cloud(); val phone = Device(data("2026-10-01"))
        sync(phone, cloud)
        cloud.revision = 2; cloud.payload = data("2026-10-01", "2026-10-05")
        phone.data = data("2026-10-01", "2026-10-04")
        val result = sync(phone, cloud)
        assertNotNull(result.conflict)
        assertEquals(data("2026-10-01", "2026-10-04"), phone.data)
        assertEquals(data("2026-10-01", "2026-10-05"), cloud.payload)
    }

    @Test fun unsentUploadIsRetriedOnNextSync() {
        val cloud = Cloud(); val phone = Device(data("2026-10-01"))
        sync(phone, cloud)
        // The app was killed after saving the pending upload but before the server answered.
        val edited = data("2026-10-01", "2026-10-06")
        phone.data = edited
        phone.state = phone.state.copy(pendingId = "p1", pendingJson = edited, pendingRevision = 1)
        sync(phone, cloud)
        assertEquals(edited, cloud.payload)
        assertEquals("", phone.state.pendingId)
    }
}
