package org.nbschool.appmanager

import java.net.URL
import javax.net.ssl.HttpsURLConnection

object HttpsDownloads {
    fun checked(url: URL): URL {
        require(url.protocol == "https" && url.host.isNotBlank() && url.userInfo == null) { "Завантаження дозволено лише через HTTPS без облікових даних у URL" }
        return url
    }
    fun redirectTarget(current: URL, location: String): URL {
        require(location.isNotBlank()) { "Сервер не вказав адресу перенаправлення" }
        return checked(URL(current, location))
    }
    fun open(address: String, checkContinue: () -> Unit): HttpsURLConnection {
        var url = checked(URL(address))
        repeat(6) { hop ->
            checkContinue()
            val connection = url.openConnection() as HttpsURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept-Encoding", "identity")
            try {
                val code = connection.responseCode
                if (code == 200) return connection
                require(code in setOf(301, 302, 303, 307, 308)) { "APK: HTTP $code" }
                require(hop < 5) { "Забагато перенаправлень APK (максимум 5)" }
                url = redirectTarget(url, connection.getHeaderField("Location") ?: "")
            } catch (error: Throwable) {
                connection.disconnect()
                throw error
            }
            // Each hop opens a fresh connection: do not forward authentication headers.
            connection.disconnect()
        }
        error("Не вдалося відкрити APK")
    }
}
