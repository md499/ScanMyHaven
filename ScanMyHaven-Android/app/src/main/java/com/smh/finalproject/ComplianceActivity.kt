package com.smh.finalproject

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ComplianceActivity : AppCompatActivity() {

    private val firestoreDB = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var textView: TextView
    private lateinit var totalMessyAreasTextView: TextView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compliance)

        backButton = findViewById(R.id.backButton)
        textView = findViewById(R.id.historyTitle)


        recyclerView = findViewById(R.id.historyRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        backButton = findViewById(R.id.backButton)
        textView = findViewById(R.id.historyTitle)

        recyclerView = findViewById(R.id.historyRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val historyList = mutableListOf<HistoryItem>()
        val currentUserID = FirebaseAuth.getInstance().currentUser?.uid

        val totalMessyAreasTextView: TextView = findViewById(R.id.totalMessyAreas)


        currentUserID?.let { uid ->
            firestoreDB.collection("images").whereEqualTo("userId", uid).get()
                .addOnSuccessListener { result ->
                    val historyMap = mutableMapOf<String, Int>()
                    val historyMapClean = mutableMapOf<String, Int>()

                    for (document in result) {
                        val timestamp = document.getString("timestamp")
                        val classification = document.getString("classification")

                        if (timestamp != null && classification == "messy") {
                            historyMap[timestamp] = (historyMap[timestamp] ?: 0) + 1
                        }

                        if (timestamp != null && classification == "clean") {
                            historyMapClean[timestamp] = (historyMapClean[timestamp] ?: 0) + 1
                        }

                    }

                    var totalMessyCount = 0

                    for ((_, messyCount) in historyMap) {
                        totalMessyCount += messyCount
                    }

                    totalMessyAreasTextView.text = "Messy Total: $totalMessyCount"
                    totalMessyAreasTextView.text = getString(R.string.total_messy_areas, totalMessyCount)


                    for ((date, messyCount) in historyMap) {
                        val isMessy = messyCount > 0
                        Log.d("isMessy", isMessy.toString())
                        historyList.add(HistoryItem(date, messyCount, isMessy, false))
                        Log.d("History", "Date: $date, Messy Count: $messyCount pending items")
                    }

                    for ((date, cleanCount) in historyMapClean) {
                        if (!historyMap.containsKey(date)) {
                            val isClean = cleanCount > 0
                            Log.d("isClean", isClean.toString())
                            historyList.add(HistoryItem(date, cleanCount, false, isClean))
                            Log.d("History", "Date: $date, Clean Count: $cleanCount items")
                        }
                    }

                    historyList.sortWith(compareByDescending{ getDateYear(it.date) })

                    val adapter = ComplianceAdapter(historyList)
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = adapter
                }

                .addOnFailureListener { exception ->
                    exception.printStackTrace()

                }
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(this@ComplianceActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    private fun getDateYear(date: String): Long {
        val parts = date.split("/")
        val year = parts[2].toIntOrNull() ?: 0
        val month = parts[0].toIntOrNull() ?: 0
        val day = parts[1].toIntOrNull() ?: 0

        return year * 10000L + month * 100L + day
    }


}
