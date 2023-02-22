package mega.privacy.android.feature.sync.ui;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairs;
import mega.privacy.android.feature.sync.domain.usecase.GetRemoteFolders;
import mega.privacy.android.feature.sync.domain.usecase.ObserveSyncState;
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairs;
import mega.privacy.android.feature.sync.domain.usecase.SyncFolderPair;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes"
})
public final class SyncViewModel_Factory implements Factory<SyncViewModel> {
  private final Provider<GetRemoteFolders> getRemoteFoldersProvider;

  private final Provider<SyncFolderPair> syncFolderPairProvider;

  private final Provider<GetFolderPairs> getFolderPairsProvider;

  private final Provider<RemoveFolderPairs> removeFolderPairsProvider;

  private final Provider<ObserveSyncState> observeSyncStateProvider;

  public SyncViewModel_Factory(Provider<GetRemoteFolders> getRemoteFoldersProvider,
      Provider<SyncFolderPair> syncFolderPairProvider,
      Provider<GetFolderPairs> getFolderPairsProvider,
      Provider<RemoveFolderPairs> removeFolderPairsProvider,
      Provider<ObserveSyncState> observeSyncStateProvider) {
    this.getRemoteFoldersProvider = getRemoteFoldersProvider;
    this.syncFolderPairProvider = syncFolderPairProvider;
    this.getFolderPairsProvider = getFolderPairsProvider;
    this.removeFolderPairsProvider = removeFolderPairsProvider;
    this.observeSyncStateProvider = observeSyncStateProvider;
  }

  @Override
  public SyncViewModel get() {
    return newInstance(getRemoteFoldersProvider.get(), syncFolderPairProvider.get(), getFolderPairsProvider.get(), removeFolderPairsProvider.get(), observeSyncStateProvider.get());
  }

  public static SyncViewModel_Factory create(Provider<GetRemoteFolders> getRemoteFoldersProvider,
      Provider<SyncFolderPair> syncFolderPairProvider,
      Provider<GetFolderPairs> getFolderPairsProvider,
      Provider<RemoveFolderPairs> removeFolderPairsProvider,
      Provider<ObserveSyncState> observeSyncStateProvider) {
    return new SyncViewModel_Factory(getRemoteFoldersProvider, syncFolderPairProvider, getFolderPairsProvider, removeFolderPairsProvider, observeSyncStateProvider);
  }

  public static SyncViewModel newInstance(GetRemoteFolders getRemoteFolders,
      SyncFolderPair syncFolderPair, GetFolderPairs getFolderPairs,
      RemoveFolderPairs removeFolderPairs, ObserveSyncState observeSyncState) {
    return new SyncViewModel(getRemoteFolders, syncFolderPair, getFolderPairs, removeFolderPairs, observeSyncState);
  }
}
