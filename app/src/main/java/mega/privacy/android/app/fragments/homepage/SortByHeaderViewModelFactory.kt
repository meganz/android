package mega.privacy.android.app.fragments.homepage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.IllegalArgumentException

class SortByHeaderViewModelFactory(@ApplicationContext private val context: Context) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SortByHeaderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SortByHeaderViewModel(context) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}