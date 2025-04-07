package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.filecontact.model.SelectionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileContactListTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that file name is displayed if no items are selected`() {
        val expectedFolderName = "Folder Name"
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = expectedFolderName,
                        selectionState = SelectionState(0, false),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }

        composeTestRule.onNodeWithText(expectedFolderName).assertExists()
    }

    @Test
    fun `test that correct title is displayed for one item selected`() {
        val expectedFolderName = "Folder Name"
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = expectedFolderName,
                        selectionState = SelectionState(1, false),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }

        composeTestRule.onNodeWithText("1 contact").assertExists()
    }

    @Test
    fun `test that correct title is displayed for multiple items selected`() {
        val expectedFolderName = "Folder Name"
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = expectedFolderName,
                        selectionState = SelectionState(5, false),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }

        composeTestRule.onNodeWithText("5 contacts").assertExists()
    }

    @Test
    fun `test that correct menu items are displayed when no contacts are selected`() {
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = "Folder name",
                        selectionState = SelectionState(0, false),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }
//Expected menu items
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SHARE_FOLDER_ITEM)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW)
            .assertIsDisplayed()
//Not expected menu items
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_REMOVE_SHARE)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_CHANGE_PERMISSION_ITEM)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_UNSELECT_ALL_ITEM)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that correct menu items are displayed when contacts are selected`() {
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = "Folder name",
                        selectionState = SelectionState(1, false),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }
//Expected menu items
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_REMOVE_SHARE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_CHANGE_PERMISSION_ITEM)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW)
            .assertIsDisplayed()

//Not expected menu items
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SHARE_FOLDER_ITEM)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_UNSELECT_ALL_ITEM)
            .assertIsNotDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM)
            .assertIsNotDisplayed()
    }


    @Test
    fun `test that correct overflow items are displayed when no contacts are selected`() {
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = "Folder name",
                        selectionState = SelectionState(0, false),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW)
            .performClick()

        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_UNSELECT_ALL_ITEM)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that correct overflow items are displayed when some but not all contacts are selected`() {
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = "Folder name",
                        selectionState = SelectionState(2, false),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW)
            .performClick()

        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_UNSELECT_ALL_ITEM)
            .assertIsDisplayed()
    }

    @Test
    fun `test that correct overflow items are displayed when all contacts re selected`() {
        composeTestRule.setContent {
            Scaffold(
                topBar = {
                    FileContactListTopBar(
                        folderName = "Folder name",
                        selectionState = SelectionState(3, true),
                        onBackPressed = {},
                        selectAll = {},
                        deselectAll = {},
                        changePermissions = {},
                        shareFolder = {},
                        removeShare = {}
                    )
                }
            ) {
                Text(modifier = Modifier.padding(it), text = "Content")
            }
        }
        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_OVERFLOW)
            .performClick()

        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_UNSELECT_ALL_ITEM)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag(TEST_TAG_FILE_CONTACT_LIST_TOP_BAR_SELECT_ALL_ITEM)
            .assertIsNotDisplayed()
    }
}

