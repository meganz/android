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
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.UpdatePendingTransferStateUseCase
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
    private val fileSystemRepository = mock<FileSystemRepository>()

    private val mockedActiveTransfers = (0L..10L).map { mock<ActiveTransfer>() }
    private val mockedTransfers = (0L..10L).map { mock<Transfer>() }

    @BeforeAll
    fun setUp() {
        underTest = CorrectActiveTransfersUseCase(
            getInProgressTransfersUseCase = getInProgressTransfersUseCase,
            transferRepository = transferRepository,
            updatePendingTransferStateUseCase = updatePendingTransferStateUseCase,
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun cleanUp() = runTest {
        reset(
            transferRepository,
            getInProgressTransfersUseCase,
            updatePendingTransferStateUseCase,
            fileSystemRepository,
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

    private fun stubActiveTransfers(areFinished: Boolean) {
        mockedActiveTransfers.forEachIndexed { index, activeTransfer ->
            whenever(activeTransfer.uniqueId).thenReturn(index.toLong())
            whenever(activeTransfer.isFinished).thenReturn(areFinished)
            whenever(activeTransfer.localPath).thenReturn("path$index")
        }
    }

    private fun stubTransfers() {
        mockedTransfers.forEachIndexed { index, transfer ->
            whenever(transfer.uniqueId).thenReturn(index.toLong())
        }
    }

    private fun subSetTransfers() = mockedTransfers.filter { it.uniqueId.mod(2) == 0 }
    private fun subSetActiveTransfers() = mockedActiveTransfers.filter { it.uniqueId.mod(3) == 0 }

    @Test
    fun `test that active transfers not finished and not in progress are set as finished when uri path does exist`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            val inProgress = subSetTransfers()
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            val expected = mockedActiveTransfers.filter { activeTransfer ->
                inProgress.map { it.uniqueId }.contains(activeTransfer.uniqueId).not()
            }
            expected.forEach {
                whenever(fileSystemRepository.doesUriPathExist(UriPath(it.localPath))) doReturn true
            }
            Truth.assertThat(expected).isNotEmpty()
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository).setActiveTransfersAsFinishedByUniqueId(
                expected.map { it.uniqueId },
                false
            )
        }

    @Test
    fun `test that active transfers not finished and not in progress are set as cancelled when uri path does not exist`() =
        runTest {
            stubActiveTransfers(false)
            stubTransfers()
            whenever(transferRepository.getCurrentActiveTransfersByType(any()))
                .thenReturn(mockedActiveTransfers)
            val inProgress = subSetTransfers()
            whenever(getInProgressTransfersUseCase()).thenReturn(inProgress)
            val expected = mockedActiveTransfers.filter { activeTransfer ->
                inProgress.map { it.uniqueId }.contains(activeTransfer.uniqueId).not()
            }
            expected.forEach {
                whenever(fileSystemRepository.doesUriPathExist(UriPath(it.localPath))) doReturn false
            }
            Truth.assertThat(expected).isNotEmpty()
            underTest(TransferType.GENERAL_UPLOAD)
            verify(transferRepository).setActiveTransfersAsFinishedByUniqueId(
                expected.map { it.uniqueId },
                true
            )
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
            verify(transferRepository, never()).setActiveTransfersAsFinishedByUniqueId(
                uniqueIds = anyOrNull(),
                cancelled = eq(true)
            )
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
                !inProgress.map { it.uniqueId }.contains(transfer.uniqueId)
            }
            expected.forEach {
                whenever(fileSystemRepository.doesUriPathExist(UriPath(it.localPath))) doReturn true
            }
            Truth.assertThat(expected).isNotEmpty()
            underTest(TransferType.GENERAL_UPLOAD)

            verify(transferRepository).removeInProgressTransfers(expected.map { it.uniqueId }
                .toSet())
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
        val pendingTransfer1 = mock<PendingTransfer> { on { this.transferUniqueId } doReturn 1L }
        val pendingTransfer2 = mock<PendingTransfer> { on { this.transferUniqueId } doReturn 2L }
        val pendingTransfer3 = mock<PendingTransfer> { on { this.transferUniqueId } doReturn 3L }
        val pendingTransfers = listOf(pendingTransfer1, pendingTransfer2, pendingTransfer3)
        val transfer2 = mock<Transfer> { on { this.uniqueId } doReturn 2L }
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
        val pendingTransfer = mock<PendingTransfer> { on { this.transferUniqueId } doReturn 1 }
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
                on { this.transferUniqueId } doReturn 1
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