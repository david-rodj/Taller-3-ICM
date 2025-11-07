package com.example.taller3icm.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.example.taller3icm.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

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
            Log.d("UserRepository", "Estado de conexión actualizado: $uid -> $conectado")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al actualizar estado: ${e.message}")
            Result.failure(e)
        }
    }

    // SOLUCIÓN: Versión mejorada con logging y mejor manejo
    fun getOnlineUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .whereEqualTo("conectado", true)  // Solo usuarios conectados
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserRepository", "Error en listener: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val user = doc.toObject(User::class.java)
                        // CRÍTICO: Verificar doblemente que conectado sea true
                        if (user != null && user.conectado) {
                            Log.d("UserRepository", "Usuario online: ${user.uid} - ${user.nombre}")
                            user
                        } else {
                            Log.d("UserRepository", "Usuario filtrado (conectado=false): ${doc.id}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("UserRepository", "Error al parsear usuario: ${e.message}")
                        null
                    }
                } ?: emptyList()

                Log.d("UserRepository", "Total usuarios conectados: ${users.size}")
                trySend(users)
            }

        awaitClose {
            Log.d("UserRepository", "Cerrando listener de usuarios")
            listener.remove()
        }
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