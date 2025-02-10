package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.accent_900_accent_050
import mega.privacy.android.shared.original.core.ui.theme.extensions.body2medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.shared.original.core.ui.theme.transparent
import mega.android.core.ui.theme.values.TextColor


/**
 * Composable UI for monthly/yearly tabs to reuse on Upgrade account screen
 */
@Composable
internal fun MonthlyYearlyTabs(
    isMonthly: Boolean,
    onTabClicked: (Boolean) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    ) {

        Button(
            onClick = { onTabClicked(true) },
            border = BorderStroke(
                width = 0.5.dp,
                color =
                if (isMonthly) transparent
                else MaterialTheme.colors.textColorSecondary
            ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =
                if (isMonthly) MaterialTheme.colors.accent_900_accent_050
                else transparent,
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp
            ),
            modifier = Modifier
                .padding(end = 8.dp)
                .testTag("$testTag$MONTHLY_TAB_TAG"),
            shape = RoundedCornerShape(8.dp),
            contentPadding = if (isMonthly)
                PaddingValues(
                    start = 11.dp,
                    end = 16.dp
                )
            else PaddingValues(
                horizontal = 16.dp
            )
        ) {
            if (isMonthly) {
                Icon(
                    tint = MaterialTheme.colors.onSecondary,
                    painter = painterResource(R.drawable.ic_plans_montly_yearly_check),
                    contentDescription = "Check icon for monthly/yearly tabs, when selected",
                    modifier = Modifier
                        .padding(end = 11.dp)
                        .testTag("$testTag$MONTHLY_CHECK_ICON_TAG"),
                )
            }
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_tab_monthly),
                textColor = if (isMonthly) TextColor.Inverse else TextColor.Primary,
                style = MaterialTheme.typography.body2medium,
            )
        }

        Button(
            onClick = { onTabClicked(false) },
            border = BorderStroke(
                width = 0.5.dp,
                color =
                if (isMonthly) MaterialTheme.colors.textColorSecondary
                else transparent,
            ),
            colors = ButtonDefaults.buttonColors(
                backgroundColor =
                if (isMonthly) transparent
                else MaterialTheme.colors.accent_900_accent_050
            ),
            elevation = ButtonDefaults.elevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp,
                disabledElevation = 0.dp
            ),
            modifier = Modifier
                .padding(end = 8.dp)
                .testTag("$testTag$YEARLY_TAB_TAG"),
            shape = RoundedCornerShape(8.dp),
            contentPadding =
            if (isMonthly)
                PaddingValues(
                    horizontal = 16.dp
                )
            else PaddingValues(
                start = 11.dp,
                end = 16.dp
            )
        ) {
            if (!isMonthly) {
                Icon(
                    tint = MaterialTheme.colors.onSecondary,
                    painter = painterResource(R.drawable.ic_plans_montly_yearly_check),
                    contentDescription = "Check icon for monthly/yearly tabs, when selected",
                    modifier = Modifier
                        .padding(end = 11.dp)
                        .testTag("$testTag$YEARLY_CHECK_ICON_TAG"),
                )
            }
            MegaText(
                text = stringResource(id = R.string.account_upgrade_account_tab_yearly),
                textColor = if (isMonthly) TextColor.Primary else TextColor.Inverse,
                style = MaterialTheme.typography.body2medium,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MonthlyYearlyTabsPreview(
    @PreviewParameter(BooleanProvider::class) initialValue: Boolean,
) {
    val isMonthly = remember { mutableStateOf(initialValue) }
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MonthlyYearlyTabs(
            isMonthly = isMonthly.value,
            onTabClicked = { isMonthly.value = it },
            testTag = "test",
        )
    }
}

internal const val MONTHLY_TAB_TAG = "tab_monthly"
internal const val YEARLY_TAB_TAG = "tab_yearly"
internal const val MONTHLY_CHECK_ICON_TAG = "image_monthly_check"
internal const val YEARLY_CHECK_ICON_TAG = "image_yearly_check"