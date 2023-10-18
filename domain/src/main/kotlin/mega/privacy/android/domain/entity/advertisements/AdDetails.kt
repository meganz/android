package mega.privacy.android.domain.entity.advertisements

/**
 * Details for each individual ad
 *
 * @param slotId         The ad slot id for specific screen
 * @param url            The ad url to be loaded in webview
 */
data class AdDetails(
    val slotId: String,
    val url: String,
)
