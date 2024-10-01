package mega.privacy.android.feature.sync.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.feature.sync.data.gateway.SyncDebrisGateway
import mega.privacy.android.feature.sync.data.gateway.SyncDebrisGatewayImpl
import mega.privacy.android.feature.sync.data.gateway.SyncGateway
import mega.privacy.android.feature.sync.data.gateway.SyncGatewayImpl
import mega.privacy.android.feature.sync.data.gateway.SyncPreferencesDatastore
import mega.privacy.android.feature.sync.data.gateway.SyncPreferencesDatastoreImpl
import mega.privacy.android.feature.sync.data.gateway.SyncPromotionDataStore
import mega.privacy.android.feature.sync.data.gateway.SyncPromotionDataStoreImpl
import mega.privacy.android.feature.sync.data.gateway.SyncSolvedIssuesGateway
import mega.privacy.android.feature.sync.data.gateway.SyncSolvedIssuesGatewayImpl
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGateway
import mega.privacy.android.feature.sync.data.gateway.SyncStatsCacheGatewayImpl
import mega.privacy.android.feature.sync.data.gateway.SyncWorkManagerGateway
import mega.privacy.android.feature.sync.data.gateway.SyncWorkManagerGatewayImpl
import mega.privacy.android.feature.sync.data.gateway.UserPausedSyncGateway
import mega.privacy.android.feature.sync.data.gateway.UserPausedSyncGatewayImpl
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGateway
import mega.privacy.android.feature.sync.data.gateway.notification.SyncNotificationGatewayImpl
import mega.privacy.android.feature.sync.data.gateway.syncPrefsDataStore
import mega.privacy.android.feature.sync.data.gateway.syncPrefsDataStoreName
import mega.privacy.android.feature.sync.data.gateway.syncPromotionDataStore
import mega.privacy.android.feature.sync.data.gateway.syncPromotionDataStoreName
import mega.privacy.android.feature.sync.data.repository.SyncDebrisRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncNewFolderParamsRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncNotificationRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncPreferencesRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncPromotionPreferencesRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncRepositoryImpl
import mega.privacy.android.feature.sync.data.repository.SyncSolvedIssuesRepositoryImpl
import mega.privacy.android.feature.sync.domain.repository.SyncDebrisRepository
import mega.privacy.android.feature.sync.domain.repository.SyncNewFolderParamsRepository
import mega.privacy.android.feature.sync.domain.repository.SyncNotificationRepository
import mega.privacy.android.feature.sync.domain.repository.SyncPreferencesRepository
import mega.privacy.android.feature.sync.domain.repository.SyncPromotionPreferencesRepository
import mega.privacy.android.feature.sync.domain.repository.SyncRepository
import mega.privacy.android.feature.sync.domain.repository.SyncSolvedIssuesRepository
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SyncDataModule {

    @Binds
    @Singleton
    fun bindSyncNotificationRepository(implementation: SyncNotificationRepositoryImpl): SyncNotificationRepository

    @Binds
    @Singleton
    fun bindSyncNotificationGateway(implementation: SyncNotificationGatewayImpl): SyncNotificationGateway

    @Binds
    @Singleton
    fun bindSyncNewFolderParamsRepository(implementation: SyncNewFolderParamsRepositoryImpl): SyncNewFolderParamsRepository

    @Binds
    @Singleton
    fun bindSyncRepository(implementation: SyncRepositoryImpl): SyncRepository

    @Binds
    @Singleton
    fun bindSyncPreferencesRepository(implementation: SyncPreferencesRepositoryImpl): SyncPreferencesRepository

    @Binds
    @Singleton
    fun bindSyncPromotionPreferencesRepository(implementation: SyncPromotionPreferencesRepositoryImpl): SyncPromotionPreferencesRepository

    @Binds
    @Singleton
    fun bindSyncSolvedIssuesRepository(implementation: SyncSolvedIssuesRepositoryImpl): SyncSolvedIssuesRepository

    @Binds
    @Singleton
    fun bindSyncDebrisRepository(implementation: SyncDebrisRepositoryImpl): SyncDebrisRepository

    @Binds
    @Singleton
    fun bindSyncGateway(implementation: SyncGatewayImpl): SyncGateway

    @Binds
    @Singleton
    fun bindSyncPrefsDatastore(implementation: SyncPreferencesDatastoreImpl): SyncPreferencesDatastore

    @Binds
    @Singleton
    fun bindSyncPromotionDataStore(implementation: SyncPromotionDataStoreImpl): SyncPromotionDataStore

    @Binds
    @Singleton
    fun bindSyncStatsCacheGateway(implementation: SyncStatsCacheGatewayImpl): SyncStatsCacheGateway

    @Binds
    @Singleton
    fun bindSyncSolvedIssuesGateway(implementation: SyncSolvedIssuesGatewayImpl): SyncSolvedIssuesGateway

    @Binds
    @Singleton
    fun bindUserPausedSyncGateway(implementation: UserPausedSyncGatewayImpl): UserPausedSyncGateway

    @Binds
    @Singleton
    fun bindSyncDebrisGateway(implementation: SyncDebrisGatewayImpl): SyncDebrisGateway

    @Binds
    @Singleton
    fun bindSyncWorkManagerGateway(implementation: SyncWorkManagerGatewayImpl): SyncWorkManagerGateway

    companion object {
        @Provides
        @Named(syncPrefsDataStoreName)
        @Singleton
        fun provideSyncPrefsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.syncPrefsDataStore

        @Provides
        @Named(syncPromotionDataStoreName)
        @Singleton
        fun provideSyncPromotionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            context.syncPromotionDataStore

        @Provides
        fun provideGson(): Gson =
            Gson()
    }
}
