package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessage
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
    fun `test that InvalidMetaMessage is mapped when meta is null`() {

        val message = mock<ChatMessage> {
            on { containsMeta }.thenReturn(null)
        }
        val isMine = true

        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
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
    ) {
        val mockContainsMeta = mock<ContainsMeta> {
            on { type }.thenReturn(metaType)
        }
        val message = mock<ChatMessage> {
            on { containsMeta }.thenReturn(mockContainsMeta)
        }

        val isMine = true
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
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