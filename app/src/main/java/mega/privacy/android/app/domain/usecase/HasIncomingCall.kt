package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

interface HasIncomingCall {
    operator fun invoke(): Flow<Boolean>
}
