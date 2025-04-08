package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageTransferTagRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.chat.message.AttachNodeWithPendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
class HandleChatTransferEventsUseCaseTest {

    private lateinit var underTest: HandleChatTransferEventsUseCase

    private val attachNodeWithPendingMessageUseCase = mock<AttachNodeWithPendingMessageUseCase>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = HandleChatTransferEventsUseCase(
            attachNodeWithPendingMessageUseCase,
            updatePendingMessageUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            attachNodeWithPendingMessageUseCase,
            updatePendingMessageUseCase,
        )
    }

    @Test
    fun `test that node is attached when a finish transfer event with pending message id is received`() =
        runTest {
            testAttachOnFinishEventWithPendingMessageId()
        }

    private val testAttachOnFinishEventWithPendingMessageId: VerifyCallsAfterTransferEvent by lazy {
        val pendingMessageId = 1231L
        val nodeHandle = 4561L
        val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
        val transfer = mock<Transfer> {
            on { this.transferType } doReturn TransferType.CHAT_UPLOAD
            on { this.nodeHandle } doReturn nodeHandle
            on { this.appData } doReturn appData
        }

        VerifyCallsAfterTransferEvent(
            name = "finishEventWithPendingMessageId",
            TransferEvent.TransferFinishEvent(transfer, null)
        ) {
            verify(attachNodeWithPendingMessageUseCase).invoke(
                pendingMessageId,
                NodeId(nodeHandle),
                appData
            )
        }
    }

    @Test
    fun `test that not chat transfer events are filtered out`() =
        runTest {
            testNotChatEventFilteredOut()
        }

    private val testNotChatEventFilteredOut by lazy {
        val pendingMessageId = 1242L
        val nodeHandle = 4572L
        val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
        val transfer = mock<Transfer> {
            on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
            on { this.nodeHandle } doReturn nodeHandle
            on { this.appData } doReturn appData
        }
        VerifyCallsAfterTransferEvent(
            name = "notChatEvent",
            TransferEvent.TransferFinishEvent(transfer, null),
            verifyNotWantedCalls = {
                verifyNoInteractions(attachNodeWithPendingMessageUseCase)
                verifyNoInteractions(updatePendingMessageUseCase)
            })
    }

    @Test
    fun `test that not transfer finish events are filtered out`() =
        runTest {
            testNoTransferFinishEventFilteredOut()
        }

    private val testNoTransferFinishEventFilteredOut by lazy {
        val pendingMessageId = 1253L
        val nodeHandle = 4563L
        val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
        val transfer = mock<Transfer> {
            on { this.transferType } doReturn TransferType.CHAT_UPLOAD
            on { this.nodeHandle } doReturn nodeHandle
            on { this.appData } doReturn appData
        }

        VerifyCallsAfterTransferEvent(
            name = "noTransferFinishEvent",
            TransferEvent.TransferUpdateEvent(transfer),
            verifyNotWantedCalls = {
                verifyNoInteractions(attachNodeWithPendingMessageUseCase)
                verifyNoInteractions(updatePendingMessageUseCase)
            })
    }

    @Test
    fun `test that pending message is updated to error attaching when an exception occurs while attaching the node`() =
        runTest {
            testExceptionAttaching()
        }

    private val testExceptionAttaching by lazy {
        val pendingMessageId = 1234L
        val nodeHandle = 4564L
        val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
        val transfer = mock<Transfer> {
            on { this.transferType } doReturn TransferType.CHAT_UPLOAD
            on { this.nodeHandle } doReturn nodeHandle
            on { this.appData } doReturn appData
        }
        VerifyCallsAfterTransferEvent(
            name = "testException",
            transferEvent = TransferEvent.TransferFinishEvent(transfer, null),
            stub = {
                whenever(
                    attachNodeWithPendingMessageUseCase.invoke(
                        pendingMessageId,
                        NodeId(nodeHandle),
                        appData
                    )
                ).thenThrow(RuntimeException())
            },
        ) {
            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageStateRequest(
                    pendingMessageId,
                    PendingMessageState.ERROR_ATTACHING
                )
            )
        }
    }

    @Test
    fun `test that pending message is updated to error uploading when finish event has an error`() =
        runTest {
            testErrorUploading()
        }

    private val testErrorUploading by lazy {
        val pendingMessageId = 1235L
        val nodeHandle = 4565L
        val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
        val transfer = mock<Transfer> {
            on { this.transferType } doReturn TransferType.CHAT_UPLOAD
            on { this.nodeHandle } doReturn nodeHandle
            on { this.appData } doReturn appData
        }
        VerifyCallsAfterTransferEvent(
            name = "testErrorUploading",
            TransferEvent.TransferFinishEvent(transfer, mock()),
            verifyNotWantedCalls = {
                verifyNoInteractions(attachNodeWithPendingMessageUseCase)
            }
        ) {
            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageStateRequest(
                    pendingMessageId,
                    PendingMessageState.ERROR_UPLOADING
                )
            )
        }
    }

    @Test
    fun `test that uniqueId are updated when start event is received`() =
        runTest {
            testUniqueId()
        }

    private val testUniqueId by lazy {
        val pendingMessageId = 1236L
        val uniqueId = 34386L
        val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
        val transfer = mock<Transfer> {
            on { it.transferType } doReturn TransferType.CHAT_UPLOAD
            on { it.uniqueId } doReturn uniqueId
            on { it.appData } doReturn appData
        }
        VerifyCallsAfterTransferEvent(
            name = "testUniqueId",
            TransferEvent.TransferStartEvent(transfer)
        ) {
            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageTransferTagRequest(
                    pendingMessageId,
                    uniqueId,
                    PendingMessageState.UPLOADING
                )
            )
        }
    }

    @Test
    fun `test that pending message node is attached if already uploaded event is received`() =
        runTest {
            testAttachAlreadyUploaded()
        }

    private val testAttachAlreadyUploaded by lazy {
        val pendingMessageId = 1237L
        val nodeHandle = 127L
        val appData = listOf(
            TransferAppData.Geolocation(345.4, 45.34),
            TransferAppData.ChatUpload(pendingMessageId)
        )
        val transfer = mock<Transfer> {
            on { it.transferType } doReturn TransferType.CHAT_UPLOAD
            on { it.isFinished } doReturn true
            on { it.appData } doReturn appData
            on { it.isAlreadyTransferred } doReturn true
            on { it.nodeHandle } doReturn nodeHandle
        }
        VerifyCallsAfterTransferEvent(
            name = "testAttachAlreadyUploaded",
            mock<TransferEvent.TransferFinishEvent> {
                on { it.transfer } doReturn transfer
            }
        ) {
            verify(attachNodeWithPendingMessageUseCase).invoke(
                pendingMessageId,
                NodeId(nodeHandle),
                appData,
            )
        }
    }

    @Test
    fun `test that pending message is updated to error uploading when a temporary error is received`() =
        runTest {
            testTemporaryError()
        }

    private val testTemporaryError by lazy {
        val pendingMessageId = 1238L
        val appData = listOf(TransferAppData.ChatUpload(pendingMessageId))
        val folderTransfer = mock<Transfer> {
            on { it.transferType } doReturn TransferType.CHAT_UPLOAD
            on { isFolderTransfer } doReturn true
            on { it.appData } doReturn appData
        }
        VerifyCallsAfterTransferEvent(
            name = "testTemporaryError",
            mock<TransferEvent.TransferTemporaryErrorEvent> {
                on { transfer } doReturn folderTransfer //to avoid checking isAlreadyTransferredEvent
            }
        ) {
            verify(updatePendingMessageUseCase).invoke(
                UpdatePendingMessageStateRequest(
                    pendingMessageId,
                    PendingMessageState.ERROR_UPLOADING
                )
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TestMultipleArguments {
        @ParameterizedTest
        @MethodSource("providePairs")
        fun `test that 2 events are handled correctly`(
            verifyCallsAfterTransferEventList: List<VerifyCallsAfterTransferEvent>,
        ) = runTest {
            verifyCallsAfterTransferEventList()
        }

        @ParameterizedTest
        @MethodSource("provideTriplets")
        fun `test that 3 events are handled correctly`(
            verifyCallsAfterTransferEventList: List<VerifyCallsAfterTransferEvent>,
        ) = runTest {
            verifyCallsAfterTransferEventList()
        }

        @ParameterizedTest
        @MethodSource("provideQuads")
        fun `test that 4 events are handled correctly`(
            verifyCallsAfterTransferEventList: List<VerifyCallsAfterTransferEvent>,
        ) = runTest {
            verifyCallsAfterTransferEventList()
        }

        private fun providePairs() = allIndividualTests.createPermutations(2)
        private fun provideTriplets() = allIndividualTests.createPermutations(3)
        private fun provideQuads() = allIndividualTests.createPermutations(4)

        private val allIndividualTests = listOf(
            testAttachOnFinishEventWithPendingMessageId,
            testNotChatEventFilteredOut,
            testNoTransferFinishEventFilteredOut,
            testExceptionAttaching,
            testErrorUploading,
            testUniqueId,
            testAttachAlreadyUploaded,
            testTemporaryError
        )

        private fun List<VerifyCallsAfterTransferEvent>.createPermutations(size: Int): List<List<VerifyCallsAfterTransferEvent>> {
            if (size == 0) return listOf(emptyList())
            if (size > this.size) throw IllegalArgumentException("Can't create permutations with more elements than the original list")

            return this.flatMap { element ->
                val remaining = this - element
                remaining.createPermutations(size - 1).map { sublist ->
                    listOf(element) + sublist
                }
            }
        }
    }

    inner class VerifyCallsAfterTransferEvent(
        val name: String,
        val transferEvent: TransferEvent,
        val stub: suspend () -> Unit = {},
        val verifyNotWantedCalls: suspend () -> Unit = {},
        val verifyCalls: suspend () -> Unit = {},
    ) {
        suspend operator fun invoke() {
            stub()
            underTest(transferEvent)

            verifyCalls()
            verifyNotWantedCalls()
        }

        override fun toString() = name
    }

    private suspend operator fun List<VerifyCallsAfterTransferEvent>.invoke() {
        this.forEach { it.stub() }
        underTest(*this.map { it.transferEvent }.toTypedArray())
        this.forEach {
            it.verifyCalls()
        }
    }
}