package com.vitairos.industrialai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import com.vitairos.industrialai.ui.Validators
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vitairos.industrialai.R
import com.vitairos.industrialai.data.MockData
import com.vitairos.industrialai.ui.ReportsViewModel
import com.vitairos.industrialai.ui.components.PrimaryButton
import com.vitairos.industrialai.ui.components.SecondaryButton
import com.vitairos.industrialai.ui.theme.Acero
import com.vitairos.industrialai.ui.theme.Hueso
import com.vitairos.industrialai.ui.theme.Oro
import com.vitairos.industrialai.ui.theme.VerdeNoche
import com.vitairos.industrialai.ui.theme.VitairosTheme

@Composable
fun LoginScreen(vm: ReportsViewModel, onLoggedIn: () -> Unit) {
    // La pantalla de ingreso siempre usa la superficie oscura de la marca.
    VitairosTheme(darkTheme = true) {
        var email by remember { mutableStateOf(MockData.users.first().email) }
        var password by remember { mutableStateOf(MockData.DEMO_PASSWORD) }
        var error by remember { mutableStateOf<String?>(null) }
        var submitted by remember { mutableStateOf(false) }
        val emailError = if (submitted) Validators.email(email) else null
        val passwordError = if (submitted) Validators.password(password) else null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(VerdeNoche)
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Image(painterResource(R.drawable.logo_vitairos), contentDescription = "Logo VITAIROS", modifier = Modifier.size(150.dp))
            Text("VITAIROS", style = MaterialTheme.typography.headlineMedium, color = Hueso)
            Text("Industrial AI · Reportes operacionales", style = MaterialTheme.typography.bodyLarge, color = Acero, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it; error = null },
                label = { Text("Correo") }, singleLine = true,
                isError = emailError != null,
                supportingText = { AnimatedVisibility(emailError != null) { Text(emailError.orEmpty()) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            )
            OutlinedTextField(
                value = password, onValueChange = { password = it; error = null },
                label = { Text("Contraseña") }, singleLine = true,
                isError = passwordError != null,
                supportingText = { AnimatedVisibility(passwordError != null) { Text(passwordError.orEmpty()) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            )
            AnimatedVisibility(error != null) {
                Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            PrimaryButton("Ingresar", onClick = {
                submitted = true
                if (Validators.email(email) == null && Validators.password(password) == null) {
                    if (vm.login(email, password)) onLoggedIn() else error = "Usuario o contraseña incorrectos (demo)"
                }
            })

            Text("o ingrese rápido con un usuario ficticio", color = Acero, style = MaterialTheme.typography.bodyMedium)
            MockData.users.forEach { demo ->
                SecondaryButton(
                    "${demo.role.label}: ${demo.name}",
                    onClick = { vm.loginAs(demo); onLoggedIn() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Maqueta académica DSY1105 · sesión simulada con usuarios ficticios. No se usan credenciales ni datos reales de VITAIROS.",
                color = Oro, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            )
        }
    }
}
