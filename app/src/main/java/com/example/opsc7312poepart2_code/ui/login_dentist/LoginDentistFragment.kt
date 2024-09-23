package com.example.opsc7312poepart2_code.ui.login_dentist

import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.poe2.R

class LoginDentistFragment : Fragment() {

    companion object {
        fun newInstance() = LoginDentistFragment()
    }

    private val viewModel: LoginDentistViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // TODO: Use the ViewModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_login_dentist, container, false)
    }
}