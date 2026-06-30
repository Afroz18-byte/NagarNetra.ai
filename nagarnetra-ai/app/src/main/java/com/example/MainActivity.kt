package com.example

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// â”€â”€â”€ NagarNetra Civic Design System â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// Token aliases imported from theme Color.kt â€” use these throughout the app
// so that the entire UI shifts when the design system palette changes.
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

val CitizenPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

val CitizenSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.secondary

val CitizenBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val CitizenCardBg: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val CitizenTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val CitizenTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val AuthorityBackground = com.example.ui.theme.BackgroundDark    // #0F172A
val AuthorityCardBg     = com.example.ui.theme.SurfaceDark       // #1E293B
val AuthorityPrimary    = com.example.ui.theme.CivicNavyDark
val AuthorityAccent     = com.example.ui.theme.SignalTealDark
val AuthorityText       = com.example.ui.theme.OnBackgroundDark  // #E2E8F0

val ColorSuccess        = com.example.ui.theme.ColorSuccess      // #16A34A
val ColorWarning        = com.example.ui.theme.ColorWarning      // #F59E0B
val ColorDanger         = com.example.ui.theme.ColorDanger       // #DC2626

val NNDeepGreen = Color(0xFF004D40)
val NNGreen = Color(0xFF008A6A)
val NNMint = Color(0xFFEAF6F1)
val NNSoftSurface = Color(0xFFF8FBFA)
val NNLine = Color(0xFFE3ECE8)
val NNInk = Color(0xFF10201D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            val webViewCacheDir = java.io.File(cacheDir, "WebView")
            val subDirs = listOf(
                "",
                "Default",
                "Default/HTTP Cache",
                "Default/HTTP Cache/Code Cache",
                "Default/HTTP Cache/Code Cache/wasm",
                "Default/HTTP Cache/Code Cache/js"
            )
            for (dirName in subDirs) {
                val dir = if (dirName.isEmpty()) webViewCacheDir else java.io.File(webViewCacheDir, dirName)
                if (!dir.exists()) {
                    val created = dir.mkdirs()
                    android.util.Log.d("WebViewFix", "Created directory ${dir.absolutePath}: $created")
                }
                val r = dir.setReadable(true, false)
                val w = dir.setWritable(true, false)
                val x = dir.setExecutable(true, false)
                android.util.Log.d("WebViewFix", "Permissions for ${dir.name}: R=$r, W=$w, X=$x")
            }
        } catch (e: Exception) {
            android.util.Log.e("WebViewFix", "Error pre-creating WebView directories", e)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val currentRole = viewModel.currentRole.collectAsState().value
            val context = LocalContext.current

            LaunchedEffect(true) {
                viewModel.uiToast.collect { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }

            val appDarkModeOverride by viewModel.isAppDarkMode.collectAsState()
            val systemInDark = isSystemInDarkTheme()
            val resolvedDark = appDarkModeOverride ?: systemInDark

            com.example.ui.theme.NagarNetraAppTheme(darkTheme = resolvedDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationOrchestrator(viewModel)
                }
            }
        }
    }
}

// NagarNetraTheme is replaced by NagarNetraAppTheme in ui/theme/Theme.kt

@Composable
fun AppNavigationOrchestrator(viewModel: MainViewModel) {
    val loggedIn by viewModel.isLoggedIn.collectAsState()

    val permissionsLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocationGranted || coarseLocationGranted) {
            viewModel.fetchDeviceLocation()
        }
    }

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(
            arrayOf(
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
        viewModel.fetchDeviceLocation()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!loggedIn) LandingScreen(viewModel) else CitizenPortalLayout(viewModel)
    }
}
@Composable
fun AppHeaderBar(viewModel: MainViewModel) {
    val loggedIn by viewModel.isLoggedIn.collectAsState()
    val appDarkModeOverride by viewModel.isAppDarkMode.collectAsState()
    val systemInDark = isSystemInDarkTheme()
    val resolvedDark = appDarkModeOverride ?: systemInDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // App logo icon with semantic description for TalkBack
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.RemoveRedEye,
                    contentDescription = "NagarNetra logo",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "NagarNetra AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Eyes of the city",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { viewModel.toggleAppTheme(systemInDark) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = if (resolvedDark) Icons.Filled.WbSunny else Icons.Filled.NightsStay,
                    contentDescription = "Toggle Dark/Light Mode",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (loggedIn) {
                // 48dp touch target for accessibility (minimum recommended)
                IconButton(
                    onClick = { viewModel.doLogout() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = "Sign out of NagarNetra",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NagarSeparator(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val brandTeal = Color(0xFF0D5C4A)
        
        // Draw center dot
        drawCircle(brandTeal, radius = 3.dp.toPx(), center = Offset(w * 0.50f, h * 0.50f))
        
        // Draw left fading line
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, brandTeal),
                startX = w * 0.20f,
                endX = w * 0.48f
            ),
            start = Offset(w * 0.20f, h * 0.50f),
            end = Offset(w * 0.48f, h * 0.50f),
            strokeWidth = 1.dp.toPx()
        )
        
        // Draw right fading line
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(brandTeal, Color.Transparent),
                startX = w * 0.52f,
                endX = w * 0.80f
            ),
            start = Offset(w * 0.52f, h * 0.50f),
            end = Offset(w * 0.80f, h * 0.50f),
            strokeWidth = 1.dp.toPx()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandingScreen(viewModel: MainViewModel) {
    val isAuthenticating by viewModel.isAuthenticating.collectAsState()
    val isRegisterMode by viewModel.isRegisterMode.collectAsState()
    val authEmail by viewModel.authEmail.collectAsState()
    val authPassword by viewModel.authPassword.collectAsState()
    val authDisplayName by viewModel.authDisplayName.collectAsState()
    val authWard by viewModel.authWard.collectAsState()
    val authError by viewModel.authError.collectAsState()
    
    var showAuthForm by remember { mutableStateOf(false) }

    // Breathing animation for the logo
    val logoScaleTransition = rememberInfiniteTransition(label = "LogoScaleAnim")
    val logoScale by logoScaleTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LogoScale"
    )

    // Horizontal floating clouds offset animation
    val cloudShiftTransition = rememberInfiniteTransition(label = "CloudShiftAnim")
    val cloudOffset by cloudShiftTransition.animateFloat(
        initialValue = 0f,
        targetValue = 35f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "CloudOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (showAuthForm) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nagarnetra",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0D5C4A),
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                NagarSeparator(modifier = Modifier.fillMaxWidth().height(8.dp))
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, Color(0xFF0D5C4A).copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isRegisterMode) "Create Account" else "Welcome Back",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0D5C4A)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isRegisterMode) "Register once to secure your profile" else "Sign in to access your dashboard",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (authError != null) {
                            Text(
                                text = authError!!,
                                color = Color.Red,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = authDisplayName,
                                onValueChange = { viewModel.authDisplayName.value = it },
                                label = { Text("Full Name", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Filled.Person, null, tint = Color(0xFF0D5C4A), modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF0D5C4A),
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            var wardExpanded by remember { mutableStateOf(false) }
                            val wardsList = listOf(
                                "Indiranagar (Ward 80)",
                                "Koramangala (Ward 151)",
                                "HSR Layout (Ward 174)",
                                "Whitefield (Ward 84)",
                                "Jayanagar (Ward 153)"
                            )
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = authWard,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Select Ward", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Filled.Home, null, tint = Color(0xFF0D5C4A), modifier = Modifier.size(18.dp)) },
                                    trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null, tint = Color(0xFF0D5C4A)) },
                                    modifier = Modifier.fillMaxWidth().clickable { wardExpanded = true },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0D5C4A),
                                        unfocusedBorderColor = Color.LightGray
                                    )
                                )
                                DropdownMenu(
                                    expanded = wardExpanded,
                                    onDismissRequest = { wardExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f).background(Color.White)
                                ) {
                                    wardsList.forEach { ward ->
                                        DropdownMenuItem(
                                            text = { Text(ward) },
                                            onClick = {
                                                viewModel.authWard.value = ward
                                                wardExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedTextField(
                            value = authEmail,
                            onValueChange = { viewModel.authEmail.value = it },
                            label = { Text("Email", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Filled.Email, null, tint = Color(0xFF0D5C4A), modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D5C4A),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        var passwordVisible by remember { mutableStateOf(false) }
                        OutlinedTextField(
                            value = authPassword,
                            onValueChange = { viewModel.authPassword.value = it },
                            label = { Text("Password", fontSize = 11.sp) },
                            leadingIcon = { Icon(Icons.Filled.Lock, null, tint = Color(0xFF0D5C4A), modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0D5C4A),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                viewModel.handleEmailAuth()
                            },
                            enabled = !isAuthenticating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(23.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D5C4A))
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Text(
                                    text = if (isRegisterMode) "Register & Start" else "Sign In",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = { viewModel.isRegisterMode.value = !isRegisterMode }
                        ) {
                            Text(
                                text = if (isRegisterMode) "Already have an account? Sign In" else "New User? Create Account",
                                color = Color(0xFF0D5C4A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { showAuthForm = false }
                        ) {
                            Text(
                                text = "Back to Home",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Image(
                    painter = painterResource(id = R.drawable.landing_hero),
                    contentDescription = "Nagarnetra Hero",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(28.dp))
                
                Button(
                    onClick = { 
                        viewModel.isRegisterMode.value = false
                        showAuthForm = true 
                    },
                    enabled = !isAuthenticating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D5C4A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Get Started",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Icon(
                            Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .size(24.dp)
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .padding(top = 24.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "Sign In",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D5C4A),
                        modifier = Modifier.clickable { 
                            viewModel.isRegisterMode.value = false
                            showAuthForm = true 
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NagarEyeLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Gradients for premium design depth
        val pinGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D5C4A), Color(0xFF008A6A)),
            startY = h * 0.16f,
            endY = h * 0.82f
        )
        val buildingGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF008A6A), Color(0xFF0D5C4A)),
            startY = h * 0.48f,
            endY = h * 0.74f
        )
        val waveGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0D5C4A), Color(0xFF053E32)),
            startY = h * 0.70f,
            endY = h * 0.81f
        )

        // 1. Draw the map pin outline path
        val pinPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.50f, h * 0.82f)
            cubicTo(w * 0.68f, h * 0.65f, w * 0.72f, h * 0.52f, w * 0.72f, h * 0.38f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = w * 0.28f,
                    top = h * 0.16f,
                    right = w * 0.72f,
                    bottom = h * 0.60f
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            cubicTo(w * 0.28f, h * 0.52f, w * 0.32f, h * 0.65f, w * 0.50f, h * 0.82f)
            close()
        }
        drawPath(pinPath, pinGradient, style = Stroke(width = 8.dp.toPx()))
        
        // 2. Draw the pin center circular ring (eye pupil)
        drawCircle(
            brush = pinGradient,
            radius = 8.dp.toPx(),
            center = Offset(w * 0.50f, h * 0.38f),
            style = Stroke(width = 4.dp.toPx())
        )
        
        // 3. Draw the wavy ground base
        val wavePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.35f, h * 0.74f)
            quadraticTo(w * 0.44f, h * 0.70f, w * 0.50f, h * 0.74f)
            quadraticTo(w * 0.56f, h * 0.71f, w * 0.65f, h * 0.74f)
            quadraticTo(w * 0.68f, h * 0.78f, w * 0.62f, h * 0.81f)
            lineTo(w * 0.38f, h * 0.81f)
            quadraticTo(w * 0.32f, h * 0.78f, w * 0.35f, h * 0.74f)
            close()
        }
        drawPath(wavePath, waveGradient)
        
        // 4. Draw the three skyscrapers sitting on the wavy base
        // Center Building (Tallest, flat roof)
        val centerBuilding = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.47f, h * 0.74f)
            lineTo(w * 0.47f, h * 0.48f)
            lineTo(w * 0.53f, h * 0.48f)
            lineTo(w * 0.53f, h * 0.74f)
            close()
        }
        drawPath(centerBuilding, buildingGradient)
        
        // Left Building (Shortest, flat roof)
        val leftBuilding = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.39f, h * 0.74f)
            lineTo(w * 0.39f, h * 0.58f)
            lineTo(w * 0.45f, h * 0.58f)
            lineTo(w * 0.45f, h * 0.74f)
            close()
        }
        drawPath(leftBuilding, buildingGradient)
        
        // Right Building (Medium, flat roof)
        val rightBuilding = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.55f, h * 0.74f)
            lineTo(w * 0.55f, h * 0.56f)
            lineTo(w * 0.61f, h * 0.56f)
            lineTo(w * 0.61f, h * 0.74f)
            close()
        }
        drawPath(rightBuilding, buildingGradient)
        
        // 5. Draw window highlights (tiny white rectangles inside buildings)
        val winW = 1.5.dp.toPx()
        val winH = 2.2.dp.toPx()
        
        // Center building windows (2 columns x 3 rows)
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.485f, h * 0.52f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.508f, h * 0.52f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.485f, h * 0.59f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.508f, h * 0.59f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.485f, h * 0.66f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.508f, h * 0.66f), androidx.compose.ui.geometry.Size(winW, winH))
        
        // Left building windows (2 columns x 2 rows)
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.405f, h * 0.62f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.428f, h * 0.62f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.405f, h * 0.68f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.428f, h * 0.68f), androidx.compose.ui.geometry.Size(winW, winH))
        
        // Right building windows (2 columns x 2 rows)
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.565f, h * 0.60f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.588f, h * 0.60f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.565f, h * 0.67f), androidx.compose.ui.geometry.Size(winW, winH))
        drawRect(Color.White.copy(alpha = 0.9f), Offset(w * 0.588f, h * 0.67f), androidx.compose.ui.geometry.Size(winW, winH))
    }
}

