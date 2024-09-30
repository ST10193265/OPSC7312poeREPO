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
        db.collection("notifications")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(context, "Failed to load notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    notificationsList.clear()
                    for (document in snapshots) {
                        val title = document.getString("title") ?: "No Title"
                        val body = document.getString("body") ?: "No Body"
                        notificationsList.add("$title: $body")
                    }
                    notificationsAdapter.notifyDataSetChanged()
                } else {
                    Toast.makeText(context, "No notifications found.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference to avoid memory leaks
    }
}