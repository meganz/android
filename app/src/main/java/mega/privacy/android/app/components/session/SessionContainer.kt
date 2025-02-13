package mega.privacy.android.app.components.session

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
    shouldFinish: Boolean = true,
    viewModel: SessionViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.checkSdkSession()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()


    when (state.isRootNodeExists) {
        true ->
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

        false -> navigateToLogin(shouldFinish)
        null -> {}
    }
}

@Composable
private fun navigateToLogin(shouldFinish: Boolean) {
    val context = LocalContext.current
    context.findActivity()?.let { activity ->
        val intent = Intent(context, LoginActivity::class.java).apply {
            if (shouldFinish) {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            putExtra(Constants.LAUNCH_INTENT, activity.intent)
        }
        context.startActivity(intent)
        if (shouldFinish) {
            activity.finish()
        }
    }
}