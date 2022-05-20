package mega.privacy.android.app.data.repository

import mega.privacy.android.app.domain.repository.ManagerStateRepository
import javax.inject.Inject

/**
 * Default manager state repository implementation
 */
class DefaultManagerStateRepository @Inject constructor() : ManagerStateRepository {

    /**
     * Current rubbish parent handle
     */
    private var rubbishBinParentHandle: Long = -1L

    /**
     * Current browser parent handle
     */
    private var browserParentHandle: Long = -1L

    override fun getRubbishBinParentHandle(): Long {
        return rubbishBinParentHandle
    }

    override fun setRubbishBinParentHandle(parentHandle: Long) {
        rubbishBinParentHandle = parentHandle
    }

    override fun getBrowserParentHandle(): Long {
        return browserParentHandle
    }

    override fun setBrowserParentHandle(parentHandle: Long) {
        browserParentHandle = parentHandle
    }
}