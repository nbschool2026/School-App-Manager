package org.nbschool.appmanager

import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Test

class HttpsDownloadsTest {
    @Test fun relativeRedirect() {
        assertEquals("https://example.org/files/app.apk", HttpsDownloads.redirectTarget(URL("https://example.org/releases/start"), "/files/app.apk").toString())
    }
    @Test fun cdnRedirect() {
        assertEquals("cdn.example.org", HttpsDownloads.redirectTarget(URL("https://example.org/app"), "https://cdn.example.org/file?token=abc").host)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsHttpDowngrade() {
        HttpsDownloads.redirectTarget(URL("https://example.org/app"), "http://example.org/app.apk")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsCredentials() {
        HttpsDownloads.redirectTarget(URL("https://example.org/app"), "https://user:password@example.org/app.apk")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsEmptyLocation() {
        HttpsDownloads.redirectTarget(URL("https://example.org/app"), "")
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsLocalFile() {
        HttpsDownloads.redirectTarget(URL("https://example.org/app"), "file:///data/app.apk")
    }
}
