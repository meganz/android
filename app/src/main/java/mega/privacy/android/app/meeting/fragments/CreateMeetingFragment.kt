package mega.privacy.android.app.meeting.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.create_meeting_fragment.*
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.*
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface

class CreateMeetingFragment : MeetingBaseFragment(), MegaRequestListenerInterface {

    companion object {
        fun newInstance() = CreateMeetingFragment()
    }

    private lateinit var viewModel: CreateMeetingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.create_meeting_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CreateMeetingViewModel::class.java)
        setProfileAvatar()
    }
    fun setProfileAvatar() {
        LogUtil.logDebug("setProfileAvatar")
        val circleAvatar = AvatarUtil.getCircleAvatar(requireContext(), megaApi.myEmail)
        if (circleAvatar!!.first) {
            meeting_thumbnail.setImageBitmap(circleAvatar.second)
        } else {
            megaApi.getUserAvatar(
                megaApi.myUser!!,
                CacheFolderManager.buildAvatarFile(
                    requireContext(),
                    megaApi.myEmail + FileUtil.JPG_EXTENSION
                ).absolutePath,
                this
            )
        }
    }
    fun setDefaultAvatar() {
        LogUtil.logDebug("setDefaultAvatar")
        meeting_thumbnail.setImageBitmap(
            AvatarUtil.getDefaultAvatar(
                AvatarUtil.getColorAvatar(
                    megaApi.myUser
                ), MegaApplication.getInstance().myAccountInfo.fullName, Constants.AVATAR_SIZE, true
            )
        )
    }

    fun setOfflineAvatar(email: String?, myHandle: Long, name: String?) {
        LogUtil.logDebug("setOfflineAvatar")
        if (meeting_thumbnail == null) {
            return
        }
        val circleAvatar = AvatarUtil.getCircleAvatar(requireContext(), email)
        if (circleAvatar!!.first) {
            meeting_thumbnail.setImageBitmap(circleAvatar.second)
        } else {
            meeting_thumbnail.setImageBitmap(
                AvatarUtil.getDefaultAvatar(
                    AvatarUtil.getColorAvatar(myHandle),
                    name,
                    Constants.AVATAR_SIZE,
                    true
                )
            )
        }
    }
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {

    }

    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {

    }

    override fun onRequestFinish(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {

    }

    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {

    }
}