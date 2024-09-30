package com.example.poe2.ui.book_appointment_dentist

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import androidx.navigation.fragment.findNavController
import com.applandeo.materialcalendarview.CalendarView
import com.example.poe2.R
import java.text.SimpleDateFormat
import java.util.Locale
import com.applandeo.materialcalendarview.EventDay
import com.applandeo.materialcalendarview.listeners.OnDayClickListener

import java.util.*


class BookAppointmentDentistFragment : Fragment() {
    private lateinit var calendarView1: CalendarView
    private lateinit var appointmentListView1: ListView
    private val appointments = hashMapOf(
        "2024-09-30" to listOf("Dentist Appointment at 10:00 AM"),
        "2024-10-01" to listOf("Meeting with John at 2:00 PM", "Lunch with Sarah at 12:00 PM")
    )

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
            //findNavController().navigate(R.id.action_nav_book_appointment_dentist_to_nav_menu_dentist)
        }

        // Initialize the CalendarView
        calendarView1 = view.findViewById(R.id.calendarView1)
        appointmentListView1 = view.findViewById(R.id.appointmentListView1)

        // Highlight days with appointments
        highlightAppointmentDays()
        calendarView1.setOnDayClickListener(object : OnDayClickListener {
            override fun onDayClick(eventDay: EventDay) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val selectedDate = dateFormat.format(eventDay.calendar.time)
                val appointmentList = appointments[selectedDate]

                if (appointmentList != null) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        appointmentList
                    )
                    appointmentListView1.adapter = adapter
                } else {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        listOf("No appointments")
                    )
                    appointmentListView1.adapter = adapter
                }
            }
        })

        return view // Make sure to return the view after setting up everything
    }

    private fun highlightAppointmentDays() {
        val datesWithAppointments = appointments.keys.map { dateString ->
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString)
            Calendar.getInstance().apply { time = date }
        }

        val events = datesWithAppointments.map { calendar ->
            EventDay(calendar, R.drawable.redcircle, Color.RED)
        }

        calendarView1.setEvents(events)
    }
}