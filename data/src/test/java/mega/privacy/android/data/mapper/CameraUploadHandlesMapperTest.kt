package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaStringMap
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class CameraUploadHandlesMapperTest {

    @Test
    fun `test that camera upload handles are mapped correctly`() {
        val firstValue = "primary"
        val secondValue = "secondary"
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }
        whenever(keyList["h"]).thenReturn(firstValue)
        whenever(keyList["sh"]).thenReturn(secondValue)
        val actual = toCameraUploadHandles(keyList)
        assertThat(actual).isEqualTo(Pair(firstValue, secondValue))
    }

    @Test
    fun `test that camera upload handles returns pair if one value exist and one does not`() {
        val value = "asdf"
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }
        whenever(keyList["h"]).thenReturn(value)
        whenever(keyList["sh"]).thenReturn("")
        val actual = toCameraUploadHandles(keyList)
        assertThat(actual).isEqualTo(Pair(value, null))
    }

    @Test
    fun `test that camera upload handles returns null pair if no value in map`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }
        whenever(keyList["h"]).thenReturn(null)
        whenever(keyList["sh"]).thenReturn(null)
        val actual = toCameraUploadHandles(keyList)
        assertThat(actual).isEqualTo(Pair(null, null))
    }

    @Test
    fun `test that camera upload handles returns null pair if value empty in map`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(2)
        }
        whenever(keyList["h"]).thenReturn("")
        whenever(keyList["sh"]).thenReturn("")
        val actual = toCameraUploadHandles(keyList)
        assertThat(actual).isEqualTo(Pair(null, null))
    }

    @Test
    fun `test that camera upload handles returns null pair if map empty`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(0)
        }
        val actual = toCameraUploadHandles(keyList)
        assertThat(actual).isEqualTo(Pair(null, null))
    }
}
