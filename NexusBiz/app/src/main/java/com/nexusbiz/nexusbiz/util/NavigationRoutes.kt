package com.nexusbiz.nexusbiz.util

sealed class Screen(val route: String) {
    // Onboarding
    object Onboarding1 : Screen("onboarding_1")
    object Onboarding2 : Screen("onboarding_2")
    object Onboarding3 : Screen("onboarding_3")
    
    // Auth
    object Login : Screen("login")
    object LoginBodega : Screen("login_bodega")
    object Register : Screen("register")
    object ChangePassword : Screen("change_password")
    object ForgotPassword : Screen("forgot_password")
    object SelectRole : Screen("select_role")
    object EnableLocation : Screen("enable_location")
    object SelectDistrict : Screen("select_district")
    
    // Main
    object Home : Screen("home")
    object ProductDetail : Screen("product_detail/{productId}") {
        const val PRODUCT_ID_ARG = "productId"
        fun createRoute(productId: String) = "product_detail/$productId"
    }
    object MyGroups : Screen("my_groups")
    object GroupDetail : Screen("group_detail/{groupId}") {
        const val GROUP_ID_ARG = "groupId"
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object PickupQR : Screen("pickup_qr/{groupId}") {
        const val GROUP_ID_ARG = "groupId"
        fun createRoute(groupId: String) = "pickup_qr/$groupId"
    }
    object StoreGroupDetail : Screen("store_group_detail/{groupId}") {
        const val GROUP_ID_ARG = "groupId"
        fun createRoute(groupId: String) = "store_group_detail/$groupId"
    }
    object QuickBuy : Screen("quick_buy/{productId}") {
        const val PRODUCT_ID_ARG = "productId"
        fun createRoute(productId: String) = "quick_buy/$productId"
    }
    object ReservationSuccess : Screen("reservation_success/{quantity}") {
        const val QUANTITY_ARG = "quantity"
        fun createRoute(quantity: Int) = "reservation_success/$quantity"
    }
    object StoreDetail : Screen("store_detail/{storeId}") {
        const val STORE_ID_ARG = "storeId"
        fun createRoute(storeId: String) = "store_detail/$storeId"
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object TermsAndPrivacy : Screen("terms_privacy")
    object StoreSubscriptionPro : Screen("store_subscription_pro")
    
    // Store Owner
    object StoreDashboard : Screen("store_dashboard")
    object StoreProfile : Screen("store_profile")
    object ModeSwitching : Screen("switching_mode/{mode}") {
        const val MODE_ARG = "mode"
        fun createRoute(targetMode: com.nexusbiz.nexusbiz.ui.screens.store.ModeSwitchTarget) =
            "switching_mode/${targetMode.modeId}"
    }
    object PublishProduct : Screen("publish_product")
    object OfferPublished : Screen("offer_published/{offerId}") {
        const val OFFER_ID_ARG = "offerId"
        fun createRoute(offerId: String) = "offer_published/$offerId"
    }
    object MyProducts : Screen("my_products")
    object ScanQR : Screen("scan_qr/{groupId}") {
        const val GROUP_ID_ARG = "groupId"
        fun createRoute(groupId: String) = "scan_qr/$groupId"
    }
    object BodegaRegistrationModal : Screen("bodega_registration_modal")
    object BodegaValidateRUC : Screen("bodega_validate_ruc")
    object BodegaCommercialData : Screen("bodega_commercial_data")
    object BodegaCredentials : Screen("bodega_credentials")
    object BodegaRegistrationSuccess : Screen("bodega_registration_success")
}
