package mega.privacy.android.app.lollipop.qrcode;

import android.content.Context;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import mega.privacy.android.app.R;

import static mega.privacy.android.app.utils.LogUtil.*;

public class QRCodePageAdapter extends FragmentPagerAdapter {

    final int PAGE_COUNT = 2;
    private Context context;

    public QRCodePageAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public Fragment getItem(int position) {
        logDebug("getItem: " + position);

        switch (position){
            case 0: {
                return MyCodeFragment.newInstance();
            }
            case 1:{
                return ScanCodeFragment.newInstance();
            }
        }
        return null;
    }

    @Override
    public int getItemPosition(Object obj) {
        return POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position){
            case 0: {
                return context.getString(R.string.section_my_code);
            }
            case 1:{
                return context.getString(R.string.section_scan_code);
            }
        }
        return null;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }
}
