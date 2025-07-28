package mega.privacy.android.domain.featuretoggle

import mega.privacy.android.domain.entity.Feature

enum class DomainFeatures(
    override val description: String,
    private val defaultValue: Boolean,
) : Feature {

    /**
     * Use mega.app domain instead of mega.nz domain
     */
    MegaDotAppDomain(
        "Use mega.app domain instead of mega.nz domain",
        false,
    )
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            DomainFeatures.entries.firstOrNull { it == feature }?.defaultValue

        override val priority = FeatureFlagValuePriority.Default
    }
}