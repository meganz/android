package mega.privacy.android.feature.sync.di;

import java.lang.System;

/**
 * Dagger module for Sync feature
 */
@dagger.hilt.InstallIn(value = {dagger.hilt.android.components.ViewModelComponent.class})
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\ba\u0018\u0000 \u00062\u00020\u0001:\u0001\u0006J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\'\u00a8\u0006\u0007"}, d2 = {"Lmega/privacy/android/feature/sync/di/SyncViewModelModule;", "", "bindGetRemoteFolders", "Lmega/privacy/android/feature/sync/domain/usecase/GetRemoteFolders;", "impl", "Lmega/privacy/android/feature/sync/domain/usecase/DefaultGetRemoteFolders;", "Companion", "sync_debug"})
@dagger.Module
public abstract interface SyncViewModelModule {
    @org.jetbrains.annotations.NotNull
    public static final mega.privacy.android.feature.sync.di.SyncViewModelModule.Companion Companion = null;
    
    @org.jetbrains.annotations.NotNull
    @dagger.Binds
    public abstract mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders bindGetRemoteFolders(@org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.usecase.DefaultGetRemoteFolders impl);
    
    @kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\t\u001a\u00020\n2\u0006\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u000b\u001a\u00020\f2\u0006\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\r"}, d2 = {"Lmega/privacy/android/feature/sync/di/SyncViewModelModule$Companion;", "", "()V", "provideGetFolderPairs", "Lmega/privacy/android/feature/sync/domain/usecase/GetFolderPairs;", "syncRepository", "Lmega/privacy/android/feature/sync/domain/repository/SyncRepository;", "provideObserveSyncState", "Lmega/privacy/android/feature/sync/domain/usecase/ObserveSyncState;", "provideRemoveFolderPairs", "Lmega/privacy/android/feature/sync/domain/usecase/RemoveFolderPairs;", "provideSyncFolderPair", "Lmega/privacy/android/feature/sync/domain/usecase/SyncFolderPair;", "sync_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        @dagger.Provides
        public final mega.privacy.android.feature.sync.domain.usecase.GetFolderPairs provideGetFolderPairs(@org.jetbrains.annotations.NotNull
        mega.privacy.android.feature.sync.domain.repository.SyncRepository syncRepository) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        @dagger.Provides
        public final mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairs provideRemoveFolderPairs(@org.jetbrains.annotations.NotNull
        mega.privacy.android.feature.sync.domain.repository.SyncRepository syncRepository) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        @dagger.Provides
        public final mega.privacy.android.feature.sync.domain.usecase.ObserveSyncState provideObserveSyncState(@org.jetbrains.annotations.NotNull
        mega.privacy.android.feature.sync.domain.repository.SyncRepository syncRepository) {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        @dagger.Provides
        public final mega.privacy.android.feature.sync.domain.usecase.SyncFolderPair provideSyncFolderPair(@org.jetbrains.annotations.NotNull
        mega.privacy.android.feature.sync.domain.repository.SyncRepository syncRepository) {
            return null;
        }
    }
}