package mega.privacy.android.core.nodecomponents.menu.menuitem.selectionmode

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.nodecomponents.menu.menuaction.AddToAlbumMenuAction
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.seconds

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddToAlbumSelectionMenuItemTest {

    @ParameterizedTest(name = "when {0}")
    @MethodSource("provideTestSelectedNodes")
    fun `test shouldDisplay returns correctly`(
        @Suppress("UNUSED_PARAMETER")
        testDescription: String,
        isDisplayed: Boolean,
        selectedNodes: List<TypedFileNode>,
    ) = runTest {
        val addToAlbumMenuItem = AddToAlbumSelectionMenuItem(mock<AddToAlbumMenuAction>())

        val result = addToAlbumMenuItem.shouldDisplay(
            hasNodeAccessPermission = false,
            selectedNodes = selectedNodes,
            canBeMovedToTarget = false,
            noNodeInBackups = false,
            noNodeTakenDown = false
        )

        assertThat(result).isEqualTo(isDisplayed)
    }

    private fun provideTestSelectedNodes() = listOf(
        Arguments.of(
            "all nodes are image or video files",
            true,
            listOf(createImageNode(), createVideoNode())
        ),
        Arguments.of("all nodes are image files", true, listOf(createImageNode())),
        Arguments.of("all nodes are video files", false, listOf(createVideoNode())),
        Arguments.of(
            "includes other typed node",
            false,
            listOf(createVideoNode(), createImageNode(), createOtherNode())
        ),
    )

    private fun createImageNode() = mock<TypedFileNode> {
        on { type }.thenReturn(
            StaticImageFileTypeInfo(
                mimeType = "image/jpeg",
                extension = "jpg"
            )
        )
    }

    private fun createVideoNode() = mock<TypedFileNode> {
        on { type }.thenReturn(
            VideoFileTypeInfo(
                mimeType = "video/mp4",
                extension = "mp4",
                duration = 120.seconds
            )
        )
    }

    private fun createOtherNode() = mock<TypedFileNode>()
}