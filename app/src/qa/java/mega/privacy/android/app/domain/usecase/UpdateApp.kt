package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.Progress

fun interface UpdateApp {
    operator fun invoke(): Flow<Progress>
}