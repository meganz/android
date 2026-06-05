package mega.privacy.android.feature.cloudexplorer.presentation.importnodes

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
internal class ImportViewModelTest {

    private lateinit var underTest: ImportViewModel

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
            underTest = ImportViewModel(getRootNodeIdUseCase)

            underTest.uiState.test {
                var state: ImportUiState = awaitItem()
                if (state is ImportUiState.Loading) {
                    state = awaitItem()
                }
                assertThat(state).isEqualTo(ImportUiState.Data(rootNodeId = expectedRoot))
            }
        }

    @Test
    fun `test that uiState falls back to NodeId minus one when use case returns null`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doReturn null
            }
            underTest = ImportViewModel(getRootNodeIdUseCase)

            underTest.uiState.test {
                var state: ImportUiState = awaitItem()
                if (state is ImportUiState.Loading) {
                    state = awaitItem()
                }
                assertThat(state).isEqualTo(ImportUiState.Data(rootNodeId = NodeId(-1)))
            }
        }

    @Test
    fun `test that uiState falls back to NodeId minus one when use case throws`() =
        runTest(testDispatcher) {
            getRootNodeIdUseCase.stub {
                onBlocking { invoke() } doAnswer { throw RuntimeException("boom") }
            }
            underTest = ImportViewModel(getRootNodeIdUseCase)

            underTest.uiState.test {
                var state: ImportUiState = awaitItem()
                if (state is ImportUiState.Loading) {
                    state = awaitItem()
                }
                assertThat(state).isEqualTo(ImportUiState.Data(rootNodeId = NodeId(-1)))
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
