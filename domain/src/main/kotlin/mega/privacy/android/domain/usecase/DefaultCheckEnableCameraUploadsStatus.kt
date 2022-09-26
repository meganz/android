package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import mega.privacy.android.domain.qualifier.IoDispatcher
import javax.inject.Inject

/**
 * Class that implements the Use Case [CheckEnableCameraUploadsStatus]
 *
 * @param getAccountDetails [GetAccountDetails] Use Case
 * @param isBusinessAccountActive [IsBusinessAccountActive] Use Case
 * @param ioDispatcher the [CoroutineDispatcher]
 */
class DefaultCheckEnableCameraUploadsStatus @Inject constructor(
    private val getAccountDetails: GetAccountDetails,
    private val isBusinessAccountActive: IsBusinessAccountActive,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CheckEnableCameraUploadsStatus {

    /**
     * Retrieves the Account Details, then checks the account's Business Status handled in
     * [checkBusinessAccountStatus]
     *
     * If the user is on a regular account, [EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS]
     * is immediately returned
     *
     * @return the corresponding [EnableCameraUploadsStatus]
     */
    override suspend fun invoke(): EnableCameraUploadsStatus = withContext(ioDispatcher) {
        val userAccount = getAccountDetails(forceRefresh = true)

        if (userAccount.isMasterBusinessAccount) {
            checkBusinessAccountStatus(isMasterBusinessAccount = true)
        } else if (userAccount.isBusinessAccount) {
            checkBusinessAccountStatus(isMasterBusinessAccount = false)
        } else {
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
        }
    }

    /**
     * Checks the Business Account status and returns the corresponding [EnableCameraUploadsStatus]
     *
     * @param isMasterBusinessAccount Whether this is a Master Business Account or not
     *
     * @return the corresponding [EnableCameraUploadsStatus]
     */
    private suspend fun checkBusinessAccountStatus(isMasterBusinessAccount: Boolean): EnableCameraUploadsStatus {
        val isBusinessAccountActive = isBusinessAccountActive()

        return if (isMasterBusinessAccount && isBusinessAccountActive) {
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
        } else if (!isMasterBusinessAccount && isBusinessAccountActive) {
            EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
        } else {
            EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
        }
    }
}