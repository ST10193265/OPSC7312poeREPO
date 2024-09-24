package com.example.poe2.ui.maps_client

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.BuildConfig
import com.example.poe2.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList

class MapsClientFragment : Fragment() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var spinnerDentists: Spinner
    private lateinit var btnGoNow: Button
    private lateinit var directionsTextView: TextView // TextView to display directions
    private var destinationLatLng: LatLng? = null
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_maps_client, container, false)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Places API
        Places.initialize(requireContext(), apiKey)

        // Initialize the Spinner
        spinnerDentists = view.findViewById(R.id.spinnerDentists)
        fetchDentistData()

        btnGoNow = view.findViewById(R.id.btnGoNow)
        directionsTextView = view.findViewById(R.id.directionsTextView) // Initialize TextView for directions

        // Home button
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_maps_client_to_nav_menu_client)
        }

        // Set click listener for Go Now button
        btnGoNow.setOnClickListener {
            if (destinationLatLng != null) {
                getDirections(destinationLatLng!!)
                Toast.makeText(requireContext(), "Fetching directions...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Please select a destination", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun fetchDentistData() {
        val dentistList = mutableListOf<String>()
        val addressMap = mutableMapOf<String, String>()
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("dentists")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (dentistSnapshot in dataSnapshot.children) {
                    val name = dentistSnapshot.child("name").getValue(String::class.java) ?: continue
                    val address = dentistSnapshot.child("address").getValue(String::class.java) ?: continue

                    dentistList.add(name)
                    addressMap[name] = address
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, dentistList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerDentists.adapter = adapter

                spinnerDentists.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                        val selectedDentist = dentistList[position]
                        val selectedAddress = addressMap[selectedDentist]
                        selectedAddress?.let { findPlace(it) }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Failed to load dentist data: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun findPlace(addressString: String) {
        val geocoder = Geocoder(requireContext())
        try {
            val addressList = geocoder.getFromLocationName(addressString, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                destinationLatLng = LatLng(address.latitude, address.longitude)
            } else {
                Toast.makeText(requireContext(), "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Geocoder service is not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDirections(destination: LatLng) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val originLatLng = LatLng(location.latitude, location.longitude)

                // Add `units=metric` to use kilometers/meters
                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${originLatLng.latitude},${originLatLng.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&units=metric" +  // This will return distances in km and meters
                        "&key=$apiKey"
                fetchDirections(url)
            } else {
                Log.d("MapsClientFragment", "Unable to get current location")
            }
        }.addOnFailureListener {
            Log.d("MapsClientFragment", "Failed to retrieve location.")
        }
    }

    private fun fetchDirections(url: String) {
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to fetch directions: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Response not successful: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                response.body?.let { responseBody ->
                    val jsonData = responseBody.string()
                    try {
                        val jsonObject = JSONObject(jsonData)
                        val routes = jsonObject.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val legs = route.getJSONArray("legs")
                            val directions = StringBuilder()

                            for (i in 0 until legs.length()) {
                                val steps = legs.getJSONObject(i).getJSONArray("steps")
                                for (j in 0 until steps.length()) {
                                    val step = steps.getJSONObject(j)

                                    // Extract instruction and distance
                                    val instruction = Html.fromHtml(step.getString("html_instructions")).toString()
                                    val distance = step.getJSONObject("distance").getString("text")
                                    val maneuver = step.optString("maneuver", "")
                                    val icon = when (maneuver) {
                                        "turn-slight-left" -> "↖️"
                                        "turn-left" -> "⬅️"
                                        "turn-sharp-left" -> "↙️"
                                        "uturn-left" -> "↩️"
                                        "turn-slight-right" -> "↗️"
                                        "turn-right" -> "➡️"
                                        "turn-sharp-right" -> "↘️"
                                        "uturn-right" -> "↪️"
                                        "straight" -> "⬆️"
                                        "ramp-left" -> "↖️"
                                        "ramp-right" -> "↗️"
                                        "fork-left" -> "↖️"
                                        "fork-right" -> "↗️"
                                        "merge" -> "🛣️"
                                        "roundabout-left" -> "↪️"
                                        "roundabout-right" -> "↩️"
                                        else -> "⬆️"
                                    }

                                    // Use regex to extract street names from instructions
                                    val streetNameRegex = Regex("onto ([^<]+)")
                                    val streetNameMatch = streetNameRegex.find(instruction)
                                    val streetName = streetNameMatch?.groups?.get(1)?.value ?: "Unknown street"

                                    // Append instruction with street name and distance
                                    directions.append("$icon $instruction ($distance)").append("\n")
                                    if (streetName != "Unknown street") {
                                        directions.append("Street: $streetName").append("\n")
                                    }
                                    directions.append("\n")
                                }
                            }

                            requireActivity().runOnUiThread {
                                directionsTextView.text = directions.toString()
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "No routes found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: JSONException) {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Error parsing JSON", Toast.LENGTH_SHORT).show()
                        }
                    }
                } ?: run {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Response body is null", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }



    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
}




