package mega.privacy.android.app.components.chatsession

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import timber.log.Timber

/**
 * Session container, check session is ready or not.
 * If not, navigate to login page.
 *
 * @param optimistic If true, assumes that the SDK session exists while waiting for a response. That way it starts showing the content immediately
 * @param loadingView Composable function that will be shown while waiting for the check session result. Use { DefaultLoadingSessionView() } for basic implementation.
 * @param content
 */
@Composable
internal fun ChatSessionContainer(
    viewModel: ChatSessionViewModel = hiltViewModel(),
    optimistic: Boolean = false,
    loadingView: @Composable () -> Unit = { },
    content: @Composable () -> Unit,
) {
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        viewModel.checkChatSession(optimistic)
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
    } else if (state is ChatSessionState.Pending) {
        loadingView()
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

@Composable
@CombinedThemePreviews
private fun ChatSessionContainerPreview(
    @PreviewParameter(BooleanProvider::class) optimistic: Boolean,
) {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        ChatSessionContainer(optimistic = optimistic) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MegaText("Content", TextColor.Primary)
            }
        }
    }
}