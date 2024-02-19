package com.smh.finalproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso


class ImageAdapter(private val items: List<Item>, private val onItemClick: (Item) -> Unit) :
    RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.image_text_fragment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = item.tag
        Picasso.get().load(item.imageUrl).into(holder.imageView)


        holder.classText.text = item.classification

        if (item.classification == "Messy") {
            val drawable =
                ContextCompat.getDrawable(holder.itemView.context, R.drawable.round_add_circle_24)
            holder.icon.setImageDrawable(drawable)

        } else {
            val drawable = ContextCompat.getDrawable(
                holder.itemView.context, R.drawable.baseline_check_circle_24
            )
            holder.icon.setImageDrawable(drawable)
        }


        holder.imageView.setOnClickListener {
            onItemClick.invoke(item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val icon: ImageView = itemView.findViewById(R.id.classificationIcon)
        val classText: TextView = itemView.findViewById(R.id.textViewClassification)
    }
}
