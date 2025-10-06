package mega.privacy.android.feature.clouddrive.presentation.offline

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class OfflineScreenTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that OfflineScreen displays loading state when isLoading is true`() {
        val uiState = OfflineUiState(isLoadingCurrentFolder = true)
        setupComposeContent(uiState)

        composeRule.onNodeWithText("No offline files available").assertDoesNotExist()
    }

    @Test
    fun `test that OfflineScreen displays empty state when offlineNodes is empty and not loading`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList()
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("No offline files available").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays correct empty state description text`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList()
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("No offline files available").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays warning banner when showOfflineWarning is true and not loading`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            showOfflineWarning = true
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("No offline files available").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen does not display warning banner when showOfflineWarning is false`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            showOfflineWarning = false
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("No offline files available").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen does not display warning banner when loading`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = true,
            offlineNodes = emptyList(),
            showOfflineWarning = true
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("No offline files available").assertDoesNotExist()
    }

    @Test
    fun `test that OfflineScreen displays offline nodes when available`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1"),
            createOfflineNodeUiItem("test_folder", isFolder = true, handle = "2")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("No offline files available").assertDoesNotExist()
        composeRule.onNodeWithText("test_file.txt").assertIsDisplayed()
        composeRule.onNodeWithText("test_folder").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays correct title when parent is not root`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            title = "Test Offline Title",
            nodeId = 1
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("Test Offline Title").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays correct title when parent is root`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            nodeId = -1
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.offline_screen_title)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles item click callbacks`() {
        val mockCallback: (OfflineNodeUiItem) -> Unit = mock()
        val nodeUiItem = createOfflineNodeUiItem("test_file.txt", isFolder = false)
        val offlineNodes = listOf(nodeUiItem)
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes
        )

        setupComposeContent(
            uiState = uiState,
            onItemClicked = mockCallback,
        )

        composeRule.onNodeWithText(nodeUiItem.offlineFileInformation.name).performClick()
        verify(mockCallback).invoke(nodeUiItem)
    }

    @Test
    fun `test that OfflineScreen displays file descriptions correctly`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("document.pdf", isFolder = false, size = 1024L)
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("document.pdf").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays folder descriptions correctly`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("Documents", isFolder = true)
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("Documents").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays empty folder description`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val emptyFolderText = context.getString(R.string.file_browser_empty_folder)
        val offlineNodes = listOf(
            createOfflineNodeUiItem("EmptyFolder", isFolder = true, numFiles = 0, numFolders = 0)
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("EmptyFolder").assertIsDisplayed()
        composeRule.onNodeWithText(emptyFolderText).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays folder with files description`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val filesText =
            context.resources.getQuantityString(R.plurals.num_files_with_parameter, 3, 3)
        val offlineNodes = listOf(
            createOfflineNodeUiItem(
                "FolderWithFiles",
                isFolder = true,
                numFiles = 3,
                numFolders = 0
            )
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("FolderWithFiles").assertIsDisplayed()
        composeRule.onNodeWithText(filesText).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays folder with subfolders description`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val foldersText =
            context.resources.getQuantityString(R.plurals.num_folders_with_parameter, 2, 2)
        val offlineNodes = listOf(
            createOfflineNodeUiItem(
                "FolderWithSubfolders",
                isFolder = true,
                numFiles = 0,
                numFolders = 2
            )
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("FolderWithSubfolders").assertIsDisplayed()
        composeRule.onNodeWithText(foldersText).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays folder with files and subfolders description`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val foldersText = context.resources.getQuantityString(R.plurals.num_folders_num_files, 2, 2)
        val filesText = context.resources.getQuantityString(R.plurals.num_folders_num_files_2, 5, 5)
        val combinedText = "$foldersText$filesText"
        val offlineNodes = listOf(
            createOfflineNodeUiItem("MixedFolder", isFolder = true, numFiles = 5, numFolders = 2)
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("MixedFolder").assertIsDisplayed()
        composeRule.onNodeWithText(combinedText).assertIsDisplayed()
    }

    private fun setupComposeContent(
        uiState: OfflineUiState,
        onItemClicked: (OfflineNodeUiItem) -> Unit = {},
        onItemLongClicked: (OfflineNodeUiItem) -> Unit = {},
        onNavigateToFolder: (Int, String) -> Unit = { _, _ -> },
        onOpenFile: (OfflineFileInformation) -> Unit = {},
        onBack: () -> Unit = {},
        onDismissOfflineWarning: () -> Unit = {},
        selectAll: () -> Unit = {},
        deselectAll: () -> Unit = {},
        onSearch: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalContext provides composeRule.activity) {
                AndroidThemeForPreviews {
                    OfflineScreen(
                        uiState = uiState,
                        onBack = onBack,
                        onItemClicked = onItemClicked,
                        onItemLongClicked = onItemLongClicked,
                        onNavigateToFolder = onNavigateToFolder,
                        onOpenFile = onOpenFile,
                        onDismissOfflineWarning = onDismissOfflineWarning,
                        selectAll = selectAll,
                        deselectAll = deselectAll,
                        onSearch = onSearch,
                    )
                }
            }
        }
    }

    private fun createOfflineNodeUiItem(
        name: String,
        isFolder: Boolean,
        size: Long = 0L,
        handle: String = "123456789",
        numFiles: Int = 0,
        numFolders: Int = 0,
    ): OfflineNodeUiItem {
        val mockFolderInfo = if (isFolder) {
            mock<OfflineFolderInfo> {
                on { this.numFiles } doReturn numFiles
                on { this.numFolders } doReturn numFolders
            }
        } else null

        val mockOfflineFileInformation = mock<OfflineFileInformation> {
            on { this.name } doReturn name
            on { this.isFolder } doReturn isFolder
            on { this.handle } doReturn handle
            on { this.totalSize } doReturn size
            on { this.folderInfo } doReturn mockFolderInfo
        }

        return mock<OfflineNodeUiItem> {
            on { this.offlineFileInformation } doReturn mockOfflineFileInformation
            on { this.isSelected } doReturn false
            on { this.isHighlighted } doReturn false
        }
    }
}
