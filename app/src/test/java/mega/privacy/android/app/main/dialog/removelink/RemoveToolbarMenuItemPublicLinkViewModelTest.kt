package mega.privacy.android.app.main.dialog.removelink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.dialog.removelink.RemovePublicLinkViewModel
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.usecase.node.GetNodeByHandleUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoveToolbarMenuItemPublicLinkViewModelTest {
    private lateinit var underTest: RemovePublicLinkViewModel
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val savedStateHandle: SavedStateHandle = mock()

    @BeforeAll
    fun setup() {
        initTestClass()
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