package dev.milinko.guitartuner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milinko.guitartuner.ui.TunerScreen
import dev.milinko.guitartuner.ui.composables.PermissionGate
import dev.milinko.guitartuner.ui.theme.GuitarTunerTheme
import dev.milinko.guitartuner.viewmodel.TunerViewModel
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GuitarTunerTheme {
                val viewModel: TunerViewModel = viewModel()
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                // 1. Pratimo stanje permisije
                var isPermissionGranted by remember {
                    mutableStateOf(checkPermission(context))
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    isPermissionGranted = granted
                    if (granted) {
                        viewModel.startListening()
                    } else {
                        // ZABELEŽI da je korisnik bar jednom odbio
                        val sharedPref = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
                        sharedPref.edit { putBoolean("permission_denied_once", true) }
                    }
                }

                // 2. Lifecycle posmatrač da osveži stanje kad se vratiš iz Settings-a
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val current = checkPermission(context)
                            isPermissionGranted = current

                            if (current) {viewModel.startListening()}
                            else{
                                // KLJUČNI DEO:
                                // Ako sistem kaže da OPET možemo da prikažemo dijalog (Rationale),
                                // to znači da je korisnik u Settings stavio "Ask every time".
                                // Tada brišemo naš lokalni "denied" flag.
                                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                                    context as Activity,
                                    android.Manifest.permission.RECORD_AUDIO
                                )

                                if (shouldShowRationale) {
                                    resetPermissionFlag(context)
                                }
                            }

                        } else if (event == Lifecycle.Event.ON_PAUSE) {
                            viewModel.stopListening()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        PermissionGate(
                            isGranted = isPermissionGranted,
                            onRequestPermission = {
                                // 3. PROVERA: Da li treba da pokažemo sistemski dijalog ili da ga šaljemo u Settings?
                                val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                                    context as Activity,
                                    android.Manifest.permission.RECORD_AUDIO
                                )

                                if (shouldShowRationale || !isPermissionAlreadyDenied(context)) {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                } else {
                                    // Ako je trajno odbio, dugme "Pokušaj ponovo" ga takođe šalje u Settings
                                    openAppSettings(context)
                                }
                            }
                        ) {
                            TunerScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}

// Helper funkcije van klase
private fun checkPermission(context: Context) = androidx.core.content.ContextCompat.checkSelfPermission(
    context, android.Manifest.permission.RECORD_AUDIO
) == android.content.pm.PackageManager.PERMISSION_GRANTED

// Ovo proverava da li je korisnik ikada ranije video dijalog
private fun isPermissionAlreadyDenied(context: Context): Boolean {
    val sharedPref = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    return sharedPref.getBoolean("permission_denied_once", false)
}

// Briše flag iz memorije (resetuje stanje)
private fun resetPermissionFlag(context: Context) {
    val sharedPref = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    sharedPref.edit { remove("permission_denied_once") }
}
private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        // "package:dev.milinko.guitartuner" - govori Androidu tačno koju aplikaciju da otvori
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}