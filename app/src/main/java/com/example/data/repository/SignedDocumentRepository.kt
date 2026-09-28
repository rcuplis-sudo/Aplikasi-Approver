package com.example.data.repository

import com.example.data.local.SignedDocumentDao
import com.example.data.local.SignedDocumentEntity
import kotlinx.coroutines.flow.Flow

class SignedDocumentRepository(private val dao: SignedDocumentDao) {
    val allDocuments: Flow<List<SignedDocumentEntity>> = dao.getAllDocuments()

    suspend fun insertDocument(document: SignedDocumentEntity) {
        dao.insert(document)
    }

    suspend fun getDocumentById(id: String): SignedDocumentEntity? {
        return dao.getDocumentById(id)
    }

    suspend fun deleteDocument(id: String) {
        dao.deleteById(id)
    }
}
