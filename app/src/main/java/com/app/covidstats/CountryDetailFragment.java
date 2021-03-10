package com.app.covidstats;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.app.covidstats.region.Region;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.softmoore.android.graphlib.Graph;
import com.softmoore.android.graphlib.GraphView;
import com.softmoore.android.graphlib.Point;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;

public class CountryDetailFragment extends Fragment {

    // the possible names of continents given by the data
    public static final String[] CONTINENTS = new String[] {
            "Europe",
            "North America",
            "South America",
            "Asia",
            "Africa",
            "Australia/Oceania",
            "World"
    };

    // holds references to each continent's drawable image
    private static final HashMap<String, Integer> REGION_DRAWABLE_HASH = new HashMap<>();

    // markers to identify the scripts with wanted data
    private final static String casesKey = "'coronavirus-cases-linear'";
    private final static String deathsKey = "'coronavirus-deaths-linear'";
    private final static String dailyCasesKey = "'graph-cases-daily'";
    private final static String dailyDeathsKey = "'graph-deaths-daily'";
    private final static String casesSeriesKey = "name: 'Cases'";
    private final static String deathsSeriesKey = "name: 'Deaths'";
    private final static String dailyCasesSeriesKey = "name: '7-day moving average'";
    private final static String dailyDeathsSeriesKey = dailyCasesSeriesKey;

    private final static String startMarker = "data: [";   // indicates where the data array starts
    private final static String endMarker = "]";           // indicates where the data array ends

    private final static int RECOVERED_COLOR_FRG = R.color.colorRecovered;
    private final static int RECOVERED_COLOR_BKG = R.color.colorRecoveredBkg;
    private final static int INFECTED_COLOR_FRG = R.color.colorInfected;
    private final static int INFECTED_COLOR_BKG = R.color.colorInfectedBkg;

    // how many distinct points are plotted in the graphs
    private final int NUM_OF_DATA_POINTS = 100;

    private float continentHeaderBaseAlpha;

    Region displayRegion;

    NestedScrollView scrollView;
    CollapsingToolbarLayout collapsingToolbarLayout;
    ImageView parentRegionImg;

    ImageView flagImg;
    TextView nameTxt;
    TextView parentRegionTxt;
    TextView popTxt;

    TextView casesTxt;
    TextView deathsTxt;
    TextView testsTxt;

    TextView activeCasesTxt;
    TextView criticalCasesTxt;
    TextView recoveredCasesTxt;

    // given as a proportion of the global stats
    TextView casesProportionTxt;
    TextView deathsProportionTxt;

    TextView testPositiveRateTxt;
    TextView fatalityRateTxt;

    // stats per million people
    TextView casesPMTxt;
    TextView deathsPMTxt;
    TextView testsPMTxt;

    // GraphView from GraphLib created by John I. Moore, Jr.
    // https://www.infoworld.com/article/3226733/graphlib-an-open-source-android-library-for-graphs.html
    GraphView casesGraphView;
    GraphView deathsGraphView;
    GraphView dailyCasesGraphView;
    GraphView dailyDeathsGraphView;
    TextView casesGraphTitle;
    TextView deathsGraphTitle;
    TextView dailyCasesGraphTitle;
    TextView dailyDeathsGraphTitle;

