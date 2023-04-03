package mega.privacy.android.app.presentation.chat.dialog

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.chat.dialog.view.AskForDisplayOverDialog
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * A dialog style activity can lead to system setting page.
 * Launched by notification even when the app is killed.
 *
 * @see mega.privacy.android.app.utils.IncomingCallNotification.toSystemSettingNotification
 */
@AndroidEntryPoint
class AskForDisplayOverActivity : AppCompatActivity() {

    @Inject
    lateinit var getThemeMode: GetThemeMode
    private val viewModel: AskForDisplayOverViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.TRANSPARENT

        viewModel.onOpenDialog()

        collectFlow(viewModel.finish) { shouldFinish ->
            if (shouldFinish) {
                finish()
            }
        }

        collectFlow(viewModel.toSettings) { allow ->
            if (allow) {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                )
                viewModel.onFinishActivity()
            }
        }

        collectFlow(viewModel.dismiss) { notNow ->
            if (notNow) {
                // This will keep showing after the activity is destroyed, so can't use snack bar.
                Toast.makeText(
                    this,
                    R.string.ask_for_display_over_explain,
                    Toast.LENGTH_LONG
                )
                    .show()

                viewModel.onFinishActivity()
            }
        }

        setContent {
            val showDialogState: Boolean by viewModel.showDialog.collectAsStateWithLifecycle()
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            AndroidTheme(isDark = themeMode.isDarkMode()) {
                AskForDisplayOverDialogView(showDialogState)
            }
        }
    }

    @Composable
    private fun AskForDisplayOverDialogView(showDialogState: Boolean) {
        AskForDisplayOverDialog(
            show = showDialogState,
            onAllow = {
                viewModel.onAllow()
            },
            onNotNow = {
                viewModel.onNotNow()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onDestroy()
    }

    override fun attachBaseContext(newBase: Context?) {
        /**
         * When selecting a non supported locale and then a supported locale in the language settings
         * causes a strange error in which the order of the two locales get randomly flipped.
         * This causes some resources to be loaded in the supported language and others in the
         * default language. I don't know what causes that to happen, but this code removes
         * unsupported locales from the configuration as a measure to prevent the strange behaviour.
         **/

        val supportedLanguages = listOf(
            "en",
            "ar",
            "de",
            "es",
            "fr",
            "id",
            "jt",
            "ja",
            "ko",
            "nl",
            "pl",
            "pt",
            "ro",
            "ru",
            "th",
            "vi",
            "zh",
        )

        newBase?.resources?.configuration?.let {
            val locales = it.locales
            val newLocales = (0 until locales.size()).mapNotNull { i ->
                locales[i].takeIf { locale -> supportedLanguages.contains(locale.language) }
            }.toTypedArray()
            it.setLocales(LocaleList(*newLocales))
        }
        super.attachBaseContext(newBase)
    }
}
