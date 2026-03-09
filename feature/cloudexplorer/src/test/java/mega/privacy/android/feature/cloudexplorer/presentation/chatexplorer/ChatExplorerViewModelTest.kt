package mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatExplorerViewModelTest {

    private lateinit var viewModel: ChatExplorerViewModel

    @BeforeEach
    fun setUp() {
        viewModel = ChatExplorerViewModel()
    }

    @Test
    fun `test that initial state is returned`() {
        assertThat(viewModel).isNotNull()
    }

    companion object {
        private val testDispatcher = UnconfinedTestDispatcher()

        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(testDispatcher)
    }
}
