package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.meta.MetaMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteGeneralMessageUseCaseTest {

    private lateinit var underTest: DeleteGeneralMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteGeneralMessageUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @ParameterizedTest
    @MethodSource("provideCorrectTypeMessages")
    fun `test that delete message is invoked when deletable message of correct type is deleted`(
        message: TypedMessage,
    ) = runTest {
        whenever(chatMessageRepository.deleteMessage(message.chatId, message.msgId))
            .thenReturn(mock<ChatMessage>())

        underTest.invoke(listOf(message))

        verify(chatMessageRepository).deleteMessage(chatId, msgId)
    }

    @ParameterizedTest
    @MethodSource("provideNonDeletableCorrectTypeMessages")
    fun `test that delete message is not invoked when no-deletable message of correct type message is deleted`(
        message: TypedMessage,
    ) = runTest {
        whenever(chatMessageRepository.deleteMessage(message.chatId, message.msgId))
            .thenReturn(mock<ChatMessage>())

        underTest.invoke(listOf(message))

        verifyNoInteractions(chatMessageRepository)
    }

    @ParameterizedTest
    @MethodSource("provideIncorrectTypeMessages")
    fun `test that delete message is not invoked when incorrect type message is deleted`(
        message: TypedMessage,
    ) = runTest {
        whenever(chatMessageRepository.deleteMessage(message.chatId, message.msgId))
            .thenReturn(mock<ChatMessage>())

        underTest.invoke(listOf(message))

        verifyNoInteractions(chatMessageRepository)
    }

    private fun provideCorrectTypeMessages() = listOf(
        mock<NormalMessage> {
            on { this.chatId } doReturn chatId
            on { this.msgId } doReturn msgId
            on { this.isDeletable } doReturn true
        },
        mock<MetaMessage> {
            on { this.chatId } doReturn chatId
            on { this.msgId } doReturn msgId
            on { this.isDeletable } doReturn true
        },
        mock<InvalidMessage> {
            on { this.chatId } doReturn chatId
            on { this.msgId } doReturn msgId
            on { this.isDeletable } doReturn true
        },
        mock<ContactAttachmentMessage> {
            on { this.chatId } doReturn chatId
            on { this.msgId } doReturn msgId
            on { this.isDeletable } doReturn true
        },
    )

    private fun provideNonDeletableCorrectTypeMessages() =
        provideCorrectTypeMessages().onEach { whenever(it.isDeletable) doReturn false }

    private fun provideIncorrectTypeMessages() =
        listOf(
            mock<NodeAttachmentMessage> {
                on { this.chatId } doReturn chatId
                on { this.msgId } doReturn msgId
                on { this.isDeletable } doReturn true
            },
            mock<PendingFileAttachmentMessage> {
                on { this.chatId } doReturn chatId
                on { this.msgId } doReturn msgId
                on { this.isDeletable } doReturn true
            },
            mock<PendingVoiceClipMessage> {
                on { this.chatId } doReturn chatId
                on { this.msgId } doReturn msgId
                on { this.isDeletable } doReturn true
            },
            mock<VoiceClipMessage> {
                on { this.chatId } doReturn chatId
                on { this.msgId } doReturn msgId
                on { this.isDeletable } doReturn true
            },
        )

    private
    val chatId = 1L

    private
    val msgId = 2L
}