package mega.privacy.android.app.upgradeAccount

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.UIAccountType
import mega.privacy.android.app.upgradeAccount.model.UpgradeAccountState
import mega.privacy.android.app.upgradeAccount.model.mapper.toFormattedPriceString
import mega.privacy.android.app.upgradeAccount.model.mapper.toFormattedSizeGBBased
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Subscription
import mega.privacy.android.presentation.controls.SimpleTopAppBar
import mega.privacy.android.presentation.theme.grey_300
import mega.privacy.android.presentation.theme.grey_400
import mega.privacy.android.presentation.theme.grey_500
import mega.privacy.android.presentation.theme.grey_600

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun UpgradeAccountView(
    state: UpgradeAccountState,
    onBackPressed: () -> Unit,
    onPlanClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            SimpleTopAppBar(
                titleId = R.string.action_upgrade_account,
                elevation = false,
                onBackPressed = onBackPressed
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues)
        ) {
            CurrentSubscriptionPlanBox(state = state)
            for (i in state.subscriptionsList.indices) {
                SubscriptionPlansInfoRow(
                    proPlan = state.subscriptionsList[i].accountType,
                    subscription = state.subscriptionsList[i],
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
            Text(
                text = stringResource(id = R.string.upgrade_comment),
                fontSize = 12.sp,
                color = if (MaterialTheme.colors.isLight) grey_500 else grey_400,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
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
    subscription: Subscription,
    onPlanClicked: () -> Unit,
) {
    val isLight = MaterialTheme.colors.isLight

    val storageValueString =
        stringResource(
            id = toFormattedSizeGBBased(subscription.storage.toLong()).first,
            toFormattedSizeGBBased(subscription.storage.toLong()).second
        )
    val transferValueString =
        stringResource(
            id = toFormattedSizeGBBased(subscription.transfer.toLong()).first,
            toFormattedSizeGBBased(subscription.transfer.toLong()).second
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

    val formattedPrice = toFormattedPriceString(subscription.amount)
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
            .clickable { onPlanClicked() }
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
                text = stringResource(id = uiAccountType.textValue).uppercase(),
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
            priceSubSubString.substring(priceSubSubString.indexOf("[A]"),
                priceSubSubString.indexOf("[/A]"))
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