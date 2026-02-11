package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import mega.privacy.android.domain.entity.uri.UriPath

@HiltViewModel(assistedFactory = ShareToMegaViewModel.Factory::class)
class ShareToMegaViewModel @AssistedInject constructor(
    @Assisted val args: Args,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: Args): ShareToMegaViewModel
    }

    data class Args(
        val shareUris: List<UriPath>?,
    )
}