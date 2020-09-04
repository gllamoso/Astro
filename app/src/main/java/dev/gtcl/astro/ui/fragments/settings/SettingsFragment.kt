package dev.gtcl.astro.ui.fragments.settings

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import dev.gtcl.astro.DARK_SETTINGS_KEY
import dev.gtcl.astro.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            setBackgroundColor(typedValue.data)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val allDarkSettings = resources.getStringArray(R.array.dark_mode_entries)
        val darkModePreference = findPreference<ListPreference>(DARK_SETTINGS_KEY)
        darkModePreference?.setOnPreferenceChangeListener { _, newValue ->
            when (allDarkSettings.indexOf(newValue.toString())) {
                0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            true
        }
    }

}