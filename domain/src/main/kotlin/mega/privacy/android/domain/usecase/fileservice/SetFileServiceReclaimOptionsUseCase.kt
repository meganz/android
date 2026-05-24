package mega.privacy.android.domain.usecase.fileservice

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import mega.privacy.android.domain.repository.FileServiceRepository
import javax.inject.Inject

/**
 * Use case to set the file service reclaim options.
 */
class SetFileServiceReclaimOptionsUseCase @Inject constructor(
    private val fileServiceRepository: FileServiceRepository,
) {
    /**
     * Invoke.
     *
     * @param options the [FileServiceReclaimOptions] to set.
     */
    suspend operator fun invoke(options: FileServiceReclaimOptions) {
        fileServiceRepository.setReclaimOptions(options)
    }
}

