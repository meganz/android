package mega.privacy.android.feature.payment.presentation.upgrade

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.AccountSubscriptionCycle
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SubscriptionStatus
import mega.privacy.android.feature.payment.model.AccountStorageUIState
import mega.privacy.android.feature.payment.model.UpgradeAccountState
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountPreviewProvider.Companion.localisedSubscriptionsList
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountPreviewProvider.Companion.multipleOfferSubscriptionsList
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountPreviewProvider.Companion.singleOfferSubscriptionsList
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountPreviewProvider.Companion.subscriptionProI
import mega.privacy.android.feature.payment.presentation.upgrade.UpgradeAccountPreviewProvider.Companion.subscriptionProLite

/**
 * Screenshot tests for [UpgradeAccountScreen], covering the legacy and revamped layouts, each offer
 * highlight, the loading skeleton, the unavailable-subscription banner and the landscape two-pane
 * layout.
 *
 * None of the fixtures carries an offer `validUntil`, so the wall-clock countdown is not rendered
 * and the reference images stay stable.
 */
class UpgradeAccountScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LegacyNewAccount() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = availableState(),
                isNewCreationAccount = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LegacyMonthlyPlanSelected() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = availableState(),
                initialChosenPlan = AccountType.PRO_I,
                initialIsMonthly = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LegacyUpgradeFromCurrentPlan() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = currentProIState(),
                isUpgradeAccount = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun LegacySubscriptionUnavailable() {
        AndroidThemeForPreviews {
            UpgradeAccount(uiState = unavailableState())
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RevampFreeUserYearly() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = availableState(),
                isSubscriptionRevampEnabled = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RevampFreeUserMonthly() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = availableState(),
                isSubscriptionRevampEnabled = true,
                initialIsMonthly = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RevampUpgradeFromCurrentPlan() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = currentProIState(),
                isUpgradeAccount = true,
                isSubscriptionRevampEnabled = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RevampSingleOffer() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = singleOfferState(),
                isSubscriptionRevampEnabled = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RevampMultipleOffers() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = UpgradeAccountState(
                    localisedSubscriptionsList = multipleOfferSubscriptionsList,
                    isSubscriptionFeatureAvailable = true,
                    cheapestSubscriptionAvailable = subscriptionProI,
                ),
                isSubscriptionRevampEnabled = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RevampLoadingSkeleton() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = UpgradeAccountState(),
                isSubscriptionRevampEnabled = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun RevampSubscriptionUnavailable() {
        AndroidThemeForPreviews {
            UpgradeAccount(
                uiState = unavailableState(),
                isSubscriptionRevampEnabled = true,
            )
        }
    }

    @PreviewTest
    @LandscapeThemePreviews
    @Composable
    fun RevampLandscape() {
        AndroidThemeForPreviews {
            Landscape {
                UpgradeAccount(
                    uiState = currentProIState(),
                    isUpgradeAccount = true,
                    isSubscriptionRevampEnabled = true,
                )
            }
        }
    }

    @PreviewTest
    @LandscapeThemePreviews
    @Composable
    fun RevampSingleOfferLandscape() {
        AndroidThemeForPreviews {
            Landscape {
                UpgradeAccount(
                    uiState = singleOfferState(),
                    isSubscriptionRevampEnabled = true,
                )
            }
        }
    }

    @Composable
    private fun Landscape(content: @Composable () -> Unit) {
        val configuration = Configuration(LocalConfiguration.current).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
        }
        CompositionLocalProvider(LocalConfiguration provides configuration, content = content)
    }

    @Composable
    private fun UpgradeAccount(
        uiState: UpgradeAccountState,
        isNewCreationAccount: Boolean = false,
        isUpgradeAccount: Boolean = false,
        isSubscriptionRevampEnabled: Boolean = false,
        initialChosenPlan: AccountType? = null,
        initialIsMonthly: Boolean = false,
    ) {
        UpgradeAccountScreen(
            onInAppCheckoutClick = {},
            maybeLaterClicked = {},
            onFreePlanClicked = {},
            onBack = {},
            uiState = uiState,
            accountStorageUiState = accountStorage,
            isNewCreationAccount = isNewCreationAccount,
            isUpgradeAccount = isUpgradeAccount,
            isSubscriptionRevampEnabled = isSubscriptionRevampEnabled,
            initialChosenPlan = initialChosenPlan,
            initialIsMonthly = initialIsMonthly,
        )
    }

    private fun availableState() = UpgradeAccountState(
        localisedSubscriptionsList = localisedSubscriptionsList,
        isSubscriptionFeatureAvailable = true,
        cheapestSubscriptionAvailable = subscriptionProLite,
    )

    private fun currentProIState() = availableState().copy(
        currentSubscriptionPlan = AccountType.PRO_I,
        subscriptionCycle = AccountSubscriptionCycle.YEARLY,
        subscriptionStatus = SubscriptionStatus.VALID,
        subscriptionRenewTime = 1_815_000_000L,
    )

    private fun singleOfferState() = UpgradeAccountState(
        localisedSubscriptionsList = singleOfferSubscriptionsList,
        isSubscriptionFeatureAvailable = true,
        cheapestSubscriptionAvailable = subscriptionProLite,
    )

    private fun unavailableState() = UpgradeAccountState(
        localisedSubscriptionsList = localisedSubscriptionsList,
        isSubscriptionFeatureAvailable = false,
    )

    private companion object {
        val accountStorage = AccountStorageUIState(
            baseStorage = 15L * 1024 * 1024 * 1024,
            totalStorage = 100L * 1024 * 1024 * 1024,
        )
    }
}

@Preview(name = "1-Dark theme", widthDp = 800, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "2-Light theme", widthDp = 800, heightDp = 400, uiMode = Configuration.UI_MODE_NIGHT_NO)
internal annotation class LandscapeThemePreviews
