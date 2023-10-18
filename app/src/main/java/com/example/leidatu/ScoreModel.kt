package com.example.leidatu

class ScoreModel : ArrayList<ScoreItem>()

data class ScoreItem(
    val name: String,
    val value: Int
)