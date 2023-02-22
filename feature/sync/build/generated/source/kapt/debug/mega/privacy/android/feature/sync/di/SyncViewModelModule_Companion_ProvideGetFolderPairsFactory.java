package mega.privacy.android.feature.sync.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import mega.privacy.android.feature.sync.domain.repository.SyncRepository;
import mega.privacy.android.feature.sync.domain.usecase.GetFolderPairs;

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
public final class SyncViewModelModule_Companion_ProvideGetFolderPairsFactory implements Factory<GetFolderPairs> {
  private final Provider<SyncRepository> syncRepositoryProvider;

  public SyncViewModelModule_Companion_ProvideGetFolderPairsFactory(
      Provider<SyncRepository> syncRepositoryProvider) {
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public GetFolderPairs get() {
    return provideGetFolderPairs(syncRepositoryProvider.get());
  }

  public static SyncViewModelModule_Companion_ProvideGetFolderPairsFactory create(
      Provider<SyncRepository> syncRepositoryProvider) {
    return new SyncViewModelModule_Companion_ProvideGetFolderPairsFactory(syncRepositoryProvider);
  }

  public static GetFolderPairs provideGetFolderPairs(SyncRepository syncRepository) {
    return Preconditions.checkNotNullFromProvides(SyncViewModelModule.Companion.provideGetFolderPairs(syncRepository));
  }
}
