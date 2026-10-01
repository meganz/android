package mega.privacy.android.domain.usecase.global

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

class GetAppVersionUseCase @Inject constructor(
    private val environmentRepository: EnvironmentRepository
) {
    operator fun invoke() = environmentRepository.getAppVersion()
}