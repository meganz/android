package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaNodeList
import mega.privacy.android.data.test.stub.StubMegaRecentActionBucket
import mega.privacy.android.data.test.stub.StubMegaRecentActionBucketList
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.data.test.stub.StubMegaTransfer
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
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
 * Full-app instrumented port of the AAT feature `sm10 HomeScreen` — the gateway-clean scenarios
 * that exercise the Home surfaces. Built on the `:data-test` fake-SDK framework and following
 * [CloudDriveUploadTest] / [TransferManagerTest] / [SettingsOptions1Test]: the whole app runs as
 * in production with only the SDK gateways faked, the UI is driven with UiAutomator (not a
 * Compose test rule), and Compose test tags are matched as resource ids because the activity
 * sets `testTagsAsResourceId = true`.
 *
 * The AAT scenarios were written against the legacy homepage (category buttons, Recents tab,
 * toolbar avatar). On develop the app renders the redesigned single-activity Home
 * (`mega.privacy.mobile.home`), so each scenario is ported against its redesigned equivalent:
 *
 * - Category screens are reached through the Home shortcut chips (Favourites, Audio). MegaChip
 *   merges its label into the node's contentDescription (no text is exposed), so chips are
 *   matched with [By.desc], not [By.text]. Both screens render the shared
 *   [mega.privacy.android.shared.nodes.components.NodesView] whose header carries the list/grid
 *   toggle.
 * - Recents is a widget on the Home screen itself (date header + row per bucket), fed through
 *   [MegaApiGateway.getRecentActionsAsync] which the fake resolves from a stubbed request
 *   carrying a [StubMegaRecentActionBucketList].
 * - There is no avatar on the redesigned Home; the account entry is the Menu tab's account row
 *   (avatar + name + email), which opens the My Account screen — tc19 is ported against that.
 *
 * Ported: tc01 (Favourites list/grid switch), tc04 (file lookup + text viewer, via the Home
 * search — see below), tc05 (Audio list/grid switch), tc09/tc10/tc11/tc12 (text/video/audio/image
 * file under Today in Recents), tc19 (My Account via the account avatar row).
 *
 * Adaptations and skips:
 * - tc03 (Documents category) is NOT ported: the redesigned app on develop has no Documents
 *   category surface (no chip, no navigation destination — only the legacy
 *   `DocumentSectionViewModel` remains, unreachable from the new navigation graph).
 * - tc04 originally looked the file up under the Documents category; it is ported against the
 *   Home search (same intent: find a seeded text file by name and open it in the text viewer).
 *   The text viewer downloads the file through [MegaApiGateway.startDownload]; the fake's
 *   scripted transfer reports a pre-created local file as the downloaded result.
 */
