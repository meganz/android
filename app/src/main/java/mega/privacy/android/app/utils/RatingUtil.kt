package mega.privacy.android.app.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import mega.privacy.android.app.MegaApplication

class RatingUtil {

    fun showReviewDialogBaseOnCondition(context: Context, size: Long, speed: Long, listener: OnCompleteListener) {
        if (shouldShowReviewDialog(size, speed)) {
            showReviewDialog(context)
            listener.onComplete()
        }
    }

    private fun shouldShowReviewDialog(size: Long, speed: Long): Boolean {
        if (size <= 0 || speed <= 0) return false
        if (byteToMb(size) >= SIZE_LIMIT && byteToMb(speed) >= SPEED_LIMIT) {
            return true
        }
        return false
    }

    private fun showReviewDialog(context: Context) {
        val manager: ReviewManager = ReviewManagerFactory.create(context)
        val request: Task<ReviewInfo> = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo: ReviewInfo = task.result
                Toast.makeText(context, "Task Successful", Toast.LENGTH_SHORT).show()
                MegaApplication.getInstance().currentActivity?.let {
                    val flow: Task<Void> =
                        manager.launchReviewFlow(it, reviewInfo)
                    flow.addOnCompleteListener {
                        Toast.makeText(context, "onComplete", Toast.LENGTH_SHORT).show()
                    }
                    flow.addOnSuccessListener {
                        Toast.makeText(context, "onSuccess", Toast.LENGTH_SHORT).show()
                    }
                    flow.addOnFailureListener { e ->
                        Toast.makeText(context, "onFailure: $e", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Task is failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val SIZE_LIMIT = 10
        const val SPEED_LIMIT = 2
    }

    private fun byteToMb(bytes: Long): Long = bytes / 1024 / 1024
}

interface OnCompleteListener{
    fun onComplete()
}