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
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.destination.InviteFriendsNavKey
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for AND-24467 — inviting friends from the achievements flow routes to
 * the Compose invite-contacts screen instead of the legacy `InviteContactActivity`.
 *
 * The achievements invite-friends screen ([InviteFriendsNavKey]) is pushed onto the production
 * navigation graph through the app's own [NavigationEventQueue] — the same seam production code
 * navigates through — because the migrated key is a dormant destination with no in-app caller yet
 * (the achievements overview is still served by a legacy shim). It is `@Serializable` only, so it
 * cannot travel through the Parcelable-gated `getIntentWithExtraDestinations` intent seam; emitting
 * it on the queue drives the real graph all the same.
 *
 * With [ApiFeatures.ContactComposeFeature] forced on, tapping the invite button routes to the
 * Compose invite-contacts screen (feature/contact, `InviteContactEntry`); with the flag off the same
 * route lands on the legacy `InviteContactActivity` instead — so the flag is what this change turns.
 * Arrival on the `invite_contacts_screen` root is the visible proof of the routing.
 *
 * Built on the `:data-test` fake-SDK framework following [MenuHiddenSectionsTest]: the whole app runs
 * as in production with only the SDK gateways faked and the flag forced through
 * [FakeFeatureFlagValueProvider]. The UI is driven with UiAutomator; Compose test tags are matched as
 * resource ids because MegaActivity sets `testTagsAsResourceId = true`. The whole journey runs against
 * a single activity launch so it records cleanly as an MR demo clip.
 */
@HiltAndroidTest
class AchievementsInviteRoutingTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    @Inject
    lateinit var navigationEventQueue: NavigationEventQueue

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

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // The invite route lands on the Compose invite-contacts screen only when this flag is on;
        // with it off the same route opens the legacy InviteContactActivity.
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        // Persist a logged-in session through the app's real credentials path; the one-time
        // customise-navigation onboarding tooltip is marked as already shown so its popup cannot
        // intercept the taps this test makes.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        fakeFlags.clear()
    }

    @Test
    fun invitingFromAchievementsRoutesToTheComposeInviteContactsScreen() {
        ActivityScenario.launch(MegaActivity::class.java)

        // Wait until the logged-in home shell is up before pushing the achievements destination.
        awaitObject(By.res(HOME_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "home-nav-item")

        // Reach the achievements invite-friends screen through the production navigation seam, the
        // same queue in-app callers navigate through, granting the 20 GB invite storage bonus.
        runBlocking {
            navigationEventQueue.emit(InviteFriendsNavKey(grantStorageInBytes = GRANT_STORAGE_BYTES))
        }

        // The invite-friends screen renders with its invite button.
        awaitObject(By.res(INVITE_BUTTON_TAG), LAUNCH_TIMEOUT, "invite-contacts-button")
        assertThat(device.hasObject(By.res(INVITE_BUTTON_TAG))).isTrue()
        Thread.sleep(HOLD_MS)

        // Tapping invite routes onward through the migrated destination.
        device.findObject(By.res(INVITE_BUTTON_TAG)).click()

        // Arrival on the Compose invite-contacts screen proves the routing: with the flag on this is
        // the feature/contact screen, not the legacy InviteContactActivity.
        awaitObject(By.res(INVITE_CONTACTS_SCREEN_TAG), LAUNCH_TIMEOUT, "invite-contacts-screen")
        Thread.sleep(FINAL_HOLD_MS)
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

        /** Demo-clip holds so a human watching the run registers each state. */
        const val HOLD_MS = 2_000L
        const val FINAL_HOLD_MS = 2_000L

        /** 20 GB invite storage bonus carried by the achievements invite-friends key. */
        const val GRANT_STORAGE_BYTES = 20_000_000_000L

        /** Main navigation Home item tag, derived from its NavKey class name. */
        const val HOME_NAV_ITEM_TAG = "main_navigation:navigation_item_Home"

        /** Mirror InviteFriendsViewTestTags.INVITE_CONTACTS_BUTTON. */
        const val INVITE_BUTTON_TAG = "invite_friends_view:invite_contacts_button"

        /** Mirror INVITE_CONTACTS_SCREEN_TAG of the feature/contact invite-contacts screen. */
        const val INVITE_CONTACTS_SCREEN_TAG = "invite_contacts_screen"
    }
}
