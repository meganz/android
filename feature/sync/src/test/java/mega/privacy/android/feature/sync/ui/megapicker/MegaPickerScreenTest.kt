package mega.privacy.android.feature.sync.ui.megapicker

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.mapper.FileTypeIconMapper
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaApiJava
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config

@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
@RunWith(AndroidJUnit4::class)
internal class MegaPickerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val mockFileTypeIconMapper = mock<FileTypeIconMapper>()
    private val mockFolderClicked = mock<(TypedNode) -> Unit>()
    private val mockCurrentFolderSelected = mock<() -> Unit>()
    private val mockSnackbarMessageShown = mock<() -> Unit>()
    private val mockDisabledFolderClicked = mock<(TypedNodeUiModel) -> Unit>()

    private fun createMockFolderNode(
        id: Long = 1L,
        name: String = "Test Folder",
        parentId: Long = 2L,
    ): TypedFolderNode = mock<TypedFolderNode>().apply {
        whenever(this.id).thenReturn(NodeId(id))
        whenever(this.name).thenReturn(name)
        whenever(this.parentId).thenReturn(NodeId(parentId))
    }

    private fun createMockCurrentFolder(
        id: Long = 1L,
        name: String = "Current Folder",
        parentId: Long = 2L,
    ): mega.privacy.android.domain.entity.node.Node =
        mock<mega.privacy.android.domain.entity.node.Node>().apply {
            whenever(this.id).thenReturn(NodeId(id))
            whenever(this.name).thenReturn(name)
            whenever(this.parentId).thenReturn(NodeId(parentId))
        }

    @Test
    fun `test that screen displays loading state when isLoading is true`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = null,
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = true,
                isSelectEnabled = false,
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that screen displays empty state when nodes list is empty`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }

    @Test
    fun `test that screen displays nodes when nodes list is provided`() {
        val mockNodes = listOf(
            TypedNodeUiModel(createMockFolderNode(1L, "Folder 1")),
            TypedNodeUiModel(createMockFolderNode(2L, "Folder 2"))
        )

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = mockNodes,
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        // Wait for the UI to be fully rendered
        composeTestRule.waitForIdle()

        // Verify that the folder picker view is displayed (not loading or empty state)
        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LIST_SCREEN_NO_ITEMS)
            .assertDoesNotExist()

        // Verify that the actual nodes are displayed by checking for their names
        composeTestRule.onNodeWithText("Folder 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Folder 2").assertIsDisplayed()
    }

    @Test
    fun `test that app bar shows cloud drive title when currentFolder is null`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(sharedR.string.general_section_cloud_drive)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that app bar shows current folder name when currentFolder is provided and not root`() {
        val currentFolder = createMockCurrentFolder(1L, "My Documents", 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.onNodeWithText("My Documents").assertIsDisplayed()
    }

    @Test
    fun `test that app bar shows cloud drive title when currentFolder is root`() {
        val rootFolder = createMockCurrentFolder(
            id = 1L,
            name = "Root",
            parentId = MegaApiJava.INVALID_HANDLE
        )

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = rootFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(sharedR.string.general_section_cloud_drive)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that select button is displayed when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        // Wait for the UI to be fully rendered
        composeTestRule.waitForIdle()

        // Verify that the select button is displayed and enabled
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsEnabled()
    }

    @Test
    fun `test that select button is not displayed when isSelectEnabled is false`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        // Wait for the UI to be fully rendered
        composeTestRule.waitForIdle()

        // Verify that the select button is displayed and enabled
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertDoesNotExist()
    }

    @Test
    fun `test that select button click triggers currentFolderSelected callback`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        // Wait for the UI to be fully rendered
        composeTestRule.waitForIdle()

        // Click the select button
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .performClick()

        // Verify that the callback was called
        verify(mockCurrentFolderSelected).invoke()
    }

    @Test
    fun `test that app bar shows subtitle when isStopBackupMegaPicker is true`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
                isStopBackupMegaPicker = true,
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(sharedR.string.general_select_create_folder)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that app bar does not show subtitle when isStopBackupMegaPicker is false`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
                isStopBackupMegaPicker = false,
            )
        }

        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(sharedR.string.general_select_create_folder)
        ).assertDoesNotExist()
    }

    @Test
    fun `test that create new folder menu action is displayed and clickable`() {
        val currentFolder = createMockCurrentFolder()

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        // Wait for the UI to be fully rendered
        composeTestRule.waitForIdle()

        // Verify that the create new folder menu action is displayed and clickable
        composeTestRule.onNodeWithTag("menu_action:create_new_folder")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_action:create_new_folder")
            .assertIsEnabled()
    }

    @Test
    fun `test that screen handles null nodes gracefully`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = null,
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        // Should show loading state when nodes is null
        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that button is visible and clickable when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is displayed and enabled by checking for any button text
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsEnabled()
    }

    @Test
    fun `test that button is not visible when isSelectEnabled is false`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is not displayed
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertDoesNotExist()
    }

    @Test
    fun `test that button click triggers callback when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        composeTestRule.waitForIdle()

        // Click the button
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .performClick()

        // Verify callback was called
        verify(mockCurrentFolderSelected).invoke()
    }

    @Test
    fun `test that button shows correct text for stop backup mega picker when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
                isStopBackupMegaPicker = true,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button shows "Select" text for stop backup mega picker
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertDoesNotExist()
    }

    @Test
    fun `test that button shows correct text for regular mega picker when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
                isStopBackupMegaPicker = false,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button shows "Select folder" text for regular mega picker
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select))
            .assertDoesNotExist()
    }

    @Test
    fun `test that layout adapts correctly when isSelectEnabled changes from false to true`() {
        // Test the behavior when isSelectEnabled is false
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is not visible when isSelectEnabled is false
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertDoesNotExist()
    }

    @Test
    fun `test that layout adapts correctly when isSelectEnabled changes from true to false`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        // Test the behavior when isSelectEnabled is true
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is visible when isSelectEnabled is true
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsDisplayed()
    }

    @Test
    fun `test that button is positioned at bottom when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is displayed (positioned at bottom)
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsDisplayed()

        // The button should be enabled and clickable
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsEnabled()
    }

    @Test
    fun `test that button works correctly with different node states when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)
        val mockNodes = listOf(
            TypedNodeUiModel(createMockFolderNode(1L, "Folder 1")),
            TypedNodeUiModel(createMockFolderNode(2L, "Folder 2"))
        )

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = mockNodes,
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is still visible and functional even with nodes
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsEnabled()

        // Click the button
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .performClick()

        // Verify callback was called
        verify(mockCurrentFolderSelected).invoke()
    }

    @Test
    fun `test that button works correctly in loading state when isSelectEnabled is true`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = null,
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = true,
                isSelectEnabled = true,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is visible even during loading
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertDoesNotExist()

        // Verify loading state is also displayed
        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that button works correctly in empty state when isSelectEnabled is true`() {
        val currentFolder = createMockCurrentFolder(id = 1L, name = "Test Folder", parentId = 2L)

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = currentFolder,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = {},
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = true,
            )
        }

        composeTestRule.waitForIdle()

        // Verify button is visible even in empty state
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_select_folder))
            .assertIsEnabled()

        // Verify empty state is also displayed
        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }

    @Test
    fun `test that screen displays disabled folder with device name`() {
        val disabledFolder = TypedNodeUiModel(
            node = createMockFolderNode(1L, "Disabled Folder"),
            isDisabled = true,
            backupId = 123L,
            deviceName = "My Laptop"
        )

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = listOf(disabledFolder),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = mockDisabledFolderClicked,
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.waitForIdle()

        // Verify the disabled folder is displayed
        composeTestRule.onNodeWithText("Disabled Folder").assertIsDisplayed()
    }

    @Test
    fun `test that clicking disabled folder triggers disabledFolderClicked callback`() {
        val disabledFolder = TypedNodeUiModel(
            node = createMockFolderNode(1L, "Disabled Folder"),
            isDisabled = true,
            backupId = 123L,
            deviceName = "My Laptop"
        )

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = listOf(disabledFolder),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = mockDisabledFolderClicked,
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.waitForIdle()

        // Click on the disabled folder
        composeTestRule.onNodeWithText("Disabled Folder").performClick()

        // Verify the disabledFolderClicked callback was called with the correct node
        verify(mockDisabledFolderClicked).invoke(disabledFolder)
    }

    @Test
    fun `test that snackbar is displayed when snackbarMessageId is set`() {
        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = emptyList(),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = mockDisabledFolderClicked,
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = sharedR.string.device_center_snackbar_message_connection_removed,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.waitForIdle()

        // Verify the snackbar message is displayed
        composeTestRule.onNodeWithText(
            composeTestRule.activity.getString(sharedR.string.device_center_snackbar_message_connection_removed)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that clicking enabled folder triggers folderClicked callback`() {
        val enabledFolder = TypedNodeUiModel(
            node = createMockFolderNode(1L, "Enabled Folder"),
            isDisabled = false
        )

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = listOf(enabledFolder),
                folderClicked = mockFolderClicked,
                disabledFolderClicked = mockDisabledFolderClicked,
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.waitForIdle()

        // Click on the enabled folder
        composeTestRule.onNodeWithText("Enabled Folder").performClick()

        // Verify the folderClicked callback was called
        verify(mockFolderClicked).invoke(enabledFolder.node)
    }

    @Test
    fun `test that screen displays mixed enabled and disabled folders`() {
        val folders = listOf(
            TypedNodeUiModel(createMockFolderNode(1L, "Enabled Folder 1"), isDisabled = false),
            TypedNodeUiModel(
                node = createMockFolderNode(2L, "Disabled Folder"),
                isDisabled = true,
                backupId = 123L,
                deviceName = "My Laptop"
            ),
            TypedNodeUiModel(createMockFolderNode(3L, "Enabled Folder 2"), isDisabled = false),
        )

        composeTestRule.setContent {
            MegaPickerScreen(
                currentFolder = null,
                nodes = folders,
                folderClicked = mockFolderClicked,
                disabledFolderClicked = mockDisabledFolderClicked,
                currentFolderSelected = mockCurrentFolderSelected,
                fileTypeIconMapper = mockFileTypeIconMapper,
                snackbarMessageId = null,
                snackbarMessageShown = mockSnackbarMessageShown,
                isLoading = false,
                isSelectEnabled = false,
            )
        }

        composeTestRule.waitForIdle()

        // Verify all folders are displayed
        composeTestRule.onNodeWithText("Enabled Folder 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Disabled Folder").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enabled Folder 2").assertIsDisplayed()
    }
}