@Composable
fun NagarLandingScene(cloudOffset: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.clipToBounds()) {
        val w = size.width
        val h = size.height
        
        val horizon = h * 0.76f
        
        // 1. Draw the sky gradient (White to very soft mint green)
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.White, Color(0xFFE5F2EC)),
                startY = 0f,
                endY = horizon
            ),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(w, horizon)
        )
        
        // Fill lower half with soft mint green
        drawRect(
            color = Color(0xFFE5F2EC),
            topLeft = Offset(0f, horizon),
            size = androidx.compose.ui.geometry.Size(w, h - horizon)
        )
        
        // 2. Horizon misty glow circle (soft fading teal-green)
        drawCircle(
            color = Color(0xFFD4ECE1).copy(alpha = 0.50f),
            radius = w * 0.40f,
            center = Offset(w * 0.50f, horizon)
        )
        
        // 3. Draw soft clouds (faint white/mint shapes) with horizontal drifting
        repeat(4) { i ->
            val cloudY = h * (0.12f + i * 0.05f)
            val baseCloudX = w * (0.05f + i * 0.22f)
            val finalX = if (i % 2 == 0) baseCloudX + cloudOffset else baseCloudX - cloudOffset
            drawOval(
                Color.White.copy(alpha = 0.65f),
                topLeft = Offset(finalX, cloudY),
                size = androidx.compose.ui.geometry.Size(w * 0.22f, h * 0.035f)
            )
        }
        
        // 4. Distant skyline silhouettes (background layer, very light green-gray) with vertical gradients
        val distantGreenGradient = Brush.verticalGradient(
            colors = listOf(Color(0xFFE2EFEA), Color(0xFFCADED4)),
            startY = horizon - h * 0.20f,
            endY = horizon
        )
        drawRect(distantGreenGradient, Offset(w * 0.20f, horizon - h * 0.09f), androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.09f))
        drawRect(distantGreenGradient, Offset(w * 0.26f, horizon - h * 0.13f), androidx.compose.ui.geometry.Size(w * 0.05f, h * 0.13f))
        drawRect(distantGreenGradient, Offset(w * 0.31f, horizon - h * 0.16f), androidx.compose.ui.geometry.Size(w * 0.04f, h * 0.16f))
        
        drawRect(distantGreenGradient, Offset(w * 0.46f, horizon - h * 0.20f), androidx.compose.ui.geometry.Size(w * 0.05f, h * 0.20f))
        drawRect(distantGreenGradient, Offset(w * 0.51f, horizon - h * 0.14f), androidx.compose.ui.geometry.Size(w * 0.06f, h * 0.14f))
        drawRect(distantGreenGradient, Offset(w * 0.57f, horizon - h * 0.10f), androidx.compose.ui.geometry.Size(w * 0.05f, h * 0.10f))
        
        // 5. Buildings in middle perspective layer (with windows, gradient mint green)
        fun drawBuilding(left: Float, top: Float, bw: Float, bh: Float, colorStart: Color, colorEnd: Color, windowCols: Int, windowRows: Int) {
            val buildingGrad = Brush.verticalGradient(
                colors = listOf(colorStart, colorEnd),
                startY = top,
                endY = top + bh
            )
            drawRect(buildingGrad, Offset(left, top), androidx.compose.ui.geometry.Size(bw, bh))
            val winW = 1.8.dp.toPx()
            val winH = 3.2.dp.toPx()
            val colSpacing = bw / (windowCols + 1)
            val rowSpacing = bh / (windowRows + 1)
            repeat(windowRows) { r ->
                repeat(windowCols) { c ->
                    if ((r + c) % 2 != 0) {
                        val winX = left + colSpacing * (c + 1) - winW / 2
                        val winY = top + rowSpacing * (r + 1) - winH / 2
                        drawRect(Color.White.copy(alpha = 0.65f), Offset(winX, winY), androidx.compose.ui.geometry.Size(winW, winH))
                    }
                }
            }
        }
        
        // Left side perspective buildings
        drawBuilding(w * 0.02f, horizon - h * 0.15f, w * 0.09f, h * 0.15f, Color(0xFFB3D7C9), Color(0xFF91BAAB), 2, 4)
        drawBuilding(w * 0.11f, horizon - h * 0.10f, w * 0.08f, h * 0.10f, Color(0xFFBEDFDF), Color(0xFF9EBEBE), 2, 3)
        drawBuilding(w * 0.19f, horizon - h * 0.07f, w * 0.07f, h * 0.07f, Color(0xFFC7E6DE), Color(0xFFA7C6BE), 1, 2)
        
        // Right side perspective buildings
        drawBuilding(w * 0.89f, horizon - h * 0.16f, w * 0.09f, h * 0.16f, Color(0xFFB3D7C9), Color(0xFF91BAAB), 2, 4)
        drawBuilding(w * 0.81f, horizon - h * 0.12f, w * 0.08f, h * 0.12f, Color(0xFFBEDFDF), Color(0xFF9EBEBE), 2, 3)
        drawBuilding(w * 0.74f, horizon - h * 0.08f, w * 0.07f, h * 0.08f, Color(0xFFC7E6DE), Color(0xFFA7C6BE), 1, 2)
        
        // 6. Draw the Road
        val roadTop = horizon
        val road = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.45f, roadTop)
            lineTo(w * 0.55f, roadTop)
            lineTo(w * 1.10f, h)
            lineTo(w * -0.10f, h)
            close()
        }
        drawPath(
            road,
            brush = Brush.verticalGradient(
                listOf(Color(0xFFC6DDD3), Color(0xFFEBF4F0)),
                startY = roadTop,
                endY = h
            )
        )
        
        // Perspective road center dashes
        drawLine(
            Color.White.copy(alpha = 0.85f),
            Offset(w * 0.50f, roadTop + (h - roadTop) * 0.10f),
            Offset(w * 0.50f, roadTop + (h - roadTop) * 0.22f),
            2.5.dp.toPx()
        )
        drawLine(
            Color.White.copy(alpha = 0.85f),
            Offset(w * 0.50f, roadTop + (h - roadTop) * 0.32f),
            Offset(w * 0.50f, roadTop + (h - roadTop) * 0.50f),
            4.5.dp.toPx()
        )
        drawLine(
            Color.White.copy(alpha = 0.85f),
            Offset(w * 0.50f, roadTop + (h - roadTop) * 0.65f),
            Offset(w * 0.50f, h),
            7.dp.toPx()
        )
        
        // Road side boundary lines (in white)
        drawLine(
            Color.White.copy(alpha = 0.85f),
            Offset(w * 0.45f, roadTop),
            Offset(w * -0.10f, h),
            3.dp.toPx()
        )
        drawLine(
            Color.White.copy(alpha = 0.85f),
            Offset(w * 0.55f, roadTop),
            Offset(w * 1.10f, h),
            3.dp.toPx()
        )
        
        // 7. Draw streetlights (thin dark green poles, soft warm yellow light points with halos)
        fun drawStreetlight(lampX: Float, lampY: Float, scale: Float, isLeft: Boolean) {
            val poleColor = Color(0xFF005A44).copy(alpha = 0.70f)
            val groundY = roadTop + (h - roadTop) * scale * 0.85f
            val baseOffset = if (isLeft) -30.dp.toPx() * scale else 30.dp.toPx() * scale
            
            // Curved pole
            val polePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(lampX + baseOffset, groundY)
                lineTo(lampX + baseOffset, lampY + 14.dp.toPx() * scale)
                quadraticTo(
                    lampX + baseOffset, lampY,
                    lampX, lampY
                )
            }
            drawPath(
                polePath,
                poleColor,
                style = Stroke(width = 2.dp.toPx() * scale, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            
            // Glowing bulb with halo
            drawCircle(Color(0xFFFFEE88).copy(alpha = 0.35f), radius = 9.dp.toPx() * scale, center = Offset(lampX, lampY))
            drawCircle(Color(0xFFFFEE88), radius = 4.dp.toPx() * scale, center = Offset(lampX, lampY))
            drawCircle(Color.White, radius = 2.dp.toPx() * scale, center = Offset(lampX, lampY))
        }
        
        // Left side streetlights (near, mid, far)
        drawStreetlight(w * 0.12f, h * 0.63f, 1.00f, isLeft = true)
        drawStreetlight(w * 0.28f, h * 0.61f, 0.55f, isLeft = true)
        drawStreetlight(w * 0.39f, h * 0.59f, 0.30f, isLeft = true)
        
        // Right side streetlights (near, mid, far)
        drawStreetlight(w * 0.88f, h * 0.63f, 1.00f, isLeft = false)
        drawStreetlight(w * 0.72f, h * 0.61f, 0.55f, isLeft = false)
        drawStreetlight(w * 0.61f, h * 0.59f, 0.30f, isLeft = false)
        
        // 8. Draw trees lining the road (perspective-scaled, with radial gradients)
        fun drawTree(treeX: Float, treeY: Float, scale: Float) {
            val trunkW = 3.5.dp.toPx() * scale
            val trunkH = 28.dp.toPx() * scale
            
            // Trunk
            drawRect(Color(0xFF005A44).copy(alpha = 0.80f), Offset(treeX - trunkW / 2, treeY), androidx.compose.ui.geometry.Size(trunkW, trunkH))
            
            // Foliage - two overlapping green circles with gradients
            val leafGradientLight = Brush.radialGradient(
                colors = listOf(Color(0xFF81C7A7), Color(0xFF55A485)),
                center = Offset(treeX, treeY - 6.dp.toPx() * scale),
                radius = 18.dp.toPx() * scale
            )
            val leafGradientDark = Brush.radialGradient(
                colors = listOf(Color(0xFF559C80), Color(0xFF2C7D60)),
                center = Offset(treeX - 6.dp.toPx() * scale, treeY + 2.dp.toPx() * scale),
                radius = 13.dp.toPx() * scale
            )
            drawCircle(brush = leafGradientLight, radius = 18.dp.toPx() * scale, center = Offset(treeX, treeY - 6.dp.toPx() * scale))
            drawCircle(brush = leafGradientDark, radius = 13.dp.toPx() * scale, center = Offset(treeX - 6.dp.toPx() * scale, treeY + 2.dp.toPx() * scale))
        }
        
        // Left side trees
        drawTree(w * 0.08f, h * 0.82f, 1.00f)
        drawTree(w * 0.20f, h * 0.71f, 0.60f)
        drawTree(w * 0.30f, h * 0.63f, 0.35f)
        
        // Right side trees
        drawTree(w * 0.92f, h * 0.82f, 1.00f)
        drawTree(w * 0.80f, h * 0.71f, 0.60f)
        drawTree(w * 0.70f, h * 0.63f, 0.35f)

        // 9. Draw the bottom mask curve (to blend with the white container background)
        val maskCurve = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, h)
            lineTo(0f, h - 35.dp.toPx())
            quadraticTo(
                w * 0.50f, h - 5.dp.toPx(), // dips in the middle to let the perspective road shine through!
                w, h - 35.dp.toPx()
            )
            lineTo(w, h)
            close()
        }
        drawPath(maskCurve, Color.White)
    }
}
// ==========================================
// PORTAL - CITIZEN EXPERIENCE
// ==========================================
@Composable
fun CitizenPortalLayout(viewModel: MainViewModel) {
    val activeTab by viewModel.activeCitizenTab.collectAsState()
    val chatOpen by viewModel.chatOpen.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val tabs = listOf(
        Triple("Home", Icons.Filled.Home, "Home"),
        Triple("Reports", Icons.Filled.ReceiptLong, "Reports"),
        Triple("Report", Icons.Filled.AddAPhoto, "Report"),
        Triple("Community", Icons.Filled.Forum, "Community"),
        Triple("Profile", Icons.Filled.AccountCircle, "Profile")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NagarNetraDrawer(viewModel = viewModel) { destination ->
                viewModel.activeCitizenTab.value = destination
                if (destination == "Report") viewModel.resetReportingFlow()
                scope.launch { drawerState.close() }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    NagarNetraTopBar(
                        title = when (activeTab) {
                            "Report" -> "Report an Issue"
                            "ReportDetails" -> "Report Details"
                            "Reports", "Status" -> "My Reports"
                            "Map" -> "Nearby Issues"
                            "Community" -> "Community Chat"
                            "Profile" -> "Profile"
                            else -> "Nagarnetra"
                        },
                        showBack = activeTab == "Report" || activeTab == "Map" || activeTab == "Community" || activeTab == "ReportDetails",
                        onBack = { if (activeTab == "ReportDetails") viewModel.activeCitizenTab.value = "Reports" else viewModel.activeCitizenTab.value = "Home" },
                        onMenu = { scope.launch { drawerState.open() } },
                        onBell = { if (activeTab != "Profile") viewModel.activeCitizenTab.value = "Updates" }
                    )
                },
                bottomBar = {
                    if (activeTab != "ReportDetails") NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                        tabs.forEach { (tabName, icon, label) ->
                            val selected = activeTab == tabName || (tabName == "Reports" && activeTab == "Status")
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    viewModel.activeCitizenTab.value = tabName
                                    if (tabName == "Report") viewModel.resetReportingFlow()
                                },
                                icon = {
                                    Box(
                                        modifier = if (tabName == "Report") Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(NNDeepGreen) else Modifier,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (tabName == "Report") Color.White else if (selected) NNDeepGreen else Color(0xFF6B7672)
                                        )
                                    }
                                },
                                label = { Text(label, fontSize = 10.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = NNDeepGreen,
                                    selectedTextColor = NNDeepGreen,
                                    indicatorColor = NNMint,
                                    unselectedIconColor = Color(0xFF6B7672),
                                    unselectedTextColor = Color(0xFF6B7672)
                                )
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .background(NNSoftSurface)
                ) {
                    when (activeTab) {
                        "Home" -> CitizenHomeScreen(viewModel)
                        "Map" -> CitizenMapScreen(viewModel)
                        "Community" -> CitizenCommunityChatScreen(viewModel)
                        "Report" -> CitizenReportScreen(viewModel)
                        "Reports", "Status", "Updates" -> CitizenUpdatesScreen(viewModel)
                        "ReportDetails" -> CitizenReportDetailsScreen(viewModel)
                        "Profile" -> CitizenProfileScreen(viewModel)
                        else -> CitizenHomeScreen(viewModel)
                    }
                }
            }

            if (activeTab != "Report" && activeTab != "Community" && activeTab != "ReportDetails") {
                SmallFloatingActionButton(
                    onClick = { viewModel.chatOpen.value = !chatOpen },
                    containerColor = NNDeepGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 92.dp)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = "Open AI assistant")
                }
            }

            AnimatedVisibility(
                visible = chatOpen,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CivicChatBotPanel(viewModel)
            }
        }
    }
}

@Composable
fun NagarNetraTopBar(title: String, showBack: Boolean, onBack: () -> Unit, onMenu: () -> Unit, onBell: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = if (showBack) onBack else onMenu, modifier = Modifier.size(44.dp)) {
            Icon(if (showBack) Icons.Filled.ArrowBack else Icons.Filled.List, contentDescription = if (showBack) "Back" else "Menu", tint = NNInk)
        }
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = NNInk, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        IconButton(onClick = onBell, modifier = Modifier.size(44.dp)) {
            Icon(if (title == "Profile") Icons.Filled.Edit else Icons.Filled.Notifications, contentDescription = if (title == "Profile") "Edit profile" else "Notifications", tint = NNInk)
        }
    }
}

@Composable
fun NagarNetraDrawer(viewModel: MainViewModel, onNavigate: (String) -> Unit) {
    val activeTab by viewModel.activeCitizenTab.collectAsState()
    ModalDrawerSheet(
        drawerContainerColor = NNDeepGreen,
        drawerContentColor = Color.White,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 22.dp)) {
                Column {
                    Text("Nagarnetra", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text("Together for a\nBetter Tomorrow", fontSize = 10.sp, color = Color.White.copy(alpha = 0.78f), lineHeight = 13.sp)
                }
            }
            DrawerNavItem("Home", Icons.Filled.Home, activeTab == "Home") { onNavigate("Home") }
            DrawerNavItem("My Reports", Icons.Filled.ReceiptLong, activeTab == "Reports" || activeTab == "Status") { onNavigate("Reports") }
            DrawerNavItem("Report an Issue", Icons.Filled.AddAPhoto, activeTab == "Report") { onNavigate("Report") }
            DrawerNavItem("Nearby Issues", Icons.Filled.Map, activeTab == "Map") { onNavigate("Map") }
            DrawerNavItem("Community Chat", Icons.Filled.Forum, activeTab == "Community") { onNavigate("Community") }
            DrawerNavItem("Notifications", Icons.Filled.Notifications, activeTab == "Updates") { onNavigate("Updates") }
            DrawerNavItem("Help & Support", Icons.Filled.HeadsetMic, false) {
                viewModel.chatOpen.value = true
                onNavigate(activeTab)
            }
            DrawerNavItem("Settings", Icons.Filled.Build, activeTab == "Profile") { onNavigate("Profile") }
            Spacer(modifier = Modifier.weight(1f))
            Divider(color = Color.White.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 12.dp))
            DrawerNavItem("Logout", Icons.Filled.Logout, false) { viewModel.doLogout() }
        }
    }
}

