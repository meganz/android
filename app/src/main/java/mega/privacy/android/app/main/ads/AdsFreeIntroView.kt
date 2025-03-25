package mega.privacy.android.app.main.ads

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.UpgradeAccountActivity
import mega.privacy.android.app.upgradeAccount.UpgradeAccountSource
import mega.privacy.android.shared.original.core.ui.controls.ads.AdsFreeItem
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.dialogs.FullScreenDialog
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.mobile.analytics.event.AdFreeDialogScreenEvent
import mega.privacy.mobile.analytics.event.AdFreeDialogScreenSkipButtonPressedEvent
import mega.privacy.mobile.analytics.event.AdFreeDialogScreenViewProPlansButtonPressedEvent
import java.util.Locale

/**
 * Ads Free Intro View with view model.
 */
@Composable
internal fun AdsFreeIntroView(
    modifier: Modifier = Modifier,
    viewModel: AdsFreeIntroViewModel = hiltViewModel(),
    onDismiss: () -> Unit,
) {
    val state = viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(AdFreeDialogScreenEvent)
    }

    AdsFreeIntroContent(
        modifier = modifier,
        onDismiss = onDismiss,
        uiState = state.value,
    )
}

/**
 * Add free intro view.
 */
@Composable
internal fun AdsFreeIntroContent(
    modifier: Modifier = Modifier,
    uiState: AdsFreeIntroUiState = AdsFreeIntroUiState(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val formattedPrice =
        uiState.cheapestSubscriptionAvailable?.localisePriceCurrencyCode(Locale.getDefault(), true)
    val formattedStorage =
        uiState.cheapestSubscriptionAvailable?.formatStorageSize(usePlaceholder = false)
    val minimalStorageValueAndUnit =
        formattedStorage?.let { "${it.size} ${stringResource(it.unit)}" }.orEmpty()
    FullScreenDialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_add_free),
                contentDescription = "Add free image",
                modifier = Modifier
                    .padding(top = 48.dp)
                    .size(200.dp)
                    .testTag(ADS_FREE_IMAGE_TEST_TAG),
            )
            MegaText(
                text = stringResource(sharedR.string.payment_ads_free_intro_title),
                textColor = TextColor.Primary,
                style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.W500),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 24.dp)
                    .testTag(ADS_FREE_TITLE_TEST_TAG),
            )

            MegaText(
                text = stringResource(
                    sharedR.string.payment_ads_free_intro_description,
                    formattedPrice?.price.orEmpty()
                ),
                textColor = TextColor.Secondary,
                style = MaterialTheme.typography.subtitle2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .testTag(ADS_FREE_DESCRIPTION_TEST_TAG),
            )

            AdsFreeItem(
                title = stringResource(sharedR.string.payment_ads_free_intro_generous_storage_label),
                desc = stringResource(
                    sharedR.string.payment_ads_free_intro_generous_storage_description,
                    minimalStorageValueAndUnit
                ),
                icon = R.drawable.ic_cloud_outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            AdsFreeItem(
                title = stringResource(sharedR.string.payment_ads_free_intro_transfer_sharing_label),
                desc = stringResource(sharedR.string.payment_ads_free_intro_transfer_sharing_description),
                icon = R.drawable.ic_circle_chart_outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            AdsFreeItem(
                title = stringResource(sharedR.string.payment_ads_free_intro_additional_security_label),
                desc = stringResource(sharedR.string.payment_ads_free_intro_additional_security_description),
                icon = R.drawable.ic_lock_outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            Spacer(Modifier.weight(1f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedMegaButton(
                    text = stringResource(R.string.general_skip),
                    onClick = {
                        Analytics.tracker.trackEvent(AdFreeDialogScreenSkipButtonPressedEvent)
                        onDismiss()
                    },
                    rounded = false,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag(SKIP_BUTTON_TEST_TAG),
                )
                RaisedDefaultMegaButton(
                    text = stringResource(sharedR.string.payment_ads_free_intro_button_view_pro_plan),
                    onClick = {
                        Analytics.tracker.trackEvent(
                            AdFreeDialogScreenViewProPlansButtonPressedEvent
                        )
                        UpgradeAccountActivity.navigate(
                            context = context,
                            source = UpgradeAccountSource.ADS_FREE_SCREEN
                        )
                        onDismiss()
                    },
                    modifier = Modifier
                        .testTag(VIEW_PRO_PLAN_BUTTON_TEST_TAG),
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun AddFreeIntroViewPreview() {
    OriginalTheme(isSystemInDarkTheme()) {
        AdsFreeIntroContent(uiState = AdsFreeIntroUiState()) {}
    }
}

/**
 * Test tag for the skip button.
 */
const val SKIP_BUTTON_TEST_TAG = "ads_free_intro_view:skip_button"

/**
 * Test tag for the view pro plan button.
 */
const val VIEW_PRO_PLAN_BUTTON_TEST_TAG = "ads_free_intro_view:view_pro_plan_button"

/**
 * Test tag for the title of the Add Free Intro View.
 */
const val ADS_FREE_TITLE_TEST_TAG = "ads_free_intro_view:title"

/**
 * Test tag for the description of the Add Free Intro View.
 */
const val ADS_FREE_DESCRIPTION_TEST_TAG = "ads_free_intro_view:description"

/**
 * Test tag for the image of the Add Free Intro View.
 */
const val ADS_FREE_IMAGE_TEST_TAG = "ads_free_intro_view:image"
