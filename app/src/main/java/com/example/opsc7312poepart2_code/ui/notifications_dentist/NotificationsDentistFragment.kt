package com.example.poe2.ui.notifications_dentist


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.navigation.fragment.findNavController
import com.example.poe2.R

class NotificationsDentistFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_notifications_dentist, container, false)

        // Initialize views
        val ibtnHome: ImageButton = view.findViewById(R.id.ibtnHome)

        // Set the home button listener to navigate to the main menu
        ibtnHome.setOnClickListener {
            findNavController().navigate(R.id.action_nav_notifications_dentist_to_nav_menu_dentist)
        }



        return view
    }
}
