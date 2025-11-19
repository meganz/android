package mega.privacy.android.app.presentation.psa.legacy

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.dialog.businessgrace.BusinessAccountContainer
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.container.AppContainerWrapper
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContentView
import mega.privacy.android.app.presentation.psa.PsaViewModel
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber
import java.security.InvalidParameterException
import javax.inject.Inject


/**
 * Legacy psa handler
 *
 * @property context
 * @property monitorThemeModeUseCase
 */
class ActivityAppContainerWrapper @Inject constructor(
    @ActivityContext private val context: Context,
    private val monitorThemeModeUseCase: MonitorThemeModeUseCase,
    private val psaGlobalState: LegacyPsaGlobalState,
    private val passcodeCryptObjectFactory: PasscodeCryptObjectFactory,
) : AppContainerWrapper, LifecycleEventObserver {

    init {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
            ?: throw InvalidParameterException("LegacyHandler can only be injected into LifecycleOwner")

        lifecycle.addObserver(this)
    }

    private var passcodeCheck: PasscodeCheck? = null

    override fun setPasscodeCheck(check: PasscodeCheck) {
        passcodeCheck = check
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate()
            else -> return
        }
    }

    private fun onCreate() {
        val activity = context as Activity
        val psaViewHolder = (activity as? ManagerActivity)?.psaViewHolder
        val isLoginActivity = activity is LoginActivity
        if (activity.findViewById<ComposeView>(R.id.legacy_container) == null
        ) {
            val view = ComposeView(activity)
                .apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        LaunchedEffect(Unit) {
                            Timber.d("LegacyMegaAppContainer view added for activity $activity")
                        }
                        val themeMode by monitorThemeModeUseCase()
                            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)

                        if (!isLoginActivity) {
                            val viewModel: PsaViewModel = hiltViewModel()
                            val psaState by psaGlobalState.state.collectAsStateWithLifecycle((context as LifecycleOwner))

                            LaunchedEffect(psaState) {
                                when (val current = psaState) {
                                    is PsaState.InfoPsa -> {
                                        psaViewHolder?.bind(fromInfoPsa(current))
                                        psaViewHolder?.toggleVisible(true)
                                    }

                                    PsaState.NoPsa -> {
                                        psaViewHolder?.toggleVisible(false)
                                    }

                                    is PsaState.StandardPsa -> {
                                        psaViewHolder?.bind(
                                            fromStandardPsa(
                                                current
                                            )
                                        )
                                        psaViewHolder?.toggleVisible(true)
                                    }

                                    is PsaState.WebPsa -> {
                                        psaViewHolder?.toggleVisible(false)
                                    }
                                }
                            }

                            val hasPsaViewHolder = psaViewHolder != null
                            val handledPsaState = when (psaState) {
                                is PsaState.InfoPsa -> if (hasPsaViewHolder.not() && isLoginActivity.not()) psaState else PsaState.NoPsa
                                PsaState.NoPsa -> psaState
                                is PsaState.StandardPsa -> if (hasPsaViewHolder.not() && isLoginActivity.not()) psaState else PsaState.NoPsa
                                is PsaState.WebPsa -> psaState
                            }
                            val containers: List<(@Composable (@Composable () -> Unit) -> Unit)?> =
                                listOf(
                                    {
                                        BusinessAccountContainer(content = it)
                                    },
                                    {
                                        PsaContentView(
                                            context = context,
                                            coroutineScope = rememberCoroutineScope(),
                                            markAsSeen = {
                                                viewModel.markAsSeen(it)
                                                psaGlobalState.clearPsa()
                                            },
                                            state = handledPsaState,
                                            content = it,
                                            containerModifier = Modifier
                                                .navigationBarsPadding(),
                                            innerModifier = { it.padding(bottom = 16.dp) }
                                        )
                                    },
                                    {
                                        PasscodeContainer(
                                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                                            canLock = { passcodeCheck?.canLock() != false },
                                            content = it,
                                        )
                                    },
                                    {
                                        OriginalTheme(
                                            isDark = themeMode.isDarkMode(),
                                            content = it
                                        )
                                    },
                                )

                            AppContainer(
                                containers = containers.filterNotNull(),
                                content = { Box(Modifier.fillMaxSize()) }
                            )
                        } else {
                            val containers: List<(@Composable (@Composable () -> Unit) -> Unit)?> =
                                listOf(
                                    {
                                        BusinessAccountContainer(content = it)
                                    },
                                    {
                                        PasscodeContainer(
                                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                                            canLock = { passcodeCheck?.canLock() != false },
                                            content = it,
                                        )
                                    },
                                    {
                                        OriginalTheme(
                                            isDark = themeMode.isDarkMode(),
                                            content = it
                                        )
                                    },
                                )

                            AppContainer(
                                containers = containers.filterNotNull(),
                                content = { Box(Modifier.fillMaxSize()) }
                            )
                        }
                    }

                }.apply {
                    id = R.id.legacy_container
                }
            activity.addContentView(
                view,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun fromInfoPsa(psaState: PsaState.InfoPsa) = with(psaState) {
        Psa(
            id = this.id,
            title = this.title,
            text = this.text,
            imageUrl = this.imageUrl,
            positiveText = null,
            positiveLink = null,
            url = null,
        )
    }

    private fun fromStandardPsa(psaState: PsaState.StandardPsa) = with(psaState) {
        Psa(
            id = this.id,
            title = this.title,
            text = this.text,
            imageUrl = this.imageUrl,
            positiveText = this.positiveText,
            positiveLink = this.positiveLink,
            url = null,
        )
    }
}


