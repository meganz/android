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
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.navigation.destination.NameCollisionNavKey
import mega.privacy.android.shared.nodes.namecollision.NameCollisionArgs
import mega.privacy.android.shared.nodes.namecollision.NameCollisionLaunchSourceHolder
import mega.privacy.android.shared.nodes.namecollision.NameCollisionOutcome
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_APPLY_TO_ALL_CHECKBOX_TAG
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_APPLY_TO_ALL_TAG
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_BODY_TAG
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_BUTTON_GROUP_TAG
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_OPTION_KEEP_BOTH_TAG
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_OPTION_REPLACE_TAG
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_OPTION_SKIP_TAG
import mega.privacy.android.shared.nodes.namecollision.view.DUPLICATE_ITEMS_SCREEN_TAG
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaNode
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import javax.inject.Inject

/**
 * Full-app instrumented tests for the redesigned duplicate items screen in `:shared:nodes`, the
 * one [ApiFeatures.NameCollisionRedesign] puts in place of the name-collision screen.
 *
 * The setup mirrors [NameCollisionScreenTest]: each test stores the collision(s) in the
 * [NameCollisionLaunchSourceHolder] under a launch id and opens [MegaActivity] straight on the
 * matching [NameCollisionNavKey], with the flag forced on through [FakeFeatureFlagValueProvider].
 * Everything below the screen is real and runs against the `:data-test` fake SDK gateway, so the
 * tests assert on the recorded gateway invocations and on the [NameCollisionOutcome] returned to
 * the caller through the [NavigationResultManager].
 */
