package mega.privacy.android.data.mapper.chat.paging

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatRichPreviewInfo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class RichPreviewEntityMapperTest {
    private val richPreviewEntityMapper = RichPreviewEntityMapper()

    @Test
    internal fun `test that values are mapped correctly`() {
        val expectedMessageId = 1L
        val expectedTitle = "title"
        val expectedDescription = "description"
        val expectedImage = "image"
        val expectedFormat = "imageFormat"
        val expectedIcon = "icon"
        val expectedIconFormat = "iconFormat"
        val expectedUrl = "url"
        val expectedDomainName = "domainName"

        val chatRichPreviewInfo = mock<ChatRichPreviewInfo> {
            on { title } doReturn expectedTitle
            on { description } doReturn expectedDescription
            on { image } doReturn expectedImage
            on { imageFormat } doReturn expectedFormat
            on { icon } doReturn expectedIcon
            on { iconFormat } doReturn expectedIconFormat
            on { url } doReturn expectedUrl
            on { domainName } doReturn expectedDomainName
        }

        val result = richPreviewEntityMapper(expectedMessageId, chatRichPreviewInfo)

        assertThat(result.messageId).isEqualTo(expectedMessageId)
        assertThat(result.title).isEqualTo(expectedTitle)
        assertThat(result.description).isEqualTo(expectedDescription)
        assertThat(result.image).isEqualTo(expectedImage)
        assertThat(result.imageFormat).isEqualTo(expectedFormat)
        assertThat(result.icon).isEqualTo(expectedIcon)
        assertThat(result.iconFormat).isEqualTo(expectedIconFormat)
        assertThat(result.url).isEqualTo(expectedUrl)
        assertThat(result.domainName).isEqualTo(expectedDomainName)
    }
}