package mega.privacy.android.domain.usecase.recentactions

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import javax.inject.Inject

/**
 * Get a list of recent actions
 */
class GetRecentActionsUseCase @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val contactsRepository: ContactsRepository,
    private val typedRecentActionBucketMapper: TypedRecentActionBucketMapper,
) {

    /**
     * Get a list of recent actions
     *
     * @param excludeSensitives Exclude sensitive nodes
     * @return a list of recent actions
     */
    suspend operator fun invoke(
        excludeSensitives: Boolean,
        maxBucketCount: Int = 500,
    ): List<RecentActionBucket> = coroutineScope {
        val recentActionsDeferred = async {
            recentActionsRepository.getRecentActions(
                excludeSensitives = excludeSensitives,
                maxBucketCount = maxBucketCount,
            )
        }
        val visibleContactsDeferred = async { contactsRepository.getAllContactsName() }
        val currentUserEmailDeferred = async { getCurrentUserEmail(false) }

        val visibleContacts = visibleContactsDeferred.await()
        val currentUserEmail = currentUserEmailDeferred.await()
        val buckets = recentActionsDeferred.await()

        typedRecentActionBucketMapper(
            buckets = buckets,
            visibleContacts = visibleContacts,
            currentUserEmail = currentUserEmail,
        ).filter { it.nodes.isNotEmpty() } // Filter out again as filterIsInstance inside map may return empty list
    }
}
