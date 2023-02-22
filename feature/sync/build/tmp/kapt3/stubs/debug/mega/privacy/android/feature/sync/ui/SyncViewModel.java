package mega.privacy.android.feature.sync.ui;

import java.lang.System;

/**
 * ViewModel for Sync feature
 */
@dagger.hilt.android.lifecycle.HiltViewModel
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000Z\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B/\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u0012\u0006\u0010\n\u001a\u00020\u000b\u00a2\u0006\u0002\u0010\fJ\b\u0010\u0014\u001a\u00020\u0015H\u0002J\b\u0010\u0016\u001a\u00020\u0015H\u0002J\u0011\u0010\u0017\u001a\u00020\u0018H\u0082@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\u0019J\u000e\u0010\u001a\u001a\u00020\u00152\u0006\u0010\u001b\u001a\u00020\u001cJ\b\u0010\u001d\u001a\u00020\u0015H\u0002J\u0010\u0010\u001e\u001a\u00020\u00152\u0006\u0010\u001f\u001a\u00020 H\u0002R\u0014\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000f0\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u000f0\u0011\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006!"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncViewModel;", "Landroidx/lifecycle/ViewModel;", "getRemoteFolders", "Lmega/privacy/android/feature/sync/domain/usecase/GetRemoteFolders;", "syncFolderPair", "Lmega/privacy/android/feature/sync/domain/usecase/SyncFolderPair;", "getFolderPairs", "Lmega/privacy/android/feature/sync/domain/usecase/GetFolderPairs;", "removeFolderPairs", "Lmega/privacy/android/feature/sync/domain/usecase/RemoveFolderPairs;", "observeSyncState", "Lmega/privacy/android/feature/sync/domain/usecase/ObserveSyncState;", "(Lmega/privacy/android/feature/sync/domain/usecase/GetRemoteFolders;Lmega/privacy/android/feature/sync/domain/usecase/SyncFolderPair;Lmega/privacy/android/feature/sync/domain/usecase/GetFolderPairs;Lmega/privacy/android/feature/sync/domain/usecase/RemoveFolderPairs;Lmega/privacy/android/feature/sync/domain/usecase/ObserveSyncState;)V", "_state", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lmega/privacy/android/feature/sync/ui/SyncState;", "state", "Lkotlinx/coroutines/flow/StateFlow;", "getState", "()Lkotlinx/coroutines/flow/StateFlow;", "fetchAllMegaFolders", "", "fetchFirstFolderPair", "getFirstFolderPair", "Lmega/privacy/android/feature/sync/domain/entity/FolderPair;", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "handleAction", "syncAction", "Lmega/privacy/android/feature/sync/ui/SyncAction;", "observeSyncStatus", "saveSelectedSyncPath", "path", "Landroid/net/Uri;", "sync_debug"})
public final class SyncViewModel extends androidx.lifecycle.ViewModel {
    private final mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders getRemoteFolders = null;
    private final mega.privacy.android.feature.sync.domain.usecase.SyncFolderPair syncFolderPair = null;
    private final mega.privacy.android.feature.sync.domain.usecase.GetFolderPairs getFolderPairs = null;
    private final mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairs removeFolderPairs = null;
    private final mega.privacy.android.feature.sync.domain.usecase.ObserveSyncState observeSyncState = null;
    private final kotlinx.coroutines.flow.MutableStateFlow<mega.privacy.android.feature.sync.ui.SyncState> _state = null;
    
    /**
     * screen state
     */
    @org.jetbrains.annotations.NotNull
    private final kotlinx.coroutines.flow.StateFlow<mega.privacy.android.feature.sync.ui.SyncState> state = null;
    
    @javax.inject.Inject
    public SyncViewModel(@org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders getRemoteFolders, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.usecase.SyncFolderPair syncFolderPair, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.usecase.GetFolderPairs getFolderPairs, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairs removeFolderPairs, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.usecase.ObserveSyncState observeSyncState) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<mega.privacy.android.feature.sync.ui.SyncState> getState() {
        return null;
    }
    
    private final void fetchAllMegaFolders() {
    }
    
    private final void fetchFirstFolderPair() {
    }
    
    private final void observeSyncStatus() {
    }
    
    /**
     * handles actions/events dispatched from UI
     */
    public final void handleAction(@org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.ui.SyncAction syncAction) {
    }
    
    private final java.lang.Object getFirstFolderPair(kotlin.coroutines.Continuation<? super mega.privacy.android.feature.sync.domain.entity.FolderPair> continuation) {
        return null;
    }
    
    /**
     * Saves selected sync path with full path. It also converts relative path to absolute.
     */
    private final void saveSelectedSyncPath(android.net.Uri path) {
    }
}