package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ContainsMeta
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateMetaMessageUseCaseTest {

    private lateinit var underTest: CreateMetaMessageUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = CreateMetaMessageUseCase()
    }

    @Test
    fun `test that InvalidMetaMessage is mapped when meta is null`() = runTest {

        val message = mock<ChatMessage> {
            on { containsMeta }.thenReturn(null)
            on { status } doReturn ChatMessageStatus.UNKNOWN
        }
        val isMine = true

        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    shouldShowAvatar = true,
                    reactions = emptyList(),
                )
            )
        ).isInstanceOf(
            InvalidMetaMessage::class.java
        )
    }

    @ParameterizedTest(name = "message type {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test that `(
        metaType: ContainsMetaType,
        clazz: Class<MetaMessage>,
    ) = runTest {
        val mockContainsMeta = mock<ContainsMeta> {
            on { type }.thenReturn(metaType)
        }
        val message = mock<ChatMessage> {
            on { containsMeta }.thenReturn(mockContainsMeta)
            on { status } doReturn ChatMessageStatus.UNKNOWN
        }

        val isMine = true
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    shouldShowAvatar = true,
                    reactions = emptyList(),
                )
            )
        ).isInstanceOf(
            clazz
        )
    }

    private fun provideParameters(): Stream<Arguments> =
        Stream.of(
            *arrayOf(
                Arguments.of(ContainsMetaType.RICH_PREVIEW, RichPreviewMessage::class.java),
                Arguments.of(ContainsMetaType.GEOLOCATION, LocationMessage::class.java),
                Arguments.of(ContainsMetaType.GIPHY, GiphyMessage::class.java),
            )
        )


}