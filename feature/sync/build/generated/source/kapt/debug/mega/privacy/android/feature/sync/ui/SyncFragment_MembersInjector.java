package mega.privacy.android.feature.sync.ui;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import mega.privacy.android.domain.usecase.GetThemeMode;

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
public final class SyncFragment_MembersInjector implements MembersInjector<SyncFragment> {
  private final Provider<GetThemeMode> getThemeModeProvider;

  public SyncFragment_MembersInjector(Provider<GetThemeMode> getThemeModeProvider) {
    this.getThemeModeProvider = getThemeModeProvider;
  }

  public static MembersInjector<SyncFragment> create(Provider<GetThemeMode> getThemeModeProvider) {
    return new SyncFragment_MembersInjector(getThemeModeProvider);
  }

  @Override
  public void injectMembers(SyncFragment instance) {
    injectGetThemeMode(instance, getThemeModeProvider.get());
  }

  @InjectedFieldSignature("mega.privacy.android.feature.sync.ui.SyncFragment.getThemeMode")
  public static void injectGetThemeMode(SyncFragment instance, GetThemeMode getThemeMode) {
    instance.getThemeMode = getThemeMode;
  }
}
