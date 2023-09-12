package mega.privacy.android.domain.usecase.transfers.uploads

import javax.inject.Inject

/**
 * Use Case to check if there are Pending Uploads or not
 */
class IsThereAnyPendingUploadsUseCase @Inject constructor(
    private val getNumPendingUploadsUseCase: GetNumPendingUploadsUseCase,
) {

    /**
     * Checks whether there are pending uploads or not
     *
     * @return true if there are pending uploads, and false if otherwise
     */
    suspend operator fun invoke() = getNumPendingUploadsUseCase() > 0
}