package test.mega.privacy.android.app.main.dialog.removelink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkViewModel
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemovePublicLinkViewModelTest {
    private lateinit var underTest: RemovePublicLinkViewModel
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initTestClass()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(getNodeByHandleUseCase, savedStateHandle)
    }

    private fun initTestClass() {
        underTest = RemovePublicLinkViewModel(getNodeByHandleUseCase, savedStateHandle)
    }

    @ParameterizedTest(name = "invoked with isTakenDown = {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isTakenDown update correctly when init view model with single node`(isDown: Boolean) =
        runTest {
            val node = mock<FileNode> {
                on { isTakenDown }.thenReturn(isDown)
            }
            whenever(savedStateHandle.get<LongArray>(RemoveAllSharingContactDialogFragment.EXTRA_NODE_IDS)).thenReturn(
                longArrayOf(1)
            )
            whenever(getNodeByHandleUseCase(1L)).thenReturn(node)
            initTestClass()
            underTest.state.test {
                Truth.assertThat(awaitItem().isNodeTakenDown).isEqualTo(isDown)
            }
        }
}