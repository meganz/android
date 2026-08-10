package mega.privacy.android.app.presentation.psa.legacy

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.qualifiers.ActivityContext
import mega.privacy.android.app.R
import mega.privacy.android.app.main.dialog.businessgrace.BusinessAccountContainer
import mega.privacy.android.app.presentation.container.AppContainer
import mega.privacy.android.app.presentation.container.AppContainerWrapper
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.core.passcode.PasscodeCheck
import mega.privacy.android.core.sharedcomponents.extension.isDarkMode
import mega.privacy.android.domain.entity.ThemeMode
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

                        val containers: List<(@Composable (@Composable () -> Unit) -> Unit)?> =
                            listOf(
                                {
                                    BusinessAccountContainer(content = it)
                                },
                                {
                                    PasscodeContainer(
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
}


