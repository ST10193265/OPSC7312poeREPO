package com.example.opsc7312poepart2_code.ui.forget_password_client

import androidx.fragment.app.viewModels
import android.os.Bundle
import android.text.InputType
import android.util.Base64
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

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference

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
        dbReference = database.getReference("clients") // Change to your clients node

        binding.btnSave.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val newPassword = binding.etxtNewPassword.text.toString().trim()
            val email = binding.etxtEmail.text.toString().trim()

            if (username.isNotEmpty() && newPassword.isNotEmpty() && email.isNotEmpty()) {
                resetPassword(username, email, newPassword)
            } else {
                Toast.makeText(requireContext(), "Please fill all fields.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            // Handle cancel button click
            // Navigate back or close the fragment
            requireActivity().onBackPressed()
        }

        // Set the password field to not visible by default
        binding.etxtNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Handle password visibility toggle
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility()
        }

        binding.btnCancel.setOnClickListener {
            clearFields()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    private fun clearFields() {
        with(binding) {
            etxtEmail.text.clear()
            etxtUsername.text.clear()
            etxtNewPassword.text.clear()
        }
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            // Show password
            binding.etxtNewPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon) // Change icon to indicate visibility
        } else {
            // Hide password
            binding.etxtNewPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon) // Change icon to indicate invisibility
        }

        // Move the cursor to the end of the text
        binding.etxtNewPassword.setSelection(binding.etxtNewPassword.text.length)
    }


    private fun resetPassword(username: String, email: String, newPassword: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val storedEmail = userSnapshot.child("email").getValue(String::class.java)

                    if (storedEmail == email) {
                        // Hash and salt the new password
                        val newSalt = generateSalt()
                        val hashedNewPassword = hashPassword(newPassword, newSalt)

                        // Update the user's password and isPasswordUpdated field
                        userSnapshot.ref.child("password").setValue(hashedNewPassword)
                        userSnapshot.ref.child("salt").setValue(Base64.encodeToString(newSalt, Base64.DEFAULT))
                        userSnapshot.ref.child("isPasswordUpdated").setValue(true) // Set the updated flag to true

                        Toast.makeText(requireContext(), "Password reset successfully!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_nav_forget_password_client_to_nav_login_client)

                    } else {
                        Toast.makeText(requireContext(), "Email does not match the username.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }


}
