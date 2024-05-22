package mega.privacy.android.app.activities.contract

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.presentation.hidenode.HiddenNodesOnboardingActivity

/**
 * Hidden node onboarding activity contract
 */
class HiddenNodeOnboardingActivityContract :
    ActivityResultContract<LongArray, Boolean>() {
    override fun createIntent(context: Context, input: LongArray): Intent {
        return HiddenNodesOnboardingActivity.createScreen(
            context = context,
            isOnboarding = true,
        )
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}