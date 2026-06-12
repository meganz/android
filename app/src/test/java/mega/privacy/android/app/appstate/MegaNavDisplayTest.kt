package mega.privacy.android.app.appstate

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
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
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.app.appstate.content.model.NavigationGraphState
import mega.privacy.android.app.appstate.content.navigation.PendingBackStack
import mega.privacy.android.app.appstate.content.navigation.PendingBackStackNavigationHandler
import mega.privacy.android.app.appstate.global.event.NavigationEventQueueReceiver
import mega.privacy.android.app.appstate.global.event.QueueEventViewModel
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.navkey.Suppressable
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.suppression.OverlaySuppressionMetadata
import mega.privacy.android.navigation.contract.suppression.SuppressionType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MegaNavDisplayTest {

    private val composeRule = createComposeRule()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeRule)

    @Serializable
    private data object PlainNavKey : NavKey

    @Serializable
    private data object SuppressingNavKey : NavKey

    @Serializable
    private data object SuppressableTarget : NavKey, Suppressable

    private val emittedEvents = mutableListOf<QueueEvent>()
    private lateinit var eventChannel: Channel<() -> QueueEvent?>
    private lateinit var receiver: NavigationEventQueueReceiver
    private lateinit var queueEventViewModel: QueueEventViewModel
    private lateinit var navigationHandler: PendingBackStackNavigationHandler
    private lateinit var loginViewModel: LoginViewModel
    private lateinit var transferHandler: TransferHandler
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner

    private val testFeatureDestination = object : FeatureDestination {
        override val navigationGraph:
                EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit =
            { _, _ ->
                entry<PlainNavKey> { Box {} }
                entry<SuppressingNavKey>(
                    metadata = mapOf(OverlaySuppressionMetadata.KEY to SuppressionType.Complete),
                ) { Box {} }
            }
    }

    @Before
    fun setUp() {
        emittedEvents.clear()
        eventChannel = Channel(Channel.UNLIMITED)
        receiver = object : NavigationEventQueueReceiver {
            override val events: ReceiveChannel<() -> QueueEvent?> = eventChannel
        }
        queueEventViewModel = spy(QueueEventViewModel(receiver))
        navigationHandler = mock()
        loginViewModel = mock()
        transferHandler = mock()

        installViewModel(queueEventViewModel)
    }

    private fun installViewModel(viewModel: QueueEventViewModel) {
        val store = ViewModelStore().apply {
            put(viewModelKeyFor(QueueEventViewModel::class.java), viewModel)
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
    fun `test that NavigationQueueEvent triggers navigationHandler navigate when not suppressed`() {
        val backStack = PendingBackStack<NavKey>(NavBackStack(PlainNavKey))
        val event = NavigationQueueEvent(keys = listOf(PlainNavKey))
        eventChannel.trySend { event }

        setContent(backStack)

        verify(navigationHandler, timeout(2_000)).navigate(event.keys, event.navOptions)
    }

    @Test
    fun `test that AppDialogEvent triggers navigationHandler displayDialog when not suppressed`() {
        val backStack = PendingBackStack<NavKey>(NavBackStack(PlainNavKey))
        val event = AppDialogEvent(dialogDestination = PlainNavKey)
        eventChannel.trySend { event }

        setContent(backStack)

        verify(navigationHandler, timeout(2_000)).displayDialog(PlainNavKey)
    }

    @Test
    fun `test that suppressable NavigationQueueEvent does not call navigate when screen suppresses overlays`() {
        val backStack = PendingBackStack<NavKey>(NavBackStack(SuppressingNavKey))
        val event = NavigationQueueEvent(keys = listOf(SuppressableTarget))
        eventChannel.trySend { event }

        setContent(backStack)
        composeRule.waitForIdle()

        verify(navigationHandler, never()).navigate(event.keys, event.navOptions)
    }

    @Test
    fun `test that suppressed AppDialogEvent calls eventHandled and does not display the dialog`() {
        val backStack = PendingBackStack<NavKey>(NavBackStack(SuppressingNavKey))
        val event = AppDialogEvent(dialogDestination = SuppressableTarget)
        eventChannel.trySend { event }

        setContent(backStack)

        verify(queueEventViewModel, timeout(2_000)).eventHandled()
        verify(navigationHandler, never()).displayDialog(any())
    }

    @Test
    fun `test that deferred events are re-emitted via emitNavigationEvent when suppression clears`() {
        val backStack = PendingBackStack<NavKey>(NavBackStack(SuppressingNavKey))
        val event = NavigationQueueEvent(keys = listOf(SuppressableTarget))
        eventChannel.trySend { event }

        setContent(backStack)
        composeRule.waitForIdle()

        verify(navigationHandler, never()).navigate(event.keys, event.navOptions)

        composeRule.runOnUiThread {
            backStack.add(PlainNavKey)
        }
        composeRule.waitForIdle()

        assertThat(emittedEvents).contains(event)
    }

    @Test
    fun `test that equal suppressed dialog events are deferred and re-emitted only once`() {
        // the spy copies the view model's signal fields, breaking the internal
        // handled/displayed handshake, so forward those calls to the real instance
        // to let the queue advance past the first event
        val realViewModel = QueueEventViewModel(receiver)
        queueEventViewModel = spy(realViewModel)
        doAnswer { realViewModel.eventDisplayed() }.whenever(queueEventViewModel).eventDisplayed()
        doAnswer { realViewModel.eventHandled() }.whenever(queueEventViewModel).eventHandled()
        installViewModel(queueEventViewModel)

        val backStack = PendingBackStack<NavKey>(NavBackStack(SuppressingNavKey))
        val event = AppDialogEvent(dialogDestination = SuppressableTarget)
        eventChannel.trySend { event }

        setContent(backStack)
        composeRule.waitUntil(timeoutMillis = 2_000) { eventHandledCount() == 1 }

        eventChannel.trySend { AppDialogEvent(dialogDestination = SuppressableTarget) }
        composeRule.waitUntil(timeoutMillis = 2_000) { eventHandledCount() == 2 }
        verify(navigationHandler, never()).displayDialog(any())

        composeRule.runOnUiThread {
            backStack.add(PlainNavKey)
        }
        composeRule.waitForIdle()

        assertThat(emittedEvents.filter { it == event }).hasSize(1)
    }

    private fun eventHandledCount() = mockingDetails(queueEventViewModel).invocations
        .count { it.method.name == "eventHandled" }

    private fun setContent(backStack: PendingBackStack<NavKey>) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner,
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
                    entryDecorators = emptyList()
                )
            }
        }
    }

    private fun <T> viewModelKeyFor(clazz: Class<T>): String =
        "androidx.lifecycle.ViewModelProvider.DefaultKey:${clazz.canonicalName}"
}