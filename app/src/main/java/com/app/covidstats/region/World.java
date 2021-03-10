package com.app.covidstats.region;

import java.util.ArrayList;

public class World {
    private ArrayList<Continent> continents = new ArrayList<>();
    private ArrayList<Country> countries = new ArrayList<>();
    private int currentCountryIndex = 0;

    public boolean hasCountries() { return countries.size() > 0; }

    public void setCountries(ArrayList<Country> countries) { this.countries = countries; }

    public ArrayList<Country> getCountries() { return countries; }

    public Region getCurrentCountry() {
        if (IsValidIndex(currentCountryIndex))
            return countries.get(currentCountryIndex);
        return null;
    }

    public void setCurrentCountryIndex(int i) {
        if (IsValidIndex(i))
            currentCountryIndex = i;
    }

    public boolean IsValidIndex(int i) {
        return i > -1 && i < countries.size();
    }
}
