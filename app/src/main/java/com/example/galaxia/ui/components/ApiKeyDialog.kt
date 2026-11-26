package com.example.galaxia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.galaxia.data.local.NasaApiKeyManager
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaWhite

/**
 * Pop-up de configuração e recomendação da chave de API da NASA.
 * Informa os limites da DEMO_KEY (30 req/hora e 50/dia) vs chave pessoal (1.000 req/hora)
 * e oferece acesso direto para gerar a chave gratuita em https://api.nasa.gov/.
 */
@Composable
fun ApiKeyDialog(
    currentCustomKey: String?,
    isUsingCustomKey: Boolean,
    onSaveKey: (String) -> Unit,
    onUseDemoKey: () -> Unit,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var keyInput by remember(currentCustomKey) { mutableStateOf(currentCustomKey.orEmpty()) }
    var showValidationError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(GalaxiaCardBackground)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Cabeçalho com ícone, título e botão fechar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = GalaxiaCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Chave de API da NASA",
                        color = GalaxiaWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar",
                        tint = GalaxiaGray
                    )
                }
            }

            // Badge de status atual
            val statusColor = if (isUsingCustomKey) Color(0xFF5EEB9B) else Color(0xFFFFB454)
            val statusBg = if (isUsingCustomKey) Color(0xFF1F3B2C) else Color(0xFF332615)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusBg)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isUsingCustomKey) "Chave pessoal ativa" else "Usando DEMO_KEY (padrão)",
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Comparativo explicativo dos limites
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF161822))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Por padrão, o aplicativo usa a DEMO_KEY, que possui restrições de até 30 requisições por hora e 50 por dia.",
                    color = GalaxiaGray,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Text(
                    text = "Com uma chave gratuita gerada no site oficial da NASA, o limite sobe para até 1.000 requisições por hora.",
                    color = GalaxiaWhite.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botão clicável para abrir https://api.nasa.gov/
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(GalaxiaCyan.copy(alpha = 0.14f))
                    .clickable {
                        try {
                            uriHandler.openUri(NasaApiKeyManager.NASA_API_PORTAL_URL)
                        } catch (_: Exception) {}
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Solicitar chave grátis em api.nasa.gov",
                    color = GalaxiaCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Abrir site da NASA",
                    tint = GalaxiaCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Campo de entrada da chave
            TextField(
                value = keyInput,
                onValueChange = {
                    keyInput = it
                    if (showValidationError) showValidationError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cole sua chave de API da NASA", color = GalaxiaGray, fontSize = 13.sp) },
                trailingIcon = {
                    if (keyInput.isNotEmpty()) {
                        IconButton(onClick = { keyInput = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar", tint = GalaxiaGray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF161822),
                    unfocusedContainerColor = Color(0xFF161822),
                    focusedTextColor = GalaxiaWhite,
                    unfocusedTextColor = GalaxiaWhite,
                    cursorColor = GalaxiaCyan,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )

            if (showValidationError) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Informe uma chave válida (sem espaços) ou continue com a DEMO_KEY.",
                    color = Color(0xFFFF6E6E),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val trimmed = keyInput.trim()
                    if (trimmed.isBlank() || trimmed.any { it.isWhitespace() }) {
                        showValidationError = true
                    } else {
                        onSaveKey(trimmed)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GalaxiaCyan)
            ) {
                Text(
                    text = "Salvar chave",
                    color = GalaxiaBackground,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(
                onClick = onUseDemoKey,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isUsingCustomKey) "Voltar para DEMO_KEY" else "Continuar com DEMO_KEY",
                    color = GalaxiaGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
