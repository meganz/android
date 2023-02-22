package mega.privacy.android.feature.sync.domain.repository;

import java.lang.System;

/**
 * Repository for syncing folder pairs
 */
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0002\bf\u0018\u00002\u00020\u0001J\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00a6@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0005J\u000e\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007H&J\u0011\u0010\t\u001a\u00020\nH\u00a6@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0005J!\u0010\u000b\u001a\u00020\n2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000fH\u00a6@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0010\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0011"}, d2 = {"Lmega/privacy/android/feature/sync/domain/repository/SyncRepository;", "", "getFolderPairs", "", "Lmega/privacy/android/feature/sync/domain/entity/FolderPair;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "observeSyncState", "Lkotlinx/coroutines/flow/Flow;", "Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;", "removeFolderPairs", "", "setupFolderPair", "localPath", "", "remoteFolderId", "", "(Ljava/lang/String;JLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sync_debug"})
public abstract interface SyncRepository {
    
    /**
     * Establishes a pair between local and remote directories and starts the syncing process
     */
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object setupFolderPair(@org.jetbrains.annotations.NotNull
    java.lang.String localPath, long remoteFolderId, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> continuation);
    
    /**
     * Returns all setup folder pairs.
     */
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object getFolderPairs(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<mega.privacy.android.feature.sync.domain.entity.FolderPair>> continuation);
    
    /**
     * Removes all folder pairs
     */
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object removeFolderPairs(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> continuation);
    
    /**
     * Subscribes to status updates of a sync with the given syncId
     */
    @org.jetbrains.annotations.NotNull
    public abstract kotlinx.coroutines.flow.Flow<mega.privacy.android.feature.sync.domain.entity.FolderPairState> observeSyncState();
}