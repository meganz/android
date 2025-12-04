package mega.privacy.android.app.appstate

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import de.palm.composestateevents.NavigationEventEffect
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.appstate.content.NavigationGraphViewModel
import mega.privacy.android.app.appstate.content.destinations.FetchingContentNavKey
import mega.privacy.android.app.appstate.content.model.NavigationGraphState
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.appstate.content.navigation.PendingBackStack
import mega.privacy.android.app.appstate.content.navigation.PendingBackStackNavigationHandler
import mega.privacy.android.app.appstate.content.navigation.rememberPendingBackStack
import mega.privacy.android.app.appstate.content.transfer.AppTransferViewModel
import mega.privacy.android.app.appstate.content.transfer.TransferHandlerImpl
import mega.privacy.android.app.appstate.global.GlobalStateViewModel
import mega.privacy.android.app.appstate.global.SnackbarEventsViewModel
import mega.privacy.android.app.appstate.global.event.QueueEventViewModel
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.global.model.RefreshEvent
import mega.privacy.android.app.appstate.global.util.show
import mega.privacy.android.app.presence.SignalPresenceViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginNavKey
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.confirmemail.ConfirmationEmailNavKey
import mega.privacy.android.app.presentation.login.createaccount.CreateAccountNavKey
import mega.privacy.android.app.presentation.login.loginEntryProvider
import mega.privacy.android.app.presentation.login.model.LoginScreen
import mega.privacy.android.app.presentation.login.onboarding.TourNavKey
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.passcode.navigation.PasscodeNavKey
import mega.privacy.android.app.presentation.passcode.navigation.passcodeView
import mega.privacy.android.app.presentation.security.check.PasscodeCheckViewModel
import mega.privacy.android.app.presentation.security.check.model.PasscodeCheckState
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.navigation.contract.bottomsheet.BottomSheetSceneStrategy
import mega.privacy.android.navigation.contract.dialog.DialogNavKey
import mega.privacy.android.navigation.contract.queue.NavigationEventQueue
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.transparent.TransparentSceneStrategy
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import timber.log.Timber
import javax.inject.Inject

/**
 * Single Activity for Mega App
 */
@AndroidEntryPoint
class MegaActivity : ComponentActivity() {

    /**
     * Passcode crypt object factory
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * Navigation event queue
     */
    @Inject
    lateinit var navigationEventQueue: NavigationEventQueue

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    private val passcodeViewModel: PasscodeCheckViewModel by viewModels()

