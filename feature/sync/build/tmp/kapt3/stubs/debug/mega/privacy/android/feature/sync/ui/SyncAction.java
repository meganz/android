package mega.privacy.android.feature.sync.ui;

import java.lang.System;

/**
 * Action from UI
 */
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\bv\u0018\u00002\u00020\u0001:\u0004\u0002\u0003\u0004\u0005\u0082\u0001\u0004\u0006\u0007\b\t\u00a8\u0006\n"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncAction;", "", "LocalFolderSelected", "RemoteFolderSelected", "RemoveFolderPairClicked", "SyncClicked", "Lmega/privacy/android/feature/sync/ui/SyncAction$LocalFolderSelected;", "Lmega/privacy/android/feature/sync/ui/SyncAction$RemoteFolderSelected;", "Lmega/privacy/android/feature/sync/ui/SyncAction$RemoveFolderPairClicked;", "Lmega/privacy/android/feature/sync/ui/SyncAction$SyncClicked;", "sync_debug"})
public abstract interface SyncAction {
    
    /**
     * @param remoteFolder - selected remote folder
     */
    @kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncAction$RemoteFolderSelected;", "Lmega/privacy/android/feature/sync/ui/SyncAction;", "remoteFolder", "Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "(Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;)V", "getRemoteFolder", "()Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "sync_debug"})
    public static final class RemoteFolderSelected implements mega.privacy.android.feature.sync.ui.SyncAction {
        @org.jetbrains.annotations.NotNull
        private final mega.privacy.android.feature.sync.domain.entity.RemoteFolder remoteFolder = null;
        
        /**
         * @param remoteFolder - selected remote folder
         */
        @org.jetbrains.annotations.NotNull
        public final mega.privacy.android.feature.sync.ui.SyncAction.RemoteFolderSelected copy(@org.jetbrains.annotations.NotNull
        mega.privacy.android.feature.sync.domain.entity.RemoteFolder remoteFolder) {
            return null;
        }
        
        /**
         * @param remoteFolder - selected remote folder
         */
        @java.lang.Override
        public boolean equals(@org.jetbrains.annotations.Nullable
        java.lang.Object other) {
            return false;
        }
        
        /**
         * @param remoteFolder - selected remote folder
         */
        @java.lang.Override
        public int hashCode() {
            return 0;
        }
        
        /**
         * @param remoteFolder - selected remote folder
         */
        @org.jetbrains.annotations.NotNull
        @java.lang.Override
        public java.lang.String toString() {
            return null;
        }
        
        public RemoteFolderSelected(@org.jetbrains.annotations.NotNull
        mega.privacy.android.feature.sync.domain.entity.RemoteFolder remoteFolder) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final mega.privacy.android.feature.sync.domain.entity.RemoteFolder component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final mega.privacy.android.feature.sync.domain.entity.RemoteFolder getRemoteFolder() {
            return null;
        }
    }
    
    /**
     * @param path - selected local folder path
     */
    @kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\b\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\t\u0010\u0007\u001a\u00020\u0003H\u00c6\u0003J\u0013\u0010\b\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u0003H\u00c6\u0001J\u0013\u0010\t\u001a\u00020\n2\b\u0010\u000b\u001a\u0004\u0018\u00010\fH\u00d6\u0003J\t\u0010\r\u001a\u00020\u000eH\u00d6\u0001J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0005\u0010\u0006\u00a8\u0006\u0011"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncAction$LocalFolderSelected;", "Lmega/privacy/android/feature/sync/ui/SyncAction;", "path", "Landroid/net/Uri;", "(Landroid/net/Uri;)V", "getPath", "()Landroid/net/Uri;", "component1", "copy", "equals", "", "other", "", "hashCode", "", "toString", "", "sync_debug"})
    public static final class LocalFolderSelected implements mega.privacy.android.feature.sync.ui.SyncAction {
        @org.jetbrains.annotations.NotNull
        private final android.net.Uri path = null;
        
        /**
         * @param path - selected local folder path
         */
        @org.jetbrains.annotations.NotNull
        public final mega.privacy.android.feature.sync.ui.SyncAction.LocalFolderSelected copy(@org.jetbrains.annotations.NotNull
        android.net.Uri path) {
            return null;
        }
        
        /**
         * @param path - selected local folder path
         */
        @java.lang.Override
        public boolean equals(@org.jetbrains.annotations.Nullable
        java.lang.Object other) {
            return false;
        }
        
        /**
         * @param path - selected local folder path
         */
        @java.lang.Override
        public int hashCode() {
            return 0;
        }
        
        /**
         * @param path - selected local folder path
         */
        @org.jetbrains.annotations.NotNull
        @java.lang.Override
        public java.lang.String toString() {
            return null;
        }
        
        public LocalFolderSelected(@org.jetbrains.annotations.NotNull
        android.net.Uri path) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final android.net.Uri component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull
        public final android.net.Uri getPath() {
            return null;
        }
    }
    
    /**
     * Sync button clicked
     */
    @kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncAction$SyncClicked;", "Lmega/privacy/android/feature/sync/ui/SyncAction;", "()V", "sync_debug"})
    public static final class SyncClicked implements mega.privacy.android.feature.sync.ui.SyncAction {
        @org.jetbrains.annotations.NotNull
        public static final mega.privacy.android.feature.sync.ui.SyncAction.SyncClicked INSTANCE = null;
        
        private SyncClicked() {
            super();
        }
    }
    
    /**
     * Sync button clicked
     */
    @kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002\u00a8\u0006\u0003"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncAction$RemoveFolderPairClicked;", "Lmega/privacy/android/feature/sync/ui/SyncAction;", "()V", "sync_debug"})
    public static final class RemoveFolderPairClicked implements mega.privacy.android.feature.sync.ui.SyncAction {
        @org.jetbrains.annotations.NotNull
        public static final mega.privacy.android.feature.sync.ui.SyncAction.RemoveFolderPairClicked INSTANCE = null;
        
        private RemoveFolderPairClicked() {
            super();
        }
    }
}