package com.example.taller3icm.navigation
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Map : Screen("map")
    object Profile : Screen("profile")
}