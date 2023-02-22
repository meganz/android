package mega.privacy.android.feature.sync.ui;

import java.lang.System;

/**
 * @param selectedLocalFolder selected local folder
 * @param selectedMegaFolder selected MEGA folder
 * @param rootMegaRemoteFolders root MEGA remote folders
 * @param isSyncing is syncing in progress
 */
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000f\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B5\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u0012\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\u0014\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u000f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\tH\u00c6\u0003J9\u0010\u0017\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u00052\u000e\b\u0002\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u00072\b\b\u0002\u0010\b\u001a\u00020\tH\u00c6\u0001J\u0013\u0010\u0018\u001a\u00020\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001b\u001a\u00020\u001cH\u00d6\u0001J\t\u0010\u001d\u001a\u00020\u0003H\u00d6\u0001R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\b\u001a\u00020\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\u001e"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncState;", "", "selectedLocalFolder", "", "selectedMegaFolder", "Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "rootMegaRemoteFolders", "", "status", "Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;", "(Ljava/lang/String;Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;Ljava/util/List;Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;)V", "getRootMegaRemoteFolders", "()Ljava/util/List;", "getSelectedLocalFolder", "()Ljava/lang/String;", "getSelectedMegaFolder", "()Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "getStatus", "()Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "", "toString", "sync_debug"})
public final class SyncState {
    @org.jetbrains.annotations.NotNull
    private final java.lang.String selectedLocalFolder = null;
    @org.jetbrains.annotations.Nullable
    private final mega.privacy.android.feature.sync.domain.entity.RemoteFolder selectedMegaFolder = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<mega.privacy.android.feature.sync.domain.entity.RemoteFolder> rootMegaRemoteFolders = null;
    @org.jetbrains.annotations.NotNull
    private final mega.privacy.android.feature.sync.domain.entity.FolderPairState status = null;
    
    /**
     * @param selectedLocalFolder selected local folder
     * @param selectedMegaFolder selected MEGA folder
     * @param rootMegaRemoteFolders root MEGA remote folders
     * @param isSyncing is syncing in progress
     */
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.ui.SyncState copy(@org.jetbrains.annotations.NotNull
    java.lang.String selectedLocalFolder, @org.jetbrains.annotations.Nullable
    mega.privacy.android.feature.sync.domain.entity.RemoteFolder selectedMegaFolder, @org.jetbrains.annotations.NotNull
    java.util.List<mega.privacy.android.feature.sync.domain.entity.RemoteFolder> rootMegaRemoteFolders, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.entity.FolderPairState status) {
        return null;
    }
    
    /**
     * @param selectedLocalFolder selected local folder
     * @param selectedMegaFolder selected MEGA folder
     * @param rootMegaRemoteFolders root MEGA remote folders
     * @param isSyncing is syncing in progress
     */
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    /**
     * @param selectedLocalFolder selected local folder
     * @param selectedMegaFolder selected MEGA folder
     * @param rootMegaRemoteFolders root MEGA remote folders
     * @param isSyncing is syncing in progress
     */
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    /**
     * @param selectedLocalFolder selected local folder
     * @param selectedMegaFolder selected MEGA folder
     * @param rootMegaRemoteFolders root MEGA remote folders
     * @param isSyncing is syncing in progress
     */
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public java.lang.String toString() {
        return null;
    }
    
    public SyncState() {
        super();
    }
    
    public SyncState(@org.jetbrains.annotations.NotNull
    java.lang.String selectedLocalFolder, @org.jetbrains.annotations.Nullable
    mega.privacy.android.feature.sync.domain.entity.RemoteFolder selectedMegaFolder, @org.jetbrains.annotations.NotNull
    java.util.List<mega.privacy.android.feature.sync.domain.entity.RemoteFolder> rootMegaRemoteFolders, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.entity.FolderPairState status) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getSelectedLocalFolder() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final mega.privacy.android.feature.sync.domain.entity.RemoteFolder component2() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final mega.privacy.android.feature.sync.domain.entity.RemoteFolder getSelectedMegaFolder() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<mega.privacy.android.feature.sync.domain.entity.RemoteFolder> component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.util.List<mega.privacy.android.feature.sync.domain.entity.RemoteFolder> getRootMegaRemoteFolders() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.domain.entity.FolderPairState component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.domain.entity.FolderPairState getStatus() {
        return null;
    }
}