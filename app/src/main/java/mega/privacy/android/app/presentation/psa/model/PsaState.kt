package mega.privacy.android.app.presentation.psa.model

/**
 * Psa state
 */
sealed interface PsaState {
    /**
     * Id
     */
    val id: Int

    /**
     * No psa
     */
    data object NoPsa : PsaState {
        override val id = -1
    }

    /**
     * Web psa
     *
     * @property id
     * @property url
     */
    data class WebPsa(
        override val id: Int,
        val url: String,
    ) : PsaState


    /**
     * Psa
     *
     * @property id
     * @property title
     * @property text
     * @property imageUrl
     * @property positiveText
     * @property positiveLink
     */
    data class StandardPsa(
        override val id: Int,
        val title: String,
        val text: String,
        val imageUrl: String?,
        val positiveText: String,
        val positiveLink: String,
    ) : PsaState

    /**
     * Info psa
     *
     * @property id
     * @property title
     * @property text
     * @property imageUrl
     * @constructor Create empty Info psa
     */
    data class InfoPsa(
        override val id: Int,
        val title: String,
        val text: String,
        val imageUrl: String?,
    ) : PsaState
}