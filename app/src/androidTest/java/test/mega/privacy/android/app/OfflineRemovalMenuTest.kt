package mega.privacy.android.app

import android.os.SystemClock
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
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaTransfer
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.offline.GetOfflinePathForNodeUseCase
import nz.mega.sdk.MegaTransfer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Full-app instrumented test for AND-19559: after a file is made available offline and removed
 * from offline again, its node options sheet must offer "Make available offline", not
 * "Remove from Offline".
 *
 * The app deletes the active transfers list only when all of its transfers have finished, so the
 * completed offline download stays in the list while another download is still active. The test
 * keeps a second download active for this reason, and keeps the app running between the offline
 * download and the removal, as a user does. The `OfflineDownloadInProgressState` flag is on,
 * because the in-progress state only affects the offline items while it is on.
 *
 * Built on the `:data-test` fake-SDK framework like [FileFolderMenuTest]: the whole app runs as
 * in production with only the SDK gateways faked, and the UI is driven with UiAutomator. The
 * holds at each state keep the journey recordable for the MR.
 */
@HiltAndroidTest
class OfflineRemovalMenuTest {

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
    lateinit var getNodeByIdUseCase: GetNodeByIdUseCase

    @Inject
    lateinit var getOfflinePathForNodeUseCase: GetOfflinePathForNodeUseCase

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

