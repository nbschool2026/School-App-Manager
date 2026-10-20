package org.nbschool.appmanager

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

class ApkChecksTest {
    private val fixture = File(requireNotNull(System.getProperty("apkFixture")))
    private val signer = "ae36dfbc8e8695fa3a76629802d3353e8122d5570295c85bd688877c4479598c"
    private val hash = "d56b303be16c2a5319c81d1cf31d029e23ede3144ff8b4ed2e730008b73ceaf1"
    @Test fun validApkPassesForAndroid9() {
        ApkChecks.requireHash(fixture, hash)
        ApkChecks.requireSignature(fixture, 28, signer)
    }
    @Test(expected = IllegalArgumentException::class) fun incorrectHashFails() {
        ApkChecks.requireHash(fixture, "0".repeat(64))
    }
    @Test(expected = IllegalArgumentException::class) fun unexpectedSignerFails() {
        ApkChecks.requireSignature(fixture, 28, "0".repeat(64))
    }
    @Test fun damagedApkFailsCryptographicVerification() {
        val damaged = File.createTempFile("damaged-", ".apk")
        try {
            fixture.copyTo(damaged, overwrite = true)
            RandomAccessFile(damaged, "rw").use { it.seek(64); val value = it.read(); it.seek(64); it.write(value xor 1) }
            assertTrue(runCatching { ApkChecks.requireSignature(damaged, 28, signer) }.isFailure)
        } finally { damaged.delete() }
    }
}
