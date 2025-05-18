package com.example.notifire.notifications

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import com.example.notifire.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NotificationHistoryActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messages = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_history)

        listView = findViewById(R.id.listViewNotifications)

        val user = auth.currentUser ?: return

        db.collection("notifications")
            .document(user.uid)
            .collection("items")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val title = doc.getString("title") ?: ""
                    val body = doc.getString("message") ?: ""
                    val line = "$title\n$body"
                    messages.add(line)
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
                listView.adapter = adapter
            }
    }
}
