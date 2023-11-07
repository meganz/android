package mega.privacy.android.data.di

import android.content.Context
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.Room
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
import mega.privacy.android.data.database.dao.ActiveTransferDao
import mega.privacy.android.data.database.dao.BackupDao
import mega.privacy.android.data.database.dao.CameraUploadsRecordDao
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import mega.privacy.android.data.database.dao.OfflineDao
import mega.privacy.android.data.database.dao.SdTransferDao
import mega.privacy.android.data.database.dao.SyncRecordDao
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import mega.privacy.android.data.database.dao.SyncSolvedIssuesDao
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
    ): MegaDatabase =
        Room.databaseBuilder(
            applicationContext,
            MegaDatabase::class.java, MegaDatabaseConstant.DATABASE_NAME
        ).fallbackToDestructiveMigrationFrom(
            *(1..66).toList().toIntArray() // allow destructive migration for version 1 to 66
        ).addMigrations(*MegaDatabase.MIGRATIONS)
            .openHelperFactory(
                MegaOpenHelperFactor(
                    FrameworkSQLiteOpenHelperFactory(),
                    legacyDatabaseMigration
                )
            )
            .build()

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
    @Named("new_aes_key")
    internal fun provideNewAesKey(): SecretKey {
        val androidKeyStore = "AndroidKeyStore"
        val keyName = "new_aes_key"
        val keystore = KeyStore.getInstance(androidKeyStore)
        keystore.load(null)
        // only generate key one times
        if (!keystore.containsAlias(keyName)) {
            KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                androidKeyStore
            ).apply {
                init(
                    KeyGenParameterSpec.Builder(
                        keyName,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(false)
                        .build()
                )

                generateKey()
            }
        }
        return keystore.getKey(keyName, null) as SecretKey
    }

    @Provides
    @Singleton
    internal fun provideOfflineDao(database: MegaDatabase): OfflineDao =
        database.offlineDao()

    @Provides
    @Singleton
    internal fun provideSyncSolvedIssuesDao(database: MegaDatabase): SyncSolvedIssuesDao =
        database.syncSolvedIssuesDao()
}
