package mega.privacy.android.app.listeners.global

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.providers.documentprovider.CloudDriveDocumentProvider
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
                    updateCloudDriveDocumentProviderState()
                }
                MegaApplication.getInstance().checkEnabledCookies()
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
     * Enable or disable [CloudDriveDocumentProvider] based on the
     * [ApiFeatures.CloudDriveDocumentProvider] feature flag, then notify SAF so DocumentsUI
     * re-scans roots.
     */
    private suspend fun updateCloudDriveDocumentProviderState() {
        val isEnabled = runCatching {
            getFeatureFlagValueUseCase(ApiFeatures.CloudDriveDocumentProvider)
        }.getOrElse {
            false
        }
        Timber.d("CloudDriveDocumentProvider ff value $isEnabled")
        runCatching {
            val component = ComponentName(appContext, CloudDriveDocumentProvider::class.java)
            val currentState = appContext.packageManager.getComponentEnabledSetting(component)
            val desiredState = if (isEnabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED

            if (currentState != desiredState) {
                appContext.packageManager.setComponentEnabledSetting(
                    component, desiredState, PackageManager.DONT_KILL_APP,
                )
                appContext.contentResolver.notifyChange(
                    DocumentsContract.buildRootsUri(BuildConfig.CLOUD_DRIVE_DOCUMENT_PROVIDER_AUTHORITY),
                    null,
                )
            }
        }.onFailure {
            Timber.e(it, "Failed to update CloudDriveDocumentProvider state")
        }
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
