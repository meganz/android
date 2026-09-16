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
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatListItem
import mega.privacy.android.data.test.stub.StubMegaChatRoom
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
import javax.inject.Inject

/**
 * Full-app instrumented test for the archived-chats entry point wired in AND-24900: the archive
 * action in the chat-list top bar opens the archived-chats screen, which shows the archived rooms
 * (and not the active ones).
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListSelectionTest]: the whole app runs
 * as in production; only the SDK gateways are faked. Active and archived rooms are seeded together —
 * the Chats tab shows the active ones (`GetChatsUseCase` filters archived out) and the archived
 * screen shows the archived ones (the fake `getChatListItems` now honours the archived filter). The
 * UI is driven with UiAutomator; Compose test tags are matched as resource ids.
 *
 * The single continuous journey also doubles as the MR demo recording for AND-24900.
 */
@HiltAndroidTest
class ChatListArchivedNavigationTest {

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

        fakeMegaChat.resetToDefaults()
        seedRoom(ACTIVE_CHAT_ID, ACTIVE_TITLE, archived = false)
        seedRoom(ARCHIVED_CHAT_ID, ARCHIVED_TITLE, archived = true)
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
        fakeMegaChat.resetToDefaults()
    }

    @Test
    fun archiveActionOpensArchivedChatsScreen() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        // Chats tab shows the active chat and not the archived one.
        awaitObject(chatRow(ACTIVE_TITLE), LAUNCH_TIMEOUT, "active-chat-row")
        assertThat(device.hasObject(chatRow(ARCHIVED_TITLE))).isFalse()
        Thread.sleep(2_000)

        // Open the archived chats screen from the top-bar archive action.
        device.findObject(By.res(ARCHIVED_ACTION_TAG)).click()

        // The archived screen shows the archived chat and not the active one.
        awaitObject(By.res(ARCHIVED_SCREEN_TAG), LOAD_TIMEOUT, "archived-screen")
        val archivedTitle = targetContext.getString(sharedR.string.chat_archived_chats_title)
        awaitObject(By.text(archivedTitle), LOAD_TIMEOUT, "archived-app-bar-title")
        awaitObject(chatRow(ARCHIVED_TITLE), LOAD_TIMEOUT, "archived-chat-row")
        assertThat(device.hasObject(chatRow(ACTIVE_TITLE))).isFalse()
        Thread.sleep(2_000)
    }

    private fun seedRoom(chatId: Long, title: String, archived: Boolean) {
        fakeMegaChat.chatState.chatRooms[chatId] = StubMegaChatRoom(
            chatId = chatId,
            title = title,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(PEER_HANDLE_BASE + chatId to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isActive = true,
            isArchived = archived,
        )
        fakeMegaChat.chatState.chatListItems[chatId] = StubMegaChatListItem(
            chatId = chatId,
            title = title,
            lastMessage = "",
            lastTimestamp = BASE_TIMESTAMP - chatId,
            isGroup = true,
            isActive = true,
            isArchived = archived,
        )
    }

    private fun chatRow(title: String): BySelector =
        By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.text(title))

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

        const val ACTIVE_CHAT_ID = 8_001L
        const val ARCHIVED_CHAT_ID = 8_002L
        const val PEER_HANDLE_BASE = 9_001L
        const val BASE_TIMESTAMP = 1_700_000_000L

        const val ACTIVE_TITLE = "Design Team"
        const val ARCHIVED_TITLE = "Old Project"

        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
        const val ARCHIVED_ACTION_TAG = "chat_list_top_app_bar:archived_action"
        const val ARCHIVED_SCREEN_TAG = "archived_chats_screen:screen"
    }
}
