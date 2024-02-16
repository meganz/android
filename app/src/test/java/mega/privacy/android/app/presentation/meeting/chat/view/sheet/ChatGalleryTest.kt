package mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.activity.ComponentActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.domain.entity.chat.FileGalleryItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ChatGalleryTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that progress bar is shown if isLoadingGalleryFiles is true`() {
        initComposeRuleContent(isLoading = true)
        composeTestRule.onNodeWithTag(TEST_TAG_LOADING_GALLERY).assertIsDisplayed()
    }

    @Test
    fun `test that progress bar is not shown if isLoadingGalleryFiles is false`() {
        initComposeRuleContent(isLoading = false)
        composeTestRule.onNodeWithTag(TEST_TAG_LOADING_GALLERY).assertDoesNotExist()
    }

    @Test
    fun `test that ask for media permission is shown if isMediaPermissionGranted is false`() {
        initComposeRuleContent(isMediaPermissionGranted = false)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_toolbar_bottom_sheet_allow_gallery_access_title))
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_toolbar_bottom_sheet_grant_gallery_access_button))
    }

    @Test
    fun `test that ask for media permission is not shown if isMediaPermissionGranted is true`() {
        initComposeRuleContent(isMediaPermissionGranted = true)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_toolbar_bottom_sheet_allow_gallery_access_title))
            .assertDoesNotExist()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.chat_toolbar_bottom_sheet_grant_gallery_access_button))
            .assertDoesNotExist()
    }

    @Test
    fun `test that gallery item image click is passed to upper caller`() {
        val id = 2L
        val onFileGalleryItemClicked = mock<(FileGalleryItem) -> Unit>()
        val image = mock<FileGalleryItem> {
            on { this.id } doReturn id
        }
        initComposeRuleContent(
            images = listOf(image),
            onFileGalleryItemClicked = onFileGalleryItemClicked
        )
        composeTestRule.onNodeWithTag("$TEST_TAG_ATTACH_GALLERY_ITEM:$id").performClick()
        verify(onFileGalleryItemClicked).invoke(image)
    }

    @OptIn(ExperimentalMaterialApi::class)
    private fun initComposeRuleContent(
        isLoading: Boolean = false,
        isMediaPermissionGranted: Boolean = true,
        images: List<FileGalleryItem> = emptyList(),
        onFileGalleryItemClicked: (FileGalleryItem) -> Unit = mock(),
    ) {
        composeTestRule.setContent {
            ChatGalleryContent(
                sheetState = ModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Expanded,
                    isSkipHalfExpanded = false,
                    density = LocalDensity.current,
                ),
                isMediaPermissionGranted = isMediaPermissionGranted,
                images = images,
                isLoading = isLoading,
                onFileGalleryItemClicked = onFileGalleryItemClicked
            )
        }
    }
}