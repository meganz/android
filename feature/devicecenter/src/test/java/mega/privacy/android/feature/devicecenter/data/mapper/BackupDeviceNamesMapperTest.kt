package mega.privacy.android.feature.devicecenter.data.mapper

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaStringList
import nz.mega.sdk.MegaStringMap
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock

/**
 * Test class for [BackupDeviceNamesMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class BackupDeviceNamesMapperTest {

    private lateinit var underTest: BackupDeviceNamesMapper

    @BeforeAll
    fun setUp() {
        underTest = BackupDeviceNamesMapper()
    }

    @Test
    fun `test that the mapping is correct`() {
        val keySize = 2
        val firstPair = Pair("12345", "Device One")
        val secondPair = Pair("67890", "Device Two")

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
                firstPair.first to firstPair.second,
                secondPair.first to secondPair.second,
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