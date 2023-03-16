package mega.privacy.android.app.presentation.chat.dialog

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.MegaApplication
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
class AskForDisplayOverActivity : ComponentActivity() {

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
                    MegaApplication.getInstance().applicationContext,
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
}
