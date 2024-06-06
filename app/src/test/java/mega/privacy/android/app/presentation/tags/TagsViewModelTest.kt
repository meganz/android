package mega.privacy.android.app.presentation.tags

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.StateEventWithContent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.node.UpdateNodeTagUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagsViewModelTest {

    private val updateNodeTagUseCase = mock<UpdateNodeTagUseCase>()
    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private lateinit var stateHandle: SavedStateHandle
    private lateinit var underTest: TagsViewModel

    @BeforeEach
    fun setup() {
        stateHandle = SavedStateHandle(mapOf(TagsActivity.NODE_ID to 123L))
        underTest = TagsViewModel(updateNodeTagUseCase, getNodeByIdUseCase, stateHandle)
    }

    @Test
    fun `test that getNodeByHandle update uiState with nodeHandle and tags`() = runTest {
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
        whenever(getNodeByIdUseCase(NodeId(123L))).thenThrow(RuntimeException())
        val nodeHandle = NodeId(123L)
        underTest.getNodeByHandle(nodeHandle)
    }

    @Test
    fun `test that addNodeTag update node tags`() = runTest {
        whenever(updateNodeTagUseCase(NodeId(123L), newTag = "new tag")).thenReturn(Unit)
        underTest.addNodeTag("new tag")
        val uiState = underTest.uiState.value
        assertThat(uiState.informationMessage).isInstanceOf(StateEventWithContent::class.java)
    }

    @ParameterizedTest(name = "validateTagName should return {0} when tag is {1} and message is {2}")
    @MethodSource("validateTagNameProvider")
    fun `test that validateTagName should return true when tag is valid`(
        expected: Boolean,
        tag: String,
        message: String?,
    ) {
        val actual = underTest.validateTagName(tag)
        assertThat(actual).isEqualTo(expected)
        assertThat(underTest.uiState.value.message).isEqualTo(message)
    }

    private fun validateTagNameProvider(): Stream<Arguments> = Stream.of(
        Arguments.of(
            false,
            "",
            "Use tags to help you find and organise your data. Try tagging by year, location, project, or subject."
        ),
        Arguments.of(
            false,
            " ",
            "Use tags to help you find and organise your data. Try tagging by year, location, project, or subject."
        ),
        Arguments.of(false, "tag with space", "Tags can only contain letters and numbers."),
        Arguments.of(
            false,
            "tag with special characters!@#",
            "Tags can only contain letters and numbers."
        ),
        Arguments.of(true, "tag", null),
        Arguments.of(true, "tag123", null),
        Arguments.of(
            false,
            "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz",
            "Tags can be up to 32 characters long."
        )
    )
}