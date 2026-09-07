package mega.privacy.android.data.mapper.shares

import com.google.common.truth.Truth
import nz.mega.sdk.MegaNode
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
            on { isVerified }.thenReturn(expected)
        }
        val actual = underTest(megaShare).isVerified
        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that email is mapped to user for an incoming share node`() {
        val expected = "contact@mega.co.nz"

        val actual = underTest(
            email = expected,
            node = mock<MegaNode>(),
            access = MegaShare.ACCESS_READ,
            isVerified = true,
        ).user

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that node handle is mapped to node handle for an incoming share node`() {
        val expected = 123L
        val node = mock<MegaNode> {
            on { handle }.thenReturn(expected)
        }

        val actual = underTest(
            email = "contact@mega.co.nz",
            node = node,
            access = MegaShare.ACCESS_READ,
            isVerified = true,
        ).nodeHandle

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @ParameterizedTest(name = "access: {0}")
    @ValueSource(ints = [MegaShare.ACCESS_UNKNOWN, MegaShare.ACCESS_READ, MegaShare.ACCESS_READWRITE, MegaShare.ACCESS_FULL, MegaShare.ACCESS_OWNER])
    fun `test that access is mapped to access for an incoming share node`(expected: Int) {
        val actual = underTest(
            email = "contact@mega.co.nz",
            node = mock<MegaNode>(),
            access = expected,
            isVerified = true,
        ).access

        Truth.assertThat(actual).isEqualTo(accessPermissionMapper(expected))
    }

    @ParameterizedTest(name = "isVerified: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that is verified is mapped to is verified for an incoming share node`(expected: Boolean) {
        val actual = underTest(
            email = "contact@mega.co.nz",
            node = mock<MegaNode>(),
            access = MegaShare.ACCESS_READ,
            isVerified = expected,
        ).isVerified

        Truth.assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that count is one for an incoming share node when the share is verified`() {
        val actual = underTest(
            email = "contact@mega.co.nz",
            node = mock<MegaNode>(),
            access = MegaShare.ACCESS_READ,
            isVerified = true,
        ).count

        Truth.assertThat(actual).isEqualTo(1)
    }

    @Test
    fun `test that count is zero for an incoming share node when the share is not verified`() {
        val actual = underTest(
            email = "contact@mega.co.nz",
            node = mock<MegaNode>(),
            access = MegaShare.ACCESS_READ,
            isVerified = false,
        ).count

        Truth.assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `test that an incoming share node is mapped to a non pending share with no time stamp`() {
        val actual = underTest(
            email = "contact@mega.co.nz",
            node = mock<MegaNode>(),
            access = MegaShare.ACCESS_READ,
            isVerified = true,
        )

        Truth.assertThat(actual.isPending).isFalse()
        Truth.assertThat(actual.timeStamp).isEqualTo(0)
    }
}