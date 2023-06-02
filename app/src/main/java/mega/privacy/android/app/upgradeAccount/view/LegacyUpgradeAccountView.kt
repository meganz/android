package mega.privacy.android.app.upgradeAccount.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UIAccountType
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.core.ui.controls.SimpleTopAppBar
import mega.privacy.android.core.ui.theme.Typography
import mega.privacy.android.core.ui.theme.black
import mega.privacy.android.core.ui.theme.grey_300
import mega.privacy.android.core.ui.theme.grey_400
import mega.privacy.android.core.ui.theme.grey_500
import mega.privacy.android.core.ui.theme.grey_600
import mega.privacy.android.core.ui.theme.teal_200
import mega.privacy.android.core.ui.theme.teal_300
import mega.privacy.android.core.ui.theme.yellow_100
import mega.privacy.android.core.ui.theme.yellow_700
import mega.privacy.android.core.ui.theme.yellow_700_alpha_015
import java.util.Locale

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun LegacyUpgradeAccountView(
    state: UpgradeAccountState,
    onBackPressed: () -> Unit,
    onPlanClicked: (AccountType) -> Unit,
    onCustomLabelClicked: () -> Unit,
    hideBillingWarning: () -> Unit,
    onDialogPositiveButtonClicked: (Int) -> Unit,
    onDialogDismissButtonClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    Scaffold(
        topBar = {
            SimpleTopAppBar(
                titleId = R.string.action_upgrade_account,
                elevation = state.showBillingWarning,
                onBackPressed = onBackPressed
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState(), enabled = true)
        ) {
            if (state.showBillingWarning) {
                BillingWarning(hideBillingWarning)
            }
            CurrentSubscriptionPlanBox(state = state)
            state.localisedSubscriptionsList.forEach {
                SubscriptionPlansInfoRow(
                    proPlan = it.accountType,
                    subscription = it,
                    isCurrentPlan = state.currentSubscriptionPlan == it.accountType,
                    onPlanClicked = onPlanClicked
                )
                Divider(
                    thickness = 1.dp,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            top = 8.dp,
                            end = 16.dp,
                            bottom = 11.dp
                        )
                )
            }
            if (state.localisedSubscriptionsList.isNotEmpty()) {
                if (state.currentSubscriptionPlan == AccountType.PRO_III) {
                    CustomLabelText(
                        onCustomLabelClicked = onCustomLabelClicked
                    )
                }
                Text(
                    text = stringResource(id = R.string.upgrade_comment),
                    fontSize = 12.sp,
                    color = if (isLight) grey_500 else grey_400,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            }
        }
    }
    if (state.showBuyNewSubscriptionDialog) {
        BuyNewSubscriptionDialog(
            upgradeTypeInt = state.currentPayment.upgradeType,
            paymentMethod = state.currentPayment.currentPayment ?: return,
            onDialogPositiveButtonClicked = onDialogPositiveButtonClicked,
            onDialogDismissButtonClicked = { onDialogDismissButtonClicked() }
        )
    }
}

@Composable
fun BillingWarning(hideBillingWarning: () -> Unit) {
    val isLight = MaterialTheme.colors.isLight
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = 13.dp
            )
            .background(
                color = if (isLight) yellow_100 else yellow_700_alpha_015
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(.9f)
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    )
            ) {
                Text(
                    text = stringResource(id = R.string.upgrade_billing_warning),
                    fontSize = 13.sp,
                    style = Typography.caption,
                    color = if (isLight) black else yellow_700,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(.1f)
                    .padding(
                        top = 0.dp,
                        end = 17.dp
                    )
            ) {
                IconButton(onClick = hideBillingWarning) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_remove_billing_warning),
                        contentDescription = null,
                    )
                }
            }
        }
    }
}

