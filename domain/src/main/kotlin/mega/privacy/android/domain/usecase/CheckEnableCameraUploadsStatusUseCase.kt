package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.account.EnableCameraUploadsStatus
import javax.inject.Inject

/**
 * Use Case that returns the Camera Uploads status when attempting to enable the feature
 *
 * @property getAccountDetailsUseCase Retrieves the User's Account details
 * @property isBusinessAccountActive If the User is on a Master Business or Business Account, this
 * checks if his/her subscription is currently active or not
 */
class CheckEnableCameraUploadsStatusUseCase @Inject constructor(
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val isBusinessAccountActive: IsBusinessAccountActive,
) {

    /**
     * Invocation function
     *
     * @return The Camera Uploads status denoted as [EnableCameraUploadsStatus]
     */
    suspend operator fun invoke(): EnableCameraUploadsStatus {
        val userAccount = getAccountDetailsUseCase(forceRefresh = true)

        return if (userAccount.isBusinessAccount && userAccount.accountTypeIdentifier == AccountType.BUSINESS) {
            val isBusinessAccountActive = isBusinessAccountActive()
            val isMasterBusinessAccount = userAccount.isMasterBusinessAccount

            if (isMasterBusinessAccount) {
                getMasterBusinessAccountCameraUploadsStatus(isBusinessAccountActive)
            } else {
                getBusinessAccountCameraUploadsStatus(isBusinessAccountActive)
            }
        } else {
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
        }
    }

    /**
     * Retrieves the Camera Uploads Status when the User is under a Master Business Account
     *
     * @param isBusinessAccountActive true if the Master Business Account User is active
     * @return The appropriate Camera Uploads Status
     */
    private fun getMasterBusinessAccountCameraUploadsStatus(isBusinessAccountActive: Boolean) =
        if (isBusinessAccountActive) {
            EnableCameraUploadsStatus.CAN_ENABLE_CAMERA_UPLOADS
        } else {
            EnableCameraUploadsStatus.SHOW_SUSPENDED_MASTER_BUSINESS_ACCOUNT_PROMPT
        }

    /**
     * Retrieves the Camera Uploads Status when the User is under a Business Account
     *
     * @param isBusinessAccountActive true if the Business Account User is active
     * @return The appropriate Camera Uploads Status
     */
    private fun getBusinessAccountCameraUploadsStatus(isBusinessAccountActive: Boolean) =
        if (isBusinessAccountActive) {
            EnableCameraUploadsStatus.SHOW_REGULAR_BUSINESS_ACCOUNT_PROMPT
        } else {
            EnableCameraUploadsStatus.SHOW_SUSPENDED_BUSINESS_ACCOUNT_PROMPT
        }
}