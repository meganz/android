package mega.privacy.android.app.presentation.advertisements.model

/**
 * Ads Load State
 */
sealed interface AdsLoadState {

    /**
     * The Ads state is loaded
     *
     * @param url the url to load ads
     */
    data class Loaded(val url: String) : AdsLoadState

    /**
     * The Ads state is empty
     */
    object Empty : AdsLoadState
}
