package mega.privacy.android.feature.sync.data.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class MockSyncRepository_Factory implements Factory<MockSyncRepository> {
  @Override
  public MockSyncRepository get() {
    return newInstance();
  }

  public static MockSyncRepository_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static MockSyncRepository newInstance() {
    return new MockSyncRepository();
  }

  private static final class InstanceHolder {
    private static final MockSyncRepository_Factory INSTANCE = new MockSyncRepository_Factory();
  }
}
