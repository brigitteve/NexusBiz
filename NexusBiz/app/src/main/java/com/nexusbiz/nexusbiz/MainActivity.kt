package com.nexusbiz.nexusbiz
import com.google.firebase.analytics.FirebaseAnalytics
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.OfferRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.data.repository.StoreRepository
import com.nexusbiz.nexusbiz.navigation.RootNavGraph
import com.nexusbiz.nexusbiz.service.RealtimeService
import com.nexusbiz.nexusbiz.ui.theme.NexusBizTheme
import com.nexusbiz.nexusbiz.ui.viewmodel.AppViewModel
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var offerRepository: OfferRepository
    private lateinit var storeRepository: StoreRepository

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar Firebase Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Verificar si Firebase está funcionando
        Log.d("Firebase", "Firebase Analytics inicializado correctamente.")
        
        // Inicializar Supabase
        // TODO: Reemplazar con tus credenciales reales de Supabase
        SupabaseManager.init(
            supabaseUrl = "https://hscancddnoqnskjfbjti.supabase.co", // Ejemplo: "https://xxxxx.supabase.co"
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhzY2FuY2Rkbm9xbnNramZianRpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQzNDI3NTIsImV4cCI6MjA3OTkxODc1Mn0.6RQtl-lqCc_haBlB6Nmq2BF19-oBqVPWOT9SDBMXSC4" // Tu anon/public key de Supabase
        )
        
        // Inicializar repositorios
        authRepository = AuthRepository()
        productRepository = ProductRepository()
        offerRepository = OfferRepository()
        storeRepository = StoreRepository()
        
        // Inicializar RealtimeService - conexión base durante toda la sesión
        // Esta conexión se mantiene activa y permite que las pantallas agreguen/quiten filtros dinámicamente
        lifecycleScope.launch {
            try {
                RealtimeService.startBaseSubscriptions()
                Log.d("MainActivity", "RealtimeService iniciado correctamente")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error al iniciar RealtimeService: ${e.message}", e)
            }
        }
        
        enableEdgeToEdge()
        setContent {
            NexusBizTheme {
                val navController = rememberNavController()

                val authViewModel: AuthViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return AuthViewModel(authRepository) as T
                        }
                    }
                )

                val appViewModel: AppViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return AppViewModel(productRepository, offerRepository) as T
                        }
                    }
                )

                RootNavGraph(
                    navController = navController,
                    authViewModel = authViewModel,
                    appViewModel = appViewModel,
                    authRepository = authRepository,
                    productRepository = productRepository,
                    offerRepository = offerRepository,
                    storeRepository = storeRepository
                )
            }
        }
    }
}
