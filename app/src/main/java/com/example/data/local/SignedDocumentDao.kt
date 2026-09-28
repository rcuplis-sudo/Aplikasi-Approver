package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SignedDocumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: SignedDocumentEntity)

    @Query("SELECT * FROM signed_documents ORDER BY timestamp DESC")
    fun getAllDocuments(): Flow<List<SignedDocumentEntity>>

    @Query("SELECT * FROM signed_documents WHERE id = :id")
    suspend fun getDocumentById(id: String): SignedDocumentEntity?

    @Query("DELETE FROM signed_documents WHERE id = :id")
    suspend fun deleteById(id: String)

    @Delete
    suspend fun delete(document: SignedDocumentEntity)
}
