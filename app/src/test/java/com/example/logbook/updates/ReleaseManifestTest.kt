package com.example.logbook.updates

import org.junit.Assert.*
import org.junit.Test

class ReleaseManifestTest {
    private fun json(code: String = "2", url: String = "", extra: String = "") =
        """{"version":"1.1.0","versionCode":$code,"url":"$url","notes":"Исправления"$extra}"""
    @Test fun metadataWithoutDownloadIsValid() {
        val release = ReleaseManifest.parse(json())
        assertEquals(2L, release.versionCode)
        assertEquals("", release.url)
    }
    @Test fun httpsDownloadIsValid() {
        assertEquals("https://example.com/app.apk", ReleaseManifest.parse(json(url = "https://example.com/app.apk")).url)
    }
    @Test fun invalidBuildsAndUnsafeLinksAreRejected() {
        for (code in listOf("0", "-1", "2.5", "\"abc\"")) assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(json(code = code)) }
        for (url in listOf("http://example.com/a.apk", "file:///a.apk", "https://user:pass@example.com/a.apk"))
            assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(json(url = url)) }
    }
    @Test fun wrongApplicationIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ReleaseManifest.parse(json(extra = ",\"applicationId\":\"other.app\"")) }
    }
}
