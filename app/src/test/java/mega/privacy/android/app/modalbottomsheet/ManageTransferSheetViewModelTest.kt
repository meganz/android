package mega.privacy.android.app.modalbottomsheet

import androidx.annotation.Nullable
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.shares.GetNodeAccessPermission
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfers.completed.GetCompletedTransferByIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManageTransferSheetViewModelTest {
    private lateinit var underTest: ManageTransferSheetViewModel
    private val getCompletedTransferByIdUseCase: GetCompletedTransferByIdUseCase = mock()
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase = mock()
    private val getNodeAccessPermission = mock<GetNodeAccessPermission>()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeAll
    fun setup() {
    }

    private fun initTestClass() {
        underTest = ManageTransferSheetViewModel(
            getCompletedTransferByIdUseCase = getCompletedTransferByIdUseCase,
            deleteCompletedTransferUseCase = deleteCompletedTransferUseCase,
            getNodeAccessPermission = getNodeAccessPermission,
            savedStateHandle = savedStateHandle
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCompletedTransferByIdUseCase,
            deleteCompletedTransferUseCase,
            getNodeAccessPermission,
            savedStateHandle
        )
    }

    @Test
    fun `test that ManageTransferSheetUiState update correctly`() = runTest {
        val completedTransfer = mock<CompletedTransfer>()
        whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID)).thenReturn(
            1
        )
        whenever(getCompletedTransferByIdUseCase(1)).thenReturn(completedTransfer)
        initTestClass()
        underTest.uiState.test {
            assertThat(awaitItem().transfer).isEqualTo(completedTransfer)
        }
    }

    @ParameterizedTest(name = " and use case returns {0}")
    @EnumSource(AccessPermission::class)
    @Nullable
    fun `test that when completed transfer is obtained, getNodeAccessPermission is invoked and state is updated correctly`(
        accessPermission: AccessPermission?,
    ) = runTest {
        val transferId = 1
        val handle = 234L
        val completedTransfer = mock<CompletedTransfer> {
            on { this.handle } doReturn handle
        }

        whenever(savedStateHandle.get<Int>(ManageTransferBottomSheetDialogFragment.TRANSFER_ID))
            .thenReturn(transferId)
        whenever(getCompletedTransferByIdUseCase(transferId)).thenReturn(completedTransfer)
        whenever(getNodeAccessPermission(NodeId(handle))).thenReturn(accessPermission)

        initTestClass()

        underTest.uiState.test {
            assertThat(awaitItem().iAmNodeOwner)
                .isEqualTo(accessPermission == AccessPermission.OWNER)
        }
    }

    @Test
    fun `test that completedTransferRemoved invoke correctly`() = runTest {
        val completedTransfer = mock<CompletedTransfer>()
        underTest.completedTransferRemoved(completedTransfer, true)
        verify(deleteCompletedTransferUseCase).invoke(completedTransfer, true)
    }
}