package com.example.notifire.admin

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.notifire.R
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import com.android.volley.toolbox.Volley
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.Request
import org.json.JSONArray
import org.json.JSONObject


class NotificationPanelActivity : AppCompatActivity() {

    private lateinit var listViewUsers: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var selectedUids: MutableMap<String, String> // name -> uid

    private val db = FirebaseFirestore.getInstance()
    private val userList = mutableListOf<String>()
    private val uidMap = mutableMapOf<String, String>() // name -> uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_panel)

        val editTitle = findViewById<EditText>(R.id.editTextTitle)
        val editMessage = findViewById<EditText>(R.id.editTextMessage)
        listViewUsers = findViewById(R.id.listViewUsers)
        val buttonSend = findViewById<Button>(R.id.buttonSend)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, userList)
        listViewUsers.adapter = adapter

        loadUsers()

        buttonSend.setOnClickListener {
            val title = editTitle.text.toString()
            val message = editMessage.text.toString()

            val checkedPositions = listViewUsers.checkedItemPositions
            val selectedTokens = mutableListOf<String>()

            for (i in 0 until userList.size) {
                if (checkedPositions.get(i)) {
                    val name = userList[i]
                    val uid = uidMap[name]
                    if (uid != null) {
                        // Recuperar token de ese UID
                        db.collection("tokens").document(uid).get().addOnSuccessListener { snap ->
                            val token = snap.getString("token")
                            if (token != null) {
                                selectedTokens.add(token)
                            }

                            if (selectedTokens.size == checkedPositions.size()) {
                                // Listo para enviar a backend
                                sendNotification(title, message, selectedTokens)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadUsers() {
        db.collection("users").get().addOnSuccessListener { result ->
            userList.clear()
            uidMap.clear()
            for (doc in result) {
                val name = doc.getString("name") ?: continue
                val uid = doc.id
                userList.add(name)
                uidMap[name] = uid
            }
            adapter.notifyDataSetChanged()
        }
    }

    private fun sendNotification(title: String, message: String, tokens: List<String>) {
        val url = "https://jsonplaceholder.typicode.com/posts" // SIMULACIÓN

        val jsonBody = JSONObject().apply {
            put("title", title)
            put("message", message)
            put("tokens", JSONArray(tokens))
        }

        val request = object : JsonObjectRequest(
            Method.POST, url, jsonBody,
            { response ->
                Toast.makeText(this, "✅ Notificación simulada enviada", Toast.LENGTH_SHORT).show()
                Log.d("Simulacion", "Respuesta: $response")
            },
            { error ->
                Toast.makeText(this, "❌ Error en simulación", Toast.LENGTH_SHORT).show()
                Log.e("Simulacion", "Error: $error")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                return mutableMapOf("Content-Type" to "application/json")
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

}
