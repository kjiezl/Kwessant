package com.kjiezl.kwessant

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.text.InputType
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException


class SignUp : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var userStatus: TextView
    private lateinit var userLatestMessage: TextView

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mDbRef: DatabaseReference

    private lateinit var btnTogglePasswordVisibility: ImageButton
    private var isPasswordVisible = false

    private val PREFS_NAME = "MyPrefsFile"
    private val REMEMBER_ME_KEY = "rememberMe"
    private lateinit var rememberMeToggle: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        supportActionBar?.hide()

        edtName = findViewById(R.id.edt_name)
        edtEmail = findViewById(R.id.edt_email)
        edtPassword = findViewById(R.id.edt_password)
        edtPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        userStatus = findViewById(R.id.txt_status)
        userLatestMessage = findViewById(R.id.txt_latest_message)

        btnSignUp = findViewById(R.id.btn_signup)

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference()

        val currentUser = mAuth.currentUser
        if (currentUser != null && getRememberMeState()) {
            val intent = Intent(this@SignUp, MainActivity::class.java)
            finish()
            startActivity(intent)
        }

        rememberMeToggle = findViewById(R.id.rememberMeToggle)
        rememberMeToggle.isChecked = getRememberMeState()

        rememberMeToggle.setOnCheckedChangeListener { _, isChecked ->
            saveRememberMeState(isChecked)
        }

        btnSignUp.setOnClickListener{
            val name = edtName.text.toString()
            val email = edtEmail.text.toString()
            val password = edtPassword.text.toString()
            val status = userStatus.text.toString()
            val latestMessage = userLatestMessage.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                signUp(name, email, password, status, latestMessage)
            } else {
                Toast.makeText(this@SignUp, "Please fill in all credentials", Toast.LENGTH_SHORT).show()
            }
        }

        btnTogglePasswordVisibility = findViewById(R.id.btnTogglePasswordVisibility)
        btnTogglePasswordVisibility.setOnClickListener {
            togglePasswordVisibility()
        }
    }

    private fun signUp(name: String, email: String, password: String, status: String, latestMessage: String){
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    addUserToDatabase(name, email, mAuth.currentUser?.uid!!, status, latestMessage)
                    val intent = Intent(this@SignUp, MainActivity::class.java)
                    finish()
                    startActivity(intent)
                } else {
                    val exception = task.exception
                    when {
                        exception is FirebaseAuthException && exception.errorCode == "ERROR_EMAIL_ALREADY_IN_USE" -> {
                            Toast.makeText(this@SignUp, "User with this email already exists", Toast.LENGTH_SHORT).show()
                        }
                        exception is FirebaseAuthWeakPasswordException -> {
                            Toast.makeText(this@SignUp, "Password should be at least 6 characters long", Toast.LENGTH_SHORT).show()
                        }
                        exception != null && !isNetworkConnected() -> {
                            Toast.makeText(this@SignUp, "Sign-up failed. Please check your internet connection", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Log.e("SignUp", "Error during sign-up: ${task.exception}")
                            Toast.makeText(this@SignUp, "Sign-up failed. Please try again", Toast.LENGTH_SHORT).show()
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

    private fun addUserToDatabase(name: String, email: String, uid: String, status: String, latestMessage: String){
        mDbRef.child("user").child(uid).setValue(User(name, email, uid, status, latestMessage))
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