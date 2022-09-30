package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import mega.privacy.android.app.domain.repository.QARepository
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.usecase.GetFeatureFlagValue
import javax.inject.Inject

class DefaultGetAllFeatureFlags @Inject constructor(
    private val qaRepository: QARepository,
    private val getFeatureFlagValue: GetFeatureFlagValue,
) : GetAllFeatureFlags {
    override fun invoke(): Flow<Map<Feature, Boolean>> {
        return combine(
            flow { emit(qaRepository.getAllFeatures()) },
            qaRepository.monitorLocalFeatureFlags()
        ) { features, localValues ->
            features.associateWith {
                localValues[it.name] ?: getFeatureFlagValue(it)
            }
        }
    }
}