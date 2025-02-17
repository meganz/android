package mega.privacy.android.domain.usecase.transfers.pending

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState.AlreadyStarted
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState.ErrorStarting
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState.NotSentToSdk
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState.SdkScanned
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorPendingTransfersUntilResolvedUseCaseTest {
    private lateinit var underTest: MonitorPendingTransfersUntilResolvedUseCase

    private val getPendingTransfersByTypeUseCase = mock<GetPendingTransfersByTypeUseCase>()

    @BeforeAll
    fun setup() {
        underTest = MonitorPendingTransfersUntilResolvedUseCase(getPendingTransfersByTypeUseCase)
    }

    @BeforeEach
    fun cleanUp() = reset(getPendingTransfersByTypeUseCase)

    @ParameterizedTest
    @EnumSource(value = TransferType::class)
    fun `test that resolved pending transfers are filtered out`(type: TransferType) =
        runTest {
            val list1 = listOf(
                createPendingTransfer(type),
                createPendingTransfer(type),
                createPendingTransfer(type, AlreadyStarted)
            )
            val list2 = listOf(
                createPendingTransfer(type),
                createPendingTransfer(type, ErrorStarting),
                createPendingTransfer(type, AlreadyStarted)
            )
            whenever(getPendingTransfersByTypeUseCase(type)) doReturn
                    flowOf(list1, list2)

            underTest(type).test {
                assertThat(awaitItem()).hasSize(list1.size)
                assertThat(awaitItem()).hasSize(list2.size)
                awaitComplete()
            }
        }

    @ParameterizedTest
    @EnumSource(value = TransferType::class)
    fun `test that flow completes after a delay when all pending transfers are resolved but not already started`(
        type: TransferType,
    ) =
        runTest {
            whenever(getPendingTransfersByTypeUseCase(type)) doReturn
                    flow {
                        emit(listOf(createPendingTransfer(type)))
                        emit(listOf(createPendingTransfer(type, SdkScanned)))
                        delay(100)
                        emit(listOf(createPendingTransfer(type, SdkScanned)))
                        delay(500)
                        //should not be received
                        emit(listOf(createPendingTransfer(type, AlreadyStarted)))
                        awaitCancellation()
                    }
            underTest(type).test {
                assertThat(awaitItem()).hasSize(1)
                assertThat(awaitItem()).hasSize(1)
                assertThat(awaitItem()).hasSize(1)
                awaitComplete()
            }
        }

    @ParameterizedTest
    @EnumSource(value = TransferType::class)
    fun `test that flow completes immediately when all pending transfers are resolved and already started`(
        type: TransferType,
    ) =
        runTest {
            whenever(getPendingTransfersByTypeUseCase(type)) doReturn
                    flow {
                        emit(listOf(createPendingTransfer(type)))
                        emit(listOf(createPendingTransfer(type, AlreadyStarted)))
                        //should not be received
                        emit(listOf(createPendingTransfer(type, AlreadyStarted)))
                        awaitCancellation()
                    }
            underTest(type).test {
                assertThat(awaitItem()).hasSize(1)
                assertThat(awaitItem()).hasSize(1)
                awaitComplete()
            }
        }

    private fun createPendingTransfer(
        type: TransferType,
        pendingTransferState: PendingTransferState = NotSentToSdk,
    ) = PendingTransfer(
        354L,
        appData = emptyList(),
        isHighPriority = false,
        nodeIdentifier = mock<PendingTransferNodeIdentifier.CloudDriveNode>(),
        uriPath = UriPath(""),
        transferType = type,
        state = pendingTransferState,
        fileName = null
    )
}