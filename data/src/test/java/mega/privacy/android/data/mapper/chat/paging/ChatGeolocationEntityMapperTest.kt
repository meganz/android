package mega.privacy.android.data.mapper.chat.paging

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.messages.ChatGeolocationInfo
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class ChatGeolocationEntityMapperTest {
    private val underTest = ChatGeolocationEntityMapper()

    @Test
    internal fun `invoke should return GeolocationEntity when message containsMeta type is GEOLOCATION`() {
        val expectedMessageId = 1L
        val expectedLatitude = 1F
        val expectedLongitude = 2F
        val expectedImage = "image"

        val chatGeolocationInfo = mock<ChatGeolocationInfo> {
            on { latitude } doReturn expectedLatitude
            on { longitude } doReturn expectedLongitude
            on { image } doReturn expectedImage
        }

        val result = underTest(expectedMessageId, chatGeolocationInfo)

        assertThat(result.messageId).isEqualTo(expectedMessageId)
        assertThat(result.latitude).isEqualTo(expectedLatitude)
        assertThat(result.longitude).isEqualTo(expectedLongitude)
        assertThat(result.image).isEqualTo(expectedImage)
    }
}