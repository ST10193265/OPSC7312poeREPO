package com.example.opsc7312poepart2_code.ui.book_appointment_client_1

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.poe2.R

class BookAppointmentClient1Fragment : Fragment() {

    companion object {
        fun newInstance() = BookAppointmentClient1Fragment()
    }

    private val viewModel: BookAppointmentClient1ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_book_appointment_client1, container, false)
    }
}