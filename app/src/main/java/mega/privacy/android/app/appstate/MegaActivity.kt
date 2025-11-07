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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import kotlinx.serialization.Serializable
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.appstate.content.AppContentStateViewModel
import mega.privacy.android.app.appstate.content.view.AppContentView
import mega.privacy.android.app.appstate.global.GlobalStateViewModel
import mega.privacy.android.app.appstate.global.SnackbarEventsViewModel
import mega.privacy.android.app.appstate.global.model.GlobalState
import mega.privacy.android.app.appstate.global.util.show
import mega.privacy.android.app.presence.SignalPresenceViewModel
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginNavDisplay
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.utils.Constants
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MegaActivity : ComponentActivity() {

    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    @Serializable
    data class LoggedInScreens(val isFromLogin: Boolean = false, val session: String) : NavKey

    private lateinit var viewModel: GlobalStateViewModel

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        consumeWarningMessage()
        consumeIntentDestinations()
    }

    private fun consumeWarningMessage() {
        intent.getStringExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE)?.let { warningMessage ->
            viewModel.queueMessage(warningMessage)
            intent.removeExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE)
        }
    }

    private fun consumeIntentDestinations() {
        intent.getDestinations()?.let { navKeys ->
            //Add nav keys to navigation queue
            navKeys.forEach {
                Timber.d("NavKey from intent: $it")
            }
            viewModel.addNavKeysToEventQueue(navKeys)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplashScreen = true
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { keepSplashScreen }
        enableEdgeToEdge()
        setContent {
            viewModel = viewModel<GlobalStateViewModel>()
            val presenceViewModel = hiltViewModel<SignalPresenceViewModel>()

            val state by viewModel.state.collectAsStateWithLifecycle()
            val snackbarEventsViewModel = viewModel<SnackbarEventsViewModel>()
            val snackbarEventsState by snackbarEventsViewModel.snackbarEventState.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }
            // This is used to recompose the LoginGraph when new login request is made
            var fromLogin by remember { mutableStateOf(false) }
            keepSplashScreen = state is GlobalState.Loading
            LaunchedEffect(Unit) {
                //consume intent extras
                consumeWarningMessage()
                consumeIntentDestinations()
            }

            AndroidTheme(isDark = state.themeMode.isDarkMode()) {
                EventEffect(
                    event = snackbarEventsState,
                    onConsumed = snackbarEventsViewModel::consumeEvent,
                    action = { snackbarHostState.show(it) }
                )

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
                    when (val currentState = state) {
                        is GlobalState.Loading -> {}
                        is GlobalState.LoggedIn -> {
                            AppContainer(
                                containers = containers,
                            ) {
                                val appContentStateViewModel =
                                    hiltViewModel<AppContentStateViewModel>()
                                AppContentView(
                                    viewModel = appContentStateViewModel,
                                    snackbarHostState = snackbarHostState,
                                    navKey = LoggedInScreens(
                                        isFromLogin = fromLogin,
                                        session = currentState.session
                                    ),
                                )
                            }
                        }

                        is GlobalState.RequireLogin -> {
                            fromLogin = true
                            LoginNavDisplay(
                                onFinish = ::finish,
                                stopShowingSplashScreen = {
                                    keepSplashScreen = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private val containers: List<@Composable (@Composable () -> Unit) -> Unit> = listOf(
        {
            PasscodeContainer(
                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                content = it
            )
        },
    )

    companion object {

        /**
         * Get Intent to open this activity with Single top and clear top flags
         */
        fun getIntent(context: Context) = Intent(
            context,
            MegaActivity::class.java
        ).apply {
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
            val intent = Intent(
                context,
                MegaActivity::class.java
            ).apply {
                putExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE, warningMessage)
                flags =
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
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
