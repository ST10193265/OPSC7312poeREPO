package com.example.opsc7312poepart2_code.ui.register_dentist

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.opsc7312poepart2_code.ui.login_dentist.LoginDentistViewModel
import com.example.poe2.R
import com.example.poe2.databinding.FragmentRegisterDentistBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterDentistFragment : Fragment() {

    private var _binding: FragmentRegisterDentistBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginDentistViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LoginDentistViewModel::class.java)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterDentistBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val editName = binding.etxtName
        val editEmail = binding.etxtEmail
        val editAddress = binding.etxtAddress
        val editUsername = binding.etxtUsername
        val editPassword = binding.etxtPassword
        val editPhoneNumber = binding.etxtPhoneNumber
        val btnRegister = binding.btnRegister
        val btnCancel = binding.btnCancel
        val iconViewPassword = binding.ibtnVisiblePassword

        // Initialize Firebase
        FirebaseApp.initializeApp(requireContext())
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists") // Change to "dentists"

        btnCancel.setOnClickListener {
            clearFields(
                editName,
                editEmail,
                editAddress,
                editUsername,
                editPassword,
                editPhoneNumber
            )
        }

        btnRegister.setOnClickListener {
            val name = editName.text.toString().trim()
            val address = editAddress.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val username = editUsername.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val phoneNumber = editPhoneNumber.text.toString().trim()

            if (isValidInput(name, address, email, username, password, phoneNumber)) {
                registerUser(name, address, email, username, password, phoneNumber)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
            }
        }

        iconViewPassword.setOnClickListener {
            togglePasswordVisibility(editPassword, iconViewPassword)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun isValidInput(
        name: String,
        address: String,
        email: String,
        username: String,
        password: String,
        phoneNumber: String
    ): Boolean {
        return name.isNotEmpty() &&
                address.isNotEmpty() &&
                email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                username.isNotEmpty() &&
                password.length >= 6 &&
                phoneNumber.isNotEmpty() // Ensure phone number is valid
    }

    private fun registerUser(
        name: String,
        address: String,
        email: String,
        username: String,
        password: String,
        phoneNumber: String
    ) {
        val userId = dbReference.push().key
        if (userId == null) {
            Toast.makeText(requireContext(), "Failed to generate user ID", Toast.LENGTH_SHORT).show()
            return
        }

        val user = hashMapOf(
            "userId" to userId,
            "name" to name,
            "address" to address,
            "email" to email,
            "username" to username,
            "password" to password, // In a real app, do not store plaintext passwords
            "phoneNumber" to phoneNumber
        )

        try {
            // Save user to Firebase Database under the "dentists" node
            dbReference.child(userId).setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Data saved successfully!", Toast.LENGTH_SHORT).show()
                    clearFields(
                        binding.etxtName,
                        binding.etxtAddress,
                        binding.etxtEmail,
                        binding.etxtPhoneNumber,
                        binding.etxtUsername,
                        binding.etxtUsername,
                        binding.etxtPassword
                    )
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error saving data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields(vararg editTexts: EditText) {
        for (editText in editTexts) {
            editText.text.clear()
        }
    }

    private fun togglePasswordVisibility(editPassword: EditText, iconViewPassword: ImageButton) {
        val inputType = editPassword.inputType
        if (inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            // Hide password
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            // Update icon to indicate hidden state
            iconViewPassword.setImageResource(R.drawable.visible_icon) // Update to your hidden icon
        } else {
            // Show password
            editPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            // Update icon to indicate visible state
            iconViewPassword.setImageResource(R.drawable.visible_icon) // Update to your visible icon
        }
        // Move the cursor to the end of the text
        editPassword.setSelection(editPassword.text.length)
    }
}
