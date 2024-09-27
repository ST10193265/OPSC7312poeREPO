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
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUsername

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

    // Getter and Setter methods for UI components
    fun getSpinnerLanguage(): Spinner {
        return spinnerLanguage
    }

    fun setSpinnerLanguage(spinner: Spinner) {
        this.spinnerLanguage = spinner
    }

    fun getSpinnerDistanceUnits(): Spinner {
        return spinnerDistanceUnits
    }

    fun setSpinnerDistanceUnits(spinner: Spinner) {
        this.spinnerDistanceUnits = spinner
    }

    fun getSpinnerDistanceRadius(): Spinner {
        return spinnerDistanceRadius
    }

    fun setSpinnerDistanceRadius(spinner: Spinner) {
        this.spinnerDistanceRadius = spinner
    }

    fun getEtEmail(): EditText {
        return etEmail
    }

    fun setEtEmail(editText: EditText) {
        this.etEmail = editText
    }

    fun getEtPhone(): EditText {
        return etPhone
    }

    fun setEtPhone(editText: EditText) {
        this.etPhone = editText
    }

    fun getBtnSave(): Button {
        return btnSave
    }

    fun setBtnSave(button: Button) {
        this.btnSave = button
    }

    fun getBtnCancel(): Button {
        return btnCancel
    }

    fun setBtnCancel(button: Button) {
        this.btnCancel = button
    }

    fun getIbtnHome(): ImageButton {
        return ibtnHome
    }

    fun setIbtnHome(button: ImageButton) {
        this.ibtnHome = button
    }

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

        // Set up distance units (default to "km")
        val distanceUnits = arrayOf("km", "m")
        val distanceUnitsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, distanceUnits)
        spinnerDistanceUnits.adapter = distanceUnitsAdapter
        spinnerDistanceUnits.setSelection(0) // Default to km

        // Set up distance radius (default to "No Limit")
        val distanceRadius = arrayOf("No Limit", "1 km", "5 km", "10 km", "20 km")
        val distanceRadiusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, distanceRadius)
        spinnerDistanceRadius.adapter = distanceRadiusAdapter
        spinnerDistanceRadius.setSelection(0) // Default to "No Limit"

        // Load and set the saved settings from Firebase
        loadSettings()

        // Handle Home Button navigation
        ibtnHome.setOnClickListener {
            navigateToHome()
        }

        // Handle Save Button click
        btnSave.setOnClickListener {
            saveSettings()
        }

        // Handle Cancel Button click (clear input or navigate back)
        btnCancel.setOnClickListener {
            clearFields()
            navigateToHome()
        }

        return view
    }

    private fun saveSettings() {
        val selectedLanguage = spinnerLanguage.selectedItem.toString()
        val selectedDistanceUnit = spinnerDistanceUnits.selectedItem.toString()
        val selectedDistanceRadius = spinnerDistanceRadius.selectedItem.toString()
        val email = etEmail.text.toString().trim()
        val phoneNumber = etPhone.text.toString().trim()

        val updatedData = mutableMapOf<String, Any>()

        // Add selected language
        updatedData["language"] = if (selectedLanguage == "Afrikaans") "af" else "en"

        // Add selected distance unit
        updatedData["distanceUnit"] = selectedDistanceUnit

        // Add selected distance radius
        updatedData["distanceRadius"] = selectedDistanceRadius

        // Only add email and phone if they're not empty
        if (email.isNotEmpty()) {
            updatedData["email"] = email
        }

        if (phoneNumber.isNotEmpty()) {
            updatedData["phoneNumber"] = phoneNumber
        }

        // Dentist ID (this should be dynamic or fetched accordingly)
        val clientId = loggedInClientUsername

        // Update data in Firebase if there's any change
        if (updatedData.isNotEmpty()) {
            if (clientId != null) {
                updateSettings(clientId, updatedData)
            }
        } else {
            Toast.makeText(requireContext(), "No changes made!", Toast.LENGTH_SHORT).show()
            navigateToHome()
        }
    }

    private fun updateSettings(clientId: String, updatedData: Map<String, Any>) {
        val selectedLanguage = spinnerLanguage.selectedItem.toString()
        database.child("clients/$clientId").updateChildren(updatedData)
            .addOnSuccessListener {
                Toast.makeText(context, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
                // Change app language and recreate activity/fragment
                updateAppLanguage(selectedLanguage)
                navigateToHome()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error updating settings: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateAppLanguage(language: String) {
        val locale = if (language == "Afrikaans") Locale("af") else Locale("en")
        Locale.setDefault(locale)

        val config = requireContext().resources.configuration
        config.setLocale(locale)

        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)

        // Recreate the activity to apply the new language setting
        requireActivity().recreate()
    }


    fun loadSettings() {
        val clientId = loggedInClientUsername // Dynamic ID needed
        database.child("clients/$clientId").get()
            .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val language = dataSnapshot.child("language").value as? String ?: "en"
                    val distanceUnit = dataSnapshot.child("distanceUnit").value as? String ?: "km"
                    val distanceRadius = dataSnapshot.child("distanceRadius").value as? String ?: "No Limit"
                    val email = dataSnapshot.child("email").value as? String ?: ""
                    val phoneNumber = dataSnapshot.child("phoneNumber").value as? String ?: ""

                    // Set the UI components with the loaded data
                    spinnerLanguage.setSelection(if (language == "af") 1 else 0)
                    spinnerDistanceUnits.setSelection(if (distanceUnit == "km") 0 else 1)
                    spinnerDistanceRadius.setSelection(if (distanceRadius == "No Limit") 0 else distanceRadius.replace(" km", "").toInt() / 5)
                    etEmail.setText(email)
                    etPhone.setText(phoneNumber)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load settings", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        spinnerLanguage.setSelection(0)
        spinnerDistanceUnits.setSelection(0) // Default to km
        spinnerDistanceRadius.setSelection(0) // Default to No Limit
        etEmail.text.clear()
        etPhone.text.clear()
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_nav_settings_client_to_nav_menu_client)
    }
}