@Composable
fun DrawerNavItem(label: String, icon: ImageVector, selected: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color.White.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.96f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun CitizenHomeScreen(viewModel: MainViewModel) {
    val allIssues by viewModel.allIssues.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val profile = currentUser ?: com.example.data.User(id = "sandbox_user", email = "sandbox@nagarnetra.in", name = "Raj Sharma")
    val recentIssues = remember(allIssues) { allIssues.sortedByDescending { it.timePosted }.take(3) }
    val criticalIssues = remember(allIssues) { allIssues.sortedByDescending { it.upvotes } }
    var feedTab by remember { mutableStateOf("Recent") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NNSoftSurface),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hello, Citizen", color = Color(0xFF5F6E69), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Let's make our\ncity better together.", color = NNInk, fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.ExtraBold)
                }
                CityIllustration(modifier = Modifier.size(width = 150.dp, height = 108.dp))
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = NNDeepGreen), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeFeatureCard("Report an Issue", "Report problems\nin your area", Icons.Filled.AddAPhoto, Modifier.weight(1f)) {
                            viewModel.activeCitizenTab.value = "Report"
                            viewModel.resetReportingFlow()
                        }
                        HomeFeatureCard("My Reports", "Track your\nreported issues", Icons.Filled.ReceiptLong, Modifier.weight(1f)) { viewModel.activeCitizenTab.value = "Reports" }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        HomeFeatureCard("Nearby Issues", "See issues\nreported nearby", Icons.Filled.LocationOn, Modifier.weight(1f)) { viewModel.activeCitizenTab.value = "Map" }
                        HomeFeatureCard("Community Chat", "Connect with\nnearby residents", Icons.Filled.Forum, Modifier.weight(1f)) { viewModel.activeCitizenTab.value = "Community" }
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Recent Issues",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (feedTab == "Recent") NNInk else Color.Gray,
                        modifier = Modifier.clickable { feedTab = "Recent" }
                    )
                    Text(
                        text = "Critical Board 🔥",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (feedTab == "Critical") NNInk else Color.Gray,
                        modifier = Modifier.clickable { feedTab = "Critical" }
                    )
                }
                TextButton(onClick = { viewModel.activeCitizenTab.value = "Reports" }) { Text("View All", color = NNDeepGreen, fontWeight = FontWeight.Bold) }
            }
        }

        if (feedTab == "Recent") {
            if (recentIssues.isEmpty()) {
                item {
                    DemoUpdateRow("Street Light Fixed in Sector 12", "Resolved", "2h ago", Icons.Filled.Lightbulb)
                    Spacer(modifier = Modifier.height(10.dp))
                    DemoUpdateRow("Garbage Pickup in Green Park", "In Progress", "5h ago", Icons.Filled.Delete)
                }
            } else {
                items(recentIssues) { issue ->
                    RecentIssueRow(issue = issue) {
                        viewModel.selectedCitizenIssue.value = issue
                        viewModel.activeCitizenTab.value = "ReportDetails"
                    }
                }
            }
        } else {
            if (criticalIssues.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No reported issues to show yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                items(criticalIssues) { issue ->
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        InstagramIssueCard(issue = issue) {
                            viewModel.upvoteIssue(issue.id)
                        }
                        if (issue.upvotes >= 25) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Warning, "Outrage", tint = ColorDanger, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "PUBLIC ALARM: This issue has crossed 25 endorsements. Automated priority escalation activated.",
                                        color = ColorDanger,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else if (issue.upvotes >= 10) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.NotificationsActive, "Critical", tint = ColorWarning, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Urgent: 10+ citizens endorsed. PriorityBot elevated severity.",
                                        color = Color(0xFFB45309),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, NNLine)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(NNMint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Stars, contentDescription = null, tint = NNDeepGreen)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${profile.points} civic points", fontWeight = FontWeight.ExtraBold, color = NNInk)
                        Text("Level ${profile.level} contributor in ${profile.ward}", fontSize = 12.sp, color = Color(0xFF66736F), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
fun CityIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val ground = h * 0.78f
        drawLine(Color(0xFFBCE5DA), Offset(0f, ground), Offset(w, ground), 4.dp.toPx())
        repeat(6) { index ->
            val bw = w / 9f
            val left = index * bw * 1.38f + bw * 0.2f
            val top = ground - (36.dp.toPx() + (index % 3) * 18.dp.toPx())
            drawRoundRect(Color(0xFF9AD8CC).copy(alpha = 0.78f), Offset(left, top), androidx.compose.ui.geometry.Size(bw, ground - top), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.7f), 2.dp.toPx(), Offset(left + bw * 0.32f, top + 13.dp.toPx()))
            drawCircle(Color.White.copy(alpha = 0.7f), 2.dp.toPx(), Offset(left + bw * 0.68f, top + 25.dp.toPx()))
        }
        drawCircle(Color(0xFF74C69D).copy(alpha = 0.8f), 18.dp.toPx(), Offset(w * 0.84f, ground - 10.dp.toPx()))
        drawRect(Color(0xFF5B8C78), Offset(w * 0.83f, ground - 8.dp.toPx()), androidx.compose.ui.geometry.Size(4.dp.toPx(), 24.dp.toPx()))
    }
}

@Composable
fun HomeFeatureCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(124.dp).clickable(onClick = onClick), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.09f))) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, maxLines = 2)
                Spacer(modifier = Modifier.height(3.dp))
                Text(subtitle, color = Color.White.copy(alpha = 0.78f), fontSize = 11.sp, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
fun RecentIssueRow(issue: Issue, onClick: () -> Unit) {
    val bitmap = remember(issue.photoBase64) { issue.photoBase64?.toBitmap() }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, NNLine)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(NNMint), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(categoryIcon(issue.category), contentDescription = null, tint = NNDeepGreen)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(issue.title, fontWeight = FontWeight.ExtraBold, color = NNInk, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(issue.status)
            }
            Text("${(System.currentTimeMillis() - issue.timePosted).coerceAtLeast(0L) / 3600000L}h ago", fontSize = 11.sp, color = Color(0xFF66736F))
        }
    }
}

