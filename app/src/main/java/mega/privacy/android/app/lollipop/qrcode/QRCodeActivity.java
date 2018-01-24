package mega.privacy.android.app.lollipop.qrcode;

import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.QRCodeSaveBottomSheetDialogFragment;
import mega.privacy.android.app.utils.Util;

/**
 * Created by mega on 12/01/18.
 */

public class QRCodeActivity extends PinActivityLollipop {

    private Toolbar tB;
    private ActionBar aB;

    private MenuItem shareMenuItem;
    private MenuItem saveMenuItem;
    private MenuItem settingsMenuItem;
    private MenuItem resetQRMenuItem;

    private FrameLayout fragmentContainer;
    private TabLayout tabLayoutQRCode;
    private ViewPager viewPagerQRCode;

    private ScanCodeFragment scanCodeFragment;
    private MyCodeFragment myCodeFragment;

    private int qrCodeFragment;

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

        fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);

        tabLayoutQRCode =  (TabLayout) findViewById(R.id.sliding_tabs_qr_code);
        viewPagerQRCode = (ViewPager) findViewById(R.id.qr_code_tabs_pager);
        viewPagerQRCode.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

                supportInvalidateOptionsMenu();

                if (position == 0) {
                    qrCodeFragment = 0;
                }
                else {
                    qrCodeFragment = 1;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        viewPagerQRCode.setAdapter(new QRCodePageAdapter(getSupportFragmentManager(),this));
        tabLayoutQRCode.setupWithViewPager(viewPagerQRCode);
        viewPagerQRCode.setCurrentItem(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        log("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_qr_code, menu);

        shareMenuItem = menu.findItem(R.id.qr_code_share);
        saveMenuItem = menu.findItem(R.id.qr_code_save);
        settingsMenuItem = menu.findItem(R.id.qr_code_settings);
        resetQRMenuItem = menu.findItem(R.id.qr_code_reset);

        Drawable share = getResources().getDrawable(R.drawable.ic_social_share_white);
        share.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_ATOP);
        shareMenuItem.setIcon(share);

        switch (qrCodeFragment) {
            case 0: {
                shareMenuItem.setVisible(true);
                saveMenuItem.setVisible(true);
                settingsMenuItem.setVisible(true);
                resetQRMenuItem.setVisible(true);
                break;
            }
            case 1: {
                shareMenuItem.setVisible(false);
                saveMenuItem.setVisible(false);
                settingsMenuItem.setVisible(false);
                resetQRMenuItem.setVisible(false);
                break;
            }
        }

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
