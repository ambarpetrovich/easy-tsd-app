package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val gtin: String,
    val category: String = "Без категории"
)

@Entity(
    tableName = "barcodes",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productId")
    ]
)
data class BarcodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val value: String,
    val type: String
)

@Entity(
    tableName = "product_attributes",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productId"),
        Index("name")
    ]
)
data class ProductAttributeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val name: String,
    val value: String
)

data class ProductWithDetails(
    @Embedded val product: ProductEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val barcodes: List<BarcodeEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val attributes: List<ProductAttributeEntity>
)

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsWithDetails(): Flow<List<ProductWithDetails>>

    @Transaction
    @Query("SELECT * FROM products WHERE gtin = :gtin LIMIT 1")
    fun getProductByGtin(gtin: String): Flow<ProductWithDetails?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: BarcodeEntity)

    @Delete
    suspend fun deleteBarcode(barcode: BarcodeEntity)

    @Query("DELETE FROM barcodes WHERE productId = :productId")
    suspend fun deleteBarcodesForProduct(productId: Int)

    @Query("SELECT DISTINCT type FROM barcodes ORDER BY type ASC")
    fun getBarcodeTypes(): Flow<List<String>>
    
    @Query("SELECT COUNT(*) FROM barcodes WHERE value = :value AND productId != :productId")
    suspend fun countBarcodeInOtherProducts(value: String, productId: Int): Int
    
    @Query("SELECT COUNT(*) FROM barcodes WHERE productId = :productId AND type = :type AND id != :excludeBarcodeId")
    suspend fun countBarcodeTypeInProduct(productId: Int, type: String, excludeBarcodeId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttribute(attribute: ProductAttributeEntity)

    @Query("DELETE FROM product_attributes WHERE productId = :productId")
    suspend fun deleteAttributesForProduct(productId: Int)

    @Query("SELECT DISTINCT name FROM product_attributes ORDER BY name ASC")
    fun getAttributeNames(): Flow<List<String>>

    @Query("SELECT DISTINCT value FROM product_attributes WHERE name = :name ORDER BY value ASC")
    fun getAttributeValues(name: String): Flow<List<String>>
}

@Database(entities = [ProductEntity::class, BarcodeEntity::class, ProductAttributeEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "products_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
