package mega.privacy.android.feature.sync.ui;

import java.lang.System;

@kotlin.Metadata(mv = {1, 7, 1}, k = 2, d1 = {"\u0000B\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\u001a4\u0010\u0000\u001a\u00020\u00012\b\u0010\u0002\u001a\u0004\u0018\u00010\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00030\u00052\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a\u0018\u0010\b\u001a\u00020\u00012\u0006\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\fH\u0007\u001aN\u0010\r\u001a\u00020\u00012\u0006\u0010\u000e\u001a\u00020\u000f2\f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00010\u00112\f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00010\u00112\f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00010\u00112\u0012\u0010\u0014\u001a\u000e\u0012\u0004\u0012\u00020\u0003\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a\b\u0010\u0015\u001a\u00020\u0001H\u0007\u001a$\u0010\u0016\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00180\u00172\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0018\u0012\u0004\u0012\u00020\u00010\u0007H\u0003\u00a8\u0006\u0019"}, d2 = {"RemoteFoldersDropDownMenu", "", "currentFolder", "Lmega/privacy/android/feature/sync/domain/entity/RemoteFolder;", "folders", "", "onFolderSelected", "Lkotlin/Function1;", "SyncScreen", "syncViewModel", "Lmega/privacy/android/feature/sync/ui/SyncViewModel;", "isDark", "", "SyncView", "state", "Lmega/privacy/android/feature/sync/ui/SyncState;", "syncClicked", "Lkotlin/Function0;", "removeClicked", "chooseLocalFolderClicked", "remoteFolderSelected", "SyncViewPreview", "getFolderPicker", "Landroidx/activity/result/ActivityResultLauncher;", "Landroid/net/Uri;", "sync_debug"})
public final class SyncScreenKt {
    
    /**
     * Composable Sync screen
     */
    @androidx.compose.runtime.Composable
    public static final void SyncScreen(@org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.ui.SyncViewModel syncViewModel, boolean isDark) {
    }
    
    @androidx.compose.runtime.Composable
    private static final androidx.activity.result.ActivityResultLauncher<android.net.Uri> getFolderPicker(kotlin.jvm.functions.Function1<? super android.net.Uri, kotlin.Unit> onFolderSelected) {
        return null;
    }
    
    /**
     * UI of Sync screen
     */
    @androidx.compose.runtime.Composable
    public static final void SyncView(@org.jetbrains.annotations.NotNull
    mega.privacy.android.feature.sync.ui.SyncState state, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> syncClicked, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> removeClicked, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function0<kotlin.Unit> chooseLocalFolderClicked, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function1<? super mega.privacy.android.feature.sync.domain.entity.RemoteFolder, kotlin.Unit> remoteFolderSelected) {
    }
    
    /**
     * Dropdown menu for selecting a remote folder.
     */
    @androidx.compose.runtime.Composable
    public static final void RemoteFoldersDropDownMenu(@org.jetbrains.annotations.Nullable
    mega.privacy.android.feature.sync.domain.entity.RemoteFolder currentFolder, @org.jetbrains.annotations.NotNull
    java.util.List<mega.privacy.android.feature.sync.domain.entity.RemoteFolder> folders, @org.jetbrains.annotations.NotNull
    kotlin.jvm.functions.Function1<? super mega.privacy.android.feature.sync.domain.entity.RemoteFolder, kotlin.Unit> onFolderSelected) {
    }
    
    @androidx.compose.runtime.Composable
    @androidx.compose.ui.tooling.preview.Preview(uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
    @androidx.compose.ui.tooling.preview.Preview
    public static final void SyncViewPreview() {
    }
}