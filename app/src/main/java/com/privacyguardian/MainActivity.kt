package com.privacyguardian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.rememberNavController
import com.privacyguardian.ui.navigation.AppNavGraph
import com.privacyguardian.ui.scanner.ScanViewModel
import com.privacyguardian.ui.theme.Background
import com.privacyguardian.ui.theme.PrivacyGuardianTheme

class MainActivity : ComponentActivity() {

    private lateinit var scanViewModel: ScanViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as PrivacyGuardianApp
        scanViewModel = ViewModelProvider(this)[ScanViewModel::class.java]
        scanViewModel.init(app)

        setContent {
            PrivacyGuardianTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize(), color = Background) {
                    val navController = rememberNavController()
                    AppNavGraph(
                        navController = navController,
                        app = app,
                        scanViewModel = scanViewModel
                    )
                }
            }
        }
    }
}
