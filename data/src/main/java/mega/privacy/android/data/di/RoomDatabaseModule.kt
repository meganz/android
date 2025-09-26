package mega.privacy.android.data.di

import android.content.Context
import android.provider.Settings
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.database.LegacyDatabaseMigration
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.SQLCipherManager
import mega.privacy.android.data.database.chat.CHAT_DATABASE_NAME
import mega.privacy.android.data.database.chat.ChatDatabase
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.ActiveTransferGroupDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.ChatPendingChangesDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.HomeWidgetConfigurationDao
import mega.privacy.android.data.database.dao.LastPageViewedInPdfDao
import mega.privacy.android.data.database.dao.MediaPlaybackInfoDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.PendingTransferDao
import mega.privacy.android.data.database.dao.SyncShownNotificationDao
import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
import mega.privacy.android.data.database.dao.TypedMessageDao
import mega.privacy.android.data.database.dao.UserPausedSyncsDao
import mega.privacy.android.data.database.dao.VideoRecentlyWatchedDao
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import timber.log.Timber
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RoomDatabaseModule {

    @Provides
    @Singleton
    @Named("database_passphrase")
    fun provideDatabasePassphrase(
        sqlCipherManager: SQLCipherManager,
    ): ByteArray? {
        return try {
            sqlCipherManager.getPassphrase()
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate database to secure database")
            null
        }
    }

    @Provides
    @Singleton
    internal fun provideMegaDatabase(
        @ApplicationContext applicationContext: Context,
        legacyDatabaseMigration: LegacyDatabaseMigration,
        sqlCipherManager: SQLCipherManager,
        @Named("database_passphrase") passphrase: ByteArray?,
    ): MegaDatabase {
        return try {
            if (passphrase == null) throw NullPointerException("Passphrase is null")
            sqlCipherManager.migrateToSecureDatabase(MegaDatabaseConstant.DATABASE_NAME, passphrase)
            MegaDatabase.init(
                applicationContext,
                SupportOpenHelperFactory(passphrase, null, false),
                legacyDatabaseMigration
            )
        } catch (e: Exception) {
            sqlCipherManager.destructSecureDatabase(MegaDatabaseConstant.DATABASE_NAME)
            MegaDatabase.init(
                applicationContext,
                FrameworkSQLiteOpenHelperFactory(),
                legacyDatabaseMigration
            )
        }
    }

    @Provides
    @Singleton
    internal fun provideChatDatabase(
        @ApplicationContext applicationContext: Context,
        @Named("database_passphrase") passphrase: ByteArray?,
        sqlCipherManager: SQLCipherManager,
    ): ChatDatabase {
        return try {
            if (passphrase == null) throw NullPointerException("Passphrase is null")
            sqlCipherManager.migrateToSecureDatabase(CHAT_DATABASE_NAME, passphrase)
            ChatDatabase.init(
                applicationContext,
                SupportOpenHelperFactory(passphrase, null, false),
            )
        } catch (e: Exception) {
            sqlCipherManager.destructSecureDatabase(CHAT_DATABASE_NAME)
            ChatDatabase.init(
                applicationContext,
                FrameworkSQLiteOpenHelperFactory(),
            )
        }
    }

    @Provides
    @Singleton
    internal fun provideSupportSQLiteOpenHelper(database: MegaDatabase): SupportSQLiteOpenHelper =
        database.openHelper

    @Provides
    @Singleton
    internal fun provideContactDao(database: MegaDatabase): ContactDao = database.contactDao()

    @Provides
    @Singleton
    internal fun provideCompletedTransferDao(database: MegaDatabase): CompletedTransferDao =
        database.completedTransferDao()

    @Provides
    @Singleton
    internal fun provideActiveTransferDao(database: MegaDatabase): ActiveTransferDao =
        database.activeTransfersDao()

    @Provides
    @Singleton
    internal fun provideActiveTransferGroupDao(database: MegaDatabase): ActiveTransferGroupDao =
        database.activeTransferGroupsDao()

    @Provides
    @Singleton
    internal fun provideBackupDao(database: MegaDatabase): BackupDao =
        database.backupDao()

    @Provides
    @Singleton
    internal fun provideCameraUploadsRecordDao(database: MegaDatabase): CameraUploadsRecordDao =
        database.cameraUploadsRecordDao()

    @Provides
    @Singleton
    internal fun provideChatPendingChangesDao(database: MegaDatabase): ChatPendingChangesDao =
        database.chatPendingChangesDao()

    @Provides
    @Singleton
    @Named("aes_key")
    internal fun provideAesKey(): ByteArray {
        val key = Settings.Secure.ANDROID_ID + "fkvn8 w4y*(NC\$G*(G($*GR*(#)*huio4h389\$G"
        return key.toByteArray().copyOfRange(0, 32)
    }

    @Provides
    @Singleton
    internal fun providePassphraseFile(
        @ApplicationContext context: Context,
    ): File = File(context.filesDir, MegaDatabaseConstant.PASSPHRASE_FILE_NAME)

    @Provides
    @Singleton
    internal fun providePassphraseEncryptedFile(
        @ApplicationContext context: Context,
        masterKey: MasterKey?,
        passphraseFile: File,
    ): EncryptedFile? {
        masterKey ?: return null
        return runCatching {
            EncryptedFile.Builder(
                context,
                passphraseFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
        }.onFailure {
            Timber.e(it)
        }.getOrNull()
    }

    @Provides
    @Singleton
    internal fun provideMasterKey(
        @ApplicationContext context: Context,
    ): MasterKey? {
        return runCatching {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }.onFailure {
            Timber.e(it, "Failed to create MasterKey")
        }.getOrNull()
    }

    @Provides
    @Singleton
    internal fun provideOfflineDao(database: MegaDatabase): OfflineDao =
        database.offlineDao()

    @Provides
    @Singleton
    internal fun provideSyncSolvedIssuesDao(database: MegaDatabase): SyncSolvedIssuesDao =
        database.syncSolvedIssuesDao()

    @Provides
    @Singleton
    internal fun provideUserPausedSyncDao(database: MegaDatabase): UserPausedSyncsDao =
        database.userPausedSyncDao()

    @Provides
    @Singleton
    internal fun provideSyncShownNotificationDao(database: MegaDatabase): SyncShownNotificationDao =
        database.syncShownNotificationDao()

    @Provides
    @Singleton
    internal fun provideTypedMessageRequestDao(chatDatabase: ChatDatabase): TypedMessageDao =
        chatDatabase.typedMessageDao()

    @Provides
    @Singleton
    internal fun provideVideoRecentlyWatchedDao(database: MegaDatabase): VideoRecentlyWatchedDao =
        database.videoRecentlyWatchedDao()

    @Provides
    @Singleton
    internal fun providePendingTransferDao(database: MegaDatabase): PendingTransferDao =
        database.pendingTransferDao()

    @Provides
    @Singleton
    internal fun provideLastPageViewedInPdfDao(database: MegaDatabase): LastPageViewedInPdfDao =
        database.lastPageViewedInPdfDao()

    @Provides
    @Singleton
    internal fun provideMediaPlaybackInfoDao(database: MegaDatabase): MediaPlaybackInfoDao =
        database.mediaPlaybackInfoDao()

    @Provides
    fun provideHomeWidgetConfigurationDao(database: MegaDatabase): HomeWidgetConfigurationDao =
        database.homeWidgetConfigurationDao()
}
