package mega.privacy.android.core.nodecomponents.menu.menuitem

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.AddToPlaylistMenuAction
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddToPlaylistBottomSheetMenuItemTest {

    @ParameterizedTest(name = "when {0}")
    @MethodSource("provideTestSelectedNode")
    fun `test shouldDisplay returns correctly`(
        @Suppress("UNUSED_PARAMETER")
        testDescription: String,
        isDisplayed: Boolean,
        selectedNode: TypedNode,
    ) = runTest {
        val addToPlaylistMenuItem =
            AddToPlaylistBottomSheetMenuItem(mock<AddToPlaylistMenuAction>())

        val result = addToPlaylistMenuItem.shouldDisplay(
            isNodeInRubbish = false,
            accessPermission = null,
            isInBackups = false,
            node = selectedNode,
            isConnected = true,
        )

        assertThat(result).isEqualTo(isDisplayed)
    }

    private fun provideTestSelectedNode() = listOf(
        Arguments.of("typedNode is a video file", true, createVideoNode()),
        Arguments.of("typedNode is not a video file", false, mock<TypedFileNode>()),
        Arguments.of("typedNode is a folder", false, mock<TypedFolderNode>()),
    )

    private fun createVideoNode() = mock<TypedFileNode> {
        on { type }.thenReturn(
            VideoFileTypeInfo(
                mimeType = "video/mp4",
                extension = "mp4",
                duration = 120.seconds
            )
        )
    }
}