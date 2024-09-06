package mega.privacy.android.app.components.session

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
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
    shouldFinish: Boolean = true,
    viewModel: SessionViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.checkSdkSession(shouldCheckChatSession)
    }
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    when {
        // if root node exists and chat session is valid, show the content
        // in case shouldCheckChatSession is false, we don't need to check chat session
        state.isRootNodeExists == true && (!shouldCheckChatSession || state.isChatSessionValid) ->
            Box(modifier = Modifier.pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press) {
                            viewModel.retryConnectionsAndSignalPresence()
                        }
                    } while (event.changes.any { it.pressed })
                }
            }) {
                content()
            }

        state.isRootNodeExists == false -> navigateToLogin(context, shouldFinish)
    }
}

private fun navigateToLogin(context: Context, shouldFinish: Boolean) {
    context.findActivity()?.let { activity ->
        val intent = Intent(context, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Constants.LAUNCH_INTENT, activity.intent)
        }
        context.startActivity(intent)
        if (shouldFinish) {
            activity.finish()
        }
    }
}