package com.example.opsc7312poepart2_code.ui.login_dentist

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentLoginDentistBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest

class LoginDentistFragment : Fragment() {

    private var _binding: FragmentLoginDentistBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private var passwordVisible = false // Password visibility state

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        var loggedInDentistUsername: String? = null // Global variable to store the logged-in username
        var loggedInDentistUserId: String? = null // Global variable to store the logged-in user ID
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginDentistBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("your_preferences", Context.MODE_PRIVATE)

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists") // Firebase node for dentists

        // Handle login button click
        binding.btnLogin.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val password = binding.etxtPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                loginUser(username, password)
            } else {
                Toast.makeText(requireContext(), "Please enter both username and password.", Toast.LENGTH_SHORT).show()
            }
        }

        // Set the password field to not visible by default
        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Handle password visibility toggle
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility(binding.etxtPassword, binding.iconViewPassword)
        }

        // Handle Forget Password text click
        binding.txtForgotPassword.setOnClickListener {
            onForgotPasswordClicked(it) // Use 'it' to pass the current view
        }

        return root
    }

    fun onForgotPasswordClicked(view: View) {
        // Handle the action
        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_forget_password_dentist)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Method to clear the input fields
    private fun clearFields() {
        binding.etxtUsername.text.clear()
        binding.etxtPassword.text.clear()
    }

    private fun loginUser(username: String, password: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java) ?: ""
                    val storedSalt = userSnapshot.child("salt").getValue(String::class.java)

                    if (storedSalt == null) {
                        Log.e("LoginDentistFragment", "Salt is missing for user: $username")
                        Toast.makeText(requireContext(), "Error: Salt is missing.", Toast.LENGTH_SHORT).show()
                        return
                    }

                    try {
                        val decodedSalt = Base64.decode(storedSalt, Base64.DEFAULT)

                        // Hash the input password with the stored salt
                        val hashedPassword = hashPassword(password, decodedSalt)

                        // Compare the hashed password with the stored hashed password
                        if (hashedPassword == storedHashedPassword) {
                            loggedInDentistUsername = username // Store the logged-in username
                            loggedInDentistUserId = userSnapshot.key // Store the user ID
                            saveLoginStatus()
                            Log.i("Logged in user", "Login successful for user: $username")
                            Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                            clearFields()
                            findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
                        } else {
                            Toast.makeText(requireContext(), "Incorrect password.", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: IllegalArgumentException) {
                        Log.e("LoginDentistFragment", "Error decoding salt: ${e.message}")
                        Toast.makeText(requireContext(), "Error decoding salt.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }

    private fun togglePasswordVisibility(editPassword: EditText, ibtnVisiblePassword: ImageView) {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            // Show password
            editPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            ibtnVisiblePassword.setImageResource(R.drawable.visible_icon) // Change to your visible icon
        } else {
            // Hide password
            editPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ibtnVisiblePassword.setImageResource(R.drawable.visible_icon) // Change to your hidden icon
        }

        // Move the cursor to the end of the text
        editPassword.setSelection(editPassword.text.length)
    }

    private fun saveLoginStatus() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putString("username", loggedInDentistUsername)
        editor.putString("id", loggedInDentistUserId)
        editor.apply()
    }
}
