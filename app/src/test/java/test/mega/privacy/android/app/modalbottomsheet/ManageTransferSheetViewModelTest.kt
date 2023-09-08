package test.mega.privacy.android.app.modalbottomsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.modalbottomsheet.ManageTransferBottomSheetDialogFragment
import mega.privacy.android.app.modalbottomsheet.ManageTransferSheetViewModel
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.usecase.transfer.DeleteCompletedTransferUseCase
import mega.privacy.android.domain.usecase.transfer.GetCompletedTransferByIdUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManageTransferSheetViewModelTest {
    private lateinit var underTest: ManageTransferSheetViewModel
    private val getCompletedTransferByIdUseCase: GetCompletedTransferByIdUseCase = mock()
    private val deleteCompletedTransferUseCase: DeleteCompletedTransferUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initTestClass()
    }

    private fun initTestClass() {
        underTest = ManageTransferSheetViewModel(
            getCompletedTransferByIdUseCase,
            deleteCompletedTransferUseCase,
            savedStateHandle
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCompletedTransferByIdUseCase,
            deleteCompletedTransferUseCase,
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
            Truth.assertThat(awaitItem().transfer).isEqualTo(completedTransfer)
        }
    }

    @Test
    fun `test that completedTransferRemoved invoke correctly`() = runTest {
        val completedTransfer = mock<CompletedTransfer>()
        underTest.completedTransferRemoved(completedTransfer, true)
        verify(deleteCompletedTransferUseCase).invoke(completedTransfer, true)
    }
}