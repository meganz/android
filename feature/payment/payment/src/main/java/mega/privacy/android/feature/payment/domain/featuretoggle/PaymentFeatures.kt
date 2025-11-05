package mega.privacy.android.feature.payment.domain.featuretoggle

import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.featuretoggle.FeatureFlagValuePriority
import mega.privacy.android.domain.featuretoggle.FeatureFlagValueProvider

internal enum class PaymentFeatures(
    override val description: String,
    private val defaultValue: Boolean,
) : Feature {

    EnableUSExternalBillingForEligibleUsers(
        description = "Enables US external billing for eligible users",
        defaultValue = false
    )
    ;

    companion object : FeatureFlagValueProvider {

        override suspend fun isEnabled(feature: Feature) =
            PaymentFeatures.entries.firstOrNull { it == feature }?.defaultValue

        override val priority: FeatureFlagValuePriority = FeatureFlagValuePriority.Default
    }
}
