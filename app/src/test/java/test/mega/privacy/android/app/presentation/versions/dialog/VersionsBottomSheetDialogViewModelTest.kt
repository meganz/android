package test.mega.privacy.android.app.presentation.versions.dialog

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.versions.dialog.VersionsBottomSheetDialogViewModel
import nz.mega.sdk.MegaNode
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/**
 * Test class for [VersionsBottomSheetDialogViewModel]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VersionsBottomSheetDialogViewModelTest {

    private lateinit var underTest: VersionsBottomSheetDialogViewModel

    private val getNodeByHandle = mock<GetNodeByHandle>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun reset() {
        underTest = VersionsBottomSheetDialogViewModel(
            getNodeByHandle = getNodeByHandle,
        )
        reset(getNodeByHandle)
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val state = awaitItem()
            assertThat(state.node).isNull()
        }
    }

    @Test
    fun `test that the node is null by default if the node handle is null`() = runTest {
        underTest.init(null)
        underTest.state.test {
            assertThat(awaitItem().node).isNull()
        }

        verifyNoInteractions(getNodeByHandle)
    }

    @Test
    fun `test that the node is retrieved`() = runTest {
        val testNode = mock<MegaNode>()

        whenever(getNodeByHandle(any())).thenReturn(testNode)

        underTest.init(123456L)
        underTest.state.test {
            assertThat(awaitItem().node).isEqualTo(testNode)
        }
    }
}