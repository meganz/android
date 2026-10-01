package mega.privacy.android.domain.entity.featureflag

/**
 * Flag
 *
 * @property type   [FlagTypes]
 * @property group  [GroupFlagTypes]
 */
data class Flag(
    val type: FlagTypes,
    val group: GroupFlagTypes,
)