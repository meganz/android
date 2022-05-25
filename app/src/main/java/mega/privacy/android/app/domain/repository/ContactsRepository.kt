package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import nz.mega.sdk.MegaContactRequest

interface ContactsRepository {

    /**
     * Monitor contact request updates.
     *
     * @return A flow of all global contact request updates.
     */
    fun monitorContactRequestUpdates(): Flow<List<MegaContactRequest>>
}