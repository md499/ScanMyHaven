package com.smh.finalproject

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth

class MainDisplayActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_display)

        showSignInOrRegisterDialog()
        auth = FirebaseAuth.getInstance()

        val editTextUsername = findViewById<EditText>(R.id.editTextEmail)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonRegister = findViewById<Button>(R.id.submitButton)


        val buttonLogin = findViewById<Button>(R.id.backButton)

        buttonLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        buttonRegister.setOnClickListener {
            val enteredUsername = editTextUsername.text.toString()
            val enteredPassword = editTextPassword.text.toString()

            auth.fetchSignInMethodsForEmail(enteredUsername).addOnCompleteListener { fetchTask ->
                if (fetchTask.isSuccessful) {
                    val signInMethods = fetchTask.result?.signInMethods ?: emptyList<String>()

                    if (signInMethods.isNotEmpty()) {
                        Toast.makeText(
                            this,
                            "User with this email already exists. Please use a different email.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        auth.createUserWithEmailAndPassword(enteredUsername, enteredPassword)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { emailVerificationTask ->
                                        if (emailVerificationTask.isSuccessful) {
                                            registerDialog()
                                        } else {
                                            Toast.makeText(
                                                this,
                                                "Failed to send verification email.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Registration failed: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Error checking user existence: ${fetchTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showSignInOrRegisterDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Email Requirements")
        builder.setMessage("1. Please enter a valid email address for verification purposes.." + "\n " +
                "2. Password must be at least 6 characters and needs an uppercase letter")
        builder.setPositiveButton("Cancel") { dialog, which ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun registerDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Registration")
        builder.setMessage("Verify your email!")
        builder.setPositiveButton("Visit Dashboard") { dialog, which ->
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        val dialog = builder.create()
        dialog.show()
    }
}
