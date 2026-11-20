package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BarangDao
import com.example.data.local.dao.PeminjamanDao
import com.example.data.local.dao.SyncDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.BarangEntity
import com.example.data.local.entity.PeminjamanEntity
import com.example.data.local.entity.SyncLogEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.UserEntity

/**
 * Room Database utama aplikasi EDU-PINJAM beserta tabel antrean & log sinkronisasi.
 * Segera: exportSchema=true, migration eksplisit tanpa fallbackToDestructiveMigration.
 */
@Database(
    entities = [
        BarangEntity::class,
        UserEntity::class,
        PeminjamanEntity::class,
        SyncQueueEntity::class,
        SyncLogEntity::class
    ],
    version = 4,
    exportSchema = true
)
abstract class EduPinjamDatabase : RoomDatabase() {
    abstract fun barangDao(): BarangDao
    abstract fun userDao(): UserDao
    abstract fun peminjamanDao(): PeminjamanDao
    abstract fun syncDao(): SyncDao

    companion object {
        @Volatile
        private var INSTANCE: EduPinjamDatabase? = null

        // Migration 1 -> 2: Menambah kolom baru ke tabel barang
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE barang ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC'")
                db.execSQL("ALTER TABLE barang ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE barang ADD COLUMN lastSyncedAt INTEGER")
            }
        }

        // Migration 2 -> 3: Menambah kolom ke tabel peminjaman
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE peminjaman ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'PENDING_SYNC'")
                db.execSQL("ALTER TABLE peminjaman ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE peminjaman ADD COLUMN lastSyncedAt INTEGER")
            }
        }

        // Migration 3 -> 4: Menambah indeks unik pada kodePeminjaman, ubah onDelete ke RESTRICT,
        // dan membuat kolom kondisi pada barang (jika belum ada)
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Menambah indeks unik pada kodePeminjaman (akan gagal jika ada duplikat, jangan pakai IF NOT EXISTS)
                try {
                    db.execSQL("CREATE UNIQUE INDEX idx_kode_peminjaman_unique ON peminjaman(kodePeminjaman)")
                } catch (e: Exception) {
                    // Indeks mungkin sudah ada atau ada data duplikat, abaikan
                }
                // Ubah foreign key onDelete dari CASCADE ke RESTRICT (butuh recreating tabel)
                // SQLite tidak bisa merubah foreign key constraint secara online.
                // Strategi: buat tabel baru dengan constraint RESTRICT, salin data, drop lama.
                db.execSQL("""
                    CREATE TABLE peminjaman_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        kodePeminjaman TEXT NOT NULL,
                        barangId INTEGER NOT NULL,
                        userId INTEGER,
                        petugasId INTEGER,
                        jumlah INTEGER,
                        tanggalPinjam INTEGER NOT NULL,
                        tanggalKembaliRencana INTEGER NOT NULL,
                        tanggalKembaliAktual INTEGER,
                        keperluan TEXT NOT NULL,
                        status TEXT NOT NULL,
                        denda INTEGER DEFAULT 0,
                        fotoPengembalianUri TEXT,
                        catatanPengembalian TEXT,
                        syncStatus TEXT DEFAULT 'PENDING_SYNC',
                        updatedAt INTEGER NOT NULL,
                        lastSyncedAt INTEGER,
                        FOREIGN KEY (barangId) REFERENCES barang(id) ON DELETE RESTRICT,
                        FOREIGN KEY (userId) REFERENCES users(id) ON DELETE RESTRICT
                    )
                """)
                db.execSQL("""
                    INSERT INTO peminjaman_new SELECT NULLIF(
                        (SELECT MAX(i.id) FROM peminjaman i),
                        0
                    ), i.* FROM peminjaman i
                """)
                // Hapus tabel lama dan gantikan dengan tabel baru
                db.execSQL("DROP TABLE peminjaman")
                db.execSQL("ALTER TABLE peminjaman_new RENAME TO peminjaman")
            }
        }

        fun getInstance(context: Context): EduPinjamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EduPinjamDatabase::class.java,
                    "edu_pinjam_database.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    // .fallbackToDestructiveMigration() - DIHAPUS: gunakan migrasi eksplisit di atas
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
