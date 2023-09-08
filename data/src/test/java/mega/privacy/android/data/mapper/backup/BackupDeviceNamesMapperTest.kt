package mega.privacy.android.data.mapper.backup

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.wrapper.StringWrapper
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringMap
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

/**
 * Test class for [BackupDeviceNamesMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupDeviceNamesMapperTest {

    private lateinit var underTest: BackupDeviceNamesMapper

    private val stringWrapper = mock<StringWrapper>()

    @BeforeAll
    fun setUp() {
        underTest = BackupDeviceNamesMapper(stringWrapper)
    }

    @BeforeEach
    fun resetMocks() {
        reset(stringWrapper)
    }

    @Test
    fun `test that the mapping is correct`() {
        val keySize = 2

        val deviceOneBase64Name = "abcdef-ghijkl"
        val deviceTwoBase64Name = "mnopq-rstuvw"
        val expectedDeviceOneName = "Device One"
        val expectedDeviceTwoName = "Device Two"

        val firstPair = Pair("12345", deviceOneBase64Name)
        val secondPair = Pair("67890", deviceTwoBase64Name)

        stringWrapper.stub {
            on { decodeBase64(deviceOneBase64Name) }.thenReturn(expectedDeviceOneName)
            on { decodeBase64(deviceTwoBase64Name) }.thenReturn(expectedDeviceTwoName)
        }

        val megaStringKeyList = mock<MegaStringList> {
            on { size() }.thenReturn(keySize)
            on { get(0) }.thenReturn(firstPair.first)
            on { get(1) }.thenReturn(secondPair.first)
        }
        val megaStringMap = mock<MegaStringMap> {
            on { keys }.thenReturn(megaStringKeyList)
            on { get(firstPair.first) }.thenReturn(firstPair.second)
            on { get(secondPair.first) }.thenReturn(secondPair.second)
        }

        assertThat(underTest(megaStringMap)).isEqualTo(
            mapOf(
                firstPair.first to expectedDeviceOneName,
                secondPair.first to expectedDeviceTwoName,
            )
        )
    }

    @Test
    fun `test that an empty map is returned if mega string map is null`() {
        assertThat(underTest(null)).isEqualTo(emptyMap<String, String>())
    }

    @ParameterizedTest(name = "key size: {0}")
    @ValueSource(ints = [0, -1])
    fun `test that an empty map is returned`(keySize: Int) {
        val megaStringKeyList = mock<MegaStringList> { on { size() }.thenReturn(keySize) }
        val megaStringMap = mock<MegaStringMap> { on { keys }.thenReturn(megaStringKeyList) }

        assertThat(underTest(megaStringMap)).isEqualTo(emptyMap<String, String>())
    }
}