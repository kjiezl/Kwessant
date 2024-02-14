package com.kjiezl.kwessant

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import android.widget.ImageButton
import android.text.InputType
import android.util.Log
import android.widget.CheckBox
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class LogIn : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnSignUp: Button

    private lateinit var mAuth: FirebaseAuth

    private lateinit var btnTogglePasswordVisibility: ImageButton
    private var isPasswordVisible = false

    private val PREFS_NAME = "MyPrefsFile"
    private val REMEMBER_ME_KEY = "rememberMe"
    private lateinit var rememberMeToggle: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        supportActionBar?.hide()

        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        btnLogin = findViewById(R.id.btn_login)
        btnSignUp = findViewById(R.id.btn_signup)

        mAuth = FirebaseAuth.getInstance()

        val currentUser = mAuth.currentUser
        if (currentUser != null && getRememberMeState()) {
            val intent = Intent(this@LogIn, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        rememberMeToggle = findViewById(R.id.rememberMeToggle)
        rememberMeToggle.isChecked = getRememberMeState()

        rememberMeToggle.setOnCheckedChangeListener { _, isChecked ->
            saveRememberMeState(isChecked)
        }

        btnSignUp.setOnClickListener{
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener{
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                login(email, password)
            } else {
                Toast.makeText(this@LogIn, "Please fill in all credentials", Toast.LENGTH_SHORT).show()
            }
        }

        btnTogglePasswordVisibility = findViewById(R.id.btnTogglePasswordVisibility)
        btnTogglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }

    }

    private fun login(email: String, password: String){
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this@LogIn, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    val exception = task.exception
                    when {
                        exception is FirebaseAuthInvalidUserException && exception.errorCode == "ERROR_USER_NOT_FOUND" -> {
                            Toast.makeText(this@LogIn, "Account does not exist", Toast.LENGTH_SHORT).show()
                        }

                        exception is FirebaseAuthInvalidCredentialsException -> {
                            Toast.makeText(this@LogIn, "Incorrect password or email", Toast.LENGTH_SHORT)
                                .show()
                        }

                        exception != null && !isNetworkConnected() -> {
                            Toast.makeText(
                                this@LogIn,
                                "Login failed. Please check your internet connection",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            Log.e("LogIn", "Error during login: ${task.exception}")
                            Toast.makeText(
                                this@LogIn,
                                "Login failed. Please try again",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    }
                }
            }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible

        val drawableRes = if (isPasswordVisible) R.drawable.ic_visibility else R.drawable.ic_visibility_off
        val drawable = getDrawable(drawableRes)
        btnTogglePasswordVisibility.setImageDrawable(drawable)

        val inputType = if (isPasswordVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        edtPassword.inputType = inputType
        edtPassword.setSelection(edtPassword.text.length)
    }

    private fun saveRememberMeState(rememberMe: Boolean) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putBoolean(REMEMBER_ME_KEY, rememberMe)
        editor.apply()
    }

    private fun getRememberMeState(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getBoolean(REMEMBER_ME_KEY, false)
    }
}