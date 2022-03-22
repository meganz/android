package mega.privacy.android.app.logging

import android.content.Context
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.usecase.*
import mega.privacy.android.app.utils.Util
import timber.log.Timber
import javax.inject.Inject

class LegacyLoggingSettingsFacade @Inject constructor(
    private val setSdkLogsEnabled: SetSdkLogsEnabled,
    private val setChatLogsEnabled: SetChatLogsEnabled,
    private val resetSdkLogger: ResetSdkLogger,
    areSdkLogsEnabled: AreSdkLogsEnabled,
    areChatLogsEnabled: AreChatLogsEnabled,
) : LegacyLoggingSettings {

    @OptIn(DelicateCoroutinesApi::class)
    private val sdkLogStatus = areSdkLogsEnabled().stateIn(GlobalScope, SharingStarted.Eagerly, false)
    @OptIn(DelicateCoroutinesApi::class)
    private val chatLogStatus = areChatLogsEnabled().stateIn(GlobalScope, SharingStarted.Eagerly, false)

    @OptIn(DelicateCoroutinesApi::class)
    override fun setStatusLoggerSDK(context: Context, enabled: Boolean) {
        GlobalScope.launch {
            setSdkLogsEnabled(enabled)
        }.invokeOnCompletion {
            if (enabled){
                Timber.i("SDK logs are now enabled - App Version: ${Util.getVersion()}")
                Util.showSnackbar(context, context.getString(R.string.settings_enable_logs))
            }else{
                Timber.i("SDK logs are now disabled - App Version: ${Util.getVersion()}")
                Util.showSnackbar(context, context.getString(R.string.settings_disable_logs))
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun setStatusLoggerKarere(context: Context, enabled: Boolean) {
        GlobalScope.launch {
            setChatLogsEnabled(enabled)
        }.invokeOnCompletion {
            if (enabled){
                Timber.i("Karere logs are now enabled - App Version: ${Util.getVersion()}")
                Util.showSnackbar(context, context.getString(R.string.settings_enable_logs))
            }else{
                Timber.i("Karere logs are now disabled - App Version: ${Util.getVersion()}")
                Util.showSnackbar(context, context.getString(R.string.settings_disable_logs))
            }
        }
    }

    override fun resetLoggerSDK() {
        resetSdkLogger()
    }

    override fun areSDKLogsEnabled(): Boolean {
        return sdkLogStatus.value
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun updateSDKLogs(enabled: Boolean) {
        GlobalScope.launch {
            setSdkLogsEnabled(enabled)
        }
    }

    override fun areKarereLogsEnabled(): Boolean {
        return chatLogStatus.value
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun updateKarereLogs(enabled: Boolean) {
        GlobalScope.launch {
            setChatLogsEnabled(enabled)
        }
    }

}