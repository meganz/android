package mega.privacy.android.app.upgradeAccount.view

import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.ChooseAccountState
import mega.privacy.android.app.upgradeAccount.view.ChooseAccountPreviewProvider.Companion.subscriptionProLite
import mega.privacy.android.app.upgradeAccount.view.components.ButtonsRow
import mega.privacy.android.app.upgradeAccount.view.components.FeatureRow
import mega.privacy.android.app.upgradeAccount.view.components.GetProPlanColumn
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R.string.dialog_onboarding_feature_storage_description
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 *  Compose UI for new Onboarding dialog (Choose account screen), this is Variant A
 *  User will see this screen when the registration was finished and user signs in for the first time ever
 */
@Composable
fun VariantAOnboardingDialogView(
    state: ChooseAccountState,
    onSkipPressed: () -> Unit,
    onViewPlansPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val cheapestSubscriptionAvailable = state.cheapestSubscriptionAvailable
    val isLoading = cheapestSubscriptionAvailable == null
    val formattedStorage = cheapestSubscriptionAvailable?.formatStorageSize(usePlaceholder = false)
    val minimalStorageUnitString = formattedStorage?.let { stringResource(id = it.unit) } ?: ""
    val minimalStorageSizeString = formattedStorage?.size ?: ""

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = modifier
                .width(390.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Image(
                painter = painterResource(id = R.drawable.image_upselling_onboarding_dialog),
                contentDescription = "",
                modifier = modifier
                    .testTag(IMAGE_TAG)
                    .size(140.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            GetProPlanColumn(
                state = state,
                isLoading = isLoading,
                bodyTextStyle = MaterialTheme.typography.subtitle2
            )
            Spacer(modifier = Modifier.height(16.dp))
            //Storage
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_storage_onboarding_dialog),
                title = stringResource(id = R.string.dialog_onboarding_feature_title_storage),
                description = stringResource(
                    id = dialog_onboarding_feature_storage_description,
                    minimalStorageSizeString,
                    minimalStorageUnitString
                ),
                testTag = STORAGE_DESCRIPTION_ROW,
                isLoading = isLoading,
            )
            //File sharing
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_file_sharing_onboarding_dialog),
                title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_file_sharing),
                description = stringResource(id = sharedR.string.dialog_onboarding_feature_description_file_sharing),
                testTag = FILE_SHARING_DESCRIPTION_ROW,
                isLoading = isLoading,
            )
            //Back-up and rewind
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_backup_onboarding_dialog),
                title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_backup_rewind),
                description = stringResource(id = sharedR.string.dialog_onboarding_feature_description_backup_rewind),
                testTag = BACKUP_DESCRIPTION_ROW,
                isLoading = isLoading,
            )
            //Extra features
            FeatureRow(
                drawableID = painterResource(id = R.drawable.ic_mega_onboarding_dialog),
                title = stringResource(id = sharedR.string.dialog_onboarding_feature_title_additional_features),
                description = stringResource(
                    id = if (state.showAdsFeature) sharedR.string.dialog_onboarding_feature_description_additional_features_with_ads else sharedR.string.dialog_onboarding_feature_description_additional_features_without_ads
                ),
                testTag = ADDITIONAL_FEATURES_DESCRIPTION_ROW,
                isLoading = isLoading,
            )
            Spacer(modifier = Modifier.height(18.dp))
            ButtonsRow(
                onSkipPressed = onSkipPressed,
                onViewPlansPressed = onViewPlansPressed,
                isLoading = isLoading,
            )
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PreviewVariantAOnboardingDialogView(
    @PreviewParameter(VariantAOnboardingDialogPreviewProvider::class) state: ChooseAccountState,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
}

internal const val IMAGE_TAG = "onboarding_screen_variant_a:image"
internal const val PRO_PLAN_TITLE = "onboarding_screen:pro_plan_title"
internal const val PRO_PLAN_TEXT = "onboarding_screen:pro_plan_description"
internal const val STORAGE_DESCRIPTION_ROW = "onboarding_screen:storage_description_row"
internal const val FILE_SHARING_DESCRIPTION_ROW =
    "onboarding_screen:file_sharing_description_row"
internal const val BACKUP_DESCRIPTION_ROW = "onboarding_screen:backup_description_row"
internal const val ADDITIONAL_FEATURES_DESCRIPTION_ROW =
    "onboarding_screen:additional_features_description_row"
internal const val SKIP_BUTTON = "onboarding_screen_variant_a:skip_button"
internal const val VIEW_PRO_PLAN_BUTTON = "onboarding_screen_variant_a:view_pro_plan_button"