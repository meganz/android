package mega.privacy.android.app.presentation.psa.legacy

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.container.AppContainerWrapper
import mega.privacy.android.app.presentation.container.LegacyMegaAppContainer
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaViewModel
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.security.PasscodeCheck
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.psa.Psa
import mega.privacy.android.domain.usecase.GetThemeMode
import java.security.InvalidParameterException
import javax.inject.Inject


/**
 * Legacy psa handler
 *
 * @property context
 * @property getThemeMode
 */
class ActivityAppContainerWrapper @Inject constructor(
    @ActivityContext private val context: Context,
    private val getThemeMode: GetThemeMode,
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
        if (activity.findViewById<ComposeView>(R.id.legacy_container) == null
        ) {
            val view = ComposeView(activity)
                .apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        val themeMode by getThemeMode()
                            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
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

                        val handledPsaState = when (psaState) {
                            is PsaState.InfoPsa -> if (psaViewHolder == null) psaState else PsaState.NoPsa
                            PsaState.NoPsa -> psaState
                            is PsaState.StandardPsa -> if (psaViewHolder == null) psaState else PsaState.NoPsa
                            is PsaState.WebPsa -> psaState
                        }

                        LegacyMegaAppContainer(
                            context = context,
                            psaState = handledPsaState,
                            markPsaAsSeen = {
                                viewModel.markAsSeen(it)
                                psaGlobalState.clearPsa()
                            },
                            themeMode = themeMode,
                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                            canLock = { passcodeCheck?.canLock() ?: true },
                        )
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


