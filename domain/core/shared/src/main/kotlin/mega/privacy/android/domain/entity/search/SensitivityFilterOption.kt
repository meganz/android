package mega.privacy.android.domain.entity.search

/**
 * Enum class for Sensitivity Search Filter Option
 */
enum class SensitivityFilterOption {
    /**
     * Option for search all sensitive and non-sensitive nodes
     */
    Disabled,

    /**
     * Option for search sensitive nodes only
     */
    SensitiveOnly,

    /**
     * Option for search non-sensitive nodes only
     */
    NonSensitiveOnly,
}