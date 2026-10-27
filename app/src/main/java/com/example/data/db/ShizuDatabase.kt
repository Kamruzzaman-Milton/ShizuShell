package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.CommandHistoryEntity
import com.example.data.model.PinnedCommandEntity
import com.example.data.model.QuickChipEntity
import com.example.data.model.SessionEntity
import com.example.data.model.SnippetEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CommandHistoryEntity::class,
        PinnedCommandEntity::class,
        SnippetEntity::class,
        QuickChipEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShizuDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun pinnedDao(): PinnedDao
    abstract fun snippetDao(): SnippetDao
    abstract fun quickChipDao(): QuickChipDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: ShizuDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): ShizuDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ShizuDatabase::class.java,
                    "shizushell.db"
                ).addCallback(DatabaseCallback(scope)).build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(db: ShizuDatabase) {
                // Initial quick chips
                val defaultChips = listOf(
                    "dumpsys battery",
                    "pm list packages -3",
                    "getprop",
                    "df -h",
                    "id",
                    "settings list global"
                )
                defaultChips.forEachIndexed { index, cmd ->
                    db.quickChipDao().insert(QuickChipEntity(command = cmd, orderIndex = index))
                }

                // Initial snippets
                val defaultSnippets = listOf(
                    SnippetEntity(
                        title = "Grant Permission",
                        commandTemplate = "pm grant {package} {permission}",
                        description = "Grant a runtime or secure permission to an app",
                        category = "Package Manager"
                    ),
                    SnippetEntity(
                        title = "Dumpsys Service",
                        commandTemplate = "dumpsys {service}",
                        description = "Dump diagnostic output of an Android system service (e.g. battery, power, wifi, activity)",
                        category = "Diagnostics"
                    ),
                    SnippetEntity(
                        title = "Get Setting",
                        commandTemplate = "settings get {namespace} {key}",
                        description = "Query global, secure, or system setting values",
                        category = "Settings"
                    ),
                    SnippetEntity(
                        title = "Put Setting",
                        commandTemplate = "settings put {namespace} {key} {value}",
                        description = "Change a global, secure, or system setting",
                        category = "Settings"
                    ),
                    SnippetEntity(
                        title = "Force Stop App",
                        commandTemplate = "am force-stop {package}",
                        description = "Force kill all processes of the specified package",
                        category = "Package Manager"
                    ),
                    SnippetEntity(
                        title = "Start Activity",
                        commandTemplate = "am start -n {package}/{component}",
                        description = "Launch an intent activity directly",
                        category = "Activity Manager"
                    ),
                    SnippetEntity(
                        title = "Filter Logcat",
                        commandTemplate = "logcat -d -s {tag}",
                        description = "Dump recent logs filtered by tag",
                        category = "Logs"
                    ),
                    SnippetEntity(
                        title = "Take Screenshot",
                        commandTemplate = "screencap -p /sdcard/{filename}.png",
                        description = "Capture display screen to SD card",
                        category = "Media"
                    )
                )
                defaultSnippets.forEach { db.snippetDao().insert(it) }

                // Initial default session
                db.sessionDao().insert(
                    SessionEntity(
                        sessionId = "session_default",
                        name = "Main"
                    )
                )
            }
        }
    }
}
