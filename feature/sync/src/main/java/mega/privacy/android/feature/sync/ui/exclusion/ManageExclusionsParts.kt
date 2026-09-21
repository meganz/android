package mega.privacy.android.feature.sync.ui.exclusion

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.chip.ChipTrailingAction
import mega.android.core.ui.components.chip.MegaChip
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

/** A heading over one group of exclusions. */
@Composable
internal fun ExclusionSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    MegaText(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { heading() },
        text = text,
        textColor = TextColor.Secondary,
        style = AppTheme.typography.titleSmall,
    )
}

/**
 * One excluded file extension, with the control that removes it.
 *
 * The design offers no way to edit an extension — changing one is a delete and a re-add — so the
 * only action is the trailing dismiss. `MegaChip` takes its body click as non-null and declares
 * the body selectable regardless, so a screen reader still offers to toggle the chip and that
 * toggle does nothing; removing it needs a nullable click on the component itself.
 */
@Composable
internal fun ExclusionExtensionChip(
    extension: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaChip(
        modifier = modifier,
        selected = false,
        text = extension,
        onClick = {},
        // the design's dismiss glyph is 16dp and the icon pack has no bare X at that size, so the
        // 24dp one stands in until SAT-2397 adds it
        trailingIcon = iconPackR.drawable.ic_x_medium_thin_outline,
        trailingAction = ChipTrailingAction(
            contentDescription = stringResource(
                sharedR.string.sync_exclusions_manage_remove_extension,
                extension,
            ),
            onClick = onRemove,
        ),
    )
}

/**
 * Shown when the folder has nothing this screen can draw.
 *
 * @param hasUnshownRules whether the file nonetheless excludes something, through lines this
 * screen does not model — a regex rule, a re-include, anything another client wrote. The usual
 * copy states that everything syncs, which would be false for such a file
 */
@Composable
internal fun ExclusionsEmptyState(
    hasUnshownRules: Boolean,
    modifier: Modifier = Modifier,
) {
    ExclusionsMessage(
        title = stringResource(sharedR.string.sync_exclusions_manage_empty_title),
        body = stringResource(
            if (hasUnshownRules) {
                sharedR.string.sync_exclusions_manage_empty_body_unshown
            } else {
                sharedR.string.sync_exclusions_manage_empty_body
            },
        ),
        modifier = modifier,
    )
}

/**
 * Shown when sync refuses the file outright.
 *
 * Drawn instead of the rules, never beside them: a refused file excludes nothing, so listing its
 * rules would report the opposite of what the sync is doing.
 */
@Composable
internal fun ExclusionsRejected(
    modifier: Modifier = Modifier,
) {
    ExclusionsMessage(
        title = stringResource(sharedR.string.sync_exclusions_manage_rejected_title),
        body = stringResource(sharedR.string.sync_exclusions_manage_rejected_body),
        modifier = modifier,
    )
}

/**
 * A message shown in place of the list.
 *
 * The design system owns the centring and spacing, so the message is handed to [EmptyStateView]
 * rather than laid out here: a hand-rolled column sits wherever its parent puts it, which left
 * these messages hanging under the top bar instead of in the middle of the space the list would
 * have filled.
 */
@Composable
private fun ExclusionsMessage(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: (@Composable ColumnScope.() -> Unit)? = null,
) {
    EmptyStateView(
        modifier = modifier.fillMaxSize(),
        illustration = null,
        title = title,
        description = SpannableText(body),
        actions = actions,
    )
}

/**
 * Shown when the exclusions could not be read.
 *
 * Deliberately not the empty state. Presenting a failed read as "nothing is excluded" would invite
 * the user to save that over a file that may hold rules.
 */
@Composable
internal fun ExclusionsLoadError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExclusionsMessage(
        title = stringResource(sharedR.string.sync_exclusions_manage_load_error_title),
        body = stringResource(sharedR.string.sync_exclusions_manage_load_error_body),
        modifier = modifier,
        actions = {
            PrimaryFilledButton(
                modifier = Modifier.wrapContentSize(),
                text = stringResource(sharedR.string.sync_exclusions_manage_load_error_retry),
                onClick = onRetry,
            )
        },
    )
}
