package mega.privacy.android.app.presentation.manager.model

/**
 * Each enum tabs should extend
 * this interface to define the different tab values for a specific screen
 */
interface Tab {
    /**
     * The position of the tab in the adapter
     */
    val position: Int
}