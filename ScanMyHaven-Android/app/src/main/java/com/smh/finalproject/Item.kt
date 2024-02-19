package com.smh.finalproject

data class Item(
    val tag: String,
    val classification: String,
    val imageUrl: String,
    val checkedItems: Map<String, Boolean> = emptyMap(),
    val imgId: String
)

