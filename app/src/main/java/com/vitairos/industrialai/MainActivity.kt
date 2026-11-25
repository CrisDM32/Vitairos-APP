package com.vitairos.industrialai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import android.os.Build
import com.vitairos.industrialai.ui.ReportsViewModel
import java.net.InetAddress
import java.net.URI

import com.vitairos.industrialai.ui.navigation.AppNavHost
import com.vitairos.industrialai.ui.theme.VitairosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vm by viewModels<ReportsViewModel>()
        val permission = "android.permission.ACCESS_LOCAL_NETWORK"
        val requestNetwork = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            if (it) vm.refresh()
        }
        if (Build.VERSION.SDK_INT >= 37 && checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED &&
            listOf(BuildConfig.IDENTITY_URL, BuildConfig.REPORTS_URL).any { url ->
                val host = URI(url).host.orEmpty()
                host.endsWith(".local") || android.util.Patterns.IP_ADDRESS.matcher(host).matches() &&
                    InetAddress.getByName(host).let { it.isSiteLocalAddress || it.isLinkLocalAddress }
            }) requestNetwork.launch(permission)
        setContent {
            VitairosTheme { AppNavHost(vm) }
        }
    }
}
