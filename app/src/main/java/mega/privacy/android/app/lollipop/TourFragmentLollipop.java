package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.TourImageAdapter;
import mega.privacy.android.app.components.LoopViewPager;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class TourFragmentLollipop extends Fragment implements View.OnClickListener{

    Context context;
    private TourImageAdapter adapter;
    private LoopViewPager viewPager;
    private ImageView firstItem;
    private ImageView secondItem;
    private ImageView thirdItem;
    private ImageView fourthItem;
    private Button bRegister;
    private Button bLogin;
    private ScrollView baseContainer;

    @Override
    public void onCreate (Bundle savedInstanceState){
        logDebug("onCreate");
        super.onCreate(savedInstanceState);

        if(context==null){
            logError("Context is null");
            return;
        }
    }

    void setStatusBarColor (int position) {
        switch (position) {
            case 0: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_1));
                break;
            }
            case 1: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_2));
                break;
            }
            case 2: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_3));
                break;
            }
            case 3: {
                ((LoginActivityLollipop) context).getWindow().setStatusBarColor(ContextCompat.getColor(context, R.color.statusbar_tour_4));
                break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setStatusBarColor(viewPager.getCurrentItem());

        // For small screen like nexus one or bigger screen, this is to force the scroll view to bottom to show buttons
        // Meanwhile, tour image glide could also be shown
        baseContainer.post(new Runnable() {
            @Override
            public void run() {
                if (baseContainer != null) {
                    baseContainer.fullScroll(View.FOCUS_DOWN);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        View v = inflater.inflate(R.layout.fragment_tour, container, false);
        viewPager = (LoopViewPager) v.findViewById(R.id.pager);
        firstItem = (ImageView) v.findViewById(R.id.first_item);
        secondItem = (ImageView) v.findViewById(R.id.second_item);
        thirdItem = (ImageView) v.findViewById(R.id.third_item);
        fourthItem = (ImageView) v.findViewById(R.id.fourth_item);

        baseContainer = (ScrollView) v.findViewById(R.id.tour_fragment_base_container);

        bLogin = (Button) v.findViewById(R.id.button_login_tour);
        bRegister = (Button) v.findViewById(R.id.button_register_tour);

        bLogin.setOnClickListener(this);
        bRegister.setOnClickListener(this);

        adapter = new TourImageAdapter((LoginActivityLollipop)context);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(0);

        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
        setStatusBarColor(0);

        viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener(){

            @Override
            public void onPageSelected (int position){

                setStatusBarColor(position);

                switch(position){
                    case 0:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 1:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 2:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        break;
                    }
                    case 3:{
                        firstItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        secondItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        thirdItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.not_selection_circle_page_adapter));
                        fourthItem.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.selection_circle_page_adapter));
                        break;
                    }
                }
            }
        });

        return v;
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){
            case R.id.button_register_tour:
                logDebug("onRegisterClick");
                ((LoginActivityLollipop)context).showFragment(CREATE_ACCOUNT_FRAGMENT);
                break;
            case R.id.button_login_tour:
                logDebug("onLoginClick");
                ((LoginActivityLollipop)context).showFragment(LOGIN_FRAGMENT);
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        logDebug("onAttach");
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity context) {
        logDebug("onAttach Activity");
        super.onAttach(context);
        this.context = context;
    }
}
