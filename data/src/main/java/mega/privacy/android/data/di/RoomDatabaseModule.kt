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
import mega.privacy.android.data.database.chat.InMemoryChatDatabase
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.ChatHistoryStateDao
import mega.privacy.android.data.database.dao.ChatRoomPreferenceDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
import mega.privacy.android.data.database.dao.TypedMessageDao
import mega.privacy.android.data.database.dao.UserPausedSyncsDao
import net.sqlcipher.database.SupportFactory
import timber.log.Timber
import java.io.File
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RoomDatabaseModule {
    @Provides
    @Singleton
    internal fun provideMegaDatabase(
        @ApplicationContext applicationContext: Context,
        legacyDatabaseMigration: LegacyDatabaseMigration,
        sqlCipherManager: SQLCipherManager,
    ): MegaDatabase {
        return try {
            val passphrase = sqlCipherManager.getPassphrase()
            sqlCipherManager.migrateToSecureDatabase(MegaDatabaseConstant.DATABASE_NAME, passphrase)
            MegaDatabase.init(
                applicationContext,
                SupportFactory(passphrase),
                legacyDatabaseMigration
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to migrate database to secure database")
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
    ): InMemoryChatDatabase {
        return InMemoryChatDatabase.create(applicationContext)
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
    internal fun provideSdTransferDao(database: MegaDatabase): SdTransferDao =
        database.sdTransferDao()

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
    internal fun provideChatRoomPreferenceDao(database: MegaDatabase): ChatRoomPreferenceDao =
        database.chatRoomPreferenceDao()

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
        passphraseFile: File,
    ): EncryptedFile? {
        return runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
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
    internal fun provideTypedMessageRequestDao(chatDatabase: InMemoryChatDatabase): TypedMessageDao =
        chatDatabase.typedMessageDao()

    @Provides
    @Singleton
    internal fun provideChatHistoryStateDao(chatDatabase: InMemoryChatDatabase): ChatHistoryStateDao =
        chatDatabase.chatHistoryStateDao()
}
