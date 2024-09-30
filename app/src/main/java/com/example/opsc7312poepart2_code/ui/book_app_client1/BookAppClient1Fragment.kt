package com.example.opsc7312poepart2_code.ui.book_app_client1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.google.firebase.database.*

class BookAppClient1Fragment : Fragment() {

    private lateinit var dentistList: ArrayList<String>
    private lateinit var listViewAdapter: ArrayAdapter<String>
    private lateinit var databaseReference: DatabaseReference

    // Log tag for debugging
    private val TAG = "BookAppClient1Fragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_book_app_client1, container, false)

        // Initialize the SearchView and ListView
        val searchView = view.findViewById<SearchView>(R.id.searchbar)
        val listView = view.findViewById<ListView>(R.id.listofDentists)

        // Initialize ImageButtons
        val ibtnMaps = view.findViewById<ImageButton>(R.id.ibtnMaps)
        val ibtnHome = view.findViewById<ImageButton>(R.id.ibtnHome)

        // Initialize the dentist list and adapter
        dentistList = ArrayList()
        listViewAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, dentistList)
        listView.adapter = listViewAdapter

        // Set up Firebase database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("dentists")

        // Fetch dentists from Firebase
        fetchDentists()

        // Handle search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                listViewAdapter.filter.filter(newText)
                return false
            }
        })

        // Handle ListView item click to navigate to another screen
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val selectedDentist = listViewAdapter.getItem(position)
            Log.d(TAG, "Selected Dentist: $selectedDentist")  // Log selected dentist
            if (selectedDentist != null) {
                try {
                    findNavController().navigate(R.id.action_nav_book_app_client1_to_nav_book_app_client2)


                } catch (e: Exception) {
                    Log.e(TAG, "Navigation error: ${e.message}")  // Log any navigation errors
                    Toast.makeText(requireContext(), "Error navigating: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Selected dentist is null")  // Log if the dentist is null
                Toast.makeText(requireContext(), "Invalid dentist selection.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set click listeners for the buttons
        ibtnMaps.setOnClickListener {
            Toast.makeText(requireContext(), "To be implemented.", Toast.LENGTH_SHORT).show()
        }

        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_book_app_client1_to_nav_menu_client)
        }

        return view
    }

    // Fetch dentists from Firebase Realtime Database
    private fun fetchDentists() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dentistList.clear()
                for (dentistSnapshot in snapshot.children) {
                    val dentistName = dentistSnapshot.child("name").getValue(String::class.java)
                    dentistName?.let { dentistList.add(it) }
                }
                listViewAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Database error: ${error.message}")  // Log any database errors
                Toast.makeText(requireContext(), "Failed to load dentists.", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
