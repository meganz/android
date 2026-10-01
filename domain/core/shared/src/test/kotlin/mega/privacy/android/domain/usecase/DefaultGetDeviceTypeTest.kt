package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.DeviceType
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetDeviceTypeTest{
    private lateinit var underTest: GetDeviceType

    @Before
    fun setUp() {
        underTest = DefaultGetDeviceType()
    }

    @Test
    fun `test that folder with empty name returns unknown type`() = runTest{
        val actual = underTest(mock {on { name }.thenReturn("")})
        assertThat(actual).isEqualTo(DeviceType.Unknown)
    }

    @Test
    fun `test that folder with win in name returns windows type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("Windows")})
        assertThat(actual).isEqualTo(DeviceType.Windows)
    }

    @Test
    fun `test that folder with desktop in name returns windows type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("desktop")})
        assertThat(actual).isEqualTo(DeviceType.Windows)
    }

    @Test
    fun `test that folder with linux in name returns linux type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("linux")})
        assertThat(actual).isEqualTo(DeviceType.Linux)
    }

    @Test
    fun `test that folder with debian in name returns linux type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("debian")})
        assertThat(actual).isEqualTo(DeviceType.Linux)
    }

    @Test
    fun `test that folder with ubuntu in name returns linux type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("ubuntu")})
        assertThat(actual).isEqualTo(DeviceType.Linux)
    }

    @Test
    fun `test that folder with centos in name returns linux type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("centos")})
        assertThat(actual).isEqualTo(DeviceType.Linux)
    }

    @Test
    fun `test that folder with mac in name returns linux type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("Macintosh")})
        assertThat(actual).isEqualTo(DeviceType.Mac)
    }

    @Test
    fun `test that folder with drive in name returns external drive type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("drive")})
        assertThat(actual).isEqualTo(DeviceType.ExternalDrive)
    }

    @Test
    fun `test that folder with ext in name returns external drive type`() = runTest{
        val actual = underTest(mock{ on { name }.thenReturn("external")})
        assertThat(actual).isEqualTo(DeviceType.ExternalDrive)
    }
}