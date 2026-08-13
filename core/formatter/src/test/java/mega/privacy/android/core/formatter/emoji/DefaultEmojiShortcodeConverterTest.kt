package mega.privacy.android.core.formatter.emoji

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.thirdpartylib.twemoji.EmojiManagerShortcodes
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultEmojiShortcodeConverterTest {

    private val underTest = DefaultEmojiShortcodeConverter()

    @BeforeAll
    fun loadEmojiData() {
        val json = """[{"emoji":"😄","aliases":["smile"]}]"""
        val assetManager = mock<AssetManager> {
            on { open(SHORTCODE_ASSET_PATH) } doReturn json.byteInputStream()
        }
        val context = mock<Context> {
            on { assets } doReturn assetManager
        }
        EmojiManagerShortcodes.initEmojiData(context)
    }

    @Test
    fun `test that convert replaces a short code with the emoji glyph`() {
        assertThat(underTest.convert(":smile:")).isEqualTo("😄")
    }

    @Test
    fun `test that convert leaves plain text unchanged`() {
        assertThat(underTest.convert("hello world")).isEqualTo("hello world")
    }

    private companion object {
        const val SHORTCODE_ASSET_PATH = "emojisshortcodes/emoji.json"
    }
}
