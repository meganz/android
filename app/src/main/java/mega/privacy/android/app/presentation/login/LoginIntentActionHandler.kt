package mega.privacy.android.app.presentation.login

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import mega.privacy.android.app.R
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.login.model.LoginIntentState
import mega.privacy.android.app.presentation.login.model.LoginState
import mega.privacy.android.app.utils.Constants
import timber.log.Timber

/**
 * Handles the login intent actions for LoginActivity.
 * LoginActivity only stays open for ACTION_FILE_EXPLORER_UPLOAD intents;
 * all other actions are redirected to MegaActivity.
 */
@Composable
fun LoginIntentActionHandler(viewModel: LoginViewModel, uiState: LoginState) {
    val activity = LocalActivity.current ?: return
    val intentAction = activity.intent?.action

    val finishSetupIntent = remember {
        {
            if (intentAction == Constants.ACTION_FILE_EXPLORER_UPLOAD) {
                Timber.d("ACTION_FILE_EXPLORER_UPLOAD: credentials null, show login_before_share")
                viewModel.setSnackbarMessageId(R.string.login_before_share)
            }
            viewModel.intentSet()
        }
    }

    val readyToFinish = remember {
        {
            val intent = activity.intent
            if (intentAction == Constants.ACTION_FILE_EXPLORER_UPLOAD
                && intent?.type == Constants.TYPE_TEXT_PLAIN
            ) {
                Timber.d("Intent to FileExplorerActivity")
                activity.startActivity(
                    Intent(activity, FileExplorerActivity::class.java)
                        .putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT))
                        .putExtra(Intent.EXTRA_SUBJECT, intent.getStringExtra(Intent.EXTRA_SUBJECT))
                        .putExtra(Intent.EXTRA_EMAIL, intent.getStringExtra(Intent.EXTRA_EMAIL))
                        .setAction(Intent.ACTION_SEND)
                        .setType(Constants.TYPE_TEXT_PLAIN)
                )
                activity.finish()
            } else if (intent?.getBooleanExtra(
                    FileExplorerActivity.EXTRA_FROM_SHARE,
                    false
                ) == true
            ) {
                Timber.d("Intent to share")
                activity.startActivity(
                    intent.apply {
                        action = Constants.ACTION_FILE_EXPLORER_UPLOAD
                        setClass(activity, FileExplorerActivity::class.java)
                    }
                )
                activity.finish()
            }
        }
    }

    LaunchedEffect(uiState.intentState) {
        uiState.intentState?.let {
            when (it) {
                LoginIntentState.ReadyForInitialSetup -> {
                    Timber.d("Ready to initial setup")
                    finishSetupIntent()
                }

                LoginIntentState.ReadyForFinalSetup -> {
                    Timber.d("Ready to finish")
                    readyToFinish()
                }

                LoginIntentState.AlreadySet -> Unit
            }
        }
    }
}
