package mega.privacy.android.domain.usecase.featureflag

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mega.privacy.android.domain.entity.navigation.Flagged
import javax.inject.Inject

/**
 * Get enabled flagged items use case
 *
 * @property getFeatureFlagValueUseCase
 */
class GetEnabledFlaggedItemsUseCase @Inject constructor(
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    operator fun <T> invoke(items: Set<T>): Flow<Set<T>> = flow {
        val filteredItems = items.filter { item ->
            if (item is Flagged) {
                getFeatureFlagValueUseCase(item.feature)
            } else {
                true
            }
        }.toSet()
        emit(filteredItems)
        awaitCancellation()
    }
}