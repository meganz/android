package mega.privacy.android.app.audioplayer

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.arch.BaseRxViewModel

class AudioPlayerViewModel @ViewModelInject constructor(
    @ApplicationContext private val context: Context,
) : BaseRxViewModel() {

}
