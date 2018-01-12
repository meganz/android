package mega.privacy.android.app.lollipop.qrcode;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Util;

/**
 * Created by mega on 12/01/18.
 */

public class QRCodeActivity extends PinActivityLollipop{

    private Toolbar tB;
    private ActionBar aB;

    private MenuItem shareMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        setContentView(R.layout.activity_qr_code);

        tB = (Toolbar) findViewById(R.id.toolbar);
        if(tB==null){
            log("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);

        tB.setTitle(getString(R.string.section_qr_code));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_qr_code, menu);

        shareMenuItem = menu.findItem(R.id.qr_code_share);

        Drawable share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        shareMenuItem.setIcon(share);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        log("onOptionsItemSelected");
        ((MegaApplication) getApplication()).sendSignalPresenceActivity();

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.qr_code_share: {

                break;
            }
            case R.id.qr_code_save: {
                QRCodeSaveBottomSheetDialogFragment qrCodeSaveBottomSheetDialogFragment = new QRCodeSaveBottomSheetDialogFragment();
                qrCodeSaveBottomSheetDialogFragment.show(getSupportFragmentManager(), qrCodeSaveBottomSheetDialogFragment.getTag());

                break;
            }
            case R.id.qr_code_settings: {

                break;
            }
            case R.id.qr_code_reset: {

                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static void log(String message) {
        Util.log("QRCodeActivity", message);
    }
}
