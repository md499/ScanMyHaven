package com.smh.finalproject

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smh.finalproject.R
import com.google.firebase.firestore.DocumentReference

class ChecklistAdapter(
    private val items: List<ChecklistItem>, private val checklistReference: DocumentReference
) : RecyclerView.Adapter<ChecklistAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.checklist_fragment, parent, false)
        Log.d("com.example.finalproject.ChecklistAdapter", "Items in onCreateViewHolder: $items")
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = items[position]
        holder.textView.text = currentItem.text
        holder.checkbox.isChecked = currentItem.isChecked
        Log.d("com.example.finalproject.ChecklistAdapter", "Item: ${currentItem.text}, isChecked: ${currentItem.isChecked}")

        // Set a listener to handle checkbox state changes
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            items[position].isChecked = isChecked
            updateFirebaseChecklistItem(currentItem.text, isChecked)
            val txt = currentItem.text
            Log.d("com.example.finalproject.ChecklistAdapter", "Item '$txt' state changed to $isChecked")
        }

    }

    private fun updateFirebaseChecklistItem(text: String, isChecked: Boolean) {
        val checklistData = mapOf("checkedItems.$text" to isChecked)
        checklistReference.update(checklistData).addOnSuccessListener {
            Log.d("com.example.finalproject.ChecklistAdapter", "Item '$text' updated successfully")
        }.addOnFailureListener { e ->
            Log.e("com.example.finalproject.ChecklistAdapter", "Error updating item '$text': ${e.message}")
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkbox: CheckBox = itemView.findViewById(R.id.todoCheckbox)
        val textView: TextView = itemView.findViewById(R.id.todoText)
    }
}

