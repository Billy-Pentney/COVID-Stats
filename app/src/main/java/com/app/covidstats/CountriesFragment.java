package com.app.covidstats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.covidstats.region.Region;

import java.util.ArrayList;

import static com.app.covidstats.CountryRecyclerViewAdapter.ItemClickListener;

public class CountriesFragment extends Fragment implements ScrollToTopInterface {
    RecyclerView recyclerView;
    CountryRecyclerViewAdapter recyclerViewAdapter;
    DataViewModel dataViewModel;
    TextView checkInternetTxt;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataViewModel = new ViewModelProvider(getActivity()).get(DataViewModel.class);
        recyclerViewAdapter = new CountryRecyclerViewAdapter(getContext(), dataViewModel.getCountries());
        recyclerViewAdapter.setClickListener(recyclerViewClickListener);

        Observer<Region> worldRegionObserver = worldRegion -> {
            ArrayList<Region> countries = worldRegion.getSubregions();
            recyclerViewAdapter.UpdateData(countries);
            DisplayNoCountriesMessage(countries.size() == 0);
        };

        dataViewModel.GetWorldLiveData().observe(getActivity(), worldRegionObserver);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_countries_list, container, false);

        recyclerView = v.findViewById(R.id.countriesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        checkInternetTxt = v.findViewById(R.id.checkInternetTxt);
        // if no countries in recycler view, display error message TextView
        DisplayNoCountriesMessage(recyclerViewAdapter.getItemCount() == 0);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(recyclerViewAdapter);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        return v;
    }

    public void SortRecyclerView(int id) {
        if (recyclerViewAdapter != null) {
            recyclerViewAdapter.sort(id);
        }
    }

    public void ScrollToTop() {
        if (recyclerView != null)
            recyclerView.smoothScrollToPosition(0);
    }

    final ItemClickListener recyclerViewClickListener = new ItemClickListener() {
        @Override
        public void onItemClick(View view, int position) {
            dataViewModel.setCurrentSubregionIndex(position);
            Navigation.findNavController(getActivity(), R.id.nav_host_fragment)
                    .navigate(R.id.action_pagerFragment_to_countryDetailFragment);
        }
    };

    private void DisplayNoCountriesMessage(boolean setVisible) {
        if (checkInternetTxt != null) {
            if (setVisible)
                checkInternetTxt.setVisibility(View.VISIBLE);
            else
                checkInternetTxt.setVisibility(View.GONE);
        }
    }
}
