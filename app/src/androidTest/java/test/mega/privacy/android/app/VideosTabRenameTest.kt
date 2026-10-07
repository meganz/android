package mega.privacy.android.app

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Configurator
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
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
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Full-app instrumented test for renaming a video from the Media > Videos tab, built on the
 * `:data-test` fake gateway framework and mirroring [RenameNodeTest].
 *
 * The Videos tab reloads its list when a video node update arrives. A rename produces a single
 * node update, so any delay applied to that trigger is paid in full before the new name shows up.
 * The test renames a video through the UI, then renames it back and forth through the fake SDK,
 * measuring each time from the `OnNodesUpdate` broadcast to the renamed row being on screen. The
 * median of those latencies must stay below [MAX_REFRESH_LATENCY_MS], which a 300ms refresh
 * debounce on node updates cannot meet. The median rather than a single sample is asserted
 * because the rest of the app reacts to the same node updates and occasionally delays one frame.
 */
@HiltAndroidTest
class VideosTabRenameTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private val videosTabLabel get() = targetContext.getString(sharedR.string.media_videos_tab_title)

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

        TestAppBoot.runCoreInitializers()

        VIDEO_NAMES.forEachIndexed { index, name ->
            fakeMegaApi.nodeTree.addNode(
                StubMegaNode(
                    handle = FIRST_VIDEO_HANDLE + index,
                    name = name,
                    parentHandle = rootHandle,
                    size = 5_242_880L,
                    duration = 60,
                ),
                parentHandle = rootHandle,
            )
        }

        fakeMegaApi.stubRequest(MegaApiGateway::renameNode)

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @Test
    fun renamedVideoRowShowsTheNewNameWithoutDebounceDelay() {
        ActivityScenario.launch(MegaActivity::class.java)

        awaitObject(By.res(MEDIA_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "media-nav-item")
        device.findObject(By.res(MEDIA_NAV_ITEM_TAG)).click()

        awaitObject(By.text(videosTabLabel), LOAD_TIMEOUT, "videos-tab")
        device.findObject(By.text(videosTabLabel)).click()

        VIDEO_NAMES.forEach { name ->
            awaitObject(videoNameRow(name), LOAD_TIMEOUT, "video-row-$name")
        }
        Thread.sleep(1_500)

        val renamedVideoRow = By.res(VIDEO_ROW_TAG).hasDescendant(videoNameRow(ORIGINAL_NAME))
        awaitObject(renamedVideoRow, LOAD_TIMEOUT, "row-to-rename")
        device.findObject(renamedVideoRow).findObject(By.res(VIDEO_MENU_ICON_TAG)).click()

        awaitObject(By.res(SHEET_LIST_TAG), LOAD_TIMEOUT, "node-options-sheet")
        scrollUntilObject(By.res(RENAME_ACTION_TAG), SHEET_LIST_TAG, "rename-action")
        device.findObject(By.res(RENAME_ACTION_TAG)).click()

        awaitObject(RENAME_INPUT, LOAD_TIMEOUT, "rename-dialog-input")
        device.findObject(RENAME_INPUT).text = NEW_NAME
        Thread.sleep(1_000)
        awaitObject(RENAME_CONFIRM, LOAD_TIMEOUT, "rename-confirm")
        device.findObject(RENAME_CONFIRM).click()

        awaitInvocation("renameNode", LOAD_TIMEOUT)
        awaitGone(RENAME_INPUT, LOAD_TIMEOUT, "rename-dialog")

        val latenciesMs = buildList {
            add(measureRenameLatency(NEW_NAME))
            repeat(FOLLOW_UP_RENAMES) { index ->
                Thread.sleep(RENAME_HOLD_MS)
                add(measureRenameLatency(if (index % 2 == 0) ORIGINAL_NAME else NEW_NAME))
            }
        }
        val medianLatencyMs = latenciesMs.sorted()[latenciesMs.size / 2]
        Log.d(LOG_TAG, "Rename latencies $latenciesMs ms, median ${medianLatencyMs}ms")

        Thread.sleep(2_000)

        assertTrue(
            "Renamed row took a median of ${medianLatencyMs}ms ($latenciesMs) to appear after " +
                    "the node update; expected under ${MAX_REFRESH_LATENCY_MS}ms",
            medianLatencyMs < MAX_REFRESH_LATENCY_MS,
        )
    }

    /**
     * Applies the SDK side of a rename to [newName] (the fake tree broadcasts `OnNodesUpdate`) and
     * returns how long the row took to show it.
     */
    private fun measureRenameLatency(newName: String): Long {
        val nodeUpdateAt = SystemClock.elapsedRealtime()
        runBlocking { fakeMegaApi.nodeTree.rename(FIRST_VIDEO_HANDLE + RENAMED_INDEX, newName) }
        return awaitObjectMeasured(videoNameRow(newName), LOAD_TIMEOUT) - nodeUpdateAt
    }

    private fun videoNameRow(name: String): BySelector =
        By.res(VIDEO_NAME_TAG).textContains(name)

    /**
     * Polls for [selector] without UiAutomator's wait interval or its wait-for-idle before each
     * lookup, so the returned `elapsedRealtime` is as close as possible to the frame the object
     * first appeared in rather than to the moment the app next went idle.
     *
     * The accessibility node cache is cleared before each lookup because Compose does not always
     * send the content-changed event that invalidates it; a stale cache adds up to a second before
     * a recomposed text is seen.
     */
    private fun awaitObjectMeasured(selector: BySelector, timeout: Long): Long {
        val configurator = Configurator.getInstance()
        val idleTimeout = configurator.waitForIdleTimeout
        configurator.waitForIdleTimeout = 0
        try {
            val deadline = SystemClock.elapsedRealtime() + timeout
            while (SystemClock.elapsedRealtime() < deadline) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    instrumentation.uiAutomation.clearCache()
                }
                if (device.hasObject(selector)) return SystemClock.elapsedRealtime()
            }
        } finally {
            configurator.waitForIdleTimeout = idleTimeout
        }
        throw AssertionError("Timed out after ${timeout}ms waiting for $selector")
    }

    private fun awaitGone(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.gone(selector), timeout)) return
        throw AssertionError("Timed out after ${timeout}ms waiting for $name to close")
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
     * Waits until the fake gateway records an invocation of [methodName], polling because the
     * production call happens in a coroutine that the UI click only enqueues.
     */
    private fun awaitInvocation(methodName: String, timeout: Long) {
        val deadline = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < deadline) {
            if (fakeMegaApi.invocations.any { it.methodName == methodName }) return
            Thread.sleep(INVOCATION_POLL_INTERVAL_MS)
        }
        throw AssertionError("Timed out after ${timeout}ms waiting for gateway call $methodName")
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
        const val LOG_TAG = "VideosTabRenameTest"

        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val MAX_SCROLL_ATTEMPTS = 10
        const val SCROLL_PERCENT = 0.8f
        const val INVOCATION_POLL_INTERVAL_MS = 100L

        /**
         * Reloading and recomposing the row takes well under 100ms, and observing it through a
         * fresh accessibility tree adds up to ~200ms, so an immediate refresh measures ~150–250ms.
         * A 300ms refresh debounce on node updates comes on top of that and cannot get under it.
         */
        const val MAX_REFRESH_LATENCY_MS = 350L

        const val FOLLOW_UP_RENAMES = 4
        const val RENAME_HOLD_MS = 1_000L

        const val FIRST_VIDEO_HANDLE = 200L
        val VIDEO_NAMES = listOf("beach.mp4", "city.mp4", "forest.mp4")
        const val RENAMED_INDEX = 1
        val ORIGINAL_NAME = VIDEO_NAMES[RENAMED_INDEX]
        const val NEW_NAME = "city-renamed.mp4"

        /** Main navigation item test tag for the Media section. */
        const val MEDIA_NAV_ITEM_TAG = "main_navigation:navigation_item_MediaMainNavKey"

        /** Mirrors VIDEO_ITEM_VIEW_TEST_TAG of VideoItemView. */
        const val VIDEO_ROW_TAG = "video_item:row_item"

        /** Mirrors VIDEO_ITEM_NAME_VIEW_TEST_TAG of VideoItemView. */
        const val VIDEO_NAME_TAG = "video_item:text_name"

        /** Mirrors VIDEO_ITEM_MENU_ICON_TEST_TAG of VideoItemView. */
        const val VIDEO_MENU_ICON_TAG = "video_item:image_menu"

        /** Mirrors NODE_OPTIONS_LAZY_COLUMN_TEST_TAG of the node options bottom sheet. */
        const val SHEET_LIST_TAG = "node_options_bottom_sheet:lazy_column"

        /** Mirrors the testTag of RenameMenuAction in the node options bottom sheet. */
        const val RENAME_ACTION_TAG = "menu_action:rename"

        /**
         * The rename dialog renders in its own window without resource ids, so it is matched by
         * class and text, as in [RenameNodeTest].
         */
        val RENAME_INPUT: BySelector = By.clazz("android.widget.EditText")
        val RENAME_CONFIRM: BySelector =
            By.clickable(true).hasDescendant(By.text("Rename"))
    }
}
