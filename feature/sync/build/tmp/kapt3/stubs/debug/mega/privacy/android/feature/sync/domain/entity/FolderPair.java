package mega.privacy.android.feature.sync.domain.entity;

import java.lang.System;

/**
 * Entity representing a folder pair
 * @property id - id of the folder pair
 * @property pairName - name of the pair
 * @property localFolderPath - path to the local folder
 * @property remoteFolder - remote folder location
 * @property state - state of the sync of the pair
 */
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0011\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B-\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0005\u0012\u0006\u0010\u0007\u001a\u00020\b\u0012\u0006\u0010\t\u001a\u00020\n\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0018\u001a\u00020\bH\u00c6\u0003J\t\u0010\u0019\u001a\u00020\nH\u00c6\u0003J;\u0010\u001a\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\nH\u00c6\u0001J\u0013\u0010\u001b\u001a\u00020\u001c2\b\u0010\u001d\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001e\u001a\u00020\u001fH\u00d6\u0001J\t\u0010 \u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0006\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000fR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006!"}, d2 = {"Lmega/privacy/android/feature/sync/domain/entity/FolderPair;", "", "id", "", "pairName", "", "localFolderPath", "remoteFolder", "Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "state", "Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;", "(JLjava/lang/String;Ljava/lang/String;Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;)V", "getId", "()J", "getLocalFolderPath", "()Ljava/lang/String;", "getPairName", "getRemoteFolder", "()Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "getState", "()Lmega/privacy/android/feature/sync/domain/entity/FolderPairState;", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "", "toString", "sync_debug"})
public final class FolderPair {
    private final long id = 0L;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String pairName = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String localFolderPath = null;
    @org.jetbrains.annotations.NotNull
    private final mega.privacy.android.feature.sync.domain.entity.RemoteFolder remoteFolder = null;
    @org.jetbrains.annotations.NotNull
    private final mega.privacy.android.feature.sync.domain.entity.FolderPairState state = null;
    
    /**
     * Entity representing a folder pair
     * @property id - id of the folder pair
     * @property pairName - name of the pair
     * @property localFolderPath - path to the local folder
     * @property remoteFolder - remote folder location
     * @property state - state of the sync of the pair
     */
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.domain.entity.FolderPair copy(long id, @org.jetbrains.annotations.NotNull
    java.lang.String pairName, @org.jetbrains.annotations.NotNull
    java.lang.String localFolderPath, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.entity.RemoteFolder remoteFolder, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.entity.FolderPairState state) {
        return null;
    }
    
    /**
     * Entity representing a folder pair
     * @property id - id of the folder pair
     * @property pairName - name of the pair
     * @property localFolderPath - path to the local folder
     * @property remoteFolder - remote folder location
     * @property state - state of the sync of the pair
     */
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    /**
     * Entity representing a folder pair
     * @property id - id of the folder pair
     * @property pairName - name of the pair
     * @property localFolderPath - path to the local folder
     * @property remoteFolder - remote folder location
     * @property state - state of the sync of the pair
     */
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    /**
     * Entity representing a folder pair
     * @property id - id of the folder pair
     * @property pairName - name of the pair
     * @property localFolderPath - path to the local folder
     * @property remoteFolder - remote folder location
     * @property state - state of the sync of the pair
     */
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public java.lang.String toString() {
        return null;
    }
    
    public FolderPair(long id, @org.jetbrains.annotations.NotNull
    java.lang.String pairName, @org.jetbrains.annotations.NotNull
    java.lang.String localFolderPath, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.entity.RemoteFolder remoteFolder, @org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.domain.entity.FolderPairState state) {
        super();
    }
    
    public final long component1() {
        return 0L;
    }
    
    public final long getId() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getPairName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getLocalFolderPath() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.domain.entity.RemoteFolder component4() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.domain.entity.RemoteFolder getRemoteFolder() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.domain.entity.FolderPairState component5() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.feature.sync.domain.entity.FolderPairState getState() {
        return null;
    }
}