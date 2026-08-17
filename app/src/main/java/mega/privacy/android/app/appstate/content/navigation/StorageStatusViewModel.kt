package mega.privacy.android.app.appstate.content.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import mega.privacy.android.app.appstate.content.navigation.model.StorageQuotaWarning
import mega.privacy.android.core.coroutine.logAndSwallowExceptions
import mega.privacy.android.domain.entity.account.StorageQuotaWarningTrigger
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.account.MonitorStorageStateUseCase
import mega.privacy.android.domain.usecase.account.SetStorageQuotaWarningShownUseCase
import mega.privacy.android.domain.usecase.account.ShouldShowStorageQuotaWarningUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.MonitorSuccessfulUploadsUseCase
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
class StorageStatusViewModel @Inject constructor(
    private val monitorStorageStateUseCase: MonitorStorageStateUseCase,
    private val monitorSuccessfulUploadsUseCase: MonitorSuccessfulUploadsUseCase,
    private val shouldShowStorageQuotaWarningUseCase: ShouldShowStorageQuotaWarningUseCase,
    private val setStorageQuotaWarningShownUseCase: SetStorageQuotaWarningShownUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) : ViewModel() {

    /**
     * Storage quota warnings to show, in the order they are raised.
     *
     * Shared with no replay: [MonitorStorageStateUseCase] re-reads the current state on every
     * subscription, so a collector restarted by a configuration change would reopen a warning the
     * user dismissed. The stop timeout outlives that recreation but ends the sharing between
     * sessions, so a second account sitting at the same state is still warned.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val quotaWarnings: SharedFlow<StorageQuotaWarning> = monitorStorageStateUseCase()
        .distinctUntilChanged()
        .flatMapLatest { storageState ->
            merge(
                flowOf(StorageQuotaWarningTrigger.LoginOrReload),
                monitorSuccessfulUploadsUseCase()
                    .map { StorageQuotaWarningTrigger.UploadSuccess },
            ).map { trigger -> storageState to trigger }
        }
        .filter { (storageState, trigger) ->
            runCatching { shouldShowStorageQuotaWarningUseCase(storageState, trigger) }
                .logAndSwallowExceptions()
                .getOrDefault(false)
        }
        .map { (storageState, trigger) ->
            StorageQuotaWarning(
                storageState = storageState,
                trigger = trigger,
                isUpsellEnabled = isQuotaWarningUpsellEnabled(),
            )
        }
        .logAndSwallowExceptions()
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5.seconds.inWholeMilliseconds,
            ),
            replay = 0,
        )

    /**
     * Spend the trigger's daily allowance, once the screen has actually shown the warning. Kept out
     * of [quotaWarnings] so a warning raised while nothing is collecting costs nothing.
     */
    suspend fun onQuotaWarningShown(warning: StorageQuotaWarning) {
        runCatching { setStorageQuotaWarningShownUseCase(warning.storageState, warning.trigger) }
            .logAndSwallowExceptions()
    }

    private suspend fun isQuotaWarningUpsellEnabled() =
        runCatching { getFeatureFlagValueUseCase(ApiFeatures.QuotaWarningUpsellScreen) }
            .getOrElse { false }
}
