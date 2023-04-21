package mega.privacy.android.data.mapper.shares

import com.google.common.truth.Truth
import nz.mega.sdk.MegaShare
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock

class ShareDataMapperTest {

    private val accessPermissionMapper = AccessPermissionMapper()
    private val underTest = ShareDataMapper(accessPermissionMapper)

    @Test
    fun `test that user is mapped to user`() {
        val expected = "email"
        val megaShare = mock<MegaShare> {
            on { user }.thenReturn(expected)
        }
        val actual = underTest(megaShare).user
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "isPending: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is pending is mapped to is pending`(expected: Boolean) {
        val megaShare = mock<MegaShare> {
            on { isPending }.thenReturn(expected)
        }
        val actual = underTest(megaShare).isPending
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that node handle is mapped to node handle`() {
        val expected = 123L
        val megaShare = mock<MegaShare> {
            on { nodeHandle }.thenReturn(expected)
        }
        val actual = underTest(megaShare).nodeHandle
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that node time stamp is mapped to time stamp`() {
        val expected = 123L
        val megaShare = mock<MegaShare> {
            on { timestamp }.thenReturn(expected)
        }
        val actual = underTest(megaShare).timeStamp
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "access: {0}")
    @ValueSource(ints = [MegaShare.ACCESS_UNKNOWN, MegaShare.ACCESS_READ, MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER])
    fun `test that access is mapped to access`(expected: Int) {
        val megaShare = mock<MegaShare> {
            on { access }.thenReturn(expected)
        }
        val actual = underTest(megaShare).access
        Truth.assertThat(actual).isEqualTo(accessPermissionMapper(expected))
    }

    @ParameterizedTest(name = "isVerified: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is verified is mapped to is verified`(expected: Boolean) {
        val megaShare = mock<MegaShare> {
            on { isPending }.thenReturn(expected)
        }
        val actual = underTest(megaShare).isPending
        Truth.assertThat(actual).isEqualTo(expected)
    }
}