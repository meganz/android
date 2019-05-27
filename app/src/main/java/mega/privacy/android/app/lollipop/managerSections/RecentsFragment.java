package mega.privacy.android.app.lollipop.managerSections;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import mega.privacy.android.app.utils.Util;

public class RecentsFragment extends Fragment implements View.OnClickListener {

    RecentsFragment recentsFragment;

    public static RecentsFragment newInstance() {
        log("newInstance");
        RecentsFragment fragment = new RecentsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onClick(View v) {

    }

    private static void log(String log) {
        Util.log("RecentsFragment",log);
    }
}
