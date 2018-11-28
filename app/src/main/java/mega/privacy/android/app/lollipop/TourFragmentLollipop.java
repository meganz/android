package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.TourImageAdapter;
import mega.privacy.android.app.components.LoopViewPager;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;

public class TourFragmentLollipop extends Fragment implements View.OnClickListener{

    Context context;
    LinearLayout mainLinearLayout;
    private TourImageAdapter adapter;
    private LoopViewPager viewPager;
    private LinearLayout bar1;
    private LinearLayout bar2;
    private LinearLayout bar3;
    private LinearLayout bar4;
    private Button bRegister;
    private Button bLogin;
    TextView tourText1;
    TextView tourText2;
    TextView achievementProgramText;
    LinearLayout shapeGrey;
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
        mainLinearLayout = (LinearLayout) v.findViewById(R.id.main_linear_layout_tour);
        scrollView = (ScrollView) v.findViewById(R.id.scroll_view_tour);
        viewPager = (LoopViewPager) v.findViewById(R.id.pager);
        bar1 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_1);
        bar2 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_2);
        bar3 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_3);
        bar4 = (LinearLayout) v.findViewById(R.id.bar_tour_layout_4);
        tourLoginCreate = (LinearLayout) v.findViewById(R.id.tour_login_create);

       // mainLinearLayout.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
        tourText1 = (TextView) v.findViewById(R.id.tour_text_1);
        tourText1.setGravity(Gravity.CENTER_VERTICAL);

        tourText2 = (TextView) v.findViewById(R.id.tour_text_2);
        tourText2.setGravity(Gravity.CENTER_VERTICAL);

        achievementProgramText = (TextView) v.findViewById(R.id.text_achievements_program);
        achievementProgramText.setText("*"+getString(R.string.footnote_achievements));

        shapeGrey = (LinearLayout) v.findViewById(R.id.shape_grey);
        optionsLayout = (LinearLayout) v.findViewById(R.id.options_layout);

        bLogin = (Button) v.findViewById(R.id.button_login_tour);
        bRegister = (Button) v.findViewById(R.id.button_register_tour);

        bLogin.setOnClickListener(this);
        bRegister.setOnClickListener(this);

        adapter = new TourImageAdapter((LoginActivityLollipop)context);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        bar1.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_red));
        bar2.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
        bar3.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
        bar4.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));

        viewPager.getLayoutParams().height = metrics.widthPixels-70;
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
                        bar1.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_red));
                        bar2.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar3.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar4.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        achievementProgramText.setVisibility(View.VISIBLE);
                        break;
                    }
                    case 1:{
                        bar1.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar2.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_red));
                        bar3.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar4.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        achievementProgramText.setVisibility(View.GONE);
                        break;
                    }
                    case 2:{
                        bar1.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar2.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar3.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_red));
                        bar4.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        achievementProgramText.setVisibility(View.GONE);
                        break;
                    }
                    case 3:{
                        bar1.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar2.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar3.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_grey));
                        bar4.setBackgroundColor(ContextCompat.getColor(context, R.color.tour_bar_red));
                        achievementProgramText.setVisibility(View.GONE);
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
        Util.log("TourFragmentLollipop", message);
    }

}
