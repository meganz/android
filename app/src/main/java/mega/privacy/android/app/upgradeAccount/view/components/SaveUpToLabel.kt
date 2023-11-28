package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_800
import mega.privacy.android.core.ui.theme.subtitle2

/**
 * Composable UI for Label Save up to 16% with yearly billing
 * to reuse on Onboarding dialog for Variant B and Upgrade account screen
 */
@Composable
internal fun SaveUpToLabel() {
    Box(
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
    ) {
        Text(
            text = stringResource(id = R.string.account_upgrade_account_label_save_up_to),
            style = subtitle2,
            color = MaterialTheme.colors.black_white,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SaveUpToLabelPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        SaveUpToLabel()
    }
}