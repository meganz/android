package mega.privacy.android.app.lollipop

import android.os.Bundle
import mega.privacy.android.app.databinding.ActivityVerifyTwoFactorBinding

class VerifyTwoFactorActivity : PinActivityLollipop() {

    private lateinit var binding: ActivityVerifyTwoFactorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyTwoFactorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeButtonEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

}