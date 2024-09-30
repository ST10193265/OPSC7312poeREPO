package com.example.opsc7312poepart2_code.ui.forget_password_client

import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentForgetPasswordClientBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest
import java.security.SecureRandom

class ForgetPasswordClientFragment : Fragment() {
    private var _binding: FragmentForgetPasswordClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase  // Firebase Database instance
    private lateinit var dbReference: DatabaseReference // Reference to the "clients" node in Firebase

    private var passwordVisible = false // Password visibility state

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgetPasswordClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients") // Reference to the clients node

        // Set up click listener for the Save button
        binding.btnSave.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val newPassword = binding.etxtNewPassword.text.toString().trim()
            val email = binding.etxtEmail.text.toString().trim()

            // Check if all fields are filled
            if (username.isNotEmpty() && newPassword.isNotEmpty() && email.isNotEmpty()) {
                resetPassword(username, email, newPassword) // Call resetPassword method
            } else {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set the password field to not visible by default
        binding.etxtNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Set click listener for the password visibility toggle icon
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility(it) // Call method to toggle password visibility
        }

        // Set click listener for the Cancel button
        binding.btnCancel.setOnClickListener {
            clearFields() // Call method to clear all fields
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference
    }

    // Method to clear all input fields
    private fun clearFields() {
        with(binding) {
            etxtEmail.text.clear() // Clear email field
            etxtUsername.text.clear() // Clear username field
            etxtNewPassword.text.clear() // Clear password field
        }
    }

    // Method to toggle password visibility
    fun togglePasswordVisibility(view: View) {
        passwordVisible = !passwordVisible // Toggle visibility state

        if (passwordVisible) {
            binding.etxtNewPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD // Show password
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon) // Change icon to visible
            Log.d("ForgetPasswordClientFragment", "Password is now visible.")
        }
        binding.etxtNewPassword.setSelection(binding.etxtNewPassword.text.length) // Move cursor to the end
    }

    // Method to reset the user's password
    private fun resetPassword(username: String, email: String, newPassword: String) {
        Log.d("ForgetPasswordClientFragment", "Attempting to reset password for user: $username")

        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val storedEmail = userSnapshot.child("email").getValue(String::class.java)

                    // Check if the provided email matches the stored email
                    if (storedEmail == email) {
                        // Hash and salt the new password
                        val newSalt = generateSalt()
                        val hashedNewPassword = hashPassword(newPassword, newSalt) // Hash the new password

                        // Update the user's password and isPasswordUpdated field
                        userSnapshot.ref.child("password").setValue(hashedNewPassword)
                        userSnapshot.ref.child("salt").setValue(Base64.encodeToString(newSalt, Base64.DEFAULT))
                        userSnapshot.ref.child("isPasswordUpdated").setValue(true) // Set the updated flag to true

                        Toast.makeText(requireContext(), "Password reset successfully!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_nav_forget_password_client_to_nav_login_client)

                        Log.d("ForgetPasswordClientFragment", "Password reset successfully for user: $username")
                    } else {
                        Toast.makeText(requireContext(), "Email does not match the username.", Toast.LENGTH_SHORT).show()
                        Log.d("ForgetPasswordClientFragment", "Email does not match for user: $username")
                    }
                } else {
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                    Log.d("ForgetPasswordClientFragment", "User not found for username: $username")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("ForgetPasswordClientFragment", "Database error: ${error.message}") // Log database errors
            }
        })
    }

    // Method to generate a salt for hashing
    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt) // Generate a random salt
        Log.d("ForgetPasswordClientFragment", "Generated salt for hashing.")
        return salt
    }
    // the code above was taken and apapted from StackOverFlow
    // https://stackoverflow.com/questions/78309846/javax-crypto-aeadbadtagexception-bad-decrypt-in-aes256-decryption
    // Jagar
    // https://stackoverflow.com/users/12053756/jagar

    // Method to hash the password using SHA-256
    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256") // Create a SHA-256 digest
        digest.update(salt) // Add the salt to the digest
        val hashedPassword = Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT) // Hash the password
        Log.d("ForgetPasswordClientFragment", "Password hashed successfully.")
        return hashedPassword
    }
    // the code above was taken and adpated from Hyperskill
    // https://hyperskill.org/learn/step/36628
}