@Composable
fun DemoUpdateRow(title: String, status: String, time: String, icon: ImageVector) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, NNLine)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(NNMint), contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = NNDeepGreen) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = NNInk, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                StatusPill(status)
            }
            Text(time, fontSize = 11.sp, color = Color(0xFF66736F))
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val color = when (status.lowercase()) {
        "resolved" -> ColorSuccess
        "rejected" -> ColorDanger
        "in progress" -> Color(0xFF3B82F6)
        else -> NNGreen
    }
    Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 4.dp)) {
        Text(status, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "garbage" -> Icons.Filled.Delete
    "water", "water leakage" -> Icons.Filled.WaterDrop
    "lights", "street light" -> Icons.Filled.Lightbulb
    "pothole", "road damage" -> Icons.Filled.Warning
    "sewage", "sewage overflow" -> Icons.Filled.WaterDrop
    else -> Icons.Filled.Build
}
@Composable
fun InstagramIssueCard(issue: Issue, onUpvote: () -> Unit) {
    val bitmap = remember(issue.photoBase64) { issue.photoBase64?.toBitmap() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CitizenCardBg),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CitizenPrimary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(issue.category.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CitizenPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(issue.locationName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CitizenTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(issue.ward, fontSize = 10.sp, color = CitizenTextSecondary)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (issue.severity >= 8) ColorDanger else ColorWarning)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFF232B38), Color(0xFF131922)))),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Report Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (issue.category.lowercase()) {
                                "pothole" -> Icons.Filled.Warning
                                "water" -> Icons.Filled.WaterDrop
                                "lights" -> Icons.Filled.Lightbulb
                                else -> Icons.Filled.Delete
                            },
                            contentDescription = null,
                            tint = CitizenPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(issue.title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(issue.description, fontSize = 12.sp, color = CitizenTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onUpvote() }
                            .background(CitizenPrimary.copy(alpha = 0.08f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.ThumbUp, contentDescription = null, tint = CitizenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${issue.upvotes} Upvotes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CitizenPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CitizenSecondary.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(issue.status.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CitizenSecondary)
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// CITIZEN: SCREEN MAP
// ------------------------------------------
@Composable
fun CitizenMapScreen(viewModel: MainViewModel) {
    val allIssues by viewModel.allIssues.collectAsState()
    val mapSelectedIssue by viewModel.mapSelectedIssue.collectAsState()
    val activeFilter by viewModel.mapActiveFilter.collectAsState()
    val isMapDarkMode by viewModel.isMapDarkMode.collectAsState()
    var isSatelliteMode by remember { mutableStateOf(false) }
    val currentLat by viewModel.reportLatitude.collectAsState()
    val currentLng by viewModel.reportLongitude.collectAsState()

    val filtered = remember(allIssues, activeFilter) {
        if (activeFilter == "All") allIssues
        else allIssues.filter { it.category.lowercase() == activeFilter.lowercase() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        com.example.ui.GoogleMapsWebView(
            modifier = Modifier.fillMaxSize(),
            mode = "cluster",
            issues = filtered,
            selectedIssue = mapSelectedIssue,
            reportLatitude = currentLat,
            reportLongitude = currentLng,
            isDarkMode = isMapDarkMode,
            useSatellite = isSatelliteMode,
            onIssueSelected = { viewModel.mapSelectedIssue.value = it }
        )

        // Filter chips overlayed beautifully at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Pothole", "Water", "Garbage", "Lights")
            filters.forEach { name ->
                val isSelected = activeFilter == name
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.mapActiveFilter.value = name },
                    label = { Text(name, fontSize = 11.sp, color = if (isSelected) Color.White else CitizenSecondary) }
                )
            }
        }

        // Clean map controls on the top right
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 12.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isMapDarkMode) Color(0xFF1E293B) else Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            border = BorderStroke(1.dp, if (isMapDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { isSatelliteMode = !isSatelliteMode }) {
                    Icon(
                        imageVector = Icons.Filled.SatelliteAlt,
                        contentDescription = "Satellite Map",
                        tint = if (isSatelliteMode) Color(0xFF0EA5E9) else if (isMapDarkMode) Color.White else CitizenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Divider(modifier = Modifier.width(32.dp), color = if (isMapDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                IconButton(onClick = { viewModel.isMapDarkMode.value = !isMapDarkMode }) {
                    Icon(
                        imageVector = if (isMapDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = if (isMapDarkMode) Color(0xFFF59E0B) else CitizenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Divider(modifier = Modifier.width(32.dp), color = if (isMapDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                IconButton(onClick = { viewModel.mapSelectedIssue.value = null }) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation, 
                        contentDescription = "Recenter Map", 
                        tint = if (isMapDarkMode) Color.White else CitizenSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = mapSelectedIssue != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            mapSelectedIssue?.let { issue ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isMapDarkMode) Color(0xFF1E293B) else Color.White),
                    border = BorderStroke(1.dp, if (isMapDarkMode) Color(0xFF334155) else CitizenPrimary.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = issue.title, 
                                    fontSize = 15.sp, 
                                    fontWeight = FontWeight.Bold, 
                                    color = if (isMapDarkMode) Color.White else CitizenTextPrimary
                                )
                                Text("📍 ${issue.locationName}", fontSize = 11.sp, color = if (isMapDarkMode) Color(0xFF94A3B8) else CitizenTextSecondary)
                            }
                            IconButton(onClick = { viewModel.mapSelectedIssue.value = null }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = issue.description, 
                            fontSize = 11.sp, 
                            color = if (isMapDarkMode) Color(0xFFCBD5E1) else CitizenTextSecondary, 
                            maxLines = 2, 
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${issue.upvotes} Citizens Affected", 
                                fontSize = 11.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = if (isMapDarkMode) Color(0xFF38BDF8) else CitizenPrimary
                            )
                            Button(
                                onClick = { viewModel.upvoteIssue(issue.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = CitizenPrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Upvote", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// CITIZEN: SCREEN REPORTING FLOW
// ------------------------------------------
@Composable
fun CitizenReportScreen(viewModel: MainViewModel) {
    val step by viewModel.reportingStep.collectAsState()
    val aiResult by viewModel.aiAnalysisResult.collectAsState()
    val notes by viewModel.exactLocationNotes.collectAsState()
    val idStr by viewModel.generatedIssueId.collectAsState()
    val mergeDuo by viewModel.duplicateMerged.collectAsState()

    Crossfade(targetState = step) { cur ->
        when (cur) {
            1 -> CitizenCaptureStep(viewModel)
            2 -> CitizenAnalyzingStep(viewModel)
            3 -> CitizenConfirmStep(viewModel, aiResult, notes)
            4 -> CitizenSuccessStep(viewModel, idStr, mergeDuo)
        }
    }
}

@Composable
fun CitizenCaptureStep(viewModel: MainViewModel) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<Bitmap?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) selectedPhoto = bitmap else Toast.makeText(context, "Camera cancelled. Tap camera to try again.", Toast.LENGTH_SHORT).show()
    }
    val galleryLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    if (bitmap != null) selectedPhoto = bitmap else Toast.makeText(context, "Could not decode image.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val voiceTranscript by viewModel.voiceAgentTranscript.collectAsState()
    val voicePrompt by viewModel.voiceAgentPrompt.collectAsState()
    val voiceListening by viewModel.voiceAgentListening.collectAsState()
    val voiceObjectId by viewModel.voiceObjectId.collectAsState()
    val speechAvailable = remember { android.speech.SpeechRecognizer.isRecognitionAvailable(context) }
    val speechRecognizer = remember(context) {
        if (speechAvailable) android.speech.SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { viewModel.setVoiceListening(true) }
            override fun onBeginningOfSpeech() { viewModel.setVoiceListening(true) }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { viewModel.setVoiceListening(false) }
            override fun onError(error: Int) {
                viewModel.setVoiceListening(false)
                Toast.makeText(context, "Voice capture paused. You can try again.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val spoken = results
                    ?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                viewModel.applyVoiceAgentTranscript(spoken, final = true)
                description = spoken
                viewModel.fetchDeviceLocation()
                cameraLauncher.launch()
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    description = partial
                    viewModel.applyVoiceAgentTranscript(partial)
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose { speechRecognizer?.destroy() }
    }

    fun startVoiceAgent() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "Speech recognition is not available on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Tell NagarNetra what issue you want to report")
        }
        viewModel.setVoiceListening(true)
        speechRecognizer.startListening(intent)
    }


    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { ReportProgressHeader(activeStep = 1) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = NNMint),
                border = BorderStroke(1.dp, NNDeepGreen.copy(alpha = 0.22f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.RecordVoiceOver, contentDescription = null, tint = NNDeepGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Voice Agent", color = NNInk, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        }
                        AssistChip(
                            onClick = {},
                            label = { Text(if (voiceListening) "Listening" else "Ready", fontSize = 11.sp) },
                            enabled = false
                        )
                    }
                    Text(voicePrompt, color = Color(0xFF40514D), fontSize = 12.sp, lineHeight = 16.sp)
                    if (voiceTranscript.isNotBlank()) {
                        Text(voiceTranscript, color = NNInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                    if (voiceObjectId.isNotBlank()) {
                        Text("Detected object ID: $voiceObjectId", color = NNDeepGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { if (voiceListening) speechRecognizer?.stopListening() else startVoiceAgent() },
                            enabled = speechAvailable,
                            colors = ButtonDefaults.buttonColors(containerColor = NNDeepGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(if (voiceListening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (voiceListening) "Stop" else "Speak")
                        }
                        Text("Camera opens automatically after your voice report.", color = Color(0xFF60736F), fontSize = 11.sp)
                    }
                }
            }
        }
        item {
            Text("Describe the Issue", fontWeight = FontWeight.ExtraBold, color = NNInk, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 250) description = it },
                placeholder = { Text("Please provide details about the issue...") },
                modifier = Modifier.fillMaxWidth().height(132.dp),
                shape = RoundedCornerShape(14.dp),
                maxLines = 5,
                supportingText = { Text("${description.length}/250", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NNDeepGreen, unfocusedBorderColor = NNLine, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )
        }
        item {
            Text("Add Photo (Required) *", fontWeight = FontWeight.ExtraBold, color = NNInk, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (selectedPhoto == null) 84.dp else 190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NNSoftSurface)
                    .border(1.dp, NNLine, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (selectedPhoto != null) {
                    Image(bitmap = selectedPhoto!!.asImageBitmap(), contentDescription = "Selected report photo", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    IconButton(onClick = { selectedPhoto = null }, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f))) {
                        Icon(Icons.Filled.Close, contentDescription = "Remove photo", tint = Color.White)
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, NNLine)) {
                            Icon(Icons.Filled.Collections, contentDescription = null, tint = NNDeepGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Upload", color = NNDeepGreen, fontWeight = FontWeight.Bold)
                        }
                        Button(onClick = { cameraLauncher.launch() }, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = NNDeepGreen)) {
                            Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Camera", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = {
                    if (selectedPhoto == null) {
                        Toast.makeText(context, "Please capture or upload a photo of the issue to proceed.", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.exactLocationNotes.value = description.ifBlank {
                            voiceTranscript.ifBlank { "Citizen uploaded photo for AI vision auto-detection." }
                        }
                        if (voiceTranscript.isNotBlank()) {
                            viewModel.applyVoiceAgentTranscript(voiceTranscript, final = true)
                        }
                        viewModel.setCapturedImage(selectedPhoto, null)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NNDeepGreen, contentColor = Color.White),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Next", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Icon(Icons.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun ReportProgressHeader(activeStep: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        listOf("Issue Details", "Location", "Submit").forEachIndexed { index, label ->
            val step = index + 1
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(if (step == activeStep) NNDeepGreen else Color(0xFFE9EFED)), contentAlignment = Alignment.Center) {
                    Text(step.toString(), color = if (step == activeStep) Color.White else Color(0xFF6C7974), fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(label, color = if (step == activeStep) NNDeepGreen else Color(0xFF6C7974), fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ReportCategoryCard(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(104.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) NNMint else Color.White),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) NNDeepGreen else NNLine)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = NNDeepGreen, modifier = Modifier.size(31.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(label, color = NNInk, fontWeight = FontWeight.Bold, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
        }
    }
}
@Composable
fun CitizenAnalyzingStep(viewModel: MainViewModel) {
    val photo by viewModel.capturedPhoto.collectAsState()
    val scanTransition = rememberInfiniteTransition(label = "scan")
    val laserY by scanTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    val statusTexts = remember {
        listOf(
            "🧠 Connecting to Gemini Pro Vision Node...",
            "📸 Scanning visual fracture and defect indices...",
            "📐 Computing defect texture and volumetric severity...",
            "📍 Calibrating localized GPS coordinate grid...",
            "🏢 Orchestrating municipal department SLA dispatch..."
        )
    }
    var currentTextIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(900)
            currentTextIndex = (currentTextIndex + 1) % statusTexts.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .size(150.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                if (photo != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = photo!!.asImageBitmap(),
                            contentDescription = "Scanning",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.85f
                        )
                        // Laser analyzer line
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val h = size.height
                            val w = size.width
                            val currentY = h * laserY
                            
                            // Laser light gradient glow
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFF10B981).copy(alpha = 0.25f), Color.Transparent),
                                    startY = currentY - 30.dp.toPx(),
                                    endY = currentY + 10.dp.toPx()
                                )
                            )
                            // Precise horizontal scanner bar
                            drawLine(
                                color = Color(0xFF10B981),
                                start = Offset(0f, currentY),
                                end = Offset(w, currentY),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                } else {
                    // High tech sonar sphere when preset values chosen
                    val scale by scanTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.3f,
                        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse),
                        label = "sonarScale"
                    )
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFF10B981), radius = (55 * scale).dp.toPx(), alpha = 0.15f)
                        drawCircle(Color(0xFF10B981), radius = (25 * scale).dp.toPx(), alpha = 0.35f)
                    }
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = "GEMINI COGNITIVE ANALYZER ACTIVE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(6.dp))
        
        LinearProgressIndicator(
            color = Color(0xFF10B981),
            trackColor = Color(0xFF10B981).copy(alpha = 0.15f),
            modifier = Modifier
                .width(180.dp)
                .height(4.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(20.dp))
        
        Crossfade(
            targetState = statusTexts[currentTextIndex],
            label = "status_anim"
        ) { text ->
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = CitizenTextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
fun CitizenConfirmStep(
    viewModel: MainViewModel,
    result: IssueAnalysisResult?,
    notes: String
) {
    if (result == null) return
    val photo by viewModel.capturedPhoto.collectAsState()
    val isMapDarkMode by viewModel.isMapDarkMode.collectAsState()

    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        revealed = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Step 2: AI Verified Analysis ✨", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Inspect computer-vision metadata prior to BBMP dispatch", fontSize = 11.sp, color = CitizenTextSecondary)

        Spacer(modifier = Modifier.height(12.dp))

        this@Column.AnimatedVisibility(
            visible = revealed,
            enter = slideInVertically(initialOffsetY = { 80 }) + fadeIn(animationSpec = tween(500)),
            exit = fadeOut()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (photo != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Black),
                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        Image(
                            bitmap = photo!!.asImageBitmap(),
                            contentDescription = "Captured civic issue image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                val agentContext by viewModel.agentContextState.collectAsState()

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CitizenCardBg)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("NagarNetra Multi-Agent Engine", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorSuccess)
                            }
                            Text(
                                text = "Confidence: ${result.confidence}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (result.confidence >= 80) ColorSuccess else ColorWarning
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        DetailRow(label = "Category", value = result.category, icon = Icons.Filled.Category)
                        DetailRow(label = "Severity Point", value = "${result.severity}/10", icon = Icons.Filled.Warning)
                        DetailRow(label = "Committed SLA", value = result.estimatedDays, icon = Icons.Filled.HourglassEmpty)
                        DetailRow(label = "Target Department", value = result.department, icon = Icons.Filled.Work)
                        DetailRow(label = "Visual Description", value = result.description, icon = Icons.Filled.Notes)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CitizenCardBg),
                    border = BorderStroke(1.dp, CitizenPrimary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Edit, contentDescription = null, tint = CitizenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify & Edit Report Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CitizenTextPrimary)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        OutlinedTextField(
                            value = result.reportTitle.ifBlank { buildLocalTitle(result.category, result.riskLevel) },
                            onValueChange = { newTitle ->
                                viewModel.aiAnalysisResult.value = result.copy(reportTitle = newTitle)
                            },
                            label = { Text("Report Title (AI-Generated, Editable)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CitizenPrimary,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        OutlinedTextField(
                            value = result.description,
                            onValueChange = { newDesc ->
                                viewModel.aiAnalysisResult.value = result.copy(description = newDesc)
                            },
                            label = { Text("Report Description (AI-Generated, Editable)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 6,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CitizenPrimary,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                if (agentContext?.requiresHumanVerification == true || result.confidence < 60) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)), // Warm Amber yellow
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Awaiting Human Review & Verification", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Reason: ${agentContext?.verificationReasons?.joinToString("; ") ?: "Confidence level below threshold (${result.confidence}%)"}",
                                fontSize = 10.sp,
                                color = Color(0xFFB45309),
                                fontWeight = FontWeight.Medium
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = Color(0xFFF59E0B).copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "Manually Select/Correct Issue Type:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            var expanded by remember { mutableStateOf(false) }
                            val claudeCategories = listOf(
                                "GARBAGE", "POTHOLE", "BROKEN_STREETLIGHT", "WATER_LEAK", "OPEN_DRAIN", 
                                "ENCROACHMENT", "DAMAGED_ROAD", "FALLEN_TREE", "ILLEGAL_DUMPING", 
                                "STAGNANT_WATER", "BROKEN_FOOTPATH", "GRAFFITI_VANDALISM", "DEAD_ANIMAL", 
                                "SEWAGE_OVERFLOW", "OTHER"
                            )
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.White,
                                        contentColor = Color(0xFF92400E)
                                    ),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = result.category,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = null,
                                            tint = Color(0xFF92400E)
                                        )
                                    }
                                }
                                
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .background(Color.White)
                                ) {
                                    claudeCategories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = cat,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color.Black
                                                )
                                            },
                                            onClick = {
                                                expanded = false
                                                val mapped = mapClaudeCategoryToStandard(cat)
                                                val route = LocalRoutingResolver.resolve(mapped, result.severity)
                                                viewModel.aiAnalysisResult.value = result.copy(
                                                    category = mapped,
                                                    department = route.department,
                                                    estimatedDays = "${route.expectedSlaDays} Days"
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                var showLogs by remember { mutableStateOf(false) }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CitizenCardBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLogs = !showLogs },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Dns,
                                    contentDescription = null,
                                    tint = CitizenTextPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Multi-Agent System Trace Logs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CitizenTextPrimary)
                            }
                            Icon(
                                imageVector = if (showLogs) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = null,
                                tint = CitizenTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (showLogs) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color.LightGray.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(6.dp))
                            agentContext?.traceLogs?.forEach { logLine ->
                                Text(
                                    text = logLine,
                                    fontSize = 10.sp,
                                    color = CitizenTextSecondary,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val context = LocalContext.current
                val currentLat by viewModel.reportLatitude.collectAsState()
                val currentLng by viewModel.reportLongitude.collectAsState()

                var latStr by remember { mutableStateOf(String.format("%.6f", currentLat)) }
                var lngStr by remember { mutableStateOf(String.format("%.6f", currentLng)) }

                LaunchedEffect(currentLat, currentLng) {
                    latStr = String.format("%.6f", currentLat)
                    lngStr = String.format("%.6f", currentLng)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CitizenCardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Filled.PinDrop, contentDescription = null, tint = CitizenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Geo-Location Parameters", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CitizenTextPrimary)
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(CitizenPrimary.copy(alpha = 0.08f))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GpsFixed,
                                    contentDescription = null,
                                    tint = CitizenPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Dual-Location: GPS captured on photo. Drag map or type below to override manually.",
                                    fontSize = 10.sp,
                                    color = CitizenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))

                        // Draggable Map Panel for live coordinates selection
                        android.util.Log.d("NagarNetraMap", "Rendering draggable GoogleMapsWebView at $currentLat, $currentLng")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        ) {
                            com.example.ui.GoogleMapsWebView(
                                mode = "draggable",
                                reportLatitude = currentLat,
                                reportLongitude = currentLng,
                                isDarkMode = isMapDarkMode,
                                onLocationUpdated = { lat, lng ->
                                    viewModel.reportLatitude.value = lat
                                    viewModel.reportLongitude.value = lng
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Autofetch button (for automatic positioning)
                        Button(
                            onClick = {
                                viewModel.fetchDeviceLocation()
                                Toast.makeText(context, "GPS autofetched exact coordinates!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CitizenPrimary.copy(alpha = 0.12f), contentColor = CitizenPrimary),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AUTOFETCH POSITION (AUTOMATIC GPS)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Modify coordinates manually below if needed:", fontSize = 10.sp, color = CitizenTextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = latStr,
                                onValueChange = {
                                    latStr = it
                                    it.toDoubleOrNull()?.let { dval ->
                                        viewModel.reportLatitude.value = dval
                                    }
                                },
                                label = { Text("Latitude", fontSize = 10.sp) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CitizenPrimary,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                )
                            )
                            OutlinedTextField(
                                value = lngStr,
                                onValueChange = {
                                    lngStr = it
                                    it.toDoubleOrNull()?.let { dval ->
                                        viewModel.reportLongitude.value = dval
                                    }
                                },
                                label = { Text("Longitude", fontSize = 10.sp) },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CitizenPrimary,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { viewModel.exactLocationNotes.value = it },
                            label = { Text("Landmark / Spot Name (Manually Entered)", fontSize = 11.sp) },
                            placeholder = { Text("e.g. Indiranagar 12th Main Road, near post office", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CitizenPrimary,
                                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.submitReport() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CitizenPrimary)
                ) {
                    Text("Confirm & Dispatched Ticket", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.resetReportingFlow() }) {
                    Text("Recapture Photo", color = CitizenTextSecondary)
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, icon: ImageVector) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = CitizenSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 10.sp, color = CitizenTextSecondary)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CitizenTextPrimary)
        }
    }
}

@Composable
fun CitizenSuccessStep(viewModel: MainViewModel, idStr: String, merged: Boolean) {
    var mapReady by remember { mutableStateOf(false) }
    val pinYOffset by animateFloatAsState(
        targetValue = if (mapReady) 0f else -130f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pinDropY"
    )
    val miniMapScale by animateFloatAsState(
        targetValue = if (mapReady) 1f else 0.75f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "mapScale"
    )
    val shadowAlpha by animateFloatAsState(
        targetValue = if (mapReady) 0.35f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "shadowAlpha"
    )

    val orchestratorTriggered by viewModel.orchestratorTriggered.collectAsState()

    LaunchedEffect(Unit) {
        delay(250)
        mapReady = true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Shimmering Confetti particles layered background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val parties = listOf(Color(0xFFFACC15), Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFE11D48), Color(0xFFA855F7))
            for (i in 0..15) {
                val x = size.width * ((i * 1.57f + 0.3f) % 1f)
                val y = size.height * ((i * 3.14f + 0.15f) % 0.85f)
                drawCircle(
                    color = parties[i % parties.size].copy(alpha = 0.35f),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Success icon badge
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(ColorSuccess.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(40.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = if (orchestratorTriggered) "ORCHESTRATOR CORRELATION LIVE! 🧠" else if (merged) "Similar Issue Merged! 🤝" else "Your Report is LIVE! 🚀",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ColorSuccess
            )
            
            Text(
                text = if (orchestratorTriggered) "OrchestratorBot (Agent 0) autonomously correlated 3 concentrated reports in Indiranagar. Consolidated tickets and dispatched directly to specialised agency. Standard 7-day SLA cycle bypassed!"
                else if (merged) "DuplicateBot merged this report into an existing active ticket. Upvoted nearby!"
                else "Computer Vision logged your coordinates into Ward 80 registry. +25 Civic Points earned!",
                fontSize = 12.sp,
                color = CitizenTextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- BOUNCY ZOOMING MINI MAP CONTROLLER (Design Fix 2) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .scale(miniMapScale),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.5.dp, ColorSuccess.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0xFFE2E8F0))) {
                    // Draw schematic streets
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawLine(Color.White, Offset(0f, h * 0.35f), Offset(w, h * 0.65f), 10.dp.toPx())
                        drawLine(Color.White, Offset(w * 0.45f, 0f), Offset(w * 0.55f, h), 10.dp.toPx())
                        
                        // Pulse ripple waves when pin lands
                        if (pinYOffset > -5f) {
                            drawCircle(ColorSuccess.copy(alpha = 0.12f), 55.dp.toPx(), Offset(w/2, h/2))
                            drawCircle(ColorSuccess.copy(alpha = 0.22f), 30.dp.toPx(), Offset(w/2, h/2))
                        }
                    }

                    // Shadow indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 16.dp)
                            .size(18.dp, 6.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = shadowAlpha))
                    )

                    // Flying / Dropping Pin
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = pinYOffset.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Drop Pin",
                            tint = CitizenPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    
                    // Small floating badge: "LIVE AT COORDINATES"
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("WARD 80 LIVE PATROL", color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Ticket Details UI Design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CitizenCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TICKET REFERENCE HEADER", fontSize = 9.sp, color = CitizenTextSecondary, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(idStr, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CitizenSecondary, fontFamily = FontFamily.Monospace)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.activeCitizenTab.value = "Status"
                        viewModel.resetReportingFlow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CitizenSecondary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Track Updates 📊", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        viewModel.activeCitizenTab.value = "Home"
                        viewModel.resetReportingFlow()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CitizenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text("Go to Home 🏠", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

// ------------------------------------------
// CITIZEN: SCREEN UPDATES TIMELINE
// ------------------------------------------
@Composable
fun CitizenUpdatesScreen(viewModel: MainViewModel) {
    val allIssues by viewModel.allIssues.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    val demoReports = remember { listOf(sampleCitizenIssue("NGR24568", "Garbage not collected\nin Green Park", "Garbage", "In Progress"), sampleCitizenIssue("NGR24569", "Street Light not\nworking in Sector 12", "Street Light", "Resolved"), sampleCitizenIssue("NGR24570", "Water leakage near\nShiv Mandir", "Water Leakage", "In Progress"), sampleCitizenIssue("NGR24571", "Road damaged near\nMarket Area", "Road Damage", "Rejected")) }
    val reports = remember(allIssues, selectedFilter) {
        val mine = allIssues.filter { it.isMyReport }.sortedByDescending { it.timePosted }.ifEmpty { demoReports }
        if (selectedFilter == "All") mine else mine.filter { it.status.equals(selectedFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "In Progress", "Resolved", "Rejected").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NNDeepGreen, selectedLabelColor = Color.White),
                        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selectedFilter == filter, borderColor = NNLine, selectedBorderColor = NNDeepGreen)
                    )
                }
            }
        }
        items(reports) { report ->
            MyReportListRow(report) {
                viewModel.selectedCitizenIssue.value = report
                viewModel.activeCitizenTab.value = "ReportDetails"
            }
        }
    }
}

fun sampleCitizenIssue(id: String, title: String, category: String, status: String): Issue = Issue(
    id = id,
    title = title,
    category = category,
    ward = "Ward 80",
    locationName = if (category == "Garbage") "Green Park, Sector 5" else "Indiranagar, Sector 12",
    latitude = 12.973,
    longitude = 77.635,
    severity = 6,
    description = "Garbage has not been collected for the past 3 days. It is causing bad smell and hygiene issues.",
    timePosted = System.currentTimeMillis() - 2L * 24L * 60L * 60L * 1000L,
    upvotes = 12,
    status = status,
    isMyReport = true,
    assignedDept = "BBMP"
)

@Composable
fun MyReportListRow(report: Issue, onClick: () -> Unit) {
    val bitmap = remember(report.photoBase64) { report.photoBase64?.toBitmap() }
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, NNLine)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(78.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE9F1EE)), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else CivicIssueIllustration(report.category, modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                StatusPill(report.status)
                Spacer(modifier = Modifier.height(6.dp))
                Text(if (report.status == "Resolved") "5 days ago" else if (report.status == "Rejected") "2 weeks ago" else "2 days ago", color = Color(0xFF7A8581), fontSize = 11.sp)
            }
            Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = Color(0xFF9CA6A2), modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
fun CitizenReportDetailsScreen(viewModel: MainViewModel) {
    val selected by viewModel.selectedCitizenIssue.collectAsState()
    val report = selected ?: sampleCitizenIssue("NGR24568", "Garbage not collected\nin Green Park", "Garbage", "In Progress")
    val bitmap = remember(report.photoBase64) { report.photoBase64?.toBitmap() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color.White),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFE9F1EE)), contentAlignment = Alignment.Center) {
                if (bitmap != null) Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else CivicIssueIllustration(report.category, modifier = Modifier.fillMaxSize())
            }
        }
        item {
            Row(verticalAlignment = Alignment.Top) {
                Text(report.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 16.sp, lineHeight = 20.sp)
                StatusPill(report.status)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLine("Report ID", "#${report.id}")
                DetailLine("Category", report.category)
                DetailLine("Location", report.locationName)
                DetailLine("Reported on", "25 May 2024, 10:30 AM")
            }
        }
        item {
            Divider(color = NNLine)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Description", fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(report.description, color = Color(0xFF3D4743), fontSize = 13.sp, lineHeight = 18.sp)
        }
        item {
            Text("Updates", fontWeight = FontWeight.ExtraBold, color = Color.Black, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            ReportTimelineItem(true, ColorSuccess, "25 May 2024, 10:30 AM", "Issue reported")
            ReportTimelineItem(false, Color(0xFF2583E9), "26 May 2024, 09:15 AM", "Assigned to Municipal Team")
        }
    }
}

@Composable
fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color(0xFF6F7A76), fontSize = 12.sp)
        Text(value, color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ReportTimelineItem(showLine: Boolean, color: Color, time: String, label: String) {
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
            if (showLine) Box(modifier = Modifier.width(2.dp).height(38.dp).background(color.copy(alpha = 0.35f)))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(time, color = Color(0xFF5E6864), fontSize = 12.sp)
            Text(label, color = Color.Black, fontSize = 13.sp)
        }
    }
}

