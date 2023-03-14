package mega.privacy.android.app.upgradeAccount.payment.component

import android.app.Activity
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.billing.BillingViewModel
import mega.privacy.android.app.upgradeAccount.payment.PaymentViewModel
import mega.privacy.android.app.upgradeAccount.payment.UserSubscription
import mega.privacy.android.core.ui.controls.MegaSpannedText
import mega.privacy.android.core.ui.controls.SimpleTopAppBar
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.theme.AndroidTheme

@Composable
internal fun PaymentScreen(
    paymentViewModel: PaymentViewModel,
    billingViewModel: BillingViewModel,
) {
    val uiState by paymentViewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity
    PaymentView(
        isMonthly = uiState.isMonthlySelected,
        monthlyPrice = uiState.monthlyPrice,
        yearlyPrice = uiState.yearlyPrice,
        isPaymentMethodAvailable = uiState.isPaymentMethodAvailable,
        onSelectChange = paymentViewModel::onSelectChange,
        titleId = uiState.title,
        titleColorId = uiState.titleColor,
        userSubscription = uiState.userSubscription,
        onPaymentClicked = {
            billingViewModel.startPurchase(
                activity,
                paymentViewModel.getProductId(uiState.isMonthlySelected, uiState.upgradeType)
            )
        }
    )
}

@Composable
internal fun PaymentView(
    isMonthly: Boolean = false,
    @StringRes titleId: Int = 0,
    @ColorRes titleColorId: Int = 0,
    monthlyPrice: String = "",
    yearlyPrice: String = "",
    isPaymentMethodAvailable: Boolean = true,
    onSelectChange: (isMonthly: Boolean) -> Unit = {},
    onPaymentClicked: () -> Unit = {},
    userSubscription: UserSubscription = UserSubscription.NOT_SUBSCRIBED,
) {
    val isBillingEnable =
        isPaymentMethodAvailable && userSubscription == UserSubscription.NOT_SUBSCRIBED
    val onBackPressedDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    Scaffold(topBar = {
        SimpleTopAppBar(
            titleId = R.string.payment,
            elevation = false,
        ) { onBackPressedDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() }
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(id = R.string.payment_methods),
                style = MaterialTheme.typography.subtitle2,
                color = colorResource(id = R.color.grey_087_white_087)
            )
            if (titleId != 0 && titleColorId != 0) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(id = titleId),
                    style = MaterialTheme.typography.subtitle1.copy(fontSize = 20.sp),
                    color = colorResource(id = titleColorId)
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_google_wallet),
                    contentDescription = "Icon"
                )
                MegaSpannedText(
                    modifier = Modifier.padding(start = 8.dp),
                    value = stringResource(id = R.string.payment_method_google_wallet),
                    baseStyle = MaterialTheme.typography.subtitle1.copy(color = colorResource(id = R.color.grey_054_white_054)),
                    styles = mapOf(
                        SpanIndicator('A') to SpanStyle(color = colorResource(id = R.color.grey_087_white_087))
                    )
                )
            }

            BillingOptionsView(
                onSelectChange = onSelectChange,
                isBillingEnable = isBillingEnable,
                isMonthly = isMonthly,
                userSubscription = userSubscription,
                monthlyPrice = monthlyPrice,
                yearlyPrice = yearlyPrice
            )

            if (isBillingEnable) {
                Spacer(modifier = Modifier.weight(1.0f))
                Button(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 62.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary),
                    onClick = onPaymentClicked
                ) {
                    Text(text = stringResource(id = R.string.proceed))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PaymentViewPreview() {
    AndroidTheme(false) {
        PaymentView(
            titleId = R.string.prolite_account,
            titleColorId = R.color.orange_400_orange_300,
            monthlyPrice = stringResource(id = R.string.billed_monthly_text, "10$"),
            yearlyPrice = stringResource(id = R.string.billed_yearly_text, "10$"),
            userSubscription = UserSubscription.NOT_SUBSCRIBED,
        )
    }
}


@Preview(showBackground = true)
@Composable
fun PaymentViewDisablePreview() {
    AndroidTheme(true) {
        PaymentView(
            isPaymentMethodAvailable = false,
            titleId = R.string.prolite_account,
            titleColorId = R.color.orange_400_orange_300,
            monthlyPrice = stringResource(id = R.string.billed_monthly_text, "10$"),
            yearlyPrice = stringResource(id = R.string.billed_yearly_text, "10$"),
            userSubscription = UserSubscription.NOT_SUBSCRIBED,
        )
    }
}