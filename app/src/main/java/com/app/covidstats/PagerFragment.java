package com.app.covidstats;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.app.covidstats.region.Region;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.MODE_PRIVATE;

public class PagerFragment extends Fragment {
    private ViewPager mPager;
    private ViewPagerAdapter pagerAdapter;
    private static final int NUM_PAGES = 2;
    private final Fragment[] fragments = new Fragment[NUM_PAGES];

    private DataViewModel dataViewModel;
    private final int[] tabTitles = new int[] { R.string.overview, R.string.countries };

    private int currentPosition = 0;

    private SwipeRefreshLayout swipeRefreshLayout;

    final String LAST_DATE_UPDATED = "date_updated";
    final String GLOBAL_CASES = "global_cases";
    final String GLOBAL_DEATHS = "global_deaths";

    private Toolbar toolbar;
    private GetDataAsync getDataAsync;

    public PagerFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragments[0] = new OverviewFragment();
        fragments[1] = new CountriesFragment();

        dataViewModel = new ViewModelProvider(getActivity()).get(DataViewModel.class);
        RestoreFromSharedPref();

        pagerAdapter = new ViewPagerAdapter(getChildFragmentManager());

        // update, but don't scroll to top
        UpdateStats(false);
    }

    @Override
    public void onStop() {
        super.onStop();
        SaveToSharedPref();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pager, container, false);

        mPager = root.findViewById(R.id.pager);
        mPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = root.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mPager, true);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) { }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // if user taps on same tab as they are currently on,
                // then any applicable ScrollView/RecyclerView is scrolled to the top

                int newPos = tabLayout.getSelectedTabPosition();
                if (currentPosition == newPos) {
                    if (newPos == 1) {
                        scrollCountriesFragmentToTop();
                    }
                }
                else {
                    currentPosition = newPos;
                }
            }
        });

        toolbar = root.findViewById(R.id.toolbar);
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }

            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    // for the CountriesFragment
                    toolbar.inflateMenu(R.menu.menu_sort);

                    // sets the radio buttons based on the current sorting type
                    int id = dataViewModel.getCurrentSortMode();
                    toolbar.getMenu().findItem(id).setChecked(true);
                }
                else {
                    // for OverviewFragment / other tabs
                    toolbar.getMenu().clear();
                    toolbar.inflateMenu(R.menu.menu_main);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // disables swipe to refresh when scrolling between pages
                if (swipeRefreshLayout != null && !swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setEnabled(state == ViewPager.SCROLL_STATE_IDLE);
                }
            }
        });

        swipeRefreshLayout = root.findViewById(R.id.swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> UpdateStats(true));

        swipeRefreshLayout.setRefreshing(true);

        return root;
    }

    private void scrollCountriesFragmentToTop() {
        // scrolls the list to the top on refresh
        CountriesFragment cf = getCountriesFragment();
        cf.ScrollToTop();
    }

    private boolean onMenuItemClick(MenuItem menuItem) {
        int id = menuItem.getItemId();

        if (id == R.id.menu_sort_by) {
            return true;
        }

        // manual refresh button if swipe inaccessible
        if (id == R.id.menu_refresh) {
            UpdateStats(true);
        }
        else {
            // controls radio buttons which sort the recycler view
            SortRecyclerView(id);
            menuItem.setChecked(true);
        }

        return true;
    }

    private void SortRecyclerView(int menuItemID) {
        if (fragments.length > 1) {
            CountriesFragment cf = getCountriesFragment();
            dataViewModel.setCurrentSortMode(menuItemID);
            cf.SortRecyclerView(menuItemID);
            cf.ScrollToTop();
        }
    }

    private CountriesFragment getCountriesFragment() {
        return (CountriesFragment)fragments[1];
    }

    private void UpdateStats(boolean scrollOnFinish) {
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(true);

        getDataAsync = new GetDataAsync(scrollOnFinish);
        getDataAsync.execute();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getDataAsync.cancel(true);
    }

    private void SaveToSharedPref() {
        // saves the current values into shared preferences

        SharedPreferences sharedPreferences = getActivity().getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(GLOBAL_CASES, dataViewModel.GetGlobalCases());
        editor.putInt(GLOBAL_DEATHS, dataViewModel.GetGlobalDeaths());
        editor.putString(LAST_DATE_UPDATED, dataViewModel.GetLastUpdated());
        editor.apply();
    }

    private void RestoreFromSharedPref() {
        // gets the values from shared preferences to display them
        SharedPreferences sharedPreferences = getActivity().getPreferences(MODE_PRIVATE);
        int restoredCases = sharedPreferences.getInt(GLOBAL_CASES, 0);
        int restoredDeaths = sharedPreferences.getInt(GLOBAL_DEATHS, 0);
        String lastUpdated = sharedPreferences.getString(LAST_DATE_UPDATED, "Never");
        dataViewModel.InitWorld(restoredCases, restoredDeaths);
        dataViewModel.SetLastUpdated(lastUpdated);
    }

    private class GetDataAsync extends AsyncTask<Void, Void, Void> {
        static final int TIMEOUT = 10000;

        // identifiers used to get webpage elements
        static final String PAGE_COUNTER_CLASS = "maincounter-number";
        static final String COUNTRY_TABLE_ID = "main_table_countries_today";

        static final int COUNTRY_TABLE_ROW_START = 9;
        static final String COUNTRY_STATS_URL = "https://www.worldometers.info/coronavirus/#countries/";
        Region world = new Region("World", "0");
        boolean scrollOnFinish = false;

        public GetDataAsync(boolean scrollOnFinish) {
            this.scrollOnFinish = scrollOnFinish;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                // connects to the countries page, so that data can be extracted from the table
                Connection connection = Jsoup.connect(COUNTRY_STATS_URL).timeout(TIMEOUT);
                Document document = connection.post();

                Element countriesTable = document.getElementById(COUNTRY_TABLE_ID);
                Elements rows = countriesTable.select("tr");

                // -1 indicates the region has no parent
                world = Region.ParseRowToRegion(rows.get(8), -1, -1);
                ArrayList<Region> countries = world.ParseRegionsFromTable(countriesTable, COUNTRY_TABLE_ROW_START);
                world.addSubRegions(countries);
            } catch (SocketTimeoutException ste) {
                DisplaySnackbar(R.string.error_timed_out);
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        private void DisplaySnackbar(int stringID) {
            Snackbar.make(getView(), getString(stringID), Snackbar.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (world != null && world.hasSubregions()) {
                String lastUpdated = GetFormattedDate();
                dataViewModel.SetLastUpdated(lastUpdated);
                dataViewModel.InitialiseWorld(world);

                if (scrollOnFinish)
                    scrollCountriesFragmentToTop();
            }
            else {
                DisplaySnackbar(R.string.check_internet);
            }

            swipeRefreshLayout.setRefreshing(false);
        }

        private String GetFormattedDate() {
            final SimpleDateFormat dateSDF = new SimpleDateFormat("MMMM dd, yyyy ");
            final SimpleDateFormat timeSDF = new SimpleDateFormat(" HH:mm");

            Date dt = Calendar.getInstance().getTime();

            return dateSDF.format(dt) + getString(R.string.at) + timeSDF.format(dt);
        }
    }

    private class ViewPagerAdapter extends FragmentStatePagerAdapter {
        public ViewPagerAdapter(@NonNull FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position > -1 && position < fragments.length)
                return fragments[position];
            return null;
        }

        @Override
        public int getCount() {
            return NUM_PAGES;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            // returns name of each page / tab name
            return getString(tabTitles[position]);
        }
    }
}
