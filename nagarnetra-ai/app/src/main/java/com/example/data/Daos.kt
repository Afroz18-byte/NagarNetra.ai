package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {
    @Query("SELECT * FROM issues ORDER BY severity DESC, upvotes DESC")
    fun getAllIssuesFlow(): Flow<List<Issue>>

    @Query("SELECT * FROM issues ORDER BY severity DESC, upvotes DESC")
    suspend fun getAllIssues(): List<Issue>

    @Query("SELECT * FROM issues WHERE id = :id LIMIT 1")
    suspend fun getIssueById(id: String): Issue?

    @Query("SELECT * FROM issues WHERE id = :id LIMIT 1")
    fun getIssueByIdFlow(id: String): Flow<Issue?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: Issue)

    @Update
    suspend fun updateIssue(issue: Issue)

    @Query("DELETE FROM issues")
    suspend fun clearIssues()

    @Query("DELETE FROM issues WHERE id LIKE 'DEMO-%'")
    suspend fun deleteDemoIssues()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<User?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): User?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface AuthorityLogDao {
    @Query("SELECT * FROM authority_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AuthorityLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuthorityLog)

    @Query("DELETE FROM authority_logs")
    suspend fun clearLogs()
}

@Dao
interface AiDecisionLogDao {
    @Query("SELECT * FROM ai_decision_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<AiDecisionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AiDecisionLog)

    @Query("DELETE FROM ai_decision_logs")
    suspend fun clearLogs()
}

@Dao
interface EmailLogDao {
    @Query("SELECT * FROM email_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<EmailLog>>

    @Query("SELECT * FROM email_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<EmailLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EmailLog)

    @Query("DELETE FROM email_logs")
    suspend fun clearLogs()
}

@Dao
interface WardReportDao {
    @Query("SELECT * FROM ward_reports ORDER BY timestamp DESC")
    fun getAllReportsFlow(): Flow<List<WardReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: WardReport)

    @Query("DELETE FROM ward_reports")
    suspend fun clearReports()
}

@Dao
interface CommunityChatDao {
    @Query("SELECT * FROM community_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    fun getMessagesForRoomFlow(roomId: String): Flow<List<CommunityChatMessage>>

    @Query("SELECT * FROM community_messages WHERE roomId IN (:roomIds) ORDER BY timestamp ASC")
    fun getMessagesForRoomsFlow(roomIds: List<String>): Flow<List<CommunityChatMessage>>

    @Query("SELECT * FROM community_messages WHERE roomId = :roomId ORDER BY timestamp ASC")
    suspend fun getMessagesForRoom(roomId: String): List<CommunityChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CommunityChatMessage): Long

    @Update
    suspend fun updateMessage(message: CommunityChatMessage)

    @Query("UPDATE community_messages SET isHiddenByAi = 1 WHERE id = :messageId")
    suspend fun hideMessageByAi(messageId: Int)

    @Query("DELETE FROM community_messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Int)
}


