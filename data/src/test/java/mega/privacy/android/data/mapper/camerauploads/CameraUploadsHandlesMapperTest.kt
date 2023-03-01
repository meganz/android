package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaStringMap
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [CameraUploadsHandlesMapper]
 */
class CameraUploadsHandlesMapperTest {
    private lateinit var underTest: CameraUploadsHandlesMapper

    @Before
    fun setUp() {
        underTest = CameraUploadsHandlesMapperImpl()
    }

    @Test
    fun `test that the Pair of Strings can be mapped correctly`() {
        val firstValue = "primary"
        val secondValue = "secondary"
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }

        whenever(keyList["h"]).thenReturn(firstValue)
        whenever(keyList["sh"]).thenReturn(secondValue)

        assertThat(underTest(keyList)).isEqualTo(Pair(firstValue, secondValue))
    }

    @Test
    fun `test that a Pair of Strings is returned even if one value is missing in the MegaStringMap`() {
        val value = "primary"
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }

        whenever(keyList["h"]).thenReturn(value)
        whenever(keyList["sh"]).thenReturn("")

        assertThat(underTest(keyList)).isEqualTo(Pair(value, null))
    }

    @Test
    fun `test that a Pair of null Strings is returned if there are null values in the MegaStringMap`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }

        whenever(keyList["h"]).thenReturn(null)
        whenever(keyList["sh"]).thenReturn(null)

        assertThat(underTest(keyList)).isEqualTo(Pair(null, null))
    }

    @Test
    fun `test that a Pair of null Strings is returned if there are empty String values in the MegaStringMap`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }

        whenever(keyList["h"]).thenReturn("")
        whenever(keyList["sh"]).thenReturn("")

        assertThat(underTest(keyList)).isEqualTo(Pair(null, null))
    }

    @Test
    fun `test that a Pair of null Strings is returned if there are no values in the MegaStringMap`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(0)
        }

        assertThat(underTest(keyList)).isEqualTo(Pair(null, null))
    }
}