package com.example.poe2.ui.notifications_client

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.opsc7312poepart2_code.ui.ApiClient
import com.example.opsc7312poepart2_code.ui.ApiResponse
import com.example.opsc7312poepart2_code.ui.ApiService
import com.example.opsc7312poepart2_code.ui.Notification
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.poe2.R
import com.example.poe2.databinding.FragmentNotificationsClientBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NotificationsClientFragment : Fragment() {

    private var _binding: FragmentNotificationsClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var btnViewNotifications: Button
    private lateinit var notificationsListView: ListView
    private lateinit var notificationsAdapter: ArrayAdapter<String>
    private val notificationsList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsClientBinding.inflate(inflater, container, false)
        val view = binding.root

        // Initialize the ListView
        notificationsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, notificationsList)
        binding.notificationsListView.adapter = notificationsAdapter // Access ListView through binding

        // Initialize views
        btnViewNotifications = view.findViewById(R.id.btnViewNotifications)
        notificationsListView = view.findViewById(R.id.notificationsListView)

        // Set up the button click listener
        btnViewNotifications.setOnClickListener {
            loadNotifications()
        }

        // Initialize the ImageButtons
        val ibtnHome: ImageButton = binding.ibtnHome // Access ImageButton through binding

        // Set OnClickListener for the Home button
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_notifications_client_to_nav_menu_client)
        }

        return view
    }

    private fun loadNotifications() {
        // Assuming you have the logged-in user's ID stored
        val currentUserId = loggedInClientUserId // Replace with the actual ID of the logged-in user

        // Create a Retrofit instance
        val apiService = ApiClient.client?.create(ApiService::class.java)

        // Make a network call to get notifications
        apiService?.getPatientNotifications()?.enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                Log.d("NotificationsClientFragment", "Response Code: ${response.code()}") // Log response code
                if (response.isSuccessful) {
                    response.body()?.let { apiResponse ->
                        Log.d("NotificationsClientFragment", "API Response: $apiResponse") // Log the API response
                        if (apiResponse.success) { // Assuming ApiResponse has a 'success' field
                            // Add notifications to the list
                            notificationsList.clear()
                            notificationsList.addAll(apiResponse.data.map { it.message }) // Assuming Notification class has a 'message' field

                            // Notify the adapter that the data has changed
                            notificationsAdapter.notifyDataSetChanged()

                            // Check if there are no notifications
                            if (notificationsList.isEmpty()) {
                                Toast.makeText(context, "No notifications available", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "No notifications available", Toast.LENGTH_SHORT).show()
                        }
                    } ?: run {
                        Log.d("NotificationsClientFragment", "Response body is null")
                        Toast.makeText(context, "No notifications available", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("NotificationsClientFragment", "Failed to load notifications: ${response.message()}") // Log error message
                    Toast.makeText(context, "Failed to load notifications: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("NotificationsClientFragment", "Error: ${t.message}") // Log error
                Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference to avoid memory leaks
    }
}

