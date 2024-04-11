package mega.privacy.android.app.upgradeAccount.view.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.fade
import com.google.accompanist.placeholder.placeholder
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.model.LocalisedSubscription
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceCurrencyCodeStringMapper
import mega.privacy.android.app.upgradeAccount.model.mapper.LocalisedPriceStringMapper
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TEXT
import mega.privacy.android.app.upgradeAccount.view.PRO_PLAN_TITLE
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.core.ui.theme.extensions.h6Medium
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import mega.privacy.android.shared.theme.MegaAppTheme
import java.util.Locale

/**
 * Composable UI to show the title and description for new Onboarding screen
 *
 * @param state   ChooseAccountState
 * @param isLoading   Boolean to show loading state
 * @param bodyTextStyle   TextStyle for the body text
 */
@Composable
fun GetProPlanColumn(
    state: ChooseAccountState,
    isLoading: Boolean,
    bodyTextStyle: TextStyle,
) {
    val cheapestSubscriptionAvailable = state.cheapestSubscriptionAvailable
    val formattedPrice =
        cheapestSubscriptionAvailable?.localisePriceCurrencyCode(Locale.getDefault(), true)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MegaText(
            text = stringResource(id = R.string.dialog_onboarding_get_pro_title),
            textColor = TextColor.Primary,
            style = MaterialTheme.typography.h6Medium,
            modifier = Modifier.testTag(PRO_PLAN_TITLE),
        )
        MegaText(
            text = if (formattedPrice != null) stringResource(
                id = R.string.dialog_onboarding_get_pro_description,
                formattedPrice.price
            ) else "",
            textColor = TextColor.Secondary,
            style = bodyTextStyle.copy(textAlign = TextAlign.Center),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
                .testTag(PRO_PLAN_TEXT)
                .placeholder(
                    color = MaterialTheme.colors.grey_020_grey_900,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                    visible = isLoading,
                ),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun GetProPlanColumnPreview() {
    val localisedPriceStringMapper = LocalisedPriceStringMapper()
    val localisedPriceCurrencyCodeStringMapper = LocalisedPriceCurrencyCodeStringMapper()
    val formattedSizeMapper = FormattedSizeMapper()

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
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        GetProPlanColumn(
            state = ChooseAccountState(
                cheapestSubscriptionAvailable = subscriptionProLite
            ),
            isLoading = false,
            bodyTextStyle = MaterialTheme.typography.subtitle2,
        )
    }
}