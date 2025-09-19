package mega.privacy.android.core.nodecomponents.dialog.sharefolder

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.indicators.LargeHUD
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.resources.R as sharedResR

@Composable
fun ShareFolderAccessDialogM3(
    handles: List<Long>,
    contactData: List<String>,
    isFromBackups: Boolean,
    viewModel: ShareFolderAccessDialogViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    LaunchedEffect(isFromBackups) {
        if (isFromBackups) {
            viewModel.shareFolder(
                handles = handles,
                contactData = contactData,
                accessPermission = AccessPermission.READ
            )
            onDismiss()
        }
    }

    if (!isFromBackups) {
        ShareFolderAccessDialogBody(
            radioButtonOptions = listOf(
                AccessPermission.READ,
                AccessPermission.READWRITE,
                AccessPermission.FULL
            ),
            onItemSelected = {
                viewModel.shareFolder(
                    handles = handles,
                    contactData = contactData,
                    accessPermission = it
                )
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ShareFolderAccessDialogBody(
    radioButtonOptions: List<AccessPermission>,
    onItemSelected: (AccessPermission) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var selectedOption by remember {
        mutableStateOf<BasicDialogRadioOption?>(null)
    }

    BasicRadioDialog(
        options = radioButtonOptions
            .map { permission ->
                BasicDialogRadioOption(
                    ordinal = permission.ordinal,
                    text = permission.getUiText(context)
                )
            }.toImmutableList(),
        selectedOption = selectedOption,
        title = SpannableText(stringResource(id = sharedResR.string.share_folder_dialog_choose_permission_title)),
        onOptionSelected = {
            selectedOption = it
        },
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(id = sharedResR.string.general_dialog_cancel_button),
                onClick = onDismiss
            ),
            BasicDialogButton(
                text = stringResource(id = sharedResR.string.general_ok),
                onClick = onClick@{
                    val option = radioButtonOptions.find {
                        it.ordinal == selectedOption?.ordinal
                    } ?: return@onClick
                    onItemSelected(option)
                },
                enabled = selectedOption != null
            )
        ),
        onDismissRequest = onDismiss,
    )
}

private fun AccessPermission.getUiText(context: Context): String =
    when (this) {
        AccessPermission.READ -> context.getString(sharedResR.string.share_folder_dialog_read_only_radio_option)
        AccessPermission.READWRITE -> context.getString(sharedResR.string.share_folder_dialog_read_write_radio_option)
        AccessPermission.FULL -> context.getString(sharedResR.string.share_folder_dialog_full_access_radio_option)
        else -> ""
    }

@CombinedThemePreviews
@Composable
private fun ShareFolderAccessDialogBodyPreview() {
    AndroidThemeForPreviews {
        ShareFolderAccessDialogBody(
            radioButtonOptions = listOf(
                AccessPermission.READ,
                AccessPermission.READWRITE,
                AccessPermission.FULL
            ),
            onItemSelected = {},
            onDismiss = {}
        )
    }
}
