package mega.privacy.android.feature.sync.di;

import java.lang.System;

@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\ba\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\'\u00a8\u0006\u0006"}, d2 = {"Lmega/privacy/android/feature/sync/di/SyncDataModule;", "", "bindSyncRepository", "Lmega/privacy/android/feature/sync/domain/repository/SyncRepository;", "implementation", "Lmega/privacy/android/feature/sync/data/repository/MockSyncRepository;", "sync_debug"})
@dagger.Module
public abstract interface SyncDataModule {
    
    @org.jetbrains.annotations.NotNull
    @javax.inject.Singleton
    @dagger.Binds
    public abstract mega.privacy.android.feature.sync.domain.repository.SyncRepository bindSyncRepository(@org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.data.repository.MockSyncRepository implementation);
}