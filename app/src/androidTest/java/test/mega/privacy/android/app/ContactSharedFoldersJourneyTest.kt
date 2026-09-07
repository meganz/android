package mega.privacy.android.app

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.presentation.contactinfo.ContactInfoComposeActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaShare
import mega.privacy.android.data.test.stub.StubMegaUser
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaUser
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.reflect.KSuspendFunction2

/**
 * Full-app instrumented journey for the native Compose per-contact incoming shared folders screen
 * (AND-24404), which replaces the legacy `ContactFileListActivity` bridge behind
 * `ContactSharedFoldersNavKey`.
 *
 * The whole app runs as in production — real navigation, activities, ViewModels, use cases and
 * repositories — with only the SDK gateways faked via `:data-test` (see
 * [mega.privacy.android.app.di.FakeSdkGatewayModule]). The journey:
 *
 * 1. Open the Compose contact info screen for a seeded contact ([ContactInfoComposeActivity], the
 *    production flag-on host, `ContactComposeFeature` forced on) and tap its "Shared folders" row.
 * 2. The row opens the new native Compose screen in-place (no legacy activity launch): the app bar
 *    carries the contact identity and the list shows exactly the folders shared by this contact —
 *    a folder seeded as shared by a different contact must not appear.
 * 3. Tapping a folder navigates into its contents (the regular cloud-drive folder screen).
 * 4. Long-pressing a folder enters selection mode with the selection app bar; back exits it.
 *
 * UI is driven with UiAutomator rather than a Compose rule: the compose rule's idle
 * synchronization interferes with the production activity's splash-gated composition. Compose test
 * tags are matched as resource ids because the app sets `testTagsAsResourceId = true`.
 */
@HiltAndroidTest
class ContactSharedFoldersJourneyTest {

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
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private val contact = StubMegaUser(email = CONTACT_EMAIL, handle = CONTACT_HANDLE)
    private val otherContact = StubMegaUser(email = OTHER_EMAIL, handle = OTHER_HANDLE)

    /** Disambiguates the by-user overload of the two `getInShares` candidates. */
    private val getInSharesByUserRef: KSuspendFunction2<MegaApiGateway, MegaUser, List<MegaNode>> =
        MegaApiGateway::getInShares

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()
        TestAppBoot.runCoreInitializers()

        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )

        // The Compose contact info screen (the journey's entry point) is gated on this flag.
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        fakeMegaApi.stub(MegaApiGateway::getContacts) { listOf(contact, otherContact) }
        fakeMegaApi.stub(MegaApiGateway::getContact) { arguments ->
            when (arguments.firstOrNull()) {
                CONTACT_EMAIL -> contact
                OTHER_EMAIL -> otherContact
                else -> null
            }
        }

        // The in-share folders live outside the account tree; parent them under a synthetic
        // in-shares handle so tree lookups by handle resolve and folder navigation lists children.
        seedFolder(HOLIDAY_HANDLE, HOLIDAY_NAME)
        seedFolder(PROJECT_HANDLE, PROJECT_NAME)
        seedFolder(OTHER_FOLDER_HANDLE, OTHER_FOLDER_NAME)
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = CHILD_FILE_HANDLE,
                name = CHILD_FILE_NAME,
                parentHandle = HOLIDAY_HANDLE,
                size = 2_048L,
            ),
            parentHandle = HOLIDAY_HANDLE,
        )

        // The contact info "Shared folders" row count resolves through getInShares. Stubs are
        // keyed by method name, so the by-user and by-order overloads share this answer.
        fakeMegaApi.stub(getInSharesByUserRef) { arguments ->
            when ((arguments.firstOrNull() as? MegaUser)?.handle) {
                CONTACT_HANDLE -> listOf(holidayFolder, projectFolder)
                OTHER_HANDLE -> listOf(otherFolder)
                else -> listOf(holidayFolder, projectFolder, otherFolder)
            }
        }

        // The per-contact screen loads all incoming shares and filters them by the contact's
        // email — seed shares from two different contacts to exercise that filtering. The shares
        // stay unverified so MapNodeToShareUseCase skips the legacy ContactUtil name lookup, which
        // cannot run under the Hilt test application (it reads MegaApplication.getInstance()).
        fakeMegaApi.stub(MegaApiGateway::getVerifiedIncomingShares) {
            listOf(
                StubMegaShare(
                    user = CONTACT_EMAIL,
                    nodeHandle = HOLIDAY_HANDLE,
                    isVerified = false,
                ),
                StubMegaShare(
                    user = CONTACT_EMAIL,
                    nodeHandle = PROJECT_HANDLE,
                    isVerified = false,
                ),
                StubMegaShare(
                    user = OTHER_EMAIL,
                    nodeHandle = OTHER_FOLDER_HANDLE,
                    isVerified = false,
                ),
            )
        }

        runBlocking {
            // One-time onboarding UI must not intercept taps on clean-data runs.
            setCustomiseNavigationTooltipShownUseCase()
            // Persist a logged-in session through the app's real credentials path.
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        fakeFlags.clear()
    }

    @Test
    fun contactSharedFoldersScreenShowsOnlyThatContactsFolders() {
        val intent = Intent(targetContext, ContactInfoComposeActivity::class.java)
            .putExtra(Constants.NAME, CONTACT_EMAIL)
        ActivityScenario.launch<ContactInfoComposeActivity>(intent)

        // Contact info renders with the shared-folders row counting this contact's two shares.
        awaitObject(By.res(SHARED_FOLDERS_ROW_TAG), LAUNCH_TIMEOUT, "shared-folders-row")
        Thread.sleep(1_500)
        device.findObject(By.res(SHARED_FOLDERS_ROW_TAG)).click()

        // The row opens the native Compose screen in-place (pre-AND-24404 it bounced out to the
        // legacy ContactFileListActivity).
        awaitObject(By.res(CONTACT_SHARED_FOLDERS_APP_BAR_TAG), LOAD_TIMEOUT, "shared-folders-app-bar")

        // Exactly this contact's folders are listed; the other contact's folder is filtered out.
        awaitObject(By.res(NODE_TITLE_TAG).text(HOLIDAY_NAME), LOAD_TIMEOUT, "holiday-folder-row")
        awaitObject(By.res(NODE_TITLE_TAG).text(PROJECT_NAME), LOAD_TIMEOUT, "project-folder-row")
        if (device.hasObject(By.res(NODE_TITLE_TAG).text(OTHER_FOLDER_NAME))) {
            throw AssertionError(
                "Folder \"$OTHER_FOLDER_NAME\" shared by $OTHER_EMAIL must not be listed"
            )
        }
        Thread.sleep(2_000)

        // Long-pressing a folder enters selection mode. UiObject2.longClick holds for exactly the
        // system long-press timeout, which Compose's detector can miss; click(duration) holds for
        // a full second.
        device.findObject(By.res(NODE_TITLE_TAG).text(PROJECT_NAME)).click(LONG_PRESS_MS)
        // NodeSelectionModeAppBar drops the caller's modifier (and with it the screen's tag), so
        // selection mode is asserted through its app bar title: the selected count.
        awaitObject(By.res(TOP_APP_BAR_TITLE_TAG).text("1"), LOAD_TIMEOUT, "selection-mode-app-bar")
        Thread.sleep(2_000)

        // Back exits selection mode without leaving the screen.
        device.pressBack()
        awaitObject(By.res(CONTACT_SHARED_FOLDERS_APP_BAR_TAG), LOAD_TIMEOUT, "selection-cleared")
        Thread.sleep(1_000)

        // Tapping a folder navigates into its contents.
        awaitObject(By.res(NODE_TITLE_TAG).text(HOLIDAY_NAME), LOAD_TIMEOUT, "holiday-folder-row-again")
        device.findObject(By.res(NODE_TITLE_TAG).text(HOLIDAY_NAME)).click()
        awaitObject(By.res(NODE_TITLE_TAG).text(CHILD_FILE_NAME), LOAD_TIMEOUT, "child-file-row")
        Thread.sleep(1_500)

        device.pressBack()
        awaitObject(By.res(CONTACT_SHARED_FOLDERS_APP_BAR_TAG), LOAD_TIMEOUT, "back-on-shared-folders")

        // Hold the final state so a human watching the run can register it.
        Thread.sleep(2_000)
    }

    private val holidayFolder: MegaNode
        get() = requireNotNull(fakeMegaApi.nodeTree.nodeByHandle(HOLIDAY_HANDLE))

    private val projectFolder: MegaNode
        get() = requireNotNull(fakeMegaApi.nodeTree.nodeByHandle(PROJECT_HANDLE))

    private val otherFolder: MegaNode
        get() = requireNotNull(fakeMegaApi.nodeTree.nodeByHandle(OTHER_FOLDER_HANDLE))

    private fun seedFolder(handle: Long, name: String) {
        fakeMegaApi.nodeTree.addNode(
            StubMegaNode(
                handle = handle,
                name = name,
                parentHandle = IN_SHARES_PARENT_HANDLE,
                isFolder = true,
                isInShare = true,
            ),
            parentHandle = IN_SHARES_PARENT_HANDLE,
        )
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
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val LONG_PRESS_MS = 1_000L

        const val CONTACT_HANDLE = 111L
        const val CONTACT_EMAIL = "alice@mega.nz"
        const val OTHER_HANDLE = 222L
        const val OTHER_EMAIL = "bob@mega.nz"

        const val IN_SHARES_PARENT_HANDLE = 900L
        const val HOLIDAY_HANDLE = 901L
        const val HOLIDAY_NAME = "Holiday Photos"
        const val PROJECT_HANDLE = 902L
        const val PROJECT_NAME = "Project Docs"
        const val OTHER_FOLDER_HANDLE = 903L
        const val OTHER_FOLDER_NAME = "Bobs Folder"
        const val CHILD_FILE_HANDLE = 910L
        const val CHILD_FILE_NAME = "beach.jpg"

        /** Mirrors SHARED_FOLDERS_ROW_TAG of ContactInfoOptionsSection. */
        const val SHARED_FOLDERS_ROW_TAG = "contact_info_options_section:row_shared_folders"

        /** Mirrors CONTACT_SHARED_FOLDERS_APP_BAR_TAG of ContactSharedFoldersScreen. */
        const val CONTACT_SHARED_FOLDERS_APP_BAR_TAG = "contact_shared_folders_screen:app_bar"

        /** Mirrors the internal title tag of MegaTopAppBar. */
        const val TOP_APP_BAR_TITLE_TAG = "default_top_app_bar:text_title"

        /** Mirrors the internal TITLE_TAG of NodeListViewItem rows. */
        const val NODE_TITLE_TAG = "node_list_view_item:title"
    }
}
