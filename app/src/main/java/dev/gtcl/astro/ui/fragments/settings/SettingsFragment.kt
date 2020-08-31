package dev.gtcl.astro.ui.fragments.settings

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.PreferenceFragmentCompat
import dev.gtcl.astro.R

class SettingsFragment: PreferenceFragmentCompat() {

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
    }

}