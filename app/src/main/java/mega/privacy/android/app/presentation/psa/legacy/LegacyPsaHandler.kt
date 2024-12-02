package mega.privacy.android.app.presentation.psa.legacy

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import java.security.InvalidParameterException
import javax.inject.Inject


/**
 * Legacy psa handler
 *
 * @property context
 * @property getThemeMode
 */
class LegacyPsaHandler @Inject constructor(
    @ActivityContext private val context: Context,
    private val getThemeMode: GetThemeMode,
) : PsaHandler, LifecycleEventObserver {

    init {
        val lifecycle = (context as? LifecycleOwner)?.lifecycle
            ?: throw InvalidParameterException("PasscodeFacade can only be injected into LifecycleOwner")

        lifecycle.addObserver(this)
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_CREATE -> onCreate()
            else -> return
        }
    }

    private fun onCreate() {
        val activity = context as Activity
        if (activity.findViewById<ComposeView>(R.id.legacy_psa) == null && activity.findViewById<ComposeView>(
                R.id.pass_code
            ) == null
        ) {
            val view = ComposeView(activity)
                .apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
                    setContent {
                        val themeMode by getThemeMode()
                            .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                        val isDarkMode = themeMode.isDarkMode()
                        OriginalTempTheme(isDark = isDarkMode) {
                            PsaContainer { Box(Modifier.fillMaxSize()) }
                        }
                    }
                }.apply {
                    id = R.id.legacy_psa
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
