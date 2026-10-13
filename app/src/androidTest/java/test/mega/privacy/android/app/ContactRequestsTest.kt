package mega.privacy.android.app

import android.app.Activity
import android.app.Application
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.contacts.ContactsActivity
import mega.privacy.android.app.contacts.requests.ContactRequestsFragment
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.stub.StubMegaContactRequest
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.MegaNavigatorEntryPoint
import mega.privacy.android.navigation.destination.ContactRequestsNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented coverage for the Compose contact requests screen in `:feature:contact`,
 * written while [ApiFeatures.ContactComposeFeature] is still off in production.
 *
 * Three behaviours shipped without instrumented coverage and are asserted here:
 * - the content-aware default tab (`ContactRequestsViewModel.resolveInitialTab`), a one-shot side
 *   effect that rewrites the seeded tab during the state `combine`. Both halves of its precedence
 *   rule are covered, because either one alone also passes a naive implementation;
 * - the in-screen search, which filters both tabs through `ContactRequestsViewModel.setQuery`;
 * - `AppNavigator.openSentRequests`, whose routing between the Compose destination and the legacy
 *   [ContactsActivity] had only JVM coverage.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListSearchTest] and
 * [ContactInfoBackNavigationTest]: the whole app runs as in production with only the SDK gateways
 * faked, requests are seeded through `MegaApiGateway::getIncomingContactRequests` /
 * `getOutgoingContactRequests`, and the UI is driven with UiAutomator — Compose test tags are
 * matched as resource ids because [MegaActivity] sets `testTagsAsResourceId = true`.
 *
 * The tab row, the request rows and the search field carry no test tags, so they are matched by
 * their rendered text and, for the search affordances, by the content descriptions core-ui
 * hardcodes. A request row renders the counterpart's email as its title (the mapper has no name to
 * show for a pre-verification contact), which is what makes row-level text matching possible.
 */
@HiltAndroidTest
class ContactRequestsTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private var scenario: ActivityScenario<MegaActivity>? = null

    /**
     * The seeded requests, read through the gateway stubs on every refresh rather than captured
     * once, so a scenario can change what the SDK reports mid-test.
     */
    private var incomingRequests: List<MegaContactRequest> = emptyList()

    private var outgoingRequests: List<MegaContactRequest> = emptyList()

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

        // Production registers the lifecycle handler in MegaApplication.onCreate, which does not
        // run under the Hilt test application; snackbar plumbing needs the current activity.
        (targetContext.applicationContext as Application)
            .registerActivityLifecycleCallbacks(activityLifecycleHandler)

        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        // The fakes are shared singletons across tests in the process; start from pristine fakes.
        fakeApi.resetToDefaults()
        incomingRequests = emptyList()
        outgoingRequests = emptyList()
        fakeApi.stub(MegaApiGateway::getIncomingContactRequests) { incomingRequests }
        fakeApi.stub(MegaApiGateway::getOutgoingContactRequests) { outgoingRequests }
        fakeApi.stub(MegaApiGateway::getContactRequestByHandle) { arguments ->
            (incomingRequests + outgoingRequests)
                .firstOrNull { it.handle == arguments.firstOrNull() }
        }
        // No alias is set, so the alias read must fail cleanly wherever contacts resolve.
        fakeApi.stubRequest(
            MegaApiGateway::getUserAlias,
            error = StubMegaError(MegaError.API_ENOENT),
        )
        fakeApi.clearInvocations()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)

            // The one-time onboarding tooltip is a popup over the screens this suite taps through.
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        (targetContext.applicationContext as Application)
            .unregisterActivityLifecycleCallbacks(activityLifecycleHandler)
        fakeFlags.clear()
        fakeApi.resetToDefaults()
    }

    // region tests

    /**
     * The screen is opened on Sent while only Received has requests, so `resolveInitialTab` yields
     * to Received; search then narrows that tab to the matching request and clearing the query
     * restores both rows.
     */
    @Test
    fun theScreenFallsBackToTheTabThatHasRequestsAndSearchFiltersThem() {
        incomingRequests = listOf(
            incomingRequest(FIRST_REQUEST_HANDLE, FIRST_RECEIVED_EMAIL),
            incomingRequest(SECOND_REQUEST_HANDLE, SECOND_RECEIVED_EMAIL),
        )

        openRequests(ContactRequestsNavKey.NavType.SentRequests)

        // Opened on Sent, settled on Received: the empty seeded tab yielded to the populated one.
        awaitObject(By.text(FIRST_RECEIVED_EMAIL), LOAD_TIMEOUT, "first-received-row")
        awaitObject(By.text(SECOND_RECEIVED_EMAIL), LOAD_TIMEOUT, "second-received-row")

        // A query matching one email alone narrows the tab to that row.
        click(searchAction(), "search-action")
        typeQuery(MATCHING_QUERY)
        awaitObject(By.text(FIRST_RECEIVED_EMAIL), LOAD_TIMEOUT, "filtered-received-row")
        awaitGone(By.text(SECOND_RECEIVED_EMAIL), "non-matching-received-row")

        // Clearing the field drops the filter and restores both rows.
        click(By.desc(CLEAR_TEXT_DESC), "clear-query")
        awaitObject(By.text(FIRST_RECEIVED_EMAIL), LOAD_TIMEOUT, "restored-first-received-row")
        awaitObject(By.text(SECOND_RECEIVED_EMAIL), LOAD_TIMEOUT, "restored-second-received-row")
    }

    /**
     * The other half of the precedence rule: when the tab the screen was opened on has requests of
     * its own, it keeps it — the fallback never fires, even though the other tab is populated too.
     * The selected tab is asserted through the rows it renders rather than the tab row's own
     * selection state, which is what a reader of this screen actually sees.
     */
    @Test
    fun theSeededTabWinsWhenItHasRequests() {
        incomingRequests = listOf(incomingRequest(FIRST_REQUEST_HANDLE, FIRST_RECEIVED_EMAIL))
        outgoingRequests = listOf(outgoingRequest(THIRD_REQUEST_HANDLE, FIRST_SENT_EMAIL))

        openRequests(ContactRequestsNavKey.NavType.SentRequests)

        awaitObject(By.text(FIRST_SENT_EMAIL), LOAD_TIMEOUT, "sent-row")
        awaitGone(By.text(FIRST_RECEIVED_EMAIL), "received-row")
    }

    /**
     * Selecting the empty Sent tab from the populated Received tab swaps the list for the Sent
     * tab's empty state.
     */
    @Test
    fun selectingTheSentTabShowsItsEmptyState() {
        incomingRequests = listOf(incomingRequest(FIRST_REQUEST_HANDLE, FIRST_RECEIVED_EMAIL))

        openRequests(ContactRequestsNavKey.NavType.ReceivedRequests)
        awaitObject(By.text(FIRST_RECEIVED_EMAIL), LOAD_TIMEOUT, "received-row")

        click(By.text(sentTabTitle()), "sent-tab")

        awaitObject(By.res(CONTACT_REQUESTS_EMPTY_TAG), LOAD_TIMEOUT, "sent-empty-state")
        awaitGone(By.text(FIRST_RECEIVED_EMAIL), "received-row-after-tab-switch")
    }

    /**
     * With the flag on, the production `openSentRequests` call routes into the Compose destination
     * on the Sent tab. Sent requests are seeded because an empty Sent tab would be moved to
     * Received by the content-aware default, which would make the assertion below meaningless.
     */
    @Test
    fun openSentRequestsRoutesToTheComposeScreenWhenTheFlagIsOn() {
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)
        incomingRequests = listOf(incomingRequest(FIRST_REQUEST_HANDLE, FIRST_RECEIVED_EMAIL))
        outgoingRequests = listOf(outgoingRequest(THIRD_REQUEST_HANDLE, FIRST_SENT_EMAIL))

        launchApp()
        openSentRequests()

        awaitObject(By.res(CONTACT_REQUESTS_SCREEN_TAG), LAUNCH_TIMEOUT, "contact-requests-screen")
        awaitObject(By.text(FIRST_SENT_EMAIL), LOAD_TIMEOUT, "sent-row")
        awaitGone(By.text(FIRST_RECEIVED_EMAIL), "received-row")
    }

    /**
     * With the flag off, the same call routes to the legacy [ContactsActivity] sent-requests
     * intent. The launch is stubbed rather than carried out: the legacy activity is a `BaseActivity`
     * that reads `MegaApplication.getInstance()` and cannot be instantiated under the Hilt test
     * application. See [ContactInfoBackNavigationTest].
     */
    @Test
    fun openSentRequestsRoutesToTheLegacyScreenWhenTheFlagIsOff() {
        fakeFlags.set(ApiFeatures.ContactComposeFeature, false)

        launchApp()

        Intents.init()
        try {
            intending(anyIntent()).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

            openSentRequests()

            awaitIntended(
                "legacy-sent-requests",
                hasComponent(ContactsActivity::class.java.name),
                hasExtra(ContactRequestsFragment.EXTRA_IS_OUTGOING, true),
            )
            assertThat(device.hasObject(By.res(CONTACT_REQUESTS_SCREEN_TAG))).isFalse()
        } finally {
            Intents.release()
        }
    }

    /**
     * Accepting a received request through its actions sheet drops it from the list and leaves the
     * other request listed. The SDK drops a resolved request from its pending list and pushes a
     * contact-request update; both are simulated so the screen re-renders off its own monitor flow
     * rather than off the tap.
     */
    @Test
    fun acceptingAReceivedRequestRemovesItFromTheList() {
        val first = incomingRequest(FIRST_REQUEST_HANDLE, FIRST_RECEIVED_EMAIL)
        val second = incomingRequest(SECOND_REQUEST_HANDLE, SECOND_RECEIVED_EMAIL)
        incomingRequests = listOf(first, second)

        openRequests(ContactRequestsNavKey.NavType.ReceivedRequests)
        awaitObject(By.text(FIRST_RECEIVED_EMAIL), LOAD_TIMEOUT, "first-received-row")
        awaitObject(By.text(SECOND_RECEIVED_EMAIL), LOAD_TIMEOUT, "second-received-row")

        applyAction(
            email = SECOND_RECEIVED_EMAIL,
            actionTag = CONTACT_REQUEST_ACTION_ACCEPT_TAG,
            name = "accept",
            resolved = second,
        )

        awaitGone(By.text(SECOND_RECEIVED_EMAIL), "accepted-row")
        awaitObject(By.text(FIRST_RECEIVED_EMAIL), LOAD_TIMEOUT, "remaining-received-row")
    }

    // endregion

    // region navigation & seeding helpers

    /** Launches the logged-in app straight onto the requests destination for [navType]. */
    private fun openRequests(navType: ContactRequestsNavKey.NavType) {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ContactRequestsNavKey(navType)),
        )
        scenario = ActivityScenario.launch(intent)
        awaitObject(By.res(CONTACT_REQUESTS_SCREEN_TAG), LAUNCH_TIMEOUT, "contact-requests-screen")
        awaitGone(By.res(CONTACT_REQUESTS_LOADING_TAG), "contact-requests-loading")
    }

    private fun launchApp() {
        scenario = ActivityScenario.launch(MegaActivity::class.java)
        awaitObject(By.res(HOME_NAV_ITEM_TAG), LAUNCH_TIMEOUT, "home-nav-item")
    }

    /**
     * The body of the production `openSentRequests` navigation helper, called with the activity its
     * in-app callers are hosted by — the legacy branch starts the target activity straight off that
     * context, which an application context cannot do without a new-task flag.
     */
    private fun openSentRequests() {
        requireNotNull(scenario).onActivity { activity ->
            EntryPointAccessors
                .fromApplication(activity, MegaNavigatorEntryPoint::class.java)
                .megaNavigator
                .openSentRequests(activity)
        }
    }

    /**
     * Opens the actions sheet on the request row titled [email], picks [actionTag], and then
     * simulates what the SDK does once the reply lands: [resolved] leaves the pending list and a
     * contact-request update is pushed, which is what makes the monitor flow re-read it.
     */
    private fun applyAction(
        email: String,
        actionTag: String,
        name: String,
        resolved: MegaContactRequest,
    ) {
        click(By.text(email), "$name-row")
        click(By.res(actionTag), "$name-action")

        awaitInvocations("replyReceivedContactRequest") {
            fakeApi.invocations.filter { it.methodName == "replyReceivedContactRequest" }
        }

        incomingRequests = incomingRequests.filterNot { it.handle == resolved.handle }
        runBlocking {
            fakeApi.emitGlobalUpdate(GlobalUpdate.OnContactRequestsUpdate(arrayListOf(resolved)))
        }
    }

    /** The repository drops requests without a source email, so both directions carry one. */
    private fun incomingRequest(handle: Long, email: String) = StubMegaContactRequest(
        handle = handle,
        sourceEmail = email,
        targetEmail = fakeApi.account.email,
        creationTime = CREATION_TIME,
        isOutgoing = false,
    )

    private fun outgoingRequest(handle: Long, email: String) = StubMegaContactRequest(
        handle = handle,
        sourceEmail = fakeApi.account.email,
        targetEmail = email,
        creationTime = CREATION_TIME,
        isOutgoing = true,
    )

    // endregion

    // region UiAutomator helpers

    private fun str(resId: Int): String = targetContext.getString(resId)

    /** The tab row carries no test tags, so a tab is matched by its title. */
    private fun sentTabTitle() = str(sharedR.string.tab_sent_requests)

    /** The search action is an icon with the description core-ui's search top app bar hardcodes. */
    private fun searchAction(): BySelector = By.desc(SEARCH_ACTION_DESC)

    private fun typeQuery(query: String) {
        awaitObject(By.clazz(EDIT_TEXT_CLASS), LOAD_TIMEOUT, "search-field")
        device.findObject(By.clazz(EDIT_TEXT_CLASS)).text = query
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

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), LOAD_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for $name ($selector) to be gone; " +
                    "hierarchy in logcat tag UiDump",
        )
    }

    /**
     * Gateway commands are dispatched from a `viewModelScope.launch`, so their invocations are not
     * recorded synchronously after the UI action that triggered them.
     */
    private fun <T> awaitInvocations(name: String, supplier: () -> List<T>): List<T> {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            SystemClock.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for an invocation of $name; " +
                    "recorded=${fakeApi.invocations.map { it.methodName }}",
        )
    }

    /** The navigator decides and launches off the application scope, so the intent is polled for. */
    private fun awaitIntended(name: String, vararg matchers: Matcher<Intent>) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        var lastError: AssertionError? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                matchers.forEach { intended(it) }
                return
            } catch (e: AssertionError) {
                lastError = e
                SystemClock.sleep(POLL_INTERVAL_MS)
            }
        }
        throw AssertionError("Expected intent for $name not observed", lastError)
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
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val POLL_INTERVAL_MS = 100L

        const val FIRST_REQUEST_HANDLE = 7_101L
        const val SECOND_REQUEST_HANDLE = 7_102L
        const val THIRD_REQUEST_HANDLE = 7_103L

        const val FIRST_RECEIVED_EMAIL = "alpharequest@mega.co.nz"
        const val SECOND_RECEIVED_EMAIL = "bravorequest@mega.co.nz"
        const val FIRST_SENT_EMAIL = "charliesent@mega.co.nz"

        /** Matches the first received email only. */
        const val MATCHING_QUERY = "alpha"

        const val CREATION_TIME = 1_700_000_000L

        const val EDIT_TEXT_CLASS = "android.widget.EditText"

        /** Content descriptions hardcoded by core-ui's search top app bar and SearchInputField. */
        const val SEARCH_ACTION_DESC = "Search"
        const val CLEAR_TEXT_DESC = "Clear Text"

        /** Main navigation Home item tag, derived from its NavKey class name. */
        const val HOME_NAV_ITEM_TAG = "main_navigation:navigation_item_Home"

        /** Mirror the ContactRequestsTestTags of the feature/contact requests screen. */
        const val CONTACT_REQUESTS_SCREEN_TAG = "contact_requests_screen"
        const val CONTACT_REQUESTS_LOADING_TAG = "contact_requests_screen:loading"
        const val CONTACT_REQUESTS_EMPTY_TAG = "contact_requests_screen:empty"
        const val CONTACT_REQUEST_ACTION_ACCEPT_TAG = "contact_request_actions_sheet:accept"
    }
}
