package com.example.poe2.ui.settings_dentist

import android.content.Context
import android.location.Geocoder
import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    // Declare variables for the UI components
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
    private lateinit var mMap: GoogleMap

    // Store the list of place suggestions
    private var placeSuggestions: List<String> = emptyList()
    private var isAddressValid = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings_dentist, container, false)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance("https://opsc7312database-default-rtdb.firebaseio.com/").reference

        // Initialize Places API for autocomplete
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())

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
            findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)  }

        btnSaveD.setOnClickListener {
            // Get the selected language
            val selectedLanguage = spinnerLanguageD.selectedItem.toString()

            // Update the language in Firebase (always update the language)
            updateSettings(selectedLanguage)

            // Change the app's language
            changeAppLanguage(selectedLanguage)

            // Define a variable to hold the updated data for Firebase
            val updatedData = mutableMapOf<String, Any>()

            // Check if the address field is not empty and validate the address
            if (etAddress.text.isNotEmpty()) {
                if (!isAddressValid) {
                    // If the address is not valid, show a message and stop the save process
                    Toast.makeText(requireContext(), "Please select a valid address from the suggestions.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    // If the address is valid, add it to the data to be updated
                    updatedData["address"] = etAddress.text.toString()
                }
            }

            // Check if the phone number field is not empty
            val phoneNumber = etPhoneD.text.toString().trim()
            if (phoneNumber.isNotEmpty()) {
                // Add the phone number to the data to be updated
                updatedData["phoneNumber"] = phoneNumber
            }


            val dentistId = "-O7EXMfOE2RETTbxNHTt"

            // If there's data to update (address or phone number), update it in Firebase
            if (updatedData.isNotEmpty()) {
                // Update the dentist's info in Firebase
                database.child("dentists/$dentistId").updateChildren(updatedData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Settings updated successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate back to the menu
                        findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(requireContext(), "Error updating settings: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // If no address or phone number was entered, just show a success message without updating Firebase
                Toast.makeText(requireContext(), "Settings updated without address/phone number change!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)
            }
        }

        // Handle Cancel Button click (clear input or navigate back)
        btnCancelD.setOnClickListener {
            clearFields()
            findNavController().navigate(R.id.action_nav_settings_dentist_to_nav_menu_dentist)
        }

        // Setup the AutoComplete for the Address field
        setupAutoCompleteForAddress()

        return view
    }

    // Set up Google Places Autocomplete for the Address field
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
                    isAddressValid = false // Reset validity when text changes
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Fetch place suggestions from Google Places API
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

    // Find the selected place and show it on the map
    private fun findPlace(placeName: String) {
        val geocoder = Geocoder(requireContext())
        val addressList = geocoder.getFromLocationName(placeName, 1)
        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            destinationLatLng = LatLng(address.latitude, address.longitude)

            // Check if mMap is initialized before using it
            if (::mMap.isInitialized && destinationLatLng != null) {
                mMap.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 12f))
            } else {
                Toast.makeText(requireContext(), "Map not initialized or no destination found.", Toast.LENGTH_SHORT).show()
            }
        }
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
        database.child("Dentists/settings/language").get()
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

