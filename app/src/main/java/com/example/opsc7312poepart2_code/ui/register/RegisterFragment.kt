package com.example.poe2.ui.register

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.poe2.ui.register.RegisterViewModel
import com.example.poe2.databinding.FragmentRegisterBinding

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val registerViewModel =
            ViewModelProvider(this).get(RegisterViewModel::class.java)

        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val txtName: TextView = binding.txtName
        val etxtName : EditText = binding.etxtName

        val txtSurname : TextView = binding.txtSurname
        val etxtSurname: EditText = binding.etxtSurname

        val txtEmail: TextView = binding.txtEmail
        val etxtEmail : EditText = binding.etxtEmail

        val txtPhone : TextView = binding.txtPhoneNumber
        val etxtPhone: EditText = binding.etxtPhone

        val txtUsername: TextView = binding.txtxUsername
        val etxtUsername : EditText = binding.etxtUsername

        val txtPassword : TextView = binding.txtPassword
        val etxtPassword: EditText = binding.etxtPassword
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
