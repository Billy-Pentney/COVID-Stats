package com.app.covidstats.region;

import android.graphics.drawable.Drawable;

import java.util.ArrayList;

public class Country extends Region {
    private final ArrayList<Region> subregions = new ArrayList<>();
    private int currentSubregionIndex = 0;

    private final static String N_A = "N/A";
    private Drawable flag = null;

    private float cases_global_proportion = -1;      // % representing the fraction of global cases
    private float deaths_global_proportion = -1;     // % representing the fraction of global deaths

    private float test_positive_rate = -1;           // approx. % of positive tests
    private float fatality_rate = -1;                // approx. % of cases which result in death

    // the minimum value where the actual value will be displayed
    // e.g. if proportion = 0.01,  it will be displayed as 0.01%
    //      if proportion = 0.009, it will be displayed as <0.01%
    private static final double minProportionValue = 0.01;

    private String url = "";
    // data values used when displaying graphs in DetailFragment
    private int[] casesGraph_Y = null;
    private int[] deathsGraph_Y = null;
    private int[] dailyCasesGraph_Y = null;
    private int[] dailyDeathsGraph_Y = null;


    public Country(String name, String population) {
        super(name, population);
    }
}
