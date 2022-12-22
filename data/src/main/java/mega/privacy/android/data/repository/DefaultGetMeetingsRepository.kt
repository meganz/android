package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.GetMeetingsRepository
import javax.inject.Inject

/**
 * Default implementation of [GetMeetingsRepository]
 *
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property megaApiGateway                     [MegaApiGateway]
 * @property ioDispatcher                       [CoroutineDispatcher]
 */
internal class DefaultGetMeetingsRepository @Inject constructor(
    private val megaChatApiGateway: MegaChatApiGateway,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GetMeetingsRepository {
}
