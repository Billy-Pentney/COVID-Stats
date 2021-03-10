package com.app.covidstats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.app.covidstats.region.Region;

public class OverviewFragment extends Fragment {
    TextView globalCasesValueTxt;
    TextView globalDeathsValueTxt;
    TextView globalCasesChangeTxt;
    TextView globalDeathsChangeTxt;
    TextView lastUpdatedValueTxt;

    DataViewModel dataViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataViewModel = new ViewModelProvider(getActivity()).get(DataViewModel.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_overview, container, false);

        globalCasesValueTxt = v.findViewById(R.id.globalCasesValue);
        globalDeathsValueTxt = v.findViewById(R.id.globalDeathsValue);
        globalCasesChangeTxt = v.findViewById(R.id.globalCasesChange);
        globalDeathsChangeTxt = v.findViewById(R.id.globalDeathsChange);
        lastUpdatedValueTxt = v.findViewById(R.id.lastUpdatedValue);

        LifecycleOwner lifecycleOwner = getViewLifecycleOwner();

        // sets up observers so that these textviews are updated when the values are updated
        Observer<String> globalCasesChangeObserver =
                globalCasesChange -> UpdateStatChangeTextView(globalCasesChange, globalCasesChangeTxt);
        Observer<String> globalDeathsChangeObserver =
                globalDeathsChange -> UpdateStatChangeTextView(globalDeathsChange, globalDeathsChangeTxt);
        Observer<String> lastUpdatedObserver =
                lastUpdated -> lastUpdatedValueTxt.setText(lastUpdated);

        Observer<Region> worldObserver = world -> {
            globalCasesValueTxt.setText(world.getTotalCases());
            globalDeathsValueTxt.setText(world.getTotalDeaths());
        };

        // apply observers to MutableLiveData in DataViewModel
        dataViewModel.GetGlobalCasesChangeLiveData().observe(lifecycleOwner, globalCasesChangeObserver);
        dataViewModel.GetGlobalDeathsChangeLiveData().observe(lifecycleOwner, globalDeathsChangeObserver);
        dataViewModel.GetLastUpdatedLiveData().observe(lifecycleOwner, lastUpdatedObserver);
        dataViewModel.GetWorldLiveData().observe(lifecycleOwner, worldObserver);

        return v;
    }

    public void UpdateStatChangeTextView(String displayTxt, TextView textView) {
        String lastText = textView.getText().toString();

        if (displayTxt.length() > 0) {
            // displays new text as long as it is longer than 0 characters
            textView.setText(displayTxt);
            if (lastText.length() == 0)
                // fades in if the previous text was blank
                textView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.anim_stat_change_appear));
        }
        else {
            // fades out if the new text has no length
            Animation fadeOutAnim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_stat_change_disappear);
            fadeOutAnim.setAnimationListener(new Animation.AnimationListener(){
                @Override
                public void onAnimationStart(Animation arg0) {
                }
                @Override
                public void onAnimationRepeat(Animation arg0) {
                }
                @Override
                public void onAnimationEnd(Animation arg0) {
                    textView.setText("");
                }
            });
            textView.startAnimation(fadeOutAnim);
        }
    }
}