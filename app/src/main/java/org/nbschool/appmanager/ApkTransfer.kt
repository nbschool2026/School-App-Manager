package org.nbschool.appmanager

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipFile

object ApkTransfer {
    const val MAX_BYTES = 512L * 1024 * 1024
    fun download(context: Context, app: CatalogApp, cancel: AtomicBoolean, progress: (Long, Long) -> Unit): File {
        val partial = File(context.cacheDir, "download.part")
        val target = File(context.cacheDir, "verified.apk")
        target.delete()
        val deadline = android.os.SystemClock.elapsedRealtime() + 5 * 60 * 1000
        val connection = HttpsDownloads.open(app.apkUrl) {
            if (cancel.get() || Thread.currentThread().isInterrupted) throw CancellationException("Завантаження скасовано")
            require(android.os.SystemClock.elapsedRealtime() < deadline) { "Час завантаження вичерпано" }
        }
        try {
            val length = connection.contentLengthLong
            require(length <= MAX_BYTES) { "APK перевищує 512 МБ" }
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            var lastProgress = 0L
            connection.inputStream.use { input -> partial.outputStream().use { output ->
                val buffer = ByteArray(65536)
                while (true) {
                    if (cancel.get() || Thread.currentThread().isInterrupted) throw CancellationException("Завантаження скасовано")
                    require(android.os.SystemClock.elapsedRealtime() < deadline) { "Час завантаження вичерпано" }
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_BYTES) { "APK перевищує 512 МБ" }
                    output.write(buffer, 0, count); digest.update(buffer, 0, count)
                    val now = android.os.SystemClock.elapsedRealtime()
                    if (now - lastProgress > 200) { progress(total, length); lastProgress = now }
                }
            } }
            require(total > 0 && (length < 0 || total == length)) { "APK завантажено не повністю" }
            val hash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
            require(hash == app.sha256) { "SHA-256 APK не збігається з каталогом" }
            progress(total, total)
            verify(context, partial, app)
            if (cancel.get()) throw CancellationException("Завантаження скасовано")
            check(partial.renameTo(target)) { "Не вдалося зберегти перевірений APK" }
            return target
        } finally { connection.disconnect(); partial.delete() }
    }

    @Suppress("DEPRECATION")
    fun verify(context: Context, file: File, app: CatalogApp) {
        // Verify cryptographic integrity for the actual tablet's Android version.
        ApkChecks.requireHash(file, app.sha256)
        ApkChecks.requireSignature(file, Build.VERSION.SDK_INT, app.signerSha256)
        val info = context.packageManager.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNING_CERTIFICATES)
            ?: error("Не вдалося прочитати APK")
        require(info.packageName == app.packageName && info.longVersionCode == app.versionCode) { "packageName або versionCode APK не збігаються з каталогом" }
        require(info.applicationInfo!!.minSdkVersion <= Build.VERSION.SDK_INT) { "APK потребує новішої версії Android" }
        ZipFile(file).use { zip ->
            val nativeAbis = zip.entries().asSequence().map { it.name }.filter { it.startsWith("lib/") && it.endsWith(".so") }
                .map { it.split('/')[1] }.toSet()
            require(nativeAbis.isEmpty() || Build.SUPPORTED_ABIS.any { it in nativeAbis }) { "APK несумісний з архітектурою планшета" }
        }
        val installed = try { context.packageManager.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES) }
            catch (_: PackageManager.NameNotFoundException) { null }
        if (installed != null) {
            require(installed.longVersionCode < app.versionCode) { "На планшеті вже така сама або новіша версія" }
            // Fail closed on key rotation/multiple signers; no automatic uninstall.
            val signers = installed.signingInfo?.apkContentsSigners
            require(signers != null && signers.size == 1 && sha256(signers.single().toByteArray()) == app.signerSha256) {
                "Підпис установленого застосунку відрізняється. Оновлення заблоковано"
            }
        }
    }
}
