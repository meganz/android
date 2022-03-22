package mega.privacy.android.app.fragments.managerFragments.cu

/**
 * This interface will be used for casting to TimelineFragment, AlbumsFragment instance.
 */
interface PhotosTabCallback {

    /**
     * Return a Int value, parent activity will handle the real back press
     *
     * @return This is legacy code. Base on the context in ManagerActivity/onBackPressed.
     * 0 - will performOnBack(), otherwise, will handle by our own logic
     *
     * TODO: Due to lack of fully understanding this context, it should be optimised in next stage. For example, meaningful the magic number.
     */
    fun onBackPressed() : Int

    /**
     * this fun is used for handle titleBar Elevation. will call in checkScrollElevation
     */
    fun checkScroll()
}