package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import nz.mega.sdk.MegaStringMap
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChatFilesFolderUserAttributeMapperTest {

    @Test
    fun `test that chat files user attribute is mapped correctly`() {
        val value = "ABCD"
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(1)
        }
        whenever(keyList["h"]).thenReturn(value)
        val actual = toChatFilesFolderUserAttribute(keyList)
        assertThat(actual).isEqualTo(value)
    }

    @Test
    fun `test that chat files user attribute is returned null if there is no value in map`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(1)
        }
        whenever(keyList["h"]).thenReturn(null)
        val actual = toChatFilesFolderUserAttribute(keyList)
        assertThat(actual).isEqualTo(null)
    }

    @Test
    fun `test that chat files user attribute is returned null if value is empty in map`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(1)
        }
        whenever(keyList["h"]).thenReturn("")
        val actual = toChatFilesFolderUserAttribute(keyList)
        assertThat(actual).isEqualTo(null)
    }

    @Test
    fun `test that chat files user attribute is returned null if map is empty`() {
        val keyList = mock<MegaStringMap> {
            on { size() }.thenReturn(0)
        }
        val actual = toChatFilesFolderUserAttribute(keyList)
        assertThat(actual).isEqualTo(null)
    }
}
