package com.app.covidstats.region;

import android.content.Context;
import android.graphics.drawable.Drawable;

import com.app.covidstats.R;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class Region {
    private final ArrayList<Region> subregions = new ArrayList<>();
    private int currentSubregionIndex = 0;

    private final static String N_A = "N/A";
    private Drawable flag = null;

    private String parentRegionName = "";

    private String displayName;
    private final String name;
    private final int population;

    protected int total_cases = 0;
    protected int total_deaths = 0;
    private int total_tests = -1;
    private int active_cases = -1;          // number of unresolved cases
    private int critical_cases = -1;        // number of patients currently in serious/critical condition
    private int total_recovered = -1;       // number of cases where the patient recovered
    private float cases_per_mil = -1;       // total cases per 1 million people
    private float deaths_per_mil = -1;      // total deaths per 1 million people
    private float tests_per_mil = -1;
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

    //private int total_cases_change = 0;
    //private int total_deaths_change = 0;

    public static String null_flag_name = "null";
    private boolean hasUniqueFlag = false;

    public Region(String name, String population) {
        this.name = extractRegionName(name);
        setDisplayName(name);
        this.population = StringToInt(population);
    }

    private String extractRegionName(String name) {
        // This is a case where the scraped data does not match the expected country name
        // so we have to manually set its name
        if (name.equals("DPRK"))
            return "n. korea";
        return name;
    }

    public void setDisplayName(String name) {
        this.displayName = name.replace(" and ", " & ");

        // The dataset uses short versions of these names, so this
        // function replaces those cases with the full names
        final String CAR_FULL = "Central African Republic";
        final String DRC_FULL = "Democratic Republic of the Congo";
        final String N_KOREA_FULL = "North Korea";
        final String S_KOREA_FULL = "South Korea";
        final String ST_VINCENT_FULL = "St. Vincent & the Grenadines";
        final String UAE_FULL = "United Arab Emirates";

        // cases which improve readability of countries
        switch (displayName) {
            case "CAR":
                displayName = CAR_FULL;        break;
            case "DRC":
                displayName = DRC_FULL;        break;
            case "DPRK":
            case "N. Korea":
                displayName = N_KOREA_FULL;
                break;
            case "S. Korea":
                displayName = S_KOREA_FULL;    break;
            case "St. Vincent Grenadines":
                displayName = ST_VINCENT_FULL; break;
            case "UAE":
                displayName = UAE_FULL;        break;
        }
    }

    public static Comparator<Region> getComparatorFromMenuID(int i) {
        switch (i) {
            case R.id.submenu_cases:
                return CasesComparator;
            case R.id.submenu_deaths:
                return DeathsComparator;
            case R.id.submenu_cases_pm:
                return CasesPMComparator;
            case R.id.submenu_deaths_pm:
                return DeathsPMComparator;
            case R.id.submenu_name:
                return NameComparator;
            case R.id.submenu_population:
                return PopulationComparator;
            default:
                // default sort type is cases
                return CasesComparator;
        }
    }

    public static Comparator<Region> NameComparator = (c1, c2) -> {
        // sort by region name ascending
        String firstName = c1.displayName;
        String secondName = c2.displayName;
        return firstName.compareTo(secondName);
    };
    public static Comparator<Region> CasesComparator = (c1, c2) -> {
        // sort by cases descending
        return Integer.compare(c2.total_cases, c1.total_cases);
    };
    public static Comparator<Region> PopulationComparator = (c1, c2) -> {
        // sort by population descending
        return Integer.compare(c2.population, c1.population);
    };
    public static Comparator<Region> DeathsComparator = (c1, c2) -> {
        // sort by population descending
        return Integer.compare(c2.total_deaths, c1.total_deaths);
    };
    public static Comparator<Region> CasesPMComparator = (c1, c2) -> {
        // sort by population descending
        return Float.compare(c2.cases_per_mil, c1.cases_per_mil);
    };
    public static Comparator<Region> DeathsPMComparator = (c1, c2) -> {
        // sort by population descending
        return Float.compare(c2.deaths_per_mil, c1.deaths_per_mil);
    };

    private Drawable GetDrawable(Context context) {
        Drawable d = GetDrawableFromName(context, name);
        // Indicates that this region has a name which corresponds to a drawable flag
        hasUniqueFlag = (d != null);

        // If the requested resource could not be found,
        // then load the null flag image
        if (!hasUniqueFlag) {
            d = GetDrawableFromName(context, null_flag_name);
        }

        return d;
    }

    public static Drawable GetDrawableFromName(Context context, String flagName) {
        final String GIF = ".gif";
        final String JPG = ".jpg";
        final String PNG = ".png";
        Drawable d = null;
        String dir = "flags/";

        try {
            String address = dir + flagName.toLowerCase() + PNG;
            InputStream ims = context.getAssets().open(address);
            d = Drawable.createFromStream(ims, null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return d;
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

    public boolean HasGraphDataPoints() {
        return (casesGraph_Y != null) &&
               (deathsGraph_Y != null) &&
               (dailyCasesGraph_Y != null) &&
               (dailyDeathsGraph_Y != null);
    }

    public boolean hasUniqueFlag() { return hasUniqueFlag; }

    public void setFlagDrawable(Drawable d) {
        flag = d;
        hasUniqueFlag = true;
    }

    public String getParentName()
    {
        return parentRegionName;
    }
    public Drawable getFlag(Context context) {
        if (flag == null)
            flag = GetDrawable(context);
        return flag;
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
    public String getCasesProportion() { return FormatAsPercent(cases_global_proportion); }
    public String getDeathsProportion() { return FormatAsPercent(deaths_global_proportion); }
    public String getTestPositivityRate() {
        return FormatAsPercent(test_positive_rate);
    }
    public String getFatalityRate() {
        return FormatAsPercent(fatality_rate);
    }
    public String getURL() {
        return url;
    }

    public int getTotalCasesInt() { return total_cases; }
    public int getTotalDeathsInt() { return total_deaths; }

    public void setParentRegionName(String parentRegionName) {
        if (this.parentRegionName.equals(""))
            this.parentRegionName = parentRegionName;
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
    public void setProportionsOfParent(int global_cases, int global_deaths) {
        this.cases_global_proportion = GetProportionFloat(total_cases, global_cases);
        this.deaths_global_proportion = GetProportionFloat(total_deaths, global_deaths);
    }
    public void setURL(String url) {
        this.url = url;
    }

    private float GetProportionFloat(int numerator, int denominator) {
        final int dp = 2;                           // number of decimal places in the result
        float powOfTen = (float)Math.pow(10, dp);

        /*
             Calculates the proportion that the local (regional) value is
             out of the total (global stats), as a percentage

             1) Multiply by 10^n to get n s.f.
             2) Round to truncate any further decimals
             3) Divide by 10^n to get a float with n d.p.
        */

        float rawVal = (float)numerator / denominator;
        return Math.round(powOfTen * rawVal) / powOfTen;
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

    public boolean isValid() {
        // regions are only considered "valid" if their population is > 0
        // this means that temporary locations (e.g. ships) are ignored
        return population > 0;
    }
    public boolean isRecovered() {
        return active_cases == 0;
    }

    public int[] getCasesGraph_Y() { return casesGraph_Y; }
    public int[] getDeathsGraph_Y() { return deathsGraph_Y; }
    public int[] getDailyCasesGraph_Y() { return dailyCasesGraph_Y; }
    public int[] getDailyDeathsGraph_Y() { return dailyDeathsGraph_Y; }

    public void setCasesGraph_Y(int[] newData) { this.casesGraph_Y = newData; }
    public void setDeathsGraph_Y(int[] newData) { this.deathsGraph_Y = newData; }
    public void setDailyCasesGraph_Y(int[] newData) { this.dailyCasesGraph_Y = newData; }
    public void setDailyDeathsGraph_Y(int[] newData) { this.dailyDeathsGraph_Y = newData; }

    public void updateCases(int cases) {
        //this.total_cases_change = cases - this.total_cases;
        this.total_cases = cases;
    }
    public void updateDeaths(int deaths) {
        //this.total_deaths_change = deaths - this.total_deaths;
        this.total_deaths = deaths;
    }

    public ArrayList<Region> getSubregions() {
        return subregions;
    }

    public void addSubRegions(ArrayList<Region> regionsToAdd) {
        subregions.addAll(regionsToAdd);
    }

    public ArrayList<Region> ParseRegionsFromTable(Element table, int startRow) {
        ArrayList<Region> parsedSubregions = new ArrayList<>();
        Elements rows = table.select("tr");

        for (int i = startRow; i < rows.size(); i++) {
            Element row = rows.get(i);
            Region thisRegion = ParseRowToRegion(row, this.total_cases, this.total_deaths);

            if (thisRegion.isValid())
                parsedSubregions.add(thisRegion);
        }

        return parsedSubregions;
    }

    public static Region ParseRowToRegion(Element row, int parent_cases, int parent_deaths) {
        Elements cols = row.select("td");

            /*                  table headers by column index
                        0 = Number                   8 = Active Cases
                        1 = Name / URL               9 = Serious/Critical Cases
                        2 = Cases                   10 = Cases Per Million
                        3 = New Cases               11 = Deaths Per Million
                        4 = Deaths                  12 = Tests
                        5 = New Deaths              13 = Tests Per Million
                        6 = Resolved                14 = Population
                        7 = New Resolved            15 = Continent                          */

        String regionName = cols.get(1).text();
        String population = cols.get(14).text();

        Region thisRegion = new Region(regionName, population);

        if (cols.size() > 14) {
            String continent = cols.get(15).text();
            thisRegion.setParentRegionName(continent);
        }

        String total_cases = cols.get(2).text();
        String total_deaths = cols.get(4).text();
        String total_recovered = cols.get(6).text();
        String active_cases = cols.get(8).text();
        String critical_cases = cols.get(9).text();
        String cases_per_mil = cols.get(10).text();
        String deaths_per_mil = cols.get(11).text();
        String total_tests = cols.get(12).text();
        String tests_per_mil = cols.get(13).text();

        thisRegion.setCases(total_cases, active_cases, critical_cases, cases_per_mil);
        thisRegion.setResolved(total_recovered, total_deaths, deaths_per_mil);
        thisRegion.setTests(total_tests, tests_per_mil);

        if (parent_cases == -1)
            parent_cases = thisRegion.getTotalCasesInt();
        if (parent_deaths == -1)
            parent_deaths = thisRegion.getTotalDeathsInt();

        thisRegion.setProportionsOfParent(parent_cases, parent_deaths);

        // gets the country-specific url so that graph data can be extracted later
        Elements elements = cols.get(1).getElementsByClass("mt_a");
        if (elements.size() > 0)
            thisRegion.setURL(elements.first().absUrl("href"));

        return thisRegion;
    }

    public boolean hasSubregions() {
        return subregions.size() > 0;
    }

    public Region getCurrentSubregion() {
        if (IsValidIndex(currentSubregionIndex))
            return subregions.get(currentSubregionIndex);
        return null;
    }
    public void setCurrentSubregionIndex(int i) {
        if (IsValidIndex(i))
            currentSubregionIndex = i;
    }

    public boolean IsValidIndex(int i) {
        return i > -1 && i < subregions.size();
    }

    public void SortSubregions() {
        Collections.sort(subregions, Region.CasesComparator);
    }

    public static Region createWorld() {
        return new Region("World", "0");
    }
}
