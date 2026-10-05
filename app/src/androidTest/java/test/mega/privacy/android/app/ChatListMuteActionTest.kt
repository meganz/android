package mega.privacy.android.app

import android.os.SystemClock
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
import mega.privacy.android.domain.usecase.chat.UpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ChatListNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaPushNotificationSettings
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Full-app instrumented test for the mute action of the chat list actions sheet (AND-24572): the
 * sheet must offer "Unmute" when every chat it was opened for is muted, and "Mute" otherwise, both
 * from a row's own actions and from the multi-select More action.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListSelectionTest] and
 * [ChatListDoNotDisturbTest]: the whole app runs as in production, including the `GetChatsUseCase`
 * pipeline that fills in each room's mute state after its first emission; only the SDK gateways
 * are faked. Chats are muted through the push notification settings the fake API replies with,
 * loaded up front as the app does once nodes are fetched, which the fake gateways never drive.
 * The actions sheet is a ModalBottomSheet in its own window where `testTagsAsResourceId` does not
 * apply, so its actions are matched by label.
 */
@HiltAndroidTest
class ChatListMuteActionTest {

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

    @Inject
    lateinit var updatePushNotificationSettingsUseCase: UpdatePushNotificationSettingsUseCase

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
        seedRoom(MUTED_CHAT_ID, MUTED_TITLE, index = 0)
        seedRoom(OTHER_MUTED_CHAT_ID, OTHER_MUTED_TITLE, index = 1)
        seedRoom(UNMUTED_CHAT_ID, UNMUTED_TITLE, index = 2)
        fakeMegaApi.stubRequest(
            MegaApiGateway::getPushNotificationSettings,
            request = StubMegaRequest(
                megaPushNotificationSettings = StubMegaPushNotificationSettings(
                    mutedChatIds = setOf(MUTED_CHAT_ID, OTHER_MUTED_CHAT_ID),
                ),
            ),
        )
        fakeMegaChat.clearInvocations()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
            setCustomiseNavigationTooltipShownUseCase()
            updatePushNotificationSettingsUseCase()
        }
        fakeMegaApi.clearInvocations()
    }

    @After
    fun tearDown() {
        fakeFlags.clear()
        fakeMegaApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun rowActionsOfferUnmuteForAMutedChat() {
        launchChatListWithMutedRowsShown()

        openRowActions(MUTED_TITLE)

        awaitObject(By.text(unmuteLabel()), LOAD_TIMEOUT, "unmute-action")
        assertThat(device.hasObject(By.text(muteLabel()))).isFalse()
    }

    @Test
    fun rowActionsOfferMuteForAnUnmutedChat() {
        launchChatListWithMutedRowsShown()

        openRowActions(UNMUTED_TITLE)

        awaitObject(By.text(muteLabel()), LOAD_TIMEOUT, "mute-action")
        assertThat(device.hasObject(By.text(unmuteLabel()))).isFalse()
    }

    @Test
    fun selectionActionsOfferUnmuteWhenEverySelectedChatIsMuted() {
        launchChatListWithMutedRowsShown()

        selectRows(MUTED_TITLE, OTHER_MUTED_TITLE)
        device.findObject(By.res(MORE_TAG)).click()

        awaitObject(By.text(unmuteLabel()), LOAD_TIMEOUT, "unmute-action")
        assertThat(device.hasObject(By.text(muteLabel()))).isFalse()
    }

    @Test
    fun selectionActionsOfferMuteWhenTheSelectionMixesMutedAndUnmutedChats() {
        launchChatListWithMutedRowsShown()

        selectRows(MUTED_TITLE, UNMUTED_TITLE)
        device.findObject(By.res(MORE_TAG)).click()

        awaitObject(By.text(muteLabel()), LOAD_TIMEOUT, "mute-action")
        assertThat(device.hasObject(By.text(unmuteLabel()))).isFalse()
    }

    @Test
    fun unmuteFromRowActionsTurnsTheChatNotificationsBackOn() {
        fakeMegaApi.stubRequest(
            MegaApiGateway::setPushNotificationSettings,
            request = StubMegaRequest(
                megaPushNotificationSettings = StubMegaPushNotificationSettings(
                    mutedChatIds = setOf(OTHER_MUTED_CHAT_ID),
                ),
            ),
        )
        launchChatListWithMutedRowsShown()

        openRowActions(MUTED_TITLE)
        awaitObject(By.text(unmuteLabel()), LOAD_TIMEOUT, "unmute-action")
        device.findObject(By.text(unmuteLabel())).click()

        val sent = awaitPushNotificationSettingsSent()
        assertThat(sent.isChatDndEnabled(MUTED_CHAT_ID)).isFalse()
        assertThat(sent.isChatDndEnabled(OTHER_MUTED_CHAT_ID)).isTrue()
        awaitGone(mutedRow(MUTED_TITLE), "muted-icon-cleared")
    }

    /**
     * Waits until the muted rows show their mute icon, so every check runs against a list that
     * has already received the chats' mute state.
     */
    private fun launchChatListWithMutedRowsShown() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        awaitObject(chatRow(UNMUTED_TITLE), LAUNCH_TIMEOUT, "unmuted-row")
        awaitObject(mutedRow(MUTED_TITLE), LOAD_TIMEOUT, "muted-row")
        awaitObject(mutedRow(OTHER_MUTED_TITLE), LOAD_TIMEOUT, "other-muted-row")
        assertThat(device.hasObject(mutedRow(UNMUTED_TITLE))).isFalse()
    }

    private fun openRowActions(title: String) {
        device.findObject(chatRow(title)).findObject(By.res(ROW_MORE_TAG)).click()
    }

    private fun selectRows(first: String, second: String) {
        device.findObject(chatRow(first)).longClick()
        awaitObject(selectionCount("1"), LOAD_TIMEOUT, "selection-count-1")
        device.findObject(chatRow(second)).click()
        awaitObject(selectionCount("2"), LOAD_TIMEOUT, "selection-count-2")
    }

    /**
     * The action is dispatched from a coroutine, so the settings write is not recorded
     * synchronously after the tap that triggered it.
     */
    private fun awaitPushNotificationSettingsSent(): MegaPushNotificationSettings {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            fakeMegaApi.invocations
                .lastOrNull { it.methodName == "setPushNotificationSettings" }
                ?.let { return it.arguments.first() as MegaPushNotificationSettings }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for setPushNotificationSettings; " +
                    "recorded methods=${fakeMegaApi.invocations.map { it.methodName }}"
        )
    }

    private fun seedRoom(chatId: Long, title: String, index: Int) {
        fakeMegaChat.chatState.chatRooms[chatId] = StubMegaChatRoom(
            chatId = chatId,
            title = title,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE_BASE + index to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isActive = true,
            isArchived = false,
        )
        fakeMegaChat.chatState.chatListItems[chatId] = StubMegaChatListItem(
            chatId = chatId,
            title = title,
            lastMessage = "",
            lastTimestamp = BASE_TIMESTAMP - index,
            isGroup = true,
            isActive = true,
        )
    }

    private fun chatRow(title: String): BySelector =
        By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.text(title))

    private fun mutedRow(title: String): BySelector =
        chatRow(title).hasDescendant(By.res(ROW_MUTE_ICON_TAG))

    private fun selectionCount(count: String): BySelector =
        By.res(SELECTION_BAR_TAG).hasDescendant(By.text(count))

    private fun muteLabel() = targetContext.getString(sharedR.string.general_mute)

    private fun unmuteLabel() = targetContext.getString(sharedR.string.general_unmute)

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

    private companion object {
        const val LAUNCH_TIMEOUT = 60_000L
        const val LOAD_TIMEOUT = 30_000L
        const val POLL_INTERVAL_MS = 100L

        const val MUTED_CHAT_ID = 8_201L
        const val OTHER_MUTED_CHAT_ID = 8_202L
        const val UNMUTED_CHAT_ID = 8_203L
        const val PEER_HANDLE_BASE = 9_201L
        const val BASE_TIMESTAMP = 1_700_000_000L

        const val MUTED_TITLE = "Design Team"
        const val OTHER_MUTED_TITLE = "Weekend Hikers"
        const val UNMUTED_TITLE = "Book Club"

        /** Mirror ChatRoomItemView / ChatListSelectionAppBar test tags. */
        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
        const val ROW_MORE_TAG = "chat_room_item:more_actions"
        const val ROW_MUTE_ICON_TAG = "chat_room_item:mute_icon"
        const val SELECTION_BAR_TAG = "chat_list_screen:selection_bar"

        /** Mirrors the CommonMenuAction.More test tag rendered in the selection app bar. */
        const val MORE_TAG = "node_selection_action:more"
    }
}
