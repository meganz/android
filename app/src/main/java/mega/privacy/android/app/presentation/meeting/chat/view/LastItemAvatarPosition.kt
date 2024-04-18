package mega.privacy.android.app.presentation.meeting.chat.view

/**
 * Enum class to represent the position of the avatar of the last item in the list. This is used to make it always visible while scrolling.
 */
enum class LastItemAvatarPosition {
    /**
     * The avatar should be fixed at the bottom of the List to simulate it's scrolling with the list
     */
    Scrolling,

    /**
     * The avatar should be at the top of the message
     */
    Top,

    /**
     * The avatar should be at the bottom of the message
     */
    Bottom;

    /**
     * @return If true, the avatar should be drawn by the message, either at the top or the bottom, otherwise it should be fixed at the bottom of the list
     */
    fun shouldBeDrawnByMessage(): Boolean = this != Scrolling
}