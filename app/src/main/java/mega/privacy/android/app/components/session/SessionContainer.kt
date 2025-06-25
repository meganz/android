package mega.privacy.android.app.components.session

import android.content.Intent
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Session container, check session is ready or not.
 * If not, navigate to login page.
 *
 * @param optimistic If true, assumes that the SDK session exists while waiting for a response. That way it starts showing the content immediately
 * @param loadingView Composable function that will be shown while waiting for the check session result. Use { DefaultLoadingSessionView() } for basic implementation.
 * @param content
 */
@Composable
internal fun SessionContainer(
    shouldFinish: Boolean = true,
    optimistic: Boolean = false,
    viewModel: SessionViewModel = hiltViewModel(),
    loadingView: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.checkSdkSession(optimistic)
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
        null -> loadingView()
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

@Composable
@CombinedThemePreviews
private fun SessionContainerPreview(
    @PreviewParameter(BooleanProvider::class) optimistic: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SessionContainer(optimistic = optimistic) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MegaText("Content", TextColor.Primary)
            }
        }
    }
}