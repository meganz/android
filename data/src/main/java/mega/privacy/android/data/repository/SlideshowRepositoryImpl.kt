package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.preferences.SlideshowPreferencesGateway
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.repository.SlideshowRepository
import javax.inject.Inject

internal class SlideshowRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val slideshowPreferencesGateway: SlideshowPreferencesGateway,
) : SlideshowRepository {
    override fun monitorOrderSetting(): Flow<SlideshowOrder?> =
        slideshowPreferencesGateway.monitorOrderSetting(megaApiGateway.myUserHandle)

    override suspend fun saveOrderSetting(order: SlideshowOrder) =
        slideshowPreferencesGateway.saveOrderSetting(megaApiGateway.myUserHandle, order)

    override fun monitorSpeedSetting(): Flow<SlideshowSpeed?> =
        slideshowPreferencesGateway.monitorSpeedSetting(megaApiGateway.myUserHandle)

    override suspend fun saveSpeedSetting(speed: SlideshowSpeed) =
        slideshowPreferencesGateway.saveSpeedSetting(megaApiGateway.myUserHandle, speed)

    override fun monitorRepeatSetting(): Flow<Boolean?> =
        slideshowPreferencesGateway.monitorRepeatSetting(megaApiGateway.myUserHandle)

    override suspend fun saveRepeatSetting(isRepeat: Boolean) =
        slideshowPreferencesGateway.saveRepeatSetting(megaApiGateway.myUserHandle, isRepeat)
}
