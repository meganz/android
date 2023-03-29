package mega.privacy.android.data.mapper.shares

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaShare
import org.junit.Test

class AccessPermissionIntMapperTest {
    private val underTest: AccessPermissionIntMapper = AccessPermissionIntMapper()

    @Test
    fun `test read access is mapped correctly`() {
        test(MegaShare.ACCESS_READ, AccessPermission.READ)
    }

    @Test
    fun `test read write access is mapped correctly`() {
        test(MegaShare.ACCESS_READWRITE, AccessPermission.READWRITE)
    }

    @Test
    fun `test full access is mapped correctly`() {
        test(MegaShare.ACCESS_FULL, AccessPermission.FULL)
    }

    @Test
    fun `test owner access is mapped correctly`() {
        test(MegaShare.ACCESS_OWNER, AccessPermission.OWNER)
    }

    @Test
    fun `test unknown is mapped correctly`() {
        test(MegaShare.ACCESS_UNKNOWN, AccessPermission.UNKNOWN)
    }

    private fun test(expected: Int, accessPerm: AccessPermission) {
        val actual = underTest(accessPerm)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}