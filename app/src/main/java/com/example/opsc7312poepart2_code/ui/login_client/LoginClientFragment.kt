package com.example.opsc7312poepart2_code.ui.login_client

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.poe2.R
import com.example.poe2.databinding.FragmentLoginClientBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.security.MessageDigest

class LoginClientFragment : Fragment() {

    private var _binding: FragmentLoginClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var auth: FirebaseAuth

    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleSignInClient: com.google.android.gms.auth.api.signin.GoogleSignInClient
    // the code above was taken form GeeksForGeeks.
    // https://www.geeksforgeeks.org/google-signing-using-firebase-authentication-in-kotlin/

    private var passwordVisible = false // Password visibility state

    companion object {
        var loggedInClientUsername: String? = null // Global variable to store the logged-in username
        var loggedInClientUserId: String? = null // Global variable to store the logged-in user ID
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database and Auth
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients")
        auth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences for login status
        sharedPreferences = requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

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
            togglePasswordVisibility()
        }

        // Handle Forget Password text click
        binding.txtForgotPassword.setOnClickListener {
            onForgotPasswordClicked(it)
        }

        // Initialize Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Initialize Google Sign-In client
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Bind the Sign-In button and set up a click listener
        binding.signInButton.setOnClickListener {
            signIn()
        }

        return root
    }

    // Initiate Google Sign-In
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // the code above was taken form GeeksForGeeks.
    // https://www.geeksforgeeks.org/google-signing-using-firebase-authentication-in-kotlin/

    // Handle the result of Google Sign-In
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Signed in successfully, show authenticated UI.
                Toast.makeText(requireContext(), "Sign-in successful.", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_nav_login_client_to_nav_menu_client)

            } catch (e: ApiException) {
                // Handle sign-in failure
                Toast.makeText(requireContext(), "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                Log.e("LoginClientFragment", "Sign-in failed: ${e.statusCode}")
            }
        }
    }
    // the code above was taken form GeeksForGeeks.
    // https://www.geeksforgeeks.org/google-signing-using-firebase-authentication-in-kotlin/


    // Navigate to the Forget Password Fragment
    fun onForgotPasswordClicked(view: View) {
        findNavController().navigate(R.id.action_nav_login_client_to_nav_forget_password_client)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Authenticate the user
    private fun loginUser(username: String, password: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java) ?: ""
                    val storedSalt = userSnapshot.child("salt").getValue(String::class.java)?.let { Base64.decode(it, Base64.DEFAULT) } ?: ByteArray(0)

                    // Hash the entered password
                    val hashedPassword = hashPassword(password, storedSalt)

                    // Compare hashed password with the stored password
                    if (hashedPassword == storedHashedPassword) {
                        loggedInClientUsername = username
                        getUserIdFromFirebase(username)
                        saveLoginStatus()
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        clearFields()
                        findNavController().navigate(R.id.action_nav_login_client_to_nav_menu_client)
                    } else {
                        Toast.makeText(requireContext(), "Incorrect password.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "User not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginClientFragment", "Database error: ${error.message}")
            }
        })
    }

    private fun clearFields() {
        binding.etxtUsername.text.clear()
        binding.etxtPassword.text.clear()
        Log.d("LoginClientFragment", "Cleared input fields")
    }

    // Retrieve the user ID from Firebase
    private fun getUserIdFromFirebase(username: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    loggedInClientUserId = userSnapshot.key
                    Log.d("LoginClientFragment", "loggedInClientUserId: $loggedInClientUserId")
                } else {
                    Toast.makeText(requireContext(), "User ID not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error retrieving user ID: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("LoginClientFragment", "Error retrieving user ID: ${error.message}")
            }
        })
    }

    // Hash the password with the provided salt
    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }
    // the code above was taken and adpated from Hyperskill
    // https://hyperskill.org/learn/step/36628

    // Toggle the visibility of the password
    private fun togglePasswordVisibility() {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            binding.etxtPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon) // Replace with the appropriate icon
        }
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length)
    }

    // Save the login status in SharedPreferences
    private fun saveLoginStatus() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
        Log.d("LoginClientFragment", "Login status saved.")
    }
}
