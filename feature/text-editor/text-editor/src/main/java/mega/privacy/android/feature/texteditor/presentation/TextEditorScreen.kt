package mega.privacy.android.feature.texteditor.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Compose screen for viewing and editing text files.
 * Skeleton only; full content in follow-up.
 */
@Composable
fun TextEditorScreen(
    viewModel: TextEditorComposeViewModel,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler {
        when {
            uiState.mode != TextEditorMode.View && uiState.isFileEdited -> onBack()
            uiState.mode == TextEditorMode.Edit -> viewModel.setViewMode()
            uiState.mode == TextEditorMode.Create -> {
                viewModel.saveFile(fromHome = false)
                onBack()
            }

            else -> onBack()
        }
    }

    MegaScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            MegaTopAppBar(
                title = uiState.fileName.ifEmpty {
                    stringResource(sharedR.string.general_new_text_file)
                },
                navigationType = AppBarNavigationType.Back(onNavigationIconClicked = onBack),
            )
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center,
        ) {
            if (!uiState.isLoading) {
                MegaText(text = "Text editor (Compose)\n${uiState.fileName}")
            }
        }
    }
}
