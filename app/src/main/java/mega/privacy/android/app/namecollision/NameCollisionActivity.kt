package mega.privacy.android.app.namecollision

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import mega.privacy.android.app.databinding.ActivityNameCollisionBinding

class NameCollisionActivity : AppCompatActivity() {

    private val viewModel: NameCollisionViewModel by viewModels()

    private lateinit var binding: ActivityNameCollisionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNameCollisionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupData(savedInstanceState)
        setupView()
        setupObservers()
    }

    private fun setupData(savedInstanceState: Bundle?) {

    }

    private fun setupView() {

    }

    private fun setupObservers() {

    }
}