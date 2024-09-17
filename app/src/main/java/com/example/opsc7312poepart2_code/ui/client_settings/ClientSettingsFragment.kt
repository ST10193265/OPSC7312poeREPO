package com.example.poe2.ui.client_settings

import android.content.Context
import android.content.res.Configuration
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.navigation.fragment.findNavController

import com.example.poe2.R
import java.util.Locale


class ClientSettingsFragment : Fragment() {

    // Declare variables for the UI components
    private lateinit var spinnerLanguage: Spinner
    private lateinit var spinnerDistanceUnits: Spinner
    private lateinit var spinnerDistanceRadius: Spinner
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var ibtnHome: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_client_settings, container, false)

        // Initialize the UI components
        ibtnHome = view.findViewById(R.id.ibtnHome)
        spinnerLanguage = view.findViewById(R.id.spinnerLanguage)
        spinnerDistanceUnits = view.findViewById(R.id.spinnerDistanceUnits)
        spinnerDistanceRadius = view.findViewById(R.id.spinnerDistanceRadius)
        etEmail = view.findViewById(R.id.etEmail)
        etPhone = view.findViewById(R.id.etPhone)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)

        // Set up language options (English, Afrikaans)
        val languages = arrayOf("English", "Afrikaans")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        spinnerLanguage.adapter = languageAdapter

        // Load and set the saved language preference
        val savedLanguage = loadLanguagePreference()
        spinnerLanguage.setSelection(languages.indexOf(if (savedLanguage == "af") "Afrikaans" else "English"))


        // Set up distance unit options (Kilometers, Meters)
        val distanceUnits = arrayOf("km", "m")
        val distanceUnitsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, distanceUnits)
        spinnerDistanceUnits.adapter = distanceUnitsAdapter

        // Set up distance unit options (Kilometers, Meters)
        val distanceRadius = arrayOf("10", "20", "30", "40","50","60", "70", "80", "90", "100")
        val distanceRadiusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, distanceRadius)
        spinnerDistanceRadius.adapter = distanceRadiusAdapter


        // Handle Home Button navigation
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }

        // Handle Save Button click
        btnSave.setOnClickListener {
            // Get the selected language
            val selectedLanguage = spinnerLanguage.selectedItem.toString()
            changeAppLanguage(selectedLanguage)

            // Get the selected distance unit and radius
            val selectedDistanceUnit = spinnerDistanceUnits.selectedItem.toString()
            val radius = spinnerDistanceRadius.selectedItem.toString().toIntOrNull() ?: 0

            // Get the entered email and phone
            val email = etEmail.text.toString()
            val phone = etPhone.text.toString()

            updateSettings(selectedLanguage, selectedDistanceUnit, radius, email, phone)
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }

        // Handle Cancel Button click (clear input or navigate back)
        btnCancel.setOnClickListener {
            clearFields()
        }

        return view
    }

    // Function to change the app's language
    private fun changeAppLanguage(language: String) {
        val locale = when (language) {
            "Afrikaans" -> Locale("af")
            else -> Locale("en")
        }

        // Set the locale
        Locale.setDefault(locale)

        // Get resources and configuration
        val resources = requireContext().resources
        val config = resources.configuration

        // Apply the locale to the configuration
        config.setLocale(locale)

        // Update the resources with the new configuration
        resources.updateConfiguration(config, resources.displayMetrics)

        // Restart the activity to apply the language change
        activity?.recreate()
    }


    // Function to update settings (this could save to a database or shared preferences)
    private fun updateSettings(language: String, distanceUnit: String, radius: Int, email: String, phone: String) {
        // Save or update user settings logic (to a local database, Firebase, etc.)
        // For example:
        // Save language preference
        // Save distance unit and radius
        // Save email and phone if provided
        if (email.isNotEmpty()) {
            // Update email
        }
        if (phone.isNotEmpty()) {
            // Update phone
        }
    }

    // Function to clear the fields
    private fun clearFields() {
        spinnerLanguage.setSelection(0)
        spinnerDistanceUnits.setSelection(0)
        spinnerDistanceRadius.setSelection(0)
        etEmail.text.clear()
        etPhone.text.clear()
    }
    private fun saveLanguagePreference(language: String) {
        val sharedPreferences = requireContext().getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("AppLanguage", language)
        editor.apply()
    }

    private fun loadLanguagePreference(): String {
        val sharedPreferences = requireContext().getSharedPreferences("AppSettingsPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("AppLanguage", "af") ?: "af" // Default to English
    }

}
