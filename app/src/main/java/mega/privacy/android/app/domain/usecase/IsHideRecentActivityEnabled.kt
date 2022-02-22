package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

interface IsHideRecentActivityEnabled {
    operator fun invoke(): Flow<Boolean>
}