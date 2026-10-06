package mega.privacy.android.app

import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.presentation.contact.NewChatComposeActivity
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaUser
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
 * Full-app instrumented test for the Compose start conversation screen the chat list's new-chat
 * FAB opens while [ApiFeatures.ContactComposeFeature] is on.
 *
 * Each journey starts on the chat list, taps the FAB and follows one row of the start screen
 * through to where it leads: a contact opens its 1:1 chat room (the chat list resolves the result
 * through `StartConversationUseCase`, which finds the room via `getChatRoomByUser`), Note to self
 * opens the note-to-self room, Invite opens the invite screen and New group chat opens the Compose
 * create-group picker. Invite and Note to self replace the start screen, so back must land on the
 * chat list. The share/forward picker hosted by [NewChatComposeActivity] must keep the
 * multi-select picker rather than the start screen.
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListFabAndEmptyStateTest]: the whole
 * app runs as in production with only the SDK gateways faked; the UI is driven with UiAutomator,
 * and Compose test tags are matched as resource ids because MegaActivity and the chat room set
 * `testTagsAsResourceId = true`. [NewChatComposeActivity] does not, so its picker is matched by
 * text.
 */
@HiltAndroidTest
class StartConversationJourneyTest {

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

    private val alice = StubMegaUser(email = ALICE_EMAIL, handle = ALICE_HANDLE)

    private val bob = StubMegaUser(email = BOB_EMAIL, handle = BOB_HANDLE)

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

        fakeMegaApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
        seedNoteToSelfChat()

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
        fakeMegaApi.resetToDefaults()
    }

    @Test
    fun newChatFabOpensTheStartConversationScreenWithItsActionsAndContacts() {
        seedContacts()

        openStartConversationFromFab()

        awaitObject(By.text(string(sharedR.string.new_chat_title)), LOAD_TIMEOUT, "new-chat-title")
        awaitObject(
            By.res(START_CONVERSATION_NEW_GROUP_TAG).hasDescendant(
                By.text(string(sharedR.string.general_new_group_chat))
            ),
            LOAD_TIMEOUT,
            "new-group-chat-row",
        )
        awaitObject(
            By.res(START_CONVERSATION_INVITE_TAG).hasDescendant(
                By.text(string(sharedR.string.invite_contacts))
            ),
            LOAD_TIMEOUT,
            "invite-row",
        )
        awaitObject(
            By.text(string(sharedR.string.general_section_contacts)),
            LOAD_TIMEOUT,
            "contacts-header",
        )
        awaitObject(By.res(START_CONVERSATION_NOTE_TO_SELF_TAG), LOAD_TIMEOUT, "note-to-self-row")
        awaitObject(By.text(ALICE_NAME), LOAD_TIMEOUT, "alice-contact-row")
        awaitObject(By.text(BOB_NAME), LOAD_TIMEOUT, "bob-contact-row")
    }

    @Test
    fun tappingAContactOpensTheOneToOneChatRoomWithThatContact() {
        seedContacts()
        seedOneToOneChatWithAlice()

        openStartConversationFromFab(holdOnChatList = true)
        // Holds keep each state on screen long enough to read when this journey is recorded for review.
        Thread.sleep(RECORDING_HOLD_MS)

        awaitObject(By.text(ALICE_NAME), LOAD_TIMEOUT, "alice-contact-row").click()

        awaitObject(
            By.res(CHAT_APP_BAR_TITLE_TAG).text(ALICE_NAME),
            LAUNCH_TIMEOUT,
            "alice-chat-room-title",
        )
        awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LOAD_TIMEOUT, "chat-room-input-bar")
        Thread.sleep(RECORDING_HOLD_MS)
    }

    @Test
    fun noteToSelfOpensTheNoteToSelfChatAndBackReturnsToTheChatList() {
        seedContacts()

        openStartConversationFromFab()

        awaitObject(By.res(START_CONVERSATION_NOTE_TO_SELF_TAG), LOAD_TIMEOUT, "note-to-self-row")
            .click()

        awaitObject(
            By.res(CHAT_APP_BAR_TITLE_TAG).text(string(sharedR.string.chat_note_to_self_chat_title)),
            LAUNCH_TIMEOUT,
            "note-to-self-chat-room-title",
        )
        awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LOAD_TIMEOUT, "chat-room-input-bar")

        device.pressBack()
        awaitChatListWithoutStartScreen("chat-list-after-note-to-self")
    }

    @Test
    fun inviteOpensTheInviteContactsScreenAndBackReturnsToTheChatList() {
        seedContacts()

        openStartConversationFromFab()

        awaitObject(By.res(START_CONVERSATION_INVITE_TAG), LOAD_TIMEOUT, "invite-row").click()
        awaitObject(By.res(INVITE_CONTACTS_SCREEN_TAG), LOAD_TIMEOUT, "invite-contacts-screen")

        device.pressBack()
        awaitChatListWithoutStartScreen("chat-list-after-invite")
    }

    @Test
    fun newGroupChatOpensTheComposeCreateGroupScreen() {
        seedContacts()

        openStartConversationFromFab()

        awaitObject(By.res(START_CONVERSATION_NEW_GROUP_TAG), LOAD_TIMEOUT, "new-group-chat-row")
            .click()

        awaitObject(By.res(CREATE_GROUP_CHAT_SCREEN_TAG), LOAD_TIMEOUT, "create-group-chat-screen")
        awaitObject(
            By.res(CREATE_GROUP_CHAT_SCREEN_TAG).hasDescendant(By.text(ALICE_NAME)),
            LOAD_TIMEOUT,
            "create-group-chat-alice",
        )
    }

    @Test
    fun shareAndForwardNewChatPickerStaysTheMultiSelectPicker() {
        seedContacts()

        ActivityScenario.launch<NewChatComposeActivity>(
            NewChatComposeActivity.getIntent(targetContext)
        )

        awaitObject(By.text(string(sharedR.string.new_chat_title)), LAUNCH_TIMEOUT, "new-chat-picker")
        awaitObject(By.text(ALICE_NAME), LOAD_TIMEOUT, "picker-alice")
        assertAbsent(By.text(string(sharedR.string.general_new_group_chat)), "new-group-chat-row")
        assertAbsent(
            By.text(string(sharedR.string.chat_note_to_self_chat_title)),
            "note-to-self-row",
        )

        // Tapping a contact selects it and offers the confirm action instead of leaving the picker.
        device.findObject(By.text(ALICE_NAME)).click()
        awaitObject(
            By.desc(string(sharedR.string.general_confirm_button)),
            LOAD_TIMEOUT,
            "picker-confirm-fab",
        )
        awaitObject(By.text(BOB_NAME), LOAD_TIMEOUT, "picker-still-open")
    }

    @Test
    fun withoutContactsTheInviteEmptyStateIsShownAndTheInviteRowIsAbsent() {
        openStartConversationFromFab()

        awaitObject(
            By.res(START_CONVERSATION_EMPTY_CONTACTS_TAG),
            LOAD_TIMEOUT,
            "empty-contacts-state",
        )
        awaitObject(
            By.text(string(sharedR.string.invite_contacts_to_start_chat_title)),
            LOAD_TIMEOUT,
            "empty-contacts-title",
        )
        awaitObject(By.res(START_CONVERSATION_EMPTY_INVITE_TAG), LOAD_TIMEOUT, "empty-invite-button")
        awaitObject(By.res(START_CONVERSATION_NEW_GROUP_TAG), LOAD_TIMEOUT, "new-group-chat-row")
        assertAbsent(By.res(START_CONVERSATION_INVITE_TAG), "invite-row")
    }

    private fun openStartConversationFromFab(holdOnChatList: Boolean = false) {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        val fab = awaitObject(By.res(CHAT_LIST_FAB_TAG), LAUNCH_TIMEOUT, "new-chat-fab")
        if (holdOnChatList) Thread.sleep(RECORDING_HOLD_MS)
        fab.click()
        awaitObject(By.res(START_CONVERSATION_LIST_TAG), LOAD_TIMEOUT, "start-conversation-list")
    }

    private fun awaitChatListWithoutStartScreen(name: String) {
        awaitObject(By.res(CHAT_LIST_SCREEN_TAG), LOAD_TIMEOUT, name)
        assertAbsent(By.res(START_CONVERSATION_SCREEN_TAG), "$name-start-screen")
    }

    private fun seedContacts() {
        fakeMegaApi.stub(MegaApiGateway::getContacts) { listOf(alice, bob) }
        fakeMegaApi.stub(MegaApiGateway::getContact) { arguments ->
            when (arguments.firstOrNull()) {
                ALICE_EMAIL -> alice
                BOB_EMAIL -> bob
                else -> null
            }
        }
        fakeMegaChat.stub(MegaChatApiGateway::getUserFullNameFromCache) { arguments ->
            when (arguments.firstOrNull()) {
                ALICE_HANDLE -> ALICE_NAME
                BOB_HANDLE -> BOB_NAME
                else -> null
            }
        }
    }

    private fun seedOneToOneChatWithAlice() {
        val room = StubMegaChatRoom(
            chatId = ALICE_CHAT_ID,
            title = ALICE_NAME,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(ALICE_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = false,
            isActive = true,
        )
        fakeMegaChat.chatState.chatRooms[ALICE_CHAT_ID] = room
        fakeMegaChat.stub(
            MegaChatApiGateway::getChatRoomByUser,
            matcher = { it.firstOrNull() == ALICE_HANDLE },
        ) { room }
    }

    private fun seedNoteToSelfChat() {
        val room = StubMegaChatRoom(
            chatId = NOTE_TO_SELF_CHAT_ID,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            isNoteToSelf = true,
            isActive = true,
        )
        fakeMegaChat.chatState.chatRooms[NOTE_TO_SELF_CHAT_ID] = room
        fakeMegaChat.stub(MegaChatApiGateway::getNoteToSelfChat) { room }
    }

    private fun string(resId: Int): String = targetContext.getString(resId)

    private fun assertAbsent(selector: BySelector, name: String) {
        if (!device.hasObject(selector)) return
        dumpHierarchy(name)
        throw AssertionError("Expected $name ($selector) to be absent; hierarchy in logcat tag UiDump")
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String): UiObject2 {
        if (device.wait(Until.hasObject(selector), timeout)) return device.findObject(selector)
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
        const val RECORDING_HOLD_MS = 2_000L

        const val ALICE_EMAIL = "alice@mega.co.nz"
        const val ALICE_NAME = "Alice Anderson"
        const val ALICE_HANDLE = 8_801L
        const val ALICE_CHAT_ID = 8_901L
        const val BOB_EMAIL = "bob@mega.co.nz"
        const val BOB_NAME = "Bob Brown"
        const val BOB_HANDLE = 8_802L
        const val NOTE_TO_SELF_CHAT_ID = 8_999L

        /** Mirror ChatListScreen test tags. */
        const val CHAT_LIST_SCREEN_TAG = "chat_list_screen:screen"
        const val CHAT_LIST_FAB_TAG = "chat_list_screen:new_chat_fab"

        /** Mirror StartConversationScreen test tags. */
        const val START_CONVERSATION_SCREEN_TAG = "start_conversation_screen"
        const val START_CONVERSATION_LIST_TAG = "start_conversation_screen:list"
        const val START_CONVERSATION_NEW_GROUP_TAG = "start_conversation_screen:new_group_chat"
        const val START_CONVERSATION_INVITE_TAG = "start_conversation_screen:invite"
        const val START_CONVERSATION_NOTE_TO_SELF_TAG = "start_conversation_screen:note_to_self"
        const val START_CONVERSATION_EMPTY_CONTACTS_TAG = "start_conversation_screen:empty_contacts"
        const val START_CONVERSATION_EMPTY_INVITE_TAG = "start_conversation_screen:empty_invite"

        /** Mirrors CREATE_GROUP_CHAT_SCREEN_TAG of the feature/contact create-group screen. */
        const val CREATE_GROUP_CHAT_SCREEN_TAG = "create_group_chat_screen"

        /** Mirrors INVITE_CONTACTS_SCREEN_TAG of the feature/contact invite-contacts screen. */
        const val INVITE_CONTACTS_SCREEN_TAG = "invite_contacts_screen"

        /** Mirror the chat room's app bar title and CHAT_TEXT_FIELD_TEXT_TAG input field. */
        const val CHAT_APP_BAR_TITLE_TAG = "mega_app_bar_title:text"
        const val CHAT_TEXT_FIELD_TAG = "chat_text_field"
    }
}
