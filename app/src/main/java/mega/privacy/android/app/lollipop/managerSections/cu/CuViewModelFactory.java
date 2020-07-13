package mega.privacy.android.app.lollipop.managerSections.cu;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import mega.privacy.android.app.DatabaseHandler;
import nz.mega.sdk.MegaApiAndroid;

class CuViewModelFactory implements ViewModelProvider.Factory {
  private final MegaApiAndroid megaApi;
  private final DatabaseHandler databaseHandler;

  CuViewModelFactory(MegaApiAndroid megaApi, DatabaseHandler databaseHandler) {
    this.megaApi = megaApi;
    this.databaseHandler = databaseHandler;
  }

  @SuppressWarnings("unchecked")
  @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    if (modelClass.isAssignableFrom(CuViewModel.class)) {
      return (T) new CuViewModel(megaApi, databaseHandler);
    }
    throw new IllegalArgumentException("Unknown ViewModel class");
  }
}
