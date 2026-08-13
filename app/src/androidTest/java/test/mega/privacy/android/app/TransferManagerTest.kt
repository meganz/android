package mega.privacy.android.app

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalTransfer
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.data.test.stub.StubMegaTransfer
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.transfers.active.CorrectActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteCompletedTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.completed.DeleteFailedOrCancelledTransfersUseCase
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaTransfer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented port of the AAT feature `sm07 Transfer Manager` — the *Portable*
 * scenarios that assert the Transfer Manager UI reacting to scripted transfer progress and to
 * pause / resume / cancel / clear actions. It is built on the `:data-test` fake-SDK framework and
 * follows [CloudDriveUploadTest]: the whole app runs as in production with only the SDK gateways
 * faked, the UI is driven with UiAutomator (not a Compose rule), and Compose test tags are matched
 * as resource ids because the screens set `testTagsAsResourceId = true`.
 *
 * How transfers are driven: the value here is the *manager UI reacting* to transfer progress, not
 * real transferred bytes. Transfers are produced by emitting the same global transfer events a
 * scripted [FakeMegaApiGateway.stubTransferScript] emits — `OnTransferStart` / `OnTransferUpdate`
 * / `OnTransferFinish`. The production app-start monitor (`MonitorTransferEventsInitializer`,
 * booted by [TestAppBoot.runCoreInitializers]) consumes them through the real pipeline
 * (`MonitorAndHandleTransferEventsUseCase` → `HandleTransferEventUseCase` → repository store /
 * Room), so the active list, the completed list and the toolbar widget update exactly as they do
 * in production. Events are chunked on a ~2s window, so assertions wait for the UI rather than
 * asserting synchronously.
 *
 * What is asserted per scenario is the user-visible outcome where the fake reproduces it
 * faithfully — a row in the correct tab, a paused/queued icon, a row gone after cancel/clear — and,
 * for pause/resume/cancel commands (fire-and-forget SDK requests the fake does not simulate a
 * follow-up event for), that the app invoked the matching gateway method, with the SDK's resulting
 * state simulated by emitting the corresponding transfer event, mirroring how [CloudDriveUploadTest]
 * simulates the SDK's post-upload node update.
 */
@HiltAndroidTest
class TransferManagerTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var deleteCompletedTransfersUseCase: DeleteCompletedTransfersUseCase

    @Inject
    lateinit var deleteFailedOrCancelledTransfersUseCase: DeleteFailedOrCancelledTransfersUseCase

    @Inject
    lateinit var correctActiveTransfersUseCase: CorrectActiveTransfersUseCase

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

        // Boot the test process through the production initialiser units (this starts the
        // transfer-events monitor that persists active/completed transfers). See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Cloud Drive is loaded via getChildren(filter, ...); resolve children through the fake node
        // tree, which starts with only the roots, so the drive is empty.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        // The global paused state is read from the SDK request's flag; make a pause-all request
        // resolve to "paused = true" so the manager's global paused state is exercised for real.
        // Resume (pause = false) keeps the default flag (false).
        fakeMegaApi.stubRequest(
            MegaApiGateway::pauseTransfers,
            request = StubMegaRequest(type = 0, flag = true),
            matcher = { it.firstOrNull() == true },
        )

        // Isolate each test: the fake gateway and Room persist across tests in the shared process.
        runCatching {
            runBlocking {
                deleteCompletedTransfersUseCase()
                deleteFailedOrCancelledTransfersUseCase()
                correctActiveTransfersUseCase(null)
            }
        }
        fakeMegaApi.clearInvocations()

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
        runCatching {
            runBlocking {
                deleteCompletedTransfersUseCase()
                deleteFailedOrCancelledTransfersUseCase()
                correctActiveTransfersUseCase(null)
            }
        }
    }

    // region tests

    /** tc01 — a single upload appears in the Active tab, then moves to the Completed tab. */
    @Test
    fun singleUploadAppearsInActiveThenCompleted() {
        openTransferManagerWith {
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
        }

        awaitActiveRow(FILE_A)

        finishTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)

        openCompletedTab()
        awaitCompletedRow(FILE_A)
    }

    /** tc02 — multiple uploads are shown simultaneously in the Active tab. */
    @Test
    fun multipleUploadsShownInActiveTab() {
        openTransferManagerWith {
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 2, uniqueId = 2L, fileName = FILE_B)
        }

        awaitActiveRow(FILE_A)
        awaitActiveRow(FILE_B)
    }

    /** tc05 — a completed transfer can be cleared and its row disappears. */
    @Test
    fun clearCompletedTransferRemovesRow() {
        openTransferManagerWith {
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
        }
        awaitActiveRow(FILE_A)

        finishTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
        openCompletedTab()
        awaitCompletedRow(FILE_A)

        // Enter selection with a long press, then Clear from the contextual top bar (real Room delete).
        device.findObject(By.res(COMPLETED_TRANSFER_ITEM_TAG)).longClick()
        click(CLEAR_SELECTED_ACTION_TAG, "clear-selected-action")

        awaitGone(By.res(COMPLETED_TRANSFER_NAME_TAG).text(FILE_A), "cleared-completed-row")
    }

    /** tc07 — pausing a single download requests the pause and the row shows the paused (play) icon. */
    @Test
    fun pauseSingleDownload() {
        pauseSingleTransfer(MegaTransfer.TYPE_DOWNLOAD)
    }

    /** tc08 — pausing a single upload requests the pause and the row shows the paused (play) icon. */
    @Test
    fun pauseSingleUpload() {
        pauseSingleTransfer(MegaTransfer.TYPE_UPLOAD)
    }

    /** tc09 — pause-all reflects the global paused state, and cancel-all clears every active row. */
    @Test
    fun pauseThenCancelAllTransfers() {
        openTransferManagerWith {
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
            startActiveTransfer(MegaTransfer.TYPE_DOWNLOAD, tag = 2, uniqueId = 2L, fileName = FILE_B)
        }
        awaitActiveRow(FILE_A)
        awaitActiveRow(FILE_B)

        // Pause all: the top-bar Pause action turns into a Resume action (global paused reflected).
        click(PAUSE_ACTION_TAG, "pause-all-action")
        awaitObject(By.res(RESUME_ACTION_TAG), LOAD_TIMEOUT, "resume-all-action")
        awaitInvocations("pauseTransfers") { fakeMegaApi.invocations.filter { it.methodName == "pauseTransfers" } }

        // Cancel all: More -> Cancel all transfers -> confirm; every active row disappears.
        click(MORE_ACTION_TAG, "more-action")
        clickText(targetContext.getString(R.string.menu_cancel_all_transfers), "cancel-all-action")
        clickText(targetContext.getString(R.string.cancel_all_action), "cancel-all-confirm")

        awaitGone(By.res(ACTIVE_TRANSFER_NAME_TAG).text(FILE_A), "cancelled-upload-row")
        awaitGone(By.res(ACTIVE_TRANSFER_NAME_TAG).text(FILE_B), "cancelled-download-row")
        awaitInvocations("cancelTransfers") { fakeMegaApi.invocations.filter { it.methodName == "cancelTransfers" } }
    }

    /** tc10 — cancelling a single selected download targets only that transfer. */
    @Test
    fun cancelSingleDownload() {
        cancelSingleTransfer(MegaTransfer.TYPE_DOWNLOAD)
    }

    /** tc11 — cancelling a single selected upload targets only that transfer. */
    @Test
    fun cancelSingleUpload() {
        cancelSingleTransfer(MegaTransfer.TYPE_UPLOAD)
    }

    /** tc12 — a single transfer can be paused and then resumed, reflected by the shown icon. */
    @Test
    fun resumeSingleTransfer() {
        openTransferManagerWith {
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
        }
        awaitActiveRow(FILE_A)

        // Pause: request pause, then simulate the SDK reporting the paused state.
        click(PAUSE_ICON_TAG, "pause-icon")
        updateTransfer(
            MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A,
            state = MegaTransfer.STATE_PAUSED,
        )
        awaitObject(By.res(PLAY_ICON_TAG), LOAD_TIMEOUT, "paused-play-icon")
        awaitInvocations("pauseTransferByTag(1, pause=true)") {
            pauseTransferByTagInvocations(tag = 1, pause = true)
        }

        // Resume: request resume, then simulate the SDK reporting the active state again.
        click(PLAY_ICON_TAG, "play-icon")
        updateTransfer(
            MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A,
            state = MegaTransfer.STATE_ACTIVE,
        )
        awaitObject(By.res(PAUSE_ICON_TAG), LOAD_TIMEOUT, "resumed-pause-icon")
        awaitInvocations("pauseTransferByTag(1, pause=false)") {
            pauseTransferByTagInvocations(tag = 1, pause = false)
        }
    }

    /** tc13 — resume-all clears the global paused state after a pause-all. */
    @Test
    fun resumeAllTransfers() {
        openTransferManagerWith {
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 2, uniqueId = 2L, fileName = FILE_B)
        }
        awaitActiveRow(FILE_A)
        awaitActiveRow(FILE_B)

        click(PAUSE_ACTION_TAG, "pause-all-action")
        awaitObject(By.res(RESUME_ACTION_TAG), LOAD_TIMEOUT, "resume-all-action")

        click(RESUME_ACTION_TAG, "resume-all-action-click")
        awaitObject(By.res(PAUSE_ACTION_TAG), LOAD_TIMEOUT, "pause-all-action-restored")
        assertThat(
            awaitInvocations("pauseTransfers x2", minCount = 2) {
                fakeMegaApi.invocations.filter { it.methodName == "pauseTransfers" }
            },
        ).hasSize(2)
    }

    /** tc14 — a transfer added while others are active joins the Active tab alongside them. */
    @Test
    fun addTransferWhileOthersActive() {
        openTransferManagerWith {
            startActiveTransfer(MegaTransfer.TYPE_UPLOAD, tag = 1, uniqueId = 1L, fileName = FILE_A)
        }
        awaitActiveRow(FILE_A)

        startActiveTransfer(MegaTransfer.TYPE_DOWNLOAD, tag = 2, uniqueId = 2L, fileName = FILE_B)

        awaitActiveRow(FILE_B)
        awaitActiveRow(FILE_A)
    }

    // endregion

    // region shared scenario bodies

    private fun pauseSingleTransfer(type: Int) {
        openTransferManagerWith {
            startActiveTransfer(type, tag = 1, uniqueId = 1L, fileName = FILE_A)
        }
        awaitActiveRow(FILE_A)

        click(PAUSE_ICON_TAG, "pause-icon")
        awaitInvocations("pauseTransferByTag(1, pause=true)") {
            pauseTransferByTagInvocations(tag = 1, pause = true)
        }

        // Simulate the SDK reporting the transfer as paused; the row swaps to the play icon.
        updateTransfer(type, tag = 1, uniqueId = 1L, fileName = FILE_A, state = MegaTransfer.STATE_PAUSED)
        awaitObject(By.res(PLAY_ICON_TAG), LOAD_TIMEOUT, "paused-play-icon")
    }

    private fun cancelSingleTransfer(type: Int) {
        // Two active transfers so selecting one is not "all selected" (which would cancel everything).
        openTransferManagerWith {
            startActiveTransfer(type, tag = 1, uniqueId = 1L, fileName = FILE_A)
            startActiveTransfer(type, tag = 2, uniqueId = 2L, fileName = FILE_B)
        }
        awaitActiveRow(FILE_A)
        awaitActiveRow(FILE_B)

        // Enter selection mode via More -> Select, select the target row, then Cancel selected.
        click(MORE_ACTION_TAG, "more-action")
        clickText(targetContext.getString(sharedR.string.general_select), "select-action")
        click(activeItemTag(tag = 1), "select-target-row")
        click(CANCEL_SELECTED_ACTION_TAG, "cancel-selected-action")
        clickText(targetContext.getString(R.string.button_continue), "cancel-selected-confirm")

        awaitInvocations("cancelTransferByTag(1)") {
            pauseTransferByTagInvocations(tag = 1, pause = null, methodName = "cancelTransferByTag")
        }

        // Simulate the SDK finishing the cancelled transfer; its row disappears, the other remains.
        finishTransfer(
            type, tag = 1, uniqueId = 1L, fileName = FILE_A,
            state = MegaTransfer.STATE_CANCELLED,
            error = MegaError.API_EINCOMPLETE,
        )
        awaitGone(By.res(ACTIVE_TRANSFER_NAME_TAG).text(FILE_A), "cancelled-row")
        awaitActiveRow(FILE_B)
    }

    // endregion

    // region navigation & transfer helpers

    /**
     * Launches the logged-in app, opens the Drive section, runs [produceTransfers] to script
     * transfer activity, then opens the Transfer Manager from the toolbar transfers widget.
     */
    private fun openTransferManagerWith(produceTransfers: () -> Unit) {
        scenario = ActivityScenario.launch(MegaActivity::class.java)

        awaitObject(By.res(DRIVE_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "drive-nav-item")
        device.findObject(By.res(DRIVE_NAV_ITEM_TAG)).click()
        awaitObject(By.res(EMPTY_VIEW_TAG), LOAD_TIMEOUT, "empty-cloud-drive")

        produceTransfers()

        // The toolbar transfers widget appears once there is transfer activity; it opens the manager.
        awaitObject(By.res(TRANSFERS_WIDGET_TAG), LOAD_TIMEOUT, "transfers-widget")
        device.findObject(By.res(TRANSFERS_WIDGET_TAG)).click()
        awaitObject(By.res(TRANSFERS_VIEW_TAG), LOAD_TIMEOUT, "transfers-view")
    }

    private fun openCompletedTab() {
        clickText(targetContext.getString(R.string.title_tab_completed_transfers), "completed-tab")
    }

    private fun startActiveTransfer(type: Int, tag: Int, uniqueId: Long, fileName: String) {
        emit(GlobalTransfer.OnTransferStart(transfer(type, tag, uniqueId, fileName)))
        emit(
            GlobalTransfer.OnTransferUpdate(
                transfer(type, tag, uniqueId, fileName, transferredBytes = FILE_BYTES / 4),
            ),
        )
    }

    private fun updateTransfer(type: Int, tag: Int, uniqueId: Long, fileName: String, state: Int) {
        emit(
            GlobalTransfer.OnTransferUpdate(
                transfer(
                    type, tag, uniqueId, fileName,
                    state = state,
                    transferredBytes = FILE_BYTES / 2,
                ),
            ),
        )
    }

    private fun finishTransfer(
        type: Int,
        tag: Int,
        uniqueId: Long,
        fileName: String,
        state: Int = MegaTransfer.STATE_COMPLETED,
        error: Int = MegaError.API_OK,
    ) {
        emit(
            GlobalTransfer.OnTransferFinish(
                transfer(
                    type, tag, uniqueId, fileName,
                    state = state,
                    isFinished = true,
                    transferredBytes = FILE_BYTES,
                ),
                StubMegaError(error),
            ),
        )
    }

    private fun transfer(
        type: Int,
        tag: Int,
        uniqueId: Long,
        fileName: String,
        state: Int = MegaTransfer.STATE_ACTIVE,
        isFinished: Boolean = false,
        transferredBytes: Long = 0L,
    ) = StubMegaTransfer(
        type = type,
        tag = tag,
        uniqueId = uniqueId,
        fileName = fileName,
        path = "/storage/emulated/0/$fileName",
        parentHandle = rootHandle,
        transferredBytes = transferredBytes,
        totalBytes = FILE_BYTES,
        state = state,
        isFinished = isFinished,
    )

    private fun emit(event: GlobalTransfer) = runBlocking { fakeMegaApi.emitGlobalTransfer(event) }

    private fun pauseTransferByTagInvocations(tag: Int, pause: Boolean?, methodName: String = "pauseTransferByTag") =
        fakeMegaApi.invocations.filter {
            it.methodName == methodName &&
                    it.arguments.getOrNull(0) == tag &&
                    (pause == null || it.arguments.getOrNull(1) == pause)
        }

    /**
     * Gateway commands are dispatched from a `viewModelScope.launch`, so their invocations are not
     * recorded synchronously after the UI action that triggers them. Poll [supplier] until at least
     * [minCount] invocations match, or fail with what was actually recorded.
     */
    private fun <T> awaitInvocations(
        name: String,
        minCount: Int = 1,
        timeoutMs: Long = LOAD_TIMEOUT,
        supplier: () -> List<T>,
    ): List<T> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.size >= minCount) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${timeoutMs}ms waiting for $minCount invocation(s) of $name; " +
                    "recorded methods=${fakeMegaApi.invocations.map { it.methodName }}",
        )
    }

    // endregion

    // region UiAutomator helpers

    private fun awaitActiveRow(fileName: String) =
        awaitObject(By.res(ACTIVE_TRANSFER_NAME_TAG).text(fileName), LOAD_TIMEOUT, "active-row-$fileName")

    private fun awaitCompletedRow(fileName: String) =
        awaitObject(By.res(COMPLETED_TRANSFER_NAME_TAG).text(fileName), LOAD_TIMEOUT, "completed-row-$fileName")

    private fun click(tag: String, name: String) {
        awaitObject(By.res(tag), LOAD_TIMEOUT, name)
        device.findObject(By.res(tag)).click()
    }

    private fun clickText(text: String, name: String) {
        awaitObject(By.text(text), LOAD_TIMEOUT, name)
        device.findObject(By.text(text)).click()
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
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to disappear; hierarchy in logcat tag UiDump",
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
        const val POLL_INTERVAL_MS = 100L

        const val FILE_A = "transfer_a.bin"
        const val FILE_B = "transfer_b.bin"
        const val FILE_BYTES = 100L * 1024 * 1024

        /** Main navigation item test tag for the Drive/Sync section. */
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"

        /** Mirrors the internal EMPTY_VIEW_TAG of the Cloud Drive empty state. */
        const val EMPTY_VIEW_TAG = "cloud_drive_empty_view:empty_state"

        /** Mirrors the internal TAG_TRANSFERS_WIDGET of the toolbar transfers widget. */
        const val TRANSFERS_WIDGET_TAG = "transfers_widget_view:button:floating_button"

        /** Mirrors TEST_TAG_TRANSFERS_VIEW of the Transfers screen. */
        const val TRANSFERS_VIEW_TAG = "transfers_view"

        /** Mirrors TEST_TAG_ACTIVE_TRANSFER_NAME. */
        const val ACTIVE_TRANSFER_NAME_TAG = "transfers_view:tab_active:transfer_name"

        /** Mirrors TEST_TAG_ACTIVE_TRANSFER_ITEM (per-row tag is suffixed with the transfer tag). */
        const val ACTIVE_TRANSFER_ITEM_TAG = "transfers_view:tab_active:transfer_item"

        /** Mirrors TEST_TAG_PAUSE_ICON / TEST_TAG_PLAY_ICON of an active transfer row. */
        const val PAUSE_ICON_TAG = "transfers_view:tab_active:transfer_item:pause_icon"
        const val PLAY_ICON_TAG = "transfers_view:tab_active:transfer_item:play_icon"

        /** Mirrors TEST_TAG_COMPLETED_TRANSFER_ITEM / _NAME. */
        const val COMPLETED_TRANSFER_ITEM_TAG = "transfers_view:tab_completed:transfer_item"
        const val COMPLETED_TRANSFER_NAME_TAG = "transfers_view:tab_completed:transfer_name"

        /** Top-bar action tags from TransferMenuAction. */
        const val PAUSE_ACTION_TAG = "transfers_view:action_pause_transfers"
        const val RESUME_ACTION_TAG = "transfers_view:action_resume_transfers"
        const val MORE_ACTION_TAG = "transfers_view:action_more"
        const val CANCEL_SELECTED_ACTION_TAG = "transfers_view:action_cancel_selected"
        const val CLEAR_SELECTED_ACTION_TAG = "transfers_view:action_clear_selected"

        fun activeItemTag(tag: Int) = "${ACTIVE_TRANSFER_ITEM_TAG}_$tag"
    }
}
