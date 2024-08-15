package mega.privacy.android.app.presentation.documentscanner

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber

/**
 * A Composable that holds views displaying the main Save Scanned Documents screen
 *
 * @param uiState The Save Scanned Documents UI State
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SaveScannedDocumentsView(
    uiState: SaveScannedDocumentsUiState,
) {
    val scaffoldState = rememberScaffoldState()
    val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    MegaScaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        scaffoldState = scaffoldState,
        topBar = {
            MegaAppBar(
                modifier = Modifier.testTag(SAVE_SCANNED_DOCUMENTS_TOOLBAR),
                title = stringResource(R.string.scan_title_save_scan),
                appBarType = AppBarType.BACK_NAVIGATION,
                onNavigationPressed = { onBackPressedDispatcher?.onBackPressed() },
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(padding),
            ) {
                // Add other UI Components here
                Timber.d("Filename: ${uiState.filename}")
            }
        }
    )
}

/**
 * A Composable Preview for [SaveScannedDocumentsView]
 */
@CombinedThemePreviews
@Composable
private fun SaveScannedDocumentsViewPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SaveScannedDocumentsView(
            uiState = SaveScannedDocumentsUiState(filename = "PDF"),
        )
    }
}

/**
 * Test Tags for Save Scanned Documents View
 */
internal const val SAVE_SCANNED_DOCUMENTS_TOOLBAR = "save_scanned_documents:mega_app_bar"