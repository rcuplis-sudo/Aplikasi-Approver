package com.example.data.repository

import com.example.data.local.SignedDocumentDao
import com.example.data.local.SignedDocumentEntity
import kotlinx.coroutines.flow.Flow

class SignedDocumentRepository(private val dao: SignedDocumentDao) {
    val allDocuments: Flow<List<SignedDocumentEntity>> = dao.getAllDocuments()

    suspend fun getAllDocumentsList(): List<SignedDocumentEntity> {
        return dao.getAllDocumentsList()
    }

    suspend fun insertDocument(document: SignedDocumentEntity) {
        dao.insert(document)
    }

    suspend fun insertAll(documents: List<SignedDocumentEntity>) {
        dao.insertAll(documents)
    }

    suspend fun getDocumentById(id: String): SignedDocumentEntity? {
        return dao.getDocumentById(id)
    }

    suspend fun deleteDocument(id: String) {
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.deleteAll()
    }
}
