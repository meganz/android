package mega.privacy.android.data.di

import android.content.Context
import android.provider.Settings
import androidx.room.Room
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
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
import mega.privacy.android.data.database.MegaOpenHelperFactor
import mega.privacy.android.data.database.SQLCipherManager
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.dao.SyncRecordDao
import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
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
        val passphrase = sqlCipherManager.getPassphrase()
        val isMigrateSuccess = runCatching {
            sqlCipherManager.migrateToSecureDatabase(MegaDatabaseConstant.DATABASE_NAME, passphrase)
        }.onFailure {
            Timber.e(it)
        }.isSuccess
        val factory = if (isMigrateSuccess) {
            SupportFactory(passphrase)
        } else {
            FrameworkSQLiteOpenHelperFactory()
        }
        return Room.databaseBuilder(
            applicationContext,
            MegaDatabase::class.java, MegaDatabaseConstant.DATABASE_NAME
        ).fallbackToDestructiveMigrationFrom(
            *(1..66).toList().toIntArray() // allow destructive migration for version 1 to 66
        ).addMigrations(*MegaDatabase.MIGRATIONS)
            .openHelperFactory(
                MegaOpenHelperFactor(
                    factory,
                    legacyDatabaseMigration
                )
            )
            .build()
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
    internal fun provideSyncRecordDao(database: MegaDatabase): SyncRecordDao =
        database.syncRecordDao()

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
    @Named("aes_key")
    internal fun provideAesKey(): ByteArray {
        val key = Settings.Secure.ANDROID_ID + "fkvn8 w4y*(NC\$G*(G($*GR*(#)*huio4h389\$G"
        return key.toByteArray().copyOfRange(0, 32)
    }

    @Provides
    @Singleton
    internal fun providePassphraseFile(
        @ApplicationContext context: Context,
    ): File = File(context.filesDir, "passphrase.bin")

    @Provides
    @Singleton
    internal fun providePassphraseEncryptedFile(
        @ApplicationContext context: Context,
        passphraseFile: File,
    ): EncryptedFile {
        return EncryptedFile.Builder(
            passphraseFile,
            context,
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
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
}
