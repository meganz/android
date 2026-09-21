package mega.privacy.android.domain.entity.featureflag

/**
 * Flag types.
 */
enum class FlagTypes {
    /**
     * Invalid type
     */
    Invalid,

    /**
     *  AB test type
     */
    ABTest,

    /**
     *  Feature type
     */
    Feature,
}