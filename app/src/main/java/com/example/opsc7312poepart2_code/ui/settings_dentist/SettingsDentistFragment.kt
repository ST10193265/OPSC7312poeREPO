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
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class SettingsDentistFragment : Fragment() {

    // Declare variables for the UI components
    private lateinit var spinnerLanguageD: Spinner
    private lateinit var etAddress: EditText
    private lateinit var etPhoneD: EditText
    private lateinit var btnSaveD: Button
    private lateinit var btnCancelD: Button
    private lateinit var ibtnHomeD: ImageButton
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings_dentist, container, false)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://opsc7312database-default-rtdb.firebaseio.com/").reference

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
        loadLanguagePreference()

        // Handle Home Button navigation
        ibtnHomeD.setOnClickListener {
            findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
        }

        // Handle Save Button click
        btnSaveD.setOnClickListener {
            // Get the selected language
            val selectedLanguage = spinnerLanguageD.selectedItem.toString()
            changeAppLanguage(selectedLanguage)

            // Save the selected language to Firebase
            updateSettings(selectedLanguage)

            // Navigate back to the menu
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
    private fun updateSettings(language: String) {
        val languageCode = if (language == "Afrikaans") "af" else "en"

        // Save language preference in Firebase
        database.child("settings/language").setValue(languageCode)
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
        database.child("settings/language").get()
            .addOnSuccessListener { dataSnapshot ->
                val language = dataSnapshot.value as? String ?: "en" // Default to English
                spinnerLanguageD.setSelection(if (language == "af") 1 else 0)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load settings", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to clear the fields
    private fun clearFields() {
        spinnerLanguageD.setSelection(-1)
        etAddress.text.clear()
        etPhoneD.text.clear()
    }
}

