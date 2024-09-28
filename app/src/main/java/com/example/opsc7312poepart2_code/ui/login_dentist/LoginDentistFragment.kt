package com.example.opsc7312poepart2_code.ui.login_dentist

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
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUserId
import com.example.opsc7312poepart2_code.ui.login_client.LoginClientFragment.Companion.loggedInClientUsername
import com.example.poe2.R
import com.example.poe2.databinding.FragmentLoginDentistBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
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
    private var passwordVisible = false

    private val RC_SIGN_IN = 9001
    private lateinit var mGoogleSignInDentist: com.google.android.gms.auth.api.signin.GoogleSignInClient

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        var loggedInDentistUsername: String? = null
        var loggedInDentistUserId: String? = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginDentistBinding.inflate(inflater, container, false)
        val root: View = binding.root

        sharedPreferences = requireActivity().getSharedPreferences("your_preferences", Context.MODE_PRIVATE)
        database = FirebaseDatabase.getInstance()
        dbReference = database.getReference("dentists")

        binding.btnLogin.setOnClickListener {
            val username = binding.etxtUsername.text.toString().trim()
            val password = binding.etxtPassword.text.toString().trim()
            if (username.isNotEmpty() && password.isNotEmpty()) {
                loginUser(username, password)
            } else {
                Toast.makeText(requireContext(), "Please enter both username and password.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        binding.iconViewPassword.setOnClickListener {
            togglePasswordVisibility(it)
        }

        binding.txtForgotPassword.setOnClickListener {
            onForgotPasswordClicked(it)
        }

        // Initialize Google Sign-In options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        // Initialize Google Sign-In client
        mGoogleSignInDentist = GoogleSignIn.getClient(requireContext(), gso)

        // Bind the Sign-In button and set up a click listener
        binding.btnGoogleSignIn.setOnClickListener {
            signIn()
        }

        return root
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInDentist.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Signed in successfully, show authenticated UI.
                Toast.makeText(requireContext(), "Sign-in successful.", Toast.LENGTH_SHORT).show()
                // Navigate to the dentist menu
                findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)

            } catch (e: ApiException) {
                // Handle sign-in failure
                Toast.makeText(requireContext(), "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onForgotPasswordClicked(view: View) {
        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_forget_password_dentist)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

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
                    val storedSalt = userSnapshot.child("salt").getValue(String::class.java)?.let { Base64.decode(it, Base64.DEFAULT) } ?: ByteArray(0)

                    val hashedPassword = hashPassword(password, storedSalt)

                    if (hashedPassword == storedHashedPassword) {
                        loggedInDentistUsername = username
                        getUserIdFromFirebase(username)
                        saveLoginStatus()
                        Toast.makeText(requireContext(), "Login successful!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_nav_login_dentist_to_nav_menu_dentist)
                    } else {
                        Toast.makeText(requireContext(), "Incorrect password.", Toast.LENGTH_SHORT).show()
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


    private fun getUserIdFromFirebase(username: String) {
        dbReference.orderByChild("username").equalTo(username).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userSnapshot = snapshot.children.first()
                    loggedInDentistUserId = userSnapshot.key
                    Log.e("LoginClientFragment", "loggedInDentistUserId: $loggedInDentistUserId")
                } else {
                    Toast.makeText(requireContext(), "User ID not found.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error retrieving user ID: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun hashPassword(password: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        return Base64.encodeToString(digest.digest(password.toByteArray()), Base64.DEFAULT)
    }

    // Ensure this method is public
    fun togglePasswordVisibility(view: View) {
        passwordVisible = !passwordVisible

        if (passwordVisible) {
            binding.etxtPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.iconViewPassword.setImageResource(R.drawable.visible_icon)
        }
//        else {
//            binding.etxtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
//            binding.iconViewPassword.setImageResource(R.drawable.hidden_icon)
//        }

        binding.etxtPassword.setSelection(binding.etxtPassword.text.length)
    }

    private fun saveLoginStatus() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.putString("username", loggedInDentistUsername)
        editor.putString("id", loggedInDentistUserId)
        editor.apply()
    }
}
