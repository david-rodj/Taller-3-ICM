package com.example.taller3icm.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.taller3icm.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun updateUserLocation(uid: String, lat: Double, lng: Double): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update(mapOf(
                    "latitud" to lat,
                    "longitud" to lng
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConnectionStatus(uid: String, conectado: Boolean): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update("conectado", conectado)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getOnlineUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereEqualTo("conectado", true)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull {
                    it.toObject(User::class.java)
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }

    suspend fun updateUserProfile(
        uid: String,
        nombre: String,
        identificacion: String,
        telefono: String
    ): Result<Unit> {
        return try {
            firestore.collection("users").document(uid)
                .update(mapOf(
                    "nombre" to nombre,
                    "identificacion" to identificacion,
                    "telefono" to telefono
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}