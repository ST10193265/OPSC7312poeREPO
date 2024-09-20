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
import android.widget.Toast
import androidx.navigation.fragment.findNavController

import com.example.poe2.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_client_settings, container, false)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://opsc7312database-default-rtdb.firebaseio.com/").reference

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

        // Load and set the saved language preference from Firebase
        loadLanguagePreference()

        // Handle Home Button navigation
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }

        // Handle Save Button click
        btnSave.setOnClickListener {
            // Get the selected language
            val selectedLanguage = spinnerLanguage.selectedItem.toString()
            changeAppLanguage(selectedLanguage)

            // Save the selected language to Firebase
            updateSettings(selectedLanguage)

            // Navigate back to the menu
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }

        // Handle Cancel Button click (clear input or navigate back)
        btnCancel.setOnClickListener {
            clearFields()
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
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

    // Function to update settings (save language preference to Firebase)
    private fun updateSettings(language: String) {
        val languageCode = if (language == "Afrikaans") "af" else "en"

        // Save language preference in Firebase
        database.child("client/settings/language").setValue(languageCode)
            .addOnSuccessListener {
                if (isAdded) { // Check if fragment is still added
                    context?.let {
                        Toast.makeText(it, "Language preference saved!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded) { // Check if fragment is still added
                    context?.let {
                        Toast.makeText(it, "Error saving language: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    // Function to load the saved language preference from Firebase
    private fun loadLanguagePreference() {
        database.child("clients/settings/language").get()
            .addOnSuccessListener { dataSnapshot ->
                val language = dataSnapshot.value as? String ?: "en" // Default to English
                spinnerLanguage.setSelection(if (language == "af") 1 else 0)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load settings", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to clear the fields
    private fun clearFields() {
        spinnerLanguage.setSelection(-1)
        spinnerDistanceUnits.setSelection(-1)
        spinnerDistanceRadius.setSelection(-1)
        etEmail.text.clear()
        etPhone.text.clear()
    }
}


