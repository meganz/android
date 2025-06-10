package mega.privacy.android.domain.usecase.navigation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import javax.inject.Inject
import kotlin.reflect.KClass

class GetStartScreenPreferenceDestinationUseCase @Inject constructor(
    private val monitorStartScreenPreference: MonitorStartScreenPreference,
) {
    operator fun invoke(): Flow<KClass<*>> {
        //Currently assuming that the existing start screen class will be used as destinations, but this might change in the final implementation
        return monitorStartScreenPreference().map {
            it::class
        }
    }
}