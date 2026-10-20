package org.nbschool.appmanager

import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class CatalogApp(val name: String, val packageName: String, val versionCode: Long,
    val versionName: String, val apkUrl: String, val sha256: String, val signerSha256: String)

enum class AppStatus(val label: String) {
    MISSING("Не встановлено"), UPDATE("Є оновлення"), CURRENT("Актуальна або новіша версія")
}

fun compareVersion(installed: Long?, offered: Long): AppStatus = when {
    installed == null -> AppStatus.MISSING
    installed < offered -> AppStatus.UPDATE
    else -> AppStatus.CURRENT
}

object Catalog {
    private fun https(value: String): URL = URL(value).also {
        require(it.protocol == "https" && it.host.isNotBlank() && it.userInfo == null) { "Потрібна HTTPS-адреса" }
    }

    fun load(address: String): List<CatalogApp> {
        val connection = https(address).openConnection() as HttpsURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = false
        try {
            require(connection.responseCode == 200) { "Сервер повернув HTTP ${connection.responseCode}" }
            val bytes = connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 1_048_576) { "Каталог завеликий" }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            return parse(bytes.toString(Charsets.UTF_8))
        } finally { connection.disconnect() }
    }

    fun parse(text: String): List<CatalogApp> {
        val root = JSONObject(text)
        require(root.getInt("schemaVersion") == 1) { "Невідома версія каталогу" }
        val apps = root.getJSONArray("apps")
        require(apps.length() <= 300) { "Забагато застосунків" }
        val seen = mutableSetOf<String>()
        return (0 until apps.length()).map { index ->
            val app = apps.getJSONObject(index)
            val pkg = app.getString("packageName")
            require(pkg.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")) && seen.add(pkg)) { "Некоректний або повторний packageName" }
            val version = app.getLong("versionCode")
            require(version > 0) { "Некоректний versionCode" }
            val url = app.getString("apkUrl"); https(url)
            val hash = app.getString("sha256").lowercase()
            val signer = app.getString("signerSha256").lowercase()
            require(listOf(hash, signer).all { it.matches(Regex("[0-9a-f]{64}")) }) { "Некоректний SHA-256" }
            CatalogApp(app.getString("name"), pkg, version, app.getString("versionName"), url, hash, signer)
        }
    }
}
