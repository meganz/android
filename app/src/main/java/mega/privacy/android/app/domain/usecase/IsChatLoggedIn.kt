package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

interface IsChatLoggedIn {
    operator fun invoke(): Flow<Boolean>
}
