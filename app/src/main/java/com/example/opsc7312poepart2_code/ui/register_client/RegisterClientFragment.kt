package com.example.opsc7312poepart2_code.ui.register_client

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.InputType
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientViewModel
import com.example.poe2.R
import com.example.poe2.databinding.FragmentRegisterClientBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.util.Log
import android.widget.ImageButton
import com.google.firebase.FirebaseApp

class RegisterClientFragment : Fragment() {
    private var _binding: FragmentRegisterClientBinding? = null
    private val binding get() = _binding!!

    private val loginViewModel: LoginClientViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LoginClientViewModel::class.java)
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val editName = binding.etxtName
        val editSurname = binding.etxtSurname
        val editEmail = binding.etxtEmail
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
        dbReference = database.getReference("clients") // Change to "clients"

        btnCancel.setOnClickListener{
            clearFields(
                binding.etxtName,
                binding.etxtSurname,
                binding.etxtEmail,
                binding.etxtUsername,
                binding.etxtPassword,
                binding.etxtPhoneNumber
            )
        }

        btnRegister.setOnClickListener {
            //Log.d("RegisterClient", "Button clicked")

            val name = editName.text.toString().trim()
            val surname = editSurname.text.toString().trim()
            val email = editEmail.text.toString().trim()
            val username = editUsername.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val phoneNumber = editPhoneNumber.text.toString().trim()

           // Log.d("RegisterClient", "Captured Inputs - Name: $name, Email: $email")

            if (isValidInput(name, surname, email, username, password, phoneNumber)) {
               // Log.d("RegisterClient", "Valid Input")
                registerUser(name, surname, email, username, password, phoneNumber)
            } else {
              //  Log.d("RegisterClient", "Invalid Input")
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
        surname: String,
        email: String,
        username: String,
        password: String,
        phoneNumber: String
    ): Boolean {
        return name.isNotEmpty() &&
                surname.isNotEmpty() &&
                email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                username.isNotEmpty() &&
                password.length >= 6 &&
                phoneNumber.isNotEmpty() // Ensure phone number is valid
    }

    private fun registerUser(
        name: String,
        surname: String,
        email: String,
        username: String,
        password: String,
        phoneNumber: String
    ) {
//        Log.d("RegisterClient", "Inside registerUser")

        val userId = dbReference.push().key
        if (userId == null) {
//            Log.e("RegisterClient", "Failed to generate user ID")
            Toast.makeText(requireContext(), "Failed to generate user ID", Toast.LENGTH_SHORT).show()
            return
        }

        val user = hashMapOf(
            "userId" to userId,
            "name" to name,
            "surname" to surname,
            "email" to email,
            "username" to username,
            "password" to password, // In a real app, do not store plaintext passwords
            "phoneNumber" to phoneNumber
        )

//        Log.d("RegisterClient", "Saving user with ID: $userId")
//        Log.d("RegisterClient", "User data: $user")

        try {
            // Save user to Firebase Database under the "clients" node
            dbReference.child(userId).setValue(user)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Data saved successfully!", Toast.LENGTH_SHORT).show()
                    clearFields(
                        binding.etxtName,
                        binding.etxtSurname,
                        binding.etxtEmail,
                        binding.etxtUsername,
                        binding.etxtPassword,
                        binding.etxtPhoneNumber
                    )

                }
                .addOnFailureListener { exception ->
                    Toast.makeText(requireContext(), "Error saving data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
           // Log.e("RegisterClient", "Exception: ${e.message}")
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
            iconViewPassword.setImageResource(R.drawable.visible_icon)
        } else {
            // Show password
            editPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            // Update icon to indicate visible state
            iconViewPassword.setImageResource(R.drawable.visible_icon)
        }
        // Move the cursor to the end of the text
        editPassword.setSelection(editPassword.text.length)
    }
}