@HiltAndroidTest
class HomeScreenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

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

        // Cloud Drive children may be resolved on start; answer through the fake node tree.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

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

    /** tc01 — a favourited file shows under Favourites (with heart icon) and survives a list/grid switch. */
    @Test
    fun favouritesListGridSwitchShowsSeededFile() {
        val fileName = "sm10tc01.txt"
        seedSearchableFile(fileName, isFavourite = true)

        launchHome()
        val favouritesChip = str("video_section_title_favourite_playlist")
        awaitObject(By.desc(favouritesChip), LOAD_TIMEOUT, "favourites-chip")
        device.findObject(By.desc(favouritesChip)).click()

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "favourites-list-row")
        awaitObject(By.res(NODE_LIST_FAVOURITE_ICON_TAG), LOAD_TIMEOUT, "favourites-heart-icon")

        click(By.res(GRID_VIEW_TOGGLE_TAG), "grid-view-toggle")
        awaitObject(By.res(NODE_GRID_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "favourites-grid-item")

        click(By.res(LIST_VIEW_TOGGLE_TAG), "list-view-toggle")
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "favourites-list-row-restored")
    }

    /**
     * tc04 — looking a text file up by name and selecting the result opens it in the text viewer,
     * which shows the file name and its content. The AAT scenario searched under the Documents
     * category; with no Documents surface in the redesigned app this uses the Home search.
     */
    @Test
    fun fileLookupOpensTextViewerWithContent() {
        val fileName = "sm10tc04.txt"
        seedSearchableFile(fileName)

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

        launchHome()
        click(By.res(SEARCH_ACTION_TAG), "home-search-action")

        // The search field is a Compose text field exposed as an EditText node.
        awaitObject(By.clazz("android.widget.EditText"), LOAD_TIMEOUT, "search-input")
        device.findObject(By.clazz("android.widget.EditText")).text = fileName

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "search-result-row")
        device.findObject(By.res(NODE_LIST_TITLE_TAG).text(fileName)).click()

        awaitObject(By.text(FILE_CONTENT), LOAD_TIMEOUT, "text-viewer-content")
        awaitObject(By.text(fileName), LOAD_TIMEOUT, "text-viewer-file-name")
    }

    /** tc05 — a seeded audio file shows under the Audio category and survives a list/grid switch. */
    @Test
    fun audioListGridSwitchShowsSeededFile() {
        val fileName = "Sound-AutoTest.mp3"
        seedSearchableFile(fileName)

        launchHome()
        val audioChip = str("home_screen_audios_chip_title")
        awaitObject(By.desc(audioChip), LOAD_TIMEOUT, "audio-chip")
        device.findObject(By.desc(audioChip)).click()

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "audio-list-row")

        click(By.res(GRID_VIEW_TOGGLE_TAG), "grid-view-toggle")
        awaitObject(By.res(NODE_GRID_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "audio-grid-item")
    }

    /** tc09 — a text file added today shows under the Today header in Recents. */
    @Test
    fun recentTextFileShownUnderToday() {
        assertRecentFileShownUnderToday("sm10tc08.txt")
    }

    /** tc10 — a video file added today shows under the Today header in Recents. */
    @Test
    fun recentVideoFileShownUnderToday() {
        assertRecentFileShownUnderToday("Video-AutoTest.mp4")
    }

    /** tc11 — an audio file added today shows under the Today header in Recents. */
    @Test
    fun recentAudioFileShownUnderToday() {
        assertRecentFileShownUnderToday("Sound-AutoTest.mp3")
    }

    /** tc12 — an image file added today shows under the Today header in Recents. */
    @Test
    fun recentImageFileShownUnderToday() {
        assertRecentFileShownUnderToday("Image-AutoTest.png")
    }

    /** tc19 — the account (avatar) row opens My Account, showing the account details. */
    @Test
    fun myAccountOpensFromAccountAvatarRow() {
        launchHome()

        click(By.res(MENU_NAV_ITEM_TAG), "menu-nav-item")
        click(By.res(MENU_MY_ACCOUNT_ITEM_TAG), "menu-account-row")

        awaitObject(
            By.res(MY_ACCOUNT_EMAIL_TAG).text(fakeMegaApi.account.email),
            LOAD_TIMEOUT,
            "my-account-email",
        )
        scrollUntilRes(MY_ACCOUNT_CONTACTS_TAG)
        awaitObject(By.res(MY_ACCOUNT_CONTACTS_TAG), LOAD_TIMEOUT, "my-account-contacts-row")
        scrollUntilRes(MY_ACCOUNT_LAST_SESSION_TAG)
        awaitObject(By.res(MY_ACCOUNT_LAST_SESSION_TAG), LOAD_TIMEOUT, "my-account-last-session-row")
    }

    // endregion

    // region shared scenario bodies

    /**
     * Seeds [fileName] as a today-timestamped single-file recent-actions bucket and asserts that
     * the Home Recents widget shows it under the Today header.
     */
    private fun assertRecentFileShownUnderToday(fileName: String) {
        val node = seedSearchableFile(fileName)
        fakeMegaApi.stubRequest(
            MegaApiGateway::getRecentActionsAsync,
            request = StubMegaRequest(
                type = MegaRequest.TYPE_GET_RECENT_ACTIONS,
                recentActions = StubMegaRecentActionBucketList(
                    listOf(
                        StubMegaRecentActionBucket(
                            timestamp = System.currentTimeMillis() / 1000,
                            userEmail = fakeMegaApi.account.email,
                            parentHandle = rootHandle,
                            id = "bucket-1",
                            nodes = StubMegaNodeList(listOf(node)),
                        ),
                    ),
                ),
            ),
        )

        launchHome()

        scrollUntilRes(RECENTS_DATE_HEADER_TAG)
        awaitObject(
            By.res(RECENTS_DATE_HEADER_TAG).text(str("search_dropdown_chip_filter_type_date_today")),
            LOAD_TIMEOUT,
            "recents-today-header",
        )
        awaitObject(
            By.res(RECENTS_ITEM_FIRST_LINE_TAG).text(fileName),
            LOAD_TIMEOUT,
            "recents-row-$fileName",
        )
    }

    // endregion

    // region seeding helpers

    /**
     * Seeds a file node in the fake tree and answers every node search with it — the Favourites,
     * Audio and Search screens all resolve their content through
     * [MegaApiGateway.searchWithFilter], whose filter is an opaque native SDK object, so the
     * search is answered by method rather than by filter contents.
     */
    private fun seedSearchableFile(
        fileName: String,
        isFavourite: Boolean = false,
    ): StubMegaNode {
        val node = StubMegaNode(
            handle = FILE_HANDLE,
            name = fileName,
            parentHandle = rootHandle,
            size = FILE_SIZE,
            modificationTime = System.currentTimeMillis() / 1000,
            isFavourite = isFavourite,
        )
        fakeMegaApi.nodeTree.addNode(node, parentHandle = rootHandle)
        fakeMegaApi.stub(MegaApiGateway::searchWithFilter) { listOf(node) }
        return node
    }

    // endregion

    // region navigation & UiAutomator helpers

    /** Launches the logged-in app; the redesigned Home is the default start screen. */
    private fun launchHome() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        awaitObject(By.res(HOME_APP_BAR_TAG), LAUNCH_TIMEOUT, "home-app-bar")
    }

    private fun str(name: String): String {
        val id = targetContext.resources.getIdentifier(name, "string", targetContext.packageName)
        require(id != 0) { "string resource not found: $name" }
        return targetContext.getString(id)
    }

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
    }

    private fun clickText(text: String, name: String) = click(By.text(text), name)

    /**
     * Scrolls the first scrollable container until the node tagged [tag] is on screen. No-op when
     * it is already visible or nothing is scrollable — compose lazy lists only compose (and expose)
     * items near the viewport.
     */
    private fun scrollUntilRes(tag: String) {
        if (device.hasObject(By.res(tag))) return
        runCatching {
            UiScrollable(UiSelector().scrollable(true))
                .apply { setMaxSearchSwipes(MAX_SCROLL_SWIPES) }
                .scrollIntoView(UiSelector().resourceId(tag))
        }
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

        const val FILE_HANDLE = 100L
        const val FILE_SIZE = 1_024L
        const val FILE_CONTENT = "hello from the sm10 home screen port"

        /** Mirrors HOME_MAIN_APP_BAR_TAG of the redesigned HomeScreen. */
        const val HOME_APP_BAR_TAG = "home_screen:main_app_bar"

        /** Main navigation item test tag for the Menu tab (`MenuHomeScreen` destination). */
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Mirrors CommonMenuAction.Search.testTag (Home top bar search action). */
        const val SEARCH_ACTION_TAG = "app_bar:search"

        /** Mirrors the internal TITLE_TAG / FAVOURITE_ICON_TAG of NodeListViewItem rows. */
        const val NODE_LIST_TITLE_TAG = "node_list_view_item:title"
        const val NODE_LIST_FAVOURITE_ICON_TAG = "node_list_view_item:favourite_icon"

        /** Mirrors NODE_TITLE_TEXT_TEST_TAG of NodeGridViewItem. */
        const val NODE_GRID_TITLE_TAG = "node_grid_view_item:node_title"

        /** Mirrors GRID_VIEW_TOGGLE_TAG / LIST_VIEW_TOGGLE_TAG of NodeHeaderItem. */
        const val GRID_VIEW_TOGGLE_TAG = "header_view_item:grid_view_toggle"
        const val LIST_VIEW_TOGGLE_TAG = "header_view_item:list_view_toggle"

        /** Mirrors DATE_HEADER_TEST_TAG / FIRST_LINE_TEST_TAG of the Recents widget rows. */
        const val RECENTS_DATE_HEADER_TAG = "recents_widget:date_header"
        const val RECENTS_ITEM_FIRST_LINE_TAG = "recent_action_list_item_view:first_line"

        /** Mirrors MenuHomeScreenUiTestTags.MY_ACCOUNT_ITEM (row hosting the avatar). */
        const val MENU_MY_ACCOUNT_ITEM_TAG = "menu_home_screen:my_account_item"

        /** Mirrors MyAccountHomeViewTestTags of the My Account screen. */
        const val MY_ACCOUNT_EMAIL_TAG = "my_account_home_view:email_text"
        const val MY_ACCOUNT_CONTACTS_TAG = "my_account_home_view:list_item:contacts"
        const val MY_ACCOUNT_LAST_SESSION_TAG = "my_account_home_view:list_item:last_session"
    }
}
