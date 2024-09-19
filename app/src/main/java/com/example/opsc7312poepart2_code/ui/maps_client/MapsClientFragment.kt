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
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList

class MapsClientFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var btnGoNow: Button
    private lateinit var placesClient: PlacesClient
    private var destinationLatLng: LatLng? = null

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
        Places.initialize(requireContext(), getString(R.string.google_maps_key))
        placesClient = Places.createClient(requireContext())

        // Find Views
        autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView)
        btnGoNow = view.findViewById(R.id.btnGoNow)

        // Initialize the ImageButtons
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_maps_client_to_nav_menu_client)
        }

        // Set up autocomplete for places
        setupAutoComplete()

        // Set click listener for Go Now button
        btnGoNow.setOnClickListener {
            destinationLatLng?.let {
                getDirections(it)
            } ?: Toast.makeText(requireContext(), "Please select a destination", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            updateLocationUI()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun updateLocationUI() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                } else {
                    val defaultLocation = LatLng(-34.0, 151.0)  // Example: Sydney
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                }
            }
        } else {
            mMap.isMyLocationEnabled = false
            Toast.makeText(requireContext(), "Location permission is required.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Ensure mMap is initialized before calling updateLocationUI
                updateLocationUI()
            } else {
                Toast.makeText(context, "Location permission is required.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupAutoComplete() {
        val autocompleteAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, emptyList<String>())
        autoCompleteTextView.setAdapter(autocompleteAdapter)

        autoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selectedPlace = parent.getItemAtPosition(position) as String
            findPlace(selectedPlace)
        }

        autoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrEmpty()) {
                    fetchPlaceSuggestions(s.toString())
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
            val suggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, suggestions)
            autoCompleteTextView.setAdapter(adapter)
            adapter.notifyDataSetChanged()
        }
    }

    private fun findPlace(placeName: String) {
        val geocoder = Geocoder(requireContext())
        val addressList = geocoder.getFromLocationName(placeName, 1)
        if (addressList != null && addressList.isNotEmpty()) {
            val address = addressList[0]
            destinationLatLng = LatLng(address.latitude, address.longitude)
            mMap.addMarker(MarkerOptions().position(destinationLatLng!!).title("Destination"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng!!, 12f))
        }
    }

    private fun getDirections(destination: LatLng) {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val originLatLng = LatLng(location.latitude, location.longitude)
                val apiKey = getString(R.string.google_maps_key)

                val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                        "origin=${originLatLng.latitude},${originLatLng.longitude}" +
                        "&destination=${destination.latitude},${destination.longitude}" +
                        "&key=$apiKey"

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
                    Toast.makeText(requireContext(), "Failed to fetch directions", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val jsonData = responseBody.string()

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
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
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
