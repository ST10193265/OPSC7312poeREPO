package com.example.poe2.ui.notifications_client

import android.os.Bundle
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
import com.example.poe2.R
import com.example.poe2.databinding.FragmentNotificationsClientBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore

class NotificationsClientFragment : Fragment() {

    /*
    * Code attributions:
    *
    * Auther: freeCodeCamp.org; Python API Development - Comprehensive Course for Beginners: link- https://youtu.be/0sOvCWFmrtA
    * Auther: Phillipp Lackner; Local Notifications in Android - The Full Guide (Android Studio Tutorial) : link - https://youtu.be/LP623htmWcI
    * Auther: Code With Cal; Daily Calendar View Android Studio Tutorial : link - https://youtu.be/Aig99t-gNqM
    * Auther: Foxandroid; How to Add SearchView in Android App using Kotlin | SearchView | Kotlin | Android studio : link - https://youtu.be/oE8nZRJ9vxA
    *
    * AI Tools
    * Gemini
    * ChatGpt
    * Amazon Q
    * CoPilot
    *
    * */

    private var _binding: FragmentNotificationsClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var btnViewNotifications: Button
    private lateinit var notificationsListView: ListView
    private lateinit var notificationsAdapter: ArrayAdapter<String>
    private val notificationsList = mutableListOf<String>()
    private lateinit var database: DatabaseReference

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

        // Initialize Firebase Realtime Database reference to "appointments"
        database = FirebaseDatabase.getInstance().getReference("appointments")

        // Initialize the ImageButtons
        val ibtnHome: ImageButton = binding.ibtnHome // Access ImageButton through binding

        // Set up the button click listener
        btnViewNotifications.setOnClickListener {
            loadNotifications()
        }

        // Set OnClickListener for the Home button
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_notifications_client_to_nav_menu_client)
        }

        return view
    }

    private fun loadNotifications() {
        val currentUserId = "user123" // Replace with the actual ID of the logged-in user

        // Listen for changes in the "appointments" table in Firebase Realtime Database
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notificationsList.clear()

                // Loop through each child in the snapshot (appointments)
                for (appointmentSnapshot in snapshot.children) {
                    val dentist = appointmentSnapshot.child("dentist").getValue(String::class.java) ?: "Unknown Dentist"
                    val slot = appointmentSnapshot.child("slot").getValue(String::class.java) ?: "Unknown Slot"
                    val date = appointmentSnapshot.child("date").getValue(String::class.java) ?: "Unknown Date"
                    val description = appointmentSnapshot.child("description").getValue(String::class.java) ?: "No Description"
                    val userId = appointmentSnapshot.child("userId").getValue(String::class.java) ?: ""

                    // Check if the appointment belongs to the logged-in user
                    if (userId == currentUserId) {
                        // Format the notification message
                        val notificationMessage = "Appointment with $dentist on $date at $slot: $description"

                        // Add the notification message to the list
                        notificationsList.add(notificationMessage)
                    }
                }

                // Notify the adapter that the data has changed
                notificationsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle potential errors
                Toast.makeText(context, "Failed to load appointments: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference to avoid memory leaks
    }
}