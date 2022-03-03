package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DefaultIsChatLoggedIn @Inject constructor() : IsChatLoggedIn {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(true)
        }
    }
}