package mega.privacy.android.domain.entity.continuewhereleftoff

/**
 * Result of monitoring continue-where-you-left-off items.
 *
 * @property items the recently used items, already filtered and flagged for sensitivity.
 * @property isHiddenResolved whether [items] reflect the real hidden-nodes state (feature
 * eligibility and the "show hidden items" setting). It is false for the initial fail-open
 * emission produced before the account/settings load, and true once the real values have been
 * applied. Consumers MUST keep showing a loading state (rather than rendering [items]) while this
 * is false, so sensitive items are never briefly shown unblurred before their blur is applied.
 */
data class ContinueWhereLeftOffResult(
    val items: List<ContinueWhereLeftOffItem>,
    val isHiddenResolved: Boolean,
)
