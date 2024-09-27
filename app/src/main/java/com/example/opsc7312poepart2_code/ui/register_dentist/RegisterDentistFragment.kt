package com.example.opsc7312poepart2_code.ui.register_dentist

import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentRegisterDentistBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest
import java.security.SecureRandom

class RegisterDentistFragment : Fragment() {

    private var _binding: FragmentRegisterDentistBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference

    private var passwordVisible = false // Track password visibility state

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterDentistBinding.inflate(inflater, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists") // Node for dentists

        // Set password visibility to hidden by default
        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Set up button click listeners
        binding.btnCancel.setOnClickListener {
            clearFields()
        }

        binding.btnRegister.setOnClickListener {
            onRegisterClick()
        }

        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility()
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onRegisterClick() {
        val name = binding.etxtName.text.toString().trim()
        val address = binding.etxtAddress.text.toString().trim()
        val email = binding.etxtEmail.text.toString().trim()
        val username = binding.etxtUsername.text.toString().trim()
        val password = binding.etxtPassword.text.toString().trim()
        val phoneNumber = binding.etxtPhoneNumber.text.toString().trim()

        if (isValidInput(name, address, email, username, password, phoneNumber)) {
            registerUser(name, address, email, username, password, phoneNumber)
        } else {
            Toast.makeText(requireContext(), "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidInput(
        name: String, address: String, email: String, username: String, password: String, phoneNumber: String
    ): Boolean {
        return name.isNotEmpty() &&
                address.isNotEmpty() &&
                email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                username.isNotEmpty() &&
                password.length >= 6 &&
                phoneNumber.isNotEmpty()
    }

    private fun registerUser(
        name: String, address: String, email: String, username: String, password: String, phoneNumber: String
    ) {
        val userId = dbReference.push().key ?: return showToast("Failed to generate user ID")

        // Hash and salt the password
        val salt = generateSalt()
        val hashedPassword = hashPassword(password, salt)

        val user = hashMapOf(
            "userId" to userId,
            "name" to name,
            "address" to address,
            "email" to email,
            "username" to username,
            "password" to hashedPassword,
            "salt" to Base64.encodeToString(salt, Base64.DEFAULT),
            "phoneNumber" to phoneNumber,
            "isPasswordUpdated" to false
        )

        dbReference.child(userId).setValue(user)
            .addOnSuccessListener {
                showToast("Data saved successfully!")
                clearFields()
                findNavController().navigate(R.id.action_nav_register_dentist_to_nav_login_dentist)
            }
            .addOnFailureListener { exception ->
                showToast("Error saving data: ${exception.message}")
            }
    }

    private fun generateSalt(): ByteArray {
        return ByteArray(16).apply { SecureRandom().nextBytes(this) }
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }

    private fun clearFields() {
        with(binding) {
            etxtName.text.clear()
            etxtAddress.text.clear()
            etxtEmail.text.clear()
            etxtUsername.text.clear()
            etxtPassword.text.clear()
            etxtPhoneNumber.text.clear()
        }
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible
        binding.etxtPassword.inputType = if (passwordVisible) {
            InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        binding.iconViewPassword.setImageResource(if (passwordVisible) R.drawable.visible_icon else R.drawable.visible_icon)
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length) // Keep cursor at the end
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
