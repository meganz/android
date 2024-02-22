package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteMessagesUseCaseTest {

    private lateinit var underTest: DeleteMessagesUseCase

    private val deleteGeneralMessageUseCase = mock<DeleteGeneralMessageUseCase>()
    private val deleteVoiceClipMessageUseCase = mock<DeleteVoiceClipMessageUseCase>()
    private val deleteNodeAttachmentMessageUseCase = mock<DeleteNodeAttachmentMessageUseCase>()

    private val deleteMessagesUseCases = setOf(
        deleteGeneralMessageUseCase,
        deleteNodeAttachmentMessageUseCase,
        deleteVoiceClipMessageUseCase,
    )

    @BeforeAll
    fun setup() {
        underTest = DeleteMessagesUseCase(deleteMessagesUseCases)
    }

    @AfterEach
    fun resetMocks() {
        reset(
            deleteGeneralMessageUseCase,
            deleteVoiceClipMessageUseCase,
            deleteNodeAttachmentMessageUseCase,
        )
    }

    @Test
    fun `test that all messages are deleted`() = runTest {
        val message1 = mock<NormalMessage> {
            on { isDeletable } doReturn true
        }
        val message2 = mock<NodeAttachmentMessage> {
            on { isDeletable } doReturn true
        }
        val message3 = mock<VoiceClipMessage> {
            on { isDeletable } doReturn true
        }
        val initialList = listOf(message1, message2, message3)
        val secondList = listOf(message2, message3)
        val thirdList = listOf(message3)
        val finalList = emptyList<TypedMessage>()
        whenever(deleteGeneralMessageUseCase(any())).thenReturn(secondList)
        whenever(deleteNodeAttachmentMessageUseCase(any())).thenReturn(thirdList)
        whenever(deleteVoiceClipMessageUseCase(any())).thenReturn(finalList)
        underTest.invoke(initialList)
        verify(deleteGeneralMessageUseCase).invoke(initialList)
        verify(deleteNodeAttachmentMessageUseCase).invoke(secondList)
        verify(deleteVoiceClipMessageUseCase).invoke(thirdList)
        verifyNoMoreInteractions(deleteGeneralMessageUseCase)
        verifyNoMoreInteractions(deleteNodeAttachmentMessageUseCase)
        verifyNoMoreInteractions(deleteVoiceClipMessageUseCase)
    }
}