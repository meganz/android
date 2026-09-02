package mega.privacy.android.app.service.iar

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.di.GlobalManagementEntryPoint
import mega.privacy.android.app.middlelayer.iar.OnCompleteListener
import mega.privacy.android.app.middlelayer.iar.RatingHandler
import timber.log.Timber
import javax.inject.Inject

/**
 * Implement rating feature for Google play store using google APIs
 *
 * @param context Context for getting rating dialog
 */
class RatingHandlerImpl @Inject constructor(@ApplicationContext context: Context) :
    RatingHandler(context) {

    override fun showReviewDialog(context: Context, listener: OnCompleteListener) {
        val manager: ReviewManager = ReviewManagerFactory.create(context)
        val request: Task<ReviewInfo> = manager.requestReviewFlow()
        val activityLifecycleHandler = EntryPointAccessors.fromApplication(
            context,
            GlobalManagementEntryPoint::class.java,
        ).activityLifecycleHandler()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                activityLifecycleHandler.getCurrentActivity()?.let {
                    val flow: Task<Void> = manager.launchReviewFlow(it, reviewInfo)
                    flow.addOnCompleteListener {
                        Timber.d("Rating Task Complete")
                        listener.onComplete()
                    }
                }
            } else {
                Timber.e("RatingTask is failed")
            }
        }
    }
}