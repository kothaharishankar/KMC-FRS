package com.example.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.AttendanceRecord
import com.example.model.User
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "employee_photos")
data class EmployeePhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: String,
    val employeeName: String,
    val date: String,
    val time: String,
    val photoBase64: String
)

@Dao
interface EmployeePhotoDao {
    @Query("SELECT * FROM employee_photos ORDER BY id DESC")
    fun getAllPhotos(): Flow<List<EmployeePhoto>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: EmployeePhoto)

    @Query("DELETE FROM employee_photos")
    suspend fun clearAll()
}

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY employeeId ASC")
    fun getAllEmployees(): Flow<List<User>>

    @Query("SELECT * FROM employees WHERE employeeId = :id")
    suspend fun getEmployeeById(id: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(users: List<User>)

    @Query("DELETE FROM employees WHERE employeeId = :id")
    suspend fun deleteEmployeeById(id: String): Int

    @Query("DELETE FROM employees")
    suspend fun clearAll()
}

@Dao
interface AttendanceRecordDao {
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :empId ORDER BY timestamp DESC")
    fun getRecordsForEmployee(empId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE employeeId = :empId AND date = :todayDate LIMIT 1")
    suspend fun getTodayRecord(empId: String, todayDate: String): AttendanceRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records")
    suspend fun clearAll()
}

@Database(entities = [EmployeePhoto::class, User::class, AttendanceRecord::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeePhotoDao(): EmployeePhotoDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceRecordDao(): AttendanceRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `employees` (`employeeId` TEXT NOT NULL, `name` TEXT NOT NULL, `email` TEXT NOT NULL, `phone` TEXT NOT NULL, `role` TEXT NOT NULL, `joinedDate` TEXT NOT NULL, `department` TEXT NOT NULL DEFAULT 'Technical Support', `designation` TEXT NOT NULL DEFAULT 'Technical Executive', `status` TEXT NOT NULL DEFAULT 'ACTIVE', `faceRegistered` INTEGER NOT NULL DEFAULT 1, `password` TEXT NOT NULL DEFAULT 'password123', PRIMARY KEY(`employeeId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `attendance_records` (`id` TEXT NOT NULL, `employeeId` TEXT NOT NULL, `employeeName` TEXT NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `status` TEXT NOT NULL, `lateDuration` TEXT, `latitude` REAL NOT NULL, `longitude` REAL NOT NULL, `locationName` TEXT NOT NULL, `photoUrl` TEXT, `lateByMinutes` INTEGER, `faceConfidence` REAL NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`employeeId`) REFERENCES `employees`(`employeeId`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendance_photos_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
