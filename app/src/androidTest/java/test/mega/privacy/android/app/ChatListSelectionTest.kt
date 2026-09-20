package mega.privacy.android.app

import android.content.Intent
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
 * Full-app instrumented test for the chat-list multi-select added in AND-24787.
 *
 * With [ApiFeatures.CustomisableBottomNavigation] enabled and the Chat section added to the
 * navigation bar, the new Compose chat list is opened from the bar and driven into selection
 * mode: long-pressing a row enters multi-select, further taps toggle rows, the selection top
 * app bar reflects the count and offers select-all, and its overflow surfaces the bulk-actions
 * bottom sheet. Back exits selection. A second journey follows a bulk action through: archiving a
 * two-chat selection must reach the chat gateway once per selected chat and drop out of selection.
 *
 * Built on the `:data-test` fake-SDK framework following [MenuHiddenSectionsTest] and
 * [ChatRoomTest]: the whole app runs as in production — real navigation, ViewModels, the chat
 * repository and the `GetChatsUseCase` pipeline — with only the SDK gateways faked. The chat
 * list is seeded through the fake chat gateway ([FakeMegaChatApiGateway.chatState]): each room
 * needs both a [StubMegaChatRoom] and a matching [StubMegaChatListItem], because the repository
 * combines the two to build every list row. The UI is driven with UiAutomator; Compose test
 * tags are matched as resource ids because MegaActivity sets `testTagsAsResourceId = true`.
 *
 * The single continuous journey also doubles as the MR demo recording for AND-24787.
 */
@HiltAndroidTest
class ChatListSelectionTest {

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
        seedChatRooms()
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
    fun longPressEntersSelectionModeAndSurfacesBulkActions() {
        // Enter the new chat list on the Chats tab through the production intent seam.
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        // The list renders the seeded rooms and starts out of selection mode.
        awaitObject(chatRow(TITLES[0]), LAUNCH_TIMEOUT, "first-chat-row")
        assertThat(device.hasObject(By.res(SELECTION_BAR_TAG))).isFalse()

        // Long-press the first row → selection mode, one selected.
        device.findObject(chatRow(TITLES[0])).longClick()
        awaitObject(By.res(SELECTION_BAR_TAG), LOAD_TIMEOUT, "selection-bar")
        awaitObject(selectionCount("1"), LOAD_TIMEOUT, "selection-count-1")

        // Tap a second row → two selected.
        device.findObject(chatRow(TITLES[1])).click()
        awaitObject(selectionCount("2"), LOAD_TIMEOUT, "selection-count-2")

        // Select-all → every seeded room selected.
        device.findObject(By.res(SELECT_ALL_TAG)).click()
        awaitObject(selectionCount(TITLES.size.toString()), LOAD_TIMEOUT, "selection-count-all")

        // Overflow → the bulk-actions bottom sheet. The sheet is a ModalBottomSheet in its own
        // window where testTagsAsResourceId does not apply, so assert on a bulk action label.
        device.findObject(By.res(MORE_TAG)).click()
        val archiveLabel = targetContext.getString(sharedR.string.general_archive)
        awaitObject(By.text(archiveLabel), LOAD_TIMEOUT, "bulk-actions-sheet")

        // Dismiss the sheet, then exit selection mode with Back.
        device.pressBack()
        awaitGone(By.text(archiveLabel), "bulk-actions-sheet-dismissed")
        device.pressBack()
        awaitGone(By.res(SELECTION_BAR_TAG), "selection-bar-dismissed")
    }

    @Test
    fun bulkArchiveArchivesEverySelectedChatAndLeavesSelection() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        awaitObject(chatRow(TITLES[0]), LAUNCH_TIMEOUT, "first-chat-row")
        device.findObject(chatRow(TITLES[0])).longClick()
        awaitObject(selectionCount("1"), LOAD_TIMEOUT, "selection-count-1")
        device.findObject(chatRow(TITLES[1])).click()
        awaitObject(selectionCount("2"), LOAD_TIMEOUT, "selection-count-2")

        // Archive applies to both selected chats and needs no confirmation, so it runs on tap.
        device.findObject(By.res(MORE_TAG)).click()
        val archiveLabel = targetContext.getString(sharedR.string.general_archive)
        awaitObject(By.text(archiveLabel), LOAD_TIMEOUT, "bulk-actions-sheet")
        device.findObject(By.text(archiveLabel)).click()

        awaitArchiveInvocation(FIRST_CHAT_ID)
        awaitArchiveInvocation(FIRST_CHAT_ID + 1)

        // Running an action clears the selection, so the selection bar goes away with the sheet.
        awaitGone(By.res(SELECTION_BAR_TAG), "selection-bar-dismissed")
    }

    /**
     * The archive command is dispatched from a `viewModelScope.launch`, so its invocation is not
     * recorded synchronously after the tap that triggered it.
     */
    private fun awaitArchiveInvocation(chatId: Long) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matched = fakeMegaChat.invocations.any {
                it.methodName == "archiveChat" &&
                        it.arguments.getOrNull(0) == chatId &&
                        it.arguments.getOrNull(1) == true
            }
            if (matched) return
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for archiveChat($chatId, archive=true); " +
                    "recorded methods=${fakeMegaChat.invocations.map { it.methodName }}"
        )
    }

    private fun seedChatRooms() {
        TITLES.forEachIndexed { index, title ->
            val chatId = FIRST_CHAT_ID + index
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
                lastMessage = "Message in $title",
                lastTimestamp = BASE_TIMESTAMP - index,
                isGroup = true,
                isActive = true,
            )
        }
    }

    private fun chatRow(title: String): BySelector =
        By.res(CHAT_ROOM_ITEM_TAG).hasDescendant(By.text(title))

    private fun selectionCount(count: String): BySelector =
        By.res(SELECTION_BAR_TAG).hasDescendant(By.text(count))

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

        const val FIRST_CHAT_ID = 5_001L
        const val PEER_HANDLE_BASE = 6_001L
        const val BASE_TIMESTAMP = 1_700_000_000L

        val TITLES = listOf("Design Team", "Weekend Hikers", "Book Club", "Project Phoenix")

        /** Mirror ChatListScreen / ChatRoomItemView / ChatListSelectionAppBar test tags. */
        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
        const val SELECTION_BAR_TAG = "chat_list_screen:selection_bar"

        /** Mirror CommonMenuAction.SelectAll / More test tags rendered in the selection app bar. */
        const val SELECT_ALL_TAG = "node_selection_action:select_all"
        const val MORE_TAG = "node_selection_action:more"
    }
}
