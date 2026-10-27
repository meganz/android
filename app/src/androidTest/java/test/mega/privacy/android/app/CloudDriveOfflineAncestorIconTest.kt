package mega.privacy.android.app

import android.graphics.Rect
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for SAO-2027 / T21460436: in Cloud Drive, a folder shows the offline
 * icon only when the user made it, or a folder above it, available offline.
 *
 * Making a sub-folder available offline also stores every folder above it, so that the offline
 * files mirror the cloud folder structure on disk. The test seeds the cloud tree
 * `SAO-2027/2024/Holiday` and the offline rows that the app stores when Holiday is made available
 * offline: Holiday is the offline root, and SAO-2027 and 2024 are stored only as its ancestors.
 * The journey opens each folder level in Cloud Drive and checks the offline icon of the folder row
 * that the level shows.
 *
 * Offline rows are seeded through [NodeRepository.saveOfflineNodeInformation], the production
 * persistence path, after [NodeRepository.clearOffline] removes rows left by earlier runs. Built
 * on the `:data-test` fake-SDK framework like [OfflineRemovalMenuTest]. The `hold` calls pause
 * the journey at each state so that a screen recording for the MR shows every state.
 */
@HiltAndroidTest
class CloudDriveOfflineAncestorIconTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var nodeRepository: NodeRepository

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

        TestAppBoot.runCoreInitializers()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        runBlocking { nodeRepository.clearOffline() }
    }

    @Test
    fun onlyTheFolderMadeAvailableOfflineShowsTheOfflineIcon() {
        seedCloudTree()
        seedOfflineRowsForHoliday()

        launchCloudDrive()
        hold(CAMERA_HOLD_LONG_MS)
        assertOfflineIcon(TOP_FOLDER_NAME, expected = false)

        openFolder(TOP_FOLDER_NAME, childName = YEAR_FOLDER_NAME)
        hold(CAMERA_HOLD_LONG_MS)
        assertOfflineIcon(YEAR_FOLDER_NAME, expected = false)

        openFolder(YEAR_FOLDER_NAME, childName = HOLIDAY_FOLDER_NAME)
        hold(CAMERA_HOLD_LONG_MS)
        assertOfflineIcon(HOLIDAY_FOLDER_NAME, expected = true)
    }

    // region seeding helpers

    private fun seedCloudTree() {
        addFolder(TOP_FOLDER_HANDLE, TOP_FOLDER_NAME, parentHandle = rootHandle)
        addFolder(YEAR_FOLDER_HANDLE, YEAR_FOLDER_NAME, parentHandle = TOP_FOLDER_HANDLE)
        addFolder(HOLIDAY_FOLDER_HANDLE, HOLIDAY_FOLDER_NAME, parentHandle = YEAR_FOLDER_HANDLE)
        PHOTO_HANDLES.zip(PHOTO_NAMES).forEach { (handle, name) ->
            fakeMegaApi.nodeTree.addNode(
                StubMegaNode(
                    handle = handle,
                    name = name,
                    parentHandle = HOLIDAY_FOLDER_HANDLE,
                    isFolder = false,
                    size = PHOTO_SIZE,
                    modificationTime = System.currentTimeMillis() / 1000,
                    owner = fakeMegaApi.account.myUserHandle,
                ),
                parentHandle = HOLIDAY_FOLDER_HANDLE,
            )
        }
    }

    private fun addFolder(handle: Long, name: String, parentHandle: Long) {
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = handle,
                name = name,
                parentHandle = parentHandle,
                isFolder = true,
                owner = fakeMegaApi.account.myUserHandle,
            ),
            parentHandle = parentHandle,
        )
    }

    /**
     * Stores the offline rows that making Holiday available offline produces: the two folders
     * above it as ancestors, Holiday as the offline root, and its photos below it.
     */
    private fun seedOfflineRowsForHoliday() = runBlocking {
        nodeRepository.clearOffline()
        val topRowId = saveOfflineRow(
            TOP_FOLDER_HANDLE, TOP_FOLDER_NAME, "/", isFolder = true,
            parentRowId = null, isOfflineRoot = false,
        )
        val yearRowId = saveOfflineRow(
            YEAR_FOLDER_HANDLE, YEAR_FOLDER_NAME, "/$TOP_FOLDER_NAME/", isFolder = true,
            parentRowId = topRowId, isOfflineRoot = false,
        )
        val holidayRowId = saveOfflineRow(
            HOLIDAY_FOLDER_HANDLE, HOLIDAY_FOLDER_NAME, "/$TOP_FOLDER_NAME/$YEAR_FOLDER_NAME/",
            isFolder = true, parentRowId = yearRowId, isOfflineRoot = true,
        )
        PHOTO_HANDLES.zip(PHOTO_NAMES).forEach { (handle, name) ->
            saveOfflineRow(
                handle, name, "/$TOP_FOLDER_NAME/$YEAR_FOLDER_NAME/$HOLIDAY_FOLDER_NAME/",
                isFolder = false, parentRowId = holidayRowId, isOfflineRoot = false,
            )
        }
    }

    private suspend fun saveOfflineRow(
        handle: Long,
        name: String,
        path: String,
        isFolder: Boolean,
        parentRowId: Long?,
        isOfflineRoot: Boolean,
    ): Long = nodeRepository.saveOfflineNodeInformation(
        offlineNodeInformation = OtherOfflineNodeInformation(
            id = -1,
            path = path,
            name = name,
            handle = handle.toString(),
            isFolder = isFolder,
            lastModifiedTime = null,
            parentId = parentRowId?.toInt() ?: -1,
        ),
        parentOfflineInformationId = parentRowId,
        isOfflineRoot = isOfflineRoot,
    )

    // endregion

    // region navigation helpers

    /** Launches the logged-in app and opens the Cloud Drive section. */
    private fun launchCloudDrive() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        click(By.res(DRIVE_NAV_ITEM_TAG), "drive-nav-item")
        awaitObject(titleOf(TOP_FOLDER_NAME), "top-folder-row")
    }

    private fun openFolder(name: String, childName: String) {
        click(titleOf(name), "$name-row")
        awaitObject(titleOf(childName), "$childName-row")
    }

    // endregion

    // region assertions

    /**
     * Checks whether the row titled [name] shows the offline icon. Rows carry no per-row tag, and
     * the offline icon sits on the subtitle line below the title. An icon belongs to the row of
     * the nearest title above it.
     */
    private fun assertOfflineIcon(name: String, expected: Boolean) {
        val titles = device.findObjects(By.res(NODE_TITLE_TAG)).map { it.text to it.visibleBounds }
        val rowTitle = titles.firstOrNull { it.first == name }
            ?: run {
                dumpHierarchy("$name-row")
                throw AssertionError("No row titled $name")
            }
        val hasIcon = device.findObjects(By.res(OFFLINE_ICON_TAG)).any { icon ->
            ownerTitle(icon.visibleBounds, titles) == rowTitle.first
        }
        if (hasIcon != expected) dumpHierarchy("$name-offline-icon")
        assertWithMessage("Offline icon on the $name row").that(hasIcon).isEqualTo(expected)
    }

    private fun ownerTitle(iconBounds: Rect, titles: List<Pair<String, Rect>>) =
        titles.filter { it.second.top <= iconBounds.centerY() }
            .maxByOrNull { it.second.top }
            ?.first

    // endregion

    // region UiAutomator helpers

    private fun titleOf(name: String): BySelector = By.res(NODE_TITLE_TAG).text(name)

    private fun hold(durationMs: Long) = Thread.sleep(durationMs)

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, name)
        device.findObject(selector).click()
    }

    private fun awaitObject(selector: BySelector, name: String) {
        if (device.wait(Until.hasObject(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
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
        const val CAMERA_HOLD_LONG_MS = 2_000L

        const val TOP_FOLDER_HANDLE = 200L
        const val YEAR_FOLDER_HANDLE = 201L
        const val HOLIDAY_FOLDER_HANDLE = 202L
        const val TOP_FOLDER_NAME = "SAO-2027"
        const val YEAR_FOLDER_NAME = "2024"
        const val HOLIDAY_FOLDER_NAME = "Holiday"
        val PHOTO_HANDLES = listOf(203L, 204L)
        val PHOTO_NAMES = listOf("photo1.jpg", "photo2.jpg")
        const val PHOTO_SIZE = 1_024L

        const val DRIVE_NAV_ITEM_TAG = "main_navigation:navigation_item_DriveSyncNavKey"
        const val NODE_TITLE_TAG = "node_list_view_item:title"
        const val OFFLINE_ICON_TAG = "node_list_view_item:offline_icon"
    }
}
