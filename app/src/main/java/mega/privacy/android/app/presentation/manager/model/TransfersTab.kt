package mega.privacy.android.app.presentation.manager.model

/**
 * Define the different tab values for the transfers screen
 */
enum class TransfersTab(override val position: Int) : Tab {
    /**
     * Default value
     */
    NONE(-1),

    /**
     * Pending tab
     */
    PENDING_TAB(0),

    /**
     * Completed tab
     */
    COMPLETED_TAB(1);

    companion object {
        private val map = values().associateBy(TransfersTab::position)

        /**
         * Retrieve the enum value based on the position value
         *
         * @param position
         */
        fun fromPosition(position: Int) = map[position] ?: NONE
    }

}