package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

interface FetchMultiFactorAuthSetting {
    operator fun invoke(): Flow<Boolean>
}