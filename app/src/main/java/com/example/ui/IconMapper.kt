package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun getIconByName(name: String): ImageVector {
        return when (name) {
            "Fastfood" -> Icons.Default.Fastfood
            "ShoppingCart" -> Icons.Default.ShoppingCart
            "Receipt" -> Icons.Default.Receipt
            "DirectionsCar" -> Icons.Default.DirectionsCar
            "House" -> Icons.Default.House
            "MedicalServices" -> Icons.Default.MedicalServices
            "LocalPlay" -> Icons.Default.LocalPlay
            "Work" -> Icons.Default.Work
            "TrendingUp" -> Icons.Default.TrendingUp
            "Category" -> Icons.Default.Category
            "Star" -> Icons.Default.Star
            "AttachMoney" -> Icons.Default.AttachMoney
            "Build" -> Icons.Default.Build
            "School" -> Icons.Default.School
            "Flight" -> Icons.Default.Flight
            "FitnessCenter" -> Icons.Default.FitnessCenter
            else -> Icons.Default.Category
        }
    }
}
