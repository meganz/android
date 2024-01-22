package mega.privacy.android.data.mapper.chat.paging

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.messages.ChatGifInfo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class GiphyEntityMapperTest {
    private val underTest = GiphyEntityMapper()

    @Test
    internal fun `invoke should return GiphyEntity when message containsMeta type is GIPHY`() {
        val expectedMessageId = 1L
        val expectedMp4Src = "mp4Src"
        val expectedWebpSrc = "webpSrc"
        val expectedTitle = "title"
        val expectedMp4Size = 1
        val expectedWebpSize = 2
        val expectedWidth = 3
        val expectedHeight = 4

        val chatGifInfo = mock<ChatGifInfo> {
            on { mp4Src } doReturn expectedMp4Src
            on { webpSrc } doReturn expectedWebpSrc
            on { title } doReturn expectedTitle
            on { mp4Size } doReturn expectedMp4Size
            on { webpSize } doReturn expectedWebpSize
            on { width } doReturn expectedWidth
            on { height } doReturn expectedHeight
        }

        val actual = underTest(expectedMessageId, chatGifInfo)

        assertThat(actual.messageId).isEqualTo(expectedMessageId)
        assertThat(actual.mp4Src).isEqualTo(expectedMp4Src)
        assertThat(actual.webpSrc).isEqualTo(expectedWebpSrc)
        assertThat(actual.title).isEqualTo(expectedTitle)
        assertThat(actual.mp4Size).isEqualTo(expectedMp4Size)
        assertThat(actual.webpSize).isEqualTo(expectedWebpSize)
        assertThat(actual.width).isEqualTo(expectedWidth)
        assertThat(actual.height).isEqualTo(expectedHeight)
    }
}