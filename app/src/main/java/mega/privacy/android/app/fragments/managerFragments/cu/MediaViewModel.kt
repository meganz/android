package mega.privacy.android.app.fragments.managerFragments.cu

import android.content.Context
import androidx.hilt.lifecycle.ViewModelInject
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.gallery.ui.GalleryViewModel
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.repo.MegaNodeRepo
import nz.mega.sdk.MegaApiAndroid
import nz.mega.sdk.MegaNode

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