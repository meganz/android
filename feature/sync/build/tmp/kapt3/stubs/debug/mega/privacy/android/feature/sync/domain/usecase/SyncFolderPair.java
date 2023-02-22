package mega.privacy.android.feature.sync.domain.usecase;

import java.lang.System;

/**
 * Establishes a pair between local and remote directories and starts the syncing process
 */
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00e6\u0080\u0001\u0018\u00002\u00020\u0001J!\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H\u00a6B\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\b\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\t"}, d2 = {"Lmega/privacy/android/feature/sync/domain/usecase/SyncFolderPair;", "", "invoke", "", "localPath", "", "remotePath", "Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "(Ljava/lang/String;Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "sync_debug"})
public abstract interface SyncFolderPair {
    
    @org.jetbrains.annotations.Nullable
    public abstract java.lang.Object invoke(@org.jetbrains.annotations.NotNull
    java.lang.String localPath, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.entity.RemoteFolder remotePath, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> continuation);
}