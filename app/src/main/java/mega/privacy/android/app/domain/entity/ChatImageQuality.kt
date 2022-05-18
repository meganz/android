package mega.privacy.android.app.domain.entity

/**
 * Enum class defining chat image quality available settings.
 */
enum class ChatImageQuality {

    Automatic, Original, Optimised;

    companion object {
        val DEFAULT = Automatic
    }

}
