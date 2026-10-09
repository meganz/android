package mega.privacy.android.app

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
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.state.childrenMatching
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.preference.FolderPreference
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.SetCloudSortOrder
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.folderpreference.SetFolderPreferenceUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.node.HandleToBase64UseCase
import mega.privacy.android.domain.usecase.setting.SetSortingPreferenceUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaSearchFilter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Instrumented cover for the image viewer following a folder's own sort order, built on the
 * `:data-test` fake-SDK framework and following [ImagePreviewFileInfoTest]: the whole app runs as
 * in production with only the SDK gateways faked, and the UI is driven with UiAutomator.
 *
 * Per-folder sorting is on and the folder is sorted by size while the global cloud order is by
 * name, so the two orders disagree. The fake `getChildren` ignores its order argument, so it is
 * stubbed here to sort like the SDK for the two orders this journey uses.
 *
 * The viewer's name and position labels are matched by text: their test tags sit on a layout
 * that does not expose the text.
 *
 * The holds keep the journey watchable when it is screen-recorded for the MR.
 */
@HiltAndroidTest
class ImagePreviewFolderSortOrderTest {

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
    lateinit var setSortingPreferenceUseCase: SetSortingPreferenceUseCase

    @Inject
    lateinit var setCloudSortOrder: SetCloudSortOrder

    @Inject
    lateinit var setFolderPreferenceUseCase: SetFolderPreferenceUseCase

    @Inject
    lateinit var handleToBase64UseCase: HandleToBase64UseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

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
            instrumentation.targetContext.packageName,
            android.Manifest.permission.POST_NOTIFICATIONS,
        )

        TestAppBoot.runCoreInitializers()

        fakeFlags.set(ApiFeatures.SortingAndViewMode, enabled = true)
        fakeMegaApi.stub(MegaApiGateway::getChildren) { arguments ->
            val children = fakeMegaApi.nodeTree.childrenMatching(arguments[0] as MegaSearchFilter)
            when (arguments[1] as Int) {
                MegaApiJava.ORDER_SIZE_DESC -> children.sortedByDescending { it.size }
                MegaApiJava.ORDER_DEFAULT_ASC -> children.sortedWith(
                    compareByDescending<MegaNode> { it.isFolder }.thenBy { it.name }
                )

                else -> children
            }
        }

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        fakeFlags.clear()
    }

    /**
     * With per-folder sorting on, opening the first image of a folder sorted by size starts the
     * viewer at that image and swiping follows the folder's order, not the global name order.
     */
    @Test
    fun imagePreviewFollowsThePerFolderSortOrder() {
        seedFolderWithImages()
        runBlocking {
            setSortingPreferenceUseCase(SortingPreference.PerFolder)
            setCloudSortOrder(SortOrder.ORDER_DEFAULT_ASC)
            setFolderPreferenceUseCase(
                FolderPreference(
                    folderKey = handleToBase64UseCase(FOLDER_HANDLE),
                    sortOrder = SortOrder.ORDER_SIZE_DESC,
                    viewType = ViewType.LIST,
                )
            )
        }

        launchCloudDrive()
        click(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), "folder-row")
        awaitObject(By.res(NODE_TITLE_TAG).text(IMAGE_B), LOAD_TIMEOUT, "folder-rows")
        assertThat(visibleRowTitles()).containsExactly(IMAGE_C, IMAGE_A, IMAGE_B).inOrder()
        Thread.sleep(HOLD_MS)

        click(By.res(NODE_TITLE_TAG).text(IMAGE_C), "open-first-image")
        awaitObject(By.text(IMAGE_C), LOAD_TIMEOUT, "viewer-first-image")
        Thread.sleep(HOLD_MS)
        awaitObject(By.text(position(1)), SHORT_TIMEOUT, "viewer-position-1")

        swipeToNextImage()
        awaitObject(By.text(IMAGE_A), SHORT_TIMEOUT, "viewer-second-image")
        awaitObject(By.text(position(2)), SHORT_TIMEOUT, "viewer-position-2")
        Thread.sleep(SHORT_HOLD_MS)

        swipeToNextImage()
        awaitObject(By.text(IMAGE_B), SHORT_TIMEOUT, "viewer-third-image")
        awaitObject(By.text(position(3)), SHORT_TIMEOUT, "viewer-position-3")
        Thread.sleep(HOLD_MS)
    }

    private fun position(index: Int): String =
        instrumentation.targetContext.getString(R.string.wizard_steps_indicator, index, IMAGE_COUNT)

    private fun seedFolderWithImages() {
        fakeMegaApi.nodeTree.addNode(
            node(FOLDER_HANDLE, FOLDER_NAME, rootHandle, isFolder = true, size = 0L),
            parentHandle = rootHandle,
        )
        listOf(
            Triple(IMAGE_A_HANDLE, IMAGE_A, 2 * MB),
            Triple(IMAGE_B_HANDLE, IMAGE_B, 1 * MB),
            Triple(IMAGE_C_HANDLE, IMAGE_C, 3 * MB),
        ).forEach { (handle, name, size) ->
            fakeMegaApi.nodeTree.addNode(
                node(handle, name, FOLDER_HANDLE, isFolder = false, size = size),
                parentHandle = FOLDER_HANDLE,
            )
        }
    }

    private fun node(handle: Long, name: String, parentHandle: Long, isFolder: Boolean, size: Long) =
        StubMegaNode(
            handle = handle,
            name = name,
            parentHandle = parentHandle,
            isFolder = isFolder,
            size = size,
            modificationTime = System.currentTimeMillis() / 1000,
            owner = fakeMegaApi.account.myUserHandle,
        )

    private fun launchCloudDrive() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        click(By.res(DRIVE_NAV_ITEM_TAG), "drive-nav-item")
        awaitObject(By.res(NODE_TITLE_TAG).text(FOLDER_NAME), LOAD_TIMEOUT, "cloud-drive-rows")
    }

    private fun visibleRowTitles(): List<String> =
        device.findObjects(By.res(NODE_TITLE_TAG))
            .sortedBy { it.visibleBounds.top }
            .map { it.text }

    private fun swipeToNextImage() {
        val y = device.displayHeight / 2
        device.swipe(device.displayWidth * 4 / 5, y, device.displayWidth / 5, y, SWIPE_STEPS)
    }

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
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
        const val SHORT_TIMEOUT = 5_000L
        const val HOLD_MS = 2_000L
        const val SHORT_HOLD_MS = 1_500L
        const val SWIPE_STEPS = 20
        const val MB = 1_048_576L

        const val FOLDER_HANDLE = 500L
        const val FOLDER_NAME = "Sort demo"

        const val IMAGE_A_HANDLE = 501L
        const val IMAGE_B_HANDLE = 502L
        const val IMAGE_C_HANDLE = 503L
        const val IMAGE_A = "photo-a.jpg"
        const val IMAGE_B = "photo-b.jpg"
        const val IMAGE_C = "photo-c.jpg"
        const val IMAGE_COUNT = 3

        /** Main navigation item test tag for the Drive/Sync section. */
        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"

        /** Mirrors the internal title tag of NodeListViewItem rows. */
        const val NODE_TITLE_TAG = "node_list_view_item:title"
    }
}
