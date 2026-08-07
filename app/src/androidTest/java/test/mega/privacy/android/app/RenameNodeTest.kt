package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Reference full-app instrumented test for renaming a node, built on the `:data-test` fake gateway
 * framework and mirroring [CloudDriveUploadTest].
 *
 * The whole app runs as in production — real activities, navigation, ViewModels, use cases and
 * repositories — with only the SDK gateways faked (see
 * [mega.privacy.android.app.di.FakeSdkGatewayModule]). The scenario:
 *
 * 1. The fake reports an already logged-in account (persisted through the app's own
 *    save-credentials path) with a single file seeded in the Cloud Drive.
 * 2. The rename is driven from the UI: open the file row's overflow menu, tap "Rename", type the
 *    new name and confirm. The production flow calls [MegaApiGateway.renameNode] (stubbed to
 *    succeed).
 * 3. The SDK-side effect of the rename is applied with the `:data-test` mutating helper
 *    [mega.privacy.android.data.test.state.FakeNodeTree.rename], which renames the node in the
 *    fake tree and broadcasts the matching `OnNodesUpdate` in one call — the app's node monitoring
 *    picks it up and the row shows the new name.
 *
 * UI is driven with UiAutomator rather than a Compose test rule: the compose rule's idle
 * synchronization interferes with the production activity's splash-gated composition, while
 * UiAutomator observes the UI without touching the app's frame clock. Compose test tags are
 * matched as resource ids because the app sets `testTagsAsResourceId = true`.
 */
@HiltAndroidTest
class RenameNodeTest {

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

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        // Boot the test process through the production initialiser units (MegaApplication's
        // onCreate/onStart do not run under the Hilt test application). No units are excluded: all
        // are safe in the test process.
        TestAppBoot.runCoreInitializers()

        // The node list is loaded via getChildren(filter, ...); the filter is an opaque SDK object,
        // so resolve children through the fake node tree explicitly.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        // Seed a single file in the Cloud Drive so the list renders one renameable row.
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = FILE_HANDLE,
                name = ORIGINAL_NAME,
                parentHandle = rootHandle,
                size = 1_024L,
            ),
            parentHandle = rootHandle,
        )

        // The rename request completes successfully (this is also the fake's default; stated
        // explicitly for the scenario).
        fakeMegaApi.stubRequest(MegaApiGateway::renameNode)

        // Persist a logged-in session through the app's real credentials path; the account details
        // come from the fake gateway's logged-in defaults.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @Test
    fun renamedFileRowShowsTheNewName() {
        ActivityScenario.launch(MegaActivity::class.java)

        // Logged-in home UI is up once the bottom navigation renders; open the Drive section.
        awaitObject(By.res(DRIVE_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "drive-nav-item")
        device.findObject(By.res(DRIVE_NAV_ITEM_TAG)).click()

        // The seeded file row appears; open its overflow menu.
        awaitObject(By.res(NODE_TITLE_TAG).text(ORIGINAL_NAME), LOAD_TIMEOUT, "seeded-file-row")
        awaitObject(By.res(MORE_ICON_TAG), LOAD_TIMEOUT, "node-more-icon")
        device.findObject(By.res(MORE_ICON_TAG)).click()

        // Node options bottom sheet → Rename, which sits below the sheet's initially visible area.
        scrollUntilObject(By.res(RENAME_ACTION_TAG), SHEET_LIST_TAG, "rename-action")
        device.findObject(By.res(RENAME_ACTION_TAG)).click()

        // Rename dialog: replace the text and confirm.
        awaitObject(RENAME_INPUT, LOAD_TIMEOUT, "rename-dialog-input")
        device.findObject(RENAME_INPUT).text = NEW_NAME
        awaitObject(RENAME_CONFIRM, LOAD_TIMEOUT, "rename-confirm")
        device.findObject(RENAME_CONFIRM).click()

        // Production code has now called renameNode. Apply the SDK-side effect (rename in the fake
        // tree + OnNodesUpdate broadcast) with the new `:data-test` mutating helper.
        assertThat(fakeMegaApi.invocations.any { it.methodName == "renameNode" }).isTrue()
        runBlocking { fakeMegaApi.nodeTree.rename(FILE_HANDLE, NEW_NAME) }

        // The list refreshes and the row shows the new name.
        awaitObject(By.res(NODE_TITLE_TAG).text(NEW_NAME), LOAD_TIMEOUT, "renamed-file-row")

        // Hold the final state briefly so a human watching the run can see the result.
        Thread.sleep(2_000)
    }

    /**
     * Scrolls the list identified by [containerTag] until [selector] is present. The bottom sheet
     * renders its actions in a LazyColumn, so entries below the visible area are not composed and
     * cannot be matched by UiAutomator until they are scrolled into view.
     */
    private fun scrollUntilObject(selector: BySelector, containerTag: String, name: String) {
        repeat(MAX_SCROLL_ATTEMPTS) {
            if (device.hasObject(selector)) return
            val container = device.findObject(By.res(containerTag)) ?: return@repeat
            container.setGestureMargin(container.visibleBounds.height() / 4)
            container.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
    }

    /**
     * Waits for [selector]; on timeout, writes the window hierarchy to logcat (tag `UiDump`,
     * chunked — it survives the post-test uninstall) before failing.
     */
    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
            Log.d("UiDump", "[$name#$index] $chunk")
        }
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
        )
    }

    private companion object {
        const val LAUNCH_TIMEOUT = 30_000L
        const val LOAD_TIMEOUT = 15_000L
        const val MAX_SCROLL_ATTEMPTS = 10
        const val SCROLL_PERCENT = 0.8f

        const val FILE_HANDLE = 100L
        const val ORIGINAL_NAME = "before.txt"
        const val NEW_NAME = "after.txt"

        /** Main navigation item test tag for the Drive/Sync section. */
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"

        /** Mirrors the internal TITLE_TAG of NodeListViewItem rows. */
        const val NODE_TITLE_TAG = "node_list_view_item:title"

        /** Mirrors the internal MORE_ICON_TAG of NodeListViewItem rows. */
        const val MORE_ICON_TAG = "node_list_view_item:more_icon"

        /** Mirrors NODE_OPTIONS_LAZY_COLUMN_TEST_TAG of the node options bottom sheet. */
        const val SHEET_LIST_TAG = "node_options_bottom_sheet:lazy_column"

        /** Mirrors the testTag of RenameMenuAction in the node options bottom sheet. */
        const val RENAME_ACTION_TAG = "menu_action:rename"

        /** Localized label of the rename dialog's confirm button (context_rename). */
        const val RENAME_CONFIRM_TEXT = "Rename"

        /**
         * The rename dialog renders in its own window, where the activity's `testTagsAsResourceId`
         * does not apply and no node carries a resource id, so it is matched by class and text.
         * The confirm label sits in a child of the clickable node and is also used by the dialog
         * title, hence the descendant match.
         */
        val RENAME_INPUT: BySelector = By.clazz("android.widget.EditText").text(ORIGINAL_NAME)
        val RENAME_CONFIRM: BySelector =
            By.clickable(true).hasDescendant(By.text(RENAME_CONFIRM_TEXT))
    }
}
