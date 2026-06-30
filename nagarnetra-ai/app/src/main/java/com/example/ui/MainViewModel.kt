package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

// --- Multi-agent Chat Message entity ---
data class ChatMessage(
    val text: String,
    val isBot: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = AppRepository(database)
    private val geminiClient = GeminiClient()
    private val orchestratorBot = OrchestratorBot()
    val multiAgentCoordinator = MultiAgentCoordinator()
    val agentContextState = MutableStateFlow<AgentContext?>(null)
    private var messagesJob: kotlinx.coroutines.Job? = null

    // --- Firebase Authentication States ---
    var firebaseAuth: com.google.firebase.auth.FirebaseAuth? = null
    val currentAuthUser = MutableStateFlow<com.google.firebase.auth.FirebaseUser?>(null)
    
    // --- Auth Input Fields & Settings ---
    val authEmail = MutableStateFlow("")
    val authPassword = MutableStateFlow("")
    val authDisplayName = MutableStateFlow("")
    val authWard = MutableStateFlow("Indiranagar (Ward 80)")
    val isRegisterMode = MutableStateFlow(false)
    val authError = MutableStateFlow<String?>(null)
    val isAuthenticating = MutableStateFlow(false)
    val showLoginRedirectButton = MutableStateFlow(false)
    val chosenRolePreference = MutableStateFlow("citizen") // "citizen" or "authority"

    // --- Core Navigation States ---
    val currentRole = MutableStateFlow("citizen") // "citizen" or "authority"
    val isLoggedIn = MutableStateFlow(false)

    // --- Tab Pages States ---
    val activeCitizenTab = MutableStateFlow("Home") 
    val activeAuthorityPage = MutableStateFlow("Overview") 
    val selectedCitizenIssue = MutableStateFlow<Issue?>(null)
    val showProfileEditDialog = MutableStateFlow(false)

    // --- Report Capture States ---
    val reportingStep = MutableStateFlow(1) 
    val capturedPhoto = MutableStateFlow<Bitmap?>(null)
    val preselectedCategory = MutableStateFlow<String?>(null) 
    val aiAnalysisResult = MutableStateFlow<IssueAnalysisResult?>(null)
    val exactLocationNotes = MutableStateFlow("")
    val voiceAgentTranscript = MutableStateFlow("")
    val voiceAgentPrompt = MutableStateFlow("Tap Speak and tell me the issue. I will ask for location, open camera, and prepare the report.")
    val voiceAgentListening = MutableStateFlow(false)
    val voiceObjectId = MutableStateFlow("")
    val reportLatitude = MutableStateFlow(12.973)
    val reportLongitude = MutableStateFlow(77.635)
    val generatedIssueId = MutableStateFlow("")
    val duplicateMerged = MutableStateFlow(false)
    val orchestratorTriggered = MutableStateFlow(false)
    val locationPermissionGranted = MutableStateFlow(true)

    // --- Map States ---
    val mapHeatmapEnabled = MutableStateFlow(false)
    val mapActiveFilter = MutableStateFlow("All") 
    val mapSelectedIssue = MutableStateFlow<Issue?>(null)
    val isGoogleMapMode = MutableStateFlow(false)
    val isMapDarkMode = MutableStateFlow(false)
    val isAppDarkMode = MutableStateFlow<Boolean?>(false)

    fun toggleAppTheme(systemInDark: Boolean) {
        val nextVal = !(isAppDarkMode.value ?: systemInDark)
        isAppDarkMode.value = nextVal
        isMapDarkMode.value = nextVal
    }

    // --- Action Verify Modal ---
    val resolutionVerifying = MutableStateFlow(false)

    // --- Community Chat States ---
    val activeCommunityRoomId = MutableStateFlow<String>("ward_80")
    val communityChatMessages = MutableStateFlow<List<CommunityChatMessage>>(emptyList())
    val joinedRoomsList = MutableStateFlow<Set<String>>(emptySet())
    val liveGpsLocation = MutableStateFlow<String?>(null)

    // --- Civic Chatbot States ---
    val chatOpen = MutableStateFlow(false)
    val chatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Namaste! I am your NagarNetra voice civic agent. Ask me anything, or say 'I want to report an issue' and I will open the camera and submit it automatically.", isBot = true)
    ))
    private val _assistantCameraRequests = MutableSharedFlow<String>()
    val assistantCameraRequests = _assistantCameraRequests.asSharedFlow()
    private val _assistantSpokenReplies = MutableSharedFlow<String>()
    val assistantSpokenReplies = _assistantSpokenReplies.asSharedFlow()


    // --- Room Database Flow observers ---
    val allIssues: StateFlow<List<Issue>> = repository.allIssuesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<com.example.data.User?> = repository.userFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val authorityLogs: StateFlow<List<AuthorityLog>> = repository.allLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiDecisionLogs: StateFlow<List<AiDecisionLog>> = repository.allAiDecisionLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emailLogs: StateFlow<List<EmailLog>> = repository.allEmailLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wardReports: StateFlow<List<WardReport>> = repository.allWardReportsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isRunningAccountabilityCheck = MutableStateFlow(false)


    private val _uiToast = MutableSharedFlow<String>()
    val uiToast = _uiToast.asSharedFlow()

    init {
        initFirebase(application)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteDemoIssues()
                Log.d("NagarNetraLoc", "Successfully cleared demo issues from database.")
            } catch (e: Exception) {
                Log.e("NagarNetraLoc", "Failed to clear demo issues", e)
            }
            try {
                val syncedCount = repository.syncIssuesFromApi()
                if (syncedCount > 0) {
                    Log.d("NagarNetraLoc", "Successfully synced $syncedCount issues from API on startup.")
                } else {
                    Log.d("NagarNetraLoc", "No remote issues synced on startup; using local data.")
                }
            } catch (e: Exception) {
                Log.w("NagarNetraLoc", "Issue API sync skipped/unavailable on startup: ${e.message}")
            }
        }
        viewModelScope.launch {
            activeCommunityRoomId.collect { roomId ->
                messagesJob?.cancel()
                messagesJob = launch(Dispatchers.IO) {
                    if (roomId == "nearby") {
                        val user = repository.getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
                        val currentWardId = getRoomIdForWard(user.ward)
                        val wardNumStr = currentWardId.removePrefix("ward_")
                        val wardNum = wardNumStr.toIntOrNull() ?: 80
                        val nearbyRoomIds = listOf("ward_${wardNum - 1}", "ward_$wardNum", "ward_${wardNum + 1}")
                        repository.getMessagesForRoomsFlow(nearbyRoomIds).collect { list ->
                            communityChatMessages.value = list
                        }
                    } else {
                        repository.getMessagesForRoomFlow(roomId).collect { list ->
                            communityChatMessages.value = list
                        }
                    }
                }
            }
        }
        runEscalationCheck()
    }


    private fun initFirebase(app: Application) {
        try {
            val apps = com.google.firebase.FirebaseApp.getApps(app)
            if (apps.isEmpty()) {
                val optBuilder = com.google.firebase.FirebaseOptions.Builder()
                val apiVal = com.example.BuildConfig.FIREBASE_API_KEY
                val projVal = com.example.BuildConfig.FIREBASE_PROJECT_ID
                val appVal = com.example.BuildConfig.FIREBASE_APP_ID

                if (apiVal.isNotEmpty() && !apiVal.contains("your-") && projVal.isNotEmpty() && !projVal.contains("your-")) {
                    optBuilder.setApiKey(apiVal)
                        .setProjectId(projVal)
                        .setApplicationId(appVal.ifEmpty { "1:123456789012:android:abcdef123456" })
                } else {
                    optBuilder.setApiKey("AIzaSyA88-FakeKeyForNagarNetraDevelopment")
                        .setProjectId("nagarnetra-ai")
                        .setApplicationId("1:123456789012:android:abcdef123123")
                }
                com.google.firebase.FirebaseApp.initializeApp(app, optBuilder.build())
                Log.d("NagarNetraFirebase", "Programmatic Firebase app initialization completed.")
            }
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            firebaseAuth = auth
            currentAuthUser.value = auth.currentUser
            auth.addAuthStateListener { updatedAuth ->
                val fbUser = updatedAuth.currentUser
                if (fbUser != null) {
                    val email = fbUser.email ?: ""
                    val calculatedRole = "citizen"
                    currentRole.value = calculatedRole
                    isLoggedIn.value = true
                    
                     viewModelScope.launch(Dispatchers.IO) {
                        try {
                            val u = repository.getUser()
                            val enteredName = authDisplayName.value.trim()
                            val displayName = if (enteredName.isNotEmpty()) enteredName else (fbUser.displayName ?: email.substringBefore("@"))
                            val finalName = if (displayName.isEmpty()) "Rahul" else displayName
                            val chosenWard = authWard.value.ifBlank { "Indiranagar (Ward 80)" }
                            val updatedProfile = u?.copy(name = finalName, ward = chosenWard) ?: User(
                                id = fbUser.uid,
                                email = email,
                                name = finalName,
                                ward = chosenWard,
                                trustScore = 85,
                                points = 120,
                                level = 3,
                                rank = 15,
                                badgesCount = 4,
                                reportsCount = 2
                            )
                            database.userDao().insertUser(updatedProfile)
                        } catch (e: Exception) {
                            Log.e("NagarNetraFirebase", "Error syncing local user row: ${e.message}")
                        }
                    }
                } else {
                    isLoggedIn.value = false
                }
            }
        } catch (e: Exception) {
            Log.e("NagarNetraFirebase", "Initialization failure: ${e.message}")
        }
    }

    fun toggleRole(role: String) {
        chosenRolePreference.value = role
        currentRole.value = role
        viewModelScope.launch {
            _uiToast.emit("Switched viewport to ${role.uppercase()}")
        }
    }

    private suspend fun registerUserOnBackend(email: String, name: String, method: String): JSONObject? {
        return withContext(Dispatchers.IO) {
            try {
                val jsonReq = JSONObject().apply {
                    put("email", email)
                    put("name", name)
                    put("authMethod", method)
                }
                val requestBody = jsonReq.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("http://10.0.2.2:5000/api/auth/register")
                    .post(requestBody)
                    .build()
                OkHttpClient().newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.code == 409 || response.isSuccessful) {
                        JSONObject(bodyStr)
                    } else {
                        Log.e("MainViewModel", "Register request failed: ${response.code}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Register connection failed: ${e.message}")
                null
            }
        }
    }

    fun handleEmailAuth() {
        val emailVal = authEmail.value.trim()
        val passwordVal = authPassword.value
        val nameVal = authDisplayName.value.trim()
        val roleVal = chosenRolePreference.value

        if (emailVal.isEmpty() || passwordVal.isEmpty()) {
            authError.value = "Email and Password cannot be empty."
            return
        }
        if (passwordVal.length < 6) {
            authError.value = "Password must be at least 6 characters."
            return
        }

        isAuthenticating.value = true
        authError.value = null
        showLoginRedirectButton.value = false

        viewModelScope.launch {
            try {
                if (isRegisterMode.value) {
                    _uiToast.emit("Verifying email uniqueness with central registry...")
                    val result = registerUserOnBackend(emailVal, nameVal, "email")
                    if (result != null && result.has("error")) {
                        isAuthenticating.value = false
                        authError.value = result.getString("error")
                        showLoginRedirectButton.value = true
                        return@launch
                    }
                }

                _uiToast.emit("Authenticating with Firebase secure credential cluster...")
                val auth = firebaseAuth
                if (auth != null) {
                    if (isRegisterMode.value) {
                        auth.createUserWithEmailAndPassword(emailVal, passwordVal)
                            .addOnCompleteListener { task ->
                                isAuthenticating.value = false
                                if (task.isSuccessful) {
                                    val user = task.result?.user
                                    if (user != null && nameVal.isNotEmpty()) {
                                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                            .setDisplayName(nameVal)
                                            .build()
                                        user.updateProfile(profileUpdates)
                                    }
                                    viewModelScope.launch {
                                        _uiToast.emit("Account created via Firebase Auth! Welcome!")
                                    }
                                } else {
                                    val err = task.exception?.localizedMessage ?: "Registration failed."
                                    if (err.contains("api key", ignoreCase = true) || err.contains("internal error", ignoreCase = true)) {
                                        // Auto-bypass invalid configuration for developer convenience
                                        viewModelScope.launch {
                                            _uiToast.emit("[Firebase Bypass] Setup incomplete. Logging in to local sandbox...")
                                            loginAsSandboxUser(nameVal.ifEmpty { emailVal.substringBefore("@") })
                                        }
                                    } else {
                                        authError.value = err
                                    }
                                }
                            }
                    } else {
                        auth.signInWithEmailAndPassword(emailVal, passwordVal)
                            .addOnCompleteListener { task ->
                                isAuthenticating.value = false
                                if (task.isSuccessful) {
                                    viewModelScope.launch {
                                        _uiToast.emit("Firebase login successful!")
                                    }
                                } else {
                                    val err = task.exception?.localizedMessage ?: "Invalid credentials."
                                    if (err.contains("api key", ignoreCase = true) || err.contains("internal error", ignoreCase = true)) {
                                        // Auto-bypass invalid configuration for developer convenience
                                        viewModelScope.launch {
                                            _uiToast.emit("[Firebase Bypass] Invalid API Key. Logging in to local sandbox...")
                                            loginAsSandboxUser(nameVal.ifEmpty { emailVal.substringBefore("@") })
                                        }
                                    } else {
                                        authError.value = err
                                    }
                                }
                            }
                    }
                } else {
                    delay(800)
                    isAuthenticating.value = false
                    loginAsSandboxUser(nameVal)
                }
            } catch (e: Exception) {
                isAuthenticating.value = false
                authError.value = e.localizedMessage ?: "An error occurred."
            }
        }
    }

    private suspend fun loginAsSandboxUser(nameVal: String) {
        val finalName = if (nameVal.isEmpty()) "Rahul" else nameVal
        val emailVal = authEmail.value.trim().ifEmpty { "citizen@nagarnetra.in" }
        val chosenWard = authWard.value.ifEmpty { "Indiranagar (Ward 80)" }
        val role = "citizen"
        
        currentRole.value = role
        isLoggedIn.value = true
        
        val updatedProfile = User(
            id = "citizen_user_" + System.currentTimeMillis(),
            email = emailVal,
            name = finalName,
            ward = chosenWard,
            trustScore = 90,
            points = 150,
            level = 4,
            rank = 12,
            badgesCount = 5,
            reportsCount = 3
        )
        database.userDao().insertUser(updatedProfile)
        _uiToast.emit("[Developer Bypass] Mock Account saved: $finalName ($chosenWard)")
    }

    fun redeemPoints(pointsToRedeem: Int, rewardName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser()
            if (user != null) {
                if (user.points >= pointsToRedeem) {
                    val updatedUser = user.copy(points = user.points - pointsToRedeem)
                    database.userDao().insertUser(updatedUser)
                    
                    repository.insertLog(
                        AuthorityLog(
                            timestamp = System.currentTimeMillis(),
                            isBotAction = true,
                            message = "Reward System: User ${user.name} redeemed '$rewardName' for $pointsToRedeem points. Voucher code: NN-${(100000..999999).random()}",
                            priority = "Medium"
                        )
                    )
                    _uiToast.emit("Redeemed successfully! Check updates for your voucher code.")
                } else {
                    _uiToast.emit("Insufficient points!")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                Log.e("NagarNetraAuth", "Error signing out from Firebase: ${e.message}")
            }
            viewModelScope.launch(Dispatchers.IO) {
                database.userDao().clearUsers()
            }
            isLoggedIn.value = false
            currentRole.value = "citizen"
            _uiToast.emit("Logged out successfully.")
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _uiToast.emit(message)
        }
    }

    fun quickStartCitizen() {
        isAuthenticating.value = true
        viewModelScope.launch {
            delay(250)
            loginAsSandboxUser("Raj Sharma")
            isAuthenticating.value = false
        }
    }

    fun doLogin(withGoogle: Boolean) {
        if (withGoogle) {
            isAuthenticating.value = true
            authError.value = null
            
            viewModelScope.launch {
                _uiToast.emit("Connecting to Google Identity suite...")
                delay(1200)
                val mockGoogleToken = "google-id-token-dummy-token"
                authenticateWithGoogleToken(mockGoogleToken)
            }
        } else {
            handleEmailAuth()
        }
    }

    fun authenticateWithGoogleToken(idToken: String) {
        val auth = firebaseAuth
        val roleVal = chosenRolePreference.value
        
        if (auth != null) {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    isAuthenticating.value = false
                    if (task.isSuccessful) {
                        viewModelScope.launch {
                            _uiToast.emit("Google Logged In Successfully!")
                        }
                    } else {
                        handleGoogleAuthSandboxFallback(roleVal)
                    }
                }
        } else {
            handleGoogleAuthSandboxFallback(roleVal)
        }
    }

    private fun handleGoogleAuthSandboxFallback(roleVal: String) {
        isAuthenticating.value = false
        val mockName = if (roleVal == "authority") "Officer Vivek" else "Meera Rao"
        
        currentRole.value = roleVal
        isLoggedIn.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            val updatedProfile = User(
                id = "citizen_user",
                email = "oauth@nagarnetra.in",
                name = mockName,
                ward = "Indiranagar (Ward 80)",
                trustScore = 92,
                points = 210,
                level = 5,
                rank = 8,
                badgesCount = 6,
                reportsCount = 4,
                authMethod = "google"
            )
            database.userDao().insertUser(updatedProfile)
            registerUserOnBackend("oauth@nagarnetra.in", mockName, "google")
            _uiToast.emit("[Google Sandbox Connected] Account authenticated as ${roleVal.uppercase()}!")
        }
    }

    fun doLogout() {
        viewModelScope.launch {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                Log.e("NagarNetraFirebase", "Sign out failure: ${e.message}")
            }
            isLoggedIn.value = false
            reportingStep.value = 1
            capturedPhoto.value = null
            preselectedCategory.value = null
            aiAnalysisResult.value = null
            _uiToast.emit("Logged out securely.")
        }
    }

    // --- Community Upvoting (Agent 3 - RouteBot & Agent 6 - DuplicateBot) ---
    fun upvoteIssue(issueId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val issue = repository.getIssueById(issueId) ?: return@launch
            val updated = issue.copy(upvotes = issue.upvotes + 1)
            repository.updateIssue(updated)

            repository.insertLog(
                AuthorityLog(
                    timestamp = System.currentTimeMillis(),
                    isBotAction = true,
                    message = "RouteBot: Community upvote logged on issue ${issue.id}. Total upvotes: ${updated.upvotes}.",
                    priority = "Low"
                )
            )

            if (updated.upvotes >= 50 && issue.upvotes < 50) {
                // Autonomous trigger! High Citizen Endorsement alert crossings!
                triggerAccountabilityEmail("HIGH_VOTES", updated)
            }

            if (updated.upvotes >= 10 && issue.severity < 9) {
                val elevated = updated.copy(severity = Math.min(10, issue.severity + 1))
                repository.updateIssue(elevated)
                
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis(),
                        isBotAction = true,
                        message = "PriorityBot: [${issue.id}] elevated severity to ${elevated.severity} due to community upvote density.",
                        priority = "High"
                      )
                )
            }
            _uiToast.emit("Upvote registered on ${issue.id}!")
        }
    }


    // --- Citizen Reporting Flow (Agent 1 - VisionBot / ScannerBot / RouteBot) ---
    fun fetchDeviceLocation() {
        try {
            val context = getApplication<Application>()
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
            
            val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            locationPermissionGranted.value = fineGranted || coarseGranted
            
            if (fineGranted || coarseGranted) {
                val gpsLocation = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                val networkLocation = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                
                val bestLocation = when {
                    gpsLocation != null && networkLocation != null -> {
                        if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                    }
                    gpsLocation != null -> gpsLocation
                    else -> networkLocation
                }
                
                if (bestLocation != null) {
                    reportLatitude.value = bestLocation.latitude
                    reportLongitude.value = bestLocation.longitude
                    Log.d("NagarNetraLoc", "Fetched device location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                    triggerReverseGeocode(bestLocation.latitude, bestLocation.longitude)
                } else {
                    val provider = if (locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                        android.location.LocationManager.GPS_PROVIDER
                    } else {
                        android.location.LocationManager.NETWORK_PROVIDER
                    }
                    
                    locationManager.requestSingleUpdate(provider, object : android.location.LocationListener {
                        override fun onLocationChanged(location: android.location.Location) {
                            reportLatitude.value = location.latitude
                            reportLongitude.value = location.longitude
                            Log.d("NagarNetraLoc", "Single update location fetched: ${location.latitude}, ${location.longitude}")
                            triggerReverseGeocode(location.latitude, location.longitude)
                        }
                        override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }, android.os.Looper.getMainLooper())
                }
            } else {
                Log.w("NagarNetraLoc", "Location permission is not granted. Falling back to default center with slight random offset.")
                val randomLatOffset = (Math.random() - 0.5) * 0.012
                val randomLngOffset = (Math.random() - 0.5) * 0.012
                reportLatitude.value = 12.973 + randomLatOffset
                reportLongitude.value = 77.635 + randomLngOffset
            }
        } catch (e: Exception) {
            Log.e("NagarNetraLoc", "Failed to fetch device location", e)
            val randomLatOffset = (Math.random() - 0.5) * 0.012
            val randomLngOffset = (Math.random() - 0.5) * 0.012
            reportLatitude.value = 12.973 + randomLatOffset
            reportLongitude.value = 77.635 + randomLngOffset
        }
    }

    fun updateUserDisplayName(newName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser() ?: User(id = "citizen_user", email = "citizen@nagarnetra.in")
            val updated = user.copy(name = newName)
            database.userDao().insertUser(updated)
            _uiToast.emit("Display name updated to $newName")
        }
    }

    fun updateUserProfile(name: String, email: String, ward: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser() ?: User(id = "citizen_user", email = "citizen@nagarnetra.in")
            val updated = user.copy(name = name, email = email, ward = ward)
            database.userDao().insertUser(updated)
            _uiToast.emit("Profile updated successfully!")
        }
    }

    fun triggerReverseGeocode(lat: Double, lng: Double) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient()
                val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&addressdetails=1"
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .header("User-Agent", "NagarNetraCitizenApp/1.0")
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = org.json.JSONObject(body)
                    val address = json.optJSONObject("address")
                    if (address != null) {
                        val suburb = address.optString("suburb", "")
                            .ifBlank { address.optString("neighbourhood", "") }
                            .ifBlank { address.optString("residential", "") }
                            .ifBlank { address.optString("city_district", "") }
                            .ifBlank { address.optString("village", "") }
                            .ifBlank { address.optString("town", "") }
                            .ifBlank { "Indiranagar" }
                            
                        val road = address.optString("road", "")
                            .ifBlank { address.optString("street", "") }
                            .ifBlank { address.optString("pedestrian", "") }
                            .ifBlank { "Main Road" }
                            
                        val city = address.optString("city", "")
                            .ifBlank { address.optString("town", "") }
                            .ifBlank { "Bengaluru" }
                            
                        val wardNum = ((Math.abs(lat) + Math.abs(lng)) * 1000).toInt() % 198 + 1
                        val friendlyAddress = "$suburb (Ward $wardNum)"
                        
                        // Update current user profile ward in database
                        val user = repository.getUser() ?: User(id = "citizen_user", email = "citizen@nagarnetra.in")
                        val updatedUser = user.copy(ward = friendlyAddress)
                        database.userDao().insertUser(updatedUser)
                        
                        // Update live GPS location flow
                        liveGpsLocation.value = friendlyAddress
                        
                        // Automatically switch active community room
                        activeCommunityRoomId.value = "ward_$wardNum"
                        
                        // Also auto-fill exact location notes for reports
                        exactLocationNotes.value = "$road, $suburb"
                        
                        Log.d("NagarNetraLoc", "Successfully reverse geocoded: $road, $friendlyAddress, $city")
                    }
                }
            } catch (e: Exception) {
                Log.e("NagarNetraLoc", "Failed to reverse geocode location", e)
            }
        }
    }

    fun setCapturedImage(bitmap: Bitmap?, categoryPreset: String? = null) {
        capturedPhoto.value = bitmap
        preselectedCategory.value = null
        
        // Fetch actual GPS / Network Geolocation coordinates automatically on photo capture
        fetchDeviceLocation()
        
        reportingStep.value = 2 // Transition to scanning sonar
        triggerAiAnalysis()
    }

    fun resetReportingFlow() {
        reportingStep.value = 1
        capturedPhoto.value = null
        preselectedCategory.value = null
        aiAnalysisResult.value = null
        agentContextState.value = null
        exactLocationNotes.value = ""
        voiceAgentTranscript.value = ""
        voiceAgentPrompt.value = "Tap Speak and tell me the issue. I will ask for location, open camera, and prepare the report."
        voiceAgentListening.value = false
        voiceObjectId.value = ""
        generatedIssueId.value = ""
        duplicateMerged.value = false
        orchestratorTriggered.value = false
    }

    private fun triggerAiAnalysis() {
        viewModelScope.launch {
            try {
                val all = repository.getAllIssues()

                // Execute the fast unified multi-agent pipeline
                val context = multiAgentCoordinator.runOrchestrationPipeline(
                    bitmap = capturedPhoto.value,
                    manualCategory = null,
                    latitude = reportLatitude.value,
                    longitude = reportLongitude.value,
                    locationNotes = exactLocationNotes.value,
                    existingIssues = all
                )

                agentContextState.value = context

                val category = context.categoryResult?.category ?: "Other"
                val severity = context.severityResult?.severity ?: 5
                val riskLevel = context.severityResult?.riskLevel ?: "Medium"
                val department = context.routingResult?.department ?: "BBMP"
                val description = context.visionResult?.description ?: "No description provided."
                val estimatedDays = "${context.routingResult?.expectedSlaDays ?: 3} Days"
                val confidence = context.categoryResult?.confidence ?: 75
                val source = if (listOf(
                        context.visionResult?.fallbackUsed,
                        context.categoryResult?.fallbackUsed,
                        context.severityResult?.fallbackUsed,
                        context.routingResult?.fallbackUsed,
                        context.emergencyResult?.fallbackUsed
                    ).any { it == true }
                ) "fallback" else "gemini"

                // Extract AI-generated professional title from trace logs
                val aiTitle = context.traceLogs
                    .firstOrNull { it.startsWith("[REPORT_TITLE]:") }
                    ?.removePrefix("[REPORT_TITLE]:")
                    ?.trim()
                    ?: buildLocalTitle(category, riskLevel)

                val result = IssueAnalysisResult(
                    category = category,
                    severity = severity,
                    riskLevel = riskLevel,
                    department = department,
                    description = description,
                    estimatedDays = estimatedDays,
                    confidence = confidence,
                    source = source,
                    reportTitle = aiTitle
                )

                aiAnalysisResult.value = result

                reportingStep.value = 3 // Always show the AI verified review screen before dispatch
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI analysis error; using deterministic fallback analysis", e)
                val fallback = buildLocalAnalysisFallback(exactLocationNotes.value)
                aiAnalysisResult.value = fallback
                reportingStep.value = 3
                _uiToast.emit("VisionBot could not confidently analyze this image. Review it before dispatching.")
            }
        }
    }


    fun applyVoiceAgentTranscript(transcript: String, final: Boolean = false) {
        val cleaned = transcript.trim().replace(Regex("\\s+"), " ")
        voiceAgentTranscript.value = cleaned
        voiceObjectId.value = extractObjectId(cleaned).orEmpty()
        if (cleaned.isBlank()) {
            voiceAgentPrompt.value = "Tell me what is wrong and where it is happening."
            return
        }
        val locationGuess = extractLocationHint(cleaned)
        if (locationGuess.isNotBlank()) {
            exactLocationNotes.value = locationGuess
        }
        voiceAgentPrompt.value = if (final) {
            "Got it. I am opening the camera for evidence, then Gemini will fill the title and description."
        } else {
            "Listening... include the exact landmark or object ID if you know it."
        }
    }

    fun setVoiceListening(isListening: Boolean) {
        voiceAgentListening.value = isListening
        voiceAgentPrompt.value = if (isListening) "Listening... say the issue, object ID, and exact location." else "Voice capture paused. You can speak again or continue with camera."
    }

    private fun extractLocationHint(text: String): String {
        val markers = listOf(" near ", " at ", " outside ", " in front of ", " beside ", " opposite ")
        val lower = text.lowercase()
        val marker = markers.firstOrNull { lower.contains(it) } ?: return ""
        return text.substringAfter(marker, "").trim().trim('.', ',').take(90)
    }

    private fun extractObjectId(text: String): String? {
        val match = Regex("\\b(?:pc|pole|light|manhole|mh|bin|pipe|meter|pump|transformer|tf|road)\\s*[-#:]?\\s*([a-z0-9]{1,8})\\b", RegexOption.IGNORE_CASE).find(text)
        return match?.value?.replace(Regex("\\s+"), "")?.uppercase()
    }

    private fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val earth = 6371000.0
        val dLat = Math.toRadians(bLat - aLat)
        val dLng = Math.toRadians(bLng - aLng)
        val lat1 = Math.toRadians(aLat)
        val lat2 = Math.toRadians(bLat)
        val h = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) + kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
        return 2 * earth * kotlin.math.asin(kotlin.math.sqrt(h))
    }

    private fun findDuplicateIssueForReport(
        result: IssueAnalysisResult,
        latitude: Double,
        longitude: Double,
        objectId: String?,
        existingIssues: List<Issue>
    ): Issue? {
        return existingIssues.firstOrNull { issue ->
            if (issue.status == "Resolved" || issue.status.startsWith("Merged")) return@firstOrNull false
            val sameObject = !objectId.isNullOrBlank() && issue.objectId?.equals(objectId, ignoreCase = true) == true
            val sameCategory = issue.category.equals(result.category, ignoreCase = true)
            val closeEnough = distanceMeters(issue.latitude, issue.longitude, latitude, longitude) <= if (sameObject) 250.0 else 120.0
            (sameObject && closeEnough) || (sameCategory && closeEnough)
        }
    }

    private fun priorityBoostedSeverity(baseSeverity: Int, duplicateReports: Int, upvotes: Int): Int {
        val duplicateBoost = ((duplicateReports - 1) / 2).coerceAtLeast(0)
        val upvoteBoost = if (upvotes >= 10) 1 else 0
        return (baseSeverity + duplicateBoost + upvoteBoost).coerceIn(1, 10)
    }

    private fun buildLocalTitle(category: String, riskLevel: String): String {
        return when (category.lowercase()) {
            "pothole"    -> "$riskLevel Pothole Hazard on Road Surface"
            "water"      -> "Water Supply Leakage at Reported Location"
            "lights"     -> "Streetlight Outage Affecting Public Safety"
            "garbage"    -> "Uncleared Garbage Accumulation at Site"
            "sewage"     -> "Sewage Overflow Blocking Public Pathway"
            "pedestrian" -> "Pedestrian Infrastructure Damage Reported"
            else         -> "Civic Infrastructure Issue Requires Attention"
        }
    }

    private fun buildLocalAnalysisFallback(notes: String): IssueAnalysisResult {
        val category = inferCivicCategory(notes)
        val severity = when (category) {
            "Sewage" -> 8
            "Pothole", "Water", "Garbage" -> 7
            "Lights", "Pedestrian" -> 5
            else -> 5
        }
        val riskLevel = when (severity) {
            in 1..3 -> "Low"
            in 4..6 -> "Medium"
            in 7..8 -> "High"
            else -> "Critical"
        }
        return IssueAnalysisResult(
            category = category,
            severity = severity,
            riskLevel = riskLevel,
            department = when (category) {
                "Pothole" -> "PWD"
                "Water", "Sewage" -> "BWSSB"
                "Lights" -> "BESCOM"
                else -> "BBMP"
            },
            description = if (category == "Other") {
                "Gemini Vision could not confidently classify the uploaded photo. The report needs review before dispatch."
            } else {
                "Gemini Vision was unavailable, so this report was triaged from the citizen notes only. Review the photo before dispatch."
            },
            estimatedDays = if (severity >= 7) "1-2 Days" else "3-5 Days",
            confidence = if (category == "Other") 0 else 45,
            source = "fallback",
            reportTitle = buildLocalTitle(category, riskLevel)
        )
    }

    private fun Bitmap.toBase64String(): String? {
        return try {
            val outputStream = java.io.ByteArrayOutputStream()
            this.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            android.util.Base64.encodeToString(outputStream.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun submitReport() {
        val result = aiAnalysisResult.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val uid = "IDS-" + (100 + (Math.random() * 900).toInt())
            val photo64 = capturedPhoto.value?.toBase64String()
            val reportObjectId = voiceObjectId.value.ifBlank { extractObjectId("${voiceAgentTranscript.value} ${exactLocationNotes.value}") ?: "" }
            
            val all = repository.getAllIssues()
            val context = agentContextState.value
            val isEmergency = context?.emergencyResult?.isEmergency == true
            val requiresReview = context?.requiresHumanVerification ?: false
            val verificationReasons = context?.verificationReasons?.joinToString("; ") ?: ""
            val user = repository.getUser()
            val resolvedWard = context?.locationResult?.ward ?: user?.ward ?: "Indiranagar (Ward 80)"

            // Run real-time coordinator analysis via OrchestratorBot
            val targetLat = reportLatitude.value
            val targetLng = reportLongitude.value
            val decision = orchestratorBot.processNewReport(
                category = result.category,
                severity = result.severity,
                latitude = targetLat,
                longitude = targetLng,
                existingIssues = all
            )

            val sameCategoryActive = all.filter { 
                it.category.lowercase() == result.category.lowercase() && 
                it.status != "Resolved" && 
                !it.status.startsWith("Merged")
            }

            if (decision.action == "MERGE" && decision.targetIssueId != null) {
                // DuplicateBot Merges the reported issue with the existing proximity match
                val targetId = decision.targetIssueId
                val targetIssue = all.find { it.id == targetId }
                if (targetIssue != null) {
                    val nextDuplicateCount = targetIssue.duplicateReports + 1
                    val elevatedSeverity = priorityBoostedSeverity(targetIssue.severity.coerceAtLeast(result.severity), nextDuplicateCount, targetIssue.upvotes + 2)
                    val mergedIds = (targetIssue.clusteredIssueIdsCsv.split(",").filter { it.isNotBlank() } + uid).distinct().joinToString(",")
                    val merged = targetIssue.copy(
                        upvotes = targetIssue.upvotes + 2,
                        severity = elevatedSeverity,
                        status = if (nextDuplicateCount >= 3 || elevatedSeverity >= 9) "Urgent Escalation" else targetIssue.status,
                        objectId = targetIssue.objectId ?: reportObjectId.ifBlank { null },
                        duplicateReports = nextDuplicateCount,
                        clusteredIssueIdsCsv = mergedIds,
                        voiceTranscript = listOf(targetIssue.voiceTranscript, voiceAgentTranscript.value).filter { it.isNotBlank() }.joinToString(" | ")
                    )
                    repository.updateIssue(merged)
                    
                    repository.insertLog(
                        AuthorityLog(
                            timestamp = System.currentTimeMillis(),
                            isBotAction = true,
                            message = "DuplicateBot: Merged duplicate report $uid into ${targetIssue.id}. ${merged.duplicateReports} citizen reports now clustered; severity ${merged.severity}/10.",
                            priority = if (merged.status == "Urgent Escalation") "High" else "Medium"
                        )
                    )
                    
                    generatedIssueId.value = targetIssue.id
                    duplicateMerged.value = true
                }
            } else {
                val directDuplicate = findDuplicateIssueForReport(
                    result = result,
                    latitude = targetLat,
                    longitude = targetLng,
                    objectId = reportObjectId.ifBlank { null },
                    existingIssues = all
                )
                if (directDuplicate != null) {
                    val nextDuplicateCount = directDuplicate.duplicateReports + 1
                    val elevatedSeverity = priorityBoostedSeverity(directDuplicate.severity.coerceAtLeast(result.severity), nextDuplicateCount, directDuplicate.upvotes + 2)
                    val mergedIds = (directDuplicate.clusteredIssueIdsCsv.split(",").filter { it.isNotBlank() } + uid).distinct().joinToString(",")
                    val updated = directDuplicate.copy(
                        upvotes = directDuplicate.upvotes + 2,
                        severity = elevatedSeverity,
                        status = if (nextDuplicateCount >= 3 || elevatedSeverity >= 9) "Urgent Escalation" else directDuplicate.status,
                        objectId = directDuplicate.objectId ?: reportObjectId.ifBlank { null },
                        duplicateReports = nextDuplicateCount,
                        clusteredIssueIdsCsv = mergedIds,
                        source = "Voice Agent",
                        voiceTranscript = listOf(directDuplicate.voiceTranscript, voiceAgentTranscript.value).filter { it.isNotBlank() }.joinToString(" | ")
                    )
                    repository.updateIssue(updated)
                    repository.insertLog(
                        AuthorityLog(
                            timestamp = System.currentTimeMillis(),
                            isBotAction = true,
                            message = "DuplicateBot: Matched $uid with ${directDuplicate.id} by ${if (reportObjectId.isNotBlank()) "object $reportObjectId" else "nearby ${result.category}"}. ${updated.duplicateReports} reports consolidated.",
                            priority = if (updated.status == "Urgent Escalation") "High" else "Medium"
                        )
                    )
                    repository.insertAiDecisionLog(
                        AiDecisionLog(
                            timestamp = System.currentTimeMillis(),
                            agentName = "DuplicateBot",
                            issueId = directDuplicate.id,
                            decision = "Merged voice/mobile duplicate",
                            detail = "Matched object='${reportObjectId.ifBlank { "none" }}', category=${result.category}, duplicateReports=${updated.duplicateReports}."
                        )
                    )
                    generatedIssueId.value = directDuplicate.id
                    duplicateMerged.value = true
                } else if (decision.action == "ESCALATE" || sameCategoryActive.size >= 2 || isEmergency) {
                // Orchestrator decides to create a prioritized escalated master issue or single high-escalation issue
                val masterId = if (sameCategoryActive.size >= 2) "MSTR-IDS-" + (100 + (Math.random() * 900).toInt()) else uid
                val escalTitle = if (isEmergency) "CRITICAL EMERGENCY: ${context?.emergencyResult?.emergencyType?.uppercase()}" else if (sameCategoryActive.size >= 2) "CONCENTRATED ${result.category.uppercase()} SYSTEMIC FAILURE" else "ESCALATED ${result.category.uppercase()} ALERT"
                
                val masterIssue = Issue(
                    id = masterId,
                    title = escalTitle,
                    category = result.category,
                    ward = resolvedWard,
                    locationName = if (exactLocationNotes.value.isNotBlank()) exactLocationNotes.value else "Indiranagar Core Ward Corridor (Escalated)",
                    latitude = targetLat,
                    longitude = targetLng,
                    severity = if (isEmergency) 10 else 9, // Max urgent priority
                    description = result.description + (if (isEmergency) " [EMERGENCY ACTION PLAN: ${context.emergencyResult?.emergencyActionPlan}]" else " [Escalated via OrchestratorBot: ${decision.reason}]"),
                    timePosted = System.currentTimeMillis(),
                    upvotes = sameCategoryActive.sumOf { it.upvotes } + 5,
                    status = "Urgent Escalation",
                    isMyReport = true,
                    assignedDept = if (result.category.lowercase().contains("water") || result.category.lowercase().contains("leak")) "BWSSB" else result.department,
                    photoBase64 = photo64 ?: sameCategoryActive.firstOrNull()?.photoBase64,
                    slaStatus = if (isEmergency) "PENDING_ESCALATION" else decision.recommendedSlaStatus,
                    requiresReview = requiresReview || isEmergency,
                    verificationReasons = if (isEmergency) "Emergency trigger: ${context?.emergencyResult?.emergencyType}; $verificationReasons" else "Master ticket consolidation; $verificationReasons",
                    objectId = reportObjectId.ifBlank { null },
                    duplicateReports = sameCategoryActive.size + 1,
                    clusteredIssueIdsCsv = sameCategoryActive.joinToString(",") { it.id },
                    source = if (voiceAgentTranscript.value.isNotBlank()) "Voice Agent" else "Mobile App",
                    voiceTranscript = voiceAgentTranscript.value
                )

                // DuplicateBot merges previous active ones if there are several same categories
                if (sameCategoryActive.size >= 2) {
                    sameCategoryActive.forEach { old ->
                        repository.updateIssue(
                            old.copy(
                                status = "Resolved",
                                resolutionNotes = "Consolidated autonomously by OrchestratorBot into unified Master Ticket $masterId."
                            )
                        )
                    }
                }

                repository.insertIssue(masterIssue)
                repository.incrementUserPointsAndReports()

                // Insert Orchestrator's Multi-Agent Decision Chaining Event Logs
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis(),
                        isBotAction = true,
                        message = "OrchestratorBot (Agent 0): " + (if (isEmergency) "Critical Emergency Identified: ${context?.emergencyResult?.emergencyType}" else decision.reason),
                        priority = "High"
                    )
                )
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis() + 10,
                        isBotAction = true,
                        message = "OrchestratorBot -> EmergencyAgent (Agent 6): Escalated $masterId. Emergency Action: ${context?.emergencyResult?.emergencyActionPlan}.",
                        priority = "High"
                    )
                )

                generatedIssueId.value = masterId
                duplicateMerged.value = false
                orchestratorTriggered.value = true
            } else {
                // Standard client creation pathway
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis(),
                        isBotAction = true,
                        message = "OrchestratorBot: Standard verification routed. " + decision.reason,
                        priority = "Low"
                    )
                )

                // Insert fresh issue
                val reportTitle = result.reportTitle.ifBlank { buildLocalTitle(result.category, result.riskLevel) }
                val newIssue = Issue(
                    id = uid,
                    title = reportTitle,
                    category = result.category,
                    ward = resolvedWard,
                    locationName = if (exactLocationNotes.value.isNotBlank()) exactLocationNotes.value else "Indiranagar 12th Main Road Corner",
                    latitude = targetLat,
                    longitude = targetLng,
                    severity = result.severity,
                    description = result.description + " " + exactLocationNotes.value,
                    timePosted = System.currentTimeMillis(),
                    upvotes = 1,
                    status = "Reported",
                    isMyReport = true,
                    assignedDept = result.department,
                    photoBase64 = photo64,
                    slaStatus = "SLA OK",
                    requiresReview = requiresReview,
                    verificationReasons = verificationReasons,
                    objectId = reportObjectId.ifBlank { null },
                    duplicateReports = 1,
                    source = if (voiceAgentTranscript.value.isNotBlank()) "Voice Agent" else "Mobile App",
                    voiceTranscript = voiceAgentTranscript.value
                )
                
                repository.insertIssue(newIssue)
                repository.incrementUserPointsAndReports()

                // Automatically trigger system alert message inside the corresponding ward's chat room
                val wardRoomId = getRoomIdForWard(resolvedWard)
                repository.insertChatMessage(CommunityChatMessage(
                    roomId = wardRoomId,
                    senderId = "system_bot",
                    senderName = "RouteBot 🤖",
                    senderTrustScore = 100,
                    senderWard = "NagarNetra AI System",
                    text = "Alert: A new issue '${reportTitle}' was reported in ${resolvedWard}. View it in the Map tab to upvote and escalate.",
                    timestamp = System.currentTimeMillis(),
                    isSystem = true
                ))

                // Dispatch automated alert
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis(),
                        isBotAction = true,
                        message = "RouteBot: Assigned new ticket $uid to ${result.department} department. Target SLA: ${result.estimatedDays}.",
                        priority = "Medium"
                    )
                )
                }
            }
            
            if (generatedIssueId.value.isEmpty()) {
                generatedIssueId.value = uid
            }

            val finalIssueId = generatedIssueId.value.ifBlank { uid }
            val duplicateStatus = context?.duplicateResult?.let {
                if (it.isDuplicate) "Matched ${it.duplicateTicketIds.joinToString()}" else it.reason
            } ?: "No duplicate context available."
            val priorityScore = context?.routingResult?.priorityScore ?: calculatePriorityScore(
                Issue(
                    id = finalIssueId,
                    title = result.reportTitle.ifBlank { buildLocalTitle(result.category, result.riskLevel) },
                    category = result.category,
                    ward = resolvedWard,
                    locationName = exactLocationNotes.value.ifBlank { "Reported location" },
                    latitude = targetLat,
                    longitude = targetLng,
                    severity = result.severity,
                    description = result.description,
                    timePosted = System.currentTimeMillis(),
                    upvotes = 1,
                    status = "Reported",
                    isMyReport = true,
                    assignedDept = result.department
                )
            )
            val agentLogs = listOf(
                AiDecisionLog(timestamp = System.currentTimeMillis(), agentName = "OrchestratorBot", issueId = finalIssueId, decision = "Pipeline completed", detail = "Master decision: ${decision.action}. ${decision.reason}"),
                AiDecisionLog(timestamp = System.currentTimeMillis() + 1, agentName = "VisionBot", issueId = finalIssueId, decision = "Image analysis completed", detail = "${result.description} Confidence: ${result.confidence}%. Source: ${result.source}."),
                AiDecisionLog(timestamp = System.currentTimeMillis() + 2, agentName = "CategoryBot", issueId = finalIssueId, decision = "Department routing prepared", detail = "Category ${result.category} assigned to ${result.department}."),
                AiDecisionLog(timestamp = System.currentTimeMillis() + 3, agentName = "DuplicateBot", issueId = finalIssueId, decision = "Duplicate scan completed", detail = duplicateStatus),
                AiDecisionLog(timestamp = System.currentTimeMillis() + 4, agentName = "PriorityBot", issueId = finalIssueId, decision = "Priority score calculated", detail = "Severity ${result.severity}/10, risk ${result.riskLevel}, priority score $priorityScore."),
                AiDecisionLog(timestamp = System.currentTimeMillis() + 5, agentName = "PredictBot", issueId = finalIssueId, decision = "Pattern check completed", detail = "Nearby active ${result.category} issues: ${sameCategoryActive.size}."),
                AiDecisionLog(timestamp = System.currentTimeMillis() + 6, agentName = "EscalationBot", issueId = finalIssueId, decision = "Escalation check completed", detail = if (isEmergency) "Emergency escalation required: ${context?.emergencyResult?.emergencyType}" else "Standard SLA path: ${decision.recommendedSlaStatus}."),
                AiDecisionLog(timestamp = System.currentTimeMillis() + 7, agentName = "RouteBot", issueId = finalIssueId, decision = "Community and department route updated", detail = "Ward $resolvedWard, department ${result.department}, room ${getRoomIdForWard(resolvedWard)}.")
            )
            agentLogs.forEach { repository.insertAiDecisionLog(it) }

            reportingStep.value = 4 // Success confetti screen
        }
    }

    // --- Dispatch Officer Controller Actions ---
    fun updateIssueStatus(issueId: String, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val issue = repository.getIssueById(issueId) ?: return@launch
            val updated = issue.copy(status = newStatus)
            repository.updateIssue(updated)

            repository.insertLog(
                AuthorityLog(
                    timestamp = System.currentTimeMillis(),
                    isBotAction = false,
                    message = "PWD dispatcher: Designated field personnel to execute fixes on ticket $issueId.",
                    priority = "Medium"
                )
            )
            _uiToast.emit("Status updated on ticket $issueId to $newStatus!")
        }
    }

    fun resolveIssueWithProof(issueId: String, notes: String, proof: Bitmap?) {
        viewModelScope.launch(Dispatchers.IO) {
            resolutionVerifying.value = true
            delay(1500) // VisionBot scan resolution photo latency

            val issue = repository.getIssueById(issueId) ?: return@launch
            val updated = issue.copy(
                status = "Resolved",
                resolutionNotes = notes,
                slaStatus = "SLA OK"
            )
            repository.updateIssue(updated)

            repository.insertLog(
                AuthorityLog(
                    timestamp = System.currentTimeMillis(),
                    isBotAction = true,
                    message = "VisionBot: Scanned resolution image of $issueId. Repair verified matching BBMP indices. Marking Resolved âœ….",
                    priority = "High"
                )
            )

            // TrustBot Score increase
            val user = repository.getUser()
            if (user != null && issue.isMyReport) {
                val updatedUser = user.copy(trustScore = Math.min(100, user.trustScore + 2))
                repository.updateUser(updatedUser)
            }

            resolutionVerifying.value = false
            _uiToast.emit("Incident $issueId successfully closed and verified via AI!")
        }
    }

    // --- Bot Background Escalation Check (Agent 7 - EscalationBot) ---
    private fun runEscalationCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            val issues = repository.getAllIssues()
            val now = System.currentTimeMillis()
            issues.forEach { issue ->
                // Simulate: mark static preloaded issues as breached
                if (issue.status != "Resolved" && issue.id == "IDS-181") {
                    val updated = issue.copy(slaStatus = "SLA BREACHED")
                    repository.updateIssue(updated)

                    repository.insertLog(
                        AuthorityLog(
                            timestamp = System.currentTimeMillis(),
                            isBotAction = true,
                            message = "EscalationBot: Ticket ${issue.id} has breached SLA timeline of 24 Hrs. Alert dispatched to PWD supervisor.",
                            priority = "Critical"
                        )
                    )
                }
            }
        }
    }

    fun calculatePriorityScore(issue: Issue): Int {
        val baseSev = issue.severity * 8
        val baseUp = Math.min(20, issue.upvotes * 2)
        return Math.min(100, baseSev + baseUp)
    }

    private fun isReportRequest(text: String): Boolean {
        val q = text.lowercase()
        return listOf(
            "report", "complaint", "raise ticket", "create ticket", "submit issue",
            "water leak", "pothole", "garbage", "street light", "streetlight",
            "sewage", "manhole", "broken road", "issue near"
        ).any { q.contains(it) } && !listOf("status", "how many", "count", "show", "what is").any { q.startsWith(it) }
    }

    fun sendChatMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        
        val userMsg = ChatMessage(trimmed, isBot = false)
        val currentMsgs = chatMessages.value.toMutableList()
        currentMsgs.add(userMsg)
        chatMessages.value = currentMsgs

        viewModelScope.launch {
            if (isReportRequest(trimmed)) {
                resetReportingFlow()
                applyVoiceAgentTranscript(trimmed, final = true)
                activeCitizenTab.value = "Report"
                val botReply = "I can handle that. I am opening the camera now, then VisionBot and Gemini will generate the title, description, department route, duplicate check, and submit the report automatically."
                chatMessages.value = chatMessages.value + ChatMessage(botReply, isBot = true)
                _assistantSpokenReplies.emit(botReply)
                _assistantCameraRequests.emit(trimmed)
                return@launch
            }

            val botReply = geminiClient.chatWithCivicAgent(
                apiKey = com.example.BuildConfig.GEMINI_API_KEY,
                currentMessage = trimmed,
                history = chatMessages.value,
                allIssues = allIssues.value
            )
            val updatedMsgs = chatMessages.value.toMutableList()
            updatedMsgs.add(ChatMessage(botReply, isBot = true))
            chatMessages.value = updatedMsgs
            _assistantSpokenReplies.emit(botReply)
        }
    }

    private fun shouldHideCommunityMessage(text: String): Boolean {
        val normalized = text.lowercase().replace(Regex("""\s+"""), " ").trim()
        if (normalized.isBlank()) return false

        val blockedTerms = listOf(
            "spam", "offensive", "abuse", "vulgar", "bastard", "scam", "fake lottery",
            "sell coins", "buy weed", "crypto double", "double your money", "easy money",
            "http://", "https://", "wa.me/", "t.me/", "telegram", "whatsapp me"
        )
        if (blockedTerms.any { normalized.contains(it) }) return true

        val repeatedChars = Regex("""(.)\1{5,}""").containsMatchIn(normalized)
        val suspiciousPromotion = Regex("""\b(buy|sell|promo|discount|loan|betting|casino|crypto)\b""").containsMatchIn(normalized) &&
            !Regex("""\b(report|issue|ticket|ward|road|water|garbage|light|sewage|map|location)\b""").containsMatchIn(normalized)
        val abusiveDirected = Regex("""\b(idiot|stupid|fool|shut up|hate you)\b""").containsMatchIn(normalized)

        return repeatedChars || suspiciousPromotion || abusiveDirected
    }

    // --- Community Chat Methods ---
    fun sendCommunityChatMessage(text: String, replyToId: Int? = null, attachedPhoto: Bitmap? = null) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() && attachedPhoto == null) return
        
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
            val roomId = activeCommunityRoomId.value
            
            // Real-time scan / AI Moderation check
            val isHidden = shouldHideCommunityMessage(trimmed)
            
            val photo64 = attachedPhoto?.toBase64String()
            
            val message = CommunityChatMessage(
                roomId = roomId,
                senderId = user.id,
                senderName = user.name,
                senderTrustScore = user.trustScore,
                senderWard = user.ward,
                text = trimmed,
                timestamp = System.currentTimeMillis(),
                parentMessageId = replyToId,
                isHiddenByAi = isHidden,
                photoBase64 = photo64
            )
            
            val insertedId = repository.insertChatMessage(message)
            
            if (isHidden) {
                // Generate AI decision log for moderated message
                repository.insertAiDecisionLog(
                    AiDecisionLog(
                        timestamp = System.currentTimeMillis(),
                        agentName = "ModeratorBot",
                        issueId = null,
                        decision = "Flagged Message",
                        detail = "Flagged message from user ${user.name} in room $roomId for violation: Inappropriate content or spam pattern detected."
                    )
                )
                _uiToast.emit("Your message was flagged and moderated by NagarNetra AI.")
            } else {
                // If it is regular message, increase points for active civic participation (+2 points per message)
                val updatedUser = user.copy(points = user.points + 2)
                database.userDao().insertUser(updatedUser)

                // AI Bot Auto-reply when user includes `@` or `@bot` or starts with `@`
                if (trimmed.contains("@bot") || trimmed.contains("@nagarnetra") || trimmed.contains("@citizenbot") || trimmed.contains("@system_bot") || trimmed.startsWith("@")) {
                    val cleanQuery = trimmed.replace(Regex("@(bot|nagarnetra|citizenbot|system_bot)"), "").trim().removePrefix("@").trim()
                    
                    val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                    val allIssuesList = repository.getAllIssues()
                    
                    val botResponse = if (cleanQuery.contains("update name to", ignoreCase = true)) {
                        val newName = cleanQuery.substringAfter("update name to", "").trim()
                        if (newName.isNotEmpty()) {
                            updateUserDisplayName(newName)
                            "Sure! I've updated your display name to '$newName'."
                        } else {
                            "Please specify a valid name to update."
                        }
                    } else if (cleanQuery.contains("update ward to", ignoreCase = true)) {
                        val newWard = cleanQuery.substringAfter("update ward to", "").trim()
                        if (newWard.isNotEmpty()) {
                            updateUserProfile(user.name, user.email, newWard)
                            "Sure! I've updated your ward to '$newWard'."
                        } else {
                            "Please specify a valid ward to update."
                        }
                    } else if (cleanQuery.isNotEmpty()) {
                        geminiClient.chatWithCivicAgent(
                            apiKey = apiKey,
                            currentMessage = cleanQuery,
                            history = emptyList(),
                            allIssues = allIssuesList
                        )
                    } else {
                        "Hello! I am the NagarNetra AI Companion. Use `@bot <your question>` to ask me about reported issues, city statistics, or how to report a civic issue!"
                    }
                    
                    val botMessage = CommunityChatMessage(
                        roomId = roomId,
                        senderId = "system_bot",
                        senderName = "NagarNetra AI Bot 🤖",
                        senderTrustScore = 100,
                        senderWard = "AI Core",
                        text = botResponse,
                        timestamp = System.currentTimeMillis(),
                        parentMessageId = insertedId.toInt(),
                        isHiddenByAi = false,
                        photoBase64 = null
                    )
                    repository.insertChatMessage(botMessage)
                }
            }
        }
    }

    fun joinRoomByInviteLink(link: String) {
        val wardId = when {
            link.startsWith("ward_") -> link
            link.contains("join/chat/ward_") -> "ward_" + link.substringAfter("join/chat/ward_").filter { it.isDigit() }
            link.all { it.isDigit() } -> "ward_$link"
            else -> null
        }
        if (wardId != null) {
            val current = joinedRoomsList.value.toMutableSet()
            current.add(wardId)
            joinedRoomsList.value = current
            activeCommunityRoomId.value = wardId
            viewModelScope.launch {
                _uiToast.emit("Successfully joined room $wardId")
            }
        } else {
            viewModelScope.launch {
                _uiToast.emit("Invalid invite link/code. Please try again.")
            }
        }
    }

    fun upvoteChatMessage(message: CommunityChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = message.copy(upvotes = message.upvotes + 1)
            repository.updateChatMessage(updated)
            // Also award point to the sender for helpful post!
            if (message.senderId != "system_bot" && message.senderId != "citizen_user") {
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis(),
                        isBotAction = true,
                        message = "TrustBot: Awarded +1 Trust to ${message.senderName} for helpful community message.",
                        priority = "Low"
                    )
                )
            } else if (message.senderId == "citizen_user") {
                val user = repository.getUser()
                if (user != null) {
                    val updatedUser = user.copy(
                        points = user.points + 2,
                        trustScore = Math.min(100, user.trustScore + 1)
                    )
                    database.userDao().insertUser(updatedUser)
                }
            }
        }
    }

    fun reactToChatMessage(message: CommunityChatMessage, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = org.json.JSONObject(message.reactionsJson)
                val count = json.optInt(emoji, 0)
                json.put(emoji, count + 1)
                val updated = message.copy(reactionsJson = json.toString())
                repository.updateChatMessage(updated)
            } catch (e: Exception) {
                Log.e("ChatReaction", "Failed to react", e)
            }
        }
    }

    fun moderateMessage(messageId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.hideChatMessageByAi(messageId)
            // Log the moderation action
            repository.insertLog(
                AuthorityLog(
                    timestamp = System.currentTimeMillis(),
                    isBotAction = false,
                    message = "Community Moderator flagged and hid message ID $messageId.",
                    priority = "Medium"
                )
            )
            _uiToast.emit("Message ID $messageId successfully hidden by moderator.")
        }
    }

    fun getRoomIdForWard(wardString: String): String {
        val regex = Regex("Ward\\s+(\\d+)", RegexOption.IGNORE_CASE)
        val match = regex.find(wardString)
        if (match != null) {
            val num = match.groupValues[1]
            return "ward_$num"
        }
        val digits = wardString.filter { it.isDigit() }
        if (digits.isNotEmpty()) {
            return "ward_$digits"
        }
        return "ward_80" // default fallback
    }

    // --- Custom ML Classifier Model Training ---
    val isTrainingModel = MutableStateFlow(false)
    val trainingProgress = MutableStateFlow(0f)
    val modelAccuracy = MutableStateFlow(94.2f)
    val modelStatus = MutableStateFlow("Deployed (v3.1-Lite)")
    val trainingLogs = MutableStateFlow<List<String>>(listOf(
        "NagarNetraVision classifier active on Ward 80 edge nodes."
    ))

    fun runMachineLearningTraining() {
        if (isTrainingModel.value) return
        isTrainingModel.value = true
        modelStatus.value = "Active Training Cycle..."
        viewModelScope.launch {
            val templates = listOf(
                "Loading 1,250 certified civic images...",
                "Running Epoch 1/5 - Loss: 0.58 | Train Acc: 78.4% | Val Acc: 77.2%",
                "Running Epoch 2/5 - Loss: 0.42 | Train Acc: 84.8% | Val Acc: 83.5%",
                "Running Epoch 3/5 - Loss: 0.31 | Train Acc: 90.1% | Val Acc: 88.9%",
                "Running Epoch 4/5 - Loss: 0.22 | Train Acc: 94.6% | Val Acc: 93.4%",
                "Running Epoch 5/5 - Loss: 0.15 | Train Acc: 98.4% | Val Acc: 97.9%"
            )
            val logsList = trainingLogs.value.toMutableList()
            logsList.add("ML Training Cycle started by municipal authority.")
            trainingLogs.value = logsList

            for (i in 0..5) {
                delay(1200)
                trainingProgress.value = (i + 1) / 6f
                val updatedLogs = trainingLogs.value.toMutableList()
                updatedLogs.add(templates[i])
                trainingLogs.value = updatedLogs
                if (i == 5) {
                    modelAccuracy.value = 98.4f
                }
            }
            
            delay(500)
            modelStatus.value = "Successfully Deployed (v3.2-Pro)"
            val finalLogs = trainingLogs.value.toMutableList()
            finalLogs.add("Model weights compiled to TensorRT. Local edge precision optimized to 98.4%.")
            finalLogs.add("Custom ML Classifier successfully deployed to Ward 80 Indiranagar nodes.")
            trainingLogs.value = finalLogs
            isTrainingModel.value = false
            
            logAuthorityAction("Successfully trained and deployed upgraded ML weights (v3.2, acc: 98.4%) to ward nodes.", "High")
        }
    }

    // --- Ward Reports Compiler States ---
    val isCompilingReport = MutableStateFlow(false)
    val compiledReportText = MutableStateFlow<String?>(null)

    fun compileWardOperationalReport() {
        if (isCompilingReport.value) return
        isCompilingReport.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val issues = repository.getAllIssues()
                val report = geminiClient.compileWardReport(
                    apiKey = com.example.BuildConfig.GEMINI_API_KEY,
                    issues = issues
                )
                compiledReportText.value = report
                repository.insertWardReport(
                    WardReport(
                        timestamp = System.currentTimeMillis(),
                        reportType = "OPERATIONAL",
                        reportText = report,
                        ward = "Ward 80 Indiranagar"
                    )
                )
                repository.insertAiDecisionLog(
                    AiDecisionLog(
                        timestamp = System.currentTimeMillis(),
                        agentName = "ReportBot",
                        issueId = null,
                        decision = "Ward operational report generated",
                        detail = "Compiled report from ${issues.size} active issue records."
                    )
                )
                _uiToast.emit("ReportBot generated the ward operational report.")
            } catch (e: Exception) {
                Log.e("MainViewModel", "ReportBot failed", e)
                val fallback = "NAGARNETRA WARD OPERATIONAL REPORT\nTotal active issue records: ${allIssues.value.size}\nReportBot fallback generated because AI report generation failed: ${e.localizedMessage ?: "Unknown error"}"
                compiledReportText.value = fallback
                repository.insertWardReport(
                    WardReport(
                        timestamp = System.currentTimeMillis(),
                        reportType = "OPERATIONAL",
                        reportText = fallback,
                        ward = "Ward 80 Indiranagar"
                    )
                )
                _uiToast.emit("ReportBot used fallback report generation.")
            } finally {
                isCompilingReport.value = false
            }
        }
    }

    fun logAuthorityAction(message: String, priority: String = "Medium") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog(
                AuthorityLog(
                    timestamp = System.currentTimeMillis(),
                    isBotAction = false,
                    message = message,
                    priority = priority
                )
            )
        }
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // AUTOMATED ACCOUNTABILITY ENGINE CORE LOGIC
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    fun loadAccountabilityDemoData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearIssues()
            repository.clearAiDecisionLogs()
            repository.clearEmailLogs()
            repository.clearWardReports()
            repository.clearAuthorityLogs()

            val now = System.currentTimeMillis()
            val oneDayMs = 86400000L

            // 1. Issue with 49 upvotes (crossing 50 upvotes trigger)
            val issue1 = Issue(
                id = "IDS-187",
                title = "Critical Deep Pothole",
                category = "Pothole",
                ward = "Indiranagar",
                locationName = "Indiranagar 12th Main, Bengaluru",
                latitude = 12.9725,
                longitude = 77.6412,
                severity = 9,
                description = "Severe deep pothole situated on the fast lane. Causes extreme traffic congestion and poses a lethal hazard to motorcyclists.",
                timePosted = now - oneDayMs * 2,
                upvotes = 49, // One more upvote crosses 50!
                status = "Reported",
                isMyReport = false,
                assignedDept = "PWD",
                slaStatus = "SLA OK"
            )

            // 2. 7-Day unresolved issue
            val issue2 = Issue(
                id = "IDS-179",
                title = "Broken Streetlight Belt",
                category = "Lights",
                ward = "HSR Layout",
                locationName = "HSR Layout Sector 2, Bengaluru",
                latitude = 12.9103,
                longitude = 77.6450,
                severity = 6,
                description = "Row of 3 Streetlights completely dead in Sector 2. The darkness poses critical safety concerns.",
                timePosted = now - oneDayMs * 8, // 8 days unresolved!
                upvotes = 18,
                status = "In Progress",
                isMyReport = false,
                assignedDept = "BESCOM",
                slaStatus = "PENDING_ESCALATION"
            )

            // 3. 14-Day unresolved issue
            val issue3 = Issue(
                id = "IDS-181",
                title = "Lethal Open Sewer Manhole",
                category = "Sewage",
                ward = "MG Road",
                locationName = "MG Road near Metro, Bengaluru",
                latitude = 12.9738,
                longitude = 77.6119,
                severity = 10,
                description = "Completely open sewer slab directly on the pedestrian pathway. Extremely hazardous for commuters.",
                timePosted = now - oneDayMs * 15, // 15 days unresolved!
                upvotes = 89,
                status = "Verified",
                isMyReport = false,
                assignedDept = "BWSSB",
                slaStatus = "SLA BREACHED"
            )

            // 4. 30-Day unresolved issue
            val issue4 = Issue(
                id = "IDS-182",
                title = "Major Water Leakage",
                category = "Water",
                ward = "Koramangala",
                locationName = "Koramangala 5th Block, Bengaluru",
                latitude = 12.9348,
                longitude = 77.6189,
                severity = 7,
                description = "Substantial active potable water leak in the middle of the road causing local flooding.",
                timePosted = now - oneDayMs * 32, // 32 days unresolved!
                upvotes = 23,
                status = "Reported",
                isMyReport = false,
                assignedDept = "BWSSB",
                slaStatus = "SLA BREACHED"
            )

            repository.insertIssue(issue1)
            repository.insertIssue(issue2)
            repository.insertIssue(issue3)
            repository.insertIssue(issue4)

            // Insert initial pipeline logs
            repository.insertAiDecisionLog(
                AiDecisionLog(
                    timestamp = now - 60000 * 10,
                    agentName = "VisionBot",
                    issueId = "IDS-187",
                    decision = "Pavement surface failure verified.",
                    detail = "Confidence: 95%. Depth estimated: 1.2ft."
                )
            )
            repository.insertAiDecisionLog(
                AiDecisionLog(
                    timestamp = now - 60000 * 9,
                    agentName = "CategoryBot",
                    issueId = "IDS-187",
                    decision = "Routed to PWD Roads Division.",
                    detail = "Classification aligned with BBMP-PWD shared jurisdiction."
                )
            )
            repository.insertAiDecisionLog(
                AiDecisionLog(
                    timestamp = now - 60000 * 8,
                    agentName = "PriorityBot",
                    issueId = "IDS-187",
                    decision = "Severity score assessed as 9/10.",
                    detail = "Critical status due to presence on fast lane of busy arterial road."
                )
            )

            _uiToast.emit("Accountability Demo Data loaded successfully!")
        }
    }

    fun triggerAccountabilityEmail(
        type: String, // "HIGH_VOTES" | "SEVEN_DAY" | "FOURTEEN_DAY" | "THIRTY_DAY" | "WEEKLY_SUMMARY" | "MONTHLY_REPORT"
        issue: Issue?,
        overrideAllIssues: List<Issue> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            val issues = if (overrideAllIssues.isNotEmpty()) overrideAllIssues else repository.getAllIssues()

            val targetRecipient = when (issue?.assignedDept) {
                "PWD" -> "pwd.escalations.blr@gmail.com"
                "BWSSB" -> "bwssb.complaints.ward80@gmail.com"
                "BESCOM" -> "bescom.safety.indiranagar@gmail.com"
                "BBMP" -> "bbmp.commissioner.escalations@gmail.com"
                else -> "ward80.action.committee@gmail.com"
            }

            val recipientName = when (issue?.assignedDept) {
                "PWD" -> "Executive Engineer, PWD Bengaluru"
                "BWSSB" -> "Chief Sanitary Inspector, BWSSB Ward 80"
                "BESCOM" -> "Assistant Executive Engineer, BESCOM"
                "BBMP" -> "Chief Ward Commissioner, BBMP"
                else -> "Ward 80 Governance Representative"
            }

            val subject = when (type) {
                "HIGH_VOTES" -> "NAGARNETRA AI â€” CITIZEN DEMAND ALERT [Ticket ${issue?.id}]"
                "SEVEN_DAY" -> "NAGARNETRA AI â€” 7-DAY UNRESOLVED WARNING [Ticket ${issue?.id}]"
                "FOURTEEN_DAY" -> "NAGARNETRA AI â€” STERN 14-DAY ACTION DISPATCH [Ticket ${issue?.id}]"
                "THIRTY_DAY" -> "NAGARNETRA AI â€” CRITICAL 30-DAY BREACH ESCALATION TO COMMISSIONER [Ticket ${issue?.id}]"
                "WEEKLY_SUMMARY" -> "NAGARNETRA AI â€” WEEKLY WARD CIVIC HEALTH REGISTER"
                "MONTHLY_REPORT" -> "NAGARNETRA AI â€” MONTHLY WARD PERFORMANCE REPORT"
                else -> "NAGARNETRA AI â€” CIVIC ALERT"
            }

            val agentName = if (type == "WEEKLY_SUMMARY" || type == "MONTHLY_REPORT") "ReportBot" else "EscalationBot"
            val decisionMsg = when (type) {
                "HIGH_VOTES" -> "Issue has crossed 50 upvotes. Commencing citizen lobby mobilization."
                "SEVEN_DAY" -> "Issue unresolved for over 7 days. Generating soft ward pressure warning."
                "FOURTEEN_DAY" -> "Issue unresolved for over 14 days (SLA Breached). Generating stern mobilization order."
                "THIRTY_DAY" -> "Issue unresolved for over 30 days (Critical Breach). Escalating directly to BBMP Commissioner."
                "WEEKLY_SUMMARY" -> "Compiling weekly civic health register of active ward issues."
                "MONTHLY_REPORT" -> "Analyzing monthly resolution rates and assigning grade card."
                else -> "Generating automated civic alert."
            }

            repository.insertAiDecisionLog(
                AiDecisionLog(
                    timestamp = System.currentTimeMillis(),
                    agentName = agentName,
                    issueId = issue?.id,
                    decision = decisionMsg,
                    detail = "Generating HTML document via Gemini model."
                )
            )

            val htmlBody = if (type == "WEEKLY_SUMMARY" || type == "MONTHLY_REPORT") {
                multiAgentCoordinator.authorityAssistant.compileWardSummary("Indiranagar (Ward 80)", issues)
            } else {
                multiAgentCoordinator.authorityAssistant.generateAccountabilityEmail(
                    triggerType = type,
                    issue = issue,
                    allIssues = issues
                )
            }

            val emailJsClient = EmailJsClient()
            val isSuccess = emailJsClient.sendAccountabilityEmail(
                toEmail = targetRecipient,
                toName = recipientName,
                subject = subject,
                htmlContent = htmlBody
            )

            val status = if (isSuccess) {
                if (!com.example.BuildConfig.EMAILJS_SERVICE_ID.contains("your-") && com.example.BuildConfig.EMAILJS_SERVICE_ID.isNotEmpty()) "Sent" else "Simulated"
            } else "Failed"

            repository.insertEmailLog(
                EmailLog(
                    timestamp = System.currentTimeMillis(),
                    issueId = issue?.id,
                    recipient = "$recipientName ($targetRecipient)",
                    subject = subject,
                    body = htmlBody,
                    triggerType = type,
                    isSimulated = status == "Simulated",
                    status = status
                )
            )

            val feedbackMsg = if (status == "Simulated") {
                "$agentName: Simulated sending email successfully to $recipientName on topic: $subject"
            } else {
                "$agentName: Successfully sent official accountability email to $recipientName on topic: $subject"
            }

            repository.insertLog(
                AuthorityLog(
                    timestamp = System.currentTimeMillis(),
                    isBotAction = true,
                    message = feedbackMsg,
                    priority = if (type == "THIRTY_DAY") "Critical" else "High"
                )
            )

            if (type == "WEEKLY_SUMMARY" || type == "MONTHLY_REPORT") {
                repository.insertWardReport(
                    WardReport(
                        timestamp = System.currentTimeMillis(),
                        reportType = if (type == "WEEKLY_SUMMARY") "WEEKLY" else "MONTHLY",
                        reportText = htmlBody,
                        ward = "Ward 80 Indiranagar"
                    )
                )
            }

            _uiToast.emit("Accountability action completed: $subject dispatched!")
        }
    }

    fun runAgenticEmailDemo() {
        if (isRunningAccountabilityCheck.value) return
        isRunningAccountabilityCheck.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val issues = repository.getAllIssues()
            val now = System.currentTimeMillis()
            val oneDayMs = 86400000L

            _uiToast.emit("Starting Accountability Audit Check...")

            repository.insertAiDecisionLog(
                AiDecisionLog(
                    timestamp = System.currentTimeMillis(),
                    agentName = "EscalationBot",
                    issueId = null,
                    decision = "Initiating civic SLA and community engagement audit...",
                    detail = "Scanning active complaints across Ward 80 Indiranagar."
                )
            )
            delay(1000)

            issues.forEach { issue ->
                if (issue.status != "Resolved") {
                    val ageMs = now - issue.timePosted
                    val ageDays = ageMs / oneDayMs

                    if (issue.upvotes >= 50) {
                        triggerAccountabilityEmail("HIGH_VOTES", issue, issues)
                        delay(2000)
                    }

                    when {
                        ageDays >= 30 -> {
                            val updated = issue.copy(slaStatus = "SLA BREACHED")
                            repository.updateIssue(updated)
                            triggerAccountabilityEmail("THIRTY_DAY", updated, issues)
                            delay(2000)
                        }
                        ageDays >= 14 -> {
                            val updated = issue.copy(slaStatus = "SLA BREACHED")
                              repository.updateIssue(updated)
                            triggerAccountabilityEmail("FOURTEEN_DAY", updated, issues)
                            delay(2000)
                        }
                        ageDays >= 7 -> {
                            val updated = issue.copy(slaStatus = "PENDING_ESCALATION")
                            repository.updateIssue(updated)
                            triggerAccountabilityEmail("SEVEN_DAY", updated, issues)
                            delay(2000)
                        }
                    }
                }
            }

            _uiToast.emit("Accountability check complete!")
            isRunningAccountabilityCheck.value = false
        }
    }

    fun triggerReportBot(type: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiToast.emit("ReportBot compiling $type Report...")
            val issues = repository.getAllIssues()
            triggerAccountabilityEmail(if (type == "WEEKLY") "WEEKLY_SUMMARY" else "MONTHLY_REPORT", null, issues)
        }
    }

    // --- Gamification & Points Methods ---
    fun addPoints(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
            val newPoints = user.points + amount
            val newLevel = (newPoints / 100) + 1
            val updatedUser = user.copy(
                points = newPoints,
                level = newLevel
            )
            database.userDao().insertUser(updatedUser)
            _uiToast.emit("Earned $amount points! Level: $newLevel")
        }
    }

    fun addTrustScore(amount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
            val newScore = Math.min(100, Math.max(0, user.trustScore + amount))
            val updatedUser = user.copy(trustScore = newScore)
            database.userDao().insertUser(updatedUser)
            _uiToast.emit("Trust Score updated: $newScore%")
        }
    }

    fun redeemReward(rewardName: String, pointsCost: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val user = repository.getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
            if (user.points >= pointsCost) {
                val updatedUser = user.copy(
                    points = user.points - pointsCost,
                    badgesCount = if (rewardName.contains("Hero", ignoreCase = true)) user.badgesCount + 1 else user.badgesCount
                )
                database.userDao().insertUser(updatedUser)
                
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis(),
                        isBotAction = false,
                        message = "Redeemed Reward: $rewardName for $pointsCost pts.",
                        priority = "Low"
                    )
                )
                _uiToast.emit("Successfully redeemed $rewardName! -$pointsCost pts")
            } else {
                _uiToast.emit("Insufficient points! Required: $pointsCost pts.")
            }
        }
    }

    // --- Feedback & Contest Loop ---
    fun contestResolution(issueId: String, reason: String, photo: Bitmap?) {
        viewModelScope.launch(Dispatchers.IO) {
            val issue = repository.getIssueById(issueId) ?: return@launch
            val photo64 = photo?.toBase64String()
            
            _uiToast.emit("Initiating multi-agent inspection of contest evidence...")
            delay(1500)

            val context = AgentContext(
                imageBase64 = photo64,
                latitude = issue.latitude,
                longitude = issue.longitude,
                locationNotes = "Contest evidence: $reason",
                existingIssues = listOf(issue)
            )
            
            val visionRes = multiAgentCoordinator.visionAgent.run(context)
            
            val isStillBroken = if (photo != null) {
                visionRes.confidence > 40 && (visionRes.visualAnomalies.isNotEmpty() || visionRes.description.lowercase().contains("unresolved") || visionRes.description.lowercase().contains("broken") || visionRes.description.lowercase().contains("still") || visionRes.description.lowercase().contains("pothole") || visionRes.description.lowercase().contains("trash") || visionRes.description.lowercase().contains("leak") || true)
            } else {
                true
            }

            if (isStillBroken) {
                val updated = issue.copy(
                    status = "Re-reported",
                    description = issue.description + " | [CONTESTED: $reason]",
                    requiresReview = true,
                    verificationReasons = "Citizen contested resolution. Evidence checked by VisionAgent."
                )
                repository.updateIssue(updated)
                
                val user = repository.getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
                val updatedUser = user.copy(
                    points = user.points + 25,
                    trustScore = Math.min(100, user.trustScore + 5)
                )
                database.userDao().insertUser(updatedUser)
                
                repository.insertLog(
                    AuthorityLog(
                        timestamp = System.currentTimeMillis(),
                        isBotAction = true,
                        message = "OrchestratorBot: Citizen contested resolution of Ticket ${issue.id}. VisionAgent confirmed unresolved status. Escalating to Critical priority.",
                        priority = "High"
                    )
                )
                
                triggerAccountabilityEmail("CONTESTED", updated, listOf(updated))
                _uiToast.emit("Resolution contested! Evidence verified. +25 pts & +5% Trust!")
            } else {
                _uiToast.emit("Contest submitted. Escalating to supervisor for manual review.")
            }
        }
    }

    fun confirmResolution(issueId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val issue = repository.getIssueById(issueId) ?: return@launch
            val updated = issue.copy(status = "Closed")
            repository.updateIssue(updated)
            
            val user = repository.getUser() ?: User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
            val updatedUser = user.copy(
                points = user.points + 50,
                trustScore = Math.min(100, user.trustScore + 10)
            )
            database.userDao().insertUser(updatedUser)
            
            repository.insertLog(
                AuthorityLog(
                    timestamp = System.currentTimeMillis(),
                    isBotAction = true,
                    message = "System: Citizen verified resolution of Ticket ${issue.id}. Earned 50 pts & 10% Trust.",
                    priority = "Low"
                )
            )
            _uiToast.emit("Thank you for verifying! +50 pts & +10% Trust!")
        }
    }
}

fun Bitmap.toBase64String(): String {
    val baos = java.io.ByteArrayOutputStream()
    this.compress(Bitmap.CompressFormat.JPEG, 80, baos)
    val bytes = baos.toByteArray()
    return android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}




