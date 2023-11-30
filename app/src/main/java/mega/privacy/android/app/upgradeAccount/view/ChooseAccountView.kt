package mega.privacy.android.app.upgradeAccount.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
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
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.UIAccountType
import mega.privacy.android.app.upgradeAccount.model.extensions.toUIAccountType
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.body2
import mega.privacy.android.core.ui.theme.caption
import mega.privacy.android.core.ui.theme.extensions.black_white
import mega.privacy.android.core.ui.theme.extensions.body4
import mega.privacy.android.core.ui.theme.extensions.green_500_green_300
import mega.privacy.android.core.ui.theme.extensions.grey_500_grey_400
import mega.privacy.android.core.ui.theme.extensions.grey_600_grey_300
import mega.privacy.android.core.ui.theme.extensions.red_500_red_300
import mega.privacy.android.core.ui.theme.subtitle2
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.legacy.core.ui.controls.appbar.SimpleTopAppBar
import mega.privacy.android.legacy.core.ui.controls.text.MegaSpannedText
import java.util.Locale

/**
 * Onboarding screen where new user selects which type of Account he wants (Free or Pro)
 * screen appears when new user log ins for the first time to newly created account (didn't log in any other client yet)
 */

@Composable
fun ChooseAccountView(
    state: ChooseAccountState,
    onBackPressed: () -> Unit = {},
    onPlanClicked: (AccountType) -> Unit = {},
) {
    Scaffold(
        topBar = {
            SimpleTopAppBar(
                titleId = R.string.choose_account_fragment,
                elevation = false,
                onBackPressed = onBackPressed
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState(), enabled = true)
                .fillMaxWidth(),
        ) {
            if (state.localisedSubscriptionsList.isNotEmpty()) {

                FreePlanRow(onPlanClicked = onPlanClicked)

                Divider(
                    thickness = 1.dp,
                    modifier = Modifier
                        .padding(
                            start = 16.dp,
                            top = 10.dp,
                            end = 16.dp,
                            bottom = 11.dp
                        )
                )

                state.localisedSubscriptionsList.forEach {
                    ChooseSubscriptionPlansInfoRow(
                        proPlan = it.accountType,
                        subscription = it,
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
                    style = caption,
                    color = MaterialTheme.colors.grey_500_grey_400,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .align(Alignment.Start)
                )
            }
        }
    }
}

/**
 * Row to display Free plan
 */
@Composable
fun FreePlanRow(
    onPlanClicked: (AccountType) -> Unit,
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clickable { onPlanClicked(AccountType.FREE) }
            .alpha(alpha = ContentAlpha.high)
            .testTag(AccountType.FREE.toString())
    ) {
        val (image, title, storage, transfer, footnote) = createRefs()

        Image(
            painter = painterResource(id = UIAccountType.FREE.iconValue),
            contentDescription = null,
            modifier = Modifier.constrainAs(image) {
                top.linkTo(parent.top, margin = 16.dp)
                start.linkTo(parent.start, margin = 16.dp)
            }
        )
        Text(
            text = stringResource(id = UIAccountType.FREE.textValue),
            color = MaterialTheme.colors.green_500_green_300,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(image.bottom)
                start.linkTo(image.start)
            }
        )
        MegaSpannedText(
            value = stringResource(
                id = R.string.account_upgrade_storage_label,
                "20 GB+"
            ) + "[B]1[/B]",
            baseStyle = subtitle2,
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(
                    color = MaterialTheme.colors.black_white,
                ),
                SpanIndicator('B') to SpanStyle(
                    baselineShift = BaselineShift.Superscript,
                    color = MaterialTheme.colors.red_500_red_300,
                    fontSize = 12.sp
                )
            ),
            modifier = Modifier.constrainAs(storage) {
                top.linkTo(parent.top, margin = 10.dp)
                start.linkTo(image.end, margin = 60.dp)
            },
            color = MaterialTheme.colors.grey_600_grey_300,
        )
        MegaSpannedText(
            value = stringResource(id = R.string.account_choose_free_limited_transfer_quota),
            baseStyle = subtitle2,
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(
                    color = MaterialTheme.colors.black_white,
                )
            ),
            modifier = Modifier.constrainAs(transfer) {
                top.linkTo(storage.bottom, margin = 2.dp)
                start.linkTo(storage.start)
            },
            color = MaterialTheme.colors.grey_600_grey_300,
        )
        MegaSpannedText(
            value = "[A]1 [/A]" + stringResource(id = R.string.footnote_achievements),
            baseStyle = MaterialTheme.typography.body4,
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(
                    baselineShift = BaselineShift.Superscript,
                    color = MaterialTheme.colors.red_500_red_300,
                    fontSize = 10.sp
                )
            ),
            modifier = Modifier.constrainAs(footnote) {
                linkTo(transfer.start, parent.end, bias = 0f)
                top.linkTo(transfer.bottom, margin = 5.dp)
                width = Dimension.preferredWrapContent
            },
            color = MaterialTheme.colors.grey_600_grey_300,
        )
    }
}

