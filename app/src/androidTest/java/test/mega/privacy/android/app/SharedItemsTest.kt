package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaSearchFilter
import nz.mega.sdk.MegaShare
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm18 ShareItems` — the Shared Items scenarios
 * that a fake share topology can reproduce. Built on the `:data-test` fake-SDK framework and
 * following [HomeScreenTest] / [RenameNodeTest] / [ChatRoomTest]: the whole app runs as in
 * production with only the SDK gateways faked, the UI is driven with UiAutomator, and Compose
 * test tags are matched as resource ids because [MegaActivity] sets `testTagsAsResourceId = true`.
 *
 * Share topology is seeded through `FakeMegaApiGateway.shares` ([mega.privacy.android.data.test.state.FakeShareState]):
 * incoming shares feed `get{Unverified,Verified}IncomingShares` + `getUserFromInShare` +
 * `getAccess` + `getContact`, outgoing shares feed `getOutgoingSharesNode` / `getOutShares`, and
 * public links feed `getPublicLinks`. Listener-based share commands (`deleteNode` for leave,
 * `setShareAccess` for remove-share / change-permission, `disableExport` for remove-link) complete
 * OK on the fake but never mutate state themselves; each test asserts the gateway invocation
 * ([awaitInvocations]) and then applies the SDK-side effect with the share state's mutating
 * helpers, which broadcast the matching `OnNodesUpdate` so the production monitoring refreshes
 * the screen.
 *
 * Shares are seeded UNVERIFIED throughout: resolving the display name of a verified
 * single-recipient share goes through the legacy `ContactUtil.getMegaUserNameDB` static, which
 * requires `MegaApplication.getInstance()` — never initialised under the Hilt test application —
 * so the production mapper would drop the share. Unverified shares render the peer's email as the
 * row subtitle (the AAT scenarios assert the account email too) through the same production
 * pipeline, without the legacy static.
 *
 * Ported: tc01 (incoming owner + list/grid), tc02 (outgoing recipient + list/grid), tc03 (links
 * tab lists folder+file links), tc04/tc05/tc06 (incoming folder info at read-only / read-write /
 * full access, against the revamped Compose file info screen — the legacy FileInfoActivity talks
 * to the SDK directly and cannot run on the fakes, so `FileInfoRevamp` is forced on), tc07 (leave
 * incoming share), tc08 (remove outgoing share), tc09 (remove folder+file links), tc11 (rename
 * outgoing folder), tc12 (outgoing folder info: shared-with contact + permission), tc15 (change
 * share permission — carries the demo-recording camera holds).
 *
 * Not ported (out of scope per AND-24640): tc10/tc13/tc14/tc16/tc17/tc18 — cross-account
 * copy/move/download outcomes needing real bytes or a second real account.
 */
@HiltAndroidTest
class SharedItemsTest {

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

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

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

        // The Info action must open the revamped Compose file info screen; the legacy
        // FileInfoActivity accesses the SDK directly and cannot run on the fake gateways.
        fakeFlags.set(ApiFeatures.FileInfoRevamp, true)

        // Children are resolved through the fake node tree by the filter's parent handle. A
        // blanket root answer must not be used: NodeRepositoryImpl.isChildrenEmpty recurses into
        // folder children, and answering every parent with the root's children (which contain the
        // seeded share folders themselves) recurses forever.
        fakeMegaApi.stub(MegaApiGateway::getChildren) { arguments ->
            val parentHandle = (arguments.firstOrNull() as? MegaSearchFilter)
                ?.byLocationHandle()
                ?.takeIf { it != MegaApiJava.INVALID_HANDLE }
                ?: rootHandle
            fakeMegaApi.nodeTree.childrenOf(parentHandle)
        }

        // Production registers this in MegaApplication.onCreate; under the Hilt test application
        // nothing does, and SnackBarHandlerImpl silently drops messages when it cannot resolve a
        // current activity — the leave-share snackbar assertion needs it.
        registerActivityLifecycleHandlerOnce()

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

    private fun registerActivityLifecycleHandlerOnce() {
        if (lifecycleHandlerRegistered) return
        lifecycleHandlerRegistered = true
        val application = targetContext.applicationContext as android.app.Application
        instrumentation.runOnMainSync {
            application.registerActivityLifecycleCallbacks(activityLifecycleHandler)
        }
    }

    // region tests

    /** tc01 — an incoming shared folder lists with its owner shown and survives a list/grid switch. */
    @Test
    fun incomingShareShowsOwnerAndSurvivesListGridSwitch() {
        val name = "f-sm18tc01"
        seedIncomingShare(name, MegaShare.ACCESS_READ)

        openSharedItems()

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "incoming-row")
        awaitObject(
            By.res(NODE_LIST_SUBTITLE_TAG).text(OWNER_EMAIL),
            LOAD_TIMEOUT,
            "incoming-row-owner",
        )
        awaitObject(By.res(NODE_LIST_PERMISSION_ICON_TAG), LOAD_TIMEOUT, "incoming-permission-icon")

        click(By.res(GRID_VIEW_TOGGLE_TAG), "grid-view-toggle")
        awaitObject(By.res(NODE_GRID_TITLE_TAG).text(name), LOAD_TIMEOUT, "incoming-grid-item")

        click(By.res(LIST_VIEW_TOGGLE_TAG), "list-view-toggle")
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "incoming-row-restored")
    }

    /** tc02 — an outgoing shared folder lists with its recipient shown and survives a list/grid switch. */
    @Test
    fun outgoingShareShowsRecipientAndSurvivesListGridSwitch() {
        val name = "f-sm18tc02"
        seedOutgoingShare(name, MegaShare.ACCESS_READ)

        openSharedItems()
        openTab(str("shares_screen_outgoing_shares_tab_title"))

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "outgoing-row")
        awaitObject(
            By.res(NODE_LIST_SUBTITLE_TAG).text(PEER_EMAIL),
            LOAD_TIMEOUT,
            "outgoing-row-recipient",
        )

        click(By.res(GRID_VIEW_TOGGLE_TAG), "grid-view-toggle")
        awaitObject(By.res(NODE_GRID_TITLE_TAG).text(name), LOAD_TIMEOUT, "outgoing-grid-item")
    }

    /** tc03 — seeded folder and file links list under the Links tab, flagged with the link icon. */
    @Test
    fun linksTabListsSeededFolderAndFileLinks() {
        val folderName = "f-sm18tc03"
        val fileName = "sm18tc03.txt"
        seedFolderLink(folderName)
        seedFileLink(fileName)

        openSharedItems()
        openTab(str("shares_screen_links_shares_tab_title"))

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(folderName), LOAD_TIMEOUT, "folder-link-row")
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "file-link-row")
        awaitObject(By.res(NODE_LIST_LINK_ICON_TAG), LOAD_TIMEOUT, "link-icon")
    }

    /** tc04 — an incoming read-only shared folder's info shows the owner and the access level. */
    @Test
    fun incomingFolderInfoShowsReadOnlyAccess() {
        assertIncomingFolderInfoShowsAccess(
            name = "f-sm18tc04",
            access = MegaShare.ACCESS_READ,
            accessLabel = str("share_folder_dialog_read_only_radio_option"),
        )
    }

    /** tc05 — an incoming read-and-write shared folder's info shows the access level. */
    @Test
    fun incomingFolderInfoShowsReadWriteAccess() {
        assertIncomingFolderInfoShowsAccess(
            name = "f-sm18tc05",
            access = MegaShare.ACCESS_READWRITE,
            accessLabel = str("share_folder_dialog_read_write_radio_option"),
        )
    }

    /** tc06 — an incoming full-access shared folder's info shows the access level. */
    @Test
    fun incomingFolderInfoShowsFullAccess() {
        assertIncomingFolderInfoShowsAccess(
            name = "f-sm18tc06",
            access = MegaShare.ACCESS_FULL,
            accessLabel = str("share_folder_dialog_full_access_radio_option"),
        )
    }

    /** tc07 — leaving an incoming share drives deleteNode and the row disappears once the SDK confirms. */
    @Test
    fun leaveIncomingShareRemovesRowAfterConfirmation() {
        val name = "f-sm18tc07"
        seedIncomingShare(name, MegaShare.ACCESS_READ)

        openSharedItems()
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "incoming-row")

        click(By.res(NODE_LIST_MORE_ICON_TAG), "row-more-icon")
        scrollSheetUntil(By.res(LEAVE_SHARE_ACTION_TAG), "leave-share-action")
        device.findObject(By.res(LEAVE_SHARE_ACTION_TAG)).click()

        // The confirmation dialog renders in its own window without resource ids; match by text.
        awaitObject(
            By.text(str("leave_shared_folder_confirmation_message")),
            LOAD_TIMEOUT,
            "leave-dialog",
        )
        clickText(str("general_leave"), "leave-confirm")

        awaitObject(
            By.text(plural("shared_items_incoming_shares_snackbar_leaving_shares_success", 1)),
            LOAD_TIMEOUT,
            "left-share-snackbar",
        )
        awaitInvocations("deleteNode", 1)

        // Apply the SDK-side effect: drop the share + node and broadcast the removal.
        runBlocking { fakeMegaApi.shares.removeIncomingShare(IN_SHARE_HANDLE) }

        awaitGone(By.res(NODE_LIST_TITLE_TAG).text(name), "incoming-row-gone")
    }

    /** tc08 — removing an outgoing share drives setShareAccess and empties the Outgoing tab. */
    @Test
    fun removeOutgoingShareEmptiesOutgoingTab() {
        val name = "f-sm18tc08"
        seedOutgoingShare(name, MegaShare.ACCESS_READ)

        openSharedItems()
        openTab(str("shares_screen_outgoing_shares_tab_title"))
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "outgoing-row")

        click(By.res(NODE_LIST_MORE_ICON_TAG), "row-more-icon")
        scrollSheetUntil(By.res(REMOVE_SHARE_ACTION_TAG), "remove-share-action")
        device.findObject(By.res(REMOVE_SHARE_ACTION_TAG)).click()

        awaitObject(By.text(str("stop_sharing_dialog_title")), LOAD_TIMEOUT, "stop-sharing-dialog")
        clickText(str("stop_sharing_dialog_positive_button_text"), "stop-sharing-confirm")

        awaitInvocations("setShareAccess", 1)

        // Apply the SDK-side effect: drop the recipient and broadcast the out-share change.
        runBlocking { fakeMegaApi.shares.removeOutgoingShare(OUT_SHARE_HANDLE) }

        awaitGone(By.res(NODE_LIST_TITLE_TAG).text(name), "outgoing-row-gone")
    }

    /** tc09 — removing the folder and file links drives disableExport and the rows disappear. */
    @Test
    fun removeLinksRemovesRowsFromLinksTab() {
        val folderName = "f-sm18tc09"
        val fileName = "sm18tc09.txt"
        seedFolderLink(folderName)
        seedFileLink(fileName)

        openSharedItems()
        openTab(str("shares_screen_links_shares_tab_title"))
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(folderName), LOAD_TIMEOUT, "folder-link-row")
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(fileName), LOAD_TIMEOUT, "file-link-row")

        removeLinkOfRow(folderName, "folder")
        awaitInvocations("disableExport", 1)
        runBlocking { fakeMegaApi.shares.removePublicLink(LINK_FOLDER_HANDLE) }
        awaitGone(By.res(NODE_LIST_TITLE_TAG).text(folderName), "folder-link-row-gone")

        removeLinkOfRow(fileName, "file")
        awaitInvocations("disableExport", 2)
        runBlocking { fakeMegaApi.shares.removePublicLink(LINK_FILE_HANDLE) }
        awaitGone(By.res(NODE_LIST_TITLE_TAG).text(fileName), "file-link-row-gone")
    }

    /** tc11 — renaming an outgoing shared folder drives renameNode and the row shows the new name. */
    @Test
    fun renameOutgoingFolderUpdatesRow() {
        val name = "f-sm18tc11"
        val newName = "renamed-sm18tc11"
        seedOutgoingShare(name, MegaShare.ACCESS_READ)

        openSharedItems()
        openTab(str("shares_screen_outgoing_shares_tab_title"))
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "outgoing-row")

        click(By.res(NODE_LIST_MORE_ICON_TAG), "row-more-icon")
        scrollSheetUntil(By.res(RENAME_ACTION_TAG), "rename-action")
        device.findObject(By.res(RENAME_ACTION_TAG)).click()

        // The rename dialog renders in its own window without resource ids; match by class/text.
        awaitObject(By.clazz("android.widget.EditText").text(name), LOAD_TIMEOUT, "rename-input")
        device.findObject(By.clazz("android.widget.EditText").text(name)).text = newName
        val confirm = By.clickable(true).hasDescendant(By.text(RENAME_CONFIRM_TEXT))
        awaitObject(confirm, LOAD_TIMEOUT, "rename-confirm")
        device.findObject(confirm).click()

        awaitInvocations("renameNode", 1)

        // Apply the SDK-side effect through the node tree (share flags survive the copy).
        runBlocking { fakeMegaApi.nodeTree.rename(OUT_SHARE_HANDLE, newName) }

        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(newName), LOAD_TIMEOUT, "renamed-row")
    }

    /** tc12 — the outgoing folder's info shows the shared-with count, recipient and permission. */
    @Test
    fun outgoingFolderInfoShowsSharedWithContactAndPermission() {
        val name = "f-sm18tc12"
        seedOutgoingShare(name, MegaShare.ACCESS_READ)

        openSharedItems()
        openTab(str("shares_screen_outgoing_shares_tab_title"))
        openInfoForRow(name)

        openSharedWithRecipients()

        awaitObject(
            By.res(SHARE_RECIPIENT_ITEM_TAG).hasDescendant(By.text(PEER_EMAIL)),
            LOAD_TIMEOUT,
            "recipient-row",
        )
        awaitObject(
            By.text(str("file_properties_shared_folder_read_only")),
            LOAD_TIMEOUT,
            "recipient-permission",
        )
    }

    /**
     * tc15 — changing the share permission from the recipient's options drives setShareAccess and
     * the recipient row renders the new level. Carries the demo-recording camera holds: the
     * before-state, the action and the after-state are each held on screen.
     */
    @Test
    fun changeSharePermissionRendersNewLevel() {
        val name = "f-sm18tc15"
        seedOutgoingShare(name, MegaShare.ACCESS_READ)

        openSharedItems()
        openTab(str("shares_screen_outgoing_shares_tab_title"))
        openInfoForRow(name)
        openSharedWithRecipients()

        awaitObject(
            By.res(SHARE_RECIPIENT_ITEM_TAG).hasDescendant(By.text(PEER_EMAIL)),
            LOAD_TIMEOUT,
            "recipient-row",
        )
        awaitObject(
            By.text(str("file_properties_shared_folder_read_only")),
            LOAD_TIMEOUT,
            "recipient-permission-before",
        )
        // Camera hold: the recipient at Read-only, before the change.
        Thread.sleep(1_500)

        click(By.res(SHARE_RECIPIENT_OPTIONS_TAG), "recipient-options")

        // The options and permission sheets are modal windows without resource ids on their
        // action rows; match by text.
        clickText(str("file_properties_shared_folder_change_permissions"), "change-permissions")
        clickText(str("file_properties_shared_folder_full_access"), "full-access-option")

        awaitInvocations("setShareAccess", 1)

        // Apply the SDK-side effect: update the share record and broadcast the out-share change.
        runBlocking {
            fakeMegaApi.shares.setOutgoingShareAccess(
                OUT_SHARE_HANDLE,
                PEER_EMAIL,
                MegaShare.ACCESS_FULL,
            )
        }

        awaitObject(
            By.res(SHARE_RECIPIENT_ITEM_TAG)
                .hasDescendant(By.text(str("file_properties_shared_folder_full_access"))),
            LOAD_TIMEOUT,
            "recipient-permission-after",
        )
        // Camera hold: the recipient at Full access, closing the demo recording.
        Thread.sleep(2_000)
    }

    // endregion

    // region shared scenario bodies

    /**
     * Seeds an incoming share at [access], opens its folder info from the node options sheet and
     * asserts the owner row and the [accessLabel] permission row of the revamped file info screen.
     */
    private fun assertIncomingFolderInfoShowsAccess(name: String, access: Int, accessLabel: String) {
        seedIncomingShare(name, access)

        openSharedItems()
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "incoming-row")
        awaitObject(By.res(NODE_LIST_PERMISSION_ICON_TAG), LOAD_TIMEOUT, "incoming-permission-icon")

        openInfoForRow(name)

        awaitObject(
            By.res(FILE_INFO_OWNER_TAG).hasDescendant(By.text(OWNER_EMAIL)),
            LOAD_TIMEOUT,
            "file-info-owner",
        )
        awaitObject(
            By.res(FILE_INFO_PERMISSIONS_TAG).hasDescendant(By.text(accessLabel)),
            LOAD_TIMEOUT,
            "file-info-permission",
        )
    }

    /** Opens the row's node options sheet and taps Info, landing on the file info screen. */
    private fun openInfoForRow(name: String) {
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(name), LOAD_TIMEOUT, "row-$name")
        moreIconOfRow(name).click()
        scrollSheetUntil(By.res(INFO_ACTION_TAG), "info-action")
        device.findObject(By.res(INFO_ACTION_TAG)).click()
        awaitObject(By.res(FILE_INFO_SCREEN_TAG), LOAD_TIMEOUT, "file-info-screen")
    }

    /** Taps the file info "Shared with" row (asserting its "1 contact" value) to open the recipients. */
    private fun openSharedWithRecipients() {
        awaitObject(
            By.res(FILE_INFO_SHARED_WITH_TAG)
                .hasDescendant(By.text(plural("file_info_information_num_contacts", 1))),
            LOAD_TIMEOUT,
            "file-info-shared-with",
        )
        device.findObject(By.res(FILE_INFO_SHARED_WITH_TAG)).click()
    }

    /** Opens the row's node options sheet on the Links tab and drives Remove link + confirmation. */
    private fun removeLinkOfRow(name: String, label: String) {
        moreIconOfRow(name).click()
        scrollSheetUntil(By.res(REMOVE_LINK_ACTION_TAG), "$label-remove-link-action")
        device.findObject(By.res(REMOVE_LINK_ACTION_TAG)).click()

        awaitObject(
            By.text(str("remove_links_warning_message")),
            LOAD_TIMEOUT,
            "$label-remove-link-dialog",
        )
        clickText(str("general_remove"), "$label-remove-link-confirm")
    }

    // endregion

    // region seeding helpers

    private fun seedIncomingShare(name: String, access: Int): StubMegaNode {
        val node = StubMegaNode(
            handle = IN_SHARE_HANDLE,
            name = name,
            parentHandle = MegaApiJava.INVALID_HANDLE,
            isFolder = true,
            isInShare = true,
        )
        fakeMegaApi.shares.addIncomingShare(
            node = node,
            ownerEmail = OWNER_EMAIL,
            ownerHandle = OWNER_HANDLE,
            access = access,
            isVerified = false,
        )
        return node
    }

    private fun seedOutgoingShare(name: String, access: Int): StubMegaNode {
        val node = StubMegaNode(
            handle = OUT_SHARE_HANDLE,
            name = name,
            parentHandle = rootHandle,
            isFolder = true,
            isOutShare = true,
        )
        fakeMegaApi.shares.addOutgoingShare(
            node = node,
            userEmail = PEER_EMAIL,
            userHandle = PEER_HANDLE,
            access = access,
            isVerified = false,
        )
        return node
    }

    private fun seedFolderLink(name: String): StubMegaNode {
        val node = StubMegaNode(
            handle = LINK_FOLDER_HANDLE,
            name = name,
            parentHandle = rootHandle,
            isFolder = true,
            isExported = true,
            publicLink = "https://mega.nz/folder/abc#folderkey",
        )
        fakeMegaApi.shares.addPublicLink(node)
        return node
    }

    private fun seedFileLink(name: String): StubMegaNode {
        val node = StubMegaNode(
            handle = LINK_FILE_HANDLE,
            name = name,
            parentHandle = rootHandle,
            size = 1_024L,
            isExported = true,
            publicLink = "https://mega.nz/file/def#filekey",
        )
        fakeMegaApi.shares.addPublicLink(node)
        return node
    }

    // endregion

    // region navigation & UiAutomator helpers

    /** Launches the logged-in app and opens Shared Items through the Menu tab. */
    private fun openSharedItems() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        awaitObject(By.res(HOME_APP_BAR_TAG), LAUNCH_TIMEOUT, "home-app-bar")

        click(By.res(MENU_NAV_ITEM_TAG), "menu-nav-item")
        // The account rows render only once the menu's async flows resolve; scrolling before that
        // races past the (still empty) list and strands the target row above the viewport.
        awaitObject(By.res(MENU_ACCOUNT_ITEM_TAG), LOAD_TIMEOUT, "menu-account-items")
        val sharedItemsLabel = str("video_section_videos_location_option_shared_items")
        scrollUntil(By.text(sharedItemsLabel))
        clickText(sharedItemsLabel, "shared-items-menu-row")

        awaitObject(By.res(SHARES_APP_BAR_TAG), LOAD_TIMEOUT, "shares-app-bar")
    }

    /** Switches to the Shared Items tab titled [title] (Incoming is the initial tab). */
    private fun openTab(title: String) {
        clickText(title, "tab-$title")
    }

    /**
     * The more-options icon of the row titled [title], resolved by walking up from the title to
     * the row container — rows carry no unique container tag, so the icon is scoped through its
     * row rather than matched globally (several rows may be on screen).
     */
    private fun moreIconOfRow(title: String): UiObject2 {
        awaitObject(By.res(NODE_LIST_TITLE_TAG).text(title), LOAD_TIMEOUT, "row-$title")
        var node: UiObject2 = device.findObject(By.res(NODE_LIST_TITLE_TAG).text(title))
        repeat(MAX_PARENT_HOPS) {
            node = node.parent ?: return@repeat
            node.findObject(By.res(NODE_LIST_MORE_ICON_TAG))?.let { return it }
        }
        dumpHierarchy("row-more-icon-$title")
        throw AssertionError("No more-options icon found for row $title")
    }

    private fun str(name: String): String {
        val id = targetContext.resources.getIdentifier(name, "string", targetContext.packageName)
        require(id != 0) { "string resource not found: $name" }
        return targetContext.getString(id)
    }

    private fun plural(name: String, quantity: Int): String {
        val id = targetContext.resources.getIdentifier(name, "plurals", targetContext.packageName)
        require(id != 0) { "plurals resource not found: $name" }
        return targetContext.resources.getQuantityString(id, quantity, quantity)
    }

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
    }

    private fun clickText(text: String, name: String) = click(By.text(text), name)

    /**
     * Scrolls the first scrollable container until [selector] is on screen. No-op when it is
     * already visible or nothing is scrollable — compose lazy lists only compose (and expose)
     * items near the viewport.
     */
    private fun scrollUntil(selector: BySelector) {
        if (device.hasObject(selector)) return
        runCatching {
            UiScrollable(UiSelector().scrollable(true))
                .apply { setMaxSearchSwipes(MAX_SCROLL_SWIPES) }
                .scrollForward()
        }
        repeat(MAX_SCROLL_SWIPES) {
            if (device.hasObject(selector)) return
            val scrollable = device.findObject(By.scrollable(true)) ?: return
            scrollable.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
    }

    /**
     * Scrolls the node options bottom sheet until [selector] is present — the sheet renders its
     * actions in a LazyColumn, so entries below the visible area are not composed until scrolled
     * into view.
     */
    private fun scrollSheetUntil(selector: BySelector, name: String) {
        awaitObject(By.res(SHEET_LIST_TAG), LOAD_TIMEOUT, "node-options-sheet")
        repeat(MAX_SCROLL_SWIPES) {
            if (device.hasObject(selector)) return
            val container = device.findObject(By.res(SHEET_LIST_TAG)) ?: return@repeat
            container.setGestureMargin(container.visibleBounds.height() / 4)
            container.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
    }

    /**
     * Waits until the fake gateway records at least [count] invocations of [methodName], polling
     * because the production call happens in a coroutine that the UI click only enqueues.
     */
    private fun awaitInvocations(methodName: String, count: Int) {
        val deadline = System.currentTimeMillis() + LOAD_TIMEOUT
        while (System.currentTimeMillis() < deadline) {
            if (fakeMegaApi.invocations.count { it.methodName == methodName } >= count) return
            Thread.sleep(INVOCATION_POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $count invocation(s) of $methodName"
        )
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
        )
    }

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name to disappear ($selector); " +
                    "hierarchy in logcat tag UiDump",
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
        /** Once-per-process guard; the orchestrator gives each test a fresh process. */
        var lifecycleHandlerRegistered = false

        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val MAX_SCROLL_SWIPES = 12
        const val MAX_PARENT_HOPS = 8
        const val SCROLL_PERCENT = 0.8f
        const val INVOCATION_POLL_INTERVAL_MS = 100L

        const val IN_SHARE_HANDLE = 100L
        const val OUT_SHARE_HANDLE = 200L
        const val LINK_FOLDER_HANDLE = 300L
        const val LINK_FILE_HANDLE = 301L

        /** The simulated second account owning the incoming shares. */
        const val OWNER_EMAIL = "sm18.owner@mega.nz"
        const val OWNER_HANDLE = 222L

        /** The simulated recipient of the outgoing shares. */
        const val PEER_EMAIL = "sm18.peer@mega.nz"
        const val PEER_HANDLE = 333L

        /** Mirrors HOME_MAIN_APP_BAR_TAG of the redesigned HomeScreen. */
        const val HOME_APP_BAR_TAG = "home_screen:main_app_bar"

        /** Main navigation item test tag for the Menu tab (`MenuHomeScreen` destination). */
        const val MENU_NAV_ITEM_TAG = "main_navigation:navigation_item_MenuHomeScreen"

        /** Mirrors MenuHomeScreenUiTestTags.ACCOUNT_ITEM — any rendered account-section row. */
        const val MENU_ACCOUNT_ITEM_TAG = "menu_home_screen:account_item"

        /** Mirrors SHARES_MAIN_APP_BAR_TAG of the Compose SharesScreen. */
        const val SHARES_APP_BAR_TAG = "shares_screen:main_app_bar"

        /** Mirrors the internal tags of NodeListViewItem / NodeGridViewItem rows. */
        const val NODE_LIST_TITLE_TAG = "node_list_view_item:title"
        const val NODE_LIST_SUBTITLE_TAG = "node_list_view_item:subtitle"
        const val NODE_LIST_MORE_ICON_TAG = "node_list_view_item:more_icon"
        const val NODE_LIST_PERMISSION_ICON_TAG = "node_list_view_item:permission_icon"
        const val NODE_LIST_LINK_ICON_TAG = "node_list_view_item:link_icon"
        const val NODE_GRID_TITLE_TAG = "node_grid_view_item:node_title"

        /** Mirrors GRID_VIEW_TOGGLE_TAG / LIST_VIEW_TOGGLE_TAG of NodeHeaderItem. */
        const val GRID_VIEW_TOGGLE_TAG = "header_view_item:grid_view_toggle"
        const val LIST_VIEW_TOGGLE_TAG = "header_view_item:list_view_toggle"

        /** Mirrors NODE_OPTIONS_LAZY_COLUMN_TEST_TAG of the node options bottom sheet. */
        const val SHEET_LIST_TAG = "node_options_bottom_sheet:lazy_column"

        /** Mirror the testTags of the node options menu actions. */
        const val INFO_ACTION_TAG = "menu_action:info"
        const val LEAVE_SHARE_ACTION_TAG = "menu_action:leave_share"
        const val REMOVE_SHARE_ACTION_TAG = "menu_action:remove_share"
        const val REMOVE_LINK_ACTION_TAG = "menu_action:remove_link"
        const val RENAME_ACTION_TAG = "menu_action:rename"

        /** Localized label of the rename dialog's confirm button (see RenameNodeTest). */
        const val RENAME_CONFIRM_TEXT = "Rename"

        /** Mirror the FILE_INFO_* tags of the revamped FileInfoScreen. */
        const val FILE_INFO_SCREEN_TAG = "file_info_screen:scaffold"
        const val FILE_INFO_OWNER_TAG = "file_info_screen:owner"
        const val FILE_INFO_PERMISSIONS_TAG = "file_info_screen:permissions"
        const val FILE_INFO_SHARED_WITH_TAG = "file_info_screen:shared_with"

        /** Mirror SHARE_RECIPIENT_LIST_ITEM / SHARE_RECIPIENT_LIST_ITEM_OPTIONS of the recipients list. */
        const val SHARE_RECIPIENT_ITEM_TAG = "share_recipient_list:item"
        const val SHARE_RECIPIENT_OPTIONS_TAG = "share_recipient_list:item_options"
    }
}
