package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BarcodeEntity
import com.example.data.ProductAttributeEntity
import com.example.data.ProductEntity
import com.example.data.ProductWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val dao = database.productDao()

    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<String?>(null)

    val allProducts: StateFlow<List<ProductWithDetails>> = dao.getAllProductsWithDetails()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredProducts = combine(allProducts, searchQuery, selectedCategory) { products, query, category ->
        products.filter { p ->
            val matchesSearch = p.product.name.contains(query, ignoreCase = true) ||
                    p.product.gtin.contains(query, ignoreCase = true) ||
                    p.barcodes.any { it.value.contains(query, ignoreCase = true) } ||
                    p.attributes.any { it.value.contains(query, ignoreCase = true) || it.name.contains(query, ignoreCase = true) }
            val matchesCategory = category == null || p.product.category == category
            matchesSearch && matchesCategory
        }.groupBy { it.product.category }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val categories = combine(allProducts) { (products) ->
        products.map { it.product.category }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val existingBarcodeTypes = dao.getBarcodeTypes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Прочее"))

    val existingAttributeNames = dao.getAttributeNames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getAttributeValues(name: String): Flow<List<String>> {
        return dao.getAttributeValues(name)
    }

    fun getProductByGtin(gtin: String): Flow<ProductWithDetails?> {
        return dao.getProductByGtin(gtin)
    }

    fun addOrUpdateProduct(
        product: ProductEntity,
        barcodes: List<BarcodeEntity>,
        attributes: List<ProductAttributeEntity>,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Verify constraints
                val productId = if (product.id == 0) {
                    dao.insertProduct(product).toInt()
                } else {
                    dao.updateProduct(product)
                    product.id
                }

                // Check constraints
                for (barcode in barcodes) {
                    val countOther = dao.countBarcodeInOtherProducts(barcode.value, productId)
                    if (countOther > 0) {
                        throw Exception("ШК ${barcode.value} уже существует в другом товаре.")
                    }
                    val typeCount = barcodes.count { it.type == barcode.type }
                    if (typeCount > 1) {
                        throw Exception("Не может быть более 1 ШК вида '${barcode.type}'.")
                    }
                }

                dao.deleteBarcodesForProduct(productId)
                for (barcode in barcodes) {
                    dao.insertBarcode(barcode.copy(productId = productId, id = 0))
                }

                dao.deleteAttributesForProduct(productId)
                for (attr in attributes) {
                    dao.insertAttribute(attr.copy(productId = productId, id = 0))
                }
                
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun deleteProduct(product: ProductEntity) {
        viewModelScope.launch {
            dao.deleteProduct(product)
        }
    }
}
