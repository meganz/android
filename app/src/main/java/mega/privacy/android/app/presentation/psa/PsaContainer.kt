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
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.extensions.launchUrl
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.psa.view.InfoPsaView
import mega.privacy.android.app.presentation.psa.view.PsaView
import mega.privacy.android.app.presentation.psa.view.WebPsaView
import mega.privacy.android.shared.original.core.ui.controls.sheets.MegaBottomSheetContainer
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

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

    PsaContentView(
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
 * @param containerModifier - Workaround for legacy screens
 * @param innerModifier - Workaround for legacy screens
 * @param navigateToPsaPage
 * @param content
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PsaContentView(
    context: Context,
    state: PsaState,
    coroutineScope: CoroutineScope,
    markAsSeen: (Int) -> Unit,
    containerModifier: Modifier = Modifier,
    innerModifier: (Modifier) -> Modifier = { it },
    navigateToPsaPage: (Context, String) -> Unit = ::navigateToWebView,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.semantics {
        testTagsAsResourceId = true
    }
    ) {
        content()
        PsaStateView(
            state = state,
            markAsSeen = markAsSeen,
            navigateToPsaPage = { url ->
                coroutineScope.launch {
                    navigateToPsaPage(
                        context,
                        url
                    )
                }
            },
            containerModifier = containerModifier,
            innerModifier = innerModifier,
        )
    }
}


@Composable
internal fun PsaStateView(
    state: PsaState,
    markAsSeen: (Int) -> Unit,
    navigateToPsaPage: (String) -> Unit,
    containerModifier: Modifier,
    innerModifier: (Modifier) -> Modifier,
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
            NestedPsaView(containerModifier) { modifier: Modifier ->
                val psaModifier = innerModifier(modifier)
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
                    modifier = psaModifier
                )
            }
        }

        is PsaState.InfoPsa -> {
            NestedPsaView(containerModifier) { modifier: Modifier ->
                val psaModifier = innerModifier(modifier)
                InfoPsaView(
                    title = state.title,
                    text = state.text,
                    imageUrl = state.imageUrl,
                    onDismiss = { markAsSeen(state.id) },
                    modifier = psaModifier,
                )
            }
        }
    }
}

@Composable
private fun NestedPsaView(
    containerModifier: Modifier,
    psaView: @Composable (Modifier) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        MegaBottomSheetContainer(
            modifier = containerModifier
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
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        PsaStateView(
            state = psaState,
            markAsSeen = {},
            navigateToPsaPage = {},
            containerModifier = Modifier,
            innerModifier = { it },
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
