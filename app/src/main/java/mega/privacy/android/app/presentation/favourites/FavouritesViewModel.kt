package mega.privacy.android.app.presentation.favourites

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.GetAllFavorites
import mega.privacy.android.app.presentation.extensions.toFavourite
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.facade.MegaUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.ClickEventState
import mega.privacy.android.app.presentation.favourites.model.Favourite
import mega.privacy.android.app.presentation.favourites.model.FavouriteFile
import mega.privacy.android.app.presentation.favourites.model.FavouriteLoadState
import mega.privacy.android.app.usecase.exception.MegaException
import nz.mega.sdk.MegaNode
import javax.inject.Inject

/**
 * The view model regarding favourites
 * @param context Context
 * @param getAllFavorites DefaultGetFavorites
 * @param stringUtilWrapper StringUtilWrapper
 * @param megaUtilWrapper MegaUtilWrapper
 */
@HiltViewModel
class FavouritesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getAllFavorites: GetAllFavorites,
    private val stringUtilWrapper: StringUtilWrapper,
    private val megaUtilWrapper: MegaUtilWrapper
) :
    ViewModel() {
    private val _favouritesState =
        MutableStateFlow<FavouriteLoadState>(FavouriteLoadState.Loading)
    val favouritesState = _favouritesState.asStateFlow()

    private val _clickEventState = MutableSharedFlow<ClickEventState>(replay = 0)
    val clickEventState = _clickEventState.asSharedFlow()

    private var currentNodeJob : Job? = null

    init {
        getAllFavourites()
    }

    /**
     * Get all favourites
     */
    private fun getAllFavourites() {
        _favouritesState.update {
            FavouriteLoadState.Loading
        }
        // Cancel the previous job avoid to repeatedly observed
        currentNodeJob?.cancel()
        currentNodeJob = viewModelScope.launch {
            runCatching {
                getAllFavorites()
            }.onSuccess { flow ->
                flow.collect { list ->
                    _favouritesState.update {
                        when {
                            list.isNullOrEmpty() -> {
                                FavouriteLoadState.Empty
                            }
                            else -> {
                                FavouriteLoadState.Success(list.map { favouriteInfo ->
                                    favouriteInfo.toFavourite({ node: MegaNode ->
                                        megaUtilWrapper.availableOffline(
                                            context,
                                            node
                                        )
                                    }, stringUtilWrapper)
                                })
                            }

                        }
                    }
                }
            }.onFailure { exception ->
                if (exception is MegaException) {
                    _favouritesState.update {
                        FavouriteLoadState.Error(exception)
                    }
                }
            }
        }
    }

    /**
     * Open file
     * @param favourite Favourite
     */
    fun openFile(favourite: Favourite) {
        viewModelScope.launch {
            _clickEventState.emit(
                if (favourite.isFolder) {
                    ClickEventState.OpenFolder(favourite.handle)
                } else {
                    ClickEventState.OpenFile(favourite as FavouriteFile)
                }
            )
        }
    }

    /**
     * Three dots clicked
     * @param favourite Favourite
     */
    fun threeDotsClicked(favourite: Favourite) {
        viewModelScope.launch {
            _clickEventState.emit(
                if (megaUtilWrapper.isOnline(context)) {
                    ClickEventState.OpenBottomSheetFragment(favourite)
                } else {
                    ClickEventState.Offline
                }
            )
        }
    }
}