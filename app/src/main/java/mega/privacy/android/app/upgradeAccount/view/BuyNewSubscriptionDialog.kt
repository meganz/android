package mega.privacy.android.app.upgradeAccount.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.PaymentPlatformType

@Composable
fun BuyNewSubscriptionDialog(
    upgradeTypeInt: Int,
    paymentMethod: PaymentMethod,
    onDialogPositiveButtonClicked: (Int) -> Unit,
    onDialogDismissButtonClicked: () -> Unit,
) {
    ConfirmationDialog(
        title = stringResource(id = R.string.title_existing_subscription),
        text = chooseCorrectBodyString(paymentMethod),
        confirmButtonText = stringResource(id = R.string.button_buy_new_subscription),
        cancelButtonText = stringResource(id = R.string.general_dismiss),
        onConfirm = { onDialogPositiveButtonClicked(upgradeTypeInt) },
        onDismiss = { onDialogDismissButtonClicked() })
}

@Composable
private fun chooseCorrectBodyString(paymentMethod: PaymentMethod) =
    when (paymentMethod.platformType) {
        PaymentPlatformType.SUBSCRIPTION_FROM_ANDROID_PLATFORM -> {
            stringResource(
                R.string.message_subscription_from_android_platform,
                paymentMethod.methodName
            )
        }

        PaymentPlatformType.SUBSCRIPTION_FROM_ITUNES -> {
            stringResource(R.string.message_subscription_from_itunes)
        }

        else -> {
            stringResource(R.string.message_subscription_from_other_platform)
        }
    }
