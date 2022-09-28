package mega.privacy.android.app.data.repository

import android.app.ActivityManager
import android.app.Application
import android.app.ApplicationExitInfo
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
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
import mega.privacy.android.app.protobuf.TombstoneProtos.Tombstone
import mega.privacy.android.app.utils.Util
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
import java.io.IOException
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
    private val application: Application,
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

    override suspend fun startUpLogging() {
        withContext(ioDispatcher) {
            Util.checkAppUpgrade()
            checkMegaStandbyBucket()
            getTombstoneInfo()
        }
    }

    /**
     * Get the current standby bucket of the app.
     * The system determines the standby state of the app based on app usage patterns.
     *
     * @return the current standby bucket of the app：
     * STANDBY_BUCKET_ACTIVE,
     * STANDBY_BUCKET_WORKING_SET,
     * STANDBY_BUCKET_FREQUENT,
     * STANDBY_BUCKET_RARE,
     * STANDBY_BUCKET_RESTRICTED,
     * STANDBY_BUCKET_NEVER
     */
    private fun checkMegaStandbyBucket() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val usageStatsManager =
                application.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return
            val standbyBucket = usageStatsManager.appStandbyBucket
            Timber.d("getAppStandbyBucket(): %s", standbyBucket)
        }
    }

    /**
     * Get the tombstone information.
     */
    private fun getTombstoneInfo() {
        Timber.d("getTombstoneInfo")
        val activityManager =
            application.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val exitReasons = activityManager.getHistoricalProcessExitReasons(
                /* packageName = */null,
                /* pid = */0,
                /* maxNum = */3
            )
            exitReasons.forEach { exitReason ->
                if (exitReason.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                    // Get the tombstone input stream.
                    try {
                        exitReason.traceInputStream?.use {
                            // The tombstone parser built with protoc uses the tombstone schema, then parses the trace.
                            val tombstone =
                                Tombstone.parseFrom(it)
                            Timber.e("Tombstone Info%s", tombstone.toString())
                        }
                    } catch (e: IOException) {
                        Timber.e(e)
                    }
                }
            }
        }
    }
}