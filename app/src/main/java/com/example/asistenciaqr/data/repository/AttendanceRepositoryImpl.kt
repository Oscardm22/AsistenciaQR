package com.example.asistenciaqr.data.repository

import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.repository.AttendanceRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AttendanceRepositoryImpl : AttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val attendanceCollection = db.collection("attendance")

    override suspend fun registerAttendance(record: AttendanceRecord): Result<Boolean> {
        return try {
            attendanceCollection.document(record.id).set(record).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAttendanceByUser(userId: String): Result<List<AttendanceRecord>> {
        return try {
            val records = attendanceCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(AttendanceRecord::class.java)

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllAttendance(): Result<List<AttendanceRecord>> {
        return try {
            val records = attendanceCollection
                .get()
                .await()
                .toObjects(AttendanceRecord::class.java)

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTodayAttendanceByUser(userId: String): Result<List<AttendanceRecord>> {
        return try {
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val records = attendanceCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .get()
                .await()
                .toObjects(AttendanceRecord::class.java)

            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}