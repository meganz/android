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
 * Full-app instrumented test for the chat list's in-tab search.
 *
 * The search top app bar filters the rendered rows through the real
 * `ChatListViewModel.onSearchQueryChange` pipeline: a matching query narrows the tab to the
 * matching rooms, a non-matching one swaps the list for the no-results view, clearing the field
 * restores every row, and leaving search mode both restores the rows and brings the tab-aware FAB
 * back (it is hidden while searching).
 *
 * Built on the `:data-test` fake-SDK framework following [ChatListFabAndEmptyStateTest]: the whole
 * app runs as in production with only the SDK gateways faked; the UI is driven with UiAutomator,
 * and Compose test tags are matched as resource ids because MegaActivity sets
 * `testTagsAsResourceId = true`.
 */
@HiltAndroidTest
class ChatListSearchTest {

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
    fun searchNarrowsTheChatsTabAndClearingItRestoresEveryRow() {
        val intent = MegaActivity.getIntentWithExtraDestinations(
            targetContext,
            listOf(ChatListNavKey(showMeetingTab = false)),
        )
        ActivityScenario.launch<MegaActivity>(intent)

        // Every seeded room is listed and the new-chat FAB is up.
        TITLES.forEach { awaitObject(chatRow(it), LAUNCH_TIMEOUT, "chat-row-$it") }
        awaitObject(By.res(CHAT_LIST_FAB_TAG), LOAD_TIMEOUT, "new-chat-fab")

        // Entering search swaps the title for the search field and hides the FAB.
        searchAction().click()
        awaitObject(searchField(), LOAD_TIMEOUT, "search-field")
        awaitGone(By.res(CHAT_LIST_FAB_TAG), "new-chat-fab-hidden")

        // A matching query narrows the tab to the matching room only.
        typeQuery(MATCHING_QUERY)
        awaitObject(chatRow(TITLES[2]), LOAD_TIMEOUT, "filtered-chat-row")
        awaitGone(chatRow(TITLES[0]), "non-matching-chat-row")

        // Clearing the field restores every row without leaving search mode.
        device.findObject(By.desc(CLEAR_TEXT_DESC)).click()
        TITLES.forEach { awaitObject(chatRow(it), LOAD_TIMEOUT, "restored-chat-row-$it") }
        awaitObject(searchField(), LOAD_TIMEOUT, "search-field-still-open")

        // A query no room matches shows the no-results view instead of the list.
        typeQuery(NO_MATCH_QUERY)
        awaitObject(By.res(CHAT_LIST_NO_RESULTS_TAG), LOAD_TIMEOUT, "no-results-view")

        // The soft keyboard the focused field raised covers the centred message, so it is dropped
        // first — back reaches the keyboard before the screen.
        device.pressBack()
        val noResultsTitle =
            targetContext.getString(sharedR.string.chat_list_search_no_results_title)
        awaitObject(By.text(noResultsTitle), LOAD_TIMEOUT, "no-results-title")

        // Leaving search restores the unfiltered list and the FAB.
        exitSearchMode()
        TITLES.forEach { awaitObject(chatRow(it), LOAD_TIMEOUT, "unfiltered-chat-row-$it") }
    }

    /**
     * The action is matched by its test tag rather than its content description: the description is
     * a translated string the toolbar owns, so it moves whenever the toolbar is reworked.
     */
    private fun searchAction(): UiObject2 =
        awaitObject(By.res(CHAT_LIST_SEARCH_ACTION_TAG), LOAD_TIMEOUT, "search-action")

    private fun searchField(): BySelector = By.clazz(EDIT_TEXT_CLASS)

    private fun typeQuery(query: String) {
        awaitObject(searchField(), LOAD_TIMEOUT, "search-field").text = query
    }

    /**
     * Leaves search through the search bar's own back affordance: system back is not handled by the
     * search top app bar, it pops the whole destination. The FAB, hidden for as long as search is
     * on, confirms the exit.
     */
    private fun exitSearchMode() {
        repeat(EXIT_SEARCH_ATTEMPTS) {
            awaitObject(By.desc(NAVIGATION_ICON_DESC), LOAD_TIMEOUT, "search-back-affordance").click()
            if (device.wait(Until.hasObject(By.res(CHAT_LIST_FAB_TAG)), LOAD_TIMEOUT)) return
        }
        dumpHierarchy("search-mode-exit")
        throw AssertionError(
            "Tapping the search bar back affordance $EXIT_SEARCH_ATTEMPTS times never left search " +
                    "mode; hierarchy in logcat tag UiDump"
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

    private fun awaitObject(selector: BySelector, timeout: Long, name: String): UiObject2 {
        if (device.wait(Until.hasObject(selector), timeout)) return device.findObject(selector)
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
        const val EXIT_SEARCH_ATTEMPTS = 2

        const val FIRST_CHAT_ID = 5_301L
        const val PEER_HANDLE_BASE = 6_301L
        const val BASE_TIMESTAMP = 1_700_000_000L

        val TITLES = listOf("Design Team", "Weekend Hikers", "Book Club")
        const val MATCHING_QUERY = "Book"
        const val NO_MATCH_QUERY = "zzzzz"

        const val EDIT_TEXT_CLASS = "android.widget.EditText"

        /** Content description hardcoded by core-ui's SearchInputField. */
        const val CLEAR_TEXT_DESC = "Clear Text"

        /** The MegaSearchTopAppBar back affordance rendered by AppBarNavigationType.Back. */
        const val NAVIGATION_ICON_DESC = "Navigation Icon"

        /** Mirror ChatListScreen / ChatListContent / ChatRoomItemView / ChatListTopAppBar tags. */
        const val CHAT_ROOM_ITEM_TAG = "chat_room_item:row"
        const val CHAT_LIST_SEARCH_ACTION_TAG = "chat_list_top_app_bar:search_action"
        const val CHAT_LIST_FAB_TAG = "chat_list_screen:new_chat_fab"
        const val CHAT_LIST_NO_RESULTS_TAG = "chat_list_content:chat_no_results"
    }
}
