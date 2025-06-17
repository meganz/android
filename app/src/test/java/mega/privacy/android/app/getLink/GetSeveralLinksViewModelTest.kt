package mega.privacy.android.app.getLink

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_NONE_SENSITIVE
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_FOLDER
import mega.privacy.android.app.getLink.GetLinkActivity.Companion.HIDDEN_NODE_WARNING_TYPE_LINKS
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.HasSensitiveDescendantUseCase
import mega.privacy.android.domain.usecase.HasSensitiveInheritedUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
class GetSeveralLinksViewModelTest {
    private lateinit var underTest: GetSeveralLinksViewModel

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val hasSensitiveInheritedUseCase = mock<HasSensitiveInheritedUseCase>()
    private val hasSensitiveDescendantUseCase = mock<HasSensitiveDescendantUseCase>()

    @BeforeEach
    fun setUp() {
        initUnderTest()
    }

    private fun initUnderTest() {
        underTest = GetSeveralLinksViewModel(
            megaApi = mock(),
            exportNodesUseCase = mock(),
            hasSensitiveDescendantUseCase = hasSensitiveDescendantUseCase,
            hasSensitiveInheritedUseCase = hasSensitiveInheritedUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            get1On1ChatIdUseCase = mock(),
            sendTextMessageUseCase = mock(),
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            hasSensitiveDescendantUseCase,
            hasSensitiveInheritedUseCase
        )
    }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_NONE_SENSITIVE when exportedData is not null`() =
        runTest {
            val handles = listOf(10000L, 20000L, 30000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(nodeId)
                    on { exportedData }.thenReturn(mock())
                }
            }
            typedNodes.onEachIndexed { index, node ->
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
            }

            underTest.checkSensitiveItems(handles)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_NONE_SENSITIVE)
            }
        }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_WARNING_TYPE_LINKS when isMarkedSensitive is true`() =
        runTest {
            val handles = listOf(10000L, 20000L, 30000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(nodeId)
                    on { isMarkedSensitive }.thenReturn(true)
                }
            }
            typedNodes.onEachIndexed { index, node ->
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
            }

            underTest.checkSensitiveItems(handles)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_LINKS)
            }
        }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_WARNING_TYPE_LINKS when hasSensitiveInheritedUseCase returns true`() =
        runTest {
            val handles = listOf(10000L, 20000L, 30000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFileNode> {
                    on { id }.thenReturn(nodeId)
                }
            }
            typedNodes.onEachIndexed { index, node ->
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
            }
            whenever(hasSensitiveInheritedUseCase(nodeIds[0])).thenReturn(true)

            underTest.checkSensitiveItems(handles)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_LINKS)
            }
        }

    @Test
    fun `test that hasSensitiveItems updated the value is HIDDEN_NODE_WARNING_TYPE_FOLDER`() =
        runTest {
            val handles = listOf(10000L, 20000L, 30000L)
            val nodeIds = handles.map { NodeId(it) }
            val typedNodes = nodeIds.map { nodeId ->
                mock<TypedFolderNode> {
                    on { id }.thenReturn(nodeId)
                    on { isMarkedSensitive }.thenReturn(false)
                }
            }
            typedNodes.onEachIndexed { index, node ->
                whenever(getNodeByIdUseCase(nodeIds[index])).thenReturn(node)
                whenever(hasSensitiveDescendantUseCase(nodeIds[index])).thenReturn(true)
            }
            whenever(hasSensitiveInheritedUseCase(any())).thenReturn(false)

            underTest.checkSensitiveItems(handles)

            underTest.hasSensitiveItemsFlow.test {
                assertThat(awaitItem()).isEqualTo(HIDDEN_NODE_WARNING_TYPE_FOLDER)
            }
        }
}