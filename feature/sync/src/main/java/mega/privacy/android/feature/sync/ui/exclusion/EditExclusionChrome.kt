package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.button.InlineAnchoredButtonGroup
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.Button
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The chrome every Edit exclusion screen shares: the title, the trash action and the Cancel/Save
 * bar, around whichever form the rule being edited calls for.
 *
 * Owned here rather than by either form so that the by-name and by-size screens cannot drift
 * apart, and so that neither form has to know it is one of two.
 *
 * Deleting asks for no confirmation, because the screen hands the deletion back for the
 * exclusions list to stage: Cancel there still undoes it.
 *
 * @param onDelete invoked when the user takes the exclusion out through the trash action. It
 * carries no argument because the trash removes the whole exclusion, never part of one
 * @param onSave invoked when the user keeps the exclusion as the form now reads it
 * @param onDismiss invoked when the user backs out or cancels, discarding the draft
 */
@Composable
internal fun EditExclusionScaffold(
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MegaScaffold(
        modifier = modifier,
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(EDIT_EXCLUSION_TOP_BAR_TAG),
                title = stringResource(sharedR.string.sync_exclusions_edit_title),
                navigationType = AppBarNavigationType.Back(onDismiss),
                actions = listOf(CommonMenuAction.Delete),
                onActionPressed = { onDelete() },
            )
        },
        bottomBar = {
            InlineAnchoredButtonGroup(
                modifier = Modifier.testTag(EDIT_EXCLUSION_BUTTON_GROUP_TAG),
                buttonGroup = listOf(
                    {
                        Button.TextOnlyButtonM3(
                            text = stringResource(sharedR.string.general_dialog_cancel_button),
                            onClick = onDismiss,
                        )
                    },
                    {
                        Button.PrimaryButtonM3(
                            text = stringResource(sharedR.string.general_action_save),
                            onClick = onSave,
                        )
                    },
                ),
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
        },
    )
}

internal const val EDIT_EXCLUSION_TOP_BAR_TAG = "edit_exclusion:top_bar"
internal const val EDIT_EXCLUSION_BUTTON_GROUP_TAG = "edit_exclusion:button_group"
