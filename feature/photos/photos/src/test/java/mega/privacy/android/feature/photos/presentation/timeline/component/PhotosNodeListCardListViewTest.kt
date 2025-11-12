package mega.privacy.android.feature.photos.presentation.timeline.component

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import mega.privacy.android.domain.entity.photos.DownloadPhotoResult
import mega.privacy.android.feature.photos.extensions.LocalDownloadPhotoResultMock
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotoNodeListCardItem
import mega.privacy.android.feature.photos.presentation.timeline.model.PhotosNodeListCard
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class PhotosNodeListCardListViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `test that the correct dates are displayed`() {
        val photo = mock<PhotoUiState.Image>()
        val firstDate = "firstDate"
        val secondDate = "secondDate"
        val thirdDate = "thirdDate"
        val photos = persistentListOf(
            PhotosNodeListCard.Days(
                date = firstDate,
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = false
                ),
                photosCount = 10
            ),
            PhotosNodeListCard.Months(
                date = secondDate,
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = true
                ),
            ),
            PhotosNodeListCard.Years(
                date = thirdDate,
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = false
                ),
            )
        )
        composeRuleScope {
            setView(photos = photos)

            onNodeWithText(firstDate).assertIsDisplayed()
            onNodeWithText(secondDate).assertIsDisplayed()
            onNodeWithTag("fast_scroll_lazy_column:lazy_column_content")
                .performScrollToNode(hasText(thirdDate))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that the image item is displayed`() {
        val photo = mock<PhotoUiState.Image>()
        val photos = persistentListOf(
            PhotosNodeListCard.Days(
                date = "2 September",
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = false
                ),
                photosCount = 10
            )
        )
        composeRuleScope {
            setView(photos = photos)

            onNodeWithTag("fast_scroll_lazy_column:lazy_column_content", useUnmergedTree = true)
                .performScrollToNode(hasTestTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_IMAGE_TAG))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that the photo count is displayed when the photos count is greater than 1 and the period is days`() {
        val photo = mock<PhotoUiState.Image>()
        val photos = persistentListOf(
            PhotosNodeListCard.Days(
                date = "2 September",
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = false
                ),
                photosCount = 10
            )
        )
        composeRuleScope {
            setView(photos = photos)

            onNodeWithTag("fast_scroll_lazy_column:lazy_column_content", useUnmergedTree = true)
                .performScrollToNode(hasTestTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG))
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that the photo count does not exist when the photos count is less than 1 and the period is days`() {
        val photo = mock<PhotoUiState.Image>()
        val photos = persistentListOf(
            PhotosNodeListCard.Days(
                date = "2 September",
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = false
                ),
                photosCount = 0
            )
        )
        composeRuleScope {
            setView(photos = photos)

            onNodeWithTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the photo count does not exist when the photos count equals to 1 and the period is days`() {
        val photo = mock<PhotoUiState.Image>()
        val photos = persistentListOf(
            PhotosNodeListCard.Days(
                date = "2 September",
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = false
                ),
                photosCount = 1
            )
        )
        composeRuleScope {
            setView(photos = photos)

            onNodeWithTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the photo count does not exist when the period is not days`() {
        val photo = mock<PhotoUiState.Image>()
        val photos = persistentListOf(
            PhotosNodeListCard.Years(
                date = "2025",
                photoItem = PhotoNodeListCardItem(
                    photo = photo,
                    isMarkedSensitive = false
                ),
            )
        )
        composeRuleScope {
            setView(photos = photos)

            onNodeWithTag(PHOTOS_NODE_LIST_CARD_LIST_VIEW_PHOTO_COUNT_TAG).assertDoesNotExist()
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setView(
        photos: ImmutableList<PhotosNodeListCard> = persistentListOf(),
        onClick: (photo: PhotosNodeListCard) -> Unit = {},
    ) {
        setContent {
            CompositionLocalProvider(LocalDownloadPhotoResultMock provides DownloadPhotoResult.Idle) {
                PhotosNodeListCardListView(
                    photos = photos,
                    onClick = onClick
                )
            }
        }
    }
}
