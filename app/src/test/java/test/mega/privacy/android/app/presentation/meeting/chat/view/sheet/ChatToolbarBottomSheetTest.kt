package test.mega.privacy.android.app.presentation.meeting.chat.view.sheet

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.ChatToolbarBottomSheet
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_CONTACT
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_FILE
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_GALLERY
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_GIF
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_LOCATION
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_ATTACH_FROM_SCAN
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_GALLERY_LIST
import mega.privacy.android.app.presentation.meeting.chat.view.sheet.TEST_TAG_LOADING_GALLERY
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterialApi::class)
@RunWith(AndroidJUnit4::class)
class ChatToolbarBottomSheetTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private val onAttachFileClicked: () -> Unit = mock()
    private val onAttachContactClicked: () -> Unit = mock()
    private val onPickLocation: () -> Unit = mock()

    @Test
    fun `test that gallery list shows`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_GALLERY_LIST).assertExists()
    }

    @Test
    fun `test that gallery list is shown`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_GALLERY).assertIsDisplayed()
    }

    @Test
    fun `test that file button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_FILE).assertIsDisplayed()
    }

    @Test
    fun `test that gif button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_GIF).assertIsDisplayed()
    }

    @Test
    fun `test that scan button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_SCAN).assertIsDisplayed()
    }

    @Test
    fun `test that location button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_LOCATION).assertIsDisplayed()
    }

    @Test
    fun `test that contact button is shown in chat bottom sheet`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_CONTACT).assertIsDisplayed()
    }

    @Test
    fun `test that contact button click is passed to upper caller`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_CONTACT).performClick()
        verify(onAttachContactClicked).invoke()
    }

    @Test
    fun `test that file button click is passed to upper caller`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_FILE).performClick()
        verify(onAttachFileClicked).invoke()
    }

    @Test
    fun `test that location button click is passed to upper caller`() {
        initComposeRuleContent()
        composeTestRule.onNodeWithTag(TEST_TAG_ATTACH_FROM_LOCATION).performClick()
        verify(onPickLocation).invoke()
    }

    @Test
    fun `test that progress bar is shown if isLoadingGalleryFiles is true`() {
        initComposeRuleContent(isLoadingGalleryFiles = true)
        composeTestRule.onNodeWithTag(TEST_TAG_LOADING_GALLERY).assertIsDisplayed()
    }

    @Test
    fun `test that progress bar is not shown if isLoadingGalleryFiles is false`() {
        initComposeRuleContent(isLoadingGalleryFiles = false)
        composeTestRule.onNodeWithTag(TEST_TAG_LOADING_GALLERY).assertDoesNotExist()
    }

    private fun initComposeRuleContent(isLoadingGalleryFiles: Boolean = false) {
        composeTestRule.setContent {
            ChatToolbarBottomSheet(
                onAttachFileClicked = onAttachFileClicked,
                onAttachContactClicked = onAttachContactClicked,
                onPickLocation = onPickLocation,
                isLoadingGalleryFiles = isLoadingGalleryFiles,
                sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
                onTakePicture = {},
            )
        }
    }
}