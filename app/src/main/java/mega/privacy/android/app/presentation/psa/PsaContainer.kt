package mega.privacy.android.app.presentation.psa

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.psa.view.InfoPsaView
import mega.privacy.android.app.presentation.psa.view.PsaView
import mega.privacy.android.app.presentation.psa.view.WebPsaView
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetContainer
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Psa container
 *
 * @param viewModel
 * @param content
 */
@Composable
fun PsaContainer(
    navigateToPsaPage: (Context, String) -> Unit = ::navigateToWebView,
    viewModel: PsaViewModel = hiltViewModel(),
    content: @Composable () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Box {
        content()
        PsaContainerContent(
            state = state,
            markAsSeen = viewModel::markAsSeen,
            navigateToPsaPage = { url ->
                coroutineScope.launch {
                    navigateToPsaPage(
                        context,
                        url
                    )
                }
            },
        )
    }
}

@Composable
internal fun PsaContainerContent(
    state: PsaState,
    markAsSeen: (Int) -> Unit,
    navigateToPsaPage: (String) -> Unit,
) {
    when (state) {
        is PsaState.NoPsa -> {}

        is PsaState.WebPsa -> {
            WebPsaView(
                psa = state,
                markAsSeen = { markAsSeen(state.id) }
            )
        }

        is PsaState.StandardPsa -> {
            NestedPsaView { modifier: Modifier ->
                PsaView(
                    title = state.title,
                    text = state.text,
                    imageUrl = state.imageUrl,
                    positiveText = state.positiveText,
                    onPositiveTapped = {
                        navigateToPsaPage(state.positiveLink)
                        markAsSeen(state.id)
                    },
                    onDismiss = { markAsSeen(state.id) },
                    modifier = modifier
                )
            }
        }

        is PsaState.InfoPsa -> {
            NestedPsaView { modifier: Modifier ->
                InfoPsaView(
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
private fun NestedPsaView(
    psaView: @Composable (Modifier) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        MegaBottomSheetContainer(
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
    val intent = Intent(context, WebViewActivity::class.java)
    intent.data = Uri.parse(psaUrl)
    context.startActivity(intent)
}

@CombinedThemePreviews
@Composable
private fun PsaContainerPreview(@PreviewParameter(PsaStatePreviewParameterProvider::class) psaState: PsaState) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        PsaContainerContent(
            state = psaState,
            markAsSeen = {},
            navigateToPsaPage = {},
        )

    }
}


internal class PsaStatePreviewParameterProvider : PreviewParameterProvider<PsaState> {
    override val values: Sequence<PsaState> = sequenceOf(
        PsaState.WebPsa(
            id = 1,
            url = "https://www.mega.nz/psa",
        ),
        PsaState.StandardPsa(
            id = 2,
            title = "Standard PSA",
            text = "This is a standard PSA",
            imageUrl = null,
            positiveText = "Positive",
            positiveLink = "https://www.mega.nz/psa",
        ),
        PsaState.InfoPsa(
            id = 3,
            title = "Info PSA",
            text = "This is an info PSA",
            imageUrl = null,
        ),
    )

}
