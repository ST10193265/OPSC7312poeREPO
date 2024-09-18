package com.example.poe2.ui.settings_dentist

import android.content.Context
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

class SettingsDentistFragment : Fragment() {


    // Declare variables for the UI components
    private lateinit var spinnerLanguageD: Spinner
 private lateinit var    etAddress: EditText
    private lateinit var etPhoneD: EditText
    private lateinit var btnSaveD: Button
    private lateinit var btnCancelD: Button
    private lateinit var ibtnHomeD: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings_dentist, container, false)

        // Initialize the UI components
        ibtnHomeD = view.findViewById(R.id.ibtnHomeD)
        spinnerLanguageD = view.findViewById(R.id.spinnerLanguageD)

        etAddress = view.findViewById(R.id.etAddress)
        etPhoneD = view.findViewById(R.id.etPhoneD)
        btnSaveD = view.findViewById(R.id.btnSaveD)
        btnCancelD = view.findViewById(R.id.btnCancelD)

        // Set up language options (English, Afrikaans)
        val languages = arrayOf("English", "Afrikaans")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        spinnerLanguageD.adapter = languageAdapter

        // Load and set the saved language preference
        val savedLanguage = loadLanguagePreference()
        spinnerLanguageD.setSelection(languages.indexOf(if (savedLanguage == "af") "Afrikaans" else "English"))




        // Handle Home Button navigation
        ibtnHomeD.setOnClickListener {
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }

        // Handle Save Button click
        btnSaveD.setOnClickListener {
            // Get the selected language
            val selectedLanguage = spinnerLanguageD.selectedItem.toString()
            changeAppLanguage(selectedLanguage)


            // Get the entered email and phone
            val Address = etAddress.text.toString()
            val phone = etPhoneD.text.toString()

            updateSettings(selectedLanguage, Address, phone)
            findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)
        }

        // Handle Cancel Button click (clear input or navigate back)
        btnCancelD.setOnClickListener {
            clearFields()
            findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)
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
    private fun updateSettings(language: String, address: String, phone: String) {
        // Save or update user settings logic (to a local database, Firebase, etc.)
        // For example:
        // Save language preference
        // Save distance unit and radius
        // Save email and phone if provided
        if (address.isNotEmpty()) {
            // Update email
        }
        if (phone.isNotEmpty()) {
            // Update phone
        }
    }

    // Function to clear the fields
    private fun clearFields() {
        spinnerLanguageD.setSelection(-1)
           etAddress.text.clear()
        etPhoneD.text.clear()
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