package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.LogUtil.logDebug

abstract class AbstractMeetingOnBoardingFragment: MeetingBaseFragment() {
    val abstractMeetingOnBoardingViewModel: AbstractMeetingOnBoardingViewModel  by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var view: View = inflater.inflate(R.layout.meeting_on_boarding_fragment, container, false)
        onSubCreateView(view)
        return view
    }

    /**
     * Called by onCreateView(), for inherit subclasses to initialize UI
     *
     * @param view The root View of the inflated hierarchy
     */
    abstract fun onSubCreateView(view: View)

    /**
     * Used by inherit subclasses
     * Create / Join / Join as Guest
     */
    abstract fun meetingButtonClick()

    /**
     * Pop key board
     */
    fun showKeyboardDelayed(view: EditText) {
        GlobalScope.async {
            delay(50)
            view.isFocusable = true;
            view.isFocusableInTouchMode = true;
            view.requestFocus();
            val imm =
                view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.let {
                imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
            }
        }
    }

    /**
     * Hide key board immediately
     */
    fun hideKeyboard(view: View) {
        logDebug("hideKeyboard() ")
        view.clearFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.let {
            imm.hideSoftInputFromWindow (view.windowToken, 0, null)
        }
    }

    /**
     * Get Avatar
     */
    fun setProfileAvatar() {
        logDebug("setProfileAvatar")
        abstractMeetingOnBoardingViewModel.avatar.observe(viewLifecycleOwner) {
            meeting_thumbnail.setImageBitmap(it)
        }
    }

    /**
     * Control background camera preview
     *
     * @param bShow true: open camera preview; false: close camera preview
     */
    fun showBackgroundCameraPreview(bShow: Boolean){

        when(bShow) {
            true -> {
                camera_preview.visibility = View.VISIBLE
            }
            false -> camera_preview.visibility = View.GONE
        }
    }

}