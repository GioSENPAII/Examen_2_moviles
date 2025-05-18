package com.example.notifire.profile

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notifire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        val nameEdit = findViewById<EditText>(R.id.editTextNewName)
        val saveButton = findViewById<Button>(R.id.buttonSave)

        val user = auth.currentUser ?: return

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                nameEdit.setText(doc.getString("name") ?: "")
            }

        saveButton.setOnClickListener {
            val newName = nameEdit.text.toString()

            db.collection("users").document(user.uid)
                .update("name", newName)
                .addOnSuccessListener {
                    Toast.makeText(this, "✅ Perfil actualizado", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "❌ Error al actualizar", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
