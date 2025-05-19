// Archivo: app/src/main/java/com/example/notifire/auth/AuthViewModel.kt

package com.example.notifire.auth

import android.util.Log
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
        // Validar campos vacíos
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _authResult.value = "Por favor completa todos los campos"
            return
        }

        // Validar contraseña mínima
        if (password.length < 6) {
            _authResult.value = "La contraseña debe tener al menos 6 caracteres"
            return
        }

        // Validar correo electrónico
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authResult.value = "Formato de correo electrónico inválido"
            return
        }

        // Validar clave maestra para admin
        if (isAdmin && masterKey != Constants.MASTER_ADMIN_PASSWORD) {
            _authResult.value = "Clave maestra incorrecta para crear administrador"
            return
        }

        val role = if (isAdmin) "admin" else "user"

        // Mostrar que está procesando
        _authResult.value = "Procesando..."

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                val uid = it.user?.uid ?: return@addOnSuccessListener

                // Crear el objeto de usuario
                val userData = hashMapOf(
                    "uid" to uid,
                    "name" to name,
                    "email" to email,
                    "role" to role
                )

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        _authResult.value = if (role == "admin") {
                            "✅ Administrador creado exitosamente"
                        } else {
                            "✅ Usuario creado exitosamente"
                        }
                    }
                    .addOnFailureListener { error ->
                        _authResult.value = "Error al guardar datos: ${error.message}"
                        Log.e("AuthViewModel", "Error al guardar usuario en Firestore", error)
                    }
            }
            .addOnFailureListener { error ->
                when {
                    error.message?.contains("email address is already in use") == true -> {
                        _authResult.value = "Este correo ya está registrado"
                    }
                    error.message?.contains("network error") == true -> {
                        _authResult.value = "Error de red, verifica tu conexión a internet"
                    }
                    else -> {
                        _authResult.value = "Error en registro: ${error.message}"
                        Log.e("AuthViewModel", "Error en autenticación", error)
                    }
                }
            }
    }

    fun login(email: String, password: String) {
        // Validar campos vacíos
        if (email.isEmpty() || password.isEmpty()) {
            _authResult.value = "Por favor completa todos los campos"
            return
        }

        // Validar correo electrónico
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _authResult.value = "Formato de correo electrónico inválido"
            return
        }

        // Mostrar que está procesando
        _authResult.value = "Iniciando sesión..."

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _authResult.value = "Login exitoso"
            }
            .addOnFailureListener { error ->
                when {
                    error.message?.contains("password is invalid") == true -> {
                        _authResult.value = "Contraseña incorrecta"
                    }
                    error.message?.contains("no user record") == true -> {
                        _authResult.value = "Usuario no encontrado"
                    }
                    error.message?.contains("network error") == true -> {
                        _authResult.value = "Error de red, verifica tu conexión a internet"
                    }
                    else -> {
                        _authResult.value = "Error en login: ${error.message}"
                        Log.e("AuthViewModel", "Error en login", error)
                    }
                }
            }
    }
}