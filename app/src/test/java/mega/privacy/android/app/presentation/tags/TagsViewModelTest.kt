package mega.privacy.android.app.presentation.tags

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.MonitorNodeUpdatesById
import mega.privacy.android.domain.usecase.node.ManageNodeTagUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagsViewModelTest {

    private val manageNodeTagUseCase = mock<ManageNodeTagUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val monitorNodeUpdatesById = mock<MonitorNodeUpdatesById>()
    private val tagsValidationMessageMapper = mock<TagsValidationMessageMapper>()
    private lateinit var stateHandle: SavedStateHandle
    private lateinit var underTest: TagsViewModel

    @BeforeEach
    fun resetMock() {
        stateHandle = SavedStateHandle(mapOf(TagsActivity.NODE_ID to 123L))
        whenever(tagsValidationMessageMapper.invoke("")).thenReturn(Pair("", false))
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        underTest = TagsViewModel(
            manageNodeTagUseCase = manageNodeTagUseCase,
            getNodeByIdUseCase = getNodeByIdUseCase,
            monitorNodeUpdatesById = monitorNodeUpdatesById,
            tagsValidationMessageMapper = tagsValidationMessageMapper,
            stateHandle = stateHandle
        )
    }

    @AfterEach
    fun clear() {
        reset(
            manageNodeTagUseCase,
            getNodeByIdUseCase,
            monitorNodeUpdatesById,
            tagsValidationMessageMapper
        )
    }

    @Test
    fun `test that getNodeByHandle update uiState with nodeHandle and tags`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        val node = mock<TypedNode> {
            on { id } doReturn NodeId(123L)
            on { name } doReturn "tags"
            on { tags } doReturn listOf("tag1", "tag2")
        }
        whenever(getNodeByIdUseCase(NodeId(123L))).thenReturn(node)
        val nodeId = NodeId(123L)
        underTest.getNodeByHandle(nodeId)
        val uiState = underTest.uiState.value
        assertThat(uiState.tags).containsExactly("tag1", "tag2")
    }

    @Test
    fun `test that getNodeByHandle log error when getNodeByIdUseCase fails`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        whenever(getNodeByIdUseCase(NodeId(123L))).thenThrow(RuntimeException())
        val nodeHandle = NodeId(123L)
        underTest.getNodeByHandle(nodeHandle)
    }

    @Test
    fun `test that addNodeTag update node tags`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        whenever(manageNodeTagUseCase(NodeId(123L), newTag = "new tag")).thenReturn(Unit)
        underTest.addNodeTag("new tag")
        verify(manageNodeTagUseCase).invoke(NodeId(123L), newTag = "new tag")
    }

    @Test
    fun `test that validateTagName returns true when tag is valid`() = runTest {
        whenever(tagsValidationMessageMapper.invoke("tag")).thenReturn(Pair("", false))
        underTest.validateTagName("tag")
        verify(tagsValidationMessageMapper).invoke("tag", emptyList(), emptyList())
    }


    @Test
    fun `test monitorNodeUpdatesById updates node`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(
            flowOf(listOf(NodeChanges.Tags))
        )
        underTest.getNodeByHandle(NodeId(123L))
        verify(getNodeByIdUseCase, times(2)).invoke(NodeId(123L))
    }

    @Test
    fun `test that removeTag update node tags`() = runTest {
        whenever(monitorNodeUpdatesById.invoke(nodeId = NodeId(123L))).thenReturn(emptyFlow())
        whenever(manageNodeTagUseCase(NodeId(123L), oldTag = "old tag", newTag = null)).thenReturn(
            Unit
        )
        underTest.removeTag("old tag")
        verify(manageNodeTagUseCase).invoke(NodeId(123L), oldTag = "old tag", newTag = null)
    }
}