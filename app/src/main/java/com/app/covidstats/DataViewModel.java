package com.app.covidstats;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.app.covidstats.region.Region;

import java.util.ArrayList;

public class DataViewModel extends ViewModel {
    private final MutableLiveData<String> global_cases_change = new MutableLiveData<>();
    private final MutableLiveData<String> global_deaths_change = new MutableLiveData<>();
    private final MutableLiveData<String> lastUpdatedString = new MutableLiveData<>();
    private final MutableLiveData<Region> worldLiveData = new MutableLiveData<>();
    private int currentSortMode = R.id.submenu_cases;

    public DataViewModel() {
        global_cases_change.setValue("");
        global_deaths_change.setValue("");
        lastUpdatedString.setValue("");
        worldLiveData.setValue(Region.createWorld());
    }

    public int getCurrentSortMode() { return currentSortMode; }
    public void setCurrentSortMode(int id) { currentSortMode = id; }

    public void InitialiseWorld(Region worldData) {
        int diffCases = GetDifference(GetGlobalCases(), worldData.getTotalCasesInt());
        UpdateGlobalCasesChange(diffCases);

        int diffDeaths = GetDifference(GetGlobalDeaths(), worldData.getTotalDeathsInt());
        UpdateGlobalDeathsChange(diffDeaths);

        worldData.SortSubregions();
        worldLiveData.postValue(worldData);
    }
    public void InitWorld(int cases, int deaths) {
        GetWorldDetails().updateCases(cases);
        GetWorldDetails().updateDeaths(deaths);
    }

    public ArrayList<Region> getCountries()
    {
        return GetWorldDetails().getSubregions();
    }

    public Region GetWorldDetails() {
        return worldLiveData.getValue();
    }
    public MutableLiveData<Region> GetWorldLiveData() {
        return worldLiveData;
    }

    private int GetDifference(int oldVal, int newVal) {
        int diff = newVal - oldVal;

        // returns 0 at a minimum, the difference if it's greater than 0
        return Math.max(diff, 0);
    }
    public void SetLastUpdated(String newUpdated)
    {
        lastUpdatedString.setValue(newUpdated);
    }
    public String GetLastUpdated() {
        return lastUpdatedString.getValue();
    }
    public MutableLiveData<String> GetLastUpdatedLiveData() { return lastUpdatedString; }

    public int GetGlobalCases() {
        return GetWorldDetails().getTotalCasesInt();
    }
    public int GetGlobalDeaths() {
        return GetWorldDetails().getTotalDeathsInt();
    }

    public void UpdateGlobalCasesChange(int casesChange) {
        String changeStr = "";

        if (casesChange > 0)
            changeStr = "+" + Region.IntToString(casesChange);

        global_cases_change.setValue(changeStr);
    }
    public void UpdateGlobalDeathsChange(int deathsChange) {
        String changeStr = "";

        if (deathsChange > 0)
            changeStr = "+" + Region.IntToString(deathsChange);

        global_deaths_change.setValue(changeStr);
    }
    public MutableLiveData<String> GetGlobalCasesChangeLiveData() {
        return global_cases_change;
    }
    public MutableLiveData<String> GetGlobalDeathsChangeLiveData() {
        return global_deaths_change;
    }

    public void setCurrentSubregionIndex(int i)
    {
        GetWorldDetails().setCurrentSubregionIndex(i);
    }
}
