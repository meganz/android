package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow

interface ShouldHideRecentActivity {
    operator fun invoke(): Flow<Boolean>
}