    private val globalStateViewModel: GlobalStateViewModel by viewModels()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        lifecycleScope.launch {
            consumeIntentExtras()
        }
    }

    private suspend fun consumeIntentExtras() {
        consumeActions()
        consumeWarningMessage()
        consumeIntentDestinations()
    }

    private fun consumeActions() {
        if (intent.action == Constants.ACTION_REFRESH) {
            globalStateViewModel.refreshSession(RefreshEvent.Refresh)
            intent.action = null
        } else if (intent.action == Constants.ACTION_REFRESH_API_SERVER) {
            globalStateViewModel.refreshSession(RefreshEvent.ChangeEnvironment)
            intent.action = null
        }
    }

    private fun consumeWarningMessage() {
        intent.getStringExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE)?.let { warningMessage ->
            globalStateViewModel.queueMessage(warningMessage)
            intent.removeExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE)
        }
    }

    private suspend fun consumeIntentDestinations() {
        intent.getDestinations()?.let { navKeys ->
            //Add nav keys to navigation queue
            navKeys.forEach {
                Timber.d("NavKey from intent: $it")
            }
            navigationEventQueue.emit(navKeys)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplashScreen by mutableStateOf(true)
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        enableEdgeToEdge()
        lifecycleScope.launch {
            consumeIntentExtras()
        }
        setContent {
            val navGraphViewModel = hiltViewModel<NavigationGraphViewModel>() // nav graph content
            val presenceViewModel = hiltViewModel<SignalPresenceViewModel>()
            val snackbarEventsViewModel = viewModel<SnackbarEventsViewModel>()
            val appTransferViewModel = hiltViewModel<AppTransferViewModel>()
            val loginViewModel = hiltViewModel<LoginViewModel>()
            val navigationEventViewModel = hiltViewModel<QueueEventViewModel>()

            val navGraphState by navGraphViewModel.state.collectAsStateWithLifecycle()
            val globalState by globalStateViewModel.state.collectAsStateWithLifecycle()
            val rootNodeState by globalStateViewModel.rootNodeExistsFlow.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(globalState, navGraphState) {
                if (globalState !is GlobalState.Loading && navGraphState !is NavigationGraphState.Loading) {
                    keepSplashScreen = false
                }
            }

            if (!keepSplashScreen) {
                val backStack: PendingBackStack<NavKey> =
                    rememberPendingBackStack(HomeScreensNavKey())

                val passcodeState by passcodeViewModel.state.collectAsStateWithLifecycle()

                val navigationHandler = remember {
                    val authStatus = (globalState as? GlobalState.LoggedIn)?.session?.let {
                        PendingBackStackNavigationHandler.AuthStatus.LoggedIn(it)
                    } ?: PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn
                    PendingBackStackNavigationHandler(
                        backstack = backStack,
                        currentAuthStatus = authStatus,
                        defaultLandingScreen = HomeScreensNavKey(),
                        defaultLoginDestination = LoginNavKey(),
                        initialLoginDestination = TourNavKey,
                        hasRootNode = rootNodeState.exists,
                        isPasscodeLocked = passcodeState is PasscodeCheckState.Locked,
                        passcodeDestination = PasscodeNavKey,
                        fetchRootNodeDestination = ::FetchingContentNavKey,
                        navigationResultManager = navigationResultManager,
                    )
                }

                LaunchedEffect(globalState) {
                    val authStatus =
                        (globalState as? GlobalState.LoggedIn)?.session?.let {
                            PendingBackStackNavigationHandler.AuthStatus.LoggedIn(it)
                        } ?: PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn
                    if (authStatus == PendingBackStackNavigationHandler.AuthStatus.NotLoggedIn) {
                        loginViewModel.stopLogin(isPerformLocalLogOut = false)
                    }
                    navigationHandler.onLoginChange(authStatus)
                }

                LaunchedEffect(rootNodeState) {
                    navigationHandler.onRootNodeChange(rootNodeState)
                }

                LaunchedEffect(passcodeState) {
                    navigationHandler.onPasscodeStateChanged(passcodeState is PasscodeCheckState.Locked)
                }


                val transferHandler = remember { TransferHandlerImpl(appTransferViewModel) }
                val transparentStrategy = remember { TransparentSceneStrategy<NavKey>() }
                val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
                val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }

                AndroidTheme(isDark = globalState.themeMode.isDarkMode()) {
                    Box(modifier = Modifier.pointerInput(Unit) {
                        awaitEachGesture {
                            do {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Press) {
                                    presenceViewModel.signalPresence()
                                }
                            } while (event.changes.any { it.pressed })
                        }
                    }) {
                        when (val graphstate = navGraphState) {
                            is NavigationGraphState.Loading -> {}
                            is NavigationGraphState.Data -> {
                                val snackbarEventsState by snackbarEventsViewModel.snackbarEventState.collectAsStateWithLifecycle()
                                val transferState by appTransferViewModel.state.collectAsStateWithLifecycle()
                                val loginState by loginViewModel.state.collectAsStateWithLifecycle()
                                val navigationEvents by navigationEventViewModel.navigationEvents.collectAsStateWithLifecycle()


                                LaunchedEffect(loginState.isPendingToFinishActivity) {
                                    if (loginState.isPendingToFinishActivity) {
                                        finish()
                                    }
                                }

                                EventEffect(
                                    event = snackbarEventsState,
                                    onConsumed = snackbarEventsViewModel::consumeEvent,
                                    action = { snackbarHostState.show(it) }
                                )


                                CompositionLocalProvider(
                                    LocalSnackBarHostState provides snackbarHostState
                                ) {
                                    NavDisplay(
                                        backStack = backStack,
                                        onBack = { navigationHandler.back() },
                                        sceneStrategy = transparentStrategy.chain(dialogStrategy)
                                            .chain(bottomSheetStrategy),
                                        entryDecorators = listOf(
                                            rememberSaveableStateHolderNavEntryDecorator(),
                                            rememberViewModelStoreNavEntryDecorator()
                                        ),
                                        entryProvider = entryProvider {
                                            graphstate.featureDestinations
                                                .forEach { destination ->
                                                    destination.navigationGraph(
                                                        this,
                                                        navigationHandler,
                                                        transferHandler
                                                    )
                                                }

                                            graphstate.appDialogDestinations.forEach { destination ->
                                                destination.navigationGraph(
                                                    this as EntryProviderScope<DialogNavKey>,
                                                    navigationHandler,
                                                    navigationEventViewModel::eventHandled
                                                )
                                            }

                                            loginEntryProvider(
                                                navigationHandler = navigationHandler,
                                                loginViewModel = loginViewModel,
                                                onFinish = ::finish
                                            )

                                            passcodeView(passcodeCryptObjectFactory)
                                        },
                                        transitionSpec = {
                                            // Slide in from right when navigating forward
                                            slideInHorizontally(
                                                initialOffsetX = { it },
                                            ) togetherWith slideOutHorizontally(
                                                targetOffsetX = { -it },
                                            )
                                        },
                                        popTransitionSpec = {
                                            // Slide in from left when navigating back
                                            slideInHorizontally(
                                                initialOffsetX = { -it },
                                            ) togetherWith slideOutHorizontally(
                                                targetOffsetX = { it },
                                            )
                                        },
                                        predictivePopTransitionSpec = {
                                            // Slide in from left when navigating back
                                            slideInHorizontally(
                                                initialOffsetX = { -it },
                                            ) togetherWith slideOutHorizontally(
                                                targetOffsetX = { it },
                                            )
                                        }
                                    )

                                    StartTransferComponent(
                                        event = transferState.transferEvent,
                                        onConsumeEvent = appTransferViewModel::consumedTransferEvent,
                                    )
                                }

                                NavigationEventEffect(
                                    event = navigationEvents,
                                    onConsumed = navigationEventViewModel::eventDisplayed
                                ) {
                                    when (it) {
                                        is NavigationQueueEvent -> {
                                            navigationHandler.navigate(it.keys)
                                        }

                                        is AppDialogEvent -> {
                                            navigationHandler.displayDialog(it.dialogDestination)
                                        }
                                    }
                                }

                                val focusManager = LocalFocusManager.current
                                NavigationEventEffect(
                                    loginState.isPendingToShowFragment,
                                    loginViewModel::isPendingToShowFragmentConsumed
                                ) {
                                    if (it != LoginScreen.LoginScreen) {
                                        keepSplashScreen = false
                                    }

                                    when (it) {
                                        LoginScreen.LoginScreen -> navigationHandler.navigate(
                                            LoginNavKey()
                                        )

                                        LoginScreen.CreateAccount -> navigationHandler.navigate(
                                            CreateAccountNavKey()
                                        )

                                        LoginScreen.Tour -> {
                                            focusManager.clearFocus()
                                            navigationHandler.navigate(TourNavKey)
                                        }

                                        LoginScreen.ConfirmEmail -> navigationHandler.navigate(
                                            ConfirmationEmailNavKey
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {

        /**
         * Get Intent to open this activity with Single top and clear top flags
         */
        fun getIntent(context: Context, warningMessage: String? = null) = Intent(
            context,
            MegaActivity::class.java
        ).apply {
            warningMessage?.let {
                putExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE, warningMessage)
            }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        /**
         * Get a pending intent to open this activity with the specified warning message
         */
        fun getPendingIntentForWarningMessage(
            context: Context,
            warningMessage: String,
            requestCode: Int = warningMessage.hashCode(), //unique request code per warning message
        ): PendingIntent {
            val intent = getIntent(context, warningMessage)
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * Get a pending intent to open this activity with the specified nav key
         */
        fun <T> getPendingIntentWithExtraDestination(
            context: Context,
            navKey: T,
            requestCode: Int = navKey.hashCode(), //unique request code per nav key
        ) where T : NavKey, T : Parcelable = getPendingIntentWithExtraDestinations(
            context, listOf(navKey), requestCode
        )

        /**
         * Get a pending intent to open this activity with the specified nav keys
         */
        fun <T> getPendingIntentWithExtraDestinations(
            context: Context,
            navKeys: List<T>,
            requestCode: Int = navKeys.hashCode(), //unique request code per nav keys
        ): PendingIntent where T : NavKey, T : Parcelable = PendingIntent.getActivity(
            context,
            requestCode,
            getIntentWithExtraDestinations(context, navKeys),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun <T> getIntentWithExtraDestinations(
            context: Context,
            navKeys: List<T>,
        ) where T : NavKey, T : Parcelable = getIntent(context).apply {
            putParcelableArrayListExtra(EXTRA_NAV_KEYS, ArrayList(navKeys))
        }

        private fun Intent.getDestinations(): List<NavKey>? {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableArrayListExtra(EXTRA_NAV_KEYS, NavKey::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelableArrayListExtra(EXTRA_NAV_KEYS)
            }
        }

        private const val EXTRA_NAV_KEYS = "navKeys"
    }
}


private infix fun <T : Any> SceneStrategy<T>.chain(sceneStrategy: SceneStrategy<T>): SceneStrategy<T> =
    object : SceneStrategy<T> {
        override fun SceneStrategyScope<T>.calculateScene(
            entries: List<NavEntry<T>>,
        ): Scene<T>? =
            this@chain.run { calculateScene(entries) } ?: with(sceneStrategy) {
                calculateScene(
                    entries
                )
            }
    }
