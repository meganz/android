package mega.privacy.android.feature.documentscanner.domain.entity

/**
 * Quality assessment result for a scanned page.
 */
enum class PageQuality {
    /** Page is clear and well-exposed */
    GOOD,

    /** Page appears blurry */
    BLURRY,

    /** Page is too dark */
    DARK,

    /** Page is too bright / washed out */
    OVEREXPOSED,
}
