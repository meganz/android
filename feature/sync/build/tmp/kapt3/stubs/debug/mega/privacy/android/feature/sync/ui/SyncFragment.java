package mega.privacy.android.feature.sync.ui;

import java.lang.System;

/**
 * Screen for syncing local folder with MEGA
 */
@kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0007\u0018\u0000 \u001a2\u00020\u0001:\u0001\u001aB\u0005\u00a2\u0006\u0002\u0010\u0002J$\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u00122\b\u0010\u0013\u001a\u0004\u0018\u00010\u00142\b\u0010\u0015\u001a\u0004\u0018\u00010\u0016H\u0016J\f\u0010\u0017\u001a\u00020\u0018*\u00020\u0019H\u0007R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\bR\u001b\u0010\t\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u001b"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncFragment;", "Landroidx/fragment/app/Fragment;", "()V", "getThemeMode", "Lmega/privacy/android/domain/usecase/GetThemeMode;", "getGetThemeMode", "()Lmega/privacy/android/domain/usecase/GetThemeMode;", "setGetThemeMode", "(Lmega/privacy/android/domain/usecase/GetThemeMode;)V", "syncViewModel", "Lmega/privacy/android/feature/sync/ui/SyncViewModel;", "getSyncViewModel", "()Lmega/privacy/android/feature/sync/ui/SyncViewModel;", "syncViewModel$delegate", "Lkotlin/Lazy;", "onCreateView", "Landroid/view/View;", "inflater", "Landroid/view/LayoutInflater;", "container", "Landroid/view/ViewGroup;", "savedInstanceState", "Landroid/os/Bundle;", "isDarkMode", "", "Lmega/privacy/android/domain/entity/ThemeMode;", "Companion", "sync_debug"})
@dagger.hilt.android.AndroidEntryPoint
public final class SyncFragment extends androidx.fragment.app.Fragment {
    @org.jetbrains.annotations.NotNull
    public static final mega.privacy.android.feature.sync.ui.SyncFragment.Companion Companion = null;
    
    /**
     * Get Theme Mode
     */
    @javax.inject.Inject
    public mega.privacy.android.domain.usecase.GetThemeMode getThemeMode;
    private final kotlin.Lazy syncViewModel$delegate = null;
    
    public SyncFragment() {
        super();
    }
    
    /**
     * Returns the instance of SyncFragment
     */
    @org.jetbrains.annotations.NotNull
    @kotlin.jvm.JvmStatic
    public static final mega.privacy.android.feature.sync.ui.SyncFragment newInstance() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final mega.privacy.android.domain.usecase.GetThemeMode getGetThemeMode() {
        return null;
    }
    
    public final void setGetThemeMode(@org.jetbrains.annotations.NotNull
    mega.privacy.android.domain.usecase.GetThemeMode p0) {
    }
    
    private final mega.privacy.android.feature.sync.ui.SyncViewModel getSyncViewModel() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public android.view.View onCreateView(@org.jetbrains.annotations.NotNull
    android.view.LayoutInflater inflater, @org.jetbrains.annotations.Nullable
    android.view.ViewGroup container, @org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
        return null;
    }
    
    /**
     * Is current theme mode a dark theme
     */
    @androidx.compose.runtime.Composable
    public final boolean isDarkMode(@org.jetbrains.annotations.NotNull
    mega.privacy.android.domain.entity.ThemeMode $this$isDarkMode) {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 7, 1}, k = 1, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0003\u001a\u00020\u0004H\u0007\u00a8\u0006\u0005"}, d2 = {"Lmega/privacy/android/feature/sync/ui/SyncFragment$Companion;", "", "()V", "newInstance", "Lmega/privacy/android/feature/sync/ui/SyncFragment;", "sync_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        /**
         * Returns the instance of SyncFragment
         */
        @org.jetbrains.annotations.NotNull
        @kotlin.jvm.JvmStatic
        public final mega.privacy.android.feature.sync.ui.SyncFragment newInstance() {
            return null;
        }
    }
}