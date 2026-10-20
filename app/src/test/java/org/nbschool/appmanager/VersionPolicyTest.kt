package org.nbschool.appmanager
import org.junit.Assert.assertEquals
import org.junit.Test
class VersionPolicyTest {
    @Test fun missing() = assertEquals(AppStatus.MISSING, compareVersion(null, 20))
    @Test fun older() = assertEquals(AppStatus.UPDATE, compareVersion(19, 20))
    @Test fun equal() = assertEquals(AppStatus.CURRENT, compareVersion(20, 20))
    @Test fun newer() = assertEquals(AppStatus.CURRENT, compareVersion(21, 20))
    @Test fun longCodes() = assertEquals(AppStatus.UPDATE, compareVersion(3_000_000_000, 3_000_000_001))
}
