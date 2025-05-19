package com.example.notifire.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.notifire.data.model.User
import com.example.notifire.common.Constants

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authResult = MutableLiveData<String>()
    val authResult: LiveData<String> = _authResult

    fun registerUser(name: String, email: String, password: String, isAdmin: Boolean, masterKey: String? = null) {
        if (isAdmin && masterKey != Constants.MASTER_ADMIN_PASSWORD) {
            _authResult.value = "Clave maestra incorrecta"
            return
        }

        val role = if (isAdmin) "admin" else "user"

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = it.user?.uid ?: return@addOnSuccessListener
                val user = User(uid, name, email, role)

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener {
                        _authResult.value = if (role == "admin") {
                            "✅ Administrador creado exitosamente"
                        } else {
                            "✅ Usuario creado exitosamente"
                        }

                    }
                    .addOnFailureListener {
                        _authResult.value = "Error al guardar datos: ${it.message}"
                    }
            }
            .addOnFailureListener {
                _authResult.value = "Error en autenticación: ${it.message}"
            }
    }

    fun login(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _authResult.value = "Login exitoso"
            }
            .addOnFailureListener {
                _authResult.value = "Error en login: ${it.message}"
            }
    }
}
