package mega.privacy.android.domain.usecase.fileservice

import mega.privacy.android.domain.entity.fileservice.FileServiceReclaimOptions
import mega.privacy.android.domain.repository.FileServiceRepository
import javax.inject.Inject

/**
 * Use case to set the file service reclaim options.
 *
 * Keeping this orchestration at the use case layer is a deliberate business decision: callers
 * configure both surfaces with a single invocation, and if the logged-in user's file service
 * and the public link file service ever need to diverge, the change is local to this class —
 * the repository stays single-purpose.
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
        fileServiceRepository.setPublicLinkReclaimOptions(options)
    }
}

