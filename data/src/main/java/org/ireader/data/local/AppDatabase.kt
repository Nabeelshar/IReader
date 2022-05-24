package org.ireader.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.ireader.common_models.entities.Book
import org.ireader.common_models.entities.BookCategory
import org.ireader.common_models.entities.CatalogRemote
import org.ireader.common_models.entities.Category
import org.ireader.common_models.entities.Category.Companion.ALL_ID
import org.ireader.common_models.entities.Category.Companion.UNCATEGORIZED_ID
import org.ireader.common_models.entities.Chapter
import org.ireader.common_models.entities.Download
import org.ireader.common_models.entities.FontEntity
import org.ireader.common_models.entities.History
import org.ireader.common_models.entities.RemoteKeys
import org.ireader.common_models.entities.Update
import org.ireader.data.local.dao.BookCategoryDao
import org.ireader.data.local.dao.CatalogDao
import org.ireader.data.local.dao.CategoryDao
import org.ireader.data.local.dao.ChapterDao
import org.ireader.data.local.dao.DownloadDao
import org.ireader.data.local.dao.FontDao
import org.ireader.data.local.dao.HistoryDao
import org.ireader.data.local.dao.LibraryBookDao
import org.ireader.data.local.dao.LibraryDao
import org.ireader.data.local.dao.RemoteKeysDao
import org.ireader.data.local.dao.UpdatesDao
import java.util.concurrent.Executors

@Database(
    entities = [
        Book::class,
        CatalogRemote::class,
        Category::class,
        Chapter::class,
        Download::class,
        History::class,
        Update::class,
        RemoteKeys::class,
        FontEntity::class,
        BookCategory::class
    ],
    version = 18,
    exportSchema = true,
)
@TypeConverters(DatabaseConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract val libraryBookDao: LibraryBookDao
    abstract val chapterDao: ChapterDao
    abstract val remoteKeysDao: RemoteKeysDao
    abstract val downloadDao: DownloadDao
    abstract val catalogDao: CatalogDao
    abstract val historyDao: HistoryDao
    abstract val updatesDao: UpdatesDao
    abstract val libraryDao:LibraryDao
    abstract val categoryDao:CategoryDao
    abstract val bookCategoryDao: BookCategoryDao
    abstract val fontDao: FontDao

    companion object {
        const val DATABASE_NAME = "infinity_db"
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) :AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                // prepopulate the database after onCreate was called
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // insert the data on the IO Thread
                        Executors.newSingleThreadExecutor().execute {
                           getInstance(context).categoryDao.insertDate(systemCategories)
//                            db.query("""
//                                INSERT OR IGNORE INTO category VALUES (0, "", 0, 0, 0);
//                                INSERT OR IGNORE INTO category VALUES (-2, "", 0, 0, 0);
//                                CREATE TRIGGER IF NOT EXISTS system_categories_deletion_trigger BEFORE DELETE ON category
//                                BEGIN SELECT CASE
//                                  WHEN old.id <= 0 THEN
//                                    RAISE(ABORT, 'System category cant be deleted')
//                                  END;
//                                END;
//                            """.trimIndent())
                        }
                    }
                })
                .addMigrations(
                    MIGRATION_8_9,
                    MIGRATION_11_12
                )
                .fallbackToDestructiveMigration()
                .addCallback(object :RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        Executors.newSingleThreadExecutor().execute {

                        }
                    }
                })
                .build()

        val systemCategories = listOf<Category>(Category(UNCATEGORIZED_ID,"",0,0,0),Category(ALL_ID,"",0,0,0))

    }

}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE book_table ADD COLUMN beingDownloaded INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE book_table RENAME COLUMN download TO isDownloaded")
    }
}
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE library ADD COLUMN beingDownloaded INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE library RENAME COLUMN download TO isDownloaded")
    }
}
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE library ADD COLUMN tableId INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_12_11 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE history_table RENAME TO history")
    }
}
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE history_table RENAME TO history")
    }
}

