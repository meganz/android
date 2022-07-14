package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

fun interface GetVibrateCountUseCase {

    operator fun invoke(): Flow<Boolean>
}