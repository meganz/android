package mega.privacy.android.app.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import mega.privacy.android.app.data.gateway.FileCompressionGateway
import mega.privacy.android.app.data.gateway.LogWriterGateway
import mega.privacy.android.app.data.gateway.LogbackLogConfigurationGateway
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.gateway.preferences.LoggingPreferencesGateway
import mega.privacy.android.app.logging.ChatLogger
import mega.privacy.android.app.logging.SdkLogger
import mega.privacy.android.app.presentation.logging.tree.LineNumberDebugTree
import mega.privacy.android.app.presentation.logging.tree.LogFlowTree
import mega.privacy.android.domain.entity.logging.LogEntry
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.LoggingRepository
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatLoggerInterface
import nz.mega.sdk.MegaLoggerInterface
import timber.log.Timber
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Timber logging repository
 *
 * @property megaSdkLogger
 * @property megaChatLogger
 * @property sdkLogFlowTree
 * @property chatLogFlowTree
 * @property loggingConfig
 * @property sdkLogger
 * @property chatLogger
 * @property context
 * @property fileCompressionGateway
 * @property megaApiGateway
 * @property ioDispatcher
 * @property loggingPreferencesGateway
 * @property appScope
 */
class TimberLoggingRepository @Inject constructor(
    private val megaSdkLogger: MegaLoggerInterface,
    private val megaChatLogger: MegaChatLoggerInterface,
    @SdkLogger private val sdkLogFlowTree: LogFlowTree,
    @ChatLogger private val chatLogFlowTree: LogFlowTree,
    private val loggingConfig: LogbackLogConfigurationGateway,
    @SdkLogger private val sdkLogger: LogWriterGateway,
    @ChatLogger private val chatLogger: LogWriterGateway,
    @ApplicationContext private val context: Context,
    private val fileCompressionGateway: FileCompressionGateway,
    private val megaApiGateway: MegaApiGateway,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val loggingPreferencesGateway: LoggingPreferencesGateway,
    @ApplicationScope private val appScope: CoroutineScope,
) : LoggingRepository {

    init {
        if (!Timber.forest().contains(sdkLogFlowTree)) {
            Timber.plant(sdkLogFlowTree)
        }
        if (!Timber.forest().contains(chatLogFlowTree)) {
            Timber.plant(chatLogFlowTree)
        }
        MegaChatApiAndroid.setLoggerObject(megaChatLogger)
        MegaApiAndroid.addLoggerObject(megaSdkLogger)
    }

    override fun enableLogAllToConsole() {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
        Timber.plant(LineNumberDebugTree())
    }

    override fun resetSdkLogging() {
        MegaApiAndroid.removeLoggerObject(megaSdkLogger)
        MegaApiAndroid.addLoggerObject(megaSdkLogger)
    }

    override fun getSdkLoggingFlow(): Flow<LogEntry> = sdkLogFlowTree.logFlow.onSubscription {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_MAX)
        loggingConfig.resetLoggingConfiguration()
    }.onCompletion {
        MegaApiAndroid.setLogLevel(MegaApiAndroid.LOG_LEVEL_FATAL)
    }

    override fun getChatLoggingFlow(): Flow<LogEntry> =
        chatLogFlowTree.logFlow.onSubscription {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_MAX)
            loggingConfig.resetLoggingConfiguration()
        }.onCompletion {
            MegaChatApiAndroid.setLogLevel(MegaChatApiAndroid.LOG_LEVEL_ERROR)
        }

    override suspend fun logToSdkFile(logMessage: LogEntry) =
        withContext(ioDispatcher) { sdkLogger.writeLogEntry(logMessage) }

    override suspend fun logToChatFile(logMessage: LogEntry) =
        withContext(ioDispatcher) { chatLogger.writeLogEntry(logMessage) }

    override suspend fun compressLogs(): File = withContext(ioDispatcher) {
        val loggingDirectoryPath = loggingConfig.getLoggingDirectoryPath()
        require(loggingDirectoryPath != null) { "Logging configuration file missing or logging directory not configured" }
        createEmptyFile().apply {
            fileCompressionGateway.zipFolder(
                File(loggingDirectoryPath),
                this
            )
        }
    }

    private fun createEmptyFile() =
        File("${context.cacheDir.path}/${getLogFileName()}").apply {
            if (exists()) delete()
            createNewFile()
        }

    private fun getLogFileName() =
        "${getFormattedDate()}_Android_${megaApiGateway.accountEmail}.zip"

    private fun getFormattedDate() = DateTimeFormatter.ofPattern("dd_MM_yyyy__HH_mm_ss")
        .withZone(ZoneId.from(ZoneOffset.UTC))
        .format(Instant.now())

    override fun isSdkLoggingEnabled(): SharedFlow<Boolean> =
        loggingPreferencesGateway.isLoggingPreferenceEnabled()
            .shareIn(appScope, SharingStarted.WhileSubscribed(), replay = 1)

    override suspend fun setSdkLoggingEnabled(enabled: Boolean) {
        loggingPreferencesGateway.setLoggingEnabledPreference(enabled)
    }

    override fun isChatLoggingEnabled(): Flow<Boolean> =
        loggingPreferencesGateway.isChatLoggingPreferenceEnabled()

    override suspend fun setChatLoggingEnabled(enabled: Boolean) {
        loggingPreferencesGateway.setChatLoggingEnabledPreference(enabled)
    }
}