package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.exception.chat.MessageNonDeletableException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteNodeAttachmentMessageByIdsUseCaseTest {

    private lateinit var underTest: DeleteNodeAttachmentMessageByIdsUseCase

    private val isMessageDeletableUseCase = mock<IsMessageDeletableUseCase>()
    private val revokeAttachmentMessageUseCase = mock<RevokeAttachmentMessageUseCase>()

    private val chatId = 1L
    private val msgId = 2L

    @BeforeAll
    fun setup() {
        underTest = DeleteNodeAttachmentMessageByIdsUseCase(
            isMessageDeletableUseCase,
            revokeAttachmentMessageUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(isMessageDeletableUseCase, revokeAttachmentMessageUseCase)
    }

    @Test
    fun `test that if message is deletable RevokeAttachmentMessageUseCase is invoked`() = runTest {
        whenever(isMessageDeletableUseCase(chatId, msgId)).thenReturn(true)

        underTest(chatId, msgId)

        verify(revokeAttachmentMessageUseCase).invoke(chatId, msgId)
    }

    @Test
    fun `test that if message is not deletable MessageNonDeletableException is thrown and RevokeAttachmentMessageUseCase is not invoked`() =
        runTest {
            whenever(isMessageDeletableUseCase(chatId, msgId)).thenReturn(false)

            assertThrows<MessageNonDeletableException> {
                underTest(chatId, msgId)
            }

            verifyNoInteractions(revokeAttachmentMessageUseCase)
        }
}