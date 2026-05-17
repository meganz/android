package mega.privacy.android.app.listeners.global

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.AlertsAndWarnings
import mega.privacy.android.data.mapper.StorageStateMapper
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.featureflag.MiscLoadedState
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.account.BroadcastMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.account.SetSecurityUpgradeInAppUseCase
import mega.privacy.android.domain.usecase.domainmigration.UpdateDomainNameUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.BroadcastMiscStateUseCase
import nz.mega.sdk.MegaEvent
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

class GlobalOnEventHandler @Inject constructor(
    @ApplicationContext private val appContext: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val storageStateMapper: StorageStateMapper,
    private val setSecurityUpgradeInAppUseCase: SetSecurityUpgradeInAppUseCase,
    private val broadcastMiscStateUseCase: BroadcastMiscStateUseCase,
    private val getAccountDetailsUseCase: GetAccountDetailsUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val updateDomainNameUseCase: UpdateDomainNameUseCase,
    private val broadcastMyAccountUpdateUseCase: BroadcastMyAccountUpdateUseCase,
) {
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    operator fun invoke(event: MegaEvent?) {
        if (event == null) return

        Timber.d("Event received: text(${event.text}), type(${event.type}), number(${event.number})")

        when (event.type) {
            MegaEvent.EVENT_STORAGE -> {
                val state = storageStateMapper(event.number.toInt())
                Timber.d("EVENT_STORAGE: $state")
                when (state) {
                    StorageState.Change -> refreshAccountDetail()
                    StorageState.PayWall -> AlertsAndWarnings.showOverDiskQuotaPaywallWarning()

                    else -> sendMyAccountUpdateBroadcast(
                        MyAccountUpdate.Action.STORAGE_STATE_CHANGED,
                        state
                    )
                }
            }

            MegaEvent.EVENT_ACCOUNT_BLOCKED -> {
                Timber.d("EVENT_ACCOUNT_BLOCKED: %s", event.number)
            }

            MegaEvent.EVENT_BUSINESS_STATUS -> sendBroadcastUpdateAccountDetails()
            MegaEvent.EVENT_MISC_FLAGS_READY -> {
                applicationScope.launch {
                    broadcastMiscStateUseCase(MiscLoadedState.FlagsReady)
                    updateDomainName()
                }
                MegaApplication.getInstance().checkEnabledCookies()
                initialiseAdsIfNeeded()
            }

            MegaEvent.EVENT_UPGRADE_SECURITY -> applicationScope.launch {
                setSecurityUpgradeInAppUseCase(true)
            }
        }
    }

    private fun refreshAccountDetail() {
        applicationScope.launch {
            runCatching {
                getAccountDetailsUseCase(forceRefresh = true)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Initialise ads if needed
     */
    private fun initialiseAdsIfNeeded() {
        applicationScope.launch {
            runCatching {
                val isAdsFeatureEnabled =
                    getFeatureFlagValueUseCase(ApiFeatures.GoogleAdsFeatureFlag)
                if (!isAdsFeatureEnabled) return@launch

                if (!isProcessForeground()) {
                    Timber.d("Skipping MobileAds init: process not in foreground")
                    return@launch
                }

                if (!isMobileAdsInitializeCalled.getAndSet(true)) {
                    Timber.d("Initialising MobileAds")
                    MobileAds.initialize(
                        appContext,
                        InitializationConfig.Builder("ca-app-pub-2135147798858967~2157690671")
                            .build()
                    )
                }
            }.onFailure {
                Timber.e(it, "MobileAds initialization failed")
            }
        }
    }

    private suspend fun isProcessForeground(): Boolean = withContext(Dispatchers.Main) {
        ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(Lifecycle.State.STARTED)
    }

    private suspend fun updateDomainName() {
        runCatching { updateDomainNameUseCase() }
            .onFailure { Timber.e(it, "UpdateDomainNameUseCase failed") }
    }

    private fun sendBroadcastUpdateAccountDetails() {
        sendMyAccountUpdateBroadcast(MyAccountUpdate.Action.UPDATE_ACCOUNT_DETAILS, null)
    }

    /**
     * Send broadcast to App Event
     */
    private fun sendMyAccountUpdateBroadcast(
        action: MyAccountUpdate.Action,
        storageState: StorageState?,
    ) =
        applicationScope.launch {
            val data = MyAccountUpdate(
                action = action,
                storageState = storageState
            )
            broadcastMyAccountUpdateUseCase(data)
        }
}
