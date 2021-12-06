package mega.privacy.android.app.fragments.managerFragments.cu

import android.content.Context
import android.os.Environment
import android.util.Pair
import androidx.collection.LongSparseArray
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import io.reactivex.rxjava3.subjects.Subject
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaPreferences
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.constants.SettingsConstants
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler.getClickedCard
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler.monthClicked
import mega.privacy.android.app.fragments.homepage.photos.CardClickHandler.yearClicked
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.data.GalleryItem
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.repo.MegaNodeRepo
import mega.privacy.android.app.utils.Constants.GET_THUMBNAIL_THROTTLE_MS
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.LogUtil.logDebug
import mega.privacy.android.app.utils.PreviewUtils
import mega.privacy.android.app.utils.RxUtil.IGNORE
import mega.privacy.android.app.utils.RxUtil.logErr
import mega.privacy.android.app.utils.ThumbnailUtilsLollipop
import mega.privacy.android.app.utils.Util.fromEpoch
import mega.privacy.android.app.utils.ZoomUtil
import nz.mega.sdk.*
import java.io.File
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.TimeUnit

class MediaViewModel @ViewModelInject constructor(
    @MegaApi private val mMegaApi: MegaApiAndroid,
    private val mRepo: MegaNodeRepo,
    @ApplicationContext private val mAppContext: Context,
    private val mSortOrderManagement: SortOrderManagement
) : GalleryViewModel(mMegaApi, mRepo, mAppContext) {

    /**
     * get real mega nodes from mega api
     *
     * @param n this mega node from folder, it must be not-null
     */
    override fun getRealMegaNodes(n: MegaNode?): List<Pair<Int, MegaNode>> =
        mRepo.getMediaAsPairsByAMegaNode(n!!, mSortOrderManagement.getOrderCloud())


}