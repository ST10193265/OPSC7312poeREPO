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
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.*
import java.security.MessageDigest

class LoginClientFragment : Fragment() {

    private var _binding: FragmentLoginClientBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: FirebaseDatabase
    private lateinit var dbReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private var passwordVisible = false // Password visibility state

    companion object {
        var loggedInClientUsername: String? = null // Global variable to store the logged-in username
        private const val RC_SIGN_IN = 9001 // Request code for Google Sign-In
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginClientBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize Firebase Database
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("clients")

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("loginPrefs", Context.MODE_PRIVATE)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Replace with your web client ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

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

        // Handle Google Sign-In button click
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // Set the password field to not visible by default
        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        // Handle password visibility toggle
        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Handle Forget Password text click
        binding.txtForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_nav_login_client_to_nav_forget_password_client)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun authenticateUser(username: String, password: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    val storedHashedPassword = userSnapshot.child("password").getValue(String::class.java)
                    val storedSaltBase64 = userSnapshot.child("salt").getValue(String::class.java)
                    val storedSalt = Base64.decode(storedSaltBase64, Base64.DEFAULT)

                    val hashedEnteredPassword = hashPassword(password, storedSalt)

                    if (hashedEnteredPassword == storedHashedPassword) {
                        loggedInClientUsername = username // Store the logged-in username
                        val editor = sharedPreferences.edit()
                        editor.putBoolean("isLoggedIn", true)
                        editor.putString("username", loggedInClientUsername) // Save to SharedPreferences
                        Log.i("Logged in user", "Login successful for user: $username")

                        editor.apply()

                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
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

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account)
        } catch (e: ApiException) {
            Toast.makeText(requireContext(), "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Get the signed-in user's email
                    val email = account.email
                    // Check if the user exists in your database
                    checkUserInDatabase(email)
                } else {
                    Toast.makeText(requireContext(), "Google Sign-In failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserInDatabase(email: String?) {
        // Query the database for the user by email
        dbReference.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // User exists in the database, proceed to login
                    loggedInClientUsername = snapshot.children.first().child("username").getValue(String::class.java) ?: "User"
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isLoggedIn", true)
                    editor.putString("username", loggedInClientUsername) // Save to SharedPreferences
                    editor.apply()

                    Toast.makeText(requireContext(), "Google Sign-In successful!", Toast.LENGTH_SHORT).show()
                    navigateToMenu()
                } else {
                    // User does not exist in the database
                    Toast.makeText(requireContext(), "This Google account is not registered.", Toast.LENGTH_SHORT).show()
                    // Optionally, you can add a method to handle new users or show a registration prompt
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
            binding.etxtPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon)
        } else {
            binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon)
        }
        binding.etxtPassword.setSelection(binding.etxtPassword.text.length)
    }
}
