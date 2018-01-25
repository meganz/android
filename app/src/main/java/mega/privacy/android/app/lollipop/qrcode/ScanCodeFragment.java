package mega.privacy.android.app.lollipop.qrcode;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

/**
 * Created by mega on 22/01/18.
 */

public class ScanCodeFragment extends Fragment implements ZXingScannerView.ResultHandler{


    private ActionBar aB;

    private Context context;

    public static ZXingScannerView scannerView;

    private final int MY_PERMISSIONS_REQUEST_CAMERA = 1010;

    public static ScanCodeFragment newInstance() {
        log("newInstance");
        ScanCodeFragment fragment = new ScanCodeFragment();
        return fragment;
    }

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");

        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        scannerView = new ZXingScannerView(getActivity());

//        TextView textView = new TextView(getActivity());
//        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//        layoutParams.setMargins(70, 100, 70, 0);
//        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
//        textView.setLayoutParams(layoutParams);
//        textView.setText("Line up the QR code to scan it with your device's camera");
//        textView.setTextColor(getResources().getColor(R.color.white));
//        scannerView.addView(textView);

        if (aB == null){
            aB = ((AppCompatActivity)context).getSupportActionBar();
        }

        if(aB!=null){
            aB.setTitle(getString(R.string.section_qr_code));
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);
        }

        return scannerView;
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    @Override
    public void onResume() {
        log("onResume");
        super.onResume();
        scannerView.setResultHandler(this);
//        scannerView.startCamera();
    }

    @Override
    public void onPause() {
        log("onPause");
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        log("handleResult");
//        Do something with the result here
//        log(rawResult.getText()); // Prints scan results
//        log(rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)

        // call the alert dialog

//        Handler handler = new Handler();
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                scannerView.resumeCameraPreview(ScanCodeFragment.this);
//            }
//        }, 2000);

        Invite(rawResult);

    }

    public void Invite (Result rawResult){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Qr scan result");
        builder.setMessage("Result :"+rawResult.getText()+"\nType :"+rawResult.getBarcodeFormat().toString())
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        scannerView.resumeCameraPreview(ScanCodeFragment.this);
                    }
                })
                .setNegativeButton("Scan Again", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        scannerView.resumeCameraPreview(ScanCodeFragment.this);
                    }
                });

        builder.create().show();
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
        Util.log("ScanCodeFragment", log);
    }

}
