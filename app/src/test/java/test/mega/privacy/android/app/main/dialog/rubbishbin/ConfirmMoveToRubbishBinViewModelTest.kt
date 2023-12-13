package test.mega.privacy.android.app.main.dialog.rubbishbin

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinDialogFragment
import mega.privacy.android.app.main.dialog.rubbishbin.ConfirmMoveToRubbishBinViewModel
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import mega.privacy.android.domain.usecase.IsSecondaryFolderEnabled
import mega.privacy.android.domain.usecase.camerauploads.GetPrimarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondarySyncHandleUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConfirmMoveToRubbishBinViewModelTest {
    private lateinit var underTest: ConfirmMoveToRubbishBinViewModel
    private val isNodeInRubbish: IsNodeInRubbish = mock()
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase = mock()
    private val isSecondaryFolderEnabled: IsSecondaryFolderEnabled = mock()
    private val getPrimarySyncHandleUseCase: GetPrimarySyncHandleUseCase = mock()
    private val getSecondarySyncHandleUseCase: GetSecondarySyncHandleUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isNodeInRubbish,
            isCameraUploadsEnabledUseCase,
            isSecondaryFolderEnabled,
            getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase,
            savedStateHandle,
        )
    }

    private fun initTestClass() {
        underTest = ConfirmMoveToRubbishBinViewModel(
            isNodeInRubbish = isNodeInRubbish,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            isSecondaryFolderEnabled = isSecondaryFolderEnabled,
            getPrimarySyncHandleUseCase = getPrimarySyncHandleUseCase,
            getSecondarySyncHandleUseCase = getSecondarySyncHandleUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @ParameterizedTest(name = "test that isNodeInRubbish is set correctly when isNodeInRubbish use-case returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isNodeInRubbish is updated correctly`(boolean: Boolean) =
        runTest {
            whenever(savedStateHandle.get<LongArray>(ConfirmMoveToRubbishBinDialogFragment.EXTRA_HANDLES)).thenReturn(
                longArrayOf(1L)
            )
            whenever(isNodeInRubbish(1L)).thenReturn(boolean)
            initTestClass()
            underTest.state.test {
                val state = awaitItem()
                Truth.assertThat(state.isNodeInRubbish).isEqualTo(boolean)
            }
        }


    @ParameterizedTest(name = "isPrimaryCuFolderHandle {0}, handle is {1} and CU primary handle is {2}")
    @MethodSource("provideCuParameters")
    fun `test that isPrimaryCuFolderHandle updated correctly when calling isCameraUploadsEnabledUseCase and getPrimarySyncHandleUseCase`(
        isCameraUploadsEnabled: Boolean,
        handle: Long,
        primaryCuHandle: Long,
        expected: Boolean,
    ) = runTest {
        whenever(savedStateHandle.get<LongArray>(ConfirmMoveToRubbishBinDialogFragment.EXTRA_HANDLES)).thenReturn(
            longArrayOf(handle)
        )
        whenever(isCameraUploadsEnabledUseCase()).thenReturn(isCameraUploadsEnabled)
        whenever(getPrimarySyncHandleUseCase()).thenReturn(primaryCuHandle)
        initTestClass()
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.isCameraUploadsPrimaryNodeHandle).isEqualTo(expected)
        }
    }

    @ParameterizedTest(name = "isSecondaryFolderEnabled {0}, handle is {1} and CU secondary handle is {2}")
    @MethodSource("provideCuParameters")
    fun `test that isSecondaryCuFolderHandle updated correctly when calling isSecondaryFolderEnabled and getSecondarySyncHandleUseCase`(
        isSecondaryFolderEnabled: Boolean,
        handle: Long,
        secondaryCuHandle: Long,
        expected: Boolean,
    ) = runTest {
        whenever(savedStateHandle.get<LongArray>(ConfirmMoveToRubbishBinDialogFragment.EXTRA_HANDLES)).thenReturn(
            longArrayOf(handle)
        )
        whenever(isSecondaryFolderEnabled()).thenReturn(isSecondaryFolderEnabled)
        whenever(getSecondarySyncHandleUseCase()).thenReturn(secondaryCuHandle)
        initTestClass()
        underTest.state.test {
            val state = awaitItem()
            Truth.assertThat(state.isCameraUploadsSecondaryNodeHandle).isEqualTo(expected)
        }
    }

    private fun provideCuParameters(): Stream<Arguments> =
        Stream.of(
            *arrayOf(
                Arguments.of(false, 1L, 2L, false),
                Arguments.of(false, 1L, 1L, false),
                Arguments.of(true, 1L, 2L, false),
                Arguments.of(true, 1L, 1L, true),
            )
        )
}
