package com.example.opsc7312poepart2_code.ui.book_app_client2

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.util.*

class BookAppClient2Fragment : Fragment() {

    private lateinit var spinnerSlots: Spinner
    private lateinit var txtSelectedDentist: TextView
    private lateinit var editTextDescription: EditText // EditText for description
    private lateinit var btnBook: Button
    private lateinit var btnDate: Button
    private lateinit var btnHome: ImageButton

    private lateinit var database: DatabaseReference

    private var selectedDate: String? = null // Variable to store the selected date
    private var userId: String? = null // Variable to hold the user ID
    private var dentistId: String? = null // Variable to hold the dentist ID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_book_app_client2, container, false)

        // Initialize views
        spinnerSlots = view.findViewById(R.id.sTime)
        txtSelectedDentist = view.findViewById(R.id.txtSelectedDentist)
        editTextDescription = view.findViewById(R.id.etxtDescription) // Initialize the EditText
        btnBook = view.findViewById(R.id.btnBook)
        btnDate = view.findViewById(R.id.btnDate)
        btnHome = view.findViewById(R.id.ibtnHome)

        // Set the dentist name to the selected dentist
        val selectedDentist = arguments?.getString("selectedDentist")
        dentistId = arguments?.getString("dentistId") // Get the dentist ID from arguments
        txtSelectedDentist.text = selectedDentist

        // Populate the spinner with hourly slots from 8 AM to 4 PM
        val slots = generateTimeSlots(8, 16) // Start at 8 AM (8) to 4 PM (16)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, slots)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSlots.adapter = adapter

        // Firebase Database initialization
        database = FirebaseDatabase.getInstance().getReference("appointments") // Firebase table reference for appointments

        // Get the user ID from Firebase
        userId = getUserIdFromFirebase()

        btnDate.setOnClickListener {
            // Handle date selection
            showDatePicker()
        }

        btnHome.setOnClickListener {
          //  findNavController().navigate(R.id.)
        }

        // Handle book button click
        btnBook.setOnClickListener {
            val selectedSlot = spinnerSlots.selectedItem.toString()
            val description = editTextDescription.text.toString() // Get the description

            // Check if a date has been selected before booking
            if (selectedDate != null) {
                Log.d(
                    "BookAppClient2Fragment",
                    "Booking appointment with: Dentist: $selectedDentist, Slot: $selectedSlot, Date: $selectedDate, Description: $description"
                )
                bookAppointment(selectedDentist ?: "", selectedSlot, selectedDate!!, description, userId ?: "", dentistId ?: "")
            } else {
                Toast.makeText(requireContext(), "Please select a date.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Function to generate time slots from startHour to endHour
    private fun generateTimeSlots(startHour: Int, endHour: Int): List<String> {
        val slots = mutableListOf<String>()
        for (hour in startHour until endHour + 1) { // include endHour
            for (minute in listOf(0, 30)) { // Every 30 minutes
                val time = String.format(
                    "%02d:%02d %s",
                    hour % 12, minute, if (hour < 12) "AM" else "PM"
                )
                slots.add(time)
            }
        }
        return slots
    }

    // Function to show a date picker dialog
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear" // Format the date

                // Log the selected date
                Log.d("BookAppClient2Fragment", "Selected Date: $selectedDate")
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    // Function to book the appointment and save to Firebase database
    private fun bookAppointment(dentist: String, slot: String, date: String, description: String, userId: String, dentistId: String) {
        // Generate a unique ID for each booking
        val appointmentId = database.push().key

        if (appointmentId != null) {
            val bookingDetails = mapOf(
                "appointmentId" to appointmentId, // Saving the unique appointment ID
                "dentist" to dentist,              // Saving the selected dentist's name
                "slot" to slot,                    // Saving the selected time slot
                "date" to date,                    // Saving the selected date
                "description" to description,      // Saving the description
                "userId" to userId,                // Saving the user ID
                "dentistId" to dentistId           // Saving the dentist ID
            )

            // Log the booking details before saving
            Log.d("BookAppClient2Fragment", "Booking Details: $bookingDetails")

            // Save booking details to Firebase under "appointments"
            database.child(appointmentId).setValue(bookingDetails)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Appointment booked successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Log.e("BookAppClient2Fragment", "Failed to book appointment: ${error.message}")
                    Toast.makeText(requireContext(), "Failed to book appointment. Please try again.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("BookAppClient2Fragment", "Failed to generate appointment ID")
        }
    }

    // Function to get the user ID from Firebase (implement this according to your authentication method)
    private fun getUserIdFromFirebase(): String? {
        // Replace this with your actual logic to retrieve the user ID
        return "someUserId" // Placeholder for demonstration
    }
}
