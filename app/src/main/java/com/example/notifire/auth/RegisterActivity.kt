package com.example.notifire.auth

import android.os.Bundle
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.notifire.R

class RegisterActivity : AppCompatActivity() {

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val name = findViewById<EditText>(R.id.editTextName)
        val email = findViewById<EditText>(R.id.editTextEmail)
        val password = findViewById<EditText>(R.id.editTextPassword)
        val isAdminSwitch = findViewById<Switch>(R.id.switchAdmin)
        val masterKey = findViewById<EditText>(R.id.editTextMasterKey)
        val registerBtn = findViewById<Button>(R.id.buttonRegister)

        masterKey.isEnabled = false

        isAdminSwitch.setOnCheckedChangeListener { _, isChecked ->
            masterKey.isEnabled = isChecked
        }

        registerBtn.setOnClickListener {
            viewModel.registerUser(
                name.text.toString(),
                email.text.toString(),
                password.text.toString(),
                isAdminSwitch.isChecked,
                masterKey.text.toString()
            )
        }

        viewModel.authResult.observe(this, Observer {
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            if (it.contains("Registro exitoso")) {
                finish()
            }
        })
    }
}
