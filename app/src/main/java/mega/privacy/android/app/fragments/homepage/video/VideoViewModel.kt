/*
 * feature 
 * mega.privacy.android.app.fragments.homepage.video 
 * 
 * Created on 11/09/20 11:31 AM.
 */
package mega.privacy.android.app.fragments.homepage.video

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.fragments.homepage.TypedFilesRepository

class VideoViewModel @ViewModelInject constructor(
    private val repository: TypedFilesRepository
) : ViewModel() {

}

