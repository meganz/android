package mega.privacy.android.app.consent.initialiser

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.timeout
import mega.privacy.android.app.appstate.initialisation.initialisers.PostLoginInitialiser
import mega.privacy.android.app.consent.AdConsentWrapper
import mega.privacy.android.app.consent.CookieDialog
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorMiscLoadedUseCase
import mega.privacy.android.domain.usecase.setting.ShouldShowGenericCookieDialogUseCase
import mega.privacy.android.navigation.contract.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.dialog.AppDialogsEventQueue
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
) : PostLoginInitialiser(
    action = { _, _ ->
        val misFlagsLoaded = monitorMiscLoadedUseCase().filter { it }
            .timeout(20.seconds)
            .catch { Timber.e(it) }
            .firstOrNull()

        if (misFlagsLoaded == true) {
            val shouldShowConsentDialog = runCatching {
                shouldShowGenericCookieDialogUseCase(getCookieSettingsUseCase())
            }.getOrDefault(false)
            if (shouldShowConsentDialog) {
                appDialogEventQueue.emit(AppDialogEvent(CookieDialog))
            } else {
                adConsentWrapper.refreshConsent()
            }
        }
    }
)