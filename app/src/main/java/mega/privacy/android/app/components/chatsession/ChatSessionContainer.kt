package mega.privacy.android.app.components.chatsession

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.utils.Constants
import timber.log.Timber

/**
 * Session container, check session is ready or not.
 * If not, navigate to login page.
 *
 * @param content
 */
@Composable
internal fun ChatSessionContainer(
    viewModel: ChatSessionViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.checkChatSession()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is ChatSessionState.Invalid) {
            Timber.d("Chat session not valid. Navigating to login")
            navigateToLogin(context)
        }
    }

    if (state is ChatSessionState.Valid) {
        Timber.d("Chat session is valid. Displaying content")
        content()
    }
}

private fun navigateToLogin(context: Context) {
    context.findActivity()?.let { activity ->
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Constants.LAUNCH_INTENT, activity.intent)
        }
        context.startActivity(intent)
        activity.finish()
    }
}