package mega.privacy.android.domain.usecase.featureflag

import mega.privacy.android.domain.entity.featureflag.Flag
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.FeatureFlagRepository
import javax.inject.Inject


/**
 * Raise hand to speak Use case.
 */
class GetFlagUseCase @Inject constructor(
    private val callRepository: CallRepository,
) {
    /**
     * Invoke
     *
     * @param nameFlag  Name flag
     * @return          [Flag]
     */
    suspend operator fun invoke(
        nameFlag: String,
    ): Flag? = callRepository.getFlag(nameFlag)
}