/**
 * Row to display Pro plan, can be reused for each Pro plan
 */
@Composable
fun ChooseSubscriptionPlansInfoRow(
    proPlan: AccountType,
    subscription: LocalisedSubscription,
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

    val uiAccountType = proPlan.toUIAccountType()

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

    ConstraintLayout(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clickable { onPlanClicked(proPlan) }
            .alpha(alpha = ContentAlpha.high)
            .testTag(proPlan.toString())
    ) {
        val (image, title, priceLabel, storageText, transferText) = createRefs()

        Image(
            painter = painterResource(id = uiAccountType.iconValue),
            contentDescription = null,
            modifier = Modifier.constrainAs(image) {
                top.linkTo(anchor = parent.top, margin = 16.dp)
                start.linkTo(anchor = parent.start, margin = 16.dp)
            }
        )
        Text(
            text = stringResource(id = uiAccountType.textValue),
            color = if (isLight) uiAccountType.colorValue else uiAccountType.colorValueDark,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(image.bottom)
                start.linkTo(anchor = image.start)
                end.linkTo(anchor = image.end)
            }
        )
        Text(
            text = createPriceText(
                isLight = isLight,
                uiAccountType = uiAccountType,
                priceString = priceString,
                priceSubString = priceSubString,
                priceSubSubString = priceSubSubString
            ),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = if (isLight) uiAccountType.colorValue else uiAccountType.colorValueDark,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .constrainAs(priceLabel) {
                    top.linkTo(parent.top, margin = 10.dp)
                    start.linkTo(image.end, 50.dp)
                }
        )
        MegaSpannedText(
            value = storageString,
            baseStyle = body2,
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(
                    color = MaterialTheme.colors.black_white,
                ),
            ),
            modifier = Modifier.constrainAs(storageText) {
                top.linkTo(priceLabel.bottom, 8.dp)
                start.linkTo(priceLabel.start, margin = 8.dp)
            },
            color = MaterialTheme.colors.grey_600_grey_300,
        )
        MegaSpannedText(
            value = transferString,
            baseStyle = body2,
            styles = hashMapOf(
                SpanIndicator('A') to SpanStyle(
                    color = MaterialTheme.colors.black_white,
                ),
            ),
            modifier = Modifier.constrainAs(transferText) {
                top.linkTo(storageText.bottom, 2.dp)
                start.linkTo(storageText.start)
            },
            color = MaterialTheme.colors.grey_600_grey_300,
        )
    }
}

/**
 * function to display string for Pro plans prices correctly
 * MegaSpannedText doesn't work, because the same placeholder is used
 * as new design will be applied soon there is no point to update strings to fix this problem
 */
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

@CombinedThemePreviews
@Composable
private fun PreviewChooseAccountView(
    @PreviewParameter(ChooseAccountPreviewProvider::class) state: ChooseAccountState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        ChooseAccountView(state = state)
    }
}

private class ChooseAccountPreviewProvider :
    PreviewParameterProvider<ChooseAccountState> {
    val localisedSubscriptionsList: List<LocalisedSubscription> = listOf(
        subscriptionProLite,
        subscriptionProI,
        subscriptionProII,
        subscriptionProIII
    )
    override val values: Sequence<ChooseAccountState>
        get() = sequenceOf(
            ChooseAccountState(
                localisedSubscriptionsList = localisedSubscriptionsList
            )
        )

    companion object {
        val localisedPriceStringMapper = LocalisedPriceStringMapper()
        val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
        val formattedSizeMapper = FormattedSizeMapper()

        val subscriptionProI = LocalisedSubscription(
            accountType = AccountType.PRO_I,
            storage = 2048,
            monthlyTransfer = 2048,
            yearlyTransfer = 24576,
            monthlyAmount = CurrencyAmount(9.99F, Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                99.99F,
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProII = LocalisedSubscription(
            accountType = AccountType.PRO_II,
            storage = 8192,
            monthlyTransfer = 8192,
            yearlyTransfer = 98304,
            monthlyAmount = CurrencyAmount(19.99F, Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                199.99F,
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProIII = LocalisedSubscription(
            accountType = AccountType.PRO_III,
            storage = 16384,
            monthlyTransfer = 16384,
            yearlyTransfer = 196608,
            monthlyAmount = CurrencyAmount(29.99F, Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                299.99F,
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )

        val subscriptionProLite = LocalisedSubscription(
            accountType = AccountType.PRO_LITE,
            storage = 400,
            monthlyTransfer = 1024,
            yearlyTransfer = 12288,
            monthlyAmount = CurrencyAmount(4.99F, Currency("EUR")),
            yearlyAmount = CurrencyAmount(
                49.99F,
                Currency("EUR")
            ),
            localisedPrice = localisedPriceStringMapper,
            localisedPriceCurrencyCode = localisedPriceCurrencyCodeStringMapper,
            formattedSize = formattedSizeMapper,
        )
    }
}