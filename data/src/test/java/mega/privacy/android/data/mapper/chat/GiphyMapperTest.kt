package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.chat.Giphy
import nz.mega.sdk.MegaChatGiphy
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GiphyMapperTest {

    private lateinit var underTest: GiphyMapper

    @BeforeAll
    fun setup() {
        underTest = GiphyMapper()
    }

    @Test
    fun `test that giphy mapper returns correctly`() {
        val megaChatGiphy = mock<MegaChatGiphy> {
            on { mp4Src }.thenReturn("mp4Src")
            on { webpSrc }.thenReturn("webpSrc")
            on { title }.thenReturn("title")
            on { mp4Size }.thenReturn(234)
            on { webpSize }.thenReturn(445)
            on { width }.thenReturn(50)
            on { height }.thenReturn(60)
        }
        val giphy = Giphy(
            mp4Src = megaChatGiphy.mp4Src,
            webpSrc = megaChatGiphy.webpSrc,
            title = megaChatGiphy.title,
            mp4Size = megaChatGiphy.mp4Size,
            webpSize = megaChatGiphy.webpSize,
            width = megaChatGiphy.width,
            height = megaChatGiphy.height
        )
        Truth.assertThat(underTest.invoke(megaChatGiphy)).isEqualTo(giphy)
    }
}