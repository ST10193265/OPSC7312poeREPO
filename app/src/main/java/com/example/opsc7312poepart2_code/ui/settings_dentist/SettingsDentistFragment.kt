package com.example.poe2.ui.settings_dentist

import android.content.Context
import android.location.Geocoder
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistFragment
import com.example.poe2.BuildConfig
import com.example.poe2.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class SettingsDentistFragment : Fragment() {

    private lateinit var spinnerLanguageD: Spinner
    private lateinit var etAddress: AutoCompleteTextView
    private lateinit var etPhoneD: EditText
    private lateinit var btnSaveD: Button
    private lateinit var btnCancelD: Button
    private lateinit var ibtnHomeD: ImageButton
    private lateinit var database: DatabaseReference
    private lateinit var placesClient: PlacesClient
    private var destinationLatLng: LatLng? = null
    private val apiKey = BuildConfig.MAPS_API_KEY
    private var placeSuggestions: List<String> = emptyList()
    private var isAddressValid = false

    // Getters and setters for the fields
    fun getSpinnerLanguageD(): Spinner {
        return spinnerLanguageD
    }

    fun setSpinnerLanguageD(spinner: Spinner) {
        this.spinnerLanguageD = spinner
    }

    fun getEtAddress(): AutoCompleteTextView {
        return etAddress
    }

    fun setEtAddress(autoCompleteTextView: AutoCompleteTextView) {
        this.etAddress = autoCompleteTextView
    }

    fun getEtPhoneD(): EditText {
        return etPhoneD
    }

    fun setEtPhoneD(editText: EditText) {
        this.etPhoneD = editText
    }

    fun getBtnSaveD(): Button {
        return btnSaveD
    }

    fun setBtnSaveD(button: Button) {
        this.btnSaveD = button
    }

    fun getBtnCancelD(): Button {
        return btnCancelD
    }

    fun setBtnCancelD(button: Button) {
        this.btnCancelD = button
    }

    fun getIbtnHomeD(): ImageButton {
        return ibtnHomeD
    }

    fun setIbtnHomeD(imageButton: ImageButton) {
        this.ibtnHomeD = imageButton
    }

    fun getDatabase(): DatabaseReference {
        return database
    }

    fun setDatabase(databaseReference: DatabaseReference) {
        this.database = databaseReference
    }

    fun getPlacesClient(): PlacesClient {
        return placesClient
    }

    fun setPlacesClient(placesClient: PlacesClient) {
        this.placesClient = placesClient
    }

    fun getDestinationLatLng(): LatLng? {
        return destinationLatLng
    }

    fun setDestinationLatLng(latLng: LatLng?) {
        this.destinationLatLng = latLng
    }

    fun getApiKey(): String {
        return apiKey
    }

    fun getPlaceSuggestions(): List<String> {
        return placeSuggestions
    }

    fun setPlaceSuggestions(suggestions: List<String>) {
        this.placeSuggestions = suggestions
    }

    fun isAddressValid(): Boolean {
        return isAddressValid
    }

    fun setAddressValid(isValid: Boolean) {
        this.isAddressValid = isValid
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings_dentist, container, false)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://opsc7312database-default-rtdb.firebaseio.com/").reference

        // Initialize Places API for autocomplete
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())

        // Initialize UI components
        initializeUIComponents(view)

        // Load and set the saved language preference
        loadLanguagePreference()

        // Handle Home Button navigation
        ibtnHomeD.setOnClickListener {
            navigateToHome()
        }

        btnSaveD.setOnClickListener {
            saveSettings()
        }

        btnCancelD.setOnClickListener {
            clearFields()
            navigateToHome()
        }

        // Setup autocomplete for address
        setupAutoCompleteForAddress()

        return view
    }

    private fun initializeUIComponents(view: View) {
        ibtnHomeD = view.findViewById(R.id.ibtnHomeD)
        spinnerLanguageD = view.findViewById(R.id.spinnerLanguageD)
        etAddress = view.findViewById(R.id.etAddress)
        etPhoneD = view.findViewById(R.id.etPhoneD)
        btnSaveD = view.findViewById(R.id.btnSaveD)
        btnCancelD = view.findViewById(R.id.btnCancelD)

        // Set up language options
        val languages = arrayOf("English", "Afrikaans")
        val languageAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages)
        spinnerLanguageD.adapter = languageAdapter
    }

    private fun saveSettings() {
        val selectedLanguage = spinnerLanguageD.selectedItem.toString()
        val updatedData = mutableMapOf<String, Any>()

        if (selectedLanguage.isNotEmpty()) {
            updatedData["language"] = selectedLanguage
        }

        // Validate and update address
        val address = etAddress.text.toString().trim()
        if (address.isNotEmpty()) {
            if (!isAddressValid) {
                Toast.makeText(requireContext(), "Please select a valid address from suggestions.", Toast.LENGTH_SHORT).show()
                return
            } else {
                updatedData["address"] = address
            }
        }

        // Validate and update phone number
        val phoneNumber = etPhoneD.text.toString().trim()
        if (phoneNumber.isNotEmpty()) {
            updatedData["phoneNumber"] = phoneNumber
        }

        // Dentist ID (using the dynamically fetched ID from the logged-in user)
        val dentistId = LoginDentistFragment.loggedInDentistUserId

        // Update data in Firebase if there's any change
        if (dentistId != null && updatedData.isNotEmpty()) {
            updateSettings(dentistId, updatedData)
        } else {
            Toast.makeText(requireContext(), "No changes made!", Toast.LENGTH_SHORT).show()
            navigateToHome()
        }
    }

    private fun updateSettings(dentistId: String, updatedData: Map<String, Any>) {
        val selectedLanguage = spinnerLanguageD.selectedItem.toString()
        database.child("dentists/$dentistId").updateChildren(updatedData)
            .addOnSuccessListener {
                Toast.makeText(context, "Settings updated successfully!", Toast.LENGTH_SHORT).show()
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


    private fun navigateToHome() {
        findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)
    }

    private fun setupAutoCompleteForAddress() {
        val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, emptyList<String>())
        etAddress.setAdapter(autocompleteAdapter)

        etAddress.setOnItemClickListener { parent, _, position, _ ->
            val selectedPlace = parent.getItemAtPosition(position) as String
            isAddressValid = true
            findPlace(selectedPlace)
        }

        etAddress.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    fetchPlaceSuggestions(s.toString())
                    isAddressValid = false
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchPlaceSuggestions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            placeSuggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, placeSuggestions)
            etAddress.setAdapter(adapter)
            adapter.notifyDataSetChanged()
        }
    }

    private fun findPlace(placeName: String) {
        val geocoder = Geocoder(requireContext())
        val addressList = geocoder.getFromLocationName(placeName, 1)
        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            destinationLatLng = LatLng(address.latitude, address.longitude)
            // Handle the location in the map if necessary
        }
    }

    fun loadLanguagePreference() {
        database.child("Dentists/settings/language").get()
            .addOnSuccessListener { dataSnapshot ->
                val language = dataSnapshot.value as? String ?: "en"
                spinnerLanguageD.setSelection(if (language == "af") 1 else 0)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load settings", Toast.LENGTH_SHORT)
                    .show()
            }

    }

        private fun clearFields() {
            spinnerLanguageD.setSelection(0)
            etAddress.setText("")
            etPhoneD.setText("")
        }
    }



