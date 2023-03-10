package test.mega.privacy.android.app.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.CreateShareKey
import mega.privacy.android.app.domain.usecase.GetNodeByHandle
import mega.privacy.android.app.presentation.bottomsheet.NodeOptionsViewModel
import mega.privacy.android.app.presentation.bottomsheet.model.NodeBottomSheetUIState
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.usecase.MonitorConnectivity
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@ExperimentalCoroutinesApi
class NodeOptionsViewModelTest {

    private lateinit var underTest: NodeOptionsViewModel
    private val getNodeByHandle =
        mock<GetNodeByHandle> { onBlocking { invoke(any()) }.thenReturn(null) }
    private val createShareKey = mock<CreateShareKey>()
    private val monitorConnectivity = mock<MonitorConnectivity> {
        onBlocking { invoke() }.thenReturn(
            MutableStateFlow(true)
        )
    }

    private val nodeIdFlow = MutableStateFlow(-1L)

    private val savedStateHandle = mock<SavedStateHandle> {
        on {
            getStateFlow(
                NodeOptionsViewModel.NODE_ID_KEY,
                -1L
            )
        }.thenReturn(nodeIdFlow)

        on {
            getStateFlow<ShareData?>(NodeOptionsViewModel.SHARE_DATA_KEY, null)
        }.thenReturn(MutableStateFlow(null))
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        underTest = NodeOptionsViewModel(
            createShareKey = createShareKey,
            getNodeByHandle = getNodeByHandle,
            monitorConnectivity = monitorConnectivity,
            savedStateHandle = savedStateHandle
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.node).isNull()
            assertThat(initial.shareData).isNull()
            assertThat(initial.shareKeyCreated).isNull()
            assertThat(initial.isOnline).isTrue()
        }
    }

    @Test
    fun `test that shareKeyCreated is true if created successfully`() = runTest {
        val node = mock<MegaNode>()
        val nodeId = 123L
        getNodeByHandle.stub {
            onBlocking { invoke(nodeId) }.thenReturn(node)
        }
        nodeIdFlow.emit(nodeId)

        underTest.state.filter { it.node != null }
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                assertThat(awaitItem().shareKeyCreated).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isTrue()
            }
    }

    @Test
    fun `test that shareKeyCreated is false if created throws an exception`() = runTest {
        val node = mock<MegaNode>()
        val nodeId = 123L
        getNodeByHandle.stub {
            onBlocking { invoke(nodeId) }.thenReturn(node)
        }
        nodeIdFlow.emit(nodeId)

        createShareKey.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw Throwable() }
        }
        underTest.state.filter { it.node != null }
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                assertThat(awaitItem().shareKeyCreated).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isFalse()
            }
    }

    @Test
    fun `test that shareKeyCreated is false if called with null node`() = runTest{
        createShareKey.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw Throwable() }
        }

        underTest.state
            .distinctUntilChangedBy(NodeBottomSheetUIState::shareKeyCreated)
            .test {
                val initialState = awaitItem()
                assertThat(initialState.shareKeyCreated).isNull()
                assertThat(initialState.node).isNull()
                underTest.createShareKey()
                testScheduler.advanceUntilIdle()
                assertThat(awaitItem().shareKeyCreated).isFalse()
            }
    }

}