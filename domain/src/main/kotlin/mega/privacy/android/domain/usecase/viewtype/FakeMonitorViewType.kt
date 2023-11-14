package mega.privacy.android.domain.usecase.viewtype

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Fake [MonitorViewType] to simulate the emission of values
 */
class FakeMonitorViewType : MonitorViewType {

    private val flow = MutableSharedFlow<ViewType>()

    /**
     * Emits a value
     *
     * @param value the [ViewType] to be emitted
     */
    suspend fun emit(value: ViewType) = flow.emit(value)
    override fun invoke(): Flow<ViewType> = flow
}