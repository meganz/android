package mega.privacy.android.app

import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.NameCollisionFixtures.HOME_NAV_ITEM_TAG
import mega.privacy.android.app.NameCollisionFixtures.LAUNCH_ID
import mega.privacy.android.app.NameCollisionFixtures.LAUNCH_TIMEOUT
import mega.privacy.android.app.NameCollisionFixtures.LOAD_TIMEOUT
import mega.privacy.android.app.NameCollisionFixtures.NOTES
import mega.privacy.android.app.NameCollisionFixtures.PHOTOS
import mega.privacy.android.app.NameCollisionFixtures.REPORT
import mega.privacy.android.app.NameCollisionFixtures.TARGET_FOLDER_HANDLE
import mega.privacy.android.app.NameCollisionFixtures.VIDEOS
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
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
import javax.inject.Inject

/**
 * Full-app instrumented tests for the Compose name-collision screen in `:shared:nodes`.
 *
 * The screen has no production caller yet, so each test stores the collision(s) in the
 * [NameCollisionLaunchSourceHolder] under a launch id and opens [MegaActivity] straight on the
 * matching [NameCollisionNavKey], as a caller will. Everything below the screen is real: the
 * ViewModel, the collision, copy and move use cases and the node repository run against the
 * `:data-test` fake SDK gateway, whose node tree is seeded with a source folder and a target
 * folder holding same-named files and folders (see [NameCollisionFixtures]).
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
        NameCollisionFixtures.seedNodeTree(fakeMegaApi)
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

        fakeMegaApi.awaitCopy(REPORT, newName = null)
        assertThat(navigationResultManager.awaitOutcome())
            .isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        device.awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
        device.awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-after-copy")
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
        device.awaitObject(
            By.res(RENAME_CARD_TAG).hasDescendant(By.text(REPORT.renameName)),
            LOAD_TIMEOUT,
            "rename-card",
        )

        device.findObject(By.res(RENAME_BUTTON_TAG)).click()

        fakeMegaApi.awaitInvocation("moveNode") { arguments ->
            (arguments[0] as MegaNode).handle == REPORT.sourceHandle &&
                    (arguments[1] as MegaNode).handle == TARGET_FOLDER_HANDLE &&
                    arguments[2] == REPORT.renameName
        }
        // Renaming keeps both files, so the existing one must not go to the rubbish bin.
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("moveNodeToRubbishBin")
        assertThat(navigationResultManager.awaitOutcome())
            .isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        device.awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
    }

    @Test
    fun skippingTheOnlyCollisionClosesTheScreenWithoutCopying() {
        launchScreen(NameCollisionArgs.Single(REPORT.collision(), isFolderUploadContext = false))

        awaitCollisionPresented(REPORT)
        device.findObject(By.res(CANCEL_BUTTON_TAG)).click()

        device.awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
        device.awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-after-skip")
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("copyNode")
        assertThat(navigationResultManager.currentOutcome()).isNull()
    }

    @Test
    fun applyToAllCopiesEveryFileThenPresentsTheFirstFolder() {
        launchScreen(NameCollisionFixtures.mixedBatch())

        // Files are presented first, with the other file counted as pending.
        awaitCollisionPresented(REPORT)
        device.scrollUntilObject(applyForAllRow(pendingCount = 1), "apply-for-all-one-file")

        device.findObject(By.res(APPLY_FOR_ALL_CHECKBOX_TAG)).click()
        device.findObject(By.res(REPLACE_BUTTON_TAG)).click()

        // Both files are copied in one batch...
        fakeMegaApi.awaitCopy(REPORT, newName = null)
        fakeMegaApi.awaitCopy(NOTES, newName = null)
        assertThat(fakeMegaApi.invocations.count { it.methodName == "copyNode" }).isEqualTo(2)

        // ...and the folders are left for a choice of their own, starting with the first one.
        awaitCollisionPresented(PHOTOS)
        device.scrollUntilObject(applyForAllRow(pendingCount = 1), "apply-for-all-one-folder")
        assertThat(navigationResultManager.currentOutcome()).isNull()
    }

    @Test
    fun skippingAllFilesLeavesTheFoldersToResolve() {
        launchScreen(NameCollisionFixtures.mixedBatch())

        awaitCollisionPresented(REPORT)
        device.scrollUntilObject(applyForAllRow(pendingCount = 1), "apply-for-all-one-file")
        device.findObject(By.res(APPLY_FOR_ALL_CHECKBOX_TAG)).click()
        device.findObject(By.res(CANCEL_BUTTON_TAG)).click()

        awaitCollisionPresented(PHOTOS)
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("copyNode")
        assertThat(navigationResultManager.currentOutcome()).isNull()
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
        fakeMegaApi.awaitCopy(PHOTOS, newName = null)
        awaitCollisionPresented(VIDEOS)
        assertThat(device.hasObject(By.res(APPLY_FOR_ALL_TAG))).isFalse()
        assertThat(navigationResultManager.currentOutcome()).isNull()

        device.findObject(By.res(REPLACE_BUTTON_TAG)).click()

        fakeMegaApi.awaitCopy(VIDEOS, newName = null)
        assertThat(navigationResultManager.awaitOutcome())
            .isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        device.awaitGone(By.res(SCREEN_TAG), "collision-screen-closed")
        // The outcome closes the screen exactly once: the app must still be showing its home.
        device.awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-after-merge")
    }

    private fun launchScreen(args: NameCollisionArgs) {
        launchSourceHolder.put(LAUNCH_ID, args)
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(NameCollisionNavKey(LAUNCH_ID)),
        )
        ActivityScenario.launch<MegaActivity>(intent)
        device.awaitObject(By.res(SCREEN_TAG), LAUNCH_TIMEOUT, "collision-screen")
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
        device.awaitObject(
            By.res(ALREADY_EXISTS_TAG).text(alreadyExists),
            LOAD_TIMEOUT,
            "already-exists-${item.name}",
        )
        device.awaitObject(
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

    /** The "already exists" string wraps the name in [B]…[/B] markers the screen renders as a span. */
    private fun String.stripSpanMarkers() = replace(SPAN_MARKER_REGEX, "")

    private companion object {
        val SPAN_MARKER_REGEX = "\\[/?B]".toRegex()

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
