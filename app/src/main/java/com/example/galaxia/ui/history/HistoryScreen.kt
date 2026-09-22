package com.example.galaxia.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.viewmodel.FeedViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: FeedViewModel
) {
    val selectedDateString by viewModel.historySelectedDate.collectAsState()
    val historyState by viewModel.historyState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedDate = remember(selectedDateString) {
        try {
            LocalDate.parse(selectedDateString)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    val today = LocalDate.now()
    val isToday = parsedDate.isEqual(today) || parsedDate.isAfter(today)
    val minDate = LocalDate.of(1995, 6, 16)
    val isMinDate = parsedDate.isEqual(minDate) || parsedDate.isBefore(minDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GalaxiaBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Card do Seletor de Data / Calendário
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(GalaxiaCardBackground)
                .padding(16.dp)
        ) {
            Text(
                text = "Histórico de Fotos",
                color = GalaxiaWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Botão Principal do Calendário
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF161822))
                    .clickable { showDatePicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Calendário",
                        tint = GalaxiaCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = formatDisplayDate(selectedDateString),
                            color = GalaxiaWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Toque para abrir o calendário",
                            color = GalaxiaGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controles Rápidos de Navegação por Dia
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val prevDate = parsedDate.minusDays(1)
                        viewModel.selectHistoryDate(prevDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                    },
                    enabled = !isMinDate,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Dia anterior",
                        tint = if (!isMinDate) GalaxiaCyan else GalaxiaGray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Anterior", color = if (!isMinDate) GalaxiaCyan else GalaxiaGray, fontSize = 13.sp)
                }

                TextButton(
                    onClick = { showDatePicker = true }
                ) {
                    Text("Selecionar", color = GalaxiaCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        if (!isToday) {
                            val nextDate = parsedDate.plusDays(1)
                            viewModel.selectHistoryDate(nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                    },
                    enabled = !isToday,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Próximo", color = if (!isToday) GalaxiaCyan else GalaxiaGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Próximo dia",
                        tint = if (!isToday) GalaxiaCyan else GalaxiaGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Exibição dos Dados da Foto Selecionada
        when (val state = historyState) {
            is FeedViewModel.HistoryState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = GalaxiaCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Buscando foto de ${formatShortDate(selectedDateString)}...",
                            color = GalaxiaGray,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            is FeedViewModel.HistoryState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(GalaxiaCardBackground)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = Color(0xFFFF6B6B),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.fetchHistoryByDate(selectedDateString) },
                        colors = ButtonDefaults.buttonColors(containerColor = GalaxiaCyan)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = GalaxiaBackground)
                        Spacer(Modifier.size(8.dp))
                        Text("Tentar Novamente", color = GalaxiaBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }

            is FeedViewModel.HistoryState.Success -> {
                HistoryCard(apod = state.apod)
            }
        }
    }

    // Modal de Calendário Material 3
    if (showDatePicker) {
        val initialMillis = remember(parsedDate) {
            parsedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialMillis,
            selectableDates = ApodSelectableDates
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedLocalDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.selectHistoryDate(selectedLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar", color = GalaxiaCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar", color = GalaxiaWhite)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = GalaxiaCardBackground
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = GalaxiaCardBackground,
                    titleContentColor = GalaxiaCyan,
                    headlineContentColor = GalaxiaWhite,
                    weekdayContentColor = GalaxiaCyan,
                    subheadContentColor = GalaxiaWhite,
                    yearContentColor = GalaxiaWhite,
                    currentYearContentColor = GalaxiaCyan,
                    selectedYearContentColor = GalaxiaBackground,
                    selectedYearContainerColor = GalaxiaCyan,
                    dayContentColor = GalaxiaWhite,
                    disabledDayContentColor = GalaxiaGray.copy(alpha = 0.3f),
                    selectedDayContentColor = GalaxiaBackground,
                    selectedDayContainerColor = GalaxiaCyan,
                    todayContentColor = GalaxiaCyan,
                    todayDateBorderColor = GalaxiaCyan
                )
            )
        }
    }
}

@Composable
private fun HistoryCard(apod: ApodResponse) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GalaxiaCardBackground)
    ) {
        if (apod.mediaType == "image") {
            AsyncImage(
                model = apod.url,
                contentDescription = apod.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Vídeo do dia (${apod.title})",
                    color = GalaxiaWhite,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = apod.title,
                color = GalaxiaWhite,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Data formatada e Copyright
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = GalaxiaCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatShortDate(apod.date),
                        color = GalaxiaGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                apod.copyright?.let { copyright ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Copyright,
                            contentDescription = null,
                            tint = GalaxiaGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = copyright.trim().replace("\n", " "),
                            color = GalaxiaGray,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = apod.explanation,
                color = GalaxiaGray,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { /* Favoritar */ }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = GalaxiaGray,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    apod.hdurl?.let { hdurl ->
                        IconButton(
                            onClick = {
                                try {
                                    uriHandler.openUri(hdurl)
                                } catch (_: Exception) {
                                    // Tratar se a URI falhar
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir HD",
                                tint = GalaxiaCyan,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (expanded) "MOSTRAR MENOS" else "LER MAIS →",
                    color = GalaxiaCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private object ApodSelectableDates : SelectableDates {
    private val minMillis = LocalDate.of(1995, 6, 16)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val maxMillis = LocalDate.now()
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        return utcTimeMillis in minMillis..maxMillis
    }

    override fun isSelectableYear(year: Int): Boolean {
        return year in 1995..LocalDate.now().year
    }
}

private fun formatDisplayDate(dateString: String): String {
    return try {
        val localDate = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))
        localDate.format(formatter)
    } catch (_: Exception) {
        dateString
    }
}

private fun formatShortDate(dateString: String): String {
    return try {
        val localDate = LocalDate.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        localDate.format(formatter)
    } catch (e: Exception) {
        dateString
    }
}