        fakeFlags.set(ApiFeatures.OfflineDownloadInProgressState, true)
    }

    @After
    fun tearDown() {
        scenario?.close()
        fakeFlags.clear()
    }

    @Test
    fun removingADownloadedFileFromOfflineShowsMakeAvailableOfflineAgain() {
        seedFile()
        stubOfflineDownload()

        launchCloudDrive()
        hold(CAMERA_HOLD_SHORT_MS)

        openNodeOptions()
        scrollSheetUntil(By.res(AVAILABLE_OFFLINE_ACTION_TAG), "available-offline-action")
        hold(CAMERA_HOLD_SHORT_MS)
        device.findObject(By.res(AVAILABLE_OFFLINE_ACTION_TAG)).click()

        awaitInvocations("startDownloadBatch") {
            fakeMegaApi.invocationsOf(MegaApiGateway::startDownloadBatch)
        }
        hold(CAMERA_HOLD_SHORT_MS)

        openNodeOptions()
        scrollSheetUntil(By.res(REMOVE_OFFLINE_ACTION_TAG), "remove-offline-action")
        hold(CAMERA_HOLD_SHORT_MS)
        device.findObject(By.res(REMOVE_OFFLINE_ACTION_TAG)).click()
        awaitGone(By.res(SHEET_LIST_TAG), "node-options-sheet-closed")
        hold(CAMERA_HOLD_SHORT_MS)

        openNodeOptions()
        scrollSheetUntil(By.res(OFFLINE_ACTION_TAGS), "offline-action")
        hold(CAMERA_HOLD_LONG_MS)

        assertThat(device.hasObject(By.res(REMOVE_OFFLINE_ACTION_TAG))).isFalse()
        assertThat(device.hasObject(By.res(AVAILABLE_OFFLINE_ACTION_TAG))).isTrue()
    }

    // region seeding helpers

    private fun seedFile() {
        val node = StubMegaNode(
            handle = FILE_HANDLE,
            name = FILE_NAME,
            parentHandle = rootHandle,
            isFolder = false,
            size = FILE_SIZE,
            modificationTime = System.currentTimeMillis() / 1000,
            owner = fakeMegaApi.account.myUserHandle,
        )
        fakeMegaApi.nodeTree.addNode(node, parentHandle = rootHandle)
    }

    /**
     * Scripts the offline download of the seeded file the way the SDK reports it: a start event
     * while the transfer is active, then a successful finish. The app starts an offline download
     * as a batch, and the fake also reports the events to the global transfer listener, where the
     * SDK reports the file transfers of a batch. Both events carry the offline app data that the
     * app attaches to the download, so the app saves the offline information of the file when the
     * transfer finishes. The file is created at its offline path because the SDK writes it there.
     *
     * Between the two events, the script reports a progress update of another download that does
     * not finish, so that the active transfers list is not deleted when the offline download
     * finishes.
     */
    private fun stubOfflineDownload() {
        val offlineFile = runBlocking {
            val node = checkNotNull(getNodeByIdUseCase(NodeId(FILE_HANDLE)))
            File(checkNotNull(getOfflinePathForNodeUseCase(node)), FILE_NAME)
        }
        offlineFile.parentFile?.mkdirs()
        offlineFile.writeText(FILE_CONTENT)

        fun transfer(state: Int, transferredBytes: Long, isFinished: Boolean) = StubMegaTransfer(
            type = MegaTransfer.TYPE_DOWNLOAD,
            tag = TRANSFER_TAG,
            uniqueId = TRANSFER_TAG.toLong(),
            fileName = FILE_NAME,
            path = offlineFile.absolutePath,
            parentPath = offlineFile.parent,
            nodeHandle = FILE_HANDLE,
            transferredBytes = transferredBytes,
            totalBytes = FILE_SIZE,
            state = state,
            isFinished = isFinished,
            appData = "$OFFLINE_DOWNLOAD_APP_DATA>$FILE_HANDLE",
        )
        val otherDownload = StubMegaTransfer(
            type = MegaTransfer.TYPE_DOWNLOAD,
            tag = OTHER_TRANSFER_TAG,
            uniqueId = OTHER_TRANSFER_TAG.toLong(),
            fileName = OTHER_FILE_NAME,
            path = File(targetContext.cacheDir, OTHER_FILE_NAME).absolutePath,
            parentPath = targetContext.cacheDir.absolutePath,
            nodeHandle = OTHER_FILE_HANDLE,
            transferredBytes = FILE_SIZE,
            totalBytes = OTHER_FILE_SIZE,
            state = MegaTransfer.STATE_ACTIVE,
        )
        fakeMegaApi.stubTransferScript(
            MegaApiGateway::startDownloadBatch,
            steps = listOf(
                transfer(MegaTransfer.STATE_ACTIVE, transferredBytes = 0L, isFinished = false),
                otherDownload,
            ),
            finalTransfer = transfer(
                MegaTransfer.STATE_COMPLETED,
                transferredBytes = FILE_SIZE,
                isFinished = true,
            ),
            stepDelayMs = TRANSFER_STEP_DELAY_MS,
        )
    }

    // endregion

    // region navigation helpers

    /** Launches the logged-in app and opens the Cloud Drive section. */
    private fun launchCloudDrive() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        click(By.res(DRIVE_NAV_ITEM_TAG), "drive-nav-item")
        awaitObject(By.res(NODE_TITLE_TAG).text(FILE_NAME), LOAD_TIMEOUT, "file-row")
    }

    /**
     * Opens the node options sheet of the seeded file. Rows carry no per-row tag, so the more
     * icon is resolved by vertical alignment with the row's title node.
     */
    private fun openNodeOptions() {
        awaitObject(By.res(NODE_TITLE_TAG).text(FILE_NAME), LOAD_TIMEOUT, "file-row")
        val titleCenterY = device.findObject(By.res(NODE_TITLE_TAG).text(FILE_NAME))
            .visibleBounds.centerY()
        val moreIcon = device.findObjects(By.res(MORE_ICON_TAG))
            .minByOrNull { abs(it.visibleBounds.centerY() - titleCenterY) }
            ?: run {
                dumpHierarchy("more-icon")
                throw AssertionError("No more icon found for row $FILE_NAME")
            }
        moreIcon.click()
        awaitObject(By.res(SHEET_LIST_TAG), LOAD_TIMEOUT, "node-options-sheet")
    }

    // endregion

    // region UiAutomator helpers

    private fun hold(durationMs: Long) = Thread.sleep(durationMs)

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
    }

    /**
     * Scrolls the node options sheet's lazy list until [selector] is present — entries below the
     * visible area are not composed and cannot be matched until scrolled into view.
     */
    private fun scrollSheetUntil(selector: BySelector, name: String) {
        repeat(MAX_SCROLL_ATTEMPTS) {
            if (device.hasObject(selector)) return
            val container = device.findObject(By.res(SHEET_LIST_TAG)) ?: return@repeat
            container.setGestureMargin(container.visibleBounds.height() / 4)
            container.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
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
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to be gone; " +
                    "hierarchy in logcat tag UiDump",
        )
    }

    /**
     * Gateway commands are dispatched from coroutine launches, so their invocations are not
     * recorded synchronously after the UI action that triggers them. Poll [supplier] until at
     * least one invocation matches, or fail with what was actually recorded.
     */
    private fun <T> awaitInvocations(name: String, supplier: () -> List<T>): List<T> {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for an invocation of $name; " +
                    "recorded api=${fakeMegaApi.invocations.map { it.methodName }}",
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
        const val LOAD_TIMEOUT = 30_000L
        const val POLL_INTERVAL_MS = 100L
        const val MAX_SCROLL_ATTEMPTS = 10
        const val SCROLL_PERCENT = 0.8f
        const val CAMERA_HOLD_SHORT_MS = 1_000L
        const val CAMERA_HOLD_LONG_MS = 2_000L
        const val TRANSFER_STEP_DELAY_MS = 500L

        const val FILE_HANDLE = 100L
        const val FILE_SIZE = 1_024L
        const val FILE_NAME = "offline-demo.txt"
        const val FILE_CONTENT = "Offline demo"
        const val TRANSFER_TAG = 1
        const val OTHER_TRANSFER_TAG = 2
        const val OTHER_FILE_HANDLE = 101L
        const val OTHER_FILE_NAME = "other-download.mp4"
        const val OTHER_FILE_SIZE = 1_048_576_000L

        /** The SDK app data key of an offline download, written by the app on the transfer. */
        const val OFFLINE_DOWNLOAD_APP_DATA = "OFFLINE_DOWNLOAD"

        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"
        const val NODE_TITLE_TAG = "node_list_view_item:title"
        const val MORE_ICON_TAG = "node_list_view_item:more_icon"
        const val SHEET_LIST_TAG = "node_options_bottom_sheet:lazy_column"
        const val AVAILABLE_OFFLINE_ACTION_TAG = "menu_action:available_offline"
        const val REMOVE_OFFLINE_ACTION_TAG = "menu_action:remove_offline"

        /** Matches the offline item of the sheet, whichever of the two the sheet shows. */
        val OFFLINE_ACTION_TAGS: Pattern = Pattern.compile("menu_action:(available|remove)_offline")
    }
}
