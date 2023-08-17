package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import mega.privacy.android.data.extensions.failWithError
import mega.privacy.android.data.extensions.getRequestListener
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.data.mapper.MegaStringMapMapper
import mega.privacy.android.data.wrapper.StringWrapper
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.RemotePreferencesRepository
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
internal class RemotePreferencesRepositoryImpl @Inject constructor(
    private val megaApiGateway: MegaApiGateway,
    private val megaStringMapMapper: MegaStringMapMapper,
    private val stringWrapper: StringWrapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : RemotePreferencesRepository {

    companion object {
        private const val MEETING_ONBOARDING_PREF_KEY = "aObSm"
    }

    override suspend fun setMeetingTooltipPreference(item: MeetingTooltipItem) =
        withContext(ioDispatcher) {
            val preference = mapOf(
                MEETING_ONBOARDING_PREF_KEY to stringWrapper.encodeBase64(item.name)
            )
            updateAppPreferences(preference)
        }

    override suspend fun getMeetingTooltipPreference(): MeetingTooltipItem =
        withContext(ioDispatcher) {
            getAppPreferences().get(MEETING_ONBOARDING_PREF_KEY)
                ?.let(stringWrapper::decodeBase64)
                ?.let(MeetingTooltipItem::valueOf)
                ?: MeetingTooltipItem.CREATE
        }

    private suspend fun getAppPreferences(): Map<String, String> =
        withContext(ioDispatcher) {
            suspendCancellableCoroutine { continuation ->
                val listener = OptionalMegaRequestListenerInterface(
                    onRequestFinish = { request: MegaRequest, error: MegaError ->
                        when (error.errorCode) {
                            MegaError.API_OK -> {
                                val result = request.megaStringMap.let(megaStringMapMapper::invoke)
                                continuation.resume(result)
                            }

                            MegaError.API_ENOENT ->
                                continuation.resume(emptyMap())

                            else -> {
                                val method = "getUserAttribute(MegaApiJava.USER_ATTR_APPS_PREFS)"
                                continuation.failWithError(error, method)
                            }
                        }
                    }
                )

                megaApiGateway.getUserAttribute(
                    attributeIdentifier = MegaApiJava.USER_ATTR_APPS_PREFS,
                    listener = listener
                )

                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }

    private suspend fun updateAppPreferences(preferences: Map<String, String>) =
        withContext(ioDispatcher) {
            val updatedPreferences = getAppPreferences().toMutableMap()
                .apply { putAll(preferences) }
                .let(megaStringMapMapper::invoke)

            suspendCancellableCoroutine { continuation ->
                val listener = continuation.getRequestListener(
                    methodName = "setUserAttribute(MegaApiJava.USER_ATTR_APPS_PREFS)"
                ) {}

                megaApiGateway.setUserAttribute(
                    type = MegaApiJava.USER_ATTR_APPS_PREFS,
                    value = updatedPreferences,
                    listener = listener
                )

                continuation.invokeOnCancellation {
                    megaApiGateway.removeRequestListener(listener)
                }
            }
        }
}
