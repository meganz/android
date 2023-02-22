package mega.privacy.android.feature.sync.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import mega.privacy.android.feature.sync.domain.repository.SyncRepository;
import mega.privacy.android.feature.sync.domain.usecase.RemoveFolderPairs;

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
public final class SyncViewModelModule_Companion_ProvideRemoveFolderPairsFactory implements Factory<RemoveFolderPairs> {
  private final Provider<SyncRepository> syncRepositoryProvider;

  public SyncViewModelModule_Companion_ProvideRemoveFolderPairsFactory(
      Provider<SyncRepository> syncRepositoryProvider) {
    this.syncRepositoryProvider = syncRepositoryProvider;
  }

  @Override
  public RemoveFolderPairs get() {
    return provideRemoveFolderPairs(syncRepositoryProvider.get());
  }

  public static SyncViewModelModule_Companion_ProvideRemoveFolderPairsFactory create(
      Provider<SyncRepository> syncRepositoryProvider) {
    return new SyncViewModelModule_Companion_ProvideRemoveFolderPairsFactory(syncRepositoryProvider);
  }

  public static RemoveFolderPairs provideRemoveFolderPairs(SyncRepository syncRepository) {
    return Preconditions.checkNotNullFromProvides(SyncViewModelModule.Companion.provideRemoveFolderPairs(syncRepository));
  }
}
