package mega.privacy.android.app.meeting.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import kotlinx.android.synthetic.main.meeting_component_onofffab.*
import kotlinx.android.synthetic.main.meeting_component_onofffab.view.*
import kotlinx.android.synthetic.main.meeting_on_boarding_fragment.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.LogUtil.logDebug

abstract class AbstractMeetingOnBoardingFragment : MeetingBaseFragment() {

    val abstractMeetingOnBoardingViewModel: AbstractMeetingOnBoardingViewModel by viewModels()

    companion object {
        const val KEY_MEETING_MIC_STATE = "meeting_mic"
        const val KEY_MEETING_CAMERA_STATE = "meeting_camera"
        const val KEY_MEETING_SPEAKER_STATE = "meeting_speaker"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.meeting_on_boarding_fragment, container, false)
        initOnOffFab(savedInstanceState, view)
        onSubCreateView(view)
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_MEETING_MIC_STATE, fab_mic.isOn)
        outState.putBoolean(KEY_MEETING_CAMERA_STATE, fab_cam.isOn)
        outState.putBoolean(KEY_MEETING_SPEAKER_STATE, fab_speaker.isOn)
    }

    /**
     * Initialize OnOffFabs : Mic, Speaker, Camera...
     */
    private fun initOnOffFab(savedInstanceState: Bundle?, view: View) {
        savedInstanceState?.let {
            view.fab_mic.isOn = it.getBoolean(KEY_MEETING_MIC_STATE, false)
            view.fab_cam.isOn = it.getBoolean(KEY_MEETING_CAMERA_STATE, false)
            view.fab_speaker.isOn = it.getBoolean(KEY_MEETING_SPEAKER_STATE, false)
        }

        view.fab_mic.setOnClickListener {
            run {
                switchOnOffFab(it, !fab_mic.isOn)
            }
        }

        view.fab_cam.setOnClickListener {
            run {
                switchOnOffFab(it, !fab_cam.isOn)
            }
        }

        view.fab_speaker.setOnClickListener {
            run {
                switchOnOffFab(it, !fab_speaker.isOn)
            }
        }
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
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
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
            imm.hideSoftInputFromWindow(view.windowToken, 0, null)
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
     * Control OnOffFabs such as mic, camera and speaker
     *
     * @param bOn true: open camera preview; false: close camera preview
     */
    fun switchOnOffFab(id: View, bOn: Boolean) {
        when {
            // Mic
            id == fab_mic && bOn -> if (checkPermissionsAudio()) activateMic()
            id == fab_mic && !bOn -> deactivateMic()
            // Camera
            id == fab_cam && bOn -> if (checkPermissionsCamera()) activateCamera()
            id == fab_cam && !bOn -> deactivateCamera()
            // Speaker
            id == fab_speaker && bOn -> if (checkPermissionsAudio()) activateSpeaker()
            id == fab_speaker && !bOn -> deactivateSpeaker()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        logDebug("onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) return
        when (requestCode) {
            Constants.REQUEST_CAMERA, Constants.REQUEST_RECORD_AUDIO -> {
                logDebug("REQUEST_CAMERA || RECORD_AUDIO")
                if (checkPermissionsCamera()) {
                    activateCamera()
                }
            }
        }
    }

    /**
     * Determine whether you have been granted a particular permission
     *
     * @param permission The name of the permission being checked.
     * @param requestCode Application specific request code to match with a result
     *    reported to {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    fun checkPermissions(permission: String, requestCode: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        val hasPermission =
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            requestPermissions(arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    /**
     * Check Permissions of Camera
     *
     * @return whether you have been granted a particular permission
     */
    fun checkPermissionsCamera(): Boolean {
        logDebug("checkPermissionsCamera")
        return (checkPermissions(
            Manifest.permission.CAMERA,
            Constants.REQUEST_CAMERA
        ))
    }

    /**
     * Check Permissions of Audio
     *
     * @return whether you have been granted a particular permission
     */
    fun checkPermissionsAudio(): Boolean {
        logDebug("checkPermissionsCall")
        return (checkPermissions(
            Manifest.permission.RECORD_AUDIO,
            Constants.REQUEST_RECORD_AUDIO
        ))
    }

    /**
     * Activate Camera
     */
    fun activateCamera() {
        logDebug("Activate Camera")
        fab_cam.isOn = !fab_cam.isOn
        camera_preview.visibility = View.VISIBLE
    }

    /**
     * Deactivate Camera
     */
    fun deactivateCamera() {
        logDebug("Deactivate Camera")
        fab_cam.isOn = !fab_cam.isOn
        camera_preview.visibility = View.GONE
    }

    /**
     * Deactivate Speaker
     */
    private fun deactivateSpeaker() {
        fab_speaker.isOn = !fab_speaker.isOn
        logDebug("Deactivate Speaker")
    }

    /**
     * Activate Speaker
     */
    private fun activateSpeaker() {
        fab_speaker.isOn = !fab_speaker.isOn
        logDebug("Activate Speaker")
    }

    /**
     * Deactivate Mic
     */
    private fun deactivateMic() {
        logDebug("Deactivate Mic")
        fab_mic.isOn = !fab_mic.isOn
    }

    /**
     * Activate Mic
     */
    private fun activateMic() {
        logDebug("Activate Mic")
        fab_mic.isOn = !fab_mic.isOn
    }
}