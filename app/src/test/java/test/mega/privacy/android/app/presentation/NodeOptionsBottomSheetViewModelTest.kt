package test.mega.privacy.android.app.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsBottomSheetViewModel
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class NodeOptionsBottomSheetViewModelTest {

    private lateinit var underTest: NodeOptionsBottomSheetViewModel
    private val getNodeByHandle = mock<GetNodeByHandle>()
    private val createShareKey = mock<CreateShareKey>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        underTest = NodeOptionsBottomSheetViewModel(createShareKey, getNodeByHandle)
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            Truth.assertThat(initial.currentNodeHandle).isEqualTo(-1L)
            Truth.assertThat(initial.isCreateShareKeySuccess).isEqualTo(null)
        }
    }

    @Test
    fun `test that open share dialog success result gets updated in state`() = runTest {
        underTest.callOpenShareDialog(3829183L)
        underTest.state.runCatching {
            this.test {
                awaitItem().isCreateShareKeySuccess?.let { assertTrue(it) }
            }
        }
    }

    @Test
    fun `test that open share dialog failure result gets updated in state`() = runTest {
        underTest.callOpenShareDialog(-1)
        underTest.state.runCatching {
            this.test {
                awaitItem().isCreateShareKeySuccess?.let { assertFalse(it) }
            }
        }
    }

}