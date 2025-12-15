package mega.privacy.android.domain.usecase.recentactions

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.repository.RecentActionsRepository
import mega.privacy.android.domain.usecase.contact.GetCurrentUserEmail
import javax.inject.Inject

/**
 * Get a single recent action bucket by its identifier
 *
 * This use case is optimized to fetch and process only the matching bucket,
 * avoiding unnecessary processing of all buckets.
 */
class GetRecentActionBucketByIdUseCase @Inject constructor(
    private val recentActionsRepository: RecentActionsRepository,
    private val getCurrentUserEmail: GetCurrentUserEmail,
    private val contactsRepository: ContactsRepository,
    private val typedRecentActionBucketMapper: TypedRecentActionBucketMapper,
) {

    /**
     * Get a single recent action bucket by identifier
     *
     * @param bucketIdentifier The unique identifier of the bucket
     * @param excludeSensitives Exclude sensitive nodes
     * @return The matching [RecentActionBucket] or null if not found
     */
    suspend operator fun invoke(
        bucketIdentifier: String,
        excludeSensitives: Boolean,
    ): RecentActionBucket? = coroutineScope {
        val matchingBucketDeferred = async {
            recentActionsRepository.getRecentActionBucketByIdentifier(
                bucketIdentifier = bucketIdentifier,
                excludeSensitives = excludeSensitives,
            )
        }
        val visibleContactsDeferred = async { contactsRepository.getAllContactsName() }
        val currentUserEmailDeferred = async { getCurrentUserEmail(false) }

        val visibleContacts = visibleContactsDeferred.await()
        val currentUserEmail = currentUserEmailDeferred.await()
        val matchingBucket = matchingBucketDeferred.await()
            ?: return@coroutineScope null

        typedRecentActionBucketMapper(
            buckets = listOf(matchingBucket),
            visibleContacts = visibleContacts,
            currentUserEmail = currentUserEmail,
        ).firstOrNull()
    }
}

