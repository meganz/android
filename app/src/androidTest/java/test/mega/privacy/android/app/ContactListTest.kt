package mega.privacy.android.app

import android.Manifest
import android.app.Application
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
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaApiGateway
import mega.privacy.android.data.test.gateway.FakeMegaChatApiGateway
import mega.privacy.android.data.test.stub.StubMegaChatRoom
import mega.privacy.android.data.test.stub.StubMegaContactRequest
import mega.privacy.android.data.test.stub.StubMegaError
import mega.privacy.android.data.test.stub.StubMegaHandleList
import mega.privacy.android.data.test.stub.StubMegaRequest
import mega.privacy.android.data.test.stub.StubMegaUser
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.SetHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.account.GetSpecificAccountDetailUseCase
import mega.privacy.android.domain.usecase.login.SaveAccountCredentialsUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.navigation.destination.ContactsNavKey
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaContactRequest
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.util.regex.Pattern
import javax.inject.Inject
import kotlin.reflect.KFunction3
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Full-app instrumented coverage of the Compose contact list (`:feature:contact:contact`) behind
 * [ApiFeatures.ContactComposeFeature], the flag's primary surface. Built on the `:data-test`
 * fake-SDK framework following [MenuOptionsTest] and [ChatListRowNavigationTest]: the whole app
 * runs as in production with only the SDK gateways faked, the flag is forced on, and the UI is
 * driven with UiAutomator.
 *
 * The list is reached by launching [MegaActivity] with [ContactsNavKey] as an extra destination,
 * which drives the production `contactsListDestination` gate. [MegaActivity] sets
 * `testTagsAsResourceId = true`, so the list's own tags resolve as resource ids; the contact
 * actions sheet is a ModalBottomSheet living in its own window, where that flag does not apply, so
 * its actions are matched by label instead.
 *
 * Nothing instrumented exercised this screen before: the call permission launcher and the call
 * guards wired into `ContactsEntry` are host-level composition that no ViewModel test can see, and
 * the Groups destination is reachable only from here (`ContactGroupsNavKey` is internal to the
 * feature module and cannot be emitted from `:app`).
 */
