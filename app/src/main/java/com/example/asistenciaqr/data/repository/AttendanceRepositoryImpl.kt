package com.example.asistenciaqr.data.repository

import com.example.asistenciaqr.data.model.AttendanceRecord
import com.example.asistenciaqr.domain.repository.AttendanceRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class AttendanceRepositoryImpl : AttendanceRepository {
    private val db = FirebaseFirestore.getInstance()
    private val attendanceCollection = db.collection("attendance")

    override suspend fun registerAttendance(record: AttendanceRecord): Result<Boolean> {
        return try {
            val document = if (record.id.isEmpty()) {
                attendanceCollection.document() // Firebase genera ID
            } else {
                attendanceCollection.document(record.id)
            }

            val recordWithId = record.copy(id = document.id)
            document.set(recordWithId).await()
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
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val startOfDay = Timestamp(calendar.time)

            // Obtener TODOS los registros del usuario y filtrar localmente
            val allUserRecords = attendanceCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .toObjects(AttendanceRecord::class.java)

            // Filtrar por fecha localmente
            val todayRecords = allUserRecords.filter { record ->
                record.timestamp >= startOfDay
            }.sortedByDescending { it.timestamp }

            Result.success(todayRecords)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAttendanceByDateRange(
        startDate: Timestamp,
        endDate: Timestamp
    ): Result<List<AttendanceRecord>> {
        return try {
            val snapshot = attendanceCollection
                .whereGreaterThanOrEqualTo("timestamp", startDate)
                .whereLessThanOrEqualTo("timestamp", endDate)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val records = snapshot.toObjects(AttendanceRecord::class.java)
            Result.success(records)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}