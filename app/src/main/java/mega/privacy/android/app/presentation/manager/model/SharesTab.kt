package mega.privacy.android.app.presentation.manager.model

/**
 * Define the different values for the shares screen
 *
 * @param position the position of the tab in the adapter
 */
enum class SharesTab(val position: Int) {
    /**
     * Default value
     */
    NONE(-1),

    /**
     * Incoming tab
     */
    INCOMING_TAB(0),

    /**
     * Outgoing tab
     */
    OUTGOING_TAB(1),

    /**
     * Link tab
     */
    LINKS_TAB(2);

    companion object {
        private val map = values().associateBy(SharesTab::position)

        /**
         * Retrieve the enum value based on the position value
         *
         * @param position
         */
        fun fromPosition(position: Int) = map[position] ?: NONE
    }

}