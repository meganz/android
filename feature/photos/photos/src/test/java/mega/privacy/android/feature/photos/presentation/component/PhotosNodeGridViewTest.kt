package mega.privacy.android.feature.photos.presentation.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.model.PhotoNodeUiState
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.PhotosNodeContentItem
import mega.privacy.android.feature.photos.model.TimelineGridSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import java.time.LocalDateTime

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class PhotosNodeGridViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that header item is successfully displayed`() {
        val time = LocalDateTime.now()
        val items = persistentListOf(PhotosNodeContentItem.HeaderItem(time = time))
        composeRuleScope {
            setView(
                items = items,
                selectedPhotoIds = setOf()
            )

            onNodeWithTag(PHOTOS_NODE_GRID_VIEW_HEADER_BODY_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that image photos node is successfully displayed`() {
        val photoId = 1L
        val photoUiState = mock<PhotoUiState.Image> {
            on { id } doReturn photoId
            on { fileTypeInfo } doReturn StaticImageFileTypeInfo(
                mimeType = "",
                extension = "jpg"
            )
        }
        val items = persistentListOf(
            PhotosNodeContentItem.PhotoNodeItem(
                node = PhotoNodeUiState(
                    photo = photoUiState,
                    isSensitive = false,
                    defaultIcon = mega.privacy.android.icon.pack.R.drawable.ic_3d_medium_solid,
                )
            )
        )
        composeRuleScope {
            setView(
                items = items,
                selectedPhotoIds = setOf(photoId),
            )

            onNodeWithTag(PHOTOS_NODE_BODY_IMAGE_NODE_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the grid size settings icon is displayed`() {
        val time = LocalDateTime.now()
        val items = persistentListOf(
            PhotosNodeContentItem.HeaderItem(
                time = time
            )
        )
        composeRuleScope {
            setView(items = items)

            onNodeWithTag(PHOTOS_NODE_HEADER_BODY_GRID_SIZE_SETTINGS_ICON_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the grid size settings menu is displayed`() {
        val time = LocalDateTime.now()
        val items = persistentListOf(PhotosNodeContentItem.HeaderItem(time = time))
        composeRuleScope {
            setView(
                items = items,
                selectedPhotoIds = setOf()
            )

            onNodeWithTag(PHOTOS_NODE_HEADER_BODY_GRID_SIZE_SETTINGS_ICON_TAG).performClick()

            onNodeWithTag(PHOTOS_NODE_HEADER_BODY_GRID_SIZE_SETTINGS_MENU_TAG).assertIsDisplayed()
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setView(
        items: ImmutableList<PhotosNodeContentItem> = persistentListOf(),
        selectedPhotoIds: Set<Long> = setOf(),
        gridSize: TimelineGridSize = TimelineGridSize.Large,
        onGridSizeChange: (value: TimelineGridSize) -> Unit = {},
        onClick: (node: PhotoNodeUiState) -> Unit = {},
        onLongClick: (node: PhotoNodeUiState) -> Unit = {},
        downloadPhotoResult: DownloadPhotoResult = DownloadPhotoResult.Idle,
    ) {
        setContent {
            PhotosNodeGridView(
                items = items,
                selectedPhotoIds = selectedPhotoIds,
                gridSize = gridSize,
                onGridSizeChange = onGridSizeChange,
                onClick = onClick,
                onLongClick = onLongClick
            )
        }
    }
}
