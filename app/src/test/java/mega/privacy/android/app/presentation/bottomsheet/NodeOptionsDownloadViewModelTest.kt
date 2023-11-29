package mega.privacy.android.app.presentation.bottomsheet

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.transfers.startdownload.model.TransferTriggerEvent
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.node.chat.GetChatFileUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeOptionsDownloadViewModelTest {

    private lateinit var underTest: NodeOptionsDownloadViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getChatFileUseCase = mock<GetChatFileUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(getFeatureFlagValueUseCase, getChatFileUseCase, getNodeByIdUseCase)
    }

    private fun initViewModel() {
        underTest = NodeOptionsDownloadViewModel(
            getFeatureFlagValueUseCase,
            getChatFileUseCase,
            getNodeByIdUseCase,
        )
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that shouldDownloadWithDownloadWorker returns feature flag value`(
        expected: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(AppFeatures.DownloadWorker)).thenReturn(expected)
        assertThat(underTest.shouldDownloadWithDownloadWorker()).isEqualTo(expected)
    }

    @Test
    fun `test that onDownloadClicked launches the correct event`() = runTest {
        val node = mock<TypedFileNode>()
        underTest.onDownloadClicked(node)
        assertStartDownloadNode(node)
    }

    @Test
    fun `test that onSaveOfflineClicked launches the correct event`() = runTest {
        val node = mock<TypedFileNode>()
        underTest.onSaveOfflineClicked(node)
        assertStartDownloadForOffline(node)
    }

    @Test
    fun `test that onDownloadClicked with id launches the correct event`() = runTest {
        val nodeId = NodeId(1L)
        val node = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        underTest.onDownloadClicked(nodeId)
        assertStartDownloadNode(node)
    }

    @Test
    fun `test that onSaveOfflineClicked with id launches the correct event`() = runTest {
        val nodeId = NodeId(1L)
        val node = mock<TypedFileNode>()
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        underTest.onSaveOfflineClicked(nodeId)
        assertStartDownloadForOffline(node)
    }

    @Test
    fun `test that onDownloadClicked for chat file launches the correct event`() = runTest {
        val chatId = 11L
        val messageId = 22L
        val chatFile = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
        underTest.onDownloadClicked(chatId, messageId)
        assertStartDownloadNode(chatFile)
    }

    @Test
    fun `test that onSaveOfflineClicked for chat file launches the correct event`() = runTest {
        val chatId = 11L
        val messageId = 22L
        val chatFile = mock<ChatDefaultFile>()
        whenever(getChatFileUseCase(chatId, messageId)).thenReturn(chatFile)
        underTest.onSaveOfflineClicked(chatId, messageId)
        assertStartDownloadForOffline(chatFile)
    }

    private suspend fun assertStartDownloadNode(node: TypedNode) {
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadNode::class.java)
            assertThat((content as TransferTriggerEvent.StartDownloadNode).nodes)
                .containsExactly(node)
        }
    }

    private suspend fun assertStartDownloadForOffline(node: TypedNode) {
        underTest.state.test {
            val event = awaitItem()
            assertThat(event).isInstanceOf(StateEventWithContentTriggered::class.java)
            val content = (event as StateEventWithContentTriggered).content
            assertThat(content).isInstanceOf(TransferTriggerEvent.StartDownloadForOffline::class.java)
            assertThat((content as TransferTriggerEvent.StartDownloadForOffline).node)
                .isEqualTo(node)
        }
    }
}