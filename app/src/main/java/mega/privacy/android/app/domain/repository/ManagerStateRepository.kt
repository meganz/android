package mega.privacy.android.app.domain.repository

/**
 * Store data related to manager section
 */
interface ManagerStateRepository {

    /**
     * Get current rubbish parent handle in manager section
     */
    fun getRubbishBinParentHandle(): Long

    /**
     * Set current rubbish parent handle in manager section
     *
     * @param parentHandle the id of the current parent handle
     */
    fun setRubbishBinParentHandle(parentHandle: Long)

    /**
     * Get current browser parent handle in manager section
     */
    fun getBrowserParentHandle(): Long

    /**
     * Set current browser parent handle in manager section
     *
     * @param parentHandle the id of the current parent handle
     */
    fun setBrowserParentHandle(parentHandle: Long)
}