package com.example.poe2.ui.menu_dentist

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import com.example.poe2.R

class MenuDentistFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_menu_dentist, container, false)

        // Initialize the ImageButtons
        val ibtnBookAppointment: ImageButton = view.findViewById(R.id.ibtnBookAppointment)
        val ibtnNotifications: ImageButton = view.findViewById(R.id.ibtnNotifications)
        val ibtnSettings: ImageButton = view.findViewById(R.id.ibtnSettings)

        // Set OnClickListener for the Book Appointment button
        ibtnBookAppointment.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_book_appointment_dentist)
        }

        // Set OnClickListener for the Notifications button
        ibtnNotifications.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_notifications_dentist)
        }

        // Set OnClickListener for the Notifications button
        ibtnSettings.setOnClickListener {
            // Navigate to the BookAppointmentFragment using the NavController
            findNavController().navigate(R.id.action_nav_menu_dentist_to_nav_settings_dentist)
        }

        return view // Make sure to return the view after setting up everything
    }
}