@Composable
fun CivicIssueIllustration(category: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFFE6EEE9)), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Color(0xFFCAD7D1), Offset(0f, h * 0.62f), androidx.compose.ui.geometry.Size(w, h * 0.38f))
            repeat(18) { i ->
                drawCircle(Color(0xFF6A5A50).copy(alpha = 0.75f), radius = (2 + i % 4).dp.toPx(), center = Offset((i * 37 % w.toInt()).toFloat(), h * 0.68f + (i % 5) * 9.dp.toPx()))
            }
            drawRect(Color(0xFF263B38), Offset(w * 0.60f, h * 0.28f), androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.35f))
            drawRect(Color(0xFF39534F), Offset(w * 0.76f, h * 0.32f), androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.31f))
            drawCircle(Color(0xFF82938D), radius = 17.dp.toPx(), center = Offset(w * 0.42f, h * 0.58f))
        }
        Icon(categoryIcon(category), contentDescription = null, tint = NNDeepGreen.copy(alpha = 0.82f), modifier = Modifier.size(42.dp))
    }
}

@Composable
fun CitizenCommunityChatScreen(viewModel: MainViewModel) {
    val activeRoomId by viewModel.activeCommunityRoomId.collectAsState()
    val chatMessages by viewModel.communityChatMessages.collectAsState()
    val allIssues by viewModel.allIssues.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val user = currentUser ?: com.example.data.User(id = "sandbox_user", email = "sandbox@nagarnetra.in")
    val isAppDarkMode by viewModel.isAppDarkMode.collectAsState()
    val isDark = isAppDarkMode ?: isSystemInDarkTheme()

    var messageText by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<Bitmap?>(null) }
    
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedPhoto = bitmap
        }
    }

    val context = LocalContext.current
    val joinedRooms by viewModel.joinedRoomsList.collectAsState()
    var showJoinLinkDialog by remember { mutableStateOf(false) }

    // Resolve room titles
    val roomTitle = when {
        activeRoomId == "nearby" -> "Nearby Wards"
        activeRoomId == "emergency" -> "Emergency Channel ⚠️"
        activeRoomId.startsWith("ward_") -> {
            val num = activeRoomId.removePrefix("ward_")
            "Ward $num Chat"
        }
        activeRoomId.startsWith("issue_") -> {
            val issueId = activeRoomId.removePrefix("issue_")
            val issue = allIssues.firstOrNull { it.id == issueId }
            "Thread: ${issue?.title ?: issueId}"
        }
        else -> "General Chat"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Chat Header (Flat, compact styling with share button)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$roomTitle 🏛️",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CitizenPrimary
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "Coordinate directly with neighbors and city officers about local issues.",
                    fontSize = 11.sp,
                    color = CitizenTextSecondary
                )
            }
            if (activeRoomId.startsWith("ward_")) {
                IconButton(
                    onClick = {
                        val clip = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData.newPlainText("NagarNetra Chat Room Invite", "https://nagarnetra.ai/join/chat/$activeRoomId")
                        clip.setPrimaryClip(clipData)
                        Toast.makeText(context, "Invite link copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = CitizenPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Room Navigation Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ward Chat Chip
            val userWardNum = viewModel.getRoomIdForWard(user.ward).removePrefix("ward_")
            FilterChip(
                selected = activeRoomId == "ward_$userWardNum",
                onClick = { viewModel.activeCommunityRoomId.value = "ward_$userWardNum" },
                label = { Text("Ward $userWardNum Chat", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Filled.Home, null, modifier = Modifier.size(14.dp)) }
            )

            // Nearby Chat Chip
            FilterChip(
                selected = activeRoomId == "nearby",
                onClick = { viewModel.activeCommunityRoomId.value = "nearby" },
                label = { Text("Nearby Wards", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Filled.Group, null, modifier = Modifier.size(14.dp)) }
            )

            // Emergency Chat Chip
            FilterChip(
                selected = activeRoomId == "emergency",
                onClick = { viewModel.activeCommunityRoomId.value = "emergency" },
                label = { Text("Emergency Alerts ⚠️", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Filled.Warning, null, modifier = Modifier.size(14.dp)) }
            )

            // Issue Threads Chat Chip
            FilterChip(
                selected = activeRoomId == "issues" || activeRoomId.startsWith("issue_"),
                onClick = { viewModel.activeCommunityRoomId.value = "issues" },
                label = { Text("Issue Threads", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Filled.QuestionAnswer, null, modifier = Modifier.size(14.dp)) }
            )

            // Joined Rooms from Invite Links
            joinedRooms.forEach { joinedRoomId ->
                val num = joinedRoomId.removePrefix("ward_")
                FilterChip(
                    selected = activeRoomId == joinedRoomId,
                    onClick = { viewModel.activeCommunityRoomId.value = joinedRoomId },
                    label = { Text("Ward $num Chat", fontSize = 11.sp) },
                    leadingIcon = { Icon(Icons.Filled.Home, null, modifier = Modifier.size(14.dp)) }
                )
            }

            // Join Room button (+)
            IconButton(
                onClick = { showJoinLinkDialog = true },
                modifier = Modifier
                    .size(32.dp)
                    .background(CitizenPrimary.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(Icons.Filled.Add, null, tint = CitizenPrimary, modifier = Modifier.size(16.dp))
            }
        }

        if (showJoinLinkDialog) {
            var inviteText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showJoinLinkDialog = false },
                title = { Text("Join Chatroom", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Enter invite link or ward number to join a community chatroom outside your ward.", fontSize = 12.sp, color = Color.Gray)
                        OutlinedTextField(
                            value = inviteText,
                            onValueChange = { inviteText = it },
                            placeholder = { Text("e.g. ward_45 or https://nagarnetra.ai/join/chat/ward_45") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (inviteText.isNotBlank()) {
                                viewModel.joinRoomByInviteLink(inviteText)
                            }
                            showJoinLinkDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NNDeepGreen)
                    ) {
                        Text("Join", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJoinLinkDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }

        Divider(color = Color.LightGray.copy(alpha = 0.2f))

        if (activeRoomId == "issues") {
            // Show Issue Threads Selection List
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                Text("Select an issue thread to join the chat:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CitizenTextPrimary)
                Spacer(modifier = Modifier.height(10.dp))
                if (allIssues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues available to chat about.", color = CitizenTextSecondary)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(allIssues) { issue ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.activeCommunityRoomId.value = "issue_${issue.id}"
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (issue.category.lowercase()) {
                                            "pothole" -> Icons.Filled.Warning
                                            "water" -> Icons.Filled.WaterDrop
                                            "lights" -> Icons.Filled.Lightbulb
                                            "garbage" -> Icons.Filled.Delete
                                            "sewage" -> Icons.Filled.Warning
                                            else -> Icons.Filled.Build
                                        },
                                        contentDescription = null,
                                        tint = CitizenPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(issue.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CitizenTextPrimary)
                                        Text("Ref: ${issue.id} • ${issue.ward} • Severity: ${issue.severity}", fontSize = 11.sp, color = CitizenTextSecondary)
                                    }
                                    Icon(Icons.Filled.ArrowForwardIos, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Active Chat Room Panel
            Column(modifier = Modifier.weight(1f)) {
                // Emergency Channel Banner
                if (activeRoomId == "emergency") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorDanger.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        border = BorderStroke(1.dp, ColorDanger.copy(alpha = 0.4f))
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Campaign, null, tint = ColorDanger, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "CRITICAL MUNICIPAL ALERTS â€” READ ONLY",
                                fontWeight = FontWeight.Bold,
                                color = ColorDanger,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Messages list
                val listState = rememberLazyListState()
                val pinnedMessages = chatMessages.filter { it.isSystem }
                val regularMessages = chatMessages
                    .filterNot { it.isSystem }
                    .sortedBy { it.timestamp }
                val totalRenderedItems = regularMessages.size + if (pinnedMessages.isNotEmpty()) 1 else 0
                
                LaunchedEffect(totalRenderedItems) {
                    if (totalRenderedItems > 0) {
                        listState.animateScrollToItem(totalRenderedItems - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    // Pinned AI messages at the top of scroll list
                    if (pinnedMessages.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ColorWarning.copy(alpha = 0.08f))
                                    .border(1.dp, ColorWarning.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.PushPin, null, tint = ColorWarning, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PINNED AI ALERTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ColorWarning, letterSpacing = 1.sp)
                                }
                                pinnedMessages.forEach { msg ->
                                    Text(
                                        text = msg.text,
                                        fontSize = 11.sp,
                                        color = CitizenTextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Regular messages
                    items(regularMessages) { message ->
                        val isSelf = message.senderId == user.id
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
                        ) {
                            if (!isSelf) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (message.isSystem) ColorWarning.copy(alpha = 0.2f)
                                            else CitizenPrimary.copy(alpha = 0.15f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = message.senderName.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = if (message.isSystem) ColorWarning else CitizenPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column(
                                horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                // Sender header details
                                if (!isSelf) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = message.senderName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = CitizenTextPrimary
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        
                                        // Trust score / Role badge
                                        val isMod = message.senderTrustScore > 80
                                        Text(
                                            text = if (isMod) "⚡ Mod (${message.senderTrustScore}%)" else "★ ${message.senderTrustScore}%",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMod) ColorSuccess else CitizenTextSecondary,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isMod) ColorSuccess.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Message Bubble
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isSelf -> CitizenPrimary
                                            message.isSystem -> ColorWarning.copy(alpha = 0.12f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isSelf) 12.dp else 2.dp,
                                        bottomEnd = if (isSelf) 2.dp else 12.dp
                                    ),
                                    border = if (isSelf || message.isSystem) null else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        // Render Image Attachment
                                        val bitmap = remember(message.photoBase64) {
                                            if (!message.photoBase64.isNullOrBlank()) {
                                                try {
                                                    val decodedBytes = android.util.Base64.decode(message.photoBase64, android.util.Base64.DEFAULT)
                                                    BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                                } catch (e: Exception) {
                                                    null
                                                }
                                            } else {
                                                null
                                            }
                                        }

                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Chat attachment",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(140.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .padding(bottom = 6.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Render Text
                                        if (message.isHiddenByAi) {
                                            Text(
                                                text = "[This message was hidden by NagarNetra AI for violating community guidelines]",
                                                color = if (isSelf) Color.LightGray else Color.Gray,
                                                fontSize = 11.sp,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        } else {
                                            Text(
                                                text = message.text,
                                                color = if (isSelf) Color.White else CitizenTextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                // Message Footer
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = android.text.format.DateUtils.getRelativeTimeSpanString(message.timestamp).toString(),
                                        fontSize = 9.sp,
                                        color = CitizenTextSecondary
                                    )

                                    if (!message.isHiddenByAi) {
                                        // Upvote Click
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clickable { viewModel.upvoteChatMessage(message) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ThumbUp,
                                                contentDescription = "Upvote",
                                                tint = CitizenPrimary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                text = "${message.upvotes}",
                                                fontSize = 9.sp,
                                                color = CitizenTextSecondary
                                            )
                                        }

                                        // Reaction Emojis
                                        listOf("👍", "❤️", "😮", "🙏").forEach { emoji ->
                                            val count = remember(message.reactionsJson) {
                                                try {
                                                    org.json.JSONObject(message.reactionsJson).optInt(emoji, 0)
                                                } catch (e: Exception) {
                                                    0
                                                }
                                            }
                                            Text(
                                                text = if (count > 0) "$emoji $count" else emoji,
                                                fontSize = 10.sp,
                                                modifier = Modifier
                                                    .clickable { viewModel.reactToChatMessage(message, emoji) }
                                                    .padding(2.dp)
                                            )
                                        }

                                        // Moderate Button (visible if user has trust score > 80 & is not self or bot)
                                        if (user.trustScore > 80 && !isSelf && message.senderId != "system_bot") {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clickable { viewModel.moderateMessage(message.id) }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Block,
                                                    contentDescription = "Hide message",
                                                    tint = ColorDanger,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "Hide",
                                                    fontSize = 9.sp,
                                                    color = ColorDanger,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isSelf) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CitizenPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = user.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = CitizenPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Input Area
                val isReadOnly = activeRoomId == "emergency" && viewModel.currentRole.value != "authority"
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(0.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Photo preview if attached
                        if (selectedPhoto != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        bitmap = selectedPhoto!!.asImageBitmap(),
                                        contentDescription = "Attached photo preview",
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Photo attached (Scanned in real-time)", fontSize = 11.sp, color = ColorSuccess)
                                }
                                IconButton(
                                    onClick = { selectedPhoto = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                placeholder = {
                                    Text(
                                        text = if (isReadOnly) "Only city authorities can broadcast here"
                                               else "Message $roomTitle...",
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = if (!isReadOnly) {
                                    {
                                        IconButton(
                                            onClick = { photoLauncher.launch(null) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoCamera,
                                                contentDescription = "Attach photo",
                                                tint = CitizenPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                } else null,
                                enabled = !isReadOnly,
                                modifier = Modifier.weight(1f),
                                maxLines = 3,
                                shape = RoundedCornerShape(20.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    unfocusedContainerColor = MaterialTheme.colorScheme.background,
                                    focusedContainerColor = MaterialTheme.colorScheme.background,
                                    disabledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                                )
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    if (messageText.isNotBlank() || selectedPhoto != null) {
                                        viewModel.sendCommunityChatMessage(messageText, attachedPhoto = selectedPhoto)
                                        messageText = ""
                                        selectedPhoto = null
                                    }
                                },
                                enabled = !isReadOnly && (messageText.isNotBlank() || selectedPhoto != null),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = CitizenPrimary,
                                    disabledContainerColor = Color.LightGray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.Send, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// CITIZEN: SCREEN PROFILE
// ------------------------------------------
@Composable
fun CitizenProfileScreen(viewModel: MainViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val showEditDialog by viewModel.showProfileEditDialog.collectAsState()

    val profile = currentUser ?: com.example.data.User(
        id = "citizen_user",
        email = "raj.sharma@email.com",
        name = "Raj Sharma",
        ward = "Indiranagar (Ward 80)",
        trustScore = 88,
        points = 150,
        level = 4,
        rank = 14,
        badgesCount = 5,
        reportsCount = 3
    )

    if (showEditDialog) {
        var editName by remember { mutableStateOf(profile.name) }
        var editEmail by remember { mutableStateOf(profile.email) }
        var editWard by remember { mutableStateOf(profile.ward) }

        AlertDialog(
            onDismissRequest = { viewModel.showProfileEditDialog.value = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWard,
                        onValueChange = { editWard = it },
                        label = { Text("Ward") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserProfile(editName, editEmail, editWard)
                        viewModel.showProfileEditDialog.value = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NNDeepGreen)
                ) {
                    Text("Save", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showProfileEditDialog.value = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF4F7F5)),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF0D5C4A).copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D5C4A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.name.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile.name.ifBlank { "Raj Sharma" },
                        color = Color(0xFF111827),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = profile.email.ifBlank { "raj.sharma@email.com" },
                        color = Color(0xFF6B7280),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        color = Color(0xFFE5F2EC),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = profile.ward,
                            color = Color(0xFF0D5C4A),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatsBlockWidget(
                    title = "Trust Score",
                    value = "${profile.trustScore}%",
                    sub = "Excellent",
                    modifier = Modifier.weight(1f)
                )
                StatsBlockWidget(
                    title = "Points Balance",
                    value = "${profile.points}",
                    sub = "XP Points",
                    modifier = Modifier.weight(1f)
                )
                StatsBlockWidget(
                    title = "City Rank",
                    value = "#${profile.rank}",
                    sub = "Top 5%",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF005A44).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Citizen Badges",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BadgeChip(title = "Pothole Patrol", icon = Icons.Filled.Warning, color = Color(0xFFF59E0B))
                        BadgeChip(title = "Green Crusader", icon = Icons.Filled.Star, color = Color(0xFF10B981))
                        BadgeChip(title = "Community Voice", icon = Icons.Filled.Chat, color = Color(0xFF3B82F6))
                        BadgeChip(title = "Alpha Reporter", icon = Icons.Filled.PhotoCamera, color = Color(0xFF8B5CF6))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF0D5C4A).copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Redeem Civic Rewards",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF111827)
                    )
                    Text(
                        text = "Use your citizen points to claim rewards from our local partners.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    val rewards = listOf(
                        Triple("Metro Transit Smartcard Pass (10 Rides)", 120, "BMTC & Namma Metro"),
                        Triple("Eco Green Home Planter & Seeds", 80, "Green City Initiative"),
                        Triple("Premium Public Parking Coupon", 50, "BBMP Parking Facility")
                    )

                    rewards.forEachIndexed { index, reward ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reward.first, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                Text(reward.third, fontSize = 10.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = {
                                    if (profile.points >= reward.second) {
                                        viewModel.redeemPoints(reward.second, reward.first)
                                    } else {
                                        viewModel.showToast("Insufficient points. Report issues and participate in chat to earn more!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (profile.points >= reward.second) Color(0xFF0D5C4A) else Color.LightGray
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp)
                             ) {
                                Text("${reward.second} pts", fontSize = 11.sp, color = Color.White)
                            }
                        }
                        if (index < rewards.size - 1) {
                            Divider(color = Color(0xFFF3F4F6), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFF0D5C4A).copy(alpha = 0.1f))
            ) {
                Column {
                    ProfileMenuRow("Edit Profile", Icons.Filled.Edit) { viewModel.showProfileEditDialog.value = true }
                    ProfileDivider()
                    ProfileMenuRow("My Reports", Icons.Filled.ReceiptLong) { viewModel.activeCitizenTab.value = "Reports" }
                    ProfileDivider()
                    ProfileMenuRow("Notifications", Icons.Filled.Notifications) { viewModel.activeCitizenTab.value = "Updates" }
                    ProfileDivider()
                    ProfileMenuRow("Sign Out", Icons.Filled.ExitToApp) { viewModel.logout() }
                }
            }
        }
    }
}

@Composable
fun ProfileMenuRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF61706B), modifier = Modifier.size(21.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = Color(0xFF202A27), fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ArrowForwardIos, contentDescription = null, tint = Color(0xFF9AA5A1), modifier = Modifier.size(14.dp))
    }
}

@Composable
fun ProfileDivider() {
    Divider(color = NNLine, modifier = Modifier.padding(start = 49.dp))
}
@Composable
fun StatsBlockWidget(title: String, value: String, sub: String, modifier: Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 9.sp, color = CitizenTextSecondary)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = CitizenPrimary)
            Text(sub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BadgeChip(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
fun RewardHistoryRow(item: String, date: String, cost: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(item, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(date, fontSize = 9.sp, color = CitizenTextSecondary)
        }
        Text(cost, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorDanger)
    }
}

@Composable
fun PointsRuleRow(label: String, points: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = CitizenTextSecondary)
        Text(points, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ColorSuccess)
    }
}

// ------------------------------------------
// CITIZEN: CHATBOT SLIDE-UP
// ------------------------------------------
@Composable
fun CivicChatBotPanel(viewModel: MainViewModel) {
    val messages by viewModel.chatMessages.collectAsState()
    var textInput by remember { mutableStateOf("") }
    var voiceListening by remember { mutableStateOf(false) }
    var voiceReplyEnabled by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.chatOpen.value = false
            viewModel.setCapturedImage(bitmap, null)
        } else {
            Toast.makeText(context, "Camera cancelled. Tell me again when you are ready.", Toast.LENGTH_SHORT).show()
        }
    }

    val speechAvailable = remember { android.speech.SpeechRecognizer.isRecognitionAvailable(context) }
    val speechRecognizer = remember(context) {
        if (speechAvailable) android.speech.SpeechRecognizer.createSpeechRecognizer(context) else null
    }

    val tts = remember(context) { android.speech.tts.TextToSpeech(context) {} }
    DisposableEffect(tts) {
        tts.language = java.util.Locale("en", "IN")
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.assistantCameraRequests.collect {
            cameraLauncher.launch()
        }
    }

    LaunchedEffect(voiceReplyEnabled) {
        viewModel.assistantSpokenReplies.collect { reply ->
            if (voiceReplyEnabled) {
                tts.speak(reply.take(500), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "nagarnetra-reply-${System.currentTimeMillis()}")
            }
        }
    }

    DisposableEffect(speechRecognizer) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { voiceListening = true }
            override fun onBeginningOfSpeech() { voiceListening = true }
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { voiceListening = false }
            override fun onError(error: Int) {
                voiceListening = false
                Toast.makeText(context, "I could not hear that clearly. Try again.", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                voiceListening = false
                val spoken = results
                    ?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (spoken.isNotBlank()) {
                    textInput = ""
                    viewModel.sendChatMessage(spoken)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) textInput = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer?.setRecognitionListener(listener)
        onDispose { speechRecognizer?.destroy() }
    }

    fun startAssistantVoice() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            Toast.makeText(context, "Voice recognition is not available on this device.", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
            putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Ask a civic question or say you want to report an issue")
        }
        voiceListening = true
        recognizer.startListening(intent)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = CitizenCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        border = BorderStroke(1.dp, CitizenSecondary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "NagarNetra Voice Civic Agent",
                            color = MaterialTheme.colorScheme.onSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Answers questions, speaks back, and files reports by camera.",
                            color = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { voiceReplyEnabled = !voiceReplyEnabled }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (voiceReplyEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                        contentDescription = "Toggle voice replies",
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
                IconButton(onClick = { viewModel.chatOpen.value = false }, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages) { msg ->
                    ChatBubbleItem(msg)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CitizenCardBg)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (voiceListening) speechRecognizer?.stopListening() else startAssistantVoice() },
                    enabled = speechAvailable,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (voiceListening) ColorDanger else CitizenSecondary)
                ) {
                    Icon(imageVector = if (voiceListening) Icons.Filled.Stop else Icons.Filled.Mic, contentDescription = "Speak", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask anything, or say: I want to report a water leak", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        viewModel.sendChatMessage(textInput)
                        textInput = ""
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CitizenPrimary)
                ) {
                    Icon(imageVector = Icons.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ==========================================
// EXPERIENCE 2 - AUTHORITY CONTROL ROOM PANEL
// ==========================================
@Composable
fun AuthorityPanelLayout(viewModel: MainViewModel) {
    val activePage by viewModel.activeAuthorityPage.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AuthorityCardBg,
                tonalElevation = 8.dp
            ) {
                val menus = listOf(
                    Pair("Overview", Icons.Filled.Dashboard),
                    Pair("Priority", Icons.Filled.PriorityHigh),
                    Pair("Web Console", Icons.Filled.Campaign),
                    Pair("Accountability", Icons.Filled.Email),
                    Pair("Analytics", Icons.Filled.Analytics),
                    Pair("Ward Reports", Icons.Filled.Assignment)
                )

                menus.forEach { (name, icon) ->
                    val isSelected = activePage == name
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.activeAuthorityPage.value = name },
                        icon = { Icon(imageVector = icon, contentDescription = name, tint = if (isSelected) AuthorityPrimary else Color.LightGray) },
                        label = { Text(name, fontSize = 8.sp, color = if (isSelected) AuthorityPrimary else Color.LightGray) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(AuthorityBackground)
        ) {
            when (activePage) {
                "Overview" -> AuthorityOverviewPage(viewModel)
                "Priority" -> AuthorityPriorityIssuesPage(viewModel)
                "Web Console" -> AuthorityWebConsolePage(viewModel)
                "Accountability" -> AutomatedAccountabilityEnginePage(viewModel)
                "Analytics" -> AuthorityAnalyticsPage(viewModel)
                "Ward Reports" -> AuthorityWardReportsPage(viewModel)
            }
        }
    }
}

@Composable
fun AuthorityControlHeader(title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ColorSuccess)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE GOVERNMENT COMMAND MATRIX // PORTAL DISPATCHED",
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ColorSuccess,
                        letterSpacing = 0.8.sp
                    )
                }
                Text(
                    text = "UTC 42ms // OK",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.LightGray
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(4.dp))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = AuthorityText)
            Text(subtitle, fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun SciFiRadarVisual() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "radarSweep"
    )
    val radarPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "radarPulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AuthorityCardBg),
        border = BorderStroke(1.dp, AuthorityPrimary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("NEURAL INTERCEPT RADAR FEED", color = AuthorityText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("MODE: AUTONOMOUS SCAN", color = ColorSuccess, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2
                    val cy = h / 2
                    
                    // Outer rings
                    drawCircle(Color(0xFF10B981).copy(alpha = 0.15f), radius = w / 2, style = Stroke(1.dp.toPx()))
                    drawCircle(Color(0xFF10B981).copy(alpha = 0.15f), radius = w * 0.35f, style = Stroke(1.dp.toPx()))
                    drawCircle(Color(0xFF10B981).copy(alpha = 0.15f), radius = w * 0.15f, style = Stroke(1.dp.toPx()))
                    
                    // Crosshair axis
                    drawLine(Color(0xFF10B981).copy(alpha = 0.2f), Offset(0f, cy), Offset(w, cy), 1.dp.toPx())
                    drawLine(Color(0xFF10B981).copy(alpha = 0.2f), Offset(cx, 0f), Offset(cx, h), 1.dp.toPx())
                    
                    // Sweeper radial angle
                    val angleRad = Math.toRadians(rotation.toDouble())
                    val endX = cx + (w / 2) * Math.cos(angleRad).toFloat()
                    val endY = cy + (h / 2) * Math.sin(angleRad).toFloat()
                    
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color.Transparent, Color(0xFF10B981).copy(alpha = 0.3f), Color.Transparent),
                            center = Offset(cx, cy)
                        ),
                        startAngle = rotation - 45f,
                        sweepAngle = 45f,
                        useCenter = true
                    )
                    
                    drawLine(Color(0xFF10B981).copy(alpha = 0.75f), Offset(cx, cy), Offset(endX, endY), 2.dp.toPx())
                    
                    // Blink active simulated target coordinate blips
                    drawCircle(ColorDanger.copy(alpha = radarPulse), radius = 5.dp.toPx(), center = Offset(cx + 35.dp.toPx(), cy - 25.dp.toPx()))
                    drawCircle(ColorWarning.copy(alpha = radarPulse * 0.6f), radius = 4.dp.toPx(), center = Offset(cx - 30.dp.toPx(), cy + 20.dp.toPx()))
                }
                
                Text(
                    text = "RADAR FEED ACTIVE",
                    color = Color(0xFF10B981).copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 80.dp)
                )
            }
        }
    }
}

@Composable
fun AuthorityOverviewPage(viewModel: MainViewModel) {
    val logs by viewModel.authorityLogs.collectAsState()
    val allIssues by viewModel.allIssues.collectAsState()

    val totalActive = allIssues.filter { it.status != "Resolved" }.size
    val criticalCount = allIssues.filter { it.severity >= 8 && it.status != "Resolved" }.size
    val resolvedToday = allIssues.filter { it.status == "Resolved" }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AuthorityControlHeader(
                title = "Operational Telemetry Room",
                subtitle = "Real-time telemetry and automated civic dispatch queues"
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStatsCard(title = "Total Active", value = "$totalActive", icon = Icons.Filled.List, color = AuthorityPrimary, modifier = Modifier.weight(1f))
                MiniStatsCard(title = "Critical High", value = "$criticalCount", icon = Icons.Filled.CrisisAlert, color = ColorDanger, modifier = Modifier.weight(1f))
                MiniStatsCard(title = "Resolved Claims", value = "$resolvedToday", icon = Icons.Filled.DoneAll, color = ColorSuccess, modifier = Modifier.weight(1f))
            }
        }

        item {
            Text("OrchestratorBot Background Trace (Live JVM Thread)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black),
                border = BorderStroke(1.dp, ColorSuccess.copy(alpha = 0.5f))
            ) {
                LazyColumn(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(logs) { log ->
                        Text("[BOT] ${log.message}", color = ColorSuccess, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                    }
                }
            }
        }

        item {
            Text("Active SLA Escaped Warnings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
        }

        val breaches = allIssues.filter { it.slaStatus == "SLA BREACHED" }
        if (breaches.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Text("No active SLA breaches recorded.", color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        } else {
            items(breaches) { issue ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ColorDanger.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, ColorDanger)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = ColorDanger)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("SLA REACHED CRITICAL BREACH - [${issue.id}]", fontSize = 11.sp, color = ColorDanger, fontWeight = FontWeight.Bold)
                            Text("Target department ${issue.assignedDept} failed resolution deadlines.", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorityPriorityIssuesPage(viewModel: MainViewModel) {
    val allIssues by viewModel.allIssues.collectAsState()
    var selectedToResolve by remember { mutableStateOf<Issue?>(null) }

    val sorted = remember(allIssues) {
        allIssues.sortedByDescending { viewModel.calculatePriorityScore(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                AuthorityControlHeader(
                    title = "Priority Dispatch Queue",
                    subtitle = "Automated AI neural prioritization & department allocation"
                )
            }

            items(sorted) { issue ->
                val priorityScore = viewModel.calculatePriorityScore(issue)
                val color = if (priorityScore >= 80) ColorDanger else ColorWarning

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("[${issue.id}] - Score: $priorityScore", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(issue.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.DarkGray)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(issue.assignedDept, color = Color.White, fontSize = 10.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (issue.requiresReview) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFEF3C7))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Awaiting Verification: ${issue.verificationReasons}",
                                    color = Color(0xFF92400E),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(issue.description, fontSize = 11.sp, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Status: ${issue.status}", color = AuthorityPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            if (issue.status != "Resolved") {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { viewModel.updateIssueStatus(issue.id, "In Progress") },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuthorityAccent),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Dispatch", fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { selectedToResolve = issue },
                                        colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Text("Resolve", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedToResolve?.let { issue ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)
                ) {
                    var noteText by remember { mutableStateOf("") }
                    val isVerifying by viewModel.resolutionVerifying.collectAsState()

                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Certify Ticket Closed: [${issue.id}]", color = AuthorityText, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("VisionBot will perform asphalt validation scan on image prior to resolving.", fontSize = 10.sp, color = Color.LightGray)

                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            placeholder = { Text("Enter engineering action logs...", fontSize = 11.sp, color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color.White)
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        if (isVerifying) {
                            CircularProgressIndicator(color = AuthorityPrimary)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { selectedToResolve = null }) {
                                    Text("Abort", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        viewModel.resolveIssueWithProof(
                                            issue.id,
                                            if (noteText.isEmpty()) "Asphalt matches standard PPWD specification indices." else noteText,
                                            null
                                        )
                                        // Delay closure simulated
                                        selectedToResolve = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess)
                                ) {
                                    Text("Verify & Resolve")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuthorityAnalyticsPage(viewModel: MainViewModel) {
    val scroll = rememberScrollState()
    val primaryColor = CitizenPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AuthorityControlHeader(
            title = "Tactical Analytics Room",
            subtitle = "District distribution modeling & performance forecasting metrics"
        )

        // Spectacular Sweep Radar Feed Panel
        SciFiRadarVisual()

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Category Resolution Distribution", color = AuthorityText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(
                        modifier = Modifier
                            .size(95.dp)
                            .drawBehind {
                                val stroke = 12.dp.toPx()
                                drawArc(color = primaryColor, startAngle = 0f, sweepAngle = 144f, useCenter = false, style = Stroke(stroke))
                                drawArc(color = AuthorityAccent, startAngle = 144f, sweepAngle = 108f, useCenter = false, style = Stroke(stroke))
                                drawArc(color = ColorSuccess, startAngle = 252f, sweepAngle = 108f, useCenter = false, style = Stroke(stroke))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("28 Total", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("ACTIVES", color = Color.Gray, fontSize = 7.sp)
                        }
                    }

                    Column {
                        ChartKey(name = "Tarmac / Potholes (40%)", indicatorColor = primaryColor)
                        ChartKey(name = "Aqueous leaks (30%)", indicatorColor = AuthorityAccent)
                        ChartKey(name = "Garbage piles (30%)", indicatorColor = ColorSuccess)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Weekly SLA Efficiency Coefficient", color = AuthorityText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        pathEffect = pathEffect
                    )

                    val nodes = listOf(
                        Offset(size.width * 0.1f, size.height * 0.7f),
                        Offset(size.width * 0.3f, size.height * 0.6f),
                        Offset(size.width * 0.5f, size.height * 0.4f),
                        Offset(size.width * 0.7f, size.height * 0.2f),
                        Offset(size.width * 0.9f, size.height * 0.15f)
                    )

                    for (i in 0 until nodes.size - 1) {
                        drawLine(color = ColorSuccess, start = nodes[i], end = nodes[i + 1], strokeWidth = 6f)
                        drawCircle(color = ColorSuccess, radius = 8f, center = nodes[i])
                    }
                    drawCircle(color = ColorSuccess, radius = 8f, center = nodes.last())
                }
            }
        }
    }
}

@Composable
fun AuthorityWardReportsPage(viewModel: MainViewModel) {
    val compilingReport by viewModel.isCompilingReport.collectAsState()
    val compiledReportText by viewModel.compiledReportText.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AuthorityControlHeader(
                title = "Ward Reporting Dashboard",
                subtitle = "Compile district data and log logistical operations reports"
            )
        }

        // --- NEW: Custom ML Trainer Studio Card ---
        item {
            val isTraining by viewModel.isTrainingModel.collectAsState()
            val trainingProg by viewModel.trainingProgress.collectAsState()
            val statusStr by viewModel.modelStatus.collectAsState()
            val accuracyVal by viewModel.modelAccuracy.collectAsState()
            val logsList by viewModel.trainingLogs.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuthorityCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NagarNetra AI ML Trainer Studio",
                                color = AuthorityText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Train neural vision classifier weights over ward dataset",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isTraining) AuthorityPrimary.copy(alpha = 0.2f) else ColorSuccess.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusStr,
                                color = if (isTraining) AuthorityPrimary else ColorSuccess,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Primary Weights File", color = Color.Gray, fontSize = 9.sp)
                            Text("netra-vision-v3.2.weights", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Validation Accuracy", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp)
                            Text("$accuracyVal%", color = ColorSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isTraining) {
                        LinearProgressIndicator(
                            progress = { trainingProg },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AuthorityPrimary,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { viewModel.runMachineLearningTraining() },
                        enabled = !isTraining,
                        colors = ButtonDefaults.buttonColors(containerColor = AuthorityPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isTraining) "Training Neural Grid ($((trainingProg * 100).toInt())%)...." else "Start ML Training Cycle")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Model Compilation Log Console:", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black)
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(6.dp))
                            .padding(8.dp)
                    ) {
                        val consoleScrollState = rememberScrollState()
                        LaunchedEffect(logsList.size) {
                            consoleScrollState.animateScrollTo(consoleScrollState.maxValue)
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(consoleScrollState)
                        ) {
                            logsList.forEach { logLine ->
                                Text(
                                    text = "> $logLine",
                                    color = if (logLine.contains("Error", true)) Color.Red else if (logLine.contains("running", true) || logLine.contains("loading", true)) Color.Yellow else ColorSuccess,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Live AI Ward Report Compiler Panel ---
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Assemble AI Logistics Summary Report", color = AuthorityText, fontWeight = FontWeight.Bold)
                    Text("Auto-generate a ward operational report based on registered complaints registry", color = Color.LightGray, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.compileWardOperationalReport() },
                        enabled = !compilingReport,
                        colors = ButtonDefaults.buttonColors(containerColor = AuthorityPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (compilingReport) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compiling Ward 80 Report...")
                        } else {
                            Text("Compile Ward 80 Report")
                        }
                    }
                }
            }
        }

        compiledReportText?.let { report ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, ColorSuccess)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("AI RAW INTEL COMPILATION SUMMARY", color = ColorSuccess, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                            Text("v3.2 Deployed", color = ColorSuccess, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = ColorSuccess.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = report,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------
// SHARED HELPER COMPONENTS
// ------------------------------------------
@Composable
fun ChartKey(name: String, indicatorColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(indicatorColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(name, color = Color.LightGray, fontSize = 11.sp)
    }
}

@Composable
fun StatusTimelineView(currentStatus: String) {
    val states = listOf("Reported", "Verified", "In Progress", "Resolved")
    val curIndex = states.indexOf(currentStatus)

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        states.forEachIndexed { i, state ->
            val isDone = i <= curIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (i == curIndex) CitizenPrimary else if (isDone) ColorSuccess else Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(state, fontSize = 9.sp, color = if (i == curIndex) CitizenTextPrimary else CitizenTextSecondary)
            }
            if (i < states.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.5f)
                        .background(if (i < curIndex) ColorSuccess else Color.LightGray)
                )
            }
        }
    }
}

@Composable
fun NotificationFeedCard(title: String, desc: String, timeAgo: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CitizenCardBg)) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(CitizenPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Filled.Notifications, contentDescription = null, tint = CitizenPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(timeAgo, fontSize = 9.sp, color = CitizenTextSecondary)
                }
                Text(desc, fontSize = 11.sp, color = CitizenTextSecondary)
            }
        }
    }
}

@Composable
fun ChatBubbleItem(msg: ChatMessage) {
    val alignEnd = !msg.isBot
    val bubbleColor = if (alignEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (alignEnd) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = if (alignEnd) 12.dp else 0.dp,
                        bottomEnd = if (alignEnd) 0.dp else 12.dp
                    )
                )
                .background(bubbleColor)
                .padding(8.dp)
                .widthIn(max = 240.dp)
        ) {
            Text(text = msg.text, fontSize = 11.sp, color = textColor)
        }
    }
}

@Composable
fun MiniStatsCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
            Text(text = title, fontSize = 8.sp, color = Color.LightGray, maxLines = 1)
        }
    }
}

@Composable
fun AuthorityWebConsolePage(viewModel: MainViewModel) {
    val context = LocalContext.current
    val allIssues by viewModel.allIssues.collectAsState()
    
    var sirenOn by remember { mutableStateOf(false) }
    var broadcastMsg by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "CRITICAL", "PENDING"
    
    // Pulse animation for extreme alert state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alertAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuthorityBackground)
    ) {
        // --- WEB BROWSER HEADER SIMULATOR (The "different website" feel) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                // MacOS-style top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    }
                    Text(
                        text = "🌐 NAGARNETRA SEAMLESS COMMAND PORTAL v4.2",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(imageVector = Icons.Filled.Lock, contentDescription = "secured ssl", tint = ColorSuccess, modifier = Modifier.size(11.dp))
                }

                // Browser URL address bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "back", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = "next", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = "refresh", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "https://control.nagarnetra.gov/live-emergencies-feed?operator=BBMP_A7",
                        fontSize = 11.sp,
                        color = ColorSuccess,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- CORE CONTENT AREA ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Status Header block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Live Incident & Emergency Website", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
                        Text("Interactive controls linked directly to Saffron district servers", fontSize = 11.sp, color = Color.LightGray)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ColorDanger.copy(alpha = 0.2f))
                            .border(1.dp, ColorDanger, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ColorDanger))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RED ALERT ACTIVE", color = ColorDanger, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Interactive Master Siren and Warning Beacon
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (sirenOn) Modifier.border(2.dp, ColorDanger.copy(alpha = alertAlpha), RoundedCornerShape(12.dp))
                            else Modifier
                        ),
                    colors = CardDefaults.cardColors(containerColor = if (sirenOn) ColorDanger.copy(alpha = 0.08f) else AuthorityCardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (sirenOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                    contentDescription = null,
                                    tint = if (sirenOn) ColorDanger else Color.LightGray,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Master District Siren Beacon", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
                                    Text("Sounds emergency alarm across public speakers", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                            Switch(
                                checked = sirenOn,
                                onCheckedChange = {
                                    sirenOn = it
                                    if (sirenOn) {
                                        viewModel.logAuthorityAction("SYSTEM CAUTION: Master District Warning Siren has been SOUNDED manually by command portal!", "Critical")
                                        Toast.makeText(context, "Siren broadcast activated!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.logAuthorityAction("SYSTEM RECOVERY: Master Warning Siren has been silenced by command operator.", "Medium")
                                        Toast.makeText(context, "Siren silenced.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = ColorDanger,
                                    checkedTrackColor = ColorDanger.copy(alpha = 0.4f)
                                )
                            )
                        }

                        if (sirenOn) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ColorDanger, RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚠️ HIGH SPEED WARBLE ACTIVE IN INDIRANAGAR (WARD 80) WAVES",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Live Broadcast Terminal
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Live Emergency Broadcast Broadcaster", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
                        Text("Direct neural bulletin displayed instantly to citizen portals", fontSize = 10.sp, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = broadcastMsg,
                                onValueChange = { broadcastMsg = it },
                                placeholder = { Text("e.g. Danger! BWSSB Mainline burst. Expect local water cuts.", fontSize = 11.sp, color = Color.LightGray) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontSize = 12.sp, color = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuthorityPrimary,
                                    unfocusedBorderColor = Color.DarkGray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (broadcastMsg.isNotBlank()) {
                                        viewModel.logAuthorityAction("EMERGENCY BROADCAST TRANSMISSION: \"$broadcastMsg\"", "Critical")
                                        Toast.makeText(context, "Bulletins broadcasted to all citizens!", Toast.LENGTH_SHORT).show()
                                        broadcastMsg = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AuthorityPrimary)
                            ) {
                                Icon(imageVector = Icons.Filled.Send, contentDescription = "transmit", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Auxiliary Telemetry Sensors (Weather, Power, Water)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Water Pressure Gauge
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.FlashOn, contentDescription = null, tint = ColorWarning, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Electric Grid", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("104% (Heavy)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorWarning)
                            Text("BESCOM Sector 4", fontSize = 8.sp, color = Color.Gray)
                        }
                    }

                    // Water Flow Gauge
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Info, contentDescription = null, tint = ColorSuccess, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Drain Sensors", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("0.45m (Normal)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorSuccess)
                            Text("Main Storm Drain", fontSize = 8.sp, color = Color.Gray)
                        }
                    }

                    // Weather gauge
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = AuthorityCardBg)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = ColorDanger, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Zone Temp", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("36.4Â°C (High)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = ColorDanger)
                            Text("Atmospheric Sensor", fontSize = 8.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Reports Header with Filter Switches
            item {
                Column {
                    Text("Interactive Disaster & Incident reports", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
                    Text("Manage claims, route rescue drones, or sound localized alerts", fontSize = 10.sp, color = Color.LightGray)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ALL", "CRITICAL", "PENDING").forEach { filter ->
                            val isSel = selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AuthorityPrimary else Color.DarkGray.copy(alpha = 0.5f))
                                    .clickable { selectedFilter = filter }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            // Filtered Issues list!
            val filteredIssues = when (selectedFilter) {
                "CRITICAL" -> allIssues.filter { it.severity >= 8 && it.status != "Resolved" }
                "PENDING" -> allIssues.filter { it.status == "Reported" || it.status == "In Progress" }
                else -> allIssues
            }

            if (filteredIssues.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = AuthorityCardBg.copy(alpha = 0.5f))
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("No reports found matching selection.", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(filteredIssues) { issue ->
                    val priority = viewModel.calculatePriorityScore(issue)
                    val indicatorColor = if (issue.severity >= 8) ColorDanger else if (issue.severity >= 5) ColorWarning else ColorSuccess

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = AuthorityCardBg),
                        border = BorderStroke(1.dp, Color.DarkGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(indicatorColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${issue.category.uppercase()} [${issue.id}]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = indicatorColor
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.DarkGray)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "SLA Score: $priority", fontSize = 9.sp, color = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = issue.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuthorityText)
                            Text(text = issue.description, fontSize = 11.sp, color = Color.LightGray)
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Status: ${issue.status} • Dept: ${issue.assignedDept}",
                                    fontSize = 10.sp,
                                    color = AuthorityPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (issue.slaStatus == "SLA OK") ColorSuccess.copy(alpha = 0.2f) else ColorDanger.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(text = issue.slaStatus, fontSize = 8.sp, color = if (issue.slaStatus == "SLA OK") ColorSuccess else ColorDanger, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            // Interactive action buttons specifically requested
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.logAuthorityAction("Drone surveillance dispatched immediately to investigate reported ${issue.category} at Indiranagar Grid.", "Medium")
                                        Toast.makeText(context, "Drone Dispatched to Indiranagar grid!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuthorityAccent),
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Dispatch Drone", fontSize = 9.sp, color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        viewModel.logAuthorityAction("AUDIBLE ALERTS SOUNDED: Local hazard audible warning speaker activated for ticket ${issue.id}!", "High")
                                        Toast.makeText(context, "Local audio alarm sounding nearby!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorWarning),
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Sound Horn", fontSize = 9.sp, color = Color.Black)
                                }

                                Button(
                                    onClick = {
                                        viewModel.logAuthorityAction("ESCALATED REPORT: Manual administrative escalation submitted for ticket ${issue.id} directly to director level.", "Critical")
                                        Toast.makeText(context, "SLA priority escalated directly to directors!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorDanger),
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Escalate SLA", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

fun String.toBitmap(): Bitmap? {
    return try {
        val decodedBytes = android.util.Base64.decode(this, android.util.Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AutomatedAccountabilityEnginePage(viewModel: MainViewModel) {
    val aiDecisionLogs by viewModel.aiDecisionLogs.collectAsState()
    val emailLogs by viewModel.emailLogs.collectAsState()
    val wardReports by viewModel.wardReports.collectAsState()
    val isRunningAudit by viewModel.isRunningAccountabilityCheck.collectAsState()
    
    var activeSubTab by remember { mutableStateOf("Agents") } // "Agents" | "Emails" | "Reports"
    var selectedEmailForDetail by remember { mutableStateOf<EmailLog?>(null) }
    var selectedReportForDetail by remember { mutableStateOf<WardReport?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AuthorityControlHeader(
                title = "Automated Accountability Engine",
                subtitle = "Autonomous pressure loops, escalations, and official notification dispatches"
            )
        }

        // --- SECTION 1: Why this is Agentic AI Explanation Card ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("explanation_card"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                border = BorderStroke(1.dp, AuthorityPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "Shield",
                            tint = AuthorityPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "HOW THE AGENTIC LOOP ENFORCES CIVIC ACCOUNTABILITY",
                            color = AuthorityPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "This engine acts as a continuous autonomous pressure system. Instead of waiting for administrators, the AI Agent Pipeline constantly audits active tickets, compiles official government-compliant HTML notification forms with specific legal/SLA mandates, and auto-dispatches them to ward representatives.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("1. Scan", "AI scans SLAs & upvotes", ColorWarning),
                            Triple("2. Draft", "Gemini builds legal HTML", AuthorityPrimary),
                            Triple("3. Send", "Auto-dispatched via EmailJS", ColorSuccess)
                        ).forEach { (step, text, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(step, color = color, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text, color = Color.Gray, fontSize = 8.sp, textAlign = TextAlign.Center, lineHeight = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 2: Control Room Buttons ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuthorityCardBg),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "CONTROL ROOM DISPATCH TRIGGERS",
                        color = AuthorityText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.loadAccountabilityDemoData() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("load_demo_data_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Load Demo Data", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Button(
                            onClick = { viewModel.runAgenticEmailDemo() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isRunningAudit) Color.DarkGray else AuthorityPrimary),
                            modifier = Modifier.weight(1f).height(44.dp).testTag("run_audit_button"),
                            enabled = !isRunningAudit,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isRunningAudit) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auditing...", fontSize = 11.sp, color = Color.White)
                                } else {
                                    Icon(imageVector = Icons.Filled.Autorenew, contentDescription = "Run", modifier = Modifier.size(16.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Audit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerReportBot("WEEKLY") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("weekly_report_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Weekly Summary", fontSize = 10.sp, color = Color.LightGray)
                        }

                        Button(
                            onClick = { viewModel.triggerReportBot("MONTHLY") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            border = BorderStroke(1.dp, Color(0xFF475569)),
                            modifier = Modifier.weight(1f).height(38.dp).testTag("monthly_report_button"),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Monthly Report", fontSize = 10.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }

        // --- SECTION 3: Live Logs Console & Tab Selectors ---
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuthorityCardBg),
                border = BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "REAL-TIME ENGINE AUDITS",
                            color = AuthorityText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ColorSuccess.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SYSTEM ACTIVE", color = ColorSuccess, fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(2.dp)
                    ) {
                        listOf("Agents", "Emails", "Reports").forEach { tab ->
                            val isSelected = activeSubTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(0xFF1E293B) else Color.Transparent)
                                    .clickable { activeSubTab = tab }
                                    .padding(vertical = 8.dp)
                                    .testTag("tab_$tab"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AuthorityPrimary else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Logs list depending on selection
                    when (activeSubTab) {
                        "Agents" -> {
                            if (aiDecisionLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No agent decisions logged yet. Run Audit or Load Demo Data to initialize.", color = Color.Gray, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    aiDecisionLogs.take(15).forEach { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0F172A))
                                                .padding(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(
                                                        when (log.agentName) {
                                                            "VisionBot" -> Color(0xFF8B5CF6)
                                                            "CategoryBot" -> Color(0xFF3B82F6)
                                                            "DuplicateBot" -> Color(0xFFEF4444)
                                                            "PriorityBot" -> Color(0xFFF59E0B)
                                                            "PredictBot" -> Color(0xFF10B981)
                                                            "EscalationBot" -> AuthorityPrimary
                                                            "ReportBot" -> Color(0xFFEC4899)
                                                            else -> Color.Gray
                                                        }.copy(alpha = 0.15f)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = log.agentName,
                                                    fontSize = 8.sp,
                                                    color = when (log.agentName) {
                                                        "VisionBot" -> Color(0xFFC084FC)
                                                        "CategoryBot" -> Color(0xFF60A5FA)
                                                        "DuplicateBot" -> Color(0xFFF87171)
                                                        "PriorityBot" -> Color(0xFFFBBF24)
                                                        "PredictBot" -> Color(0xFF34D399)
                                                        "EscalationBot" -> AuthorityPrimary
                                                        "ReportBot" -> Color(0xFFF472B6)
                                                        else -> Color.White
                                                    },
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = log.decision,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = android.text.format.DateFormat.format("hh:mm aa", log.timestamp).toString(),
                                                        color = Color.Gray,
                                                        fontSize = 8.sp
                                                    )
                                                }
                                                Text(
                                                    text = log.detail,
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp
                                                )
                                                if (log.issueId != null) {
                                                    Text(
                                                        text = "Target ID: ${log.issueId}",
                                                        color = AuthorityPrimary.copy(alpha = 0.8f),
                                                        fontSize = 8.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "Emails" -> {
                            if (emailLogs.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No email logs generated yet.", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    emailLogs.forEach { log ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0F172A))
                                                .clickable { selectedEmailForDetail = log }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = log.recipient,
                                                        color = Color.White,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                if (log.status == "Sent") ColorSuccess.copy(alpha = 0.15f)
                                                                else ColorWarning.copy(alpha = 0.15f)
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = log.status.uppercase(),
                                                            color = if (log.status == "Sent") ColorSuccess else ColorWarning,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = log.subject,
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = android.text.format.DateFormat.format("MMM dd, hh:mm aa", log.timestamp).toString(),
                                                    color = Color.Gray,
                                                    fontSize = 8.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.Send,
                                                contentDescription = "Detail",
                                                tint = AuthorityPrimary,
                                                modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "Reports" -> {
                            if (wardReports.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No Compiled Reports saved in DB yet.", color = Color.Gray, fontSize = 11.sp)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    wardReports.forEach { report ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xFF0F172A))
                                                .clickable { selectedReportForDetail = report }
                                                .padding(10.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = if (report.reportType == "WEEKLY") "WEEKLY SUMMARY REGISTER" else "MONTHLY PERFORMANCE REGISTER",
                                                        color = AuthorityPrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = android.text.format.DateFormat.format("MMM dd, yyyy", report.timestamp).toString(),
                                                        color = Color.Gray,
                                                        fontSize = 8.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Geographical Ward: ${report.ward}",
                                                    color = Color.LightGray,
                                                    fontSize = 10.sp
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.ReceiptLong,
                                                contentDescription = "Read",
                                                tint = ColorSuccess,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Detail Modal overlays for rich text/HTML reading ---
    selectedEmailForDetail?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedEmailForDetail = null },
            confirmButton = {
                TextButton(onClick = { selectedEmailForDetail = null }) {
                    Text("Close", color = AuthorityPrimary)
                }
            },
            title = {
                Column {
                    Text("ACCOUNTABILITY DISPATCH DETAIL", color = AuthorityPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(log.subject, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFF0F172A))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Recipient: ${log.recipient}\nStatus: ${log.status}\nTrigger: ${log.triggerType}\n\nGENERATED HTML BODY PREVIEW:\n",
                        color = Color.Gray,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Divider(color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = log.body,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            },
            containerColor = AuthorityCardBg,
            shape = RoundedCornerShape(12.dp)
        )
    }

    selectedReportForDetail?.let { report ->
        AlertDialog(
            onDismissRequest = { selectedReportForDetail = null },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { 
                            Toast.makeText(context, "Ward Report exported to /downloads as PDF successfully!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Download, contentDescription = "PDF", modifier = Modifier.size(14.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export as PDF", fontSize = 10.sp, color = Color.White)
                    }
                    TextButton(onClick = { selectedReportForDetail = null }) {
                        Text("Close", color = AuthorityPrimary)
                    }
                }
            },
            title = {
                Column {
                    Text("REPORT REGISTER", color = ColorSuccess, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(if (report.reportType == "WEEKLY") "Weekly Summary Register" else "Monthly Performance Register", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFF0F172A))
                        .padding(10.dp)
                ) {
                    Text(
                        text = report.reportText,
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            },
            containerColor = AuthorityCardBg,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

















