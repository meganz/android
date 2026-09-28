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
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatListItem
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaPushNotificationSettings
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatRoom
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern
import javax.inject.Inject

/**
 * Full-app instrumented test for the chat list Do not disturb action (AND-25009): picking it while
 * every chat is already silenced offers to unmute instead of reopening the mute period picker.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListArchivedNavigationTest]: the whole
 * app runs as in production; only the SDK gateways are faked. The push notification settings the
 * fake replies with are stated up front for each write, since the stub cannot derive a new mute
 * state from the settings the app hands it. The UI is driven with UiAutomator; the overflow menu,
 * the picker dialog and the snackbar all live in their own windows, so they are matched by label.
 *
 * The single continuous journey also doubles as the MR demo recording for AND-25009.
 */
@HiltAndroidTest
class ChatListDoNotDisturbTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

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

        fakeMegaApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
        seedRoom(CHAT_ID, CHAT_TITLE)
        stubPushNotificationSettings(doNotDisturbEnabled = false)
        fakeMegaChat.clearInvocations()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        fakeFlags.clear()
        fakeMegaApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun doNotDisturbOffersUnmuteWhenAlreadyOn() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        awaitObject(chatRow(CHAT_TITLE), LAUNCH_TIMEOUT, "chat-row")

        // Do not disturb is off, so the action opens the mute period picker; pick one hour.
        stubPushNotificationSettings(doNotDisturbEnabled = true)
        openDoNotDisturbAction()
        awaitObject(By.text(pickerTitle()), LOAD_TIMEOUT, "mute-picker")
        device.findObject(By.text(oneHourOption())).click()
        awaitGone(By.text(pickerTitle()), LOAD_TIMEOUT, "mute-picker-closed")

        // Do not disturb is on, so the action now offers to unmute instead of the picker.
        openDoNotDisturbAction()
        awaitObject(By.text(mutedMessage()), SNACKBAR_TIMEOUT, "muted-snackbar")
        awaitObject(unmuteAction(), SNACKBAR_TIMEOUT, "unmute-action")
        assertThat(device.hasObject(By.text(pickerTitle()))).isFalse()

        // Unmute turns Do not disturb off again.
        stubPushNotificationSettings(doNotDisturbEnabled = false)
        device.findObject(unmuteAction()).click()
        awaitGone(By.text(mutedMessage()), LOAD_TIMEOUT, "muted-snackbar-dismissed")

        // Off again, so the action is back to offering the mute period picker.
        openDoNotDisturbAction()
        awaitObject(By.text(pickerTitle()), LOAD_TIMEOUT, "mute-picker-again")
        assertThat(device.hasObject(By.text(mutedMessage()))).isFalse()
    }

    private fun openDoNotDisturbAction() {
        awaitObject(By.res(SHOW_MORE_TAG), LOAD_TIMEOUT, "overflow-button")
        device.findObject(By.res(SHOW_MORE_TAG)).click()
        awaitObject(By.text(doNotDisturbLabel()), LOAD_TIMEOUT, "do-not-disturb-action")
        device.findObject(By.text(doNotDisturbLabel())).click()
    }

    /**
     * Seeds what the fake replies with for the next push notification settings read or write.
     */
    private fun stubPushNotificationSettings(doNotDisturbEnabled: Boolean) {
        val settings = StubMegaPushNotificationSettings(
            globalChatsDndEnabled = doNotDisturbEnabled,
            globalChatsDnd = if (doNotDisturbEnabled) MUTED_UNTIL_TIMESTAMP else -1L,
        )
        fakeMegaApi.stubRequest(
            MegaApiGateway::getPushNotificationSettings,
            request = StubMegaRequest(megaPushNotificationSettings = settings),
        )
        fakeMegaApi.stubRequest(
            MegaApiGateway::setPushNotificationSettings,
            request = StubMegaRequest(megaPushNotificationSettings = settings),
        )
    }

    private fun seedRoom(chatId: Long, title: String) {
        fakeMegaChat.chatState.chatRooms[chatId] = StubMegaChatRoom(
            chatId = chatId,
            title = title,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isActive = true,
        )
        fakeMegaChat.chatState.chatListItems[chatId] = StubMegaChatListItem(
            chatId = chatId,
            title = title,
            lastMessage = "",
            lastTimestamp = BASE_TIMESTAMP,
            isGroup = true,
            isActive = true,
        )
    }

    private fun chatRow(title: String): BySelector =
        By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.text(title))

    private fun doNotDisturbLabel() =
        targetContext.getString(sharedR.string.title_dialog_mute_chat_notifications)

    private fun pickerTitle() =
        targetContext.getString(sharedR.string.contact_info_mute_notifications_dialog_title)

    private fun oneHourOption() =
        targetContext.resources.getQuantityString(sharedR.plurals.contact_info_mute_option_hours, 1, 1)

    private fun mutedMessage() =
        targetContext.getString(sharedR.string.chat_list_notifications_muted_message)

    private fun unmuteAction(): BySelector = By.text(
        Pattern.compile(
            Pattern.quote(targetContext.getString(sharedR.string.general_unmute)),
            Pattern.CASE_INSENSITIVE,
        )
    )

    private fun awaitObject(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.hasObject(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump"
        )
    }

    private fun awaitGone(selector: BySelector, timeout: Long, name: String) {
        if (device.wait(Until.gone(selector), timeout)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector) to go; hierarchy in logcat tag UiDump"
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
        const val SNACKBAR_TIMEOUT = 5_000L

        const val CHAT_ID = 8_101L
        const val PEER_HANDLE = 9_101L
        const val BASE_TIMESTAMP = 1_700_000_000L
        const val MUTED_UNTIL_TIMESTAMP = 4_102_444_800L

        const val CHAT_TITLE = "Design Team"

        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
        const val SHOW_MORE_TAG = "top_app_bar_actions:show_more"
    }
}
