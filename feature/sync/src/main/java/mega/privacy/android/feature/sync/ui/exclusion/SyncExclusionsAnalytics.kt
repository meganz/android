package mega.privacy.android.feature.sync.ui.exclusion

import mega.privacy.android.feature.sync.domain.entity.MegaIgnoreDefaults
import mega.privacy.android.feature.sync.domain.entity.ParsedMegaIgnore
import mega.privacy.android.feature.sync.domain.entity.SyncSizeExclusion
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.mobile.analytics.core.event.identifier.GeneralEventIdentifier
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsBiggerThanSizeLimitAddedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsBiggerThanSizeLimitEditedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsChangesSavedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsNotBetweenSizeLimitAddedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsNotBetweenSizeLimitEditedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsSetupConfiguredEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsSmallerThanSizeLimitAddedEvent
import mega.privacy.mobile.analytics.event.AndroidSyncExclusionsSmallerThanSizeLimitEditedEvent

/** [ChangesSaved][AndroidSyncExclusionsChangesSavedEvent] for the model that was written. */
internal fun ParsedMegaIgnore.toChangesSavedEvent() = with(counts()) {
    AndroidSyncExclusionsChangesSavedEvent(ruleCount, customRuleCount, hasSizeLimit)
}

/** [SetupConfigured][AndroidSyncExclusionsSetupConfiguredEvent] for the model that was written. */
internal fun ParsedMegaIgnore.toSetupConfiguredEvent() = with(counts()) {
    AndroidSyncExclusionsSetupConfiguredEvent(ruleCount, customRuleCount, hasSizeLimit)
}

/** The size limit these bounds add, or null when they declare none. */
internal fun SyncSizeExclusion.toSizeLimitAddedEvent(): GeneralEventIdentifier? =
    when (condition()) {
        SyncSizeCondition.BiggerThan -> AndroidSyncExclusionsBiggerThanSizeLimitAddedEvent
        SyncSizeCondition.SmallerThan -> AndroidSyncExclusionsSmallerThanSizeLimitAddedEvent
        SyncSizeCondition.NotBetween -> AndroidSyncExclusionsNotBetweenSizeLimitAddedEvent
        null -> null
    }

/** The size limit these bounds are edited into, or null when they declare none. */
internal fun SyncSizeExclusion.toSizeLimitEditedEvent(): GeneralEventIdentifier? =
    when (condition()) {
        SyncSizeCondition.BiggerThan -> AndroidSyncExclusionsBiggerThanSizeLimitEditedEvent
        SyncSizeCondition.SmallerThan -> AndroidSyncExclusionsSmallerThanSizeLimitEditedEvent
        SyncSizeCondition.NotBetween -> AndroidSyncExclusionsNotBetweenSizeLimitEditedEvent
        null -> null
    }

/**
 * The shape of a model, which is all the events report of it: no pattern, extension or byte value
 * leaves the device.
 */
private class ExclusionCounts(
    val ruleCount: Int,
    val customRuleCount: Int,
    val hasSizeLimit: Boolean,
)

private fun ParsedMegaIgnore.counts() = ExclusionCounts(
    ruleCount = rules.size,
    customRuleCount = rules.count { !MegaIgnoreDefaults.isDefault(it) },
    hasSizeLimit = !size.isUnbounded,
)

/**
 * Read the way the by-size form authors bounds, so that a pair is always a range whichever way
 * round its bounds lie.
 */
private fun SyncSizeExclusion.condition(): SyncSizeCondition? {
    val smaller = excludeSmallerThanBytes
    val larger = excludeLargerThanBytes
    return when {
        smaller != null && larger != null -> SyncSizeCondition.NotBetween
        larger != null -> SyncSizeCondition.BiggerThan
        smaller != null -> SyncSizeCondition.SmallerThan
        else -> null
    }
}
