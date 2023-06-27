package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.chat.ChatGeolocation
import nz.mega.sdk.MegaChatGeolocation
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatGeolocationMapperTest {

    private lateinit var underTest: ChatGeolocationMapper

    @BeforeAll
    fun setup() {
        underTest = ChatGeolocationMapper()
    }

    @Test
    fun `test that chat geolocation mapper returns correctly`() {
        val megaChatGeolocation = mock<MegaChatGeolocation> {
            on { longitude }.thenReturn(34.2F)
            on { latitude }.thenReturn(456.45F)
            on { image }.thenReturn(null)
        }
        val chatGeolocation = ChatGeolocation(
            longitude = megaChatGeolocation.longitude,
            latitude = megaChatGeolocation.latitude,
            image = megaChatGeolocation.image
        )
        Truth.assertThat(underTest.invoke(megaChatGeolocation)).isEqualTo(chatGeolocation)
    }
}