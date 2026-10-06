package com.example.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.presentation.components.ContentCard
import com.example.presentation.components.ContinueWatchingCard
import com.example.presentation.components.HeroBanner
import com.example.presentation.components.SeikoTopBar
import com.example.presentation.details.ContentDetailBottomSheet
import com.example.ui.theme.SeikoBlack
import com.example.ui.theme.SeikoBorder
import com.example.ui.theme.SeikoCardSurface
import com.example.ui.theme.SeikoDarkSurface
import com.example.ui.theme.SeikoRed
import com.example.ui.theme.TextDarkGray
import com.example.ui.theme.TextGray
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onPlayContent: (Content, Episode?) -> Unit,
    onOpenDetail: (Content) -> Unit,
    onCloseDetail: () -> Unit,
    onToggleMyList: (Content) -> Unit,
    onDownloadEpisode: (Content, Episode) -> Unit,
    onFilterChange: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    var isSearchActive by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SeikoBlack,
                modifier = Modifier.width(300.dp)
            ) {
                // Navigation menu matching Screenshot 3
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val navItems = listOf(
                        "INICIO",
                        "PELÍCULAS",
                        "SERIES",
                        "GÉNEROS",
                        "DESCARGAS",
                        "SUBIR"
                    )

                    navItems.forEach { item ->
                        Text(
                            text = item,
                            color = if (item == "INICIO") SeikoRed else Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch { drawerState.close() }
                                    if (item == "DESCARGAS") {
                                        onNavigateToDownloads()
                                    }
                                }
                                .padding(vertical = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = "CERRAR",
                        color = TextDarkGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clickable { coroutineScope.launch { drawerState.close() } }
                            .padding(8.dp)
                    )
                }
            }
        }
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(SeikoBlack)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // Hero Banner
                uiState.featured?.let { featured ->
                    item {
                        HeroBanner(
                            content = featured,
                            onPlayClick = { onPlayContent(featured, featured.seasons.firstOrNull()?.episodes?.firstOrNull()) },
                            onInfoClick = { onOpenDetail(featured) },
                            onToggleMyList = { onToggleMyList(featured) },
                            isInMyList = featured.isInMyList
                        )
                    }
                }

                // Indicador de Modo Niños según el perfil activo
                if (uiState.activeProfile.isKids) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "MODO NIÑOS ACTIVO",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• ${uiState.activeProfile.name}: Contenido apto para toda la familia",
                                color = TextGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Search Bar (if triggered)
                if (isSearchActive) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = uiState.searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text("Buscar anime, gacha o película...", color = TextGray) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = SeikoRed
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        isSearchActive = false
                                        onSearchChange("")
                                        keyboardController?.hide()
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Cerrar búsqueda",
                                            tint = Color.White
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SeikoRed,
                                    unfocusedBorderColor = SeikoBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Search Results
                    if (uiState.searchQuery.isNotBlank()) {
                        item {
                            Text(
                                text = "Resultados para '${uiState.searchQuery}' (${uiState.searchResults.size})",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.searchResults) { item ->
                                    ContentCard(
                                        content = item,
                                        onClick = { onOpenDetail(item) }
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. CONTINUAR VIENDO (Screenshots 1 & 2)
                if (uiState.continueWatching.isNotEmpty()) {
                    item {
                        SectionHeader(title = "CONTINUAR VIENDO")
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(uiState.continueWatching) { item ->
                                ContinueWatchingCard(
                                    content = item,
                                    onClick = { onPlayContent(item, item.seasons.firstOrNull()?.episodes?.firstOrNull()) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 2. RECOMENDADOS PARA TI (Filtrado por géneros más vistos del usuario)
                if (uiState.recommendedForYou.isNotEmpty()) {
                    item {
                        val genresSubtitle = if (uiState.userTopGenres.isNotEmpty()) {
                            "Basado en tus géneros favoritos: ${uiState.userTopGenres.joinToString(" • ")}"
                        } else {
                            "Personalizado según tus preferencias"
                        }
                        SectionHeader(
                            title = "RECOMENDADOS PARA TI",
                            subtitle = genresSubtitle
                        )
                    }
                    if (uiState.userTopGenres.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.userTopGenres.forEach { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(SeikoRed.copy(alpha = 0.15f))
                                            .border(1.dp, SeikoRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "#$genre",
                                            color = SeikoRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.testTag("recommended_for_you_row")
                        ) {
                            items(uiState.recommendedForYou) { item ->
                                ContentCard(
                                    content = item,
                                    onClick = { onOpenDetail(item) },
                                    modifier = Modifier.testTag("recommended_card_${item.id}")
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                // 3. NUESTRAS RECOMENDACIONES (Screenshot 2)
                item {
                    SectionHeader(title = "NUESTRAS RECOMENDACIONES")
                }
                item {
                    // Filter Chips: TODOS, RECIENTES, MÁS VISTOS
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("TODOS", "RECIENTES", "MÁS VISTOS").forEach { filter ->
                            val isSelected = uiState.selectedFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) SeikoRed else SeikoDarkSurface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) SeikoRed else SeikoBorder,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { onFilterChange(filter) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = filter,
                                    color = if (isSelected) Color.White else TextGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.recommendations) { item ->
                            ContentCard(
                                content = item,
                                onClick = { onOpenDetail(item) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 3. SERIES EN EMISIÓN
                item {
                    SectionHeader(title = "SERIES EN EMISIÓN")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.series) { item ->
                            ContentCard(
                                content = item,
                                onClick = { onOpenDetail(item) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // 4. PELÍCULAS Y ESPECIALES
                item {
                    SectionHeader(title = "PELÍCULAS")
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.movies) { item ->
                            ContentCard(
                                content = item,
                                onClick = { onOpenDetail(item) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // 5. SEIKOYT Footer (Screenshots 2 & 3)
                item {
                    SeikoFooter()
                }
            }

            // Top Bar overlaid on top
            SeikoTopBar(
                activeProfile = uiState.activeProfile,
                onMenuClick = {
                    coroutineScope.launch { drawerState.open() }
                },
                onSearchClick = {
                    isSearchActive = !isSearchActive
                },
                onProfileClick = onProfileClick,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            // Floating Red Action / Support Button (Screenshots 1-3)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(SeikoRed)
                    .clickable { /* Community / support dialog */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "Comunidad SeikoTV",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // BottomSheet Detail Dialog
            uiState.selectedContentForDetail?.let { detailContent ->
                ContentDetailBottomSheet(
                    content = detailContent,
                    onDismiss = onCloseDetail,
                    onPlayContent = onPlayContent,
                    onDownloadEpisode = onDownloadEpisode,
                    onToggleMyList = onToggleMyList,
                    isInMyList = detailContent.isInMyList
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Red accent vertical bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SeikoRed)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SeikoFooter() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SeikoDarkSurface)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "SEIKO",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "YT",
                color = SeikoRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "La plataforma definitiva de entretenimiento Gacha. Contenido de calidad creado por y para la comunidad.",
            color = TextDarkGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "#STAYGACHA",
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "© 2026 SEIKOYT. TODOS LOS DERECHOS RESERVADOS.\nHECHO CON ❤ POR EL TEAM SEIKO",
            color = TextDarkGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp
        )
    }
}
