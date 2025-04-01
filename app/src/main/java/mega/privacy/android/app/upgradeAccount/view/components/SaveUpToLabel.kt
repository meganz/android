package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.chip.MegaChip
import mega.privacy.android.shared.original.core.ui.controls.chip.NotificationChipStyle
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemeComponentPreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.grey_020_grey_800

/**
 * Composable UI for Label Save up to 16% with yearly billing
 * to reuse on Onboarding dialog for Variant B and Upgrade account screen
 */
@Composable
internal fun SaveUpToLabel() {
    MegaChip(
        selected = true,
        text = stringResource(id = R.string.account_upgrade_account_label_save_up_to),
        style = NotificationChipStyle.Success,
        modifier = Modifier
            .padding(
                start = 16.dp,
                top = 3.dp,
                bottom = 16.dp
            )
            .background(
                color = MaterialTheme.colors.grey_020_grey_800,
                shape = RoundedCornerShape(100.dp)
            )
    )
}

@CombinedThemeComponentPreviews
@Composable
private fun SaveUpToLabelPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SaveUpToLabel()
    }
}