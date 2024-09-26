package mega.privacy.android.feature.sync.domain.usecase.notifcation

import javax.inject.Inject
import kotlin.random.Random

/**
 * Use case to create a unique id for a notification
 */
class CreateSyncNotificationIdUseCase @Inject constructor() {

    operator fun invoke(): Int = Random.nextInt(Integer.MAX_VALUE)
}
