package com.example.poe2.ui.maps_client

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.text.Editable
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

class MapsClientFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var spinnerDentists: Spinner
    private lateinit var btnGoNow: Button
    private lateinit var placesClient: PlacesClient
    private var destinationLatLng: LatLng? = null
    private val apiKey = BuildConfig.MAPS_API_KEY
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_maps_client, container, false)

        // Initialize the map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Initialize Places API
        Places.initialize(requireContext(), apiKey)
        placesClient = Places.createClient(requireContext())

        // Initialize the Spinner
        spinnerDentists = view.findViewById(R.id.spinnerDentists)
        fetchDentistData()

        btnGoNow = view.findViewById(R.id.btnGoNow)

        // Home button
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_maps_client_to_nav_menu_client)
        }

        // Set click listener for Go Now button
        btnGoNow.setOnClickListener {
            if (destinationLatLng != null) {
                getDirections(destinationLatLng!!)
                Toast.makeText(requireContext(), "Successful", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(requireContext(), "Please select a destination", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        updateLocationUI()
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
                Toast.makeText(requireContext(), "Failed to load dentist data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateLocationUI() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    Log.w("MapsClientFragment", "Current location is null.")
                }
            }
        } else {
            mMap.isMyLocationEnabled = false
            Toast.makeText(requireContext(), "Location permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findPlace(addressString: String) {
        val geocoder = Geocoder(requireContext())
        try {
            val addressList = geocoder.getFromLocationName(addressString, 1)
            if (addressList != null && addressList.isNotEmpty()) {
                val address = addressList[0]
                destinationLatLng = LatLng(address.latitude, address.longitude)
                mMap.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 12f))
            } else {
                Toast.makeText(requireContext(), "Address not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Toast.makeText(requireContext(), "Geocoder service is not available", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDirections(destination: LatLng) {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val originLatLng = LatLng(location.latitude, location.longitude)

                Log.d("MapsClientFragment", "Origin: $originLatLng, Destination: $destination")

                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${originLatLng.latitude},${originLatLng.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&key=$apiKey"
                Log.d("MapsClientFragment", "URL: $url") // Log the URL being called
                fetchDirections(url)
            } else {
                Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show()
            }
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
                    Log.d("MapsClientFragment", "Response JSON: $jsonData") // Log the response data

                    try {
                        val jsonObject = JSONObject(jsonData)
                        val routes = jsonObject.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
                            val points = decodePolyline(overviewPolyline)

                            requireActivity().runOnUiThread {
                                drawPolylineOnMap(points)
                            }
                        } else {
                            requireActivity().runOnUiThread {
                                Toast.makeText(requireContext(), "No routes found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
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


    private fun decodePolyline(encoded: String): List<LatLng> {
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
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng(lat / 1E5, lng / 1E5)
            poly.add(latLng)
        }
        return poly
    }

    private fun drawPolylineOnMap(points: List<LatLng>) {
        val polylineOptions = PolylineOptions().addAll(points).color(Color.BLUE).width(5f)
        mMap.addPolyline(polylineOptions)
    }
}


