package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow


interface IsOnline {
operator fun invoke(): Flow<Boolean>
}