    GetCountryDataAsync getCountryDataAsync = new GetCountryDataAsync();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_country_detail, container, false);

        DataViewModel dvm = new ViewModelProvider(getActivity()).get(DataViewModel.class);
        Region world = dvm.GetWorldDetails();
        displayRegion = world.getCurrentSubregion();

        // gets float alpha from dimensions
        TypedValue outValue = new TypedValue();
        getResources().getValue(R.dimen.continentHeaderAlpha, outValue, true);
        continentHeaderBaseAlpha = outValue.getFloat();

        REGION_DRAWABLE_HASH.put("Europe", R.drawable.europe);
        REGION_DRAWABLE_HASH.put("Asia", R.drawable.asia);
        REGION_DRAWABLE_HASH.put("North America", R.drawable.north_america);
        REGION_DRAWABLE_HASH.put("South America", R.drawable.south_america);
        REGION_DRAWABLE_HASH.put("Africa", R.drawable.africa);
        REGION_DRAWABLE_HASH.put("Australia/Oceania", R.drawable.oceania);
        REGION_DRAWABLE_HASH.put("World", R.drawable.world);
        REGION_DRAWABLE_HASH.put("USA", R.drawable.north_america);

        GetViews(v);

        if (displayRegion != null)
            DisplayDetails();

        return v;
    }

    private void GetViews(View v) {
        scrollView = v.findViewById(R.id.nestedScrollView);

        flagImg = v.findViewById(R.id.flagLrgImg);
        parentRegionImg = v.findViewById(R.id.parentRegionImg);
        parentRegionTxt = v.findViewById(R.id.parentRegionNameTxt);

        // these views are resized about their top-left point
        flagImg.setPivotX(0);
        flagImg.setPivotY(0);
        parentRegionTxt.setPivotX(0);
        parentRegionTxt.setPivotY(0);

        collapsingToolbarLayout = v.findViewById(R.id.collapsingToolbarLayout);
        AppBarLayout appBarLayout = v.findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener((appBarLayout1, verticalOffset) -> {
            // fades and scales TopBar contents on scroll
            float proportionScrolled = (float)verticalOffset / (2 * appBarLayout1.getTotalScrollRange());
            float alpha = 1 + 3 * proportionScrolled;
            float scale = 1 + proportionScrolled;
            flagImg.setAlpha(alpha);
            flagImg.setScaleX(scale);
            flagImg.setScaleY(scale);
            parentRegionTxt.setAlpha(alpha);
            parentRegionTxt.setScaleX(scale);
            parentRegionTxt.setScaleY(scale);
            parentRegionImg.setAlpha(alpha * continentHeaderBaseAlpha);
        });

        popTxt = v.findViewById(R.id.popValTxt);
        casesTxt = v.findViewById(R.id.casesValTxt);
        deathsTxt = v.findViewById(R.id.deathsValTxt);
        testsTxt = v.findViewById(R.id.testsValTxt);

        activeCasesTxt = v.findViewById(R.id.activeCasesValTxt);
        criticalCasesTxt = v.findViewById(R.id.criticalCasesValTxt);
        recoveredCasesTxt = v.findViewById(R.id.recoveredCasesValTxt);

        casesProportionTxt = v.findViewById(R.id.casesPropValTxt);
        deathsProportionTxt = v.findViewById(R.id.deathsPropValTxt);

        testPositiveRateTxt = v.findViewById(R.id.testPositiveRateValTxt);
        fatalityRateTxt = v.findViewById(R.id.fatalityRateValTxt);

        casesPMTxt = v.findViewById(R.id.casesPMValTxt);
        deathsPMTxt = v.findViewById(R.id.deathsPMValTxt);
        testsPMTxt = v.findViewById(R.id.testsPMValTxt);

        casesGraphView = v.findViewById(R.id.casesGraphView);
        deathsGraphView = v.findViewById(R.id.deathsGraphView);
        dailyCasesGraphView = v.findViewById(R.id.dailyCasesGraphView);
        dailyDeathsGraphView = v.findViewById(R.id.dailyDeathsGraphView);

        casesGraphTitle = v.findViewById(R.id.casesGraphTxt);
        deathsGraphTitle = v.findViewById(R.id.deathsGraphTxt);
        dailyCasesGraphTitle = v.findViewById(R.id.dailyCasesGraphTxt);
        dailyDeathsGraphTitle = v.findViewById(R.id.dailyDeathsGraphTxt);
    }

    private void DisplayFlagDrawable() {
        Drawable d = displayRegion.getFlag(getContext());
        boolean showImage = false;

        if (d != null) {
            flagImg.setImageDrawable(d);
            showImage = true;
        }

        setViewVisibility(flagImg, showImage);
    }

    private void setViewVisibility(View v, boolean setVisible) {
        Animation anim = null;
        int currVisibility = v.getVisibility();

        if (setVisible && currVisibility != View.VISIBLE) {
            anim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_graph_appear);
            v.setVisibility(View.VISIBLE);
        }
        else if (!setVisible) {
            anim = AnimationUtils.loadAnimation(getContext(), R.anim.anim_graph_disappear);
            anim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { }

                @Override
                public void onAnimationEnd(Animation animation) {
                    v.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) { }
            });
        }

        if (anim != null)
            v.startAnimation(anim);
    }

    private void DisplayDetails() {
        /*  THEMES the window based on the state of the infection in the country
        if (country.isRecovered())
            DisplayTheme(RECOVERED_COLOR_FRG, RECOVERED_COLOR_BKG);
        else
            DisplayTheme(INFECTED_COLOR_FRG, INFECTED_COLOR_BKG);   */

        DisplayFlagDrawable();

        // sets header continent image based on that of the country
        String parentRegionName = displayRegion.getParentName();

        if (REGION_DRAWABLE_HASH.containsKey(parentRegionName)) {
            //noinspection ConstantConditions -> already checked if hash contains key
            int drawableResourceID = REGION_DRAWABLE_HASH.get(parentRegionName);
            Drawable parentRegionDrawable = getResources().getDrawable(drawableResourceID);
            parentRegionImg.setImageDrawable(parentRegionDrawable);
        }
        else {
            // if error, hide the image view
            parentRegionImg.setVisibility(View.GONE);
        }

        collapsingToolbarLayout.setTitle(displayRegion.getName());
        parentRegionTxt.setText(parentRegionName);

        popTxt.setText(displayRegion.getPopulation());
        casesTxt.setText(displayRegion.getTotalCases());
        deathsTxt.setText(displayRegion.getTotalDeaths());
        testsTxt.setText(displayRegion.getTotalTests());

        activeCasesTxt.setText(displayRegion.getActiveCases());
        criticalCasesTxt.setText(displayRegion.getCriticalCases());
        recoveredCasesTxt.setText(displayRegion.getRecoveredCases());

        casesProportionTxt.setText(displayRegion.getCasesProportion());
        deathsProportionTxt.setText(displayRegion.getDeathsProportion());
        testPositiveRateTxt.setText(displayRegion.getTestPositivityRate());
        fatalityRateTxt.setText(displayRegion.getFatalityRate());

        casesPMTxt.setText(displayRegion.getCasesPerMil());
        deathsPMTxt.setText(displayRegion.getDeathsPerMil());
        testsPMTxt.setText(displayRegion.getTestsPerMil());

        GetCasesData();
    }

    private void DisplayTheme(int frg, int bkg) {
        // sets theme based on country status (i.e. infected/cured)
        int frgColor = getResources().getColor(frg);
        int bkgColor = getResources().getColor(bkg);

        collapsingToolbarLayout.setBackgroundColor(frgColor);
        scrollView.setBackgroundColor(bkgColor);
    }

    private void GetCasesData() {
        if (!displayRegion.HasGraphDataPoints()) {
            // if the data hasn't been fetched, get it (and display after Async finishes)
            getCountryDataAsync = new GetCountryDataAsync();
            getCountryDataAsync.execute();
        }
        else
            // otherwise, display the data
            DisplayAllGraphs();
    }

    private class GetCountryDataAsync extends AsyncTask<Void, Void, Void> {
        static final int TIMEOUT = 1000;
        private boolean imageFetched = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // connects to the individual country page
            // extracts data for graphs, and downloads the flag if necessary

            try {
                final String URL = displayRegion.getURL();
                if (URL.length() > 0) {
                    // connects to that country's page
                    Connection connection = Jsoup.connect(URL).timeout(TIMEOUT);
                    Document document = connection.post();

                    GetCasesData(document);
                    /*if (!displayRegion.hasUniqueFlag())
                        // if the country's flag was not a local asset, it is fetched
                        GetFlagFromPage(document);*/
                }
            } catch (SocketTimeoutException ste) {
                Snackbar.make(getView(), getString(R.string.error_graph_timed_out), Snackbar.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        private void GetFlagFromPage(Document document) throws IOException {
            // downloads flag image from the webpage

            // 0 is the worldometers logo, 1 is the flag
            Element flagImage = document.select("img").get(1);
            String imageUrl = flagImage.absUrl("src");

            URL url = new URL(imageUrl);

            InputStream input = (InputStream)url.getContent();
            Drawable flagDrawable = Drawable.createFromStream(input , "src");

            // store in region object
            displayRegion.setFlagDrawable(flagDrawable);

            imageFetched = true;
        }

        private void GetCasesData(Document document) throws IOException {
            // searches through the page scripts to extract the data arrays
            for (Element script : document.getElementsByTag("script")) {
                for (DataNode dataNode : script.dataNodes()) {
                    String data = dataNode.getWholeData();
                    // parses case and death data arrays for that country
                    // (since each dataset is in a different script, the selection is mutually exclusive)

                    if (data.contains(casesKey))
                        displayRegion.setCasesGraph_Y(GetDataIntArray(data, casesSeriesKey));
                    else if (data.contains(deathsKey))
                        displayRegion.setDeathsGraph_Y(GetDataIntArray(data, deathsSeriesKey));
                    else if (data.contains(dailyCasesKey))
                        displayRegion.setDailyCasesGraph_Y(GetDataIntArray(data, dailyCasesSeriesKey));
                    else if (data.contains(dailyDeathsKey))
                        displayRegion.setDailyDeathsGraph_Y(GetDataIntArray(data, dailyDeathsSeriesKey));
                }
            }
        }

        private int[] GetDataIntArray(String data, String seriesStartMark) {
            // some graphs in the scripts have multiple series (e.g. 3-Day average, 7-Day average)
            // finds the series with the required data
            int seriesIndex = data.indexOf(seriesStartMark);
            // finds the start of the data after that series
            int index = data.indexOf(startMarker, seriesIndex);
            // finds the end of the data array
            int lastIndex = data.indexOf(endMarker, index);

            // isolates the string contents of the data array
            String dataString = data.substring(index + startMarker.length(), lastIndex);

            // splits string into collection of data as strings
            String[] tokens = dataString.split(",");

            // step is the interval used when reducing the number of data points
            // e.g. step = 2 means taking every 2nd data point
            int step = tokens.length / NUM_OF_DATA_POINTS;

            // ensures step is always 1 or more
            step = Math.max(step, 1);

            // integer division disregards any remainder / decimal result
            int[] dataValues = new int[tokens.length / step];

            // converts data strings to ints so they can be shown in a graph
            for (int i = 0; i < dataValues.length; i++) {
                // take every nth data-point, where n is the step size
                // this is because many of the points are redundant and can be ignored
                String token = tokens[i * step];

                if (i > 0)
                    // returns previous value if error occurs
                    dataValues[i] = parseToken(token, dataValues[i-1]);
                else
                    // returns 0 if error occurs
                    dataValues[i] = parseToken(token, 0);
            }

            return dataValues;
        }

        private int parseToken(String token, int errorVal) {
            // errorVal is returned if the token could not be parsed to an int

            if (token == null)
                return errorVal;

            try {
                return Integer.parseInt(token);
            }
            catch (NumberFormatException ex) {
                ex.printStackTrace();
                // catch if the data string could not be parsed into an integer
                return errorVal;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            DisplayAllGraphs();

            if (imageFetched)
                DisplayFlagDrawable();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getCountryDataAsync.cancel(true);
    }

    private void DisplayAllGraphs() {
        DisplayGraph(displayRegion.getCasesGraph_Y(), casesGraphView, Color.CYAN, casesGraphTitle);
        DisplayGraph(displayRegion.getDeathsGraph_Y(), deathsGraphView, Color.RED, deathsGraphTitle);
        DisplayGraph(displayRegion.getDailyCasesGraph_Y(), dailyCasesGraphView, Color.GREEN, dailyCasesGraphTitle);
        DisplayGraph(displayRegion.getDailyDeathsGraph_Y(), dailyDeathsGraphView, Color.YELLOW, dailyDeathsGraphTitle);
    }

    private void DisplayGraph(int[] y_values, GraphView graphView, int colour, TextView graphTitle) {
        if (y_values == null) {
            // if there is no data to display, create some to prevent the graph crashing
            y_values = new int[]{ 0 };
            ToggleGraphVisibility(graphView, graphTitle, false);
        }
        else {
            ToggleGraphVisibility(graphView, graphTitle, true);
        }

        int num_points = y_values.length;

        Point[] points = new Point[num_points];
        int maxVal = y_values[0];

        for (int i = 0; i < points.length; i++) {
            int y = y_values[i];
            // populate a points array with the values
            points[i] = new Point(i, y);
            if (y > maxVal)
                maxVal = y;
        }

        /*double[] yAxisTicks = new double[11];
        int tickAmt = maxVal / 10;
        for (int i = 0; i < yAxisTicks.length; i++) {
            yAxisTicks[i] = i * tickAmt;
        }

        int yAxisLabelChars = Integer.toString(maxVal).length();
        double pixelsPerChar = num_points / (double)30;
        double xMin = -pixelsPerChar * yAxisLabelChars;
        */

        Graph casesGraph = new Graph.Builder()
                .addLineGraph(points, colour)
                .setWorldCoordinates(0, num_points, 0, 1.1 * maxVal)
                .setBackgroundColor(Color.TRANSPARENT)
                .setAxesColor(Color.WHITE)
                .setXTicks(new double[]{})
                .setYTicks(new double[]{})
                .build();
        graphView.setGraph(casesGraph);
    }

    private void ToggleGraphVisibility(GraphView graphView, TextView graphTitle, boolean toShow) {
        Animation ANIMATION;

        if (toShow) {
            graphView.setVisibility(View.VISIBLE);
            graphTitle.setVisibility(View.VISIBLE);
            ANIMATION = AnimationUtils.loadAnimation(getContext(), R.anim.anim_graph_appear);
        }
        else {
            ANIMATION = AnimationUtils.loadAnimation(getContext(), R.anim.anim_graph_disappear);
            // listener to hide graph when animation finished
            ANIMATION.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) { }

                @Override
                public void onAnimationEnd(Animation animation) {
                    graphView.setVisibility(View.GONE);
                    graphTitle.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animation animation) { }
            });
        }

        graphView.startAnimation(ANIMATION);
        graphTitle.startAnimation(ANIMATION);
    }
}
