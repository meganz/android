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
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.feature.contact.qrcode.navigation.QrCodeNavKey
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import nz.mega.sdk.MegaRequest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Full-app instrumented test for AND-24906 — the Compose "My QR code" screen in :feature:contact.
 *
 * [QrCodeNavKey] is pushed onto the production navigation graph through the app's own
 * [NavigationEventQueue], the same seam in-app callers navigate through. The destination is dormant
 * (nothing in :app routes to it until MR#3) and the key is `@Serializable` only, so it cannot travel
 * through the Parcelable-gated `getIntentWithExtraDestinations` intent seam — emitting it on the
 * queue drives the real graph all the same. See [AchievementsInviteRoutingTest].
 *
 * The journey walks the full QR lifecycle in one activity launch: generate, copy the contact link,
 * reset, delete back to the empty state, and generate again. The delete → re-create leg is the
 * `QRCodeAvailable ⇄ QRCodeUnavailable` transition the screen's state model turns on.
 *
 * Not covered, both out of process and therefore undrivable through the gateway fakes: the Scan-QR
 * half (`scanBarcode` launches the ML Kit scanner) and save-to-device (`OpenDocumentTree` is the
 * system file picker). Those are covered by the screenshot goldens instead.
 *
 * Built on the `:data-test` fake-SDK framework: the whole app runs as in production with only the
 * SDK gateways faked and [ApiFeatures.ContactComposeFeature] forced on. The UI is driven with
 * UiAutomator; Compose test tags are matched as resource ids because MegaActivity sets
 * `testTagsAsResourceId = true`. The holds keep the run watchable as an MR demo clip.
 */
@HiltAndroidTest
class MyQrCodeJourneyTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

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

        TestAppBoot.runCoreInitializers()

        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        // The contact link is built from the request's node handle, so a fixed handle keeps the
        // rendered link stable across runs instead of the encoded default.
        fakeMegaApi.stubRequest(
            MegaApiGateway::contactLinkCreate,
            request = StubMegaRequest(
                type = MegaRequest.TYPE_CONTACT_LINK_CREATE,
                nodeHandle = CONTACT_LINK_HANDLE,
            ),
        )

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
    fun myQrCodeCreateCopyResetAndDelete() {
        ActivityScenario.launch(MegaActivity::class.java)

        awaitObject(By.res(HOME_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "home-nav-item")

        runBlocking { navigationEventQueue.emit(QrCodeNavKey) }

        // The screen opens on the generating state and settles into the rendered QR code.
        awaitObject(By.res(QR_CODE_SCREEN_TAG), LAUNCH_TIMEOUT, "my-qr-code-screen")
        awaitObject(By.res(QR_CODE_IMAGE_TAG), LOAD_TIMEOUT, "qr-code-image")
        assertThat(device.hasObject(By.res(QR_CODE_LINK_TAG))).isTrue()
        Thread.sleep(HOLD_MS)

        // Tapping the contact link copies it and confirms with a snackbar.
        click(By.res(QR_CODE_LINK_TAG), "contact-link-button")
        awaitObject(By.text(str(sharedR.string.general_link_copied_clipboard)), LOAD_TIMEOUT, "copied-snackbar")
        Thread.sleep(HOLD_MS)

        // The overflow carries the QR-only actions; they exist solely in the available state.
        click(overflowButton(), "overflow-button")
        awaitObject(By.res(RESET_ACTION_TAG), LOAD_TIMEOUT, "reset-action")
        assertThat(device.hasObject(By.res(DELETE_ACTION_TAG))).isTrue()
        Thread.sleep(HOLD_MS)

        // Reset generates a new contact link and confirms with a snackbar.
        click(By.res(RESET_ACTION_TAG), "reset-action")
        awaitObject(By.text(str(sharedR.string.contacts_qr_reset_success)), LOAD_TIMEOUT, "reset-snackbar")
        awaitObject(By.res(QR_CODE_IMAGE_TAG), LOAD_TIMEOUT, "qr-code-image-after-reset")
        Thread.sleep(HOLD_MS)

        // Delete drops the screen back to the empty state, where the QR and its actions are gone.
        click(overflowButton(), "overflow-button-before-delete")
        click(By.res(DELETE_ACTION_TAG), "delete-action")
        awaitObject(By.res(QR_CODE_CREATE_TAG), LOAD_TIMEOUT, "create-qr-code-button")
        assertThat(device.hasObject(By.res(QR_CODE_IMAGE_TAG))).isFalse()
        assertThat(device.hasObject(overflowButton())).isFalse()
        Thread.sleep(HOLD_MS)

        // Creating again restores the QR code from the same empty state.
        click(By.res(QR_CODE_CREATE_TAG), "create-qr-code-button")
        awaitObject(By.res(QR_CODE_IMAGE_TAG), LOAD_TIMEOUT, "qr-code-image-after-recreate")
        assertThat(device.hasObject(By.res(QR_CODE_LINK_TAG))).isTrue()
        Thread.sleep(FINAL_HOLD_MS)
    }

    private fun str(resId: Int): String = targetContext.getString(resId)

    /** The app bar overflow carries no test tag; it is identified by its content description. */
    private fun overflowButton(): BySelector = By.desc(str(sharedR.string.more_options))

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
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

        /** Demo-clip holds so a human watching the run registers each state. */
        const val HOLD_MS = 2_000L
        const val FINAL_HOLD_MS = 2_000L

        /** Fixed handle behind the generated contact link, so the rendered link is stable. */
        const val CONTACT_LINK_HANDLE = 0x4D4547414E5AL

        /** Main navigation Home item tag, derived from its NavKey class name. */
        const val HOME_NAV_ITEM_TAG = "main_navigation:navigation_item_Home"

        /** Mirror the MY_QR_CODE_* tags of the feature/contact My-QR screen. */
        const val QR_CODE_SCREEN_TAG = "my_qr_code_screen"
        const val QR_CODE_IMAGE_TAG = "my_qr_code_screen:image_qr_code"
        const val QR_CODE_LINK_TAG = "my_qr_code_screen:button_contact_link"
        const val QR_CODE_CREATE_TAG = "my_qr_code_screen:button_create"
        const val RESET_ACTION_TAG = "my_qr_code_screen:action_reset"
        const val DELETE_ACTION_TAG = "my_qr_code_screen:action_delete"
    }
}