@Composable
fun CurrentSubscriptionPlanBox(state: UpgradeAccountState) {
    val isLight = MaterialTheme.colors.isLight

    val uiAccountType = when (state.currentSubscriptionPlan) {
        AccountType.PRO_I -> UIAccountType.PRO_I
        AccountType.PRO_II -> UIAccountType.PRO_II
        AccountType.PRO_III -> UIAccountType.PRO_III
        AccountType.PRO_LITE -> UIAccountType.PRO_LITE
        else -> UIAccountType.DEFAULT
    }
    val text: String =
        stringResource(
            id = R.string.type_of_my_account,
            stringResource(id = uiAccountType.textValue)
        )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 5.dp,
                bottom = 30.dp
            )
    ) {
        Text(
            text = createCurrentAccountText(
                isLight = isLight,
                uiAccountType = uiAccountType,
                text = text
            ),
            fontSize = 14.sp,
            color = if (isLight) grey_500 else grey_400,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun SubscriptionPlansInfoRow(
    proPlan: AccountType,
    subscription: LocalisedSubscription,
    isCurrentPlan: Boolean,
    onPlanClicked: (AccountType) -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    val storageValueString =
        stringResource(
            id = subscription.formatStorageSize().unit,
            subscription.formatStorageSize().size
        )
    val transferValueString =
        stringResource(
            id = subscription.formatTransferSize(true).unit,
            subscription.formatTransferSize(true).size
        )

    val uiAccountType = when (proPlan) {
        AccountType.PRO_I -> UIAccountType.PRO_I
        AccountType.PRO_II -> UIAccountType.PRO_II
        AccountType.PRO_III -> UIAccountType.PRO_III
        AccountType.PRO_LITE -> UIAccountType.PRO_LITE
        else -> UIAccountType.PRO_LITE
    }

    val storageString = stringResource(
        id = R.string.account_upgrade_storage_label,
        storageValueString
    )
    val transferString = stringResource(
        id = R.string.account_upgrade_transfer_quota_label,
        transferValueString
    )

    val formattedPrice = subscription.localisePrice(Locale.getDefault())
    val priceString = stringResource(
        id = R.string.type_month,
        formattedPrice
    )
    val priceSubString = priceString.replaceFirst("[A]", "")
    val priceSubSubString = priceSubString.replaceFirst("[/A]", "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clickable { onPlanClicked(proPlan) }
            .alpha(alpha = if (isCurrentPlan) ContentAlpha.disabled else ContentAlpha.high)
            .testTag(proPlan.toString())
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(.3f)
                .padding(top = 10.dp)
        ) {
            Image(
                painter = painterResource(id = uiAccountType.iconValue),
                contentDescription = null
            )
            Text(
                text = stringResource(id = uiAccountType.textValue),
                color = if (isLight) uiAccountType.colorValue else uiAccountType.colorValueDark
            )
        }
        Column(
            modifier = Modifier
                .weight(.7f)
                .padding(start = 10.dp)
        ) {
            Text(
                text = createPriceText(
                    isLight = isLight,
                    uiAccountType = uiAccountType,
                    priceString = priceString,
                    priceSubString = priceSubString,
                    priceSubSubString = priceSubSubString,
                ),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isLight) uiAccountType.colorValue else uiAccountType.colorValueDark,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            )
            Text(
                text = createStorageTransferText(
                    isLight,
                    storageString,
                ),
                modifier = Modifier.padding(
                    start = 9.dp,
                    top = 8.dp
                )
            )
            Text(
                text = createStorageTransferText(
                    isLight,
                    transferString,
                ),
                modifier = Modifier.padding(
                    start = 9.dp,
                    top = 2.dp
                )
            )
        }
    }
}

@Composable
private fun CustomLabelText(
    onCustomLabelClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight
    val customLabelString = stringResource(id = R.string.label_custom_plan)
    Text(
        text = createCustomLabelText(
            isLight,
            customLabelString
        ),
        fontSize = 12.sp,
        color = if (isLight) grey_500 else grey_400,
        modifier = Modifier
            .clickable { onCustomLabelClicked() }
            .padding(
                horizontal = 16.dp
            )
    )
    Divider(
        thickness = 1.dp,
        modifier = Modifier
            .padding(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 11.dp
            )
    )
}

@Composable
private fun createCurrentAccountText(
    isLight: Boolean,
    uiAccountType: UIAccountType,
    text: String,
) = buildAnnotatedString {
    append(
        text.substring(0, text.indexOf("[A]"))
    )
    withStyle(SpanStyle(if (isLight) uiAccountType.colorValue else uiAccountType.colorValueDark)) {
        append(
            text.substring(
                text.indexOf("[A]"),
                text.indexOf("[/A]")
            )
                .replace("[A]", "")
                .replace("[/A]", "")
        )
    }
}

@Composable
private fun createPriceText(
    isLight: Boolean,
    uiAccountType: UIAccountType,
    priceString: String,
    priceSubString: String,
    priceSubSubString: String,
) = buildAnnotatedString {
    withStyle(SpanStyle(if (isLight) uiAccountType.colorValue else uiAccountType.colorValueDark)) {
        append(
            priceString.substring(0, priceString.indexOf("[/A]")).replace("[A]", "")
                .replace("[/A]", "")
        )
    }
    append(
        priceSubString.substring(
            priceSubString.indexOf("[/A]"),
            priceSubString.indexOf("[A]")
        ).replace("[/A]", "")
    )
    withStyle(SpanStyle(if (isLight) uiAccountType.colorValue else uiAccountType.colorValueDark)) {
        append(
            priceSubSubString.substring(
                priceSubSubString.indexOf("[A]"),
                priceSubSubString.indexOf("[/A]")
            )
                .replace("[A]", "")
        )
    }
    append(
        priceSubSubString.substring(
            priceSubSubString.indexOf("[/A]"),
            priceSubSubString.length
        ).replace("[/A]", "")
    )
}

@Composable
private fun createStorageTransferText(
    isLight: Boolean,
    storageTransferString: String,
) = buildAnnotatedString {
    append(
        storageTransferString.substring(0, storageTransferString.indexOf("[A]"))
    )
    append(
        storageTransferString.substring(
            storageTransferString.indexOf("[A]"),
            storageTransferString.indexOf("[/A]")
        ).replace("[A]", "")
    )
    withStyle(SpanStyle(if (isLight) grey_600 else grey_300)) {
        append(
            storageTransferString.substring(
                storageTransferString.indexOf("[/A]"),
                storageTransferString.length
            )
                .replace("[/A]", "")
        )
    }
}

@Composable
private fun createCustomLabelText(
    isLight: Boolean,
    customLabelString: String,
) = buildAnnotatedString {
    append(
        customLabelString.substring(0, customLabelString.indexOf("[A]"))
    )
    withStyle(SpanStyle(if (isLight) teal_300 else teal_200)) {
        append(
            customLabelString.substring(
                customLabelString.indexOf("[A]"),
                customLabelString.indexOf("[/A]")
            ).replace("[A]", "")
        )
    }
    append(
        customLabelString.substring(
            customLabelString.indexOf("[/A]"),
            customLabelString.length
        ).replace("[/A]", "")
    )
}
