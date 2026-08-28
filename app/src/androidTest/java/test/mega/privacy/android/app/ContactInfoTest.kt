package mega.privacy.android.app

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.boot.TestAppBoot
import mega.privacy.android.app.di.FakeFeatureFlagValueProvider
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.app.main.megachat.chat.explorer.ChatExplorerActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoComposeActivity
import mega.privacy.android.app.presentation.meeting.chat.ChatActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatMessage
import mega.privacy.android.data.test.stub.StubMegaChatMessageUser
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaNode
import mega.privacy.android.data.test.stub.StubMegaPushNotificationSettings
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.data.test.stub.StubMegaUser
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.navigation.destination.ChatNavKey
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
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
 * Full-app instrumented port of the AAT feature `sm17 ChatRoom_ContactInfo` — the contact info
 * scenarios reached from a one-to-one chat room. Built on the `:data-test` fake-SDK framework
 * following [ChatRoomTest] and [ContactInfoBackNavigationTest]: the whole app runs as in
 * production with only the SDK gateways faked, the `ContactComposeFeature` flag is forced on so
 * the shipped Compose contact-info screen (feature/contact) renders, and the UI is driven with
 * UiAutomator.
 *
 * How contact info is opened: a one-to-one chat room is seeded into the fake chat state and the
 * peer contact into the fake api gateway. The layout scenario (tc01) travels the production
 * journey — [ChatActivity] three-dot menu → Info → [ContactInfoComposeActivity] — while the other
 * scenarios launch [ContactInfoComposeActivity] directly with the seeded chat id (seed-then-
 * navigate). The activity hosts the full navigation graph, so in-screen navigation (send message,
 * verify credentials, manage chat history, share contact) runs the production destinations.
 *
 * [ContactInfoComposeActivity] does not set `testTagsAsResourceId`, so the contact-info screen is
 * matched by its rendered texts; downstream screens that set the flag themselves (the chat view,
 * manage chat history) are matched by test tags as resource ids. Gateway commands dispatched from
 * `viewModelScope.launch` are polled with [awaitInvocations]. Live data changes the SDK would push
 * (nickname update, retention time update) are simulated through the fakes' emit helpers so the
 * re-rendered state is asserted, matching the AAT scenarios' visible outcomes.
 *
 * tc02/tc15 (share a folder from contact info) are not ported: the shipped Compose contact-info
 * screen has no share-folder action (superseded by the send-file toolbar action); folder sharing
 * is exercised from the node context menus instead. tc05/tc06 (live call journeys) are out of
 * scope. tc11 asserts the navigation into the shared-files list only, because the target
 * NodeAttachmentHistoryActivity is a legacy PasscodeActivity that talks to the SDK directly and
 * cannot run under the fakes. tc13 asserts the gateway call and the rendered confirmation; the
 * fake does not simulate the SDK's history-truncate follow-up, so the emptied chat itself is not
 * re-rendered.
 */
@HiltAndroidTest
class ContactInfoTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeApi: FakeMegaApiGateway

    @Inject
    lateinit var fakeMegaChat: FakeMegaChatApiGateway

    @Inject
    lateinit var fakeFlags: FakeFeatureFlagValueProvider

    @Inject
    lateinit var activityLifecycleHandler: ActivityLifecycleHandler

    @Inject
    lateinit var saveAccountCredentialsUseCase: SaveAccountCredentialsUseCase

    @Inject
    lateinit var getSpecificAccountDetailUseCase: GetSpecificAccountDetailUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private var scenario: ActivityScenario<out Activity>? = null

    private val contact = StubMegaUser(email = CONTACT_EMAIL, handle = PEER_HANDLE)

    private val getInSharesByUserRef: KSuspendFunction2<MegaApiGateway, MegaUser, List<MegaNode>> =
        MegaApiGateway::getInShares

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

        // The fakes are shared singletons across tests in the process; start from pristine fakes,
        // then seed the one-to-one chat room and the peer contact this suite opens.
        fakeApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
        fakeMegaChat.chatState.chatRooms[CHAT_ID] = oneToOneChatRoom()
        fakeApi.stub(MegaApiGateway::getContacts) { listOf(contact) }
        fakeApi.stub(MegaApiGateway::getContact) { contact }
        fakeMegaChat.stub(
            MegaChatApiGateway::getUserFullNameFromCache,
            matcher = { it.getOrNull(0) == PEER_HANDLE },
        ) { CONTACT_NAME }
        // No alias is set until a scenario sets one; the alias read must fail cleanly so the
        // contact resolves without a nickname.
        fakeApi.stubRequest(
            MegaApiGateway::getUserAlias,
            error = StubMegaError(MegaError.API_ENOENT),
        )
        fakeApi.clearInvocations()
        fakeMegaChat.clearInvocations()

        // Persist a logged-in session through the app's real credentials path; account details are
        // fetched eagerly because account-detail dependent features wait for a non-empty value.
        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        (targetContext.applicationContext as Application)
            .unregisterActivityLifecycleCallbacks(activityLifecycleHandler)
        fakeApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
    }

    // region tests

    /**
     * tc01 — opening contact info from the chat room's three-dot Info option renders the
     * contact's name, email, the chat action bar and every option row of the seeded state:
     * nickname, credentials (not verified), share contact, incoming shares (0 folders),
     * chat notifications, shared files, manage chat history and remove contact.
     */
    @Test
    fun contactInfoRendersFromChatRoomInfoOption() {
        openContactInfoFromChatRoom()

        awaitObject(By.text(CONTACT_NAME), LOAD_TIMEOUT, "contact-name")
        awaitObject(By.text(CONTACT_EMAIL), LOAD_TIMEOUT, "contact-email")
        awaitObject(By.text(messageActionText()), LOAD_TIMEOUT, "message-action")
        awaitObject(By.text(audioCallActionText()), LOAD_TIMEOUT, "audio-call-action")
        awaitObject(By.text(videoCallActionText()), LOAD_TIMEOUT, "video-call-action")
        awaitObject(By.text(setNicknameText()), LOAD_TIMEOUT, "set-nickname-row")
        awaitObject(By.text(verifyCredentialsText()), LOAD_TIMEOUT, "verify-credentials-row")
        awaitObject(By.text(notVerifiedText()), LOAD_TIMEOUT, "credentials-not-verified")
        awaitObject(By.text(shareContactText()), LOAD_TIMEOUT, "share-contact-row")
        scrollToText(incomingSharesText())
        awaitObject(By.text(incomingSharesText()), LOAD_TIMEOUT, "incoming-shares-row")
        awaitObject(By.text(folderCountText(0)), LOAD_TIMEOUT, "incoming-shares-count")
        scrollToText(chatNotificationsText())
        awaitObject(By.text(chatNotificationsText()), LOAD_TIMEOUT, "chat-notifications-row")
        scrollToText(removeContactText())
        awaitObject(By.text(sharedFilesText()), LOAD_TIMEOUT, "shared-files-row")
        awaitObject(By.text(manageChatHistoryText()), LOAD_TIMEOUT, "manage-chat-history-row")
        awaitObject(By.text(removeContactText()), LOAD_TIMEOUT, "remove-contact-row")
    }

    /**
     * tc03 — setting a nickname requests it through the gateway and, once the SDK-side user
     * attribute update is simulated, renders the nickname on the header; removing it clears the
     * rendered nickname again. Carries short holds so the flow records cleanly as a demo clip.
     */
    @Test
    fun setAndRemoveNickname() {
        openContactInfo()
        Thread.sleep(CAMERA_HOLD_MS)

        clickText(setNicknameText(), "set-nickname-row")
        awaitObject(By.text(nicknameDialogSetButtonText()), LOAD_TIMEOUT, "nickname-dialog")
        fakeApi.stubRequest(
            MegaApiGateway::setUserAlias,
            request = StubMegaRequest(name = NICKNAME, text = NICKNAME),
        )
        awaitObject(By.clazz(EDIT_TEXT_CLASS), LOAD_TIMEOUT, "nickname-input")
        device.findObject(By.clazz(EDIT_TEXT_CLASS)).text = NICKNAME
        clickText(nicknameDialogSetButtonText(), "nickname-dialog-set-button")

        awaitInvocations("setUserAlias($PEER_HANDLE, \"$NICKNAME\")") {
            fakeApi.invocations.filter {
                it.methodName == "setUserAlias" &&
                        it.arguments.getOrNull(0) == PEER_HANDLE &&
                        it.arguments.getOrNull(1) == NICKNAME
            }
        }
        awaitObject(By.text(nicknameAddedText()), LOAD_TIMEOUT, "nickname-added-snackbar")

        // The real SDK confirms the alias change with an own-user attribute update, after which
        // the app refetches the alias; simulate both so the rendered name changes.
        fakeApi.stubRequest(
            MegaApiGateway::getUserAlias,
            request = StubMegaRequest(name = NICKNAME),
        )
        emitOwnUserAliasUpdate()
        awaitObject(By.text(NICKNAME), LOAD_TIMEOUT, "rendered-nickname")
        Thread.sleep(CAMERA_HOLD_MS)

        clickText(editNicknameText(), "edit-nickname-row")
        awaitObject(By.text(nicknameDialogSetButtonText()), LOAD_TIMEOUT, "edit-nickname-dialog")
        fakeApi.stubRequest(
            MegaApiGateway::setUserAlias,
            request = StubMegaRequest(),
        )
        clickText(removeText(), "nickname-dialog-remove-button")

        awaitInvocations("setUserAlias($PEER_HANDLE, null)") {
            fakeApi.invocations.filter {
                it.methodName == "setUserAlias" &&
                        it.arguments.getOrNull(0) == PEER_HANDLE &&
                        it.arguments.getOrNull(1) == null
            }
        }
        awaitObject(By.text(nicknameRemovedText()), LOAD_TIMEOUT, "nickname-removed-snackbar")

        fakeApi.stubRequest(
            MegaApiGateway::getUserAlias,
            error = StubMegaError(MegaError.API_ENOENT),
        )
        emitOwnUserAliasUpdate()
        awaitGone(By.text(NICKNAME), "rendered-nickname-removed")
        awaitObject(By.text(setNicknameText()), LOAD_TIMEOUT, "set-nickname-row-restored")
        Thread.sleep(FINAL_CAMERA_HOLD_MS)
    }

    /**
     * tc04 — the Message action opens the seeded one-to-one chat room, where a sent message goes
     * through the chat gateway and renders as a message row.
     */
    @Test
    fun sendMessageFromContactInfoOpensChat() {
        openContactInfo()

        // Only the chip's icon circle is clickable and it exposes the action as content-desc;
        // the label text below it is outside the clickable area.
        click(By.desc(messageActionText()), "message-action")

        // The chat screen sets testTagsAsResourceId itself, so its tags resolve as resource ids.
        awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LAUNCH_TIMEOUT, "chat-input-field")
        device.findObject(By.res(CHAT_TEXT_FIELD_TAG)).text = CHAT_MESSAGE
        click(By.res(SEND_ICON_TAG), "send-icon")

        awaitInvocations("sendMessage($CHAT_ID, \"$CHAT_MESSAGE\")") {
            fakeMegaChat.invocations.filter {
                it.methodName == "sendMessage" &&
                        it.arguments.getOrNull(0) == CHAT_ID &&
                        it.arguments.getOrNull(1) == CHAT_MESSAGE
            }
        }
        awaitGone(By.res(CHAT_TEXT_FIELD_TAG).text(CHAT_MESSAGE), "chat-input-cleared")
        awaitObject(By.text(CHAT_MESSAGE), LOAD_TIMEOUT, "sent-message-row")
    }

    /**
     * tc07 — a folder the contact shares with the user is counted on the incoming shares row.
     */
    @Test
    fun incomingSharesFromContactListed() {
        fakeApi.stub(getInSharesByUserRef) {
            listOf(StubMegaNode(handle = IN_SHARE_HANDLE, name = IN_SHARE_FOLDER, isFolder = true))
        }

        openContactInfo()

        scrollToText(incomingSharesText())
        awaitObject(By.text(incomingSharesText()), LOAD_TIMEOUT, "incoming-shares-row")
        awaitObject(By.text(folderCountText(1)), LOAD_TIMEOUT, "incoming-shares-count")
    }

    /**
     * tc08 — toggling chat notifications offers the mute options (30 minutes / 1 hour / 6 hours /
     * 24 hours / until turned back on), and confirming one pushes the updated notification
     * settings through the gateway.
     */
    @Test
    fun muteNotificationOptionsRenderAndMuteForOneHour() {
        fakeApi.stubRequest(
            MegaApiGateway::setPushNotificationSettings,
            request = StubMegaRequest(
                megaPushNotificationSettings = StubMegaPushNotificationSettings(),
            ),
        )
        openContactInfo()

        scrollToText(chatNotificationsText())
        click(By.checkable(true), "chat-notifications-toggle")

        awaitObject(By.text(muteDialogTitleText()), LOAD_TIMEOUT, "mute-dialog-title")
        awaitObject(By.text(muteMinutesOptionText(30)), LOAD_TIMEOUT, "mute-30-minutes-option")
        awaitObject(By.text(muteHoursOptionText(1)), LOAD_TIMEOUT, "mute-1-hour-option")
        awaitObject(By.text(muteHoursOptionText(6)), LOAD_TIMEOUT, "mute-6-hours-option")
        awaitObject(By.text(muteHoursOptionText(24)), LOAD_TIMEOUT, "mute-24-hours-option")
        awaitObject(By.text(muteUntilTurnBackOnText()), LOAD_TIMEOUT, "mute-until-turned-back-on")
        awaitObject(By.text(cancelText()), LOAD_TIMEOUT, "mute-dialog-cancel")

        clickText(muteHoursOptionText(1), "mute-1-hour-option")
        clickText(okText(), "mute-dialog-ok")

        awaitInvocations("setPushNotificationSettings") {
            fakeApi.invocations.filter { it.methodName == "setPushNotificationSettings" }
        }
    }

    /**
     * tc09 — sharing the contact into a chat attaches the contact through the chat gateway and,
     * following the confirmation snackbar's See action into the chat, renders the contact
     * attachment row. The chat picker is a legacy activity that talks to the SDK directly, so it
     * is stubbed with Espresso-Intents as if the user had picked the seeded chat.
     */
    @Test
    fun shareContactIntoChatRendersAttachmentRow() {
        fakeMegaChat.stubResult(
            MegaChatApiGateway::attachContacts,
            StubMegaChatMessage(
                msgId = ATTACH_MSG_ID,
                tempId = ATTACH_MSG_ID,
                userHandle = fakeApi.account.myUserHandle,
                type = MegaChatMessage.TYPE_CONTACT_ATTACHMENT,
                status = MegaChatMessage.STATUS_SENDING,
                timestamp = System.currentTimeMillis() / 1000,
                users = listOf(
                    StubMegaChatMessageUser(
                        handle = PEER_HANDLE,
                        name = CONTACT_NAME,
                        email = CONTACT_EMAIL,
                    ),
                ),
            ),
        )
        Intents.init()
        try {
            intending(hasComponent(ChatExplorerActivity::class.java.name)).respondWith(
                Instrumentation.ActivityResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(Constants.SELECTED_CHATS, longArrayOf(CHAT_ID)),
                ),
            )
            openContactInfo()

            clickText(shareContactText(), "share-contact-row")

            awaitInvocations("attachContacts($CHAT_ID)") {
                fakeMegaChat.invocations.filter {
                    it.methodName == "attachContacts" && it.arguments.getOrNull(0) == CHAT_ID
                }
            }
            awaitObject(By.text(sentAsMessageText()), LOAD_TIMEOUT, "sent-as-message-snackbar")
            clickText(seeText(), "snackbar-see-action")

            // The attached contact was stored through the production attach path, so the chat
            // renders it as a contact attachment row carrying the contact's email.
            awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LAUNCH_TIMEOUT, "chat-opened")
            awaitObject(By.text(CONTACT_EMAIL), LOAD_TIMEOUT, "contact-attachment-row")
        } finally {
            Intents.release()
        }
    }

    /**
     * tc10 — the credentials screen verifies and resets the contact's credentials through the
     * gateway, rendering the verified state (Reset action) in between; a contact whose
     * credentials are verified renders the Verified state on contact info.
     */
    @Test
    fun verifyAndUnverifyCredentials() {
        fakeApi.stubRequest(
            MegaApiGateway::getUserCredentials,
            request = StubMegaRequest(password = CONTACT_CREDENTIALS),
        )
        openContactInfo()

        awaitObject(By.text(notVerifiedText()), LOAD_TIMEOUT, "credentials-not-verified")
        clickText(verifyCredentialsText(), "verify-credentials-row")

        awaitObject(By.text(markAsVerifiedText()), LAUNCH_TIMEOUT, "approve-button")
        clickText(markAsVerifiedText(), "approve-button")
        awaitInvocations("verifyCredentials") {
            fakeApi.invocations.filter { it.methodName == "verifyCredentials" }
        }
        awaitObject(By.text(resetText()), LOAD_TIMEOUT, "reset-button")

        clickText(resetText(), "reset-button")
        awaitInvocations("resetCredentials") {
            fakeApi.invocations.filter { it.methodName == "resetCredentials" }
        }
        awaitObject(By.text(markAsVerifiedText()), LOAD_TIMEOUT, "approve-button-restored")

        // A contact resolved with verified credentials renders the Verified state on contact info.
        scenario?.close()
        fakeApi.stub(MegaApiGateway::areCredentialsVerified) { true }
        openContactInfo()
        awaitObject(By.text(verifiedText()), LOAD_TIMEOUT, "credentials-verified")
    }

    /**
     * tc11 — the shared files row navigates into the chat's shared-files list, carrying the
     * seeded chat id. The target NodeAttachmentHistoryActivity is a legacy PasscodeActivity that
     * talks to the SDK directly, so the launch is captured instead of rendered.
     */
    @Test
    fun sharedFilesRowOpensSharedFilesList() {
        openContactInfo()
        scrollToText(sharedFilesText())

        Intents.init()
        try {
            intending(hasComponent(NODE_ATTACHMENT_HISTORY_ACTIVITY)).respondWith(
                Instrumentation.ActivityResult(Activity.RESULT_CANCELED, null),
            )
            clickText(sharedFilesText(), "shared-files-row")
            awaitIntended(
                "shared-files-launch",
                hasComponent(NODE_ATTACHMENT_HISTORY_ACTIVITY),
                hasExtra("chatId", CHAT_ID),
            )
        } finally {
            Intents.release()
        }
    }

    /**
     * tc12 — enabling history clearing with the One day option requests the retention time
     * through the chat gateway and, once the SDK-side retention time update is simulated, renders
     * the retention period on the manage chat history screen.
     */
    @Test
    fun setHistoryClearingRetentionTimeToOneDay() {
        openContactInfo()
        scrollToText(manageChatHistoryText())
        clickText(manageChatHistoryText(), "manage-chat-history-row")

        awaitObject(By.res(HISTORY_CLEARING_SWITCH_TAG), LAUNCH_TIMEOUT, "history-clearing-switch")
        click(By.res(HISTORY_CLEARING_SWITCH_TAG), "history-clearing-switch")

        awaitObject(By.text(oneDayOptionText()), LOAD_TIMEOUT, "one-day-option")
        clickText(oneDayOptionText(), "one-day-option")
        clickText(retentionOkText(), "retention-dialog-ok")

        awaitInvocations("setChatRetentionTime($CHAT_ID, $SECONDS_IN_DAY)") {
            fakeMegaChat.invocations.filter {
                it.methodName == "setChatRetentionTime" &&
                        it.arguments.getOrNull(0) == CHAT_ID &&
                        it.arguments.getOrNull(1) == SECONDS_IN_DAY
            }
        }

        // The real SDK confirms the change with a chat room retention-time update; simulate it so
        // the rendered retention period updates.
        runBlocking {
            fakeMegaChat.emitChatRoomUpdate(
                CHAT_ID,
                ChatRoomUpdate.OnChatRoomUpdate(
                    StubMegaChatRoom(
                        chatId = CHAT_ID,
                        title = CONTACT_NAME,
                        isGroup = false,
                        peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
                        retentionTime = SECONDS_IN_DAY,
                        changes = MegaChatRoom.CHANGE_TYPE_RETENTION_TIME,
                    ),
                ),
            )
        }
        awaitObject(By.text(oneDayRetentionText()), LOAD_TIMEOUT, "rendered-retention-time")
    }

    /**
     * tc13 — clearing the chat history from the manage chat history screen requests it through
     * the chat gateway (via a confirmation dialog) and renders the cleared confirmation.
     */
    @Test
    fun clearChatHistoryFromManageChatHistory() {
        openContactInfo()
        scrollToText(manageChatHistoryText())
        clickText(manageChatHistoryText(), "manage-chat-history-row")

        awaitObject(By.res(CLEAR_HISTORY_OPTION_TAG), LAUNCH_TIMEOUT, "clear-history-option")
        click(By.res(CLEAR_HISTORY_OPTION_TAG), "clear-history-option")

        awaitObject(By.text(clearText()), LOAD_TIMEOUT, "clear-confirmation-dialog")
        clickText(clearText(), "clear-confirm-button")

        awaitInvocations("clearChatHistory($CHAT_ID)") {
            fakeMegaChat.invocations.filter {
                it.methodName == "clearChatHistory" && it.arguments.getOrNull(0) == CHAT_ID
            }
        }
        awaitObject(By.text(historyClearedText()), LOAD_TIMEOUT, "history-cleared-snackbar")
    }

    /**
     * tc14 — removing the contact requests the removal through the gateway (via a confirmation
     * dialog) and closes the contact info screen once the removal succeeds.
     */
    @Test
    fun removeContactClosesContactInfo() {
        openContactInfo()

        scrollToText(removeContactText())
        clickText(removeContactText(), "remove-contact-row")
        awaitObject(By.text(removeContactDialogTitleText()), LOAD_TIMEOUT, "remove-contact-dialog")
        clickText(removeText(), "remove-contact-confirm-button")

        awaitInvocations("removeContact") {
            fakeApi.invocations.filter { it.methodName == "removeContact" }
        }
        awaitGone(By.text(contactInfoTitleText()), "contact-info-closed")
    }

    // endregion

    // region navigation & seeding helpers

    /** Travels the production journey: seeded chat room → three-dot menu → Info → contact info. */
    private fun openContactInfoFromChatRoom() {
        val intent = Intent(targetContext, ChatActivity::class.java)
            .putExtra(ChatNavKey.LEGACY_CHAT_ID, CHAT_ID)
        scenario = ActivityScenario.launch<ChatActivity>(intent)
        awaitObject(By.res(SHOW_MORE_TAG), LAUNCH_TIMEOUT, "chat-room-loaded")
        click(By.res(SHOW_MORE_TAG), "options-overflow-button")
        clickText(infoText(), "info-option")
        awaitContactInfoLoaded()
    }

    /** Launches the Compose contact-info host directly with the seeded chat id. */
    private fun openContactInfo() {
        val intent = Intent(targetContext, ContactInfoComposeActivity::class.java)
            .putExtra(Constants.HANDLE, CHAT_ID)
        scenario = ActivityScenario.launch<ContactInfoComposeActivity>(intent)
        awaitContactInfoLoaded()
    }

    private fun awaitContactInfoLoaded() {
        awaitObject(By.text(contactInfoTitleText()), LAUNCH_TIMEOUT, "contact-info-title")
        awaitObject(By.text(CONTACT_EMAIL), LAUNCH_TIMEOUT, "contact-info-loaded")
    }

    private fun oneToOneChatRoom() = StubMegaChatRoom(
        chatId = CHAT_ID,
        title = CONTACT_NAME,
        ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
        peers = listOf(PEER_HANDLE to MegaChatRoom.PRIV_STANDARD),
        isGroup = false,
        isActive = true,
        isArchived = false,
    )

    /**
     * Emit the own-user alias attribute update the SDK pushes after an alias change; the app
     * refetches the alias (stubbed per scenario) and re-renders the contact.
     */
    private fun emitOwnUserAliasUpdate() {
        runBlocking {
            fakeApi.emitGlobalUpdate(
                GlobalUpdate.OnUsersUpdate(
                    arrayListOf(
                        StubMegaUser(
                            email = fakeApi.account.email,
                            handle = fakeApi.account.myUserHandle,
                            changes = MegaUser.CHANGE_TYPE_ALIAS.toLong(),
                        ),
                    ),
                ),
            )
        }
    }

    // endregion

    // region string resolution (the contact-info screen is matched by rendered texts)

    private fun contactInfoTitleText() =
        targetContext.getString(sharedR.string.contacts_action_contact_info)

    private fun messageActionText() =
        targetContext.getString(sharedR.string.contacts_action_send_message)

    private fun audioCallActionText() =
        targetContext.getString(sharedR.string.contacts_action_audio_call)

    private fun videoCallActionText() =
        targetContext.getString(sharedR.string.contacts_action_video_call)

    private fun setNicknameText() =
        targetContext.getString(sharedR.string.contact_info_set_nickname)

    private fun editNicknameText() =
        targetContext.getString(sharedR.string.contact_info_edit_nickname)

    private fun nicknameDialogSetButtonText() =
        targetContext.getString(sharedR.string.contact_info_nickname_dialog_set_button)

    private fun nicknameAddedText() =
        targetContext.getString(sharedR.string.contact_info_nickname_added)

    private fun nicknameRemovedText() =
        targetContext.getString(sharedR.string.contact_info_nickname_removed)

    private fun verifyCredentialsText() =
        targetContext.getString(sharedR.string.contact_info_verify_credentials)

    private fun notVerifiedText() =
        targetContext.getString(sharedR.string.contact_info_credentials_not_verified)

    private fun verifiedText() =
        targetContext.getString(sharedR.string.contact_info_credentials_verified)

    private fun shareContactText() =
        targetContext.getString(sharedR.string.contact_info_share_contact)

    private fun incomingSharesText() =
        targetContext.getString(sharedR.string.general_title_incoming_shares)

    private fun folderCountText(count: Int) = targetContext.resources.getQuantityString(
        sharedR.plurals.num_of_folders_with_parameter, count, count,
    )

    private fun chatNotificationsText() =
        targetContext.getString(sharedR.string.contact_info_chat_notifications)

    private fun muteDialogTitleText() =
        targetContext.getString(sharedR.string.contact_info_mute_notifications_dialog_title)

    private fun muteMinutesOptionText(minutes: Int) = targetContext.resources.getQuantityString(
        sharedR.plurals.contact_info_mute_option_minutes, minutes, minutes,
    )

    private fun muteHoursOptionText(hours: Int) = targetContext.resources.getQuantityString(
        sharedR.plurals.contact_info_mute_option_hours, hours, hours,
    )

    private fun muteUntilTurnBackOnText() =
        targetContext.getString(sharedR.string.contact_info_mute_option_until_turned_back_on)

    private fun sharedFilesText() =
        targetContext.getString(sharedR.string.contact_info_shared_files)

    private fun manageChatHistoryText() =
        targetContext.getString(sharedR.string.contact_info_manage_chat_history)

    private fun removeContactText() =
        targetContext.getString(sharedR.string.contacts_action_remove_contact)

    private fun removeContactDialogTitleText() =
        targetContext.getString(sharedR.string.contact_info_remove_contact_dialog_title)

    private fun removeText() = targetContext.getString(sharedR.string.general_remove)

    private fun cancelText() =
        targetContext.getString(sharedR.string.general_dialog_cancel_button)

    private fun okText() = targetContext.getString(sharedR.string.general_ok_only)

    private fun retentionOkText() = targetContext.getString(sharedR.string.general_ok)

    private fun clearText() = targetContext.getString(sharedR.string.general_clear)

    private fun infoText() = targetContext.getString(R.string.general_info)

    private fun sentAsMessageText() = targetContext.getString(R.string.sent_as_message)

    private fun seeText() = targetContext.getString(R.string.action_see)

    private fun markAsVerifiedText() =
        targetContext.getString(R.string.contact_verify_credentials_mark_as_verified_text)

    private fun resetText() = targetContext.getString(R.string.action_reset)

    private fun oneDayOptionText() =
        targetContext.getString(R.string.history_retention_option_one_day)

    private fun oneDayRetentionText() = targetContext.resources.getQuantityString(
        R.plurals.label_time_in_days_full, 1, 1,
    )

    private fun historyClearedText() = targetContext.getString(R.string.clear_history_success)

    // endregion

    // region UiAutomator helpers

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name)
        device.findObject(selector).click()
    }

    private fun clickText(text: String, name: String) = click(By.text(text), name)

    /**
     * Scrolls the first scrollable container until [text] is on screen. No-op when it is already
     * visible or nothing is scrollable.
     */
    private fun scrollToText(text: String) {
        if (device.hasObject(By.text(text))) return
        runCatching {
            UiScrollable(UiSelector().scrollable(true))
                .apply { setMaxSearchSwipes(MAX_SCROLL_SWIPES) }
                .scrollTextIntoView(text)
        }
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
     * recorded synchronously after the UI action that triggers them. Poll [supplier] until at
     * least one invocation matches, or fail with what was actually recorded.
     */
    private fun <T> awaitInvocations(name: String, supplier: () -> List<T>): List<T> {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        while (SystemClock.uptimeMillis() < deadline) {
            val matches = supplier()
            if (matches.isNotEmpty()) return matches
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError(
            "Timed out after ${LOAD_TIMEOUT}ms waiting for an invocation of $name; " +
                    "recorded api=${fakeApi.invocations.map { it.methodName }} " +
                    "chat=${fakeMegaChat.invocations.map { it.methodName }}",
        )
    }

    /** Polls Espresso-Intents until every [matchers] entry has been intended, like [awaitObject]. */
    private fun awaitIntended(
        name: String,
        vararg matchers: org.hamcrest.Matcher<Intent>,
    ) {
        val deadline = SystemClock.uptimeMillis() + LOAD_TIMEOUT
        var lastError: AssertionError? = null
        while (SystemClock.uptimeMillis() < deadline) {
            try {
                matchers.forEach { Intents.intended(it) }
                return
            } catch (e: AssertionError) {
                lastError = e
                Thread.sleep(POLL_INTERVAL_MS)
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
        const val MAX_SCROLL_SWIPES = 10

        /** Demo-clip holds carried by the nickname scenario. */
        const val CAMERA_HOLD_MS = 1_500L
        const val FINAL_CAMERA_HOLD_MS = 2_000L

        const val CHAT_ID = 1_201L
        const val PEER_HANDLE = 2_302L
        const val CONTACT_NAME = "sm17 contact"
        const val CONTACT_EMAIL = "sm17contact@mega.nz"
        const val NICKNAME = "mega chat android"
        const val CHAT_MESSAGE = "sm17tc04 msg"
        const val IN_SHARE_HANDLE = 30L
        const val IN_SHARE_FOLDER = "f-sm17tc07"
        const val ATTACH_MSG_ID = 77L
        const val SECONDS_IN_DAY = 86_400L

        /** Credentials are only rendered when the string is 40 characters long. */
        const val CONTACT_CREDENTIALS = "0123456789ABCDEF0123456789ABCDEF01234567"

        const val EDIT_TEXT_CLASS = "android.widget.EditText"

        /** Mirrors TAG_MENU_ACTIONS_SHOW_MORE of the chat app bar overflow button. */
        const val SHOW_MORE_TAG = "menuActionsShowMore"

        /** Mirrors CHAT_TEXT_FIELD_TEXT_TAG / TEST_TAG_SEND_ICON of the chat input toolbar. */
        const val CHAT_TEXT_FIELD_TAG = "chat_text_field"
        const val SEND_ICON_TAG = "chat_input_text_toolbar:send_icon"

        /** Mirrors HISTORY_CLEARING_OPTION_SWITCH_TAG / CLEAR_HISTORY_OPTION_TAG. */
        const val HISTORY_CLEARING_SWITCH_TAG = "history_clearing_option:switch_enablement_toggle"
        const val CLEAR_HISTORY_OPTION_TAG =
            "manage_chat_history_screen:clear_history_option_the_clear_all_chat_or_meeting_history_option"

        const val NODE_ATTACHMENT_HISTORY_ACTIVITY =
            "mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity"
    }
}
