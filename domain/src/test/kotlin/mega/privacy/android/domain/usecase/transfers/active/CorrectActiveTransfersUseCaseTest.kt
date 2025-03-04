package mega.privacy.android.domain.usecase.transfers.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.UpdatePendingTransferStateUseCase
import mega.privacy.android.domain.usecase.transfers.sd.HandleNotInProgressSDCardActiveTransfersUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CorrectActiveTransfersUseCaseTest {
    private lateinit var underTest: CorrectActiveTransfersUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getInProgressTransfersUseCase = mock<GetInProgressTransfersUseCase>()
    private val updatePendingTransferStateUseCase = mock<UpdatePendingTransferStateUseCase>()
    private val handleNotInProgressSDCardActiveTransfersUseCase =
        mock<HandleNotInProgressSDCardActiveTransfersUseCase>()

    private val mockedActiveTransfers = (0..10).map { mock<ActiveTransfer>() }
    private val mockedTransfers = (0..10).map { mock<Transfer>() }

    @BeforeAll
    fun setUp() {
        underTest = CorrectActiveTransfersUseCase(
            getInProgressTransfersUseCase = getInProgressTransfersUseCase,
            transferRepository = transferRepository,
            updatePendingTransferStateUseCase = updatePendingTransferStateUseCase,
            handleNotInProgressSDCardActiveTransfersUseCase = handleNotInProgressSDCardActiveTransfersUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() = runTest {
        reset(
            transferRepository,
            getInProgressTransfersUseCase,
            updatePendingTransferStateUseCase,
            *mockedActiveTransfers.toTypedArray(),
            *mockedTransfers.toTypedArray(),
        )
        whenever(
            transferRepository.monitorPendingTransfersByTypeAndState(
                anyOrNull(),
                any()
            )
        ) doReturn flowOf(emptyList())
        whenever(transferRepository.getCurrentActiveTransfersByType(any()))
            .thenReturn(emptyList())
        whenever(transferRepository.getCurrentActiveTransfers())
            .thenReturn(emptyList())
        whenever(transferRepository.getPendingTransfersByTypeAndState(any(), any()))
            .thenReturn(emptyList())
        whenever(transferRepository.getPendingTransfersByState(any()))
            .thenReturn(emptyList())
    }

    private fun stubActiveTransfers(areFinished: Boolean, isSdCardDownload: Boolean = false) {
        mockedActiveTransfers.forEachIndexed { index, activeTransfer ->
            whenever(activeTransfer.tag).thenReturn(index)
            whenever(activeTransfer.isFinished).thenReturn(areFinished)
            if (isSdCardDownload) {
                val data = mock<TransferAppData.SdCardDownload>()
                whenever(activeTransfer.appData).thenReturn(listOf(data))
            }
        }
    }

    private fun stubTransfers(isSdCardDownload: Boolean = false) {
        mockedTransfers.forEachIndexed { index, transfer ->
            whenever(transfer.tag).thenReturn(index)
            if (isSdCardDownload) {
                val data = mock<TransferAppData.SdCardDownload>()
                whenever(transfer.appData).thenReturn(listOf(data))
            }
        }
    }

    private fun subSetTransfers() = mockedTransfers.filter { it.tag.mod(2) == 0 }
    private fun subSetActiveTransfers() = mockedActiveTransfers.filter { it.tag.mod(3) == 0 }

    @Test
    fun `test that active transfers not finished and not in progress are set as cancelled`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            val inProgress = subSetTransfers()
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            val expected =
                mockedActiveTransfers.map { it.tag } - inProgress.map { it.tag }.toSet()
            Truth.assertThat(expected).isNotEmpty()
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository).setActiveTransferAsCancelledByTag(expected)
        }

    @Test
    fun `test that when there are active transfers not finished and not in progress which are SD card, HandleNotInProgressSDCardActiveTransfersUseCase is invoked`() =
        runTest {
            stubActiveTransfers(areFinished = false, isSdCardDownload = true)
            stubTransfers(isSdCardDownload = true)
            val inProgress = subSetTransfers()
            val expected = mockedActiveTransfers.filter { it.tag !in inProgress.map { it.tag } }

            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)

            underTest(TransferType.DOWNLOAD)

            verify(handleNotInProgressSDCardActiveTransfersUseCase).invoke(expected.associate { it.tag to it.appData })
        }

    @Test
    fun `test that active transfers finished and not in progress are not set as cancelled`() =
        runTest {
            stubActiveTransfers(true)
            stubTransfers()
            val inProgress = subSetTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository, never()).setActiveTransferAsCancelledByTag(anyOrNull())
        }

    @Test
    fun `test that in progress transfers not in active transfers are added`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            val alreadyInDataBase = subSetActiveTransfers()
            val notInDataBase = mockedActiveTransfers - alreadyInDataBase.toSet()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(alreadyInDataBase)
            whenever(getInProgressTransfersUseCase()).thenReturn(mockedTransfers)
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository).insertOrUpdateActiveTransfers(argThat { it ->
                it.map { it.tag } == notInDataBase.map { it.tag }
            })
        }

    @Test
    fun `test that active transfers not finished and not in progress are removed as in progress transfers`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            val inProgress = subSetTransfers()
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            val expected = mockedActiveTransfers.filter { transfer ->
                !inProgress.map { it.tag }.contains(transfer.tag)
            }
            Truth.assertThat(expected).isNotEmpty()
            underTest(TransferType.GENERAL_UPLOAD)

            verify(transferRepository).removeInProgressTransfers(expected.map { it.tag }.toSet())
        }

    @Test
    fun `test that in progress transfers not in active transfers are updated in in progress transfers`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            val alreadyInDataBase = subSetActiveTransfers()
            val notInDataBase = mockedActiveTransfers - alreadyInDataBase.toSet()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(alreadyInDataBase)
            whenever(getInProgressTransfersUseCase()).thenReturn(mockedTransfers)
            underTest(TransferType.GENERAL_UPLOAD)

            verify(transferRepository).updateInProgressTransfers(
                argThat { this.map { it.tag } == notInDataBase.map { it.tag } }
            )
        }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    @NullSource
    fun `test that pending transfers waiting for sdk scanning not known by sdk are set as errors`(
        transferType: TransferType?,
    ) = runTest {
        val pendingTransfer1 = mock<PendingTransfer> { on { this.transferTag } doReturn 1 }
        val pendingTransfer2 = mock<PendingTransfer> { on { this.transferTag } doReturn 2 }
        val pendingTransfer3 = mock<PendingTransfer> { on { this.transferTag } doReturn 3 }
        val pendingTransfers = listOf(pendingTransfer1, pendingTransfer2, pendingTransfer3)
        val transfer2 = mock<Transfer> { on { this.tag } doReturn 2 }
        val expected = listOf(pendingTransfer1, pendingTransfer3)
        if (transferType == null) {
            whenever(
                transferRepository
                    .getPendingTransfersByState(
                        PendingTransferState.SdkScanning
                    )
            ) doReturn pendingTransfers
        } else {
            whenever(
                transferRepository
                    .getPendingTransfersByTypeAndState(
                        transferType,
                        PendingTransferState.SdkScanning
                    )
            ) doReturn pendingTransfers
        }
        whenever(getInProgressTransfersUseCase()) doReturn listOf(transfer2)

        underTest(transferType)

        verify(updatePendingTransferStateUseCase)(expected, PendingTransferState.ErrorStarting)
        verify(transferRepository)
            .addCompletedTransferFromFailedPendingTransfers(eq(expected), any())
    }

    @Test
    fun `test no unnecessary calls are done when there are no pending transfers waiting for sdk scanning not known by sdk`(
    ) = runTest {
        val transferType = TransferType.DOWNLOAD
        val pendingTransfer = mock<PendingTransfer> { on { this.transferTag } doReturn 1 }
        val pendingTransfers = listOf(pendingTransfer)
        val transfer = mock<Transfer> { on { this.tag } doReturn 1 }
        whenever(
            transferRepository
                .monitorPendingTransfersByTypeAndState(
                    transferType,
                    PendingTransferState.SdkScanning
                )
        ) doReturn flowOf(pendingTransfers)
        whenever(getInProgressTransfersUseCase()) doReturn listOf(transfer)

        underTest(transferType)

        verifyNoInteractions(updatePendingTransferStateUseCase)
        verify(transferRepository, never()).addCompletedTransferFromFailedPendingTransfers(
            any(),
            any()
        )
    }

    @Test
    fun `test that pending preview transfers waiting for sdk scanning not known by sdk are not set as completed`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer> {
                on { this.transferTag } doReturn 1
                on { appData } doReturn listOf(TransferAppData.PreviewDownload)
            }
            val pendingTransfers = listOf(pendingTransfer)

            whenever(
                transferRepository
                    .getPendingTransfersByTypeAndState(
                        TransferType.DOWNLOAD,
                        PendingTransferState.SdkScanning
                    )
            ) doReturn pendingTransfers
            whenever(getInProgressTransfersUseCase()) doReturn emptyList()

            underTest(TransferType.DOWNLOAD)

            verify(updatePendingTransferStateUseCase).invoke(
                pendingTransfers,
                PendingTransferState.ErrorStarting
            )
            verify(transferRepository, never()).addCompletedTransferFromFailedPendingTransfers(
                any(),
                any()
            )
        }

    @Test
    fun `test that voice clip transfers are filtered`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.CHAT_UPLOAD
                on { this.appData } doReturn listOf(TransferAppData.VoiceClip)
            }
            val transfers = subSetTransfers()
            val inProgress = buildList {
                addAll(transfers)
                add(transfer)
            }

            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)

            underTest.invoke(TransferType.CHAT_UPLOAD)

            verify(transferRepository).updateTransferredBytes(eq(transfers))
        }

    @Test
    fun `test that background transfers are filtered`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.appData } doReturn listOf(TransferAppData.BackgroundTransfer)
            }
            val transfers = subSetTransfers()
            val inProgress = buildList {
                addAll(transfers)
                add(transfer)
            }

            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)

            underTest.invoke(TransferType.DOWNLOAD)

            verify(transferRepository).updateTransferredBytes(eq(transfers))
        }

    @Test
    fun `test that streaming transfers are filtered`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.isStreamingTransfer } doReturn true
            }
            val transfers = subSetTransfers()
            val inProgress = buildList {
                addAll(transfers)
                add(transfer)
            }

            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)

            underTest.invoke(TransferType.DOWNLOAD)

            verify(transferRepository).updateTransferredBytes(eq(transfers))
        }
}