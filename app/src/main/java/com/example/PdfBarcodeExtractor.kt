package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PdfBarcodeExtractor {
    private const val TAG = "PdfBarcodeExtractor"
    
    suspend fun extractBarcodes(context: Context, pdfUri: Uri): List<String> = withContext(Dispatchers.IO) {
        val foundCodes = mutableSetOf<String>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        
        try {
            pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                val pageCount = renderer.pageCount
                
                for (i in 0 until pageCount) {
                    processPage(renderer, i, foundCodes)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing PDF", e)
        } finally {
            renderer?.close()
            pfd?.close()
        }
        
        foundCodes.toList()
    }

    private suspend fun processPage(renderer: PdfRenderer, pageIndex: Int, foundCodes: MutableSet<String>) {
        var page: PdfRenderer.Page? = null
        try {
            page = renderer.openPage(pageIndex)
            
            // Render at high resolution, e.g. 3x standard (72 dpi * 3 = 216 dpi)
            // DataMatrix often needs good resolution.
            val density = 3.0f 
            val width = (page.width * density).toInt()
            val height = (page.height * density).toInt()
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // Fill with white background before rendering PDF (which is transparent by default)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            // Grid configurations to process: 1x1, 2x2, 3x3, 4x4
            val grids = listOf(1, 2, 3, 4)
            
            for (gridSize in grids) {
                val tileWidth = width / gridSize
                val tileHeight = height / gridSize
                
                for (row in 0 until gridSize) {
                    for (col in 0 until gridSize) {
                        val x = col * tileWidth
                        val y = row * tileHeight
                        
                        val w = if (col == gridSize - 1) width - x else tileWidth
                        val h = if (row == gridSize - 1) height - y else tileHeight
                        
                        if (w <= 0 || h <= 0) continue
                        
                        val tileBitmap = Bitmap.createBitmap(bitmap, x, y, w, h)
                        
                        // Scan normal tile
                        scanBitmapForDataMatrix(tileBitmap, foundCodes)
                        
                        // Scan inverted tile
                        val invertedBitmap = createInvertedBitmap(tileBitmap)
                        scanBitmapForDataMatrix(invertedBitmap, foundCodes)
                        
                        tileBitmap.recycle()
                        invertedBitmap.recycle()
                    }
                }
            }
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing page $pageIndex", e)
        } finally {
            page?.close()
        }
    }

    private suspend fun scanBitmapForDataMatrix(bitmap: Bitmap, foundCodes: MutableSet<String>) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_DATA_MATRIX)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        
        try {
            val barcodes = scanner.process(image).await()
            for (barcode in barcodes) {
                barcode.rawValue?.let { foundCodes.add(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning barcode in tile", e)
        } finally {
            scanner.close()
        }
    }

    private fun createInvertedBitmap(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()
        
        // Color matrix for inversion
        val mx = floatArrayOf(
            -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
            0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
            0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
            0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        )
        val colorMatrix = ColorMatrix(mx)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }
}
