package mega.privacy.android.feature.sync.data.repository;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\b\u0000\u0018\u00002\u00020\u0001B\u0007\b\u0007\u00a2\u0006\u0002\u0010\u0002J\u0017\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004H\u0096@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bH\u0016J\u0011\u0010\n\u001a\u00020\u000bH\u0096@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0006J!\u0010\f\u001a\u00020\u000b2\u0006\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u0010H\u0096@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0011\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0012"}, d2 = {"Lmega/privacy/android/feature/sync/data/repository/MockSyncRepository;", "Lmega/privacy/android/feature/sync/domain/repository/SyncRepository;", "()V", "getFolderPairs", "", "Lmega/privacy/android/feature/sync/domain/entity/FolderPair;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeSyncState", "Lkotlinx/coroutines/flow/Flow;", "Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;", "removeFolderPairs", "", "setupFolderPair", "localPath", "", "remoteFolderId", "", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sync_debug"})
public final class MockSyncRepository implements mega.privacy.android.feature.sync.domain.repository.SyncRepository {
    
    @javax.inject.Inject
    public MockSyncRepository() {
        super();
    }
    
    @org.jetbrains.annotations.Nullable
    @java.lang.Override
    public java.lang.Object setupFolderPair(@org.jetbrains.annotations.NotNull
    java.lang.String localPath, long remoteFolderId, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    @java.lang.Override
    public java.lang.Object getFolderPairs(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<mega.privacy.android.feature.sync.domain.entity.FolderPair>> continuation) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    @java.lang.Override
    public java.lang.Object removeFolderPairs(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> continuation) {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public kotlinx.coroutines.flow.Flow<mega.privacy.android.feature.sync.domain.entity.FolderPairState> observeSyncState() {
        return null;
    }
}