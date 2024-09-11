package mega.privacy.android.data.facade.chat

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.chat.ChatDatabase
import mega.privacy.android.data.database.dao.PendingMessageDao
import mega.privacy.android.data.database.entity.chat.PendingMessageEntity
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndNodeHandleRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndPathRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatStorageFacadeTest {
    private lateinit var underTest: ChatStorageFacade

    val database = mock<ChatDatabase>()

    @BeforeAll
    fun setUp() {
        underTest = ChatStorageFacade { database }
    }

    @ParameterizedTest
    @MethodSource("pendingMessageUpdatesProvider")
    fun `test that pending message update request calls the correct dao update`(
        update: UpdatePendingMessageRequest,
    ) = runTest {
        val pendingMessageDao = mock<PendingMessageDao>()
        whenever(database.pendingMessageDao()) doReturn pendingMessageDao

        underTest.updatePendingMessage(update)

        when (update) {
            is UpdatePendingMessageStateAndNodeHandleRequest -> {
                verify(pendingMessageDao).update(update)
            }

            is UpdatePendingMessageStateRequest -> {
                verify(pendingMessageDao).update(update)
            }

            is UpdatePendingMessageTransferTagRequest -> {
                verify(pendingMessageDao).update(update)
            }

            is UpdatePendingMessageStateAndPathRequest -> {
                verify(pendingMessageDao).update(update)
            }
        }
    }

    @Test
    fun `test that multiple pending message update requests calls the transaction dao update`() =
        runTest {
            val updates = pendingMessageUpdatesProvider()

            val pendingMessageDao = mock<PendingMessageDao>()
            whenever(database.pendingMessageDao()) doReturn pendingMessageDao

            underTest.updatePendingMessage(updatePendingMessageRequests = updates.toTypedArray())

            verify(pendingMessageDao).updateMultiple(updates)
        }

    @ParameterizedTest
    @EnumSource(PendingMessageState::class)
    fun `test that get pending messages by state returns result from pending message dao`(state: PendingMessageState) =
        runTest {
            val pendingMessageDao = mock<PendingMessageDao>()
            whenever(database.pendingMessageDao()) doReturn pendingMessageDao
            val expected = mock<List<PendingMessageEntity>>()
            whenever(pendingMessageDao.getByState(state)) doReturn expected

            val actual = underTest.getPendingMessagesByState(state)

            assertThat(actual).isEqualTo(expected)
        }

    @ParameterizedTest
    @EnumSource(PendingMessageState::class)
    fun `test that fetch pending messages by state returns result from pending message dao`(state: PendingMessageState) =
        runTest {
            val pendingMessageDao = mock<PendingMessageDao>()
            whenever(database.pendingMessageDao()) doReturn pendingMessageDao
            val expected = mock<Flow<List<PendingMessageEntity>>>()
            whenever(pendingMessageDao.fetchPendingMessagesByState(listOf(state))) doReturn expected

            val actual = underTest.fetchPendingMessages(state)

            assertThat(actual).isEqualTo(expected)
        }

    @Test
    fun `test that fetch pending messages by state returns result from pending message dao when there are multiple states`() =
        runTest {
            val states = arrayOf(PendingMessageState.PREPARING, PendingMessageState.READY_TO_UPLOAD)
            val pendingMessageDao = mock<PendingMessageDao>()
            whenever(database.pendingMessageDao()) doReturn pendingMessageDao
            val expected = mock<Flow<List<PendingMessageEntity>>>()
            whenever(pendingMessageDao.fetchPendingMessagesByState(states.toList())) doReturn expected

            val actual = underTest.fetchPendingMessages(states = states)

            assertThat(actual).isEqualTo(expected)
        }

    private fun pendingMessageUpdatesProvider() = listOf(
        mock<UpdatePendingMessageStateRequest>(),
        mock<UpdatePendingMessageStateAndNodeHandleRequest>(),
        mock<UpdatePendingMessageTransferTagRequest>(),
        mock<UpdatePendingMessageStateAndPathRequest>(),
    )
}