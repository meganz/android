package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CopyChatNodesUseCaseTest {
    private lateinit var underTest: CopyChatNodesUseCase

    private val copyChatNodeUseCase: CopyChatNodeUseCase = mock()
    private val newParentNode = NodeId(158401030174851)
    private val chatId = 1000L
    private val messageId = 2000L
    private val messageId2 = 3000L

    @BeforeAll
    fun setUp() {
        underTest = CopyChatNodesUseCase(
            copyChatNodeUseCase = copyChatNodeUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(copyChatNodeUseCase)
    }


    @Test
    fun `test that throw ForeignNodeException when move node throw ForeignNodeException`() =
        runTest {
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNode
                )
            ).thenThrow(ForeignNodeException::class.java)

            assertThrows<ForeignNodeException> {
                underTest(chatId, listOf(messageId), newParentNode)
            }
        }

    @Test
    fun `test that throw NotEnoughQuotaMegaException when move node throw NotEnoughQuotaMegaException`() =
        runTest {
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNode
                )
            ).thenAnswer {
                throw NotEnoughQuotaMegaException(1, "")
            }
            assertThrows<NotEnoughQuotaMegaException> {
                underTest(chatId, listOf(messageId), newParentNode)
            }
        }

    @Test
    fun `test that throw QuotaExceededMegaException when move node throw QuotaExceededMegaException`() =
        runTest {
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNode
                )
            ).thenAnswer {
                throw QuotaExceededMegaException(1, "")
            }
            assertThrows<QuotaExceededMegaException> {
                underTest(chatId, listOf(messageId), newParentNode)
            }
        }

    @Test
    fun `test that return MoveRequestResult correctly when at least one move successfully`() =
        runTest {
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNode
                )
            ).thenReturn(NodeId(1234567890))
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId2, newNodeParent = newParentNode
                )
            ).thenThrow(SecurityException::class.java)

            val result = underTest(chatId, listOf(messageId, messageId2), newParentNode)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(1)
            assertThat(result.errorCount).isEqualTo(1)
        }

    @Test
    fun `test that return MoveRequestResult correctly when move all nodes successfully`() =
        runTest {
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId,
                    newNodeParent = newParentNode
                )
            ).thenReturn(NodeId(1234567890))
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId2,
                    newNodeParent = newParentNode
                )
            ).thenReturn(NodeId(566666688))

            val result = underTest(chatId, listOf(messageId, messageId2), newParentNode)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(2)
            assertThat(result.errorCount).isEqualTo(0)
        }

    @Test
    fun `test that return MoveRequestResult correctly when move all nodes failed`() =
        runTest {
            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId, newNodeParent = newParentNode
                )
            ).thenThrow(RuntimeException::class.java)

            whenever(
                copyChatNodeUseCase(
                    chatId = chatId,
                    messageId = messageId2, newNodeParent = newParentNode
                )
            ).thenThrow(RuntimeException::class.java)


            val result = underTest(chatId, listOf(messageId, messageId2), newParentNode)

            assertThat(result.count).isEqualTo(2)
            assertThat(result.successCount).isEqualTo(0)
            assertThat(result.errorCount).isEqualTo(2)
        }
}