package com.example.taller3icm.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.core.graphics.createBitmap

object MarkerUtils {

    /**
     * Crea un marcador circular personalizado con color
     */
    fun createCustomMarker(
        context: Context,
        color: Int,
        size: Int = 80
    ): BitmapDescriptor {
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply {
            this.color = color
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val strokePaint = Paint().apply {
            this.color = android.graphics.Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius - 4f, paint)
        canvas.drawCircle(radius, radius, radius - 4f, strokePaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    /**
     * Crea un marcador con inicial del nombre
     */
    fun createTextMarker(
        context: Context,
        text: String,
        backgroundColor: Int,
        size: Int = 100
    ): BitmapDescriptor {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Círculo de fondo
        val bgPaint = Paint().apply {
            color = backgroundColor
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius - 4f, bgPaint)

        // Texto
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            isAntiAlias = true
            textSize = size / 2.5f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

        val initial = text.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        val yPos = (canvas.height / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(initial, radius, yPos, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}