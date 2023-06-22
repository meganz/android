package mega.privacy.android.app.upgradeAccount.payment.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.UserSubscription
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
internal fun BillingOptionsView(
    onSelectChange: (isMonthly: Boolean) -> Unit,
    isBillingEnable: Boolean,
    isMonthly: Boolean,
    userSubscription: UserSubscription,
    monthlyPrice: String,
    yearlyPrice: String,
) {
    Divider(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
        thickness = 1.dp,
        color = colorResource(id = R.color.grey_012_white_012)
    )

    Text(
        modifier = Modifier
            .padding(top = 16.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        text = stringResource(id = R.string.billing_period_title),
        style = MaterialTheme.typography.subtitle2,
        color = colorResource(id = R.color.grey_087_white_087)
    )

    if (monthlyPrice.isNotEmpty()) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clickable {
                    onSelectChange(true)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBillingEnable) {
                RadioButton(
                    modifier = Modifier.testTag("monthly_radio"),
                    selected = isMonthly,
                    onClick = { onSelectChange(true) },
                )
            }
            MegaSpannedText(
                modifier = Modifier
                    .padding(
                        start = if (isBillingEnable) 0.dp else 16.dp,
                        end = 16.dp
                    )
                    .alpha(if (userSubscription == UserSubscription.YEARLY_SUBSCRIBED) 0.36f else 1.0f),
                value = monthlyPrice,
                baseStyle = MaterialTheme.typography.subtitle1.copy(
                    color = colorResource(id = R.color.grey_054_white_054),
                    fontSize = 18.sp,
                ),
                styles = mapOf(
                    SpanIndicator('A') to SpanStyle(color = colorResource(id = R.color.grey_087_white_087))
                )
            )
        }
    }

    Divider(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        thickness = 1.dp,
        color = colorResource(id = R.color.grey_012_white_012)
    )

    if (yearlyPrice.isNotEmpty()) {
        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .clickable {
                    onSelectChange(false)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBillingEnable) {
                RadioButton(
                    modifier = Modifier.testTag("yearly_radio"),
                    selected = isMonthly.not(),
                    onClick = { onSelectChange(false) })
            }
            MegaSpannedText(
                modifier = Modifier
                    .padding(
                        start = if (isBillingEnable) 0.dp else 16.dp,
                        end = 16.dp
                    )
                    .alpha(if (userSubscription == UserSubscription.MONTHLY_SUBSCRIBED) 0.36f else 1.0f),
                value = yearlyPrice,
                baseStyle = MaterialTheme.typography.subtitle1.copy(
                    color = colorResource(id = R.color.grey_054_white_054),
                    fontSize = 18.sp,
                ),
                styles = mapOf(
                    SpanIndicator('A') to SpanStyle(color = colorResource(id = R.color.grey_087_white_087))
                )
            )
        }
    }

    Divider(
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
        thickness = 1.dp,
        color = colorResource(id = R.color.grey_012_white_012)
    )
}

@Preview(showBackground = true)
@Composable
fun BillingOptionsViewPreview() {
    AndroidTheme(true) {
        Column {
            BillingOptionsView(
                isBillingEnable = true,
                isMonthly = false,
                onSelectChange = {},
                monthlyPrice = stringResource(id = R.string.billed_monthly_text, "10$"),
                yearlyPrice = stringResource(id = R.string.billed_yearly_text, "10$"),
                userSubscription = UserSubscription.NOT_SUBSCRIBED,
            )
        }
    }
}