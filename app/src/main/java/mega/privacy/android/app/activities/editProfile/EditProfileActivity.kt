package mega.privacy.android.app.activities.editProfile

import android.os.Bundle
import androidx.activity.viewModels
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.databinding.ActivityEditProfileBinding

class EditProfileActivity: PasscodeActivity(){

    private val viewModel by viewModels<EditProfileViewModel>()

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}