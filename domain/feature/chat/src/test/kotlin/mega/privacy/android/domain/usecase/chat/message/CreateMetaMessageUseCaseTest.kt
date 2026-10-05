package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ContainsMeta
import mega.privacy.android.domain.entity.chat.ContainsMetaType
import mega.privacy.android.domain.entity.chat.LinkDetail
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.usecase.chat.GetLinkTypesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateMetaMessageUseCaseTest {

    private lateinit var underTest: CreateMetaMessageUseCase

    private val getLinkTypesUseCase = mock<GetLinkTypesUseCase>()

    @BeforeEach
    internal fun setUp() {
        reset(getLinkTypesUseCase)
        underTest = CreateMetaMessageUseCase(getLinkTypesUseCase)
    }

    @Test
    fun `test that TextMessage is returned when a rich preview message contains an autoplay link`() =
        runTest {
            val content = "https://mega.nz/collection/xMpQjZhK?autoplay=random#Jp3KceNZRvNsqp"
            whenever(getLinkTypesUseCase(content)) doReturn listOf(
                LinkDetail(link = content, type = RegexPatternType.AUTOPLAY_LINK)
            )

            val actual = underTest.invoke(richPreviewRequest(content))

            assertThat(actual).isInstanceOf(TextMessage::class.java)
            assertThat((actual as TextMessage).content).isEqualTo(content)
            assertThat(actual.hasOtherLink).isFalse()
        }

    @Test
    fun `test that RichPreviewMessage is returned when a rich preview message contains other links`() =
        runTest {
            val content = "https://mega.nz/collection/xMpQjZhK#Jp3KceNZRvNsqp"
            whenever(getLinkTypesUseCase(content)) doReturn listOf(
                LinkDetail(link = content, type = RegexPatternType.ALBUM_LINK)
            )

            assertThat(underTest.invoke(richPreviewRequest(content)))
                .isInstanceOf(RichPreviewMessage::class.java)
        }

    private fun richPreviewRequest(content: String): CreateTypedMessageRequest {
        val mockContainsMeta = mock<ContainsMeta> {
            on { type } doReturn ContainsMetaType.RICH_PREVIEW
        }
        val message = mock<ChatMessage> {
            on { containsMeta } doReturn mockContainsMeta
            on { status } doReturn ChatMessageStatus.UNKNOWN
            on { this.content } doReturn content
        }
        return CreateTypedMessageRequest(
            chatMessage = message,
            chatId = 123L,
            isMine = true,
            reactions = emptyList(),
            exists = true,
        )
    }

    @Test
    fun `test that InvalidMetaMessage is mapped when meta is null`() = runTest {

        val message = mock<ChatMessage> {
            on { containsMeta }.thenReturn(null)
            on { status } doReturn ChatMessageStatus.UNKNOWN
        }

        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    reactions = emptyList(),
                    exists = true,
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

        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    reactions = emptyList(),
                    exists = true,
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