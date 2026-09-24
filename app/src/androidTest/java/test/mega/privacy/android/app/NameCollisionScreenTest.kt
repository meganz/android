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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.navigation.destination.NameCollisionNavKey
import mega.privacy.android.shared.nodes.namecollision.NameCollisionArgs
import mega.privacy.android.shared.nodes.namecollision.NameCollisionLaunchSourceHolder
import mega.privacy.android.shared.nodes.namecollision.NameCollisionOutcome
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented tests for the Compose name-collision screen in `:shared:nodes`.
 *
 * The screen has no production caller yet, so each test stores the collision(s) in the
 * [NameCollisionLaunchSourceHolder] under a launch id and opens [MegaActivity] straight on the
 * matching [NameCollisionNavKey], as a caller will. Everything below the screen is real: the
 * ViewModel, the collision, copy and move use cases and the node repository run against the
 * `:data-test` fake SDK gateway, whose node tree is seeded with a source folder and a target
 * folder holding same-named files and folders.
 *
 * The fake gateway only acknowledges `copyNode` and `moveNode`, so the tests assert on the
 * recorded gateway invocations (which node, into which parent, under which name) and on the
 * [NameCollisionOutcome] the screen returns to its caller through the [NavigationResultManager]
 * under [NameCollisionNavKey.RESULT]. The UI is driven with UiAutomator; the screen's Compose test
 * tags are matched as resource ids because MegaActivity sets `testTagsAsResourceId = true`.
 */
@HiltAndroidTest
class NameCollisionScreenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var launchSourceHolder: NameCollisionLaunchSourceHolder

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

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

        fakeMegaApi.resetToDefaults()
        seedNodeTree()
        fakeMegaApi.clearInvocations()
        navigationResultManager.clearAllResults()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        fakeMegaApi.resetToDefaults()
        navigationResultManager.clearAllResults()
    }

    @Test
    fun copyAndReplaceCopiesTheFileAndReturnsTheCompletedOutcome() {
        launchScreen(NameCollisionArgs.Single(REPORT.collision(), isFolderUploadContext = false))

        awaitCollisionPresented(REPORT)
        // A lone collision offers nothing to apply the choice to.
        assertThat(device.hasObject(By.res(APPLY_FOR_ALL_TAG))).isFalse()

        device.findObject(By.res(REPLACE_BUTTON_TAG)).click()

        awaitCopy(REPORT, newName = null)
        assertThat(awaitOutcome()).isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
        awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-after-copy")
    }

    @Test
    fun moveAndRenameMovesTheFileUnderItsRenameName() {
        launchScreen(
            NameCollisionArgs.Single(
                REPORT.collision(type = NodeNameCollisionType.MOVE),
                isFolderUploadContext = false,
            )
        )

        awaitCollisionPresented(REPORT)
        // The rename card previews the name the move will use.
        awaitObject(
            By.res(RENAME_CARD_TAG).hasDescendant(By.text(REPORT.renameName)),
            LOAD_TIMEOUT,
            "rename-card",
        )

        device.findObject(By.res(RENAME_BUTTON_TAG)).click()

        awaitInvocation("moveNode") { arguments ->
            (arguments[0] as MegaNode).handle == REPORT.sourceHandle &&
                    (arguments[1] as MegaNode).handle == TARGET_FOLDER_HANDLE &&
                    arguments[2] == REPORT.renameName
        }
        // Renaming keeps both files, so the existing one must not go to the rubbish bin.
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("moveNodeToRubbishBin")
        assertThat(awaitOutcome()).isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
    }

    @Test
    fun skippingTheOnlyCollisionClosesTheScreenWithoutCopying() {
        launchScreen(NameCollisionArgs.Single(REPORT.collision(), isFolderUploadContext = false))

        awaitCollisionPresented(REPORT)
        device.findObject(By.res(CANCEL_BUTTON_TAG)).click()

        awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
        awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-after-skip")
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("copyNode")
        assertThat(currentOutcome()).isNull()
    }

    @Test
    fun applyToAllCopiesEveryFileThenPresentsTheFirstFolder() {
        launchScreen(mixedBatch())

        // Files are presented first, with the other file counted as pending.
        awaitCollisionPresented(REPORT)
        scrollUntilObject(applyForAllRow(pendingCount = 1), "apply-for-all-one-file")

        device.findObject(By.res(APPLY_FOR_ALL_CHECKBOX_TAG)).click()
        device.findObject(By.res(REPLACE_BUTTON_TAG)).click()

        // Both files are copied in one batch...
        awaitCopy(REPORT, newName = null)
        awaitCopy(NOTES, newName = null)
        assertThat(fakeMegaApi.invocations.count { it.methodName == "copyNode" }).isEqualTo(2)

        // ...and the folders are left for a choice of their own, starting with the first one.
        awaitCollisionPresented(PHOTOS)
        scrollUntilObject(applyForAllRow(pendingCount = 1), "apply-for-all-one-folder")
        assertThat(currentOutcome()).isNull()
    }

    @Test
    fun skippingAllFilesLeavesTheFoldersToResolve() {
        launchScreen(mixedBatch())

        awaitCollisionPresented(REPORT)
        scrollUntilObject(applyForAllRow(pendingCount = 1), "apply-for-all-one-file")
        device.findObject(By.res(APPLY_FOR_ALL_CHECKBOX_TAG)).click()
        device.findObject(By.res(CANCEL_BUTTON_TAG)).click()

        awaitCollisionPresented(PHOTOS)
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("copyNode")
        assertThat(currentOutcome()).isNull()
    }

    @Test
    fun mergingTheFoldersOneByOneCompletesAfterTheLastOne() {
        launchScreen(
            NameCollisionArgs.Multiple(
                collisions = listOf(PHOTOS.collision(), VIDEOS.collision()),
                isFolderUploadContext = false,
            )
        )

        awaitCollisionPresented(PHOTOS)
        device.findObject(By.res(REPLACE_BUTTON_TAG)).click()

        // The first merge just moves the queue on; the screen stays for the last folder.
        awaitCopy(PHOTOS, newName = null)
        awaitCollisionPresented(VIDEOS)
        assertThat(device.hasObject(By.res(APPLY_FOR_ALL_TAG))).isFalse()
        assertThat(currentOutcome()).isNull()

        device.findObject(By.res(REPLACE_BUTTON_TAG)).click()

        awaitCopy(VIDEOS, newName = null)
        assertThat(awaitOutcome()).isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
        // The outcome closes the screen exactly once: the app must still be showing its home.
        awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-after-merge")
    }

    private fun launchScreen(args: NameCollisionArgs) {
        launchSourceHolder.put(LAUNCH_ID, args)
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(NameCollisionNavKey(LAUNCH_ID)),
        )
        ActivityScenario.launch<MegaActivity>(intent)
        awaitObject(By.res(SCREEN_TAG), LAUNCH_TIMEOUT, "collision-screen")
    }

    private fun mixedBatch() = NameCollisionArgs.Multiple(
        collisions = listOf(
            REPORT.collision(),
            PHOTOS.collision(),
            NOTES.collision(),
            VIDEOS.collision(),
        ),
        isFolderUploadContext = false,
    )

    /**
     * Seeds a Cloud Drive holding a source folder and a target folder, where every item of the
     * source already exists by name in the target.
     */
    private fun seedNodeTree() {
        val rootHandle = fakeMegaApi.nodeTree.rootNode.handle
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = SOURCE_FOLDER_HANDLE,
                name = "Source",
                parentHandle = rootHandle,
                isFolder = true,
            ),
            parentHandle = rootHandle,
        )
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = TARGET_FOLDER_HANDLE,
                name = "Target",
                parentHandle = rootHandle,
                isFolder = true,
            ),
            parentHandle = rootHandle,
        )
        ITEMS.forEach { item ->
            fakeMegaApi.nodeTree.addNode(
                item.node(handle = item.sourceHandle, parentHandle = SOURCE_FOLDER_HANDLE),
                parentHandle = SOURCE_FOLDER_HANDLE,
            )
            fakeMegaApi.nodeTree.addNode(
                item.node(handle = item.collisionHandle, parentHandle = TARGET_FOLDER_HANDLE),
                parentHandle = TARGET_FOLDER_HANDLE,
            )
        }
    }

    /**
     * The screen presents [item]: the "already exists" text names it and the existing node's card
     * is on screen.
     */
    private fun awaitCollisionPresented(item: CollidingItem) {
        val alreadyExists = targetContext.getString(
            if (item.isFile) {
                sharedR.string.file_already_exists_in_location
            } else {
                sharedR.string.folder_already_exists_in_location
            },
            item.name,
        ).stripSpanMarkers()
        awaitObject(
            By.res(ALREADY_EXISTS_TAG).text(alreadyExists),
            LOAD_TIMEOUT,
            "already-exists-${item.name}",
        )
        awaitObject(
            By.res(CANCEL_CARD_TAG).hasDescendant(By.text(item.name)),
            LOAD_TIMEOUT,
            "existing-card-${item.name}",
        )
    }

    private fun applyForAllRow(pendingCount: Int): BySelector = By.res(APPLY_FOR_ALL_TAG).hasDescendant(
        By.text(
            targetContext.resources.getQuantityString(
                sharedR.plurals.general_name_collision_file_apply_for_all,
                pendingCount,
                pendingCount,
            )
        )
    )

    private fun awaitCopy(item: CollidingItem, newName: String?) {
        awaitInvocation("copyNode") { arguments ->
            (arguments[0] as MegaNode).handle == item.sourceHandle &&
                    (arguments[1] as MegaNode).handle == TARGET_FOLDER_HANDLE &&
                    arguments[2] == newName
        }
    }

    /**
     * The copy and move commands are dispatched from a `viewModelScope.launch`, so their invocations
     * are not recorded synchronously after the tap that triggered them.
     */
    private fun awaitInvocation(methodName: String, matches: (List<Any?>) -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matched = fakeMegaApi.invocations.any {
                it.methodName == methodName && matches(it.arguments)
            }
            if (matched) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for a matching $methodName; " +
                    "recorded=${fakeMegaApi.invocations.filter { it.methodName == methodName }}"
        )
    }

    private fun awaitOutcome(): NameCollisionOutcome = runBlocking {
        withTimeout(LOAD_TIMEOUT) {
            navigationResultManager
                .monitorResult<NameCollisionOutcome>(NameCollisionNavKey.RESULT)
                .first { it != null }
        }
    }!!

    private fun currentOutcome(): NameCollisionOutcome? = runBlocking {
        navigationResultManager.monitorResult<NameCollisionOutcome>(NameCollisionNavKey.RESULT).first()
    }

    /** The "already exists" string wraps the name in [B]…[/B] markers the screen renders as a span. */
    private fun String.stripSpanMarkers() = replace(SPAN_MARKER_REGEX, "")

    /**
     * The options are laid out in a vertically scrolling column, so on shorter screens the
     * apply-for-all row at the bottom is clipped out of the accessibility hierarchy until the
     * content is scrolled.
     */
    private fun scrollUntilObject(selector: BySelector, name: String) {
        repeat(MAX_SCROLL_ATTEMPTS) {
            if (device.hasObject(selector)) return
            val scrollable = device.findObject(By.scrollable(true)) ?: return@repeat
            scrollable.setGestureMargin(scrollable.visibleBounds.height() / 4)
            scrollable.scroll(Direction.DOWN, SCROLL_PERCENT)
        }
        awaitObject(selector, LOAD_TIMEOUT, name)
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
        )
    }

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to be gone; " +
                    "hierarchy in logcat tag UiDump"
        )
    }

    private fun dumpHierarchy(name: String) {
        val stream = ByteArrayOutputStream()
        device.dumpWindowHierarchy(stream)
        stream.toString("UTF-8").chunked(3000).forEachIndexed { index, chunk ->
            Log.d("UiDump", "[$name#$index] $chunk")
        }
    }

    /**
     * An item of the source folder that already exists by name in the target folder.
     *
     * @property sourceHandle    Handle of the node being copied or moved.
     * @property collisionHandle Handle of the same-named node already in the target folder.
     * @property renameName      Name the screen offers to keep both.
     */
    private data class CollidingItem(
        val sourceHandle: Long,
        val collisionHandle: Long,
        val name: String,
        val isFile: Boolean,
        val renameName: String,
    ) {
        fun node(handle: Long, parentHandle: Long): MegaNode = StubMegaNode(
            handle = handle,
            name = name,
            parentHandle = parentHandle,
            isFolder = !isFile,
            size = if (isFile) FILE_SIZE else 0L,
            creationTime = TIMESTAMP,
            modificationTime = TIMESTAMP,
        )

        fun collision(
            type: NodeNameCollisionType = NodeNameCollisionType.COPY,
        ) = NodeNameCollision.Default(
            collisionHandle = collisionHandle,
            nodeHandle = sourceHandle,
            name = name,
            size = if (isFile) FILE_SIZE else 0L,
            childFolderCount = 0,
            childFileCount = 0,
            lastModified = TIMESTAMP,
            parentHandle = TARGET_FOLDER_HANDLE,
            isFile = isFile,
            type = type,
        )
    }

    private companion object {
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val POLL_INTERVAL_MS = 100L
        const val MAX_SCROLL_ATTEMPTS = 5
        const val SCROLL_PERCENT = 0.8f

        const val LAUNCH_ID = "name-collision-test"
        const val SOURCE_FOLDER_HANDLE = 10L
        const val TARGET_FOLDER_HANDLE = 20L
        const val FILE_SIZE = 2_048L
        const val TIMESTAMP = 1_695_600_000L

        val REPORT = CollidingItem(
            sourceHandle = 101L,
            collisionHandle = 201L,
            name = "report.txt",
            isFile = true,
            renameName = "report (1).txt",
        )
        val NOTES = CollidingItem(
            sourceHandle = 102L,
            collisionHandle = 202L,
            name = "notes.txt",
            isFile = true,
            renameName = "notes (1).txt",
        )
        val PHOTOS = CollidingItem(
            sourceHandle = 111L,
            collisionHandle = 211L,
            name = "Photos",
            isFile = false,
            renameName = "Photos (1)",
        )
        val VIDEOS = CollidingItem(
            sourceHandle = 112L,
            collisionHandle = 212L,
            name = "Videos",
            isFile = false,
            renameName = "Videos (1)",
        )
        val ITEMS = listOf(REPORT, NOTES, PHOTOS, VIDEOS)

        val SPAN_MARKER_REGEX = "\\[/?B]".toRegex()

        const val HOME_NAV_ITEM_TAG = "main_navigation:navigation_item_Home"
        const val SCREEN_TAG = "name_collision_screen:screen"
        const val ALREADY_EXISTS_TAG = "name_collision_screen:already_exists"
        const val REPLACE_BUTTON_TAG = "name_collision_screen:replace_button"
        const val RENAME_CARD_TAG = "name_collision_screen:rename_card"
        const val RENAME_BUTTON_TAG = "name_collision_screen:rename_button"
        const val CANCEL_CARD_TAG = "name_collision_screen:cancel_card"
        const val CANCEL_BUTTON_TAG = "name_collision_screen:cancel_button"
        const val APPLY_FOR_ALL_TAG = "name_collision_screen:apply_for_all"
        const val APPLY_FOR_ALL_CHECKBOX_TAG = "name_collision_screen:apply_for_all_checkbox"
    }
}
