package com.angmolin.livechess2fen;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static CharSequence[] pieces_cnns;
    private static CharSequence[] laps_cnns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        List<CharSequence> piecesCnnsList = listAssetFiles("tflite_models/pieces");
        pieces_cnns = piecesCnnsList.toArray(new CharSequence[piecesCnnsList.size()]);

        List<CharSequence> lapsCnnsList = listAssetFiles("tflite_models/laps");
        laps_cnns = lapsCnnsList.toArray(new CharSequence[lapsCnnsList.size()]);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private List<CharSequence> listAssetFiles(String path) {
        String[] list;
        List<CharSequence> files = new ArrayList<>();

        try {
            list = getAssets().list(path);
            if (list.length > 0) {
                for (String file : list) {
                    if (listAssetFiles(path + "/" + file) == null)
                        return null;
                    else
                        files.add(file);
                }
            }
        }
        catch (IOException e) {
            return null;
        }

        return files;
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            ListPreference lpPieces = findPreference("pieces_cnn");
            lpPieces.setEntries(pieces_cnns);
            lpPieces.setEntryValues(pieces_cnns);

            ListPreference lpLaps = findPreference("laps_cnn");
            lpLaps.setEntries(laps_cnns);
            lpLaps.setEntryValues(laps_cnns);
        }


    }
}