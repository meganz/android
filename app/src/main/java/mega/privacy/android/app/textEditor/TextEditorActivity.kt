package mega.privacy.android.app.textEditor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.appstate.content.navigation.LegacyActivityScaffold
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.app.textEditor.TextEditorActivity.Companion.createIntent
import mega.privacy.android.core.sharedcomponents.parcelable
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey
import javax.inject.Inject

/**
 * A thin Compose host that lets a legacy Activity open the Compose text editor. It renders the
 * [LegacyTextEditorNavKey] destination via [LegacyActivityScaffold]; the entry itself is
 * registered by the injected [featureDestinations].
 *
 * Callers inside `MegaActivity` should navigate to [LegacyTextEditorNavKey] directly. Build the
 * launch [Intent] with [createIntent].
 */
@AndroidEntryPoint
class TextEditorActivity : AppCompatActivity() {

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startKey = intent.parcelable<LegacyTextEditorNavKey>(EXTRA_NAV_KEY)
        if (startKey == null) {
            finish()
            return
        }
        enableEdgeToEdge()
        setContent {
            val themeMode by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            LegacyActivityScaffold(
                container = { content ->
                    // File links open without a session, so the shell must not require one.
                    MegaAppContainer(
                        themeMode = themeMode,
                        isSessionRequired = false,
                        finishOnSessionRefresh = false,
                        content = content,
                    )
                },
                initialKey = startKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                onEmptyBackStack = { if (!isFinishing) finish() },
            ) { _, _ -> }
        }
    }

    companion object {
        private const val EXTRA_NAV_KEY = "NAV_KEY"

        /**
         * Builds an [Intent] that opens the text editor for [navKey].
         */
        fun createIntent(context: Context, navKey: LegacyTextEditorNavKey): Intent =
            Intent(context, TextEditorActivity::class.java).apply {
                putExtra(EXTRA_NAV_KEY, navKey)
            }
    }
}
