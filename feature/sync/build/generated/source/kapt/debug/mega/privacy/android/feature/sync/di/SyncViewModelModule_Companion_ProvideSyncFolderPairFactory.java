package mega.privacy.android.feature.sync.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import mega.privacy.android.feature.sync.domain.repository.SyncRepository;
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
public final class SyncViewModelModule_Companion_ProvideSyncFolderPairFactory implements Factory<SyncFolderPair> {
  private final Provider<SyncRepository> syncRepositoryProvider;

  public SyncViewModelModule_Companion_ProvideSyncFolderPairFactory(
      Provider<SyncRepository> syncRepositoryProvider) {
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public SyncFolderPair get() {
    return provideSyncFolderPair(syncRepositoryProvider.get());
  }

  public static SyncViewModelModule_Companion_ProvideSyncFolderPairFactory create(
      Provider<SyncRepository> syncRepositoryProvider) {
    return new SyncViewModelModule_Companion_ProvideSyncFolderPairFactory(syncRepositoryProvider);
  }

  public static SyncFolderPair provideSyncFolderPair(SyncRepository syncRepository) {
    return Preconditions.checkNotNullFromProvides(SyncViewModelModule.Companion.provideSyncFolderPair(syncRepository));
  }
}
