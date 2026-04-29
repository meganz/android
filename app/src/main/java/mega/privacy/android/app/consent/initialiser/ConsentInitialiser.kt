package mega.privacy.android.app.consent.initialiser

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import mega.privacy.android.app.consent.AdConsentWrapper
import mega.privacy.android.navigation.destination.CookieDialogNavKey
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.domain.usecase.setting.ShouldShowGenericCookieDialogUseCase
import mega.privacy.android.navigation.contract.initialisation.initialisers.AppStartInitialiserAction
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogsEventQueue
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@OptIn(FlowPreview::class)
class ConsentInitialiser @Inject constructor(
    private val getCookieSettingsUseCase: GetCookieSettingsUseCase,
    private val appDialogEventQueue: AppDialogsEventQueue,
    private val shouldShowGenericCookieDialogUseCase: ShouldShowGenericCookieDialogUseCase,
    private val monitorMiscLoadedUseCase: MonitorMiscLoadedUseCase,
    private val adConsentWrapper: AdConsentWrapper,
) : AppStartInitialiserAction(action = {
    val misFlagsLoaded = monitorMiscLoadedUseCase().filter { it }
        .timeout(20.seconds)
        .catch { Timber.e(it) }
        .firstOrNull()

    if (misFlagsLoaded == true) {
        val shouldShowCookieDialog = runCatching {
            shouldShowGenericCookieDialogUseCase(getCookieSettingsUseCase())
        }.getOrDefault(false)

        if (shouldShowCookieDialog) {
            appDialogEventQueue.emit(AppDialogEvent(CookieDialogNavKey))
        } else {
            adConsentWrapper.refreshConsent()
        }
    }
})