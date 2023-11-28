package mega.privacy.android.app.upgradeAccount.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
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
import mega.privacy.android.app.upgradeAccount.view.components.FeatureRow
import mega.privacy.android.core.theme.tokens.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.grey_020_grey_900
import mega.privacy.android.core.ui.theme.extensions.h6Medium
import mega.privacy.android.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.Currency
import mega.privacy.android.domain.entity.account.CurrencyAmount
import java.util.Locale

internal const val IMAGE_TAG = "onboarding_screen_variant_a:image"
internal const val PRO_PLAN_TITLE = "onboarding_screen_variant_a:pro_plan_title"
internal const val PRO_PLAN_TEXT = "onboarding_screen_variant_a:pro_plan_description"
internal const val FEATURE_TITLE = "onboarding_screen_variant_a:feature_title"
internal const val STORAGE_DESCRIPTION_ROW = "onboarding_screen_variant_a:storage_description_row"
internal const val TRANSFER_DESCRIPTION_ROW = "onboarding_screen_variant_a:transfer_description_row"
internal const val SECURITY_DESCRIPTION_ROW = "onboarding_screen_variant_a:security_description_row"
internal const val SKIP_BUTTON = "onboarding_screen_variant_a:skip_button"
internal const val VIEW_PRO_PLAN_BUTTON = "onboarding_screen_variant_a:view_pro_plan_button"

/**
 *  Compose UI for new Onboarding dialog (Choose account screen), this is Variant A
 *  User will see this account when the registration was finished and user signs in for the first time ever
 */
@Composable
fun VariantAOnboardingDialogView(
    state: ChooseAccountState,
    onSkipPressed: () -> Unit,
    onViewPlansPressed: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val cheapestSubscriptionAvailable = state.cheapestSubscriptionAvailable
    val isLoading = cheapestSubscriptionAvailable == null
    val formattedStorage = cheapestSubscriptionAvailable?.formatStorageSize()
    val minimalStorageString = formattedStorage?.let { stringResource(id = it.unit, it.size) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .width(390.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ){
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.image_upselling_onboarding_dialog),
                contentDescription = "",
                modifier = Modifier.testTag(IMAGE_TAG)
            )
            Spacer(modifier = Modifier.height(16.dp))
            GetProPlanColumn(state = state, isLoading = isLoading)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(id = R.string.dialog_onboarding_some_features_title),
                style = MaterialTheme.typography.subtitle1medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.textColorPrimary,
                modifier = Modifier
                    .testTag(FEATURE_TITLE)
                    .placeholder(
                        color = MaterialTheme.colors.grey_020_grey_900,
                        shape = RoundedCornerShape(4.dp),
                        highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                        visible = isLoading,
                    ),
            )
            Spacer(modifier = Modifier.height(16.dp))
            //Storage
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_storage_onboarding_dialog),
                title = stringResource(id = R.string.dialog_onboarding_feature_title_storage),
                description = if (minimalStorageString != null) stringResource(
                    id = R.string.dialog_onboarding_feature_description_storage,
                    minimalStorageString
                )
                else stringResource(id = R.string.dialog_onboarding_feature_description_storage),
                testTag = STORAGE_DESCRIPTION_ROW,
                isLoading = isLoading,
            )
            //Transfer
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_transfer_onboarding_dialog),
                title = stringResource(id = R.string.dialog_onboarding_feature_title_transfer),
                description = stringResource(id = R.string.dialog_onboarding_feature_description_transfer),
                testTag = TRANSFER_DESCRIPTION_ROW,
                isLoading = isLoading,
            )
            //Security
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_security_onboarding_dialog),
                title = stringResource(id = R.string.dialog_onboarding_feature_title_security),
                description = stringResource(id = R.string.dialog_onboarding_feature_description_security),
                testTag = SECURITY_DESCRIPTION_ROW,
                isLoading = isLoading,
            )
            Spacer(modifier = Modifier.height(38.dp))
            ButtonsRow(
                onSkipPressed = onSkipPressed,
                onViewPlansPressed = onViewPlansPressed,
                isLoading = isLoading,
            )
        }
    }
}

@Composable
fun GetProPlanColumn(
    state: ChooseAccountState,
    isLoading: Boolean,
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
        Text(
            text = stringResource(id = R.string.dialog_onboarding_get_pro_title),
            style = MaterialTheme.typography.h6Medium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorPrimary,
            modifier = Modifier.testTag(PRO_PLAN_TITLE),
        )
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .testTag(PRO_PLAN_TEXT)
                .placeholder(
                    color = MaterialTheme.colors.grey_020_grey_900,
                    shape = RoundedCornerShape(4.dp),
                    highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                    visible = isLoading,
                ),
            text = if (formattedPrice != null) stringResource(
                id = R.string.dialog_onboarding_get_pro_description,
                formattedPrice.price
            ) else "",
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.textColorPrimary,
        )
    }
}

@Composable
fun ButtonsRow(
    onSkipPressed: () -> Unit,
    onViewPlansPressed: () -> Unit,
    isLoading: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = 8.dp,
                horizontal = 16.dp
            )
            .placeholder(
                color = MaterialTheme.colors.grey_020_grey_900,
                shape = RoundedCornerShape(4.dp),
                highlight = PlaceholderHighlight.fade(MaterialTheme.colors.surface),
                visible = isLoading,
            ),
        horizontalArrangement = Arrangement.End,
    ) {
        OutlinedMegaButton(
            textId = R.string.general_skip,
            onClick = onSkipPressed,
            modifier = Modifier
                .padding(end = 8.dp)
                .testTag(SKIP_BUTTON),
        )
        RaisedDefaultMegaButton(
            textId = R.string.dialog_onboarding_button_view_pro_plan,
            onClick = onViewPlansPressed,
            modifier = Modifier.testTag(VIEW_PRO_PLAN_BUTTON)
        )
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewVariantAOnboardingDialogView(
    @PreviewParameter(VariantAOnboardingDialogPreviewProvider::class) state: ChooseAccountState,
) {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        VariantAOnboardingDialogView(
            onSkipPressed = {},
            onViewPlansPressed = {},
            state = state,
        )
    }
}

private class VariantAOnboardingDialogPreviewProvider :
    PreviewParameterProvider<ChooseAccountState> {
    override val values: Sequence<ChooseAccountState>
        get() = sequenceOf(
            ChooseAccountState(
                cheapestSubscriptionAvailable = subscriptionProLite
            )
        )

    companion object {
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
    }
}