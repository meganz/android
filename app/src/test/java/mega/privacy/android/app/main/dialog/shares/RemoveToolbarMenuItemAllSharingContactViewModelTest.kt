package mega.privacy.android.app.main.dialog.shares

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactDialogFragment
import mega.privacy.android.app.main.dialog.shares.RemoveAllSharingContactViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.shares.GetOutShareByNodeIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RemoveToolbarMenuItemAllSharingContactViewModelTest {
    private lateinit var underTest: RemoveAllSharingContactViewModel
    private val savedStateHandle: SavedStateHandle = mock()
    private val getOutShareByNodeIdUseCase: GetOutShareByNodeIdUseCase = mock()

    @BeforeAll
    fun setup() {
        initTestClass()
    }

    @BeforeEach
    fun resetMocks() {
        reset(savedStateHandle, getOutShareByNodeIdUseCase)
    }

    @Test
    fun `test that numberOfShareContact update correctly when init view model with single node`() =
        runTest {
            val shares = List(5) {
                mock<ShareData>()
            }
            whenever(savedStateHandle.get<LongArray>(RemoveAllSharingContactDialogFragment.EXTRA_NODE_IDS)).thenReturn(
                longArrayOf(1)
            )
            whenever(getOutShareByNodeIdUseCase(NodeId(1L))).thenReturn(shares)
            initTestClass()
            underTest.state.test {
                val item = awaitItem()
                Truth.assertThat(item.numberOfShareContact).isEqualTo(shares.size)
                Truth.assertThat(item.numberOfShareFolder).isEqualTo(1)
            }
        }

    private fun initTestClass() {
        underTest = RemoveAllSharingContactViewModel(savedStateHandle, getOutShareByNodeIdUseCase)
    }
}