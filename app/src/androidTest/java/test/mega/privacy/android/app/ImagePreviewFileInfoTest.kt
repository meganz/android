package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
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
 * Instrumented cover for the Image preview entry point into File info, built on the `:data-test`
 * fake-SDK framework and following [FileFolderMenuTest]: the whole app runs as in production with
 * only the SDK gateways faked, the UI is driven with UiAutomator, and Compose test tags are matched
 * as resource ids because both screens set `testTagsAsResourceId = true`.
 *
 * Image preview must reach File info through `MegaNavigator` so that it lands on `FileInfoNavKey`
 * in the single-activity graph, which is what the assertions here are written against.
 */
@HiltAndroidTest
class ImagePreviewFileInfoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val rootHandle get() = fakeMegaApi.nodeTree.rootNode.handle

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

        TestAppBoot.runCoreInitializers()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    /**
     * Opening an image from Cloud Drive and choosing Info from its overflow sheet lands on the
     * revamped File info screen showing the image's name.
     */
    @Test
    fun imagePreviewInfoOpensTheRevampedFileInfoScreen() {
        seedImage()

        launchCloudDrive()
        click(By.res(NODE_TITLE_TAG).text(IMAGE_NAME), "open-image-row")

        click(By.res(IMAGE_PREVIEW_MORE_TAG), "image-preview-more")
        click(By.res(IMAGE_PREVIEW_INFO_OPTION_TAG), "image-preview-info-option")

        awaitObject(By.res(FILE_INFO_NAME_TAG).text(IMAGE_NAME), LOAD_TIMEOUT, "file-info-name")
        awaitObject(By.res(FILE_INFO_SUBTITLE_TAG), LOAD_TIMEOUT, "file-info-subtitle")
        awaitGone(By.res(IMAGE_PREVIEW_MORE_TAG), "image-preview-app-bar")

        device.pressBack()
        awaitObject(By.res(NODE_TITLE_TAG).text(IMAGE_NAME), LOAD_TIMEOUT, "back-to-cloud-drive")
    }

    private fun seedImage(): StubMegaNode {
        val node = StubMegaNode(
            handle = IMAGE_HANDLE,
            name = IMAGE_NAME,
            parentHandle = rootHandle,
            isFolder = false,
            size = IMAGE_SIZE,
            modificationTime = System.currentTimeMillis() / 1000,
            owner = fakeMegaApi.account.myUserHandle,
        )
        fakeMegaApi.nodeTree.addNode(node, parentHandle = rootHandle)
        return node
    }

    private fun launchCloudDrive() {
        ActivityScenario.launch(MegaActivity::class.java)
        click(By.res(DRIVE_NAV_ITEM_TAG), "drive-nav-item")
        awaitObject(By.res(NODE_TITLE_TAG), LOAD_TIMEOUT, "cloud-drive-rows")
    }

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
    }

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to be gone; " +
                    "hierarchy in logcat tag UiDump",
        )
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

    private companion object {
        const val LOAD_TIMEOUT = 30_000L

        const val IMAGE_HANDLE = 300L
        const val IMAGE_NAME = "image-preview-info.jpg"
        const val IMAGE_SIZE = 2_048L

        /** Main navigation item test tag for the Drive/Sync section. */
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"

        /** Mirrors the internal title tag of NodeListViewItem rows. */
        const val NODE_TITLE_TAG = "node_list_view_item:title"

        /** Mirror IMAGE_PREVIEW_APP_BAR_MORE and IMAGE_PREVIEW_BOTTOM_SHEET_OPTION_INFO. */
        const val IMAGE_PREVIEW_MORE_TAG = "image_preview_app_bar:icon_more"
        const val IMAGE_PREVIEW_INFO_OPTION_TAG =
            "image_preview_bottom_sheet_option:menu_action_list_tile_info"

        /** Mirror the tags of the revamped File info screen. */
        const val FILE_INFO_NAME_TAG = "file_info_screen:name"
        const val FILE_INFO_SUBTITLE_TAG = "file_info_screen:subtitle"
    }
}
