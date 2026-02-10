package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateActiveTransfersUseCaseTest {

    private lateinit var underTest: UpdateActiveTransfersUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getInProgressTransfersUseCase = mock<GetInProgressTransfersUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateActiveTransfersUseCase(
            transferRepository = transferRepository,
            getInProgressTransfersUseCase = getInProgressTransfersUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(transferRepository, getInProgressTransfersUseCase)
        whenever(transferRepository.getActiveTransferGroups()).thenReturn(emptyList())
        whenever(transferRepository.getCompletedTransfers()).thenReturn(emptyList())
        whenever(getInProgressTransfersUseCase()).thenReturn(emptyList())
    }

    @Test
    fun `test that in progress transfers are inserted as active transfers`() = runTest {
        val inProgressTransfer1 = mock<Transfer> {
            on { uniqueId } doReturn 1L
        }
        val inProgressTransfer2 = mock<Transfer> {
            on { uniqueId } doReturn 2L
        }
        val inProgressTransfers = listOf(inProgressTransfer1, inProgressTransfer2)

        whenever(getInProgressTransfersUseCase()).thenReturn(inProgressTransfers)

        underTest()

        verify(transferRepository).putActiveTransfers(
            argThat { this.size == 2 && this.containsAll(inProgressTransfers) }
        )
    }

    @Test
    fun `test that completed transfers with matching transfer groups are included`() = runTest {
        val groupId1 = 100L
        val groupId2 = 200L

        val completedTransfer1 = mock<CompletedTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId1))
        }
        val completedTransfer2 = mock<CompletedTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId2))
        }
        val completedTransfers = listOf(completedTransfer1, completedTransfer2)

        val activeTransferGroup1 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId1.toInt()
        }
        val activeTransferGroup2 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId2.toInt()
        }
        val activeTransferGroups = listOf(activeTransferGroup1, activeTransferGroup2)

        whenever(transferRepository.getCompletedTransfers()).thenReturn(completedTransfers)
        whenever(transferRepository.getActiveTransferGroups()).thenReturn(activeTransferGroups)

        underTest()

        verify(transferRepository).putActiveTransfers(
            argThat { transfers ->
                transfers.size == 2 &&
                        transfers.containsAll(completedTransfers)
            }
        )
    }

    @Test
    fun `test that completed transfers without transfer groups are excluded`() = runTest {
        val groupId1 = 100L
        val groupId2 = 200L

        val completedTransfer1 = mock<CompletedTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId1))
        }
        val completedTransfer2 = mock<CompletedTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId2))
        }
        val completedTransfer3 = mock<CompletedTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(300L))
        }
        val completedTransfers = listOf(completedTransfer1, completedTransfer2, completedTransfer3)

        val activeTransferGroup1 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId1.toInt()
        }
        val activeTransferGroup2 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId2.toInt()
        }
        val activeTransferGroups = listOf(activeTransferGroup1, activeTransferGroup2)

        whenever(transferRepository.getCompletedTransfers()).thenReturn(completedTransfers)
        whenever(transferRepository.getActiveTransferGroups()).thenReturn(activeTransferGroups)

        underTest()

        verify(transferRepository).putActiveTransfers(
            argThat { transfers ->
                transfers.size == 2 && !transfers.contains(completedTransfer3)
            }
        )
    }

    @Test
    fun `test that in progress transfers and matching completed transfers are combined`() =
        runTest {
            val inProgressTransfer = mock<Transfer> {
                on { uniqueId } doReturn 1L
            }

            val groupId = 100L
            val completedTransfer = mock<CompletedTransfer> {
                on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId))
            }

            val activeTransferGroup = mock<ActiveTransferActionGroup> {
                on { this.groupId } doReturn groupId.toInt()
            }

            whenever(getInProgressTransfersUseCase()).thenReturn(listOf(inProgressTransfer))
            whenever(transferRepository.getCompletedTransfers()).thenReturn(listOf(completedTransfer))
            whenever(transferRepository.getActiveTransferGroups())
                .thenReturn(listOf(activeTransferGroup))

            underTest()

            verify(transferRepository).putActiveTransfers(
                argThat { transfers ->
                    transfers.size == 2 &&
                            transfers.contains(inProgressTransfer) &&
                            transfers.contains(completedTransfer)
                }
            )
        }
}
