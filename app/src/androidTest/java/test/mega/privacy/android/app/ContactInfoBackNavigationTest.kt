package mega.privacy.android.app

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.VerificationModes.times
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoComposeActivity
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaUser
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.destination.ChatNavKey
import nz.mega.sdk.MegaChatRoom
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import javax.inject.Inject

/**
 * Instrumented regression test for AND-24534: opening Contact info from a 1:1 chat room must launch
 * the contact-info screen on the chat's own task (so Back returns to the chat room) and must NOT
 * bounce through [MegaActivity] (the single-activity Menu root, the pre-fix behaviour) — for both
 * `ContactComposeFeature` flag states.
 *
 * The whole app runs as in production (real navigation, activities, ViewModels) with only the SDK
 * gateways faked via `:data-test` (see [mega.privacy.android.app.di.FakeSdkGatewayModule]) and the
 * `ContactComposeFeature` flag forced through [FakeFeatureFlagValueProvider].
 *
 * Contact info is triggered through the production navigator
 * ([MegaNavigator.openContactInfoActivity]) — the exact call the chat three-dot "Contact info"
 * action (`ChatRoomMenuAction.Info`) and the chat-title click both make. The launched Intent is
 * captured with Espresso-Intents rather than started, so the assertion is on the routing decision
 * (which activity, on which task) without depending on the target screen rendering — and, for the
 * flag-off case, without instantiating the legacy [ContactInfoActivity], which cannot be created
 * under the Hilt test application (its `BaseActivity` reads `MegaApplication.getInstance()`).
 */
@HiltAndroidTest
class ContactInfoBackNavigationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeChat: FakeMegaChatApiGateway

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var megaNavigator: MegaNavigator

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()
        TestAppBoot.runCoreInitializers()

        // Seed a 1:1 chat with a single peer contact.
        fakeChat.chatState.chatRooms[CHAT_ID] = StubMegaChatRoom(
            chatId = CHAT_ID,
            title = CONTACT_NAME,
            isGroup = false,
            peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
        )
        val contact = StubMegaUser(email = CONTACT_EMAIL, handle = PEER_HANDLE)
        fakeApi.stub(MegaApiGateway::getContacts) { listOf(contact) }
        fakeApi.stub(MegaApiGateway::getContact) { contact }

        // Persist a logged-in session through the app's real credentials path.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @Test
    fun contactInfoOpensOnChatTaskWhenComposeUiEnabled() {
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        assertContactInfoLaunchedOnChatTask(
            expectedComponent = ContactInfoComposeActivity::class.java,
        )
    }

    @Test
    fun contactInfoOpensOnChatTaskWhenComposeUiDisabled() {
        fakeFlags.set(ApiFeatures.ContactComposeFeature, false)

        assertContactInfoLaunchedOnChatTask(
            expectedComponent = ContactInfoActivity::class.java,
        )
    }

    private fun assertContactInfoLaunchedOnChatTask(expectedComponent: Class<out Activity>) {
        val intent = Intent(targetContext, ChatActivity::class.java)
            .putExtra(ChatNavKey.LEGACY_CHAT_ID, CHAT_ID)

        ActivityScenario.launch<ChatActivity>(intent).use {
            val chatActivity = awaitResumedActivity(ChatActivity::class.java)

            Intents.init()
            try {
                // Stub every outgoing launch so nothing is actually started — in particular the
                // legacy ContactInfoActivity, which cannot be instantiated under the test app.
                intending(anyIntent()).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))

                // The same navigator call the chat three-dot "Contact info" action makes.
                instrumentation.runOnMainSync {
                    megaNavigator.openContactInfoActivity(chatActivity, CHAT_ID)
                }

                // The flag decides which contact-info screen is launched, carrying the chat id.
                awaitIntended(
                    hasComponent(expectedComponent.name),
                    hasExtra(Constants.HANDLE, CHAT_ID),
                )

                // The regression: the pre-fix path bounced through MegaActivity (Menu root),
                // clearing the chat task. It must never be launched here.
                intended(
                    hasComponent(MegaActivity::class.java.name),
                    times(0),
                )
            } finally {
                Intents.release()
            }
        }
    }

    private fun awaitIntended(vararg matchers: Matcher<Intent>, timeoutMs: Long = INTENT_TIMEOUT) {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var lastError: AssertionError? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                matchers.forEach { intended(it) }
                return
            } catch (e: AssertionError) {
                lastError = e
                SystemClock.sleep(POLL_INTERVAL)
            }
        }
        throw AssertionError("Expected intent not observed within ${timeoutMs}ms", lastError)
    }

    private fun awaitResumedActivity(
        target: Class<out Activity>,
        timeoutMs: Long = RESUME_TIMEOUT,
    ): Activity {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        var last: Activity? = null
        while (SystemClock.uptimeMillis() < deadline) {
            val holder = arrayOfNulls<Activity>(1)
            instrumentation.runOnMainSync {
                holder[0] = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull()
            }
            val resumed = holder[0]
            last = resumed
            if (resumed != null && target.isInstance(resumed)) return resumed
            SystemClock.sleep(POLL_INTERVAL)
        }
        throw AssertionError(
            "Timed out after ${timeoutMs}ms waiting for ${target.name} to resume; " +
                    "last resumed = ${last?.javaClass?.name}"
        )
    }

    private companion object {
        const val CHAT_ID = 555L
        const val PEER_HANDLE = 222L
        const val CONTACT_NAME = "Contact Name"
        const val CONTACT_EMAIL = "contact@mega.nz"

        const val RESUME_TIMEOUT = 20_000L
        const val INTENT_TIMEOUT = 10_000L
        const val POLL_INTERVAL = 100L
    }
}
