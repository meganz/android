package mega.privacy.android.app

import android.content.Intent
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import android.app.Instrumentation.ActivityResult
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for the new chat-list flows added in AND-24788: the empty-state
 * "Learn more" link and the new-chat FAB.
 *
 * With [ApiFeatures.CustomisableBottomNavigation] and [ApiFeatures.ContactComposeFeature] on and no
 * chats seeded, the chat list shows its empty state (title, subtitle and the "Learn more" link) and
 * the new-chat FAB. The test taps the Learn more link — its `mega.io/chatandmeetings` `ACTION_VIEW`
 * is stubbed with Espresso-Intents so it stays in-app and is asserted — then taps the FAB and
 * verifies the new-chat picker opens.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListSelectionTest]: the whole app runs
 * as in production with only the SDK gateways faked; the UI is driven with UiAutomator, and Compose
 * test tags are matched as resource ids because MegaActivity sets `testTagsAsResourceId = true`.
 *
 * The archived-chats screen added in the same MR is not exercised here: it has no in-app trigger and
 * its nav entry is not registered yet (deferred), so it is covered by its screenshot test instead.
 *
 * The single continuous journey also doubles as the MR demo recording for AND-24788 (!17319).
 */
@HiltAndroidTest
class ChatListFabAndEmptyStateTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var fakeMegaChat: FakeMegaChatApiGateway

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

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

        fakeFlags.set(ApiFeatures.CustomisableBottomNavigation, true)
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        // No chats seeded -> the chat list renders its empty state.
        fakeMegaChat.resetToDefaults()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        fakeFlags.clear()
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun emptyStateLearnMoreLinkAndNewChatFab() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        // Empty state: title + the Learn more link.
        awaitObject(By.res(CHAT_LIST_EMPTY_TAG), LAUNCH_TIMEOUT, "chat-empty-state")
        val emptyTitle = targetContext.getString(sharedR.string.chat_recent_list_empty_title)
        awaitObject(By.text(emptyTitle), LOAD_TIMEOUT, "empty-title")
        awaitObject(By.textContains(LEARN_MORE_TEXT), LOAD_TIMEOUT, "learn-more-link")
        Thread.sleep(2_000)

        // Tap Learn more; its ACTION_VIEW is stubbed so it stays in-app and is asserted. The link
        // is an inline annotated span inside a single (non-clickable) description Text — there is no
        // distinct link node — so tap across the last line, where the trailing "Learn more" sits,
        // until the annotated span is hit (taps on the surrounding non-clickable text are inert).
        Intents.init()
        try {
            intending(hasAction(Intent.ACTION_VIEW)).respondWith(ActivityResult(0, null))
            val matcher = org.hamcrest.core.AllOf.allOf(
                hasAction(Intent.ACTION_VIEW), hasData(LEARN_MORE_URL),
            )
            val b = device.findObject(By.textContains(LEARN_MORE_TEXT)).visibleBounds
            val lineHeight = (b.height() / 3).coerceIn(30, 90)
            val lastLineY = b.bottom - lineHeight / 2
            var x = b.left + 40
            var opened = false
            while (x < b.right - 20 && !opened) {
                device.click(x, lastLineY)
                // Stop at the first tap that lands on the annotated "Learn more" span, so exactly
                // one ACTION_VIEW is asserted regardless of the span's exact width.
                opened = runCatching { intended(matcher) }.isSuccess
                x += 60
            }
            if (!opened) throw AssertionError("Tapping the Learn more link did not open $LEARN_MORE_URL")
        } finally {
            Intents.release()
        }
        Thread.sleep(2_000)

        // Tap the new-chat FAB -> the new-chat picker opens.
        device.findObject(By.res(CHAT_LIST_FAB_TAG)).click()
        val pickerTitle = targetContext.getString(sharedR.string.new_chat_title)
        awaitObject(By.text(pickerTitle), LOAD_TIMEOUT, "new-chat-picker")
        Thread.sleep(2_000)
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
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
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L

        const val LEARN_MORE_TEXT = "Learn more"
        const val LEARN_MORE_URL = "https://mega.io/chatandmeetings"

        /** Mirror ChatListContent / ChatListScreen test tags. */
        const val CHAT_LIST_EMPTY_TAG = "chat_list_content:chat_empty"
        const val CHAT_LIST_FAB_TAG = "chat_list_screen:new_chat_fab"
    }
}
