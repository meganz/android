package mega.privacy.android.feature.sync.domain.usecase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import mega.privacy.android.data.repository.MegaNodeRepository;

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
public final class DefaultGetRemoteFolders_Factory implements Factory<DefaultGetRemoteFolders> {
  private final Provider<MegaNodeRepository> megaNodeRepositoryProvider;

  public DefaultGetRemoteFolders_Factory(Provider<MegaNodeRepository> megaNodeRepositoryProvider) {
    this.megaNodeRepositoryProvider = megaNodeRepositoryProvider;
  }

  @Override
  public DefaultGetRemoteFolders get() {
    return newInstance(megaNodeRepositoryProvider.get());
  }

  public static DefaultGetRemoteFolders_Factory create(
      Provider<MegaNodeRepository> megaNodeRepositoryProvider) {
    return new DefaultGetRemoteFolders_Factory(megaNodeRepositoryProvider);
  }

  public static DefaultGetRemoteFolders newInstance(MegaNodeRepository megaNodeRepository) {
    return new DefaultGetRemoteFolders(megaNodeRepository);
  }
}
