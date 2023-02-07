package mega.privacy.android.app.upgradeAccount.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.theme.Typography
import mega.privacy.android.core.ui.theme.grey_400
import mega.privacy.android.core.ui.theme.grey_500
import mega.privacy.android.core.ui.theme.h6
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.domain.entity.PaymentMethod
import mega.privacy.android.domain.entity.PaymentPlatformType

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun BuyNewSubscriptionDialog(
    upgradeTypeInt: Int,
    paymentMethod: PaymentMethod,
    onDialogPositiveButtonClicked: (Int) -> Unit,
    onDialogDismissButtonClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    Dialog(
        onDismissRequest = { onDialogDismissButtonClicked() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 40.dp)
                .widthIn(max = 280.dp),
            elevation = 24.dp,
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier
                    .padding(all = 0.dp)
                    .verticalScroll(
                        state = rememberScrollState(),
                        enabled = true
                    )
            ) {
                // Dialog title
                Text(
                    text = stringResource(id = R.string.title_existing_subscription),
                    style = h6,
                    modifier = Modifier
                        .padding(
                            start = 24.dp,
                            top = 20.dp,
                            end = 24.dp,
                            bottom = 16.dp
                        )
                        .align(Alignment.Start)
                )

                // Dialog body
                Text(
                    text = chooseCorrectBodyString(paymentMethod),
                    style = Typography.body1,
                    color = if (isLight) grey_500 else grey_400,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )

                // Dialog confirm button
                Button(
                    onClick = {
                        onDialogPositiveButtonClicked(upgradeTypeInt)
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                    ),
                    modifier = Modifier.padding(
                        top = 42.dp
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    ),
                ) {
                    Text(
                        text = stringResource(id = R.string.button_buy_new_subscription),
                        color = if (isLight) teal_300 else teal_200
                    )
                }

                // Dialog dismiss button
                Button(
                    onClick = {
                        onDialogDismissButtonClicked()
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                    ),
                    modifier = Modifier.padding(
                        top = 32.dp
                    ),
                    elevation = ButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp,
                        hoveredElevation = 0.dp,
                        focusedElevation = 0.dp
                    ),
                ) {
                    Text(
                        stringResource(id = R.string.general_dismiss),
                        color = if (isLight) teal_300 else teal_200
                    )
                }
            }
        }
    }
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
