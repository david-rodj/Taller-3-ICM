package com.example.taller3icm.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.taller3icm.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUser get() = auth.currentUser

    suspend fun register(
        nombre: String,
        identificacion: String,
        email: String,
        password: String,
        telefono: String
    ): Result<User> {
        return try {
            // 1. Crear usuario en Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Error al obtener UID")

            // 2. Crear documento en Firestore
            val user = User(
                uid = uid,
                nombre = nombre,
                identificacion = identificacion,
                email = email,
                telefono = telefono,
                latitud = 0.0,
                longitud = 0.0,
                conectado = false
            )

            firestore.collection("users").document(uid).set(user).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: throw Exception("Error al obtener UID")

            val userDoc = firestore.collection("users").document(uid).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: throw Exception("Usuario no encontrado en Firestore")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        auth.signOut()
    }

    suspend fun getCurrentUser(): User? {
        return try {
            val uid = currentUser?.uid ?: return null
            val userDoc = firestore.collection("users").document(uid).get().await()
            userDoc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}