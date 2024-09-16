package com.example.poe2.ui.book_appointment_dentist

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import com.example.poe2.R


class BookAppointmentDentistFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment (use the correct layout for the BookAppointmentDentistFragment)
        val view = inflater.inflate(R.layout.fragment_book_appointment_dentist, container, false)

        // Initialize the ImageButton
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)

        // Set OnClickListener for the Home button
        ibtnHome.setOnClickListener {
            // Navigate back to the MenuDentistFragment using the NavController
            findNavController().navigate(R.id.action_nav_book_appointment_dentist_to_nav_menu_dentist)
        }

        return view // Return the view after setting up everything
    }
}
