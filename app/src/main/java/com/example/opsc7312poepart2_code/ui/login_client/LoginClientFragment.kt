package com.example.opsc7312poepart2_code.ui.login_client

import android.content.Context
import android.content.SharedPreferences
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
import com.example.poe2.databinding.FragmentLoginClientBinding
import com.google.firebase.database.*
import java.security.MessageDigest

class LoginClientFragment : Fragment() {

    private var _binding: FragmentLoginClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences


    private var passwordVisible = false // Password visibility state


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients") // Points to the clients node

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        // Handle login button click
        binding.btnLogin.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val password = binding.etxtPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                authenticateUser(username, password)
            } else {
                Toast.makeText(requireContext(), "Please enter your username and password.", Toast.LENGTH_SHORT).show()
            }
        }
        // Set the password field to not visible by default
        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Handle password visibility toggle
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Handle Forget Password text click
        binding.txtForgotPassword.setOnClickListener {
            // Navigate to ForgetPasswordFragment
            findNavController().navigate(R.id.action_nav_login_client_to_nav_forget_password_client)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun authenticateUser(username: String, password: String) {
        // Query Firebase to find the user by username
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()

                    // Retrieve stored hashed password and salt
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java)
                    val storedSaltBase64 = userSnapshot.child("salt").getValue(String::class.java)
                    val storedSalt = Base64.decode(storedSaltBase64, Base64.DEFAULT)

                    // Hash the entered password with the stored salt
                    val hashedEnteredPassword = hashPassword(password, storedSalt)

                    if (hashedEnteredPassword == storedHashedPassword) {
                        // Login successful, save login status in SharedPreferences
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("username", username)
                        editor.apply()

                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()

                        // Navigate to the main menu or next activity/fragment
                        navigateToMenu()
                    } else {
                        Toast.makeText(requireContext(), "Incorrect password.", Toast.LENGTH_SHORT).show()
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

    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }

    private fun navigateToMenu() {
        findNavController().navigate(R.id.action_nav_login_client_to_nav_menu_client)
    }

    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            // Show password
            binding.etxtPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon) // Change icon to indicate visibility
        } else {
            // Hide password
            binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon) // Change icon to indicate invisibility
        }

        // Move the cursor to the end of the text
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length)
    }
}
