package com.example.poe2.ui.calendar_client

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import com.example.poe2.R


class CalendarClientFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_calendar_client, container, false)

        // Initialize the ImageButtons
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)

        // Set OnClickListener for the Book Appointment button
        ibtnHome.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_calendar_client_to_nav_menu_client)
        }

        return view // Make sure to return the view after setting up everything
    }
}