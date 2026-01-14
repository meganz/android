package mega.privacy.android.feature.payment.presentation.upgrade

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * External checkout information bottom sheet content
 *
 * @param showInformationNextTime Current state of the checkbox
 * @param onShowInformationNextTimeChanged Callback when checkbox state changes
 * @param onCancel Click handler for Cancel button
 * @param onContinue Click handler for Continue button
 * @param coroutineScope Coroutine scope for launching coroutines
 * @param domainUrl The domain URL to display in the information text
 */
@Composable
fun ExternalCheckoutInformationBottomSheetContent(
    showInformationNextTime: Boolean,
    onShowInformationNextTimeChanged: (Boolean) -> Unit,
    onCancel: () -> Unit,
    onContinue: () -> Unit,
    coroutineScope: CoroutineScope,
    domainUrl: String,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = spacing.x16, vertical = spacing.x24)
    ) {
        // Title
        MegaText(
            text = stringResource(id = sharedR.string.external_checkout_information_title),
            style = AppTheme.typography.titleMedium,
            textColor = TextColor.Primary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.x16)
                .testTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_TITLE)
        )

        // Information text
        MegaText(
            text = stringResource(
                id = sharedR.string.external_checkout_information_description,
                domainUrl
            ),
            style = AppTheme.typography.bodyMedium,
            textColor = TextColor.Secondary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.x24)
                .testTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_DESCRIPTION)
        )

        // Checkbox
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                modifier = Modifier
                    .size(24.dp)
                    .testTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CHECKBOX),
                checked = showInformationNextTime,
                onCheckStateChanged = onShowInformationNextTimeChanged,
                clickable = true,
            )
            Spacer(modifier = Modifier.size(spacing.x8))
            MegaText(
                modifier = Modifier
                    .width(352.dp)
                    .height(20.dp)
                    .testTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CHECKBOX_LABEL),
                text = stringResource(id = sharedR.string.external_checkout_show_next_time),
                textColor = TextColor.Primary,
            )
        }

        // Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.x8),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Continue button (primary)
            PrimaryFilledButton(
                modifier = Modifier
                    .padding(top = 20.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .testTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CONTINUE),
                text = stringResource(id = sharedR.string.button_continue),
                onClick = { onContinue() },
                trailingIcon = rememberVectorPainter(IconPack.Medium.Thin.Outline.ExternalLink)
            )

            // Cancel button (secondary)
            SecondaryFilledButton(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .testTag(TEST_TAG_INFORMATION_BOTTOM_SHEET_CANCEL),
                text = stringResource(id = sharedR.string.general_dialog_cancel_button),
                onClick = { onCancel() },
            )
        }
    }
}

