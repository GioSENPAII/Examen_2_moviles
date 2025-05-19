package com.example.notifire.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.notifire.R
import com.example.notifire.common.SessionManager
import com.example.notifire.home.HomeActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val email = findViewById<EditText>(R.id.editTextEmail)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val loginBtn = findViewById<Button>(R.id.buttonLogin)

        loginBtn.setOnClickListener {
            val emailStr = email.text.toString().trim()
            val passwordStr = password.text.toString().trim()

            if (emailStr.isEmpty() || passwordStr.isEmpty()) {
                Toast.makeText(this, "Por favor llena todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(emailStr, passwordStr)
        }

        val textRegister = findViewById<TextView>(R.id.textRegister)

        textRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }


        viewModel.authResult.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            if (it == "Login exitoso") {
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                uid?.let { id -> SessionManager(this).saveSession(id) }

                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            }
        })
    }
}
