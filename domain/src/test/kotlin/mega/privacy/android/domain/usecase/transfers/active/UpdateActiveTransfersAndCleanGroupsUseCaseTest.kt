package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferActionGroup
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateActiveTransfersAndCleanGroupsUseCaseTest {

    private lateinit var underTest: UpdateActiveTransfersAndCleanGroupsUseCase

    private val transferRepository = mock<TransferRepository>()
    private val updateActiveTransfersUseCase = mock<UpdateActiveTransfersUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = UpdateActiveTransfersAndCleanGroupsUseCase(
            transferRepository = transferRepository,
            updateActiveTransfersUseCase = updateActiveTransfersUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository, updateActiveTransfersUseCase)
    }

    @Test
    fun `test that active transfers are updated before using them`() = runTest {
        whenever(transferRepository.getCurrentActiveTransfers()).thenReturn(emptyList())
        whenever(transferRepository.getActiveTransferGroups()).thenReturn(emptyList())

        underTest()

        val inOrder = inOrder(updateActiveTransfersUseCase, transferRepository)
        inOrder.verify(updateActiveTransfersUseCase).invoke()
        inOrder.verify(transferRepository).getCurrentActiveTransfers()
    }

    @Test
    fun `test that when all groups are in use, nothing is deleted`() = runTest {
        val groupId1 = 100
        val groupId2 = 200

        val activeTransfer1 = mock<ActiveTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId1.toLong()))
        }
        val activeTransfer2 = mock<ActiveTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId2.toLong()))
        }

        val activeTransferGroup1 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId1
        }
        val activeTransferGroup2 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId2
        }

        whenever(transferRepository.getCurrentActiveTransfers())
            .thenReturn(listOf(activeTransfer1, activeTransfer2))
        whenever(transferRepository.getActiveTransferGroups())
            .thenReturn(listOf(activeTransferGroup1, activeTransferGroup2))

        underTest()

        verify(transferRepository, never()).deleteActiveTransferGroup(any())
    }

    @Test
    fun `test that when a group is not in use, it is deleted`() = runTest {
        val groupId1 = 100
        val groupId2 = 200

        val activeTransfer = mock<ActiveTransfer> {
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId1.toLong()))
        }

        val activeTransferGroup1 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId1
        }
        val activeTransferGroup2 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId2
        }

        whenever(transferRepository.getCurrentActiveTransfers())
            .thenReturn(listOf(activeTransfer))
        whenever(transferRepository.getActiveTransferGroups())
            .thenReturn(listOf(activeTransferGroup1, activeTransferGroup2))

        underTest()

        verify(transferRepository).deleteActiveTransferGroup(groupId2)
        verify(transferRepository, never()).deleteActiveTransferGroup(groupId1)
    }

    @Test
    fun `test that when there are no active transfers, all groups are deleted`() = runTest {
        val groupId1 = 100
        val groupId2 = 200

        val activeTransferGroup1 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId1
        }
        val activeTransferGroup2 = mock<ActiveTransferActionGroup> {
            on { groupId } doReturn groupId2
        }

        whenever(transferRepository.getCurrentActiveTransfers()).thenReturn(emptyList())
        whenever(transferRepository.getActiveTransferGroups())
            .thenReturn(listOf(activeTransferGroup1, activeTransferGroup2))

        underTest()

        verify(transferRepository).deleteActiveTransferGroup(groupId1)
        verify(transferRepository).deleteActiveTransferGroup(groupId2)
    }
}
