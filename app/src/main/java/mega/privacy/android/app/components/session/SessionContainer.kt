package mega.privacy.android.app.components.session

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.utils.Constants

/**
 * Session container, check session is ready or not.
 * If not, navigate to login page.
 *
 * @param content
 */
@Composable
internal fun SessionContainer(
    shouldCheckChatSession: Boolean = false,
    viewModel: SessionViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    LaunchedEffect(Unit) {
        viewModel.checkSdkSession(shouldCheckChatSession)
    }
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        // if root node exists and chat session is valid, show the content
        // in case shouldCheckChatSession is false, we don't need to check chat session
        state.isRootNodeExists == true && (!shouldCheckChatSession || state.isChatSessionValid) -> content()
        state.isRootNodeExists == false -> navigateToLogin(context)
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