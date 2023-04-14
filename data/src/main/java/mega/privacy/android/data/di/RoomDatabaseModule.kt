package mega.privacy.android.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.data.database.MegaDatabase
import mega.privacy.android.data.database.MegaDatabaseConstant
import mega.privacy.android.data.database.dao.ContactDao
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
            .build()

    @Provides
    @Singleton
    internal fun provideContactDao(database: MegaDatabase): ContactDao = database.contactDao()
}