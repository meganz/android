package mega.privacy.android.domain.usecase.chat.message.forward

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.meta.ChatGeolocationInfo
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.usecase.chat.message.SendLocationMessageUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardLocationUseCaseTest {

    private lateinit var underTest: ForwardLocationUseCase

    private val sendLocationMessageUseCase = mock<SendLocationMessageUseCase>()

    private val targetChatId = 789L

    @BeforeEach
    fun setup() {
        underTest = ForwardLocationUseCase(sendLocationMessageUseCase = sendLocationMessageUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(sendLocationMessageUseCase)
    }

    @Test
    fun `test that empty is returned if message is not a location`() = runTest {
        val message = mock<NormalMessage>()
        underTest.invoke(listOf(targetChatId), message)
        assertThat(underTest.invoke(listOf(targetChatId), message)).isEmpty()
    }

    @Test
    fun `test that send location message use case is invoked and success is returned`() = runTest {
        val longitude = 1.0F
        val latitude = 1.0F
        val image = "image"
        val chatLocationIndo = mock<ChatGeolocationInfo> {
            on { this.longitude } doReturn longitude
            on { this.latitude } doReturn latitude
            on { this.image } doReturn image
        }
        val message = mock<LocationMessage> {
            on { chatGeolocationInfo } doReturn chatLocationIndo
        }
        whenever(sendLocationMessageUseCase(targetChatId, longitude, latitude, image))
            .thenReturn(Unit)
        underTest.invoke(listOf(targetChatId), message)
        verify(sendLocationMessageUseCase).invoke(targetChatId, longitude, latitude, image)
        assertThat(underTest.invoke(listOf(targetChatId), message))
            .isEqualTo(listOf(ForwardResult.Success(targetChatId)))
    }
}