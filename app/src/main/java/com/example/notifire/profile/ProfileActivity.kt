package com.example.notifire.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.notifire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.notifire.common.SessionManager
import com.example.notifire.auth.LoginActivity

class ProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var textName: TextView
    private lateinit var textEmail: TextView
    private lateinit var textRole: TextView
    private lateinit var buttonEdit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        textName = findViewById(R.id.textViewName)
        textEmail = findViewById(R.id.textViewEmail)
        textRole = findViewById(R.id.textViewRole)
        buttonEdit = findViewById(R.id.buttonEdit)

        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                textName.text = doc.getString("name")
                textEmail.text = doc.getString("email")
                textRole.text = doc.getString("role")
            }

        buttonEdit.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        val logoutButton = findViewById<Button>(R.id.buttonLogout)

        logoutButton.setOnClickListener {
            SessionManager(this).logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
