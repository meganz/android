package mega.privacy.android.app

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import androidx.work.Configuration
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.global.initialisation.GlobalInitialiser
import mega.privacy.android.app.initializer.NotificationChannelsInitializer
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaTransfer
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import nz.mega.sdk.MegaCancelToken
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaTransfer
import nz.mega.sdk.MegaTransferListenerInterface
import nz.mega.sdk.MegaUploadOptions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.reflect.KFunction6

/**
 * Example full-app instrumented test built on the `:data-test` fake gateway framework.
 *
 * The whole app runs as in production — real activities, navigation, ViewModels, use cases,
 * repositories, Room persistence and the real UploadsWorker — with only two seams faked:
 * the SDK gateways (see [mega.privacy.android.app.di.FakeSdkGatewayModule]) and the
 * out-of-process system file picker, which is stubbed with Espresso-Intents to return a
 * MediaStore file as if the user had picked it. The scenario:
 *
 * 1. The fake reports an already logged-in account, persisted through the app's own
 *    save-credentials path, with an empty Cloud Drive.
 * 2. The upload is driven from the UI: the empty view's "Add items" button opens the upload
 *    options sheet, "Upload files" launches the (stubbed) picker, and the returned URI flows
 *    through the production pipeline — file preparation, pending transfers, UploadsWorker,
 *    [MegaApiGateway.startUpload] — while the fake reports progress as if a large file were
 *    uploading, which shows in the toolbar transfers widget.
 * 3. The SDK-side effect of the finished upload is simulated (new node in the fake node tree
 *    plus a [GlobalUpdate.OnNodesUpdate]) and the file appears in the Cloud Drive list.
 *
 * UI is driven with UiAutomator rather than a Compose test rule: the compose rule's idle
 * synchronization interferes with the production activity's splash-gated composition, while
 * UiAutomator observes the UI without touching the app's frame clock. Compose test tags are
 * matched as resource ids because MegaActivity sets `testTagsAsResourceId = true`.
 */
@HiltAndroidTest
class CloudDriveUploadTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var globalInitialiser: GlobalInitialiser

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    /** Typed reference because startUpload is overloaded; stubs are shared by method name. */
    private val startUploadRef: KFunction6<MegaApiGateway, String, MegaNode, MegaCancelToken?, MegaUploadOptions, MegaTransferListenerInterface, Unit> =
        MegaApiGateway::startUpload

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    private var pickedFileUri: Uri? = null

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        // The upload flow requests POST_NOTIFICATIONS before opening the picker; pre-granting
        // makes the permission launcher return synchronously instead of showing a dialog.
        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

        // App startup initializers no-op under the Hilt test application (the component does not
        // exist when androidx.startup runs), so replicate the ones the tested flow needs now that
        // the component is available.
        Analytics.initialise(targetContext)

        // Notification channels are required by UploadsWorker's foreground notification.
        EntryPointAccessors.fromApplication(
            targetContext,
            NotificationChannelsInitializer.NotificationChannelsInitializerEntryPoint::class.java,
        ).let { it.getNotificationManager().createNotificationChannelsCompat(it.getChannels().toList()) }

        // Production triggers the app-start initialisers from MegaApplication's process
        // lifecycle observer, which does not exist under the Hilt test application. The
        // transfer-events monitor that persists upload progress is one of them.
        globalInitialiser.onAppStart()

        // The production Application provides the WorkManager configuration; the Hilt test
        // application does not, so initialize it here so UploadsWorker (and the FCM service's
        // injection graph) can resolve WorkManager.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            targetContext,
            Configuration.Builder().setWorkerFactory(workerFactory).build(),
        )

        // The node list is loaded via getChildren(filter, ...); the filter is an opaque SDK
        // object, so resolve children through the fake node tree explicitly. The tree starts
        // with only the root nodes, so Cloud Drive is initially empty.
        fakeMegaApi.stub(MegaApiGateway::getChildren) {
            fakeMegaApi.nodeTree.childrenOf(rootHandle)
        }

        // Persist a logged-in session through the app's real credentials path; the account
        // details come from the fake gateway's logged-in defaults. Account details are fetched
        // eagerly because in production the login flow populates them, and account-detail
        // dependent features (e.g. hidden-nodes gating) wait for a non-empty value.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        pickedFileUri?.let { uri ->
            runCatching { targetContext.contentResolver.delete(uri, null, null) }
        }
    }

    @Test
    fun uploadedFileAppearsInEmptyCloudDrive() {
        // The picker runs in another process (DocumentsUI), so it is the one UI step that is
        // stubbed: the ACTION_OPEN_DOCUMENT intent immediately returns a real MediaStore file,
        // as if the user had picked it. (A file in the app's own private dir would be rejected
        // by the upload pipeline's file preparation, so MediaStore is used instead.)
        val fileUri = insertPickedFileIntoMediaStore().also { pickedFileUri = it }
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
                ActivityResult(Activity.RESULT_OK, Intent().setData(fileUri)),
            )

            ActivityScenario.launch(MegaActivity::class.java)

            // Logged-in home UI is up once the bottom navigation renders; open the Drive section.
            awaitObject(By.res(DRIVE_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "drive-nav-item")
            device.findObject(By.res(DRIVE_NAV_ITEM_TAG)).click()

            awaitObject(By.res(EMPTY_VIEW_TAG), LOAD_TIMEOUT, "empty-cloud-drive")

            // Script the fake to report the upload progressively, as if a large file were
            // being uploaded: start at 0, progress updates, then success.
            fun transferStep(bytes: Long, finished: Boolean = false) = StubMegaTransfer(
                type = MegaTransfer.TYPE_UPLOAD,
                tag = 1,
                uniqueId = 1L,
                fileName = UPLOADED_FILE_NAME,
                path = fileUri.toString(),
                parentHandle = rootHandle,
                transferredBytes = bytes,
                totalBytes = LARGE_FILE_BYTES,
                state = if (finished) MegaTransfer.STATE_COMPLETED else MegaTransfer.STATE_ACTIVE,
                isFinished = finished,
            )
            fakeMegaApi.stubTransferScript(
                startUploadRef,
                steps = (0 until PROGRESS_STEPS).map { step ->
                    transferStep(step * LARGE_FILE_BYTES / PROGRESS_STEPS)
                },
                finalTransfer = transferStep(LARGE_FILE_BYTES, finished = true),
                stepDelayMs = PROGRESS_STEP_DELAY_MS,
            )

            // Drive the upload from the real UI: Add items → Upload files → (stubbed) picker.
            // Everything downstream is production code: file preparation, pending transfers,
            // UploadsWorker, and the SDK gateway call.
            device.findObject(By.res(ADD_ITEMS_BUTTON_TAG)).click()
            awaitObject(By.res(UPLOAD_FILES_OPTION_TAG), LOAD_TIMEOUT, "upload-files-option")
            device.findObject(By.res(UPLOAD_FILES_OPTION_TAG)).click()

            // The transfers widget appears in the toolbar and shows progress while the "large
            // file" uploads.
            awaitObject(By.res(TRANSFERS_WIDGET_TAG), LOAD_TIMEOUT, "transfers-widget-progress")
            assertThat(fakeMegaApi.invocations.filter { it.methodName == "startUpload" })
                .isNotEmpty()

            // Let the scripted transfer run to completion before materializing its result.
            Thread.sleep(PROGRESS_STEPS * PROGRESS_STEP_DELAY_MS + 2_000)

            // Simulate the SDK's post-upload state: the uploaded node exists under the Cloud
            // Drive root and an OnNodesUpdate is broadcast, which the app's node monitoring
            // picks up.
            val uploadedNode = StubMegaNode(
                handle = UPLOADED_NODE_HANDLE,
                name = UPLOADED_FILE_NAME,
                parentHandle = rootHandle,
                size = LARGE_FILE_BYTES,
                changes = MegaNode.CHANGE_TYPE_NEW.toLong(),
            )
            fakeMegaApi.nodeTree.addNode(uploadedNode, parentHandle = rootHandle)
            runBlocking {
                fakeMegaApi.emitGlobalUpdate(GlobalUpdate.OnNodesUpdate(arrayListOf(uploadedNode)))
            }

            // Assert the node-list row itself (tag + text) — a plain text match could be
            // satisfied by the upload-complete heads-up notification instead of the list.
            awaitObject(
                By.res(NODE_TITLE_TAG).text(UPLOADED_FILE_NAME),
                LOAD_TIMEOUT,
                "uploaded-file-row",
            )

            // Hold the final state briefly so a human watching the run can see the result.
            Thread.sleep(4_000)
        } finally {
            Intents.release()
        }
    }

    private fun insertPickedFileIntoMediaStore(): Uri {
        val resolver = targetContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, UPLOADED_FILE_NAME)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
        }
        val uri = requireNotNull(
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values),
        ) { "Could not insert the picked file into MediaStore" }
        resolver.openOutputStream(uri)?.use {
            it.write("hello from the fake gateway framework".toByteArray())
        }
        return uri
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
        const val UPLOADED_FILE_NAME = "hello.txt"
        const val UPLOADED_NODE_HANDLE = 100L

        /** Mirrors the internal EMPTY_VIEW_TAG of the Cloud Drive empty state. */
        const val EMPTY_VIEW_TAG = "cloud_drive_empty_view:empty_state"

        /** Mirrors the internal ADD_ITEMS_BUTTON_TAG of the Cloud Drive empty state. */
        const val ADD_ITEMS_BUTTON_TAG = "cloud_drive_empty_view:add_items_button"

        /** Mirrors the internal upload-files action tag of UploadOptionsBottomSheet. */
        const val UPLOAD_FILES_OPTION_TAG = "home_fab_options_panel:upload_files_action"

        /** Main navigation item test tag for the Drive/Sync section. */
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"

        /** Mirrors the internal TAG_TRANSFERS_WIDGET of the toolbar transfers widget. */
        const val TRANSFERS_WIDGET_TAG = "transfers_widget_view:button:floating_button"

        /** Mirrors the internal TITLE_TAG of NodeListViewItem rows. */
        const val NODE_TITLE_TAG = "node_list_view_item:title"

        const val LARGE_FILE_BYTES = 100L * 1024 * 1024
        const val PROGRESS_STEPS = 10
        const val PROGRESS_STEP_DELAY_MS = 800L
    }
}
