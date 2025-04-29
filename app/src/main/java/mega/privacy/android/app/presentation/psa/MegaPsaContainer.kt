package mega.privacy.android.app.presentation.psa

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.components.surface.ColumnSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.theme.AndroidTheme
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.psa.view.MegaPsaView
import mega.privacy.android.app.presentation.psa.view.SharedInfoPsaView
import mega.privacy.android.app.presentation.psa.view.WebPsaView
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * Psa container
 *
 * @param viewModel
 * @param content
 */
@Composable
fun MegaPsaContainer(
    navigateToPsaPage: (Context, String) -> Unit = ::navigateToWebView,
    viewModel: PsaViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    MegaPsaContentView(
        content = content,
        state = state,
        coroutineScope = coroutineScope,
        navigateToPsaPage = navigateToPsaPage,
        context = context,
        markAsSeen = viewModel::markAsSeen
    )
}

/**
 * Psa content view
 *
 * @param context
 * @param state
 * @param coroutineScope
 * @param markAsSeen
 * @param navigateToPsaPage
 * @param content
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MegaPsaContentView(
    context: Context,
    state: PsaState,
    coroutineScope: CoroutineScope,
    markAsSeen: (Int) -> Unit,
    navigateToPsaPage: (Context, String) -> Unit = ::navigateToWebView,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.semantics {
        testTagsAsResourceId = true
    }) {
        content()
        MegaPsaStateView(
            state = state,
            markAsSeen = markAsSeen,
            navigateToPsaPage = { url ->
                navigateToPsaPage(
                    context, url
                )
            },
        )
    }
}


@Composable
internal fun MegaPsaStateView(
    state: PsaState,
    markAsSeen: (Int) -> Unit,
    navigateToPsaPage: (String) -> Unit,
) {
    when (state) {
        is PsaState.NoPsa -> {}

        is PsaState.WebPsa -> {
            WebPsaView(
                psa = state, markAsSeen = { markAsSeen(state.id) })
        }

        is PsaState.StandardPsa -> {
            MegaNestedPsaView { modifier: Modifier ->
                MegaPsaView(
                    title = state.title,
                    text = state.text,
                    imageUrl = state.imageUrl,
                    positiveText = state.positiveText,
                    onPositiveTapped = {
                        navigateToPsaPage(state.positiveLink)
                        markAsSeen(state.id)
                    },
                    onDismiss = { markAsSeen(state.id) },
                    modifier = modifier,
                )
            }
        }

        is PsaState.InfoPsa -> {
            MegaNestedPsaView { modifier: Modifier ->
                SharedInfoPsaView(
                    title = state.title,
                    text = state.text,
                    imageUrl = state.imageUrl,
                    onDismiss = { markAsSeen(state.id) },
                    modifier = modifier,
                )
            }
        }
    }
}

@Composable
private fun MegaNestedPsaView(
    psaView: @Composable (Modifier) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        ColumnSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .shadow(6.dp)
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            psaView(
                Modifier
                    .navigationBarsPadding()
                    .padding(top = 24.dp)
            )
        }
    }
}

private fun navigateToWebView(context: Context, psaUrl: String) {
    context.launchUrl(psaUrl)
}

@CombinedThemePreviews
@Composable
private fun PsaContainerPreview(@PreviewParameter(PsaStatePreviewParameterProvider::class) psaState: PsaState) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MegaPsaStateView(
            state = psaState,
            markAsSeen = {},
            navigateToPsaPage = {},
        )

    }
}