@HiltAndroidTest
class ContactListTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var fakeMegaApi: FakeMegaApiGateway

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

    @Inject
    lateinit var setHomeConfigurationTooltipShownUseCase: SetHomeConfigurationTooltipShownUseCase

    @Inject
    lateinit var setCustomiseNavigationTooltipShownUseCase: SetCustomiseNavigationTooltipShownUseCase

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private val targetContext get() = instrumentation.targetContext

    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private var scenario: ActivityScenario<MegaActivity>? = null

    private val contact = StubMegaUser(email = CONTACT_EMAIL, handle = CONTACT_HANDLE)

    /** The id-based `getUserAttribute` overload, which the storage state is read through. */
    private val getUserAttributeByIdRef:
            KFunction3<MegaApiGateway, Int, MegaRequestListenerInterface, Unit> =
        MegaApiGateway::getUserAttribute

    @Before
    fun setUp() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(Timber.DebugTree())
        }
        hiltRule.inject()

        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            Manifest.permission.POST_NOTIFICATIONS,
        )

        // The call guard scenario starts from a revoked microphone permission; a previous run in
        // the same install may have left it granted.
        instrumentation.uiAutomation.revokeRuntimePermission(
            targetContext.packageName,
            Manifest.permission.RECORD_AUDIO,
        )

        // Boot the test process through the production initialiser units. See CloudDriveUploadTest.
        TestAppBoot.runCoreInitializers()

        // Production registers the lifecycle handler in MegaApplication.onCreate, which does not
        // run under the Hilt test application; snackbar plumbing needs the current activity.
        (targetContext.applicationContext as Application)
            .registerActivityLifecycleCallbacks(activityLifecycleHandler)

        // With the flag off the destination launches the legacy ContactsActivity instead, which
        // cannot run under the Hilt test application.
        fakeFlags.set(ApiFeatures.ContactComposeFeature, true)

        // The fakes are shared singletons across tests in the process; start from pristine fakes.
        fakeMegaApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
        seedContact()

        runBlocking {
            saveAccountCredentialsUseCase()
            getSpecificAccountDetailUseCase(storage = true, transfer = true, pro = true)

            // The one-time onboarding tooltips are popups covering the screens this suite taps
            // through, so on a fresh install they swallow the taps meant for them.
            setHomeConfigurationTooltipShownUseCase()
            setCustomiseNavigationTooltipShownUseCase()
        }
    }

    @After
    fun tearDown() {
        scenario?.close()
        (targetContext.applicationContext as Application)
            .unregisterActivityLifecycleCallbacks(activityLifecycleHandler)
        fakeFlags.clear()
        fakeMegaApi.resetToDefaults()
        fakeMegaChat.resetToDefaults()
    }

    // region tests

    /**
     * The call permission launcher and the two call guards `ContactsEntry` wraps the contact list
     * in, walked in one launch:
     * - a denied microphone permission never reaches the call and reports the microphone message;
     * - with the permission granted, a call already running in another chat is refused;
     * - an account in paywall state is sent to the over disk quota paywall instead of calling.
     */
    @Test
    fun callingAContactRequestsPermissionsAndRespectsTheCallGuards() {
        seedOneToOneChatRoom()

        launchContactList()

        openContactActionsSheet()
        clickSheetAction(sharedR.string.contacts_action_audio_call, "audio-call-action")
        denySystemPermissionDialog()
        awaitObject(
            By.text(str(sharedR.string.contact_info_microphone_permission_message)),
            LOAD_TIMEOUT,
            "microphone-permission-snackbar",
        )
        awaitGone(
            By.text(str(sharedR.string.contact_info_microphone_permission_message)),
            "microphone-permission-snackbar-dismissed",
        )

        instrumentation.uiAutomation.grantRuntimePermission(
            targetContext.packageName,
            Manifest.permission.RECORD_AUDIO,
        )
        stubCallInAnotherChat()

        openContactActionsSheet()
        clickSheetAction(sharedR.string.contacts_action_audio_call, "audio-call-action-in-call")
        awaitObject(
            By.text(str(sharedR.string.chat_start_meeting_already_in_call_error)),
            LOAD_TIMEOUT,
            "already-in-a-call-snackbar",
        )
        awaitGone(
            By.text(str(sharedR.string.chat_start_meeting_already_in_call_error)),
            "already-in-a-call-snackbar-dismissed",
        )

        clearOngoingCalls()
        stubStorageStatePayWall()

        openContactActionsSheet()
        clickSheetAction(sharedR.string.contacts_action_audio_call, "audio-call-action-paywall")
        awaitObject(
            By.text(str(sharedR.string.account_over_disk_quota_paywall_title)),
            LAUNCH_TIMEOUT,
            "over-disk-quota-paywall",
        )
    }

    /**
     * Tapping a contact row opens its one-to-one chat, and the row's actions sheet opens contact
     * info.
     *
     * The contact info leg deliberately travels the gated `contactInfoDestination` ON branch
     * inside [MegaActivity] — every other contact-info test launches `ContactInfoComposeActivity`
     * directly, so this is the only coverage of that destination. Do not "simplify" it back to
     * the activity host.
     */
    @Test
    fun openingAContactOpensItsChatAndTheActionsSheetOpensContactInfo() {
        seedOneToOneChatRoom()

        launchContactList()

        click(contactRow(), "contact-row")
        awaitObject(By.res(CHAT_TEXT_FIELD_TAG), LAUNCH_TIMEOUT, "chat-room-input-bar")

        device.pressBack()
        awaitObject(By.res(CONTACT_LIST_SCREEN_TAG), LOAD_TIMEOUT, "contact-list-restored")

        openContactActionsSheet()
        clickSheetAction(sharedR.string.contacts_action_contact_info, "contact-info-action")
        awaitObject(By.res(CONTACT_INFO_SCREEN_TAG), LAUNCH_TIMEOUT, "contact-info-screen")
        awaitObject(By.text(CONTACT_EMAIL), LOAD_TIMEOUT, "contact-info-email")
    }

    /**
     * The Requests and Groups entries open their screens and back returns to the list. Both
     * entries render only once the list has a contact, and the Groups screen is reachable from
     * nowhere else: `ContactGroupsNavKey` is internal to the feature module.
     */
    @Test
    fun requestsAndGroupsEntriesOpenTheirScreens() {
        seedIncomingContactRequest()
        seedGroupChatRoom()

        launchContactList()

        click(By.res(CONTACT_LIST_REQUESTS_ACTION_TAG), "requests-entry")
        awaitObject(By.res(CONTACT_REQUESTS_SCREEN_TAG), LAUNCH_TIMEOUT, "contact-requests-screen")
        awaitObject(By.text(REQUEST_EMAIL), LOAD_TIMEOUT, "incoming-request-row")

        device.pressBack()
        awaitObject(By.res(CONTACT_LIST_SCREEN_TAG), LOAD_TIMEOUT, "contact-list-restored")

        click(By.res(CONTACT_LIST_GROUPS_ACTION_TAG), "groups-entry")
        awaitObject(By.res(CONTACT_GROUPS_SCREEN_TAG), LAUNCH_TIMEOUT, "contact-groups-screen")
        awaitObject(By.text(GROUP_TITLE), LOAD_TIMEOUT, "seeded-group-row")

        device.pressBack()
        awaitObject(By.res(CONTACT_LIST_SCREEN_TAG), LOAD_TIMEOUT, "contact-list-restored-again")
    }

    // endregion

    // region navigation helpers

    private fun launchContactList() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ContactsNavKey),
        )
        scenario = ActivityScenario.launch(intent)
        awaitObject(By.res(CONTACT_LIST_SCREEN_TAG), LAUNCH_TIMEOUT, "contact-list-screen")
        awaitObject(contactRow(), LAUNCH_TIMEOUT, "seeded-contact-row")
    }

    /**
     * The seeded contact has neither an alias nor a cached full name, so the row renders its email
     * as the display name.
     */
    private fun contactRow(): BySelector = By.text(CONTACT_EMAIL)

    private fun openContactActionsSheet() {
        click(By.res(CONTACT_ITEM_MORE_TAG), "contact-row-more-button")
    }

    /**
     * The actions sheet is a ModalBottomSheet in its own window, where [MegaActivity]'s
     * `testTagsAsResourceId` does not apply, so its actions are matched by label.
     */
    private fun clickSheetAction(labelResId: Int, name: String) {
        click(By.text(str(labelResId)), name)
    }

    /**
     * Dismisses the runtime permission dialog by denying it. The dialog does not appear when the
     * permission is already permanently denied, in which case the launcher reports the denial
     * straight away and there is nothing to dismiss.
     */
    private fun denySystemPermissionDialog() {
        val denyButton = By.res(PERMISSION_DENY_BUTTON_ID)
        if (device.wait(Until.hasObject(denyButton), PERMISSION_DIALOG_TIMEOUT)) {
            device.findObject(denyButton).click()
        }
    }

    // endregion

    // region seeding helpers

    private fun seedContact() {
        fakeMegaApi.stub(MegaApiGateway::getContacts) { listOf(contact) }
        fakeMegaApi.stub(MegaApiGateway::getContact) { contact }
        // No alias is set, so the alias read must fail cleanly for the contact to resolve.
        fakeMegaApi.stubRequest(
            MegaApiGateway::getUserAlias,
            error = StubMegaError(MegaError.API_ENOENT),
        )
    }

    private fun seedOneToOneChatRoom() {
        val chatRoom = StubMegaChatRoom(
            chatId = CHAT_ID,
            title = CONTACT_EMAIL,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(CONTACT_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = false,
            isActive = true,
        )
        fakeMegaChat.chatState.chatRooms[CHAT_ID] = chatRoom
        fakeMegaChat.stub(
            MegaChatApiGateway::getChatRoomByUser,
            matcher = { it.firstOrNull() == CONTACT_HANDLE },
        ) { chatRoom }
    }

    private fun seedGroupChatRoom() {
        fakeMegaChat.chatState.chatRooms[GROUP_CHAT_ID] = StubMegaChatRoom(
            chatId = GROUP_CHAT_ID,
            title = GROUP_TITLE,
            ownPrivilege = MegaChatRoom.PRIV_MODERATOR,
            peers = listOf(CONTACT_HANDLE to MegaChatRoom.PRIV_STANDARD),
            isGroup = true,
            isActive = true,
        )
    }

    private fun seedIncomingContactRequest() {
        fakeMegaApi.stub(MegaApiGateway::getIncomingContactRequests) {
            listOf(
                StubMegaContactRequest(
                    handle = REQUEST_HANDLE,
                    sourceEmail = REQUEST_EMAIL,
                    targetEmail = fakeMegaApi.account.email,
                    creationTime = REQUEST_TIMESTAMP,
                    modificationTime = REQUEST_TIMESTAMP,
                    status = MegaContactRequest.STATUS_UNRESOLVED,
                    isOutgoing = false,
                ),
            )
        }
    }

    /**
     * Reports an in-progress call in a chat other than the contact's, so the call tap falls
     * through the "existing call in this chat" branch into the already-in-a-call guard.
     */
    private fun stubCallInAnotherChat() {
        fakeMegaChat.stub(
            MegaChatApiGateway::getChatCalls,
            matcher = { it.firstOrNull() == MegaChatCall.CALL_STATUS_IN_PROGRESS },
        ) { StubMegaHandleList(listOf(OTHER_CHAT_ID)) }
    }

    private fun clearOngoingCalls() {
        fakeMegaChat.stub(
            MegaChatApiGateway::getChatCalls,
            matcher = { it.firstOrNull() == MegaChatCall.CALL_STATUS_IN_PROGRESS },
        ) { StubMegaHandleList() }
    }

    private fun stubStorageStatePayWall() {
        fakeMegaApi.stubRequest(
            getUserAttributeByIdRef,
            request = StubMegaRequest(
                type = MegaRequest.TYPE_GET_ATTR_USER,
                paramType = MegaApiJava.USER_ATTR_STORAGE_STATE,
                number = MegaApiJava.STORAGE_STATE_PAYWALL.toLong(),
            ),
            matcher = { it.firstOrNull() == MegaApiJava.USER_ATTR_STORAGE_STATE },
        )
    }

    // endregion

    // region UiAutomator helpers

    private fun str(resId: Int): String = targetContext.getString(resId)

    private fun click(selector: BySelector, name: String) {
        awaitObject(selector, LOAD_TIMEOUT, name).click()
    }

    private fun awaitObject(selector: BySelector, timeout: Long, name: String): UiObject2 {
        if (device.wait(Until.hasObject(selector), timeout)) return device.findObject(selector)
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${timeout}ms waiting for $name ($selector); hierarchy in logcat tag UiDump",
        )
    }

    private fun awaitGone(selector: BySelector, name: String) {
        if (device.wait(Until.gone(selector), GONE_TIMEOUT)) return
        dumpHierarchy(name)
        throw AssertionError(
            "Timed out after ${GONE_TIMEOUT}ms waiting for $name ($selector) to be gone; " +
                    "hierarchy in logcat tag UiDump",
        )
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
        const val GONE_TIMEOUT = 30_000L
        const val PERMISSION_DIALOG_TIMEOUT = 10_000L

        const val CONTACT_EMAIL = "contactlist@mega.co.nz"
        const val CONTACT_HANDLE = 2_501L
        const val CHAT_ID = 1_501L
        const val OTHER_CHAT_ID = 1_502L
        const val GROUP_CHAT_ID = 1_503L
        const val GROUP_TITLE = "Contact list group"
        const val REQUEST_EMAIL = "requester@mega.co.nz"
        const val REQUEST_HANDLE = 3_501L
        const val REQUEST_TIMESTAMP = 1_700_000_000L

        /** Mirror the CONTACT_LIST_* tags of the feature/contact contact list. */
        const val CONTACT_LIST_SCREEN_TAG = "contact_list_screen"
        const val CONTACT_LIST_REQUESTS_ACTION_TAG = "contact_list_screen:requests_action"
        const val CONTACT_LIST_GROUPS_ACTION_TAG = "contact_list_screen:groups_action"

        /** Mirror CONTACT_ITEM_VIEW_MORE of the shared contact row. */
        const val CONTACT_ITEM_MORE_TAG = "contact_item_view:more"

        /** Mirror CONTACT_REQUESTS_SCREEN_TAG / CONTACT_GROUPS_SCREEN_TAG / CONTACT_INFO_SCREEN_TAG. */
        const val CONTACT_REQUESTS_SCREEN_TAG = "contact_requests_screen"
        const val CONTACT_GROUPS_SCREEN_TAG = "contact_groups_screen"
        const val CONTACT_INFO_SCREEN_TAG = "contact_info_screen"

        /** Mirrors CHAT_TEXT_FIELD_TEXT_TAG of the chat room input toolbar. */
        const val CHAT_TEXT_FIELD_TAG = "chat_text_field"

        /** The runtime permission dialog's deny button, whose host package varies by device. */
        val PERMISSION_DENY_BUTTON_ID: Pattern =
            Pattern.compile(".*:id/permission_deny_button")
    }
}
