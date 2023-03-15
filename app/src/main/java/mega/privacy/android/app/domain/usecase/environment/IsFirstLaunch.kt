package mega.privacy.android.app.domain.usecase.environment

import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject

class IsFirstLaunch @Inject constructor(private val environmentRepository: EnvironmentRepository) {
    suspend operator fun invoke(): Boolean = environmentRepository.getIsFirstLaunch() ?: true
}