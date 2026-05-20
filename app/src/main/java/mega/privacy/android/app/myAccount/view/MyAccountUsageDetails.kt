package mega.privacy.android.app.myAccount.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.divider.StrongDivider
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.app.myAccount.MyAccountUsageUiState
import mega.privacy.android.app.myAccount.PaymentAlertType
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.formatter.stripLinkAnnotations

@Composable
internal fun PaymentAlertSection(
    uiState: MyAccountUsageUiState,
    modifier: Modifier = Modifier,
) {
    // Get text and color based on payment alert type
    val (text, textColor) = when (uiState.paymentAlertType) {
        PaymentAlertType.BusinessExpired -> {
            stringResource(R.string.payment_overdue_label) to TextColor.Error
        }

        PaymentAlertType.BusinessGracePeriod -> {
            stringResource(R.string.payment_required_label) to TextColor.Warning
        }

        PaymentAlertType.AccountRenewsOn -> {
            val date = TimeUtils.formatDate(
                uiState.paymentAlertDate,
                TimeUtils.DATE_MM_DD_YYYY_FORMAT,
                LocalContext.current
            )
            stringResource(R.string.account_info_renews_on, date)
                .stripLinkAnnotations() to TextColor.Secondary
        }

        PaymentAlertType.AccountExpiresOn -> {
            val date = TimeUtils.formatDate(
                uiState.paymentAlertDate,
                TimeUtils.DATE_MM_DD_YYYY_FORMAT,
                LocalContext.current
            )
            stringResource(R.string.account_info_expires_on, date)
                .stripLinkAnnotations() to TextColor.Secondary
        }

        PaymentAlertType.None -> return // Don't show anything
    }

    MegaText(
        text = text,
        style = AppTheme.typography.bodyMedium,
        textColor = textColor,
        modifier = modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
}

@Composable
internal fun StorageDetailItem(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(vertical = 4.dp)
    ) {
        // Left Title
        MegaText(
            text = title,
            style = AppTheme.typography.bodyLarge,
            textColor = TextColor.Primary,
            modifier = Modifier.align(Alignment.CenterStart)
        )

        // Right Data Value
        MegaText(
            text = value,
            style = AppTheme.typography.bodyLarge,
            textColor = TextColor.Primary,
            modifier = Modifier.align(Alignment.CenterEnd)
        )

        // Bottom Divider
        StrongDivider(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
        )
    }
}
