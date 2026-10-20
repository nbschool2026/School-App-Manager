package org.nbschool.appmanager

import com.android.apksig.ApkVerifier
import java.io.File
import java.security.MessageDigest

fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

object ApkChecks {
    fun requireHash(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(65536)
            while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
        require(actual == expected) { "SHA-256 APK не збігається з каталогом" }
    }
    fun requireSignature(file: File, api: Int, expected: String) {
        val result = ApkVerifier.Builder(file).setMinCheckedPlatformVersion(api).setMaxCheckedPlatformVersion(api).build().verify()
        require(result.isVerified) { "Криптографічна перевірка підпису APK не пройдена" }
        require(result.signerCertificates.size == 1 && sha256(result.signerCertificates.single().encoded) == expected) {
            "Підпис APK не збігається з довіреним сертифікатом каталогу"
        }
    }
}
