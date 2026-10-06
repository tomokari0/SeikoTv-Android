package com.example.presentation.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.ui.theme.SeikoBorder
import com.example.ui.theme.SeikoCardSurface
import com.example.ui.theme.SeikoDarkSurface
import com.example.ui.theme.SeikoRed
import com.example.ui.theme.TextDarkGray
import com.example.ui.theme.TextGray

@Composable
fun EpisodeDrawer(
    content: Content,
    currentEpisode: Episode?,
    onSelectEpisode: (Episode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("EPISODIOS", "SINOPSIS", "REPARTO")

    val seasons = content.seasons
    val initialSeasonIndex = remember(content.id, currentEpisode?.id) {
        if (currentEpisode != null && seasons.isNotEmpty()) {
            val idx = seasons.indexOfFirst { season ->
                season.seasonNumber == currentEpisode.seasonNumber ||
                season.episodes.any { it.id == currentEpisode.id }
            }
            if (idx >= 0) idx else 0
        } else 0
    }
    var selectedSeasonIndex by remember(content.id) { mutableIntStateOf(initialSeasonIndex) }
    var seasonMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(currentEpisode?.id, currentEpisode?.seasonNumber, seasons.size) {
        if (currentEpisode != null && seasons.isNotEmpty()) {
            val idx = seasons.indexOfFirst { season ->
                season.seasonNumber == currentEpisode.seasonNumber ||
                season.episodes.any { it.id == currentEpisode.id }
            }
            if (idx >= 0) {
                selectedSeasonIndex = idx
            }
        }
    }

    val currentSeason = seasons.getOrNull(selectedSeasonIndex) ?: seasons.firstOrNull()
    val episodes = currentSeason?.episodes ?: content.seasons.flatMap { it.episodes }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(360.dp)
            .background(SeikoDarkSurface.copy(alpha = 0.96f))
            .border(1.dp, SeikoBorder, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DETALLES & EPISODIOS",
                    color = SeikoRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = SeikoRed,
                        height = 2.dp
                    )
                },
                divider = {
                    HorizontalDivider(color = SeikoBorder)
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) Color.White else TextGray
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> { // EPISODIOS
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (seasons.size > 1) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SeikoCardSurface)
                                        .border(1.dp, SeikoBorder, RoundedCornerShape(8.dp))
                                        .clickable { seasonMenuExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentSeason?.title ?: "Temporada 1",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Cambiar temporada",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = seasonMenuExpanded,
                                    onDismissRequest = { seasonMenuExpanded = false },
                                    modifier = Modifier.background(SeikoCardSurface)
                                ) {
                                    seasons.forEachIndexed { index, season ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = season.title,
                                                    color = if (index == selectedSeasonIndex) SeikoRed else Color.White,
                                                    fontWeight = if (index == selectedSeasonIndex) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedSeasonIndex = index
                                                seasonMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(episodes) { episode ->
                                val isCurrent = episode.id == currentEpisode?.id ||
                                        (episode.seasonNumber == currentEpisode?.seasonNumber && episode.episodeNumber == currentEpisode?.episodeNumber)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isCurrent) SeikoCardSurface else Color.Transparent)
                                        .border(
                                            width = if (isCurrent) 1.5.dp else 1.dp,
                                            color = if (isCurrent) SeikoRed else SeikoBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectEpisode(episode) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .width(96.dp)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black)
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(context)
                                                .data(episode.thumbnailUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = episode.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        if (isCurrent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .align(Alignment.Center)
                                                    .clip(CircleShape)
                                                    .background(SeikoRed),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        val sNum = episode.seasonNumber
                                        val seasonTag = if (sNum == 0) "Corto" else "T$sNum"
                                        Text(
                                            text = "$seasonTag • E${episode.episodeNumber}",
                                            color = SeikoRed,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = episode.title,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = episode.duration,
                                            color = TextDarkGray,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(onClick = { /* Download */ }, modifier = Modifier.size(32.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Descargar",
                                            tint = TextGray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> { // SINOPSIS
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        item {
                            Text(
                                text = content.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = content.description,
                                color = TextGray,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            if (content.genre.isNotEmpty()) {
                                Text(
                                    text = "Géneros: ${content.genre.joinToString(", ")}",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            content.director?.let {
                                Text(
                                    text = "Dirección: $it",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                2 -> { // REPARTO
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ACTORES Y CREADORES",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SeikoRed.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "FIRESTORE",
                                        color = SeikoRed,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Cast list parsed from metadata
                        val actorsList = listOf(
                            Pair("sasha_kuroyuki", "DAMIAN"),
                            Pair("Thenathetornado27", "CAMILA"),
                            Pair("solodaniofficial", "DERECK"),
                            Pair("garden_of_aval0n", "PROFESOR"),
                            Pair("JeremyTe", "DIRECTOR"),
                            Pair("KirotsuOwO", "ISAAC"),
                            Pair("sugar_belly", "ITSUKI"),
                            Pair("krakendubs24", "INVESTIGADOR")
                        )

                        items(actorsList) { (actor, character) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SeikoCardSurface)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2A2A2A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = actor.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = actor,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Voz • $character",
                                        color = SeikoRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
