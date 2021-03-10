package com.app.covidstats.region;

import android.content.Context;
import android.graphics.drawable.Drawable;

import java.util.Locale;

public class Location {
    private final static String N_A = "N/A";

    protected String displayName;
    protected String name;
    protected int population;

    protected int total_cases = 0;
    protected int total_deaths = 0;
    protected int total_tests = -1;
    protected int active_cases = -1;          // number of unresolved cases
    protected int critical_cases = -1;        // number of patients currently in serious/critical condition
    protected int total_recovered = -1;       // number of cases where the patient recovered
    protected float cases_per_mil = -1;       // total cases per 1 million people
    protected float deaths_per_mil = -1;      // total deaths per 1 million people
    protected float tests_per_mil = -1;       // total tests per 1 million people
    protected float test_positive_rate = -1;           // approx. % of positive tests
    protected float fatality_rate = -1;                // approx. % of cases which result in death

    // the minimum value where the actual value will be displayed
    // e.g. if proportion = 0.01,  it will be displayed as 0.01%
    //      if proportion = 0.009, it will be displayed as <0.01%
    protected static final double minProportionValue = 0.01;

    public Location(String name) {
        this.name = name;
    }

    public String getName()
    {
        return displayName;
    }
    public String getPopulation()
    {
        return IntToString(population);
    }
    public String getTotalCases()
    {
        return IntToString(total_cases);
    }
    public String getTotalDeaths()
    {
        return IntToString(total_deaths);
    }
    public String getTotalTests()
    {
        return IntToString(total_tests);
    }
    public String getActiveCases() {
        return IntToString(active_cases);
    }
    public String getCriticalCases() {
        return IntToString(critical_cases);
    }
    public String getRecoveredCases() {
        return IntToString(total_recovered);
    }
    public String getCasesPerMil() {
        return FloatToString(cases_per_mil);
    }
    public String getDeathsPerMil() {
        return FloatToString(deaths_per_mil);
    }
    public String getTestsPerMil() {
        return FloatToString(tests_per_mil);
    }
    public String getTestPositivityRate() {
        return FormatAsPercent(test_positive_rate);
    }
    public String getFatalityRate() {
        return FormatAsPercent(fatality_rate);
    }

    public void setCases(String total, String active, String critical, String cases_per_mil) {
        this.total_cases = StringToInt(total);
        this.active_cases = StringToInt(active);
        this.critical_cases = StringToInt(critical);
        this.cases_per_mil = StringToFloat(cases_per_mil);
    }
    public void setResolved(String recovered, String deaths, String deaths_per_mil) {
        // resolved cases = deaths + recoveries (i.e. cases which have been concluded)

        this.total_deaths = StringToInt(deaths);
        this.deaths_per_mil = StringToFloat(deaths_per_mil);     // round in case of 0.4
        this.total_recovered = StringToInt(recovered);

        // this is a best-guess to the resolved cases, by looking at the death rate for all cases
        int resolved_cases = total_cases;

        if (total_recovered > -1) {
            // resolved cases can either be a death or a recovery
            resolved_cases = total_recovered + total_deaths;
            // so the fatality rate should technically be total_deaths/resolved_cases
        }

        // fatality = num of deaths / num of concluded cases
        this.fatality_rate = GetProportionFloat(total_deaths, resolved_cases);
    }
    public void setTests(String tests, String tests_per_mil) {
        this.total_tests = StringToInt(tests);
        this.tests_per_mil = StringToFloat(tests_per_mil);

        if (this.total_tests == 0)
            this.total_tests = -1;
        if (this.tests_per_mil == 0)
            this.tests_per_mil = -1;

        if (this.total_tests > 0) {
            // positive tests / num of tests
            this.test_positive_rate = GetProportionFloat(this.total_cases, this.total_tests);
        }
    }

    private String FormatAsPercent(float value) {
        if (value >= 0 && value <= 100) {
            if (value >= minProportionValue)
                // e.g. 3.21 -> "3.21%"
                return value + "%";
            else
                // e.g. 0.005 -> "<0.01%"
                return "<" + minProportionValue + "%";
        }
        else {
            // e.g. inf -> "N/A" or -1 -> "N/A"
            return "N/A";
        }
    }
    private float GetProportionFloat(int numerator, int denominator) {
        final int dp = 2;                           // number of decimal places in the result
        float multiplier = (float)Math.pow(10, dp);

            /*
             calculates the proportion that the local (regional) value is
             out of the total (global stats), as a percentage

             1) Multiply by 100 to get percentage,
             2) Multiply by 10^n to get n s.f.
             3) Round to truncate any further decimals
             4) Divide to get a float with n d.p.
            */

        float rawVal = (float)numerator / denominator;
        return Math.round(100 * multiplier * rawVal) / multiplier;
    }

    public static String IntToString(int number) {
        if (number < 0)
            return N_A;
        return String.format(Locale.getDefault(), "%,d", number);
    }
    public static int StringToInt(String string) {
        string = string.trim();
        int output = 0;

        if (string.length() > 0) {
            string = string.replaceAll(",", "").trim();

            try {
                output = Integer.parseInt(string);
            }
            catch (NumberFormatException e) {
                return -1;
            }
        }

        return output;
    }
    public static float StringToFloat(String string) {
        string = string.trim();
        float outputFloat = 0;

        if (string.length() > 0) {
            string = string.replaceAll(",", "").trim();

            try {
                outputFloat = Float.parseFloat(string);
            }
            catch (NumberFormatException e) {
                return 0;
            }
        }

        return outputFloat;
    }
    public static String FloatToString(float number) {
        if (number < 0)
            return N_A;

        // e.g. number = 12.23
        //      roundNum = 12
        //      decimalStr  ->  12.23 - 12  ->  "0.23"  ->  ".23"
        //      integerStr = "12"
        //      return: "12" + ".23" = "12.23"

        int roundNum = (int)number;
        float decimal = number - roundNum;

        if (decimal == 0)
            // if whole number, just parse integer part
            return IntToString(roundNum);

        String decimalStr = Float.toString(decimal).substring(1);
        String integerStr = String.format(Locale.getDefault(), "%,d", roundNum);

        // parse integer and decimal parts separately and concatenate
        return integerStr + decimalStr;
    }
}
