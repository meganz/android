package mega.privacy.android.domain.entity.preference

/**
 * Start screen
 *
 * @property id
 */
enum class StartScreen(val id: Int) {
    /**
     * Cloud drive
     */
    CloudDrive(0),

    /**
     * Photos
     */
    Photos(1),

    /**
     * Home
     */
    Home(2),

    /**
     * Chat
     */
    Chat(3),

    /**
     * Shared items
     */
    SharedItems(4),

    /**
     * None
     */
    None(5);

    companion object{
        /**
         * Return the correct start screen associated with the given id, or None if none matches
         *
         * @param id
         *
         */
        operator fun invoke(id: Int) = values().firstOrNull { it.id == id } ?: None
    }
}