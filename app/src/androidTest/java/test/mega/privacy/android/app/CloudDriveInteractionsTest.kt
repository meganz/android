package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.data.test.stub.StubMegaTransfer
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaTransfer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm04 CloudDriveInteractions`, built on the
 * `:data-test` fake-SDK framework and following [CloudDriveUploadTest] / [FileFolderMenuTest]:
 * the whole app runs as in production with only the SDK gateways faked, the UI is driven with
 * UiAutomator (not a Compose test rule), and Compose test tags are matched as resource ids
 * because the activity sets `testTagsAsResourceId = true`.
 *
 * Cloud Drive is reached through the Drive bottom-navigation tab, whose Cloud Drive tab hosts the
 * same `CloudDriveContent` as the folder screen, so the navigation helpers below (open the tab,
 * open a folder row, open the search screen, open the FAB options sheet) are the entry points
 * shared by every scenario.
 *
 * Ported: tc01 + tc06 (list/grid switch and scrolling, folded into one test), tc02 (search for a
 * file), tc03 (search for a folder), tc05 (Open link shows the file-link preview), tc07 (tapping a
 * text file opens its full preview) and tc10 (tapping a folder shows its sub-contents).
 *
 * Not ported:
 * - tc04 (matching a name portion): the matching is SDK-side — the app only forwards the typed
 *   query.
 * - tc08 and tc09 (image preview, audio playback): both need fake capabilities that do not exist
 *   yet (real preview bytes, a playable local link).
 */
@HiltAndroidTest
class CloudDriveInteractionsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private var scenario: ActivityScenario<MegaActivity>? = null

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Persist a logged-in session through the app's real credentials path; account details are
        // fetched eagerly because account-detail dependent features wait for a non-empty value.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    // region tests

    /**
     * tc01 + tc06 — the seeded files show as list rows, switch to thumbnails and back, and the
     * items below the viewport are revealed by scrolling.
     */
    @Test
    fun cloudDriveSwitchesBetweenListAndGridAndScrollsToTheLastItem() {
        repeat(SCROLLABLE_FILE_COUNT) { index ->
            seedFile(handle = FILE_HANDLE + index, name = scrollableFileName(index))
        }
        val firstFileName = scrollableFileName(0)
        val lastFileName = scrollableFileName(SCROLLABLE_FILE_COUNT - 1)

        launchCloudDrive()
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(firstFileName), LOAD_TIMEOUT, "list-row")

        click(By.res(GRID_VIEW_TOGGLE_TAG), "grid-view-toggle")
        awaitObject(By.res(NODE_GRID_TITLE_TAG).text(firstFileName), LOAD_TIMEOUT, "grid-item")

        click(By.res(LIST_VIEW_TOGGLE_TAG), "list-view-toggle")
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(firstFileName), LOAD_TIMEOUT, "list-row-again")

        // tc06: the last seeded file sits below the viewport and only scrolling brings it in.
        val lastRow = By.res(NODE_LIST_TITLE_TAG).text(lastFileName)
        assertThat(device.hasObject(lastRow)).isFalse()
        scrollNodeListUntil(lastRow, "last-file-row")
    }

    /** tc02 — a file seeded in the Cloud Drive is found through the Cloud Drive search. */
    @Test
    fun cloudDriveSearchFindsSeededFile() {
        val fileName = "sm4tc02.txt"
        seedFile(handle = FILE_HANDLE, name = fileName)

        launchCloudDrive()
        searchCloudDriveFor(fileName)

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "search-result-row")
    }

    /** tc03 — a folder seeded in the Cloud Drive is found through the Cloud Drive search. */
    @Test
    fun cloudDriveSearchFindsSeededFolder() {
        val folderName = "f-sm4tc03"
        seedFolder(handle = FOLDER_HANDLE, name = folderName)

        launchCloudDrive()
        searchCloudDriveFor(folderName)

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(folderName), LOAD_TIMEOUT, "search-result-row")
    }

    /** tc05 — the FAB's "Open link" option opens the link dialog, and a file link shows its preview. */
    @Test
    fun openLinkOptionOpensTheFileLinkPreview() {
        fakeFlags.set(ApiFeatures.FileLinkRevamp, true)
        // The FAB is only offered once the Cloud Drive has content.
        seedFile(handle = FILE_HANDLE, name = "sm4tc05-seed.txt")
        fakeMegaApi.stubRequest(
            MegaApiGateway::getPublicNode,
            request = StubMegaRequest(
                type = MegaRequest.TYPE_GET_PUBLIC_NODE,
                publicMegaNode = StubMegaNode(
                    handle = PUBLIC_FILE_HANDLE,
                    name = PUBLIC_FILE_NAME,
                    size = FILE_SIZE,
                ),
            ),
        )

        launchCloudDrive()
        openFabOptionsSheet()
        clickSheetOption(OPEN_LINK_ACTION_TAG, "open-link-action")

        // The dialog renders in its own window, where the activity's `testTagsAsResourceId` does
        // not apply and no node carries a resource id, so it is matched by text and by class.
        awaitObject(
            By.text(str(sharedR.string.open_link_dialog_description)),
            LOAD_TIMEOUT,
            "open-link-dialog",
        )
        val input = By.clazz("android.widget.EditText")
        awaitObject(input, LOAD_TIMEOUT, "open-link-input")
        device.findObject(input).text = FILE_LINK_URL
        click(
            By.clickable(true).hasDescendant(By.text(str(sharedR.string.general_open_button))),
            "open-link-confirm",
        )

        awaitObject(
            By.res(FILE_LINK_FILE_NAME_TAG).text(PUBLIC_FILE_NAME),
            LOAD_TIMEOUT,
            "file-link-preview",
        )
    }

    /** tc07 — tapping a text file in the Cloud Drive opens its full preview with the content. */
    @Test
    fun tappingTextFileOpensItsFullPreview() {
        val fileName = "ReadMe-AutoTest.txt"
        seedFile(handle = FILE_HANDLE, name = fileName)

        // The text viewer resolves content by downloading the node (no local file, no streaming
        // link under the fake); script the download to report a pre-created file as its result.
        val downloadedFile = File(targetContext.cacheDir, fileName).apply {
            writeText(FILE_CONTENT)
        }
        fakeMegaApi.stubTransfer(
            MegaApiGateway::startDownload,
            transfer = StubMegaTransfer(
                type = MegaTransfer.TYPE_DOWNLOAD,
                tag = 1,
                uniqueId = 1L,
                fileName = fileName,
                path = downloadedFile.absolutePath,
                nodeHandle = FILE_HANDLE,
                transferredBytes = FILE_SIZE,
                totalBytes = FILE_SIZE,
                state = MegaTransfer.STATE_COMPLETED,
                isFinished = true,
            ),
        )

        launchCloudDrive()
        click(By.res(NODE_LIST_TITLE_TAG).text(fileName), "text-file-row")

        awaitObject(By.text(FILE_CONTENT), LOAD_TIMEOUT, "text-viewer-content")
        awaitObject(By.text(fileName), LOAD_TIMEOUT, "text-viewer-file-name")
    }

    /**
     * tc10 — a folder holding one folder and one file shows the matching child-count subtitle, and
     * tapping it lists both children.
     */
    @Test
    fun tappingFolderShowsItsChildCountSubtitleAndSubContents() {
        val parentName = "sm4tc10-parent"
        val childFolderName = "sm4tc10-child"
        val childFileName = "sm4tc10-parent_file.txt"
        seedFolder(handle = FOLDER_HANDLE, name = parentName)
        seedFolder(handle = CHILD_FOLDER_HANDLE, name = childFolderName, parentHandle = FOLDER_HANDLE)
        seedFile(handle = CHILD_FILE_HANDLE, name = childFileName, parentHandle = FOLDER_HANDLE)

        launchCloudDrive()

        val subtitle = plural(sharedR.plurals.num_of_folders_num_of_files, 1) +
                plural(sharedR.plurals.num_of_files_with_parameter, 1)
        awaitObject(By.res(NODE_SUBTITLE_TAG).text(subtitle), LOAD_TIMEOUT, "folder-child-count")

        openFolder(parentName)

        awaitObject(
            By.res(NODE_LIST_TITLE_TAG).text(childFolderName),
            LOAD_TIMEOUT,
            "child-folder-row",
        )
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(childFileName), LOAD_TIMEOUT, "child-file-row")
    }

    // endregion

    // region seeding helpers

    private fun seedFile(handle: Long, name: String, parentHandle: Long = rootHandle) =
        seedNode(handle = handle, name = name, isFolder = false, parentHandle = parentHandle)

    private fun seedFolder(handle: Long, name: String, parentHandle: Long = rootHandle) =
        seedNode(handle = handle, name = name, isFolder = true, parentHandle = parentHandle)

    private fun seedNode(
        handle: Long,
        name: String,
        isFolder: Boolean,
        parentHandle: Long,
    ): StubMegaNode {
        val node = StubMegaNode(
            handle = handle,
            name = name,
            parentHandle = parentHandle,
            isFolder = isFolder,
            size = if (isFolder) 0L else FILE_SIZE,
            modificationTime = System.currentTimeMillis() / 1000,
            owner = fakeMegaApi.account.myUserHandle,
        )
        fakeMegaApi.nodeTree.addNode(node, parentHandle = parentHandle)
        return node
    }

    private fun scrollableFileName(index: Int) = "sm4tc01-%02d.txt".format(index + 1)

    // endregion

    // region Cloud Drive navigation helpers

    /** Launches the logged-in app and opens the Drive tab, which hosts the Cloud Drive root. */
    private fun launchCloudDrive() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        awaitObject(By.res(DRIVE_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "drive-nav-item")
        device.findObject(By.res(DRIVE_NAV_ITEM_TAG)).click()
        awaitObject(By.res(DRIVE_SYNCS_APP_BAR_TAG), LOAD_TIMEOUT, "drive-app-bar")
    }

    /**
     * Opens the folder row titled [folderName] and waits for the folder's own screen. The folder
     * screen's toolbar carries the folder name too, so it is identified by its app-bar tag rather
     * than by the title — a silently failed tap would otherwise still satisfy a text match.
     */
    private fun openFolder(folderName: String) {
        click(By.res(NODE_LIST_TITLE_TAG).text(folderName), "folder-row-$folderName")
        awaitObject(By.res(CLOUD_DRIVE_APP_BAR_TAG), LOAD_TIMEOUT, "folder-app-bar")
    }

    /** Opens the search screen from the Cloud Drive toolbar and types [query] into it. */
    private fun searchCloudDriveFor(query: String) {
        click(By.res(SEARCH_ACTION_TAG), "cloud-drive-search-action")
        // The search field is a Compose text field exposed as an EditText node.
        val input = By.clazz("android.widget.EditText")
        awaitObject(input, LOAD_TIMEOUT, "search-input")
        device.findObject(input).text = query
    }

    /** Opens the Cloud Drive FAB's options sheet. */
    private fun openFabOptionsSheet() {
        click(By.res(DRIVE_SYNCS_FAB_TAG), "cloud-drive-fab")
        awaitObject(By.res(FAB_OPTIONS_SHEET_TAG), LOAD_TIMEOUT, "fab-options-sheet")
    }

    /**
     * Clicks the FAB options sheet entry tagged [actionTag], scrolling the sheet when the entry
     * sits below the visible area on shorter screens.
     */
    private fun clickSheetOption(actionTag: String, name: String) {
        val selector = By.res(actionTag)
        scrollSheetUntil(selector)
        click(selector, name)
    }

    private fun scrollSheetUntil(selector: BySelector) {
        repeat(MAX_SCROLL_SWIPES) {
            if (device.hasObject(selector)) return
            val sheet = device.findObject(By.res(FAB_OPTIONS_SHEET_TAG)) ?: return
            sheet.setGestureMargin(sheet.visibleBounds.height() / 4)
            sheet.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
    }

    // endregion

    // region UiAutomator helpers

    private fun str(resId: Int): String = targetContext.getString(resId)

    private fun plural(resId: Int, quantity: Int): String =
        targetContext.resources.getQuantityString(resId, quantity, quantity)

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
    }

    /**
     * Scrolls the node list until [selector] is on screen. The list is scrolled through its own
     * container — the Drive tab nests it inside a horizontally scrollable pager, so the first
     * scrollable node on screen is not necessarily the node list.
     */
    private fun scrollNodeListUntil(selector: BySelector, name: String) {
        repeat(MAX_SCROLL_SWIPES) {
            if (device.hasObject(selector)) return
            val container = nodeListContainer() ?: return@repeat
            container.setGestureMargin(container.visibleBounds.height() / 4)
            container.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
    }

    /** The innermost scrollable ancestor of a visible node row, i.e. the node list itself. */
    private fun nodeListContainer(): UiObject2? {
        var node = device.findObject(By.res(NODE_LIST_TITLE_TAG)) ?: return null
        repeat(MAX_PARENT_HOPS) {
            val parent = node.parent ?: return null
            if (parent.isScrollable) return parent
            node = parent
        }
        return null
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
        )
    }

    private fun dumpHierarchy(name: String) {
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
            Log.d("UiDump", "[$name#$index] $chunk")
        }
    }

    // endregion

    private companion object {
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val MAX_SCROLL_SWIPES = 12
        const val MAX_PARENT_HOPS = 8
        const val SCROLL_PERCENT = 0.8f

        const val FILE_HANDLE = 100L
        const val FOLDER_HANDLE = 200L
        const val CHILD_FOLDER_HANDLE = 201L
        const val CHILD_FILE_HANDLE = 202L
        const val PUBLIC_FILE_HANDLE = 300L
        const val FILE_SIZE = 1_024L

        /** Enough rows for the last one to start below the viewport on any phone screen. */
        const val SCROLLABLE_FILE_COUNT = 40

        const val FILE_CONTENT = "hello from the sm04 cloud drive port"

        const val PUBLIC_FILE_NAME = "sm4tc05-link.txt"

        /** A link matching the SDK file-link regex, so the deep link resolves to the file link screen. */
        const val FILE_LINK_URL = "https://mega.nz/file/AAAAAAAA#sm4tc05decryptionkey"

        /** Main navigation item test tag for the Drive/Sync section. */
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"

        /** Mirrors DRIVE_SYNCS_MAIN_APP_BAR_TAG / DRIVE_SYNCS_FAB_TAG of the Drive tab. */
        const val DRIVE_SYNCS_APP_BAR_TAG = "drive_syncs_screen:main_app_bar"
        const val DRIVE_SYNCS_FAB_TAG = "drive_syncs_screen:add_content_fab"

        /** Mirrors CLOUD_DRIVE_MAIN_APP_BAR_TAG of the folder-level Cloud Drive screen. */
        const val CLOUD_DRIVE_APP_BAR_TAG = "cloud_drive_screen:main_app_bar"

        /** Mirrors CommonMenuAction.Search.testTag. */
        const val SEARCH_ACTION_TAG = "app_bar:search"

        /** Mirrors the internal TITLE_TAG / SUBTITLE_TAG of NodeListViewItem rows. */
        const val NODE_LIST_TITLE_TAG = "node_list_view_item:title"
        const val NODE_SUBTITLE_TAG = "node_list_view_item:subtitle"

        /** Mirrors NODE_TITLE_TEXT_TEST_TAG of NodeGridViewItem. */
        const val NODE_GRID_TITLE_TAG = "node_grid_view_item:node_title"

        /** Mirrors GRID_VIEW_TOGGLE_TAG / LIST_VIEW_TOGGLE_TAG of NodeHeaderItem. */
        const val GRID_VIEW_TOGGLE_TAG = "header_view_item:grid_view_toggle"
        const val LIST_VIEW_TOGGLE_TAG = "header_view_item:list_view_toggle"

        /** Mirrors the internal tags of UploadOptionsBottomSheet. */
        const val FAB_OPTIONS_SHEET_TAG = "home_fab_options_panel"
        const val OPEN_LINK_ACTION_TAG = "home_fab_options_panel:open_link_action"

        /** Mirrors FILE_LINK_FILE_NAME_TAG of the file link screen. */
        const val FILE_LINK_FILE_NAME_TAG = "file_link_screen:file_name"
    }
}
