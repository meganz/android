package mega.privacy.android.data.di

import android.content.Context
import android.provider.Settings
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.dao.CompletedTransferDao
import mega.privacy.android.data.database.dao.ContactDao
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object RoomDatabaseModule {
    @Provides
    @Singleton
    internal fun provideMegaDatabase(@ApplicationContext applicationContext: Context): MegaDatabase =
        Room.databaseBuilder(
            applicationContext,
            MegaDatabase::class.java, MegaDatabaseConstant.DATABASE_NAME
        ).addMigrations(MegaDatabase.MIGRATION_67_68)
            .addMigrations(MegaDatabase.MIGRATION_68_69)
            .build()

    @Provides
    @Singleton
    internal fun provideContactDao(database: MegaDatabase): ContactDao = database.contactDao()

    @Provides
    @Singleton
    internal fun provideCompletedTransferDao(database: MegaDatabase): CompletedTransferDao =
        database.completedTransferDao()

    @Provides
    @Singleton
    @Named("aes_key")
    internal fun provideAesKey(): ByteArray {
        val key = Settings.Secure.ANDROID_ID + "fkvn8 w4y*(NC\$G*(G($*GR*(#)*huio4h389\$G"
        return key.toByteArray().copyOfRange(0, 32)
    }
}
