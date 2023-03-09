package mega.privacy.android.data.mapper.shares

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.shares.AccessPermission
import nz.mega.sdk.MegaShare
import org.junit.Test

class AccessPermissionMapperImplTest {
    private val underTest: AccessPermissionMapper = AccessPermissionMapperImpl()

    @Test
    fun `test read access is mapped correctly`() {
        test(AccessPermission.READ, MegaShare.ACCESS_READ)
    }

    @Test
    fun `test read write access is mapped correctly`() {
        test(AccessPermission.READWRITE, MegaShare.ACCESS_READWRITE)
    }

    @Test
    fun `test full access is mapped correctly`() {
        test(AccessPermission.FULL, MegaShare.ACCESS_FULL)
    }

    @Test
    fun `test owner access is mapped correctly`() {
        test(AccessPermission.OWNER, MegaShare.ACCESS_OWNER)
    }

    @Test
    fun `test unknown is mapped correctly`() {
        test(AccessPermission.UNKNOWN, -1)
        test(AccessPermission.UNKNOWN, 10)
    }

    private fun test(expected: AccessPermission, raw: Int) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }
}