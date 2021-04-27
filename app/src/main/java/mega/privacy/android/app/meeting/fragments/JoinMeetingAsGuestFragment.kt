package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util


@AndroidEntryPoint
class JoinMeetingAsGuestFragment : AbstractMeetingOnBoardingFragment() {

    private val viewModel: JoinMeetingAsGuestViewModel by viewModels()

    override fun onMeetingButtonClick() {
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        edit_first_name.visibility = View.VISIBLE
        edit_last_name.visibility = View.VISIBLE
        btn_start_join_meeting.setText(R.string.btn_join_meeting_as_guest)
        Util.showKeyboardDelayed(edit_first_name)
        reLayoutCameraPreviewView()
    }

    /**
     * Constrain the bottom of camera preview surface view to the top of name input EditText
     */
    private fun reLayoutCameraPreviewView() {
        val constraintSet = ConstraintSet()
        constraintSet.clone(create_meeting)
        constraintSet.connect(
            R.id.localSurfaceView,
            ConstraintSet.BOTTOM,
            R.id.edit_first_name,
            ConstraintSet.TOP,
        )
        constraintSet.applyTo(create_meeting)
    }
}