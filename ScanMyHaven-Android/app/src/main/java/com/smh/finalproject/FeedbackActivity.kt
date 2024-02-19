package com.smh.finalproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class FeedbackActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var checklistReference: DocumentReference



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)
        val imgUrl = intent.getStringExtra("imgUrl")
        val classification = intent.getStringExtra("classification")
        val tag = intent.getStringExtra("tag")
        val date = intent.getStringExtra("date")
        val imgId = intent.getStringExtra("imgId")
        Log.d("Date", date.toString())


        var img = findViewById<ImageView>(R.id.feedbackimage)
        var text = findViewById<TextView>(R.id.feedbacktext)
        val feedbackIcon = findViewById<ImageView>(R.id.feedbackClassificationIcon)
        val labelText = findViewById<TextView>(R.id.labelText)

        Picasso.get().load(imgUrl).into(img)
        text.text = classification
        labelText.text = tag

        val checklistBundle = intent.getBundleExtra("checklist")
        val checklist: Map<String, Boolean>? = checklistBundle?.let {
            val result = mutableMapOf<String, Boolean>()
            for (key in it.keySet()) {
                result[key] = it.getBoolean(key)
            }
            result
        }

        val checklistItems: List<ChecklistItem> = checklist?.map { entry ->
            ChecklistItem(entry.key, entry.value)
        } ?: emptyList()

        if (classification.equals("Messy")) {
            feedbackIcon.setImageResource(R.drawable.round_add_circle_24);
        } else {
            feedbackIcon.setImageResource(R.drawable.baseline_check_circle_24);
        }

        recyclerView = findViewById<RecyclerView>(R.id.todoRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton = findViewById<Button>(R.id.backButton)

        val firestore = FirebaseFirestore.getInstance()
        checklistReference = firestore.collection("images").document(imgId ?: "")

        if (classification.equals("Messy")) {
            Log.d("FeedbackAct", "Items in onCreateViewHolder: $checklistItems")
            recyclerView.adapter = ChecklistAdapter(checklistItems, checklistReference)
        } else {
            val cleanMessage = getCleanMessage()
            val cleanMessageTextView = findViewById<TextView>(R.id.cleanTextResult)
            cleanMessageTextView.visibility = View.VISIBLE
            cleanMessageTextView.text = cleanMessage
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("date", date)
            startActivity(intent)
            finish()
        }
    }

    private fun getCleanMessage(): String {
        return ("Maintain cleanliness & safety standards")

    }
}

