package com.example.poe2.ui.maps_client

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory
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
import java.util.Locale

class MapsClientFragment : Fragment(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var spinnerDentists: Spinner
    private lateinit var btnGoNow: Button
    private lateinit var map: GoogleMap
    private var destinationLatLng: LatLng? = null
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var textViewDirection: TextView  // New TextView for directions
    private lateinit var tts: TextToSpeech  // Text-to-Speech instance
    private var currentStep: Int = 0
    private val steps = mutableListOf<String>()  // Store directions steps

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_maps_client, container, false)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Places API
        Places.initialize(requireContext(), apiKey)

        // Initialize UI components
        spinnerDentists = view.findViewById(R.id.spinnerDentists)
        btnGoNow = view.findViewById(R.id.btnGoNow)
        textViewDirection = view.findViewById(R.id.textViewDirection)  // Initialize the TextView

        // Initialize Text-to-Speech
        tts = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US  // Set language for TTS
            }
        }

        // Initialize Map Fragment
        mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Home button
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_maps_client_to_nav_menu_client)
        }

        // Fetch dentist data
        fetchDentistData()

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
            if (addressList != null) {
                if (addressList.isNotEmpty()) {
                    val address = addressList[0]
                    destinationLatLng = LatLng(address.latitude, address.longitude)
                    // Add a marker for the destination
                    map.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination: $addressString"))
                    // Move and animate the camera to the destination
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 22f))

                } else {
                    Toast.makeText(requireContext(), "Address not found", Toast.LENGTH_SHORT).show()
                }
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
                val url = buildDirectionsUrl(originLatLng, destination)
                fetchDirections(url)
            } else {
                Log.d("MapsClientFragment", "Unable to get current location")
            }
        }.addOnFailureListener {
            Log.d("MapsClientFragment", "Failed to retrieve location.")
            Toast.makeText(requireContext(), "Failed to retrieve location", Toast.LENGTH_SHORT).show()
        }
    }


    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun buildDirectionsUrl(origin: LatLng, destination: LatLng): String {
        val str_origin = "origin=${origin.latitude},${origin.longitude}"
        val str_dest = "destination=${destination.latitude},${destination.longitude}"
        val sensor = "sensor=false"
        return "https://maps.googleapis.com/maps/api/directions/json?$str_origin&$str_dest&$sensor&key=$apiKey"
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
                if (response.isSuccessful) {
                    response.body?.let { responseBody ->
                        val jsonData = responseBody.string()
                        requireActivity().runOnUiThread {
                            parseDirectionsJson(jsonData)
                        }
                    } ?: run {
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Response body is null", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Response not successful: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun parseDirectionsJson(jsonData: String) {
        try {
            val jsonObject = JSONObject(jsonData)
            val routes = jsonObject.getJSONArray("routes")

            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val legs = route.getJSONArray("legs")
                steps.clear()  // Clear previous steps

                // Get the duration and display it
                val duration = legs.getJSONObject(0).getJSONObject("duration")
                val durationText = duration.getString("text") // e.g., "30 mins"
                textViewDirection.text = "Estimated travel time: $durationText\n" // Set estimated time in TextView

                for (i in 0 until legs.length()) {
                    val stepArray = legs.getJSONObject(i).getJSONArray("steps")
                    for (j in 0 until stepArray.length()) {
                        val step = stepArray.getJSONObject(j)
                        val instruction = step.getString("html_instructions").replace("<[^>]*>".toRegex(), "") // Remove HTML tags
                        steps.add(instruction)  // Add instructions to the list
                    }
                }

                // Start navigating by giving the first instruction
                if (steps.isNotEmpty()) {
                    textViewDirection.append(steps[currentStep])  // Set the first direction
                    speakOut(steps[currentStep])  // Voice direction
                }

                // Draw the polyline on the map
                val points = decodePoly(route.getJSONObject("overview_polyline").getString("points"))
                drawPolyline(points)
            } else {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "No routes found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: JSONException) {
            requireActivity().runOnUiThread {
                Toast.makeText(requireContext(), "Error parsing JSON: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun drawPolyline(points: List<LatLng>) {
        if (::map.isInitialized && points.isNotEmpty()) {
            val polylineOptions = PolylineOptions()
                .addAll(points)
                .width(12f)  // Increase the width
                .color(Color.RED)  // Change to a more visible color
                .geodesic(true)

            // Add the polyline to the map
            map.addPolyline(polylineOptions)

            // Move the camera to the starting point of the route
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points[0], 15f))
        }
    }

    private fun speakOut(text: String) {
        if (text.isNotEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)  // Speak the direction
        }
    }

    private fun onLocationUpdate(location: Location) {
        // Update the user's current location on the map (if needed)
        val currentLatLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 22f)) // Zoom in closer to the user's location

    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.isZoomControlsEnabled = true  // Enable zoom controls
        checkLocationPermission()  // Check for location permission and enable user location
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    onLocationUpdate(it)  // Update the user's location
                }
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission()  // Retry checking location permission
        }
    }
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0

            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) -(result shr 1) else (result shr 1)
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) -(result shr 1) else (result shr 1)
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::tts.isInitialized) {
            tts.stop()  // Stop Text-to-Speech if initialized
            tts.shutdown()  // Shutdown the TTS instance
        }
    }
}


