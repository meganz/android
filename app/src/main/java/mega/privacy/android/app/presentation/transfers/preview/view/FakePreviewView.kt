package mega.privacy.android.app.presentation.transfers.preview.view

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.qrcode.findActivity
import mega.privacy.android.app.presentation.transfers.preview.FakePreviewFragment.Companion.EXTRA_ERROR
import mega.privacy.android.app.presentation.transfers.preview.FakePreviewFragment.Companion.EXTRA_FILE_PATH
import mega.privacy.android.app.presentation.transfers.preview.model.FakePreviewState
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.appbar.AppBarType
import mega.privacy.android.shared.original.core.ui.controls.appbar.MegaAppBar
import mega.privacy.android.shared.original.core.ui.controls.layouts.MegaScaffold
import mega.privacy.android.shared.original.core.ui.controls.progressindicator.MegaAnimatedLinearProgressIndicator
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.resources.R as sharedResR

@OptIn(ExperimentalMaterialNavigationApi::class, ExperimentalComposeUiApi::class)
@Composable
internal fun FakePreviewView(
    scaffoldState: ScaffoldState,
    onBackPress: () -> Unit,
    uiState: FakePreviewState,
    consumeTransferEvent: () -> Unit,
    navigateToStorageSettings: () -> Unit,
) {
    with(uiState) {
        MegaScaffold(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .semantics { testTagsAsResourceId = true }
                .testTag(TEST_TAG_FAKE_PREVIEW),
            scaffoldState = scaffoldState,
            topBar = {
                Column {
                    MegaAppBar(
                        appBarType = AppBarType.BACK_NAVIGATION,
                        title = fileName ?: "",
                        onNavigationPressed = onBackPress,
                    )
                    MegaAnimatedLinearProgressIndicator(
                        modifier = Modifier.testTag(TEST_TAG_PROGRESS_BAR),
                        indicatorProgress = progress.floatValue,
                        progressAnimDuration = if (progress.floatValue > 0.5f) 1000 else 3000,
                        height = 4.dp,
                        strokeCap = StrokeCap.Square,
                    )
                }
            },
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                fileTypeResId?.let { id ->
                    Image(
                        modifier = Modifier
                            .size(96.dp)
                            .testTag(TEST_TAG_FILE_TYPE_ICON),
                        painter = painterResource(id = id),
                        contentDescription = "StorageStatusImage"
                    )
                }
                MegaText(
                    text = stringResource(sharedResR.string.transfers_fake_preview_text),
                    textColor = TextColor.Secondary,
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(TEST_TAG_LOADING_TEST)
                )
            }
        }

        previewFilePathToOpen?.let { path ->
            LocalContext.current.findActivity()?.apply {
                setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_FILE_PATH, path) })
                finish()
            }
        }

        error?.let {
            LocalContext.current.findActivity()?.apply {
                val intent = when (error) {
                    is QuotaExceededMegaException -> Intent()
                    else -> Intent().apply {
                        putExtra(EXTRA_ERROR, stringResource(R.string.error_temporary_unavaible))
                    }
                }

                setResult(RESULT_OK, intent)
                finish()
            }
        }

        StartTransferComponent(
            event = uiState.transferEvent,
            onConsumeEvent = consumeTransferEvent,
            snackBarHostState = scaffoldState.snackbarHostState,
            navigateToStorageSettings = navigateToStorageSettings,
        )
    }
}

@OptIn(ExperimentalMaterialNavigationApi::class)
@CombinedThemePreviews
@Composable
private fun FakePreviewViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FakePreviewView(
            scaffoldState = rememberScaffoldState(),
            onBackPress = {},
            uiState = FakePreviewState(
                fileName = "Porter_final.ai",
                fileTypeResId = iconPackR.drawable.ic_generic_medium_solid,
                progress = Progress(0.3f),
            ),
            consumeTransferEvent = {},
            navigateToStorageSettings = {},
        )
    }
}

internal const val TEST_TAG_FAKE_PREVIEW = "fake_preview_view"
internal const val TEST_TAG_PROGRESS_BAR = "$TEST_TAG_FAKE_PREVIEW:progress_bar"
internal const val TEST_TAG_FILE_TYPE_ICON = "$TEST_TAG_FAKE_PREVIEW:file_type_icon"
internal const val TEST_TAG_LOADING_TEST = "$TEST_TAG_FAKE_PREVIEW:loading_test"