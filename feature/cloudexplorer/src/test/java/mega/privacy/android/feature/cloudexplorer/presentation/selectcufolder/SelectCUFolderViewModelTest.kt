package mega.privacy.android.feature.cloudexplorer.presentation.selectcufolder

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.GetRootNodeIdUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SelectCUFolderViewModelTest {

    private lateinit var underTest: SelectCUFolderViewModel

    private val getRootNodeIdUseCase = mock<GetRootNodeIdUseCase>()

    @BeforeEach
    fun setUp() {
        reset(getRootNodeIdUseCase)
    }

    @Test
    fun `test that uiState emits Data with root node id when use case succeeds`() =
        runTest(testDispatcher) {
            val expectedRoot = NodeId(42L)
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doReturn expectedRoot
            }
            underTest = SelectCUFolderViewModel(getRootNodeIdUseCase)

            underTest.uiState.test {
                var state: SelectCUFolderUiState = awaitItem()
                if (state is SelectCUFolderUiState.Loading) {
                    state = awaitItem()
                }
                assertThat(state).isEqualTo(SelectCUFolderUiState.Data(rootNodeId = expectedRoot))
            }
        }

    @Test
    fun `test that uiState falls back to NodeId minus one when use case returns null`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doReturn null
            }
            underTest = SelectCUFolderViewModel(getRootNodeIdUseCase)

            underTest.uiState.test {
                var state: SelectCUFolderUiState = awaitItem()
                if (state is SelectCUFolderUiState.Loading) {
                    state = awaitItem()
                }
                assertThat(state).isEqualTo(SelectCUFolderUiState.Data(rootNodeId = NodeId(-1)))
            }
        }

    @Test
    fun `test that uiState falls back to NodeId minus one when use case throws`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doAnswer { throw RuntimeException("boom") }
            }
            underTest = SelectCUFolderViewModel(getRootNodeIdUseCase)

            underTest.uiState.test {
                var state: SelectCUFolderUiState = awaitItem()
                if (state is SelectCUFolderUiState.Loading) {
                    state = awaitItem()
                }
                assertThat(state).isEqualTo(SelectCUFolderUiState.Data(rootNodeId = NodeId(-1)))
            }
        }

    companion object {
        @JvmField
        val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
