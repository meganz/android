package mega.privacy.android.feature.clouddrive.presentation.offline

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.triggered
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.analytics.tracker.AnalyticsTracker
import mega.privacy.android.core.nodecomponents.list.GRID_VIEW_TOGGLE_TAG
import mega.privacy.android.core.nodecomponents.list.SORT_ORDER_TAG
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.clouddrive.R
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineNodeUiItem
import mega.privacy.android.feature.clouddrive.presentation.offline.model.OfflineUiState
import mega.privacy.mobile.analytics.event.OfflineScreenEvent
import mega.privacy.mobile.analytics.event.ViewModeButtonPressedEvent
import org.junit.Rule
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class OfflineScreenTest {
    private val analyticsTracker: AnalyticsTracker = mock()

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val analyticsRule = AnalyticsTestRule(analyticsTracker)

    @AfterEach
    fun resetMocks() {
        reset(analyticsTracker)
    }

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
    fun `test that OfflineScreen displays warning banner when showOfflineWarning is true and not loading`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            showOfflineWarning = true
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText(
            InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(R.string.offline_warning)
        ).assertIsDisplayed()
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

    @Test
    fun `test that OfflineScreen displays grid view when currentViewType is GRID`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1"),
            createOfflineNodeUiItem("test_folder", isFolder = true, handle = "2")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.GRID
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("test_file.txt").assertIsDisplayed()
        composeRule.onNodeWithText("test_folder").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays selection mode when nodes are selected`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            selectedNodeHandles = listOf(1L)
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles long click callbacks`() {
        val mockCallback: (OfflineNodeUiItem) -> Unit = mock()
        val nodeUiItem = createOfflineNodeUiItem("test_file.txt", isFolder = false)
        val offlineNodes = listOf(nodeUiItem)
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes
        )

        setupComposeContent(
            uiState = uiState,
            onItemLongClicked = mockCallback,
        )

        composeRule.onNodeWithText(nodeUiItem.offlineFileInformation.name).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles search functionality`() {
        val mockCallback: (String) -> Unit = mock()
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            searchQuery = "test query"
        )

        setupComposeContent(
            uiState = uiState,
            onSearch = mockCallback,
        )

        composeRule.onNodeWithText("test query").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles back navigation callback`() {
        val mockCallback: () -> Unit = mock()
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList()
        )

        setupComposeContent(
            uiState = uiState,
            onBack = mockCallback,
        )

        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.offline_screen_title)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles dismiss offline warning callback`() {
        val mockCallback: () -> Unit = mock()
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            showOfflineWarning = true
        )

        setupComposeContent(
            uiState = uiState,
            onDismissOfflineWarning = mockCallback,
        )

        composeRule.onNodeWithText(
            InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.offline_warning)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles select all callback`() {
        val mockCallback: () -> Unit = mock()
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            selectedNodeHandles = listOf(1L)
        )

        setupComposeContent(
            uiState = uiState,
            selectAll = mockCallback,
        )

        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles deselect all callback`() {
        val mockCallback: () -> Unit = mock()
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            selectedNodeHandles = listOf(1L)
        )

        setupComposeContent(
            uiState = uiState,
            deselectAll = mockCallback,
        )

        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles share offline files callback`() {
        val mockCallback: (List<OfflineFileInformation>) -> Unit = mock()
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            selectedNodeHandles = listOf(1L)
        )

        setupComposeContent(
            uiState = uiState,
            shareOfflineFiles = mockCallback,
        )

        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles save offline files to device callback`() {
        val mockCallback: (List<Long>) -> Unit = mock()
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            selectedNodeHandles = listOf(1L) // In selection mode
        )

        setupComposeContent(
            uiState = uiState,
            saveOfflineFilesToDevice = mockCallback,
        )

        composeRule.onNodeWithText("1").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen displays highlighted files correctly`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("highlighted_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            highlightedFiles = setOf("highlighted_file.txt")
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithText("highlighted_file.txt").assertIsDisplayed()
    }

    @Test
    fun `test that OfflineScreen handles file opening events correctly`() {
        val mockOpenFileCallback: (OfflineFileInformation) -> Unit = mock()
        val nodeUiItem = createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "789")
        val offlineNodes = listOf(nodeUiItem)
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            openOfflineNodeEvent = triggered(nodeUiItem.offlineFileInformation)
        )

        setupComposeContent(
            uiState = uiState,
            onOpenFile = mockOpenFileCallback
        )

        composeRule.onNodeWithText("test_file.txt").performClick()
        verify(mockOpenFileCallback).invoke(nodeUiItem.offlineFileInformation)
    }

    @Test
    fun `test that OfflineScreen handles folder navigation events correctly`() {
        val mockNavigateCallback: (Int, String) -> Unit = mock()
        val nodeUiItem = createOfflineNodeUiItem(
            id = 999,
            name = "test_folder",
            isFolder = true,
            handle = "999"
        )
        val offlineNodes = listOf(nodeUiItem)
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            openFolderInPageEvent = triggered(nodeUiItem.offlineFileInformation)
        )

        setupComposeContent(
            uiState = uiState,
            onNavigateToFolder = mockNavigateCallback,
        )

        composeRule.onNodeWithText("test_folder").performClick()
        verify(mockNavigateCallback).invoke(999, "test_folder")
    }

    @Test
    fun `test that loading tag is visible when isLoading is true`() {
        val uiState = OfflineUiState(isLoadingCurrentFolder = true)
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_LOADING_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_EMPTY_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_LIST_COLUMN_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_GRID_COLUMN_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that empty tag is visible when offlineNodes is empty and not loading`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList()
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_EMPTY_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_LOADING_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_LIST_COLUMN_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_GRID_COLUMN_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that warning banner tag is visible when showOfflineWarning is true`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            showOfflineWarning = true
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_TOP_WARNING_BANNER_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that warning banner tag is not visible when showOfflineWarning is false`() {
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = emptyList(),
            showOfflineWarning = false
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_TOP_WARNING_BANNER_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that list column tag is visible when currentViewType is LIST`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.LIST
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_LIST_COLUMN_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_GRID_COLUMN_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that grid column tag is visible when currentViewType is GRID`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            currentViewType = ViewType.GRID
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_GRID_COLUMN_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_LIST_COLUMN_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that selection mode bottom bar tag is visible when nodes are selected`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            selectedNodeHandles = listOf(1L)
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_SELECTION_MODE_BOTTOM_BAR_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that selection mode bottom bar tag is not visible when no nodes are selected`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            selectedNodeHandles = emptyList()
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_SELECTION_MODE_BOTTOM_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that search top app bar tag is visible when no nodes are selected`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            selectedNodeHandles = emptyList()
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_SEARCH_TOP_APP_BAR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_DEFAULT_TOP_APP_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that default top app bar tag is visible when nodes are selected`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            selectedNodeHandles = listOf(1L)
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_DEFAULT_TOP_APP_BAR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_SEARCH_TOP_APP_BAR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that sort bottom sheet tag is not visible by default`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes
        )
        setupComposeContent(uiState)

        // The sort bottom sheet should not be visible by default
        composeRule.onNodeWithTag(OFFLINE_SCREEN_SORT_BOTTOM_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that remove from offline dialog tag is not visible by default`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            selectedNodeHandles = listOf(1L)
        )
        setupComposeContent(uiState)

        // The remove dialog should not be visible by default
        composeRule.onNodeWithTag(OFFLINE_SCREEN_REMOVE_FROM_OFFLINE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that remove from offline dialog tag is not visible when not in remove state`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes,
            selectedNodeHandles = emptyList()
        )
        setupComposeContent(uiState)

        composeRule.onNodeWithTag(OFFLINE_SCREEN_REMOVE_FROM_OFFLINE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that on sort click should show sort bottom sheet`() {
        val mockSortCallback: (NodeSortConfiguration) -> Unit = mock()
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes
        )
        setupComposeContent(
            uiState = uiState,
            onSortNodes = mockSortCallback
        )

        // Initially, the sort bottom sheet should not be visible
        composeRule.onNodeWithTag(OFFLINE_SCREEN_SORT_BOTTOM_SHEET_TAG).assertDoesNotExist()

        // Click on the sort button
        composeRule.onNodeWithTag(SORT_ORDER_TAG).performClick()

        // Wait for and verify that the sort bottom sheet is now displayed
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(OFFLINE_SCREEN_SORT_BOTTOM_SHEET_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that on load should track screen event`() {
        setupComposeContent(OfflineUiState())

        verify(analyticsTracker).trackEvent(OfflineScreenEvent)
    }

    @Test
    fun `test that on view mode changed should track event`() {
        val offlineNodes = listOf(
            createOfflineNodeUiItem("test_file.txt", isFolder = false, handle = "1")
        )
        val uiState = OfflineUiState(
            isLoadingCurrentFolder = false,
            offlineNodes = offlineNodes
        )
        setupComposeContent(uiState = uiState)

        composeRule.onNodeWithTag(GRID_VIEW_TOGGLE_TAG)
            .assertIsDisplayed()
            .performClick()

        verify(analyticsTracker).trackEvent(ViewModeButtonPressedEvent)
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
        shareOfflineFiles: (List<OfflineFileInformation>) -> Unit = {},
        saveOfflineFilesToDevice: (List<Long>) -> Unit = {},
        removeOfflineNodes: (List<Long>) -> Unit = {},
        openFileInformation: (String) -> Unit = {},
        onSortNodes: (NodeSortConfiguration) -> Unit = {},
        onChangeViewType: () -> Unit = {},
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
                        shareOfflineFiles = shareOfflineFiles,
                        saveOfflineFilesToDevice = saveOfflineFilesToDevice,
                        removeOfflineNodes = removeOfflineNodes,
                        openFileInformation = openFileInformation,
                        onSortNodes = onSortNodes,
                        onChangeViewType = onChangeViewType,
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
        id: Int = 0,
    ): OfflineNodeUiItem {
        val mockFolderInfo = if (isFolder) {
            mock<OfflineFolderInfo> {
                on { this.numFiles } doReturn numFiles
                on { this.numFolders } doReturn numFolders
            }
        } else null

        val mockOfflineFileInformation = mock<OfflineFileInformation> {
            on { this.id } doReturn id
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
