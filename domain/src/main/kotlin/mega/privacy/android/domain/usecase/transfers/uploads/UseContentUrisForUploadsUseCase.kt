package mega.privacy.android.domain.usecase.transfers.uploads

import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import javax.inject.Inject

/**
 * Use case that returns whether content uris should be used for uploads or not, if not a path should be provided to the sdk instead of a content uri
 */
class UseContentUrisForUploadsUseCase @Inject constructor(
    private val transferRepository: TransferRepository,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {
    /**
     * Invoke
     *
     * @return true if content uris should be used for uploads false if file path should be used
     */
    suspend operator fun invoke(isForChat: Boolean) =
        when {
            getFeatureFlagValueUseCase(DomainFeatures.UseFileDescriptorForUploads).not() -> false
            isForChat -> false
            else -> transferRepository.allowTransfersWithContentUris()
        }
}
