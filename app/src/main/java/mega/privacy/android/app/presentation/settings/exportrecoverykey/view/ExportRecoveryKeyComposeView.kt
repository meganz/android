package mega.privacy.android.app.presentation.settings.exportrecoverykey.view

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.print.PrintHelper
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.exportrecoverykey.model.RecoveryKeyUIState
import mega.privacy.android.core.ui.controls.dialogs.MegaAlertDialog
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.white
import java.io.File

private typealias ExportRecoveryAction = () -> Unit

/**
 * Test tag for Compose Column - Action group
 */
const val COLUMN_TEST_TAG = "column_test_tag"

/**
 * Test tag for Compose Row - Action group
 */
const val ROW_TEST_TAG = "row_test_tag"

/**
 * Test tag for message SnackBar
 */
const val SNACKBAR_TEST_TAG = "snackbar_test_tag"

/**
 * Scaffold for Export Recovery Key Activity
 */
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun ExportRecoveryKeyView(
    uiState: RecoveryKeyUIState,
    onSnackBarShown: ExportRecoveryAction,
    onButtonOverflow: ExportRecoveryAction,
    onClickPrint: ExportRecoveryAction,
    onClickCopy: ExportRecoveryAction,
    onClickSave: ExportRecoveryAction,
    onPrintRecoveryKeyConsumed: () -> Unit,
    onPrintRecoveryKeyCompleted: (File) -> Unit,
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    var errorAlertMessage by remember { mutableStateOf<String?>(null) }

    EventEffect(
        event = uiState.printRecoveryKey,
        onConsumed = onPrintRecoveryKeyConsumed
    ) { file ->
        file?.let {
            printRecoveryKey(context, it, onPrintRecoveryKeyCompleted)
        } ?: run {
            errorAlertMessage = context.getString(R.string.general_text_error)
        }
    }

    Scaffold(
        scaffoldState = rememberScaffoldState(),
        snackbarHost = {
            SnackbarHost(hostState = snackBarHostState) { data ->
                Snackbar(
                    modifier = Modifier.testTag(SNACKBAR_TEST_TAG),
                    snackbarData = data,
                    backgroundColor = black.takeIf { MaterialTheme.colors.isLight } ?: white
                )
            }
        },
    ) {
        uiState.message?.let {
            LaunchedEffect(it) {
                snackBarHostState.showSnackbar(it)
                onSnackBarShown()
            }
        }

        ExportRecoveryKeyCompose(
            isActionGroupVertical = uiState.isActionGroupVertical,
            onButtonOverflow = onButtonOverflow,
            onClickPrint = onClickPrint,
            onClickCopy = onClickCopy,
            onClickSave = onClickSave
        )

        errorAlertMessage?.let { message ->
            MegaAlertDialog(
                text = message,
                confirmButtonText = stringResource(id = R.string.general_ok),
                cancelButtonText = null,
                onConfirm = { errorAlertMessage = null },
                onDismiss = { errorAlertMessage = null }
            )
        }
    }
}

/**
 * Main Compose View for Export Recovery Key Activity
 */
@Composable
fun ExportRecoveryKeyCompose(
    isActionGroupVertical: Boolean,
    onButtonOverflow: ExportRecoveryAction,
    onClickPrint: ExportRecoveryAction,
    onClickCopy: ExportRecoveryAction,
    onClickSave: ExportRecoveryAction,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.backup_title),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(top = 17.dp)
        )
        Text(
            text = stringResource(id = R.string.backup_subtitle),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(top = 33.dp)
        )
        Text(
            text = stringResource(id = R.string.backup_first_paragraph),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.textColorSecondary
            ),
            modifier = Modifier.padding(top = 20.dp)
        )
        Text(
            text = stringResource(id = R.string.backup_second_paragraph),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.textColorSecondary
            ),
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = stringResource(id = R.string.backup_third_paragraph),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(top = 24.dp)
        )
        Text(
            text = stringResource(id = R.string.backup_action),
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            style = MaterialTheme.typography.body2.copy(
                color = MaterialTheme.colors.textColorSecondary
            ),
            modifier = Modifier.padding(top = 20.dp)
        )

        if (isActionGroupVertical) {
            Column(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .testTag(COLUMN_TEST_TAG)
            ) {
                ExportRecoveryVerticalButton(
                    text = stringResource(id = R.string.context_option_print),
                    onClick = onClickPrint,
                    onButtonOverflow = onButtonOverflow
                )
                ExportRecoveryVerticalButton(
                    text = stringResource(id = R.string.context_copy),
                    onClick = onClickCopy,
                    onButtonOverflow = onButtonOverflow
                )
                ExportRecoveryVerticalButton(
                    text = stringResource(id = R.string.save_action),
                    onClick = onClickSave,
                    onButtonOverflow = onButtonOverflow
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .testTag(ROW_TEST_TAG)
            ) {
                ExportRecoveryHorizontalButton(
                    text = stringResource(id = R.string.context_option_print),
                    onClick = onClickPrint,
                    onButtonOverflow = onButtonOverflow
                )
                ExportRecoveryHorizontalButton(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(id = R.string.context_copy),
                    onClick = onClickCopy,
                    onButtonOverflow = onButtonOverflow
                )
                ExportRecoveryHorizontalButton(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(id = R.string.save_action),
                    onClick = onClickSave,
                    onButtonOverflow = onButtonOverflow
                )
            }
        }
    }
}

/**
 * Compose Action Button for Export Recovery Key Activity
 */
@Composable
fun ExportRecoveryHorizontalButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: ExportRecoveryAction,
    onButtonOverflow: ExportRecoveryAction,
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            backgroundColor = Color.Transparent, contentColor = MaterialTheme.colors.secondary
        ),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colors.secondary),
        modifier = modifier
    ) {
        ActionButtonText(text = text, onButtonOverflow = onButtonOverflow)
    }
}

/**
 * Compose Action Button for Export Recovery Key Activity
 */
@Composable
fun ExportRecoveryVerticalButton(
    text: String,
    onClick: ExportRecoveryAction,
    onButtonOverflow: ExportRecoveryAction,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = Color.Transparent, contentColor = MaterialTheme.colors.secondary
        ),
    ) {
        ActionButtonText(text = text, onButtonOverflow = onButtonOverflow)
    }
}

/**
 * Action Button Text
 */
@Composable
fun ActionButtonText(
    text: String,
    onButtonOverflow: ExportRecoveryAction,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.button,
        color = MaterialTheme.colors.secondary,
        onTextLayout = { result ->
            if (result.lineCount > 1) {
                onButtonOverflow.invoke()
            }
        }
    )
}

private fun printRecoveryKey(
    context: Context,
    file: File,
    onPrintRecoveryKeyCompleted: (File) -> Unit
) {
    PrintHelper(context).apply {
        scaleMode = PrintHelper.SCALE_MODE_FIT
        printBitmap("rKPrint", file.toUri()) {
            onPrintRecoveryKeyCompleted(file)
        }
    }
}
