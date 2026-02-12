package mega.privacy.android.domain.featuretoggle

import mega.privacy.android.domain.entity.Feature

enum class DomainFeatures(
    override val description: String,
    private val defaultValue: Boolean,
) : Feature {
    /**
     * Feature flag to select DCIM folder as Sync/Backup
     */
    DCIMSelectionAsSyncBackup(
        "Enable DCIM folder to be selected as Sync/Backup",
        false
    )
    ;

    companion object : FeatureFlagValueProvider {
        override suspend fun isEnabled(feature: Feature) =
            DomainFeatures.entries.firstOrNull { it == feature }?.defaultValue

        override val priority = FeatureFlagValuePriority.Default
    }
}
