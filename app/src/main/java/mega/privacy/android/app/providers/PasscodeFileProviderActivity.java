package mega.privacy.android.app.providers;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.utils.PasscodeUtil;

public class PasscodeFileProviderActivity extends AppCompatActivity {

	private PasscodeUtil passcodeUtil;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		passcodeUtil = new PasscodeUtil(this, new DatabaseHandler(this));
	}

	@Override
	protected void onPause() {
		passcodeUtil.pause();
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		passcodeUtil.resume();
	}
}
