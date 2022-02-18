package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

interface GetStartScreen {
    operator fun invoke(): Flow<Int>
}