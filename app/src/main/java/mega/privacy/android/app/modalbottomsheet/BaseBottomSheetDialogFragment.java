package mega.privacy.android.app.modalbottomsheet;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class BaseBottomSheetDialogFragment extends BottomSheetDialogFragment {
    private static final int HEIGHT_CHILD = 50;
    static final int HEIGHT_HEADER_LARGE = 81;
    static final int HEIGHT_HEADER_LOW = 48;

    protected Context context;
    protected MegaApiAndroid megaApi;
    protected MegaChatApiAndroid megaChatApi;
    protected DatabaseHandler dbH;

    protected DisplayMetrics outMetrics;
    private int halfHeightDisplay;
    private int heightHeader;
    protected BottomSheetBehavior mBehavior;

    protected View contentView;
    protected LinearLayout mainLinearLayout;
    protected LinearLayout items_layout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MegaApplication app = MegaApplication.getInstance();
        if (app == null || getActivity() == null) {
            logError("MegaApplication or FragmentActivity is null");
            return;
        }

        megaApi = app.getMegaApi();
        megaChatApi = app.getMegaChatApi();
        dbH = app.getDbH();

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        halfHeightDisplay = outMetrics.heightPixels / 2;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    void setBottomSheetBehavior(int heightHeader, boolean addBottomSheetCallBack) {
        this.heightHeader = heightHeader;
        mBehavior = BottomSheetBehavior.from((View) contentView.getParent());

        int peekHeight = getPeekHeight();
        if (peekHeight < halfHeightDisplay) {
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            mBehavior.setPeekHeight(peekHeight);
            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        if (addBottomSheetCallBack) {
            addBottomSheetCallBack();
        }
    }

    void setStateBottomSheetBehaviorHidden() {
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void addBottomSheetCallBack() {
        mBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss();
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                if (isScreenInPortrait(context)) {
                    ViewGroup.LayoutParams params = bottomSheet.getLayoutParams();
                    if (getActivity() != null && getActivity().findViewById(R.id.toolbar) != null) {
                        int tBHeight = getActivity().findViewById(R.id.toolbar).getHeight();
                        Rect rectangle = new Rect();
                        getActivity().getWindow().getDecorView().getWindowVisibleDisplayFrame(rectangle);
                        int windowHeight = rectangle.bottom;
                        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics());
                        int maxHeight = windowHeight - tBHeight - rectangle.top - padding;

                        if (mainLinearLayout.getHeight() > maxHeight) {
                            params.height = maxHeight;
                            bottomSheet.setLayoutParams(params);
                        }
                    }
                }
            }
        });
    }

    private int getPeekHeight() {
        int numOptions = items_layout.getChildCount();
        int numOptionsVisibles = 0;
        int heightChild = px2dp(HEIGHT_CHILD, outMetrics);
        int peekHeight = px2dp(heightHeader, outMetrics);

        for (int i = 0; i < numOptions; i++) {
            if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE) {
                numOptionsVisibles++;
            }
        }

        if ((numOptionsVisibles <= 3 && heightHeader == HEIGHT_HEADER_LARGE) || (numOptionsVisibles <= 4 && heightHeader == HEIGHT_HEADER_LOW)) {
            return peekHeight + (heightChild * numOptions);
        } else {
            for (int i = 0; i < numOptions; i++) {
                if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE && peekHeight < halfHeightDisplay) {
                    peekHeight += heightChild;

                    if (peekHeight >= halfHeightDisplay) {
                        if (items_layout.getChildAt(i + 2) != null) {
                            for (int j = i + 2; j < numOptions; j++) {
                                if (items_layout.getChildAt(j).getVisibility() == View.VISIBLE) {
                                    return peekHeight + (heightChild / 2);
                                }
                            }

                            return peekHeight + heightChild;
                        } else if (items_layout.getChildAt(i + 1) != null) {
                            if (items_layout.getChildAt(i + 1).getVisibility() == View.VISIBLE) {
                                return peekHeight + (heightChild / 2);
                            } else {
                                return peekHeight + heightChild;
                            }
                        } else {
                            return peekHeight + heightChild;
                        }
                    }
                }
            }
        }

        return peekHeight;
    }
}
