package mega.privacy.android.app.lollipop;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

public class ConfirmEmailFragmentLollipop extends Fragment {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		log("onCreateView");
		View v = inflater.inflate(R.layout.fragment_confirm_email, container, false);
		return v;
	}

	public static void log(String log) {
		Util.log("ConfirmEmailFragmentLollipop", log);
	}
}
