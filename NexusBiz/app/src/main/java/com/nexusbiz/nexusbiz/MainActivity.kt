package com.nexusbiz.nexusbiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.nexusbiz.nexusbiz.data.remote.SupabaseManager
import com.nexusbiz.nexusbiz.data.repository.AuthRepository
import com.nexusbiz.nexusbiz.data.repository.GroupRepository
import com.nexusbiz.nexusbiz.data.repository.ProductRepository
import com.nexusbiz.nexusbiz.data.repository.StoreRepository
import com.nexusbiz.nexusbiz.navigation.RootNavGraph
import com.nexusbiz.nexusbiz.ui.theme.NexusBizTheme
import com.nexusbiz.nexusbiz.ui.viewmodel.AppViewModel
import com.nexusbiz.nexusbiz.ui.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    private lateinit var authRepository: AuthRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var groupRepository: GroupRepository
    private lateinit var storeRepository: StoreRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Supabase
        // TODO: Reemplazar con tus credenciales reales de Supabase
        SupabaseManager.init(
            supabaseUrl = "https://loqibytqlewuygzzhsag.supabase.co", // Ejemplo: "https://xxxxx.supabase.co"
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxvcWlieXRxbGV3dXlnenpoc2FnIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjQxMDE0OTMsImV4cCI6MjA3OTY3NzQ5M30.yjmbnzfO79liCyUQ5ZiMco0E-0tUKmwRkg6FhleeTbM" // Tu anon/public key de Supabase
        )
        
        // Inicializar repositorios
        authRepository = AuthRepository()
        productRepository = ProductRepository()
        groupRepository = GroupRepository()
        storeRepository = StoreRepository()
        
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
                            return AppViewModel(productRepository, groupRepository) as T
                        }
                    }
                )

                RootNavGraph(
                    navController = navController,
                    authViewModel = authViewModel,
                    appViewModel = appViewModel,
                    authRepository = authRepository,
                    productRepository = productRepository,
                    groupRepository = groupRepository,
                    storeRepository = storeRepository
                )
            }
        }
    }
}
