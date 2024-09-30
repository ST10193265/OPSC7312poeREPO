package com.example.poe2.ui.notifications_client

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentNotificationsClientBinding
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsClientFragment : Fragment() {

    private var _binding: FragmentNotificationsClientBinding? = null
    private val binding get() = _binding!!

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

        // Load notifications from Firestore
        loadNotifications()

        // Initialize the ImageButtons
        val ibtnHome: ImageButton = binding.ibtnHome // Access ImageButton through binding

        // Set OnClickListener for the Home button
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_notifications_client_to_nav_menu_client)
        }

        return view
    }

    private fun loadNotifications() {
        val db = FirebaseFirestore.getInstance()

        // Listen for changes in the "appointments" collection
        db.collection("appointments")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Failed to load appointments: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    notificationsList.clear()

                    // Loop through the snapshots (appointments)
                    for (document in snapshots) {
                        val dentist = document.getString("dentist") ?: "Unknown Dentist"
                        val slot = document.getString("slot") ?: "Unknown Slot"
                        val date = document.getString("date") ?: "Unknown Date"
                        val description = document.getString("description") ?: "No Description"

                        // Format the notification message
                        val notificationMessage = "Appointment with $dentist on $date at $slot: $description"

                        // Add the notification message to the list
                        notificationsList.add(notificationMessage)
                    }

                    // Notify the adapter that the data has changed
                    notificationsAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "No appointments found.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference to avoid memory leaks
    }
}