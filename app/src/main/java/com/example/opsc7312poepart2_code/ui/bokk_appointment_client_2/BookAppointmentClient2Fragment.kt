package com.example.opsc7312poepart2_code.ui.bokk_appointment_client_2

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.poe2.R

class BookAppointmentClient2Fragment : Fragment() {

    companion object {
        fun newInstance() = BookAppointmentClient2Fragment()
    }

    private val viewModel: BookAppointmentClient2ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_book_appointment_client2, container, false)
    }
}