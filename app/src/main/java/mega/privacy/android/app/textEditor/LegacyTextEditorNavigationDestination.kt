package mega.privacy.android.app.textEditor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.feature.texteditor.presentation.TextEditorComposeViewModel
import mega.privacy.android.feature.texteditor.presentation.TextEditorScreen
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.LegacyTextEditorNavKey

/**
 * Legacy text editor destination. When [LegacyTextEditorNavKey.isTextEditorComposeEnabled] is true,
 * shows [TextEditorScreen]; otherwise starts [TextEditorActivity] and pops this destination.
 */
fun EntryProviderScope<NavKey>.legacyTextEditorScreen(removeDestination: () -> Unit) {
    entry<LegacyTextEditorNavKey>(
        metadata = transparentMetadata()
    ) { key ->
        TextEditorEntry(
            navKey = key,
            removeDestination = removeDestination,
        )
    }
}

@Composable
private fun TextEditorEntry(
    navKey: LegacyTextEditorNavKey,
    removeDestination: () -> Unit,
) {
    val context = LocalContext.current

    if (navKey.isTextEditorComposeEnabled) {
        val viewModel =
            hiltViewModel<TextEditorComposeViewModel, TextEditorComposeViewModel.Factory> { factory ->
                val mode = TextEditorMode.entries.find { it.value == navKey.mode } ?: TextEditorMode.View
                factory.create(
                    TextEditorComposeViewModel.Args(
                        nodeHandle = navKey.nodeHandle,
                        mode = mode,
                        nodeSourceType = navKey.nodeSourceType,
                        fileName = navKey.fileName,
                    )
                )
            }
        TextEditorScreen(
            viewModel = viewModel,
            onBack = removeDestination,
        )
    } else {
        LaunchedEffect(Unit) {
            context.startActivity(
                TextEditorActivity.createIntent(
                    context = context,
                    nodeHandle = navKey.nodeHandle,
                    mode = navKey.mode,
                    nodeSourceType = navKey.nodeSourceType,
                    fileName = navKey.fileName,
                )
            )
            removeDestination()
        }
    }
}
