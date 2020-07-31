package mega.privacy.android.app.fragments.managerFragments.homepage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaChatApiAndroid

class HomepageViewModelFactory(
  private val megaApi: MegaApiAndroid,
  private val megaChatApi: MegaChatApiAndroid
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(HomePageViewModel::class.java)) {
      return HomePageViewModel(megaApi, megaChatApi) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
