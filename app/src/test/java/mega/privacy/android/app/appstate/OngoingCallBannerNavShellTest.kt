package mega.privacy.android.app.appstate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.LocalMegaScaffoldTopBanner
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.app.appstate.content.model.NavigationGraphState
import mega.privacy.android.app.appstate.content.navigation.PendingBackStack
import mega.privacy.android.app.appstate.content.navigation.PendingBackStackNavigationHandler
import mega.privacy.android.app.appstate.global.call.OngoingCallBannerContent
import mega.privacy.android.app.appstate.global.call.OngoingCallBannerUiState
import mega.privacy.android.app.appstate.global.event.NavigationEventQueueReceiver
import mega.privacy.android.app.appstate.global.event.QueueEventViewModel
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.feature.chat.components.RETURN_TO_CALL_BANNER_TAG
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.queue.QueueEvent
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * Verifies the global return-to-call banner as it renders in the Navigation3 shell
 * ([MegaNavDisplay]) when provided through [LocalMegaScaffoldTopBanner]: beneath the current
 * destination's top bar, persisting across navigation, absent when no call is in progress, and
 * absent on destinations whose scaffold opts out.
 */
@RunWith(AndroidJUnit4::class)
class OngoingCallBannerNavShellTest {

    private val composeRule = createComposeRule()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    @Serializable
    private data object ScreenANavKey : NavKey

    @Serializable
    private data object ScreenBNavKey : NavKey

    @Serializable
    private data object OptedOutScreenNavKey : NavKey

    private val emittedEvents = mutableListOf<QueueEvent>()
    private lateinit var eventChannel: Channel<suspend () -> QueueEvent?>
    private lateinit var navigationHandler: PendingBackStackNavigationHandler
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var transferHandler: TransferHandler
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner

    private val testFeatureDestination = object : FeatureDestination {
        override val navigationGraph:
                EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
            { _, _ ->
                entry<ScreenANavKey> { TestScreen(SCREEN_A_TAG) }
                entry<ScreenBNavKey> { TestScreen(SCREEN_B_TAG) }
                entry<OptedOutScreenNavKey> {
                    TestScreen(OPTED_OUT_SCREEN_TAG, showTopBanner = false)
                }
            }
    }

    @Before
    fun setUp() {
        emittedEvents.clear()
        eventChannel = Channel(Channel.UNLIMITED)
        val receiver = object : NavigationEventQueueReceiver {
            override val events: ReceiveChannel<suspend () -> QueueEvent?> = eventChannel
        }
        navigationHandler = mock()
        loginViewModel = mock()
        transferHandler = mock()

        val store = ViewModelStore().apply {
            put(viewModelKeyFor(QueueEventViewModel::class.java), QueueEventViewModel(receiver))
        }
        viewModelStoreOwner = object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = store
        }
    }

    @After
    fun tearDown() {
        eventChannel.close()
    }

    @Test
    fun `test that banner is displayed below the destination top bar when a call is in progress`() {
        setContent(PendingBackStack(NavBackStack(ScreenANavKey)), uiStateWithCall())

        composeRule.onNodeWithTag(SCREEN_A_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true).assertIsDisplayed()
        val topBarBottom = composeRule.onNodeWithTag(TOP_BAR_TAG).getUnclippedBoundsInRoot().bottom
        val bannerTop =
            composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true)
                .getUnclippedBoundsInRoot().top
        assertThat(bannerTop).isAtLeast(topBarBottom)
    }

    @Test
    fun `test that destination content is placed below the banner when a call is in progress`() {
        setContent(PendingBackStack(NavBackStack(ScreenANavKey)), uiStateWithCall())

        val bannerBottom =
            composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true)
                .getUnclippedBoundsInRoot().bottom
        val contentTop = composeRule.onNodeWithTag(SCREEN_A_TAG).getUnclippedBoundsInRoot().top
        assertThat(contentTop).isAtLeast(bannerBottom)
    }

    @Test
    fun `test that banner is not displayed when the destination scaffold opts out`() {
        setContent(PendingBackStack(NavBackStack(OptedOutScreenNavKey)), uiStateWithCall())

        composeRule.onNodeWithTag(OPTED_OUT_SCREEN_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithTag(OPTED_OUT_SCREEN_TAG).assertTopPositionInRootIsEqualTo(TOP_BAR_HEIGHT)
    }

    @Test
    fun `test that banner remains displayed after navigating to a different destination`() {
        val backStack = PendingBackStack<NavKey>(NavBackStack(ScreenANavKey))
        setContent(backStack, uiStateWithCall())

        composeRule.runOnUiThread { backStack.add(ScreenBNavKey) }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SCREEN_B_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun `test that banner is not displayed when there is no call in progress`() {
        setContent(
            PendingBackStack(NavBackStack(ScreenANavKey)),
            OngoingCallBannerUiState(currentCall = null, themeMode = ThemeMode.System),
        )

        composeRule.onNodeWithTag(SCREEN_A_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(RETURN_TO_CALL_BANNER_TAG, useUnmergedTree = true).assertDoesNotExist()
    }

    private fun uiStateWithCall() = OngoingCallBannerUiState(
        currentCall = ChatCall(chatId = 1L, callId = 2L, initialTimestamp = null),
        themeMode = ThemeMode.System,
    )

    private fun setContent(
        backStack: PendingBackStack<NavKey>,
        uiState: OngoingCallBannerUiState,
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                    LocalMegaScaffoldTopBanner provides { OngoingCallBannerContent(uiState) },
                ) {
                    MegaNavDisplay(
                        backStack = backStack,
                        navigationHandler = navigationHandler,
                        graphstate = NavigationGraphState.Data(
                            featureDestinations = persistentSetOf(testFeatureDestination),
                            appDialogDestinations = persistentSetOf(),
                        ),
                        transferHandler = transferHandler,
                        loginViewModel = loginViewModel,
                        emitNavigationEvent = { emittedEvents.add(it) },
                        onFinish = {},
                        entryDecorators = emptyList(),
                    )
                }
            }
        }
    }

    @Composable
    private fun TestScreen(tag: String, showTopBanner: Boolean = true) = MegaScaffold(
        topBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(TOP_BAR_HEIGHT)
                    .testTag(TOP_BAR_TAG)
            )
        },
        contentWindowInsets = WindowInsets(0.dp),
        showTopBanner = showTopBanner,
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag(tag)
        )
    }

    private fun <T> viewModelKeyFor(clazz: Class<T>): String =
        "androidx.lifecycle.ViewModelProvider.DefaultKey:${clazz.canonicalName}"

    private companion object {
        const val SCREEN_A_TAG = "screen_a"
        const val SCREEN_B_TAG = "screen_b"
        const val OPTED_OUT_SCREEN_TAG = "opted_out_screen"
        const val TOP_BAR_TAG = "top_bar"
        val TOP_BAR_HEIGHT = 56.dp
    }
}
