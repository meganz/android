package mega.privacy.android.feature.fileinfo.presentation.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.banner.InlineWarningBanner
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Warning banner shown at the top of the File Info details when the node has been taken down. Offers
 * an action to open the takedown guidance policy and a dismiss button that hides it for this screen
 * instance.
 *
 * @param isFile whether the node is a file (false for a folder), which selects the warning wording
 * @param onDisputeClick invoked when the action button is tapped
 */
@Composable
internal fun TakenDownBanner(
    isFile: Boolean,
    onDisputeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dismissed by rememberSaveable { mutableStateOf(false) }
    if (dismissed) return

    InlineWarningBanner(
        modifier = modifier,
        body = stringResource(
            if (isFile) {
                sharedR.string.file_info_taken_down_file_warning
            } else {
                sharedR.string.file_info_taken_down_folder_warning
            },
        ),
        showCancelButton = true,
        actionButtonText = stringResource(sharedR.string.file_info_taken_down_action),
        onActionButtonClick = onDisputeClick,
        onCancelButtonClick = { dismissed = true },
    )
}
