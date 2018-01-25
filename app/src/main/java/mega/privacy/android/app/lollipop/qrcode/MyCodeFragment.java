package mega.privacy.android.app.lollipop.qrcode;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

/**
 * Created by mega on 22/01/18.
 */

public class MyCodeFragment extends Fragment implements View.OnClickListener {

    private ActionBar aB;

    private ImageView qrcode;
    private TextView qrcode_link;
    private Button qrcode_copy_link;
    private View v;

    private Context context;

    public static MyCodeFragment newInstance() {
        log("newInstance");
        MyCodeFragment fragment = new MyCodeFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        v = inflater.inflate(R.layout.fragment_mycode, container, false);

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if(aB!=null){
            aB.setTitle(getString(R.string.section_qr_code));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }

        qrcode = (ImageView) v.findViewById(R.id.qr_code_image);
        qrcode_link = (TextView) v.findViewById(R.id.qr_code_link);
        qrcode_copy_link = (Button) v.findViewById(R.id.qr_code_button_copy_link);
        qrcode_copy_link.setOnClickListener(this);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        log("onAttach");
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }

    @Override
    public void onAttach(Context context) {
        log("onAttach context");
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)getActivity()).getSupportActionBar();
    }

    private static void log(String log) {
        Util.log("MyCodeFragment", log);
    }

    @Override
    public void onClick(View v) {
        log("onClick");
        switch (v.getId()) {
            case R.id.qr_code_button_copy_link: {
                copyLink();
                break;
            }
        }
    }

    public void copyLink () {
        log("copyLink");
        showSnackbar(getString(R.string.qrcode_link_copied));
    }

    public void showSnackbar(String s){
        log("showSnackbar");
        Snackbar snackbar = Snackbar.make(v, s, Snackbar.LENGTH_LONG);
        TextView snackbarTextView = (TextView)snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarTextView.setMaxLines(5);
        snackbar.show();
    }
}
