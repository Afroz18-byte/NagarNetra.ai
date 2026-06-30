package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Issue::class, 
        User::class, 
        AuthorityLog::class,
        AiDecisionLog::class,
        EmailLog::class,
        WardReport::class,
        CommunityChatMessage::class
    ], 
    version = 6, 
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun issueDao(): IssueDao
    abstract fun userDao(): UserDao
    abstract fun authorityLogDao(): AuthorityLogDao
    abstract fun aiDecisionLogDao(): AiDecisionLogDao
    abstract fun emailLogDao(): EmailLogDao
    abstract fun wardReportDao(): WardReportDao
    abstract fun communityChatDao(): CommunityChatDao



    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "nagar_netra_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Just double check in case onCreate wasn't executed or we need to ensure data is populated
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val issueDao = database.issueDao()
                    val all = issueDao.getAllIssues()
                    if (all.isEmpty()) {
                        populateDatabase(database)
                    }
                }
            }
        }

        private suspend fun populateDatabase(db: AppDatabase) {
            val issueDao = db.issueDao()
            val userDao = db.userDao()
            val logDao = db.authorityLogDao()

            // 1. Populate Citizen User Profile
            userDao.insertUser(
                User(
                    id = "citizen_user",
                    email = "citizen@nagarnetra.in",
                    name = "Rahul",
                    ward = "Indiranagar (Ward 80)",
                    trustScore = 88,
                    points = 150,
                    level = 4,
                    rank = 14,
                    reportsCount = 3,
                    badgesCount = 5
                )
            )

            // 2. Pre-populate Community Chat Messages
            val chatDao = db.communityChatDao()
            val now = System.currentTimeMillis()
            
            // Ward 80 Chat Messages
            chatDao.insertMessage(CommunityChatMessage(
                roomId = "ward_80",
                senderId = "citizen_1",
                senderName = "Vikram K.",
                senderTrustScore = 92,
                senderWard = "Indiranagar (Ward 80)",
                text = "Namaste all! Does anyone know if the corporation is repairing the broken streetlights on 100 Feet Road? It gets very dark after 8 PM.",
                timestamp = now - 3600000 * 2
            ))
            chatDao.insertMessage(CommunityChatMessage(
                roomId = "ward_80",
                senderId = "citizen_2",
                senderName = "Meera Rao",
                senderTrustScore = 75,
                senderWard = "Indiranagar (Ward 80)",
                text = "Yes, Vikram! I filed a report on NagarNetra yesterday. It's routed to BESCOM. Hopefully, they will resolve it soon.",
                timestamp = now - 3600000
            ))
            chatDao.insertMessage(CommunityChatMessage(
                roomId = "ward_80",
                senderId = "system_bot",
                senderName = "RouteBot 🤖",
                senderTrustScore = 100,
                senderWard = "NagarNetra AI System",
                text = "Alert: A new issue 'Open Garbage Pile near Metro Station' was reported in Indiranagar. View it in the Map tab to upvote and escalate.",
                timestamp = now - 1800000,
                isSystem = true
            ))
            
            // Nearby Chat Messages
            chatDao.insertMessage(CommunityChatMessage(
                roomId = "nearby",
                senderId = "citizen_3",
                senderName = "Anish Kumar",
                senderTrustScore = 82,
                senderWard = "Koramangala (Ward 151)",
                text = "Huge traffic jam on Outer Ring Road near Silk Board junction due to waterlogging. Advise taking alternative routes if traveling towards HSR.",
                timestamp = now - 3600000 * 3
            ))
            
            // Emergency Chat Messages
            chatDao.insertMessage(CommunityChatMessage(
                roomId = "emergency",
                senderId = "system_bot",
                senderName = "Emergency Control",
                senderTrustScore = 100,
                senderWard = "BBMP Command Center",
                text = "CRITICAL UPDATE: High voltage cable snapped near Double Road. BESCOM team dispatched. Please stay clear of water pools in the area.",
                timestamp = now - 3600000
            ))

        }
    }
}
