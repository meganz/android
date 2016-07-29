package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

import mega.privacy.android.app.R;
import mega.privacy.android.app.TourImageAdapter;
import mega.privacy.android.app.components.LoopViewPager;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

public class TourFragmentLollipop extends Fragment implements View.OnClickListener{

    Context context;
    private TourImageAdapter adapter;
    private LoopViewPager viewPager;
    private LinearLayout bar1;
    private LinearLayout bar2;
    private LinearLayout bar3;
    private LinearLayout bar4;
    private TextView bRegister;
    private TextView bLogin;
    TextView tourText1;
    TextView tourText2;
    private LinearLayout tourLoginCreate;
    private LinearLayout optionsLayout;
    ScrollView scrollView;

    @Override
    public void onCreate (Bundle savedInstanceState){
        log("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            log("context is null");
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        View v = inflater.inflate(R.layout.fragment_tour, container, false);
        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_tour);
        viewPager = (LoopViewPager) v.findViewById(R.id.pager);
        bar1 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_1);
        bar2 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_2);
        bar3 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_3);
        bar4 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_4);
        tourLoginCreate = (LinearLayout) v.findViewById(R.id.tour_login_create);

        tourText1 = (TextView) v.findViewById(R.id.tour_text_1);
        tourText1.setPadding(Util.scaleWidthPx(16, metrics), 0, 0, 0);
        tourText1.setGravity(Gravity.CENTER_VERTICAL);
        android.view.ViewGroup.LayoutParams params = tourText1.getLayoutParams();

        params.height = Util.scaleHeightPx(56, metrics);
        tourText1.setLayoutParams(params);

        tourText2 = (TextView) v.findViewById(R.id.tour_text_2);
        tourText2.setPadding(Util.scaleWidthPx(16, metrics), 0, 0, 0);
        tourText2.setGravity(Gravity.CENTER_VERTICAL);
        params = tourText2.getLayoutParams();
        params.height = Util.scaleHeightPx(68, metrics);
        tourText2.setLayoutParams(params);
        //Bottom margin
        LinearLayout.LayoutParams textParams = (LinearLayout.LayoutParams)tourText2.getLayoutParams();
        textParams.setMargins(0, 0, 0, Util.scaleHeightPx(10, metrics));
        tourText2.setLayoutParams(textParams);

        float density  = getResources().getDisplayMetrics().density;

        float scaleW = Util.getScaleW(metrics, density);
        float scaleH = Util.getScaleH(metrics, density);
        float scaleText;
        if (scaleH < scaleW){
            scaleText = scaleH;
        }
        else{
            scaleText = scaleW;
        }
        tourText1.setTextSize(TypedValue.COMPLEX_UNIT_SP, (22*scaleText));
        tourText2.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));

        optionsLayout = (LinearLayout) v.findViewById(R.id.options_layout);
        LinearLayout.LayoutParams linearLayoutParams = (android.widget.LinearLayout.LayoutParams) optionsLayout.getLayoutParams();
        linearLayoutParams.setMargins(0, 0, 0, Util.scaleHeightPx(8, metrics));
        optionsLayout.setLayoutParams(linearLayoutParams);

        bLogin = (TextView) v.findViewById(R.id.button_login_tour);
        bLogin.setText(getString(R.string.login_text).toUpperCase(Locale.getDefault()));
        android.view.ViewGroup.LayoutParams paramsb2 = bLogin.getLayoutParams();
        paramsb2.height = Util.scaleHeightPx(48, metrics);
		/*paramsb2.width = Util.scaleWidthPx(63, metrics);*/
        bLogin.setLayoutParams(paramsb2);
        //Left and Right margin
        LinearLayout.LayoutParams textParamsLogin = (LinearLayout.LayoutParams)bLogin.getLayoutParams();
        textParamsLogin.setMargins(Util.scaleWidthPx(16, metrics), 0, Util.scaleWidthPx(15, metrics), 0);
        bLogin.setLayoutParams(textParamsLogin);

        bRegister = (TextView) v.findViewById(R.id.button_register_tour);
        bRegister.setText(getString(R.string.create_account).toUpperCase(Locale.getDefault()));
        android.view.ViewGroup.LayoutParams paramsb1 = bRegister.getLayoutParams();
        paramsb1.height = Util.scaleHeightPx(48, metrics);
		/*paramsb1.width = Util.scaleWidthPx(144, metrics);*/
//		bRegister.setGravity(Gravity.CENTER);
        bRegister.setLayoutParams(paramsb1);

        bLogin.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));
        bRegister.setTextSize(TypedValue.COMPLEX_UNIT_SP, (14*scaleText));

        bLogin.setOnClickListener(this);
        bRegister.setOnClickListener(this);

        adapter = new TourImageAdapter((LoginActivityLollipop)context);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        bar1.setBackgroundColor(getResources().getColor(R.color.tour_bar_red));
        bar2.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
        bar3.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
        bar4.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));

        viewPager.getLayoutParams().height = metrics.widthPixels;

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){

            @Override
            public void onPageSelected (int position){
                int[] barTitles = new int[] {
                        R.string.tour_space_title,
                        R.string.tour_speed_title,
                        R.string.tour_privacy_title,
                        R.string.tour_access_title
                };

                int[] barTexts = new int[] {
                        R.string.tour_space_text,
                        R.string.tour_speed_text,
                        R.string.tour_privacy_text,
                        R.string.tour_access_text
                };

                tourText1.setText(barTitles[position]);
                tourText2.setText(barTexts[position]);
                switch(position){
                    case 0:{
                        bar1.setBackgroundColor(getResources().getColor(R.color.tour_bar_red));
                        bar2.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar3.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar4.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        break;
                    }
                    case 1:{
                        bar1.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar2.setBackgroundColor(getResources().getColor(R.color.tour_bar_red));
                        bar3.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar4.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        break;
                    }
                    case 2:{
                        bar1.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar2.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar3.setBackgroundColor(getResources().getColor(R.color.tour_bar_red));
                        bar4.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        break;
                    }
                    case 3:{
                        bar1.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar2.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar3.setBackgroundColor(getResources().getColor(R.color.tour_bar_grey));
                        bar4.setBackgroundColor(getResources().getColor(R.color.tour_bar_red));
                        break;
                    }
                }
            }
        });
        //Set the scroll at the end of the screen
        scrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollView.scrollTo(0, scrollView.getBottom());
            }
        },100);

        return v;
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.button_register_tour:
                log("onRegisterClick");
                ((LoginActivityLollipop)context).showFragment(Constants.CREATE_ACCOUNT_FRAGMENT);
                break;
            case R.id.button_login_tour:
                log("onLoginClick");
                ((LoginActivityLollipop)context).showFragment(Constants.LOGIN_FRAGMENT);
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        log("onAttach");
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity context) {
        log("onAttach Activity");
        super.onAttach(context);
        this.context = context;
    }

    public static void log(String message) {
        Util.log("ChooseAccountFragmentLollipop", message);
    }
}