@HiltAndroidTest
class DuplicateItemsScreenTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

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

        fakeFlags.set(ApiFeatures.NameCollisionRedesign, true)

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
        fakeFlags.clear()
        fakeMegaApi.resetToDefaults()
        navigationResultManager.clearAllResults()
    }

    @Test
    fun copyAndReplaceCopiesTheFileAndReturnsTheCompletedOutcome() {
        launchScreen(NameCollisionArgs.Single(REPORT.collision(), isFolderUploadContext = false))

        awaitCollisionPresented(REPORT)
        // A lone duplicate offers nothing to apply the choice to.
        assertThat(device.hasObject(By.res(DUPLICATE_ITEMS_APPLY_TO_ALL_TAG))).isFalse()
        // Nothing is chosen yet, so the action cannot be confirmed.
        assertThat(device.hasObject(primaryButtonDisabled())).isTrue()

        device.findObject(By.res(DUPLICATE_ITEMS_OPTION_REPLACE_TAG)).click()
        device.awaitGone(primaryButtonDisabled(), "primary-enabled")
        clickPrimary(sharedR.string.general_copy)

        fakeMegaApi.awaitCopy(REPORT, newName = null)
        assertThat(navigationResultManager.awaitOutcome())
            .isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        device.awaitGone(By.res(DUPLICATE_ITEMS_SCREEN_TAG), "duplicate-items-screen-closed")
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
        // The keep-both row previews the name the move will use.
        device.awaitObject(
            By.res(DUPLICATE_ITEMS_OPTION_KEEP_BOTH_TAG).hasDescendant(
                By.text(
                    targetContext.getString(
                        sharedR.string.name_collision_option_keep_both_files_move_subtitle,
                        REPORT.renameName,
                    )
                )
            ),
            LOAD_TIMEOUT,
            "keep-both-row",
        )

        device.findObject(By.res(DUPLICATE_ITEMS_OPTION_KEEP_BOTH_TAG)).click()
        clickPrimary(sharedR.string.general_move)

        fakeMegaApi.awaitInvocation("moveNode") { arguments ->
            (arguments[0] as MegaNode).handle == REPORT.sourceHandle &&
                    (arguments[1] as MegaNode).handle == TARGET_FOLDER_HANDLE &&
                    arguments[2] == REPORT.renameName
        }
        // Renaming keeps both files, so the existing one must not go to the rubbish bin.
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("moveNodeToRubbishBin")
        assertThat(navigationResultManager.awaitOutcome())
            .isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        device.awaitGone(By.res(DUPLICATE_ITEMS_SCREEN_TAG), "duplicate-items-screen-closed")
    }

    @Test
    fun skippingTheOnlyCollisionClosesTheScreenWithoutCopying() {
        launchScreen(NameCollisionArgs.Single(REPORT.collision(), isFolderUploadContext = false))

        awaitCollisionPresented(REPORT)
        device.findObject(By.res(DUPLICATE_ITEMS_OPTION_SKIP_TAG)).click()
        clickPrimary(sharedR.string.general_copy)

        device.awaitGone(By.res(DUPLICATE_ITEMS_SCREEN_TAG), "duplicate-items-screen-closed")
        device.awaitObject(By.res(HOME_NAV_ITEM_TAG), LOAD_TIMEOUT, "home-after-skip")
        assertThat(fakeMegaApi.invocations.map { it.methodName }).doesNotContain("copyNode")
        assertThat(navigationResultManager.currentOutcome()).isNull()
    }

    @Test
    fun applyToAllCopiesEveryFileThenPresentsTheFirstFolder() {
        launchScreen(NameCollisionFixtures.mixedBatch())

        // Files are presented first and counted on their own: the first of the two files.
        awaitCollisionPresented(REPORT)
        device.awaitObject(duplicateCounter(index = 1, count = 2), LOAD_TIMEOUT, "counter-first-file")
        device.scrollUntilObject(applyToAllRow(count = 2), "apply-to-all-two-files")

        device.findObject(By.res(DUPLICATE_ITEMS_APPLY_TO_ALL_CHECKBOX_TAG)).click()
        device.findObject(By.res(DUPLICATE_ITEMS_OPTION_REPLACE_TAG)).click()
        clickPrimary(sharedR.string.general_copy)

        // Both files are copied in one batch...
        fakeMegaApi.awaitCopy(REPORT, newName = null)
        fakeMegaApi.awaitCopy(NOTES, newName = null)
        assertThat(fakeMegaApi.invocations.count { it.methodName == "copyNode" }).isEqualTo(2)

        // ...and the folders are left for a choice of their own, counted afresh from the first one.
        awaitCollisionPresented(PHOTOS)
        assertThat(device.hasObject(duplicateCounter(index = 1, count = 2))).isTrue()
        device.scrollUntilObject(applyToAllRow(count = 2), "apply-to-all-two-folders")
        assertThat(navigationResultManager.currentOutcome()).isNull()
    }

    @Test
    fun skippingAllFilesLeavesTheFoldersToResolve() {
        launchScreen(NameCollisionFixtures.mixedBatch())

        awaitCollisionPresented(REPORT)
        device.scrollUntilObject(applyToAllRow(count = 2), "apply-to-all-two-files")
        device.findObject(By.res(DUPLICATE_ITEMS_APPLY_TO_ALL_CHECKBOX_TAG)).click()
        device.findObject(By.res(DUPLICATE_ITEMS_OPTION_SKIP_TAG)).click()
        clickPrimary(sharedR.string.general_copy)

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
        device.findObject(By.res(DUPLICATE_ITEMS_OPTION_REPLACE_TAG)).click()
        clickPrimary(sharedR.string.general_copy)

        // The first merge just moves the queue on; the screen stays for the last folder, which has
        // nothing left to apply a choice to.
        fakeMegaApi.awaitCopy(PHOTOS, newName = null)
        awaitCollisionPresented(VIDEOS)
        assertThat(device.hasObject(By.res(DUPLICATE_ITEMS_APPLY_TO_ALL_TAG))).isFalse()
        assertThat(navigationResultManager.currentOutcome()).isNull()

        device.findObject(By.res(DUPLICATE_ITEMS_OPTION_REPLACE_TAG)).click()
        clickPrimary(sharedR.string.general_copy)

        fakeMegaApi.awaitCopy(VIDEOS, newName = null)
        assertThat(navigationResultManager.awaitOutcome())
            .isInstanceOf(NameCollisionOutcome.ActionCompleted::class.java)
        device.awaitGone(By.res(DUPLICATE_ITEMS_SCREEN_TAG), "duplicate-items-screen-closed")
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
        device.awaitObject(By.res(DUPLICATE_ITEMS_SCREEN_TAG), LAUNCH_TIMEOUT, "duplicate-items-screen")
    }

    /** The screen presents [item]: the body names it and the title matches its kind. */
    private fun awaitCollisionPresented(item: CollidingItem) {
        device.awaitObject(
            By.res(DUPLICATE_ITEMS_BODY_TAG).text(
                targetContext.getString(sharedR.string.name_collision_already_exists_body, item.name)
            ),
            LOAD_TIMEOUT,
            "body-${item.name}",
        )
        device.awaitObject(
            By.text(
                targetContext.getString(
                    if (item.isFile) {
                        sharedR.string.name_collision_file_exists_title
                    } else {
                        sharedR.string.name_collision_folder_exists_title
                    }
                )
            ),
            LOAD_TIMEOUT,
            "title-${item.name}",
        )
    }

    private fun duplicateCounter(index: Int, count: Int): BySelector = By.text(
        targetContext.getString(sharedR.string.name_collision_duplicate_counter, index, count)
    )

    private fun applyToAllRow(count: Int): BySelector =
        By.res(DUPLICATE_ITEMS_APPLY_TO_ALL_TAG).hasDescendant(
            By.text(
                targetContext.resources.getQuantityString(
                    sharedR.plurals.name_collision_apply_to_all,
                    count,
                    count,
                )
            )
        )

    /**
     * The anchored group holds the always-enabled Cancel button and the primary action, so a
     * disabled node inside it can only be the primary action.
     */
    private fun primaryButtonDisabled(): BySelector =
        By.res(DUPLICATE_ITEMS_BUTTON_GROUP_TAG).hasDescendant(By.enabled(false))

    private fun clickPrimary(label: Int) {
        val selector = By.res(DUPLICATE_ITEMS_BUTTON_GROUP_TAG)
            .hasDescendant(By.text(targetContext.getString(label)))
        device.awaitObject(selector, LOAD_TIMEOUT, "primary-button")
        device.findObject(By.text(targetContext.getString(label))).click()
    }
}
