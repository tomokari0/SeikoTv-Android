package com.example.data.datasource.remote

import com.example.domain.model.AudioTrack
import com.example.domain.model.Content
import com.example.domain.model.Episode
import com.example.domain.model.Season
import com.example.domain.model.SkipSegments
import com.example.domain.model.SubtitleTrack

object DefaultContentCatalog {

    private const val R2_BASE = "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev"
    private const val INTRO_VIDEO = "$R2_BASE/videos/Intro.mp4"

    // High quality reliable CDN media fallback streams (progressive MP4 and HLS)
    private const val SAMPLE_STREAM_1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    private const val SAMPLE_STREAM_2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
    private const val SAMPLE_STREAM_3 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    private const val SAMPLE_STREAM_4 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4"

    fun getAll(): List<Content> = listOf(
        createMasacreEscolar(),
        createBrotherBetrayal(),
        createKonosuba(),
        createAlternativeProject(),
        createMurderDrones(),
        createHelluvaBossVsMurderDrones(),
        createSolasEnCasa(),
        createSacrificeIsland(),
        createEstamosMuertos(),
        createUnaVenganzaApasionada(),
        createTodoONada(),
        createEntrelazados()
    )

    private fun createMasacreEscolar(): Content {
        val episodes = listOf(
            Episode(
                id = "me_ep1",
                episodeNumber = 1,
                title = "MI NAVIDAD JUNTO A TI",
                description = "Solo un especial de navidad en el colegio antes de la tragedia.",
                thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_1,
                duration = "4m",
                durationMs = 240000L,
                progressMs = 120000L,
                skipSegments = SkipSegments(0.0, 15.0)
            ),
            Episode(
                id = "me_ep2",
                episodeNumber = 2,
                title = "CAPITULO 1: LA DECLARACIÓN",
                description = "Isaac se arma de valor para declararle sus sentimientos a Itsuki antes de que las luces se apaguen.",
                thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_1,
                duration = "9m",
                durationMs = 540000L,
                progressMs = 270000L,
                skipSegments = SkipSegments(0.0, 30.0)
            ),
            Episode(
                id = "me_ep3",
                episodeNumber = 3,
                title = "NUESTRO SAN VALENTÍN",
                description = "Especial de san valentín con confesiones cruzadas en los pasillos de la escuela.",
                thumbnailUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_2,
                duration = "7m",
                durationMs = 420000L,
                progressMs = 0L,
                skipSegments = SkipSegments(0.0, 25.0)
            ),
            Episode(
                id = "me_ep4",
                episodeNumber = 4,
                title = "CAPITULO 2: LA DESAPARICIÓN",
                description = "Tras la misteriosa desaparición de la profesora de literatura, los estudiantes quedan encerrados en el gimnasio.",
                thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_3,
                duration = "7m",
                durationMs = 420000L,
                progressMs = 0L,
                skipSegments = SkipSegments(0.0, 30.0)
            ),
            Episode(
                id = "me_ep5",
                episodeNumber = 5,
                title = "CAPITULO 3: ATRAPADOS",
                description = "Once estudiantes quedan atrapados sin señal telefónica ni salidas de emergencia disponibles.",
                thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_4,
                duration = "10m",
                durationMs = 600000L,
                progressMs = 0L,
                skipSegments = SkipSegments(0.0, 30.0)
            ),
            Episode(
                id = "me_ep6",
                episodeNumber = 6,
                title = "CAPITULO 4: LA CRUELDAD",
                description = "La crueldad en el instituto alcanza su punto más alto cuando se descubre una nota ensangrentada.",
                thumbnailUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_1,
                duration = "24m",
                durationMs = 1440000L,
                skipSegments = SkipSegments(0.0, 45.0)
            ),
            Episode(
                id = "me_ep7",
                episodeNumber = 7,
                title = "CAPITULO 5: DOS AÑOS DESPUÉS",
                description = "Han pasado dos años desde la trágica muerte que inició la pesadilla en los dormitorios.",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_2,
                duration = "21m",
                durationMs = 1260000L,
                skipSegments = SkipSegments(0.0, 45.0)
            ),
            Episode(
                id = "me_ep8",
                episodeNumber = 8,
                title = "CAPITULO 6: CONSECUENCIAS",
                description = "Las consecuencias psicológicas de la persecución fracturan las alianzas entre los supervivientes.",
                thumbnailUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_3,
                duration = "27m",
                durationMs = 1620000L,
                skipSegments = SkipSegments(0.0, 40.0)
            ),
            Episode(
                id = "me_ep9",
                episodeNumber = 9,
                title = "CAPITULO 7: LA PESADILLA",
                description = "La pesadilla se desata por completo en el sótano donde se escondían las armas.",
                thumbnailUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_4,
                duration = "24m",
                durationMs = 1440000L,
                skipSegments = SkipSegments(0.0, 40.0)
            ),
            Episode(
                id = "me_ep10",
                episodeNumber = 10,
                title = "CAPITULO 11: LAS SECUELAS",
                description = "Las secuelas del engaño de Itsumi dejan al grupo desamparado frente al verdugo final.",
                thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop",
                videoUrl = SAMPLE_STREAM_2,
                duration = "25m",
                durationMs = 1500000L,
                skipSegments = SkipSegments(0.0, 35.0)
            )
        )

        val cortosList = listOf(
            episodes[0].copy(id = "me_c1", episodeNumber = 1, seasonNumber = 0),
            episodes[2].copy(id = "me_c2", episodeNumber = 2, seasonNumber = 0),
            episodes[0].copy(id = "me_c3", episodeNumber = 3, title = "RECUERDOS DEL PASILLO", seasonNumber = 0)
        )
        val t1List = listOf(
            episodes[1].copy(id = "me_s1_ep1", episodeNumber = 1, seasonNumber = 1),
            episodes[3].copy(id = "me_s1_ep2", episodeNumber = 2, seasonNumber = 1),
            episodes[4].copy(id = "me_s1_ep3", episodeNumber = 3, seasonNumber = 1),
            episodes[5].copy(id = "me_s1_ep4", episodeNumber = 4, seasonNumber = 1),
            episodes[6].copy(id = "me_s1_ep5", episodeNumber = 5, seasonNumber = 1),
            episodes[7].copy(id = "me_s1_ep6", episodeNumber = 6, seasonNumber = 1),
            episodes[8].copy(id = "me_s1_ep7", episodeNumber = 7, seasonNumber = 1),
            episodes[9].copy(id = "me_s1_ep8", episodeNumber = 8, seasonNumber = 1)
        )
        val t2List = listOf(
            episodes[5].copy(id = "me_s2_ep1", episodeNumber = 1, title = "CAPITULO 1: EL DESPERTAR", seasonNumber = 2),
            episodes[6].copy(id = "me_s2_ep2", episodeNumber = 2, title = "CAPITULO 2: SOMBRAS EN EL PASILLO", seasonNumber = 2),
            episodes[7].copy(id = "me_s2_ep3", episodeNumber = 3, title = "CAPITULO 3: LA VERDAD OCULTA", seasonNumber = 2),
            episodes[8].copy(id = "me_s2_ep4", episodeNumber = 4, title = "CAPITULO 4: JUICIO FINAL", seasonNumber = 2)
        )

        return Content(
            id = "masacre_escolar",
            title = "Masacre escolar",
            description = "Once estudiantes quedan atrapados en su colegio tras el brutal asesinato de sus compañeros y profesores a manos de un misterioso asesino que juega mentalmente con ellos al ser los únicos supervivientes. Mientras intentan escapar o ser rescatados, los estudiantes descubrirán que el verdadero peligro no siempre es el asesino... sino lo que cada uno puede llegar a convertirse..",
            thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&auto=format&fit=crop",
            backdropUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200&auto=format&fit=crop",
            videoUrl = SAMPLE_STREAM_1,
            type = "series",
            genre = listOf("Slasher", "Terror", "Thriller Psicológico", "Misterio", "Whodunit"),
            releaseYear = 2025,
            rating = "PG-13",
            status = "ongoing",
            duration = "2 Seasons",
            featured = true,
            matchPercentage = 97,
            progressMs = 544000L,
            totalDurationMs = 544000L + 360000L,
            imdbId = "tt9876543",
            imdbRating = "8.8",
            imdbVotes = "42K",
            director = "Lee Min-su, Choi Jung-mi",
            actors = "sasha_kuroyuki, Thenathetornado27, solodaniofficial, garden_of_aval0n, JeremyTe, KirotsuOwO, sugar_belly, krakendubs24, Cjota, Estelala_Gacha, sebas_virru, NayonSun, rickaoikaito, cocoon___, Akino-Sama, Tute-Fandubs, evan_15multimedia, YUNA_KI15",
            audioTracks = listOf(
                AudioTrack("a1", "Audio Original (Japonés)", "", "ja", true),
                AudioTrack("a2", "Español Latino (Doblaje)", "", "es-419"),
                AudioTrack("a3", "Español Castellano", "", "es-ES"),
                AudioTrack("a4", "Inglés (English)", "", "en")
            ),
            subtitles = listOf(
                SubtitleTrack("s1", "Español (Auto CC)", "", "es"),
                SubtitleTrack("s2", "Español (Original)", "", "es"),
                SubtitleTrack("s3", "English (Traducido)", "", "en"),
                SubtitleTrack("s4", "Japanese (Traducido)", "", "ja")
            ),
            skipSegments = SkipSegments(0.0, 30.0),
            seasons = listOf(
                Season(id = "s_cortos", seasonNumber = 0, title = "Cortos (${cortosList.size} EP)", episodes = cortosList),
                Season(id = "s_1", seasonNumber = 1, title = "Temporada 1 (${t1List.size} EP)", episodes = t1List),
                Season(id = "s_2", seasonNumber = 2, title = "Temporada 2 (${t2List.size} EP)", episodes = t2List)
            )
        )
    }

    private fun createBrotherBetrayal(): Content {
        val thumb = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("bb_ep1", 1, "Capítulo 1: Linajes Rotos", "El pacto de sangre entre hermanos se quebranta.", thumb, SAMPLE_STREAM_2, "22m", skipSegments = SkipSegments(0.0, 25.0)),
            Episode("bb_ep2", 2, "Capítulo 2: Sombras del Pasado", "Una revelación sacude el linaje de los Vance.", thumb, SAMPLE_STREAM_3, "24m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("bb_ep3", 3, "Capítulo 3: La Traición", "La venganza comienza en la oscuridad del castillo.", thumb, SAMPLE_STREAM_1, "20m", skipSegments = SkipSegments(0.0, 20.0))
        )
        return Content(
            id = "brother_betrayal",
            title = "Brother Betrayal",
            description = "After you, its me. Una historia gótica de deslealtad y venganza familiar entre hermanos de linajes opuestos.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop",
            videoUrl = SAMPLE_STREAM_2,
            type = "series",
            genre = listOf("Dark Fantasy", "Drama", "Gótico"),
            releaseYear = 2024,
            rating = "TV-MA",
            status = "cancelled",
            matchPercentage = 84,
            imdbRating = "7.9",
            audioTracks = listOf(
                AudioTrack("bb_a1", "Audio Original (Inglés)", "", "en", true),
                AudioTrack("bb_a2", "Español Latino (Doblaje)", "", "es-419"),
                AudioTrack("bb_a3", "Español Castellano", "", "es-ES"),
                AudioTrack("bb_a4", "Francés (Français)", "", "fr")
            ),
            seasons = listOf(Season("bb_s1", 1, "Temporada 1 (3 EP)", episodes))
        )
    }

    private fun createKonosuba(): Content = Content(
        id = "konosuba_crimson",
        title = "KonoSuba! Legend of Crimson",
        description = "La aldea de los Demonios Carmesí corre grave peligro, y Megumin junto a Kazuma deben emprender un viaje lleno de explosiones épicas.",
        thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop",
        backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200&auto=format&fit=crop",
        videoUrl = SAMPLE_STREAM_3,
        type = "movie",
        genre = listOf("Comedia", "Isekai", "Aventura", "Fantasía"),
        releaseYear = 2023,
        rating = "PG-13",
        status = "ongoing",
        duration = "1h 30m",
        matchPercentage = 98,
        imdbRating = "8.4",
        director = "Takaomi Kanasaki",
        audioTracks = listOf(
            AudioTrack("kn_a1", "Audio Original (Japonés)", "", "ja", true),
            AudioTrack("kn_a2", "Español Latino (Doblaje)", "", "es-419"),
            AudioTrack("kn_a3", "Español Castellano", "", "es-ES"),
            AudioTrack("kn_a4", "Inglés (English)", "", "en"),
            AudioTrack("kn_a5", "Portugués (Doblaje)", "", "pt-BR")
        )
    )

    private fun createAlternativeProject(): Content {
        val thumb = "https://images.unsplash.com/photo-1563089145-599997674d42?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("alt_ep1", 1, "Prólogo - Parte 1", "Los estudiantes despiertan en la academia sellada FUT.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1785293976128-b81apq-YTDown.com_YouTube_ALTERNATIVE-PROJECT-K-IL-L-GACHA-LIFE-SE_Media_Ax0feHb1jCM_001_1080p.mp4", "15m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("alt_ep2", 2, "Capítulo 1: Tormenta Silenciosa", "Las primeras pistas de la anomalía exterior salen a la luz.", thumb, SAMPLE_STREAM_1, "18m", skipSegments = SkipSegments(0.0, 15.0))
        )
        return Content(
            id = "alternative_project_kill",
            title = "ALTERNATIVE PROJECT - K.I.L.L.",
            description = "Un grupo de estudiantes despierta o se reúne dentro de una academia sellada ('Academia Privada FUT'), descubriendo que el mundo exterior sufre una grave anomalía meteorológica.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1563089145-599997674d42?w=1200&auto=format&fit=crop",
            videoUrl = episodes.first().videoUrl,
            type = "series",
            genre = listOf("Ciencia Ficción", "Misterio", "Acción"),
            releaseYear = 2025,
            rating = "PG-13",
            status = "ongoing",
            duration = "1 Season",
            matchPercentage = 59,
            imdbRating = "7.5",
            audioTracks = listOf(
                AudioTrack("alt_a1", "Audio Original (Japonés)", "", "ja", true),
                AudioTrack("alt_a2", "Español Latino (Doblaje)", "", "es-419"),
                AudioTrack("alt_a3", "Español Castellano", "", "es-ES"),
                AudioTrack("alt_a4", "Inglés (English)", "", "en")
            ),
            seasons = listOf(Season("alt_s1", 1, "Temporada 1 (2 EP)", episodes))
        )
    }

    private fun createMurderDrones(): Content {
        val thumb = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("md_ep1", 1, "PILOT", "Uzi conoce al dron asesino N en el desolado planeta Copper 9.", thumb, "https://12ns9ceik9.ucarecd.net/ae517c8f-8f8d-4cab-9fe9-d072d40e1998/MURDERDRONESEpisode1PILOTingles.mp4", "20m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("md_ep2", 2, "Heartbeat", "Una entidad misteriosa infecta las instalaciones subterráneas.", thumb, "https://12ns9ceik9.ucarecd.net/7ee96831-e666-48c5-baa7-990773b7b0ae/MURDERDRONESEpisode2Heartbeatingles.mp4", "18m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("md_ep3", 3, "Elegantes", "El baile de graduación se convierte en un juego mortal.", thumb, "https://12ns9ceik9.ucarecd.net/d9f9092b-3dae-437b-a50d-fab08e64c261/MurderDronesEpisodio3Ingles.mp4", "19m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("md_ep4", 4, "Síndrome de la Cabaña", "El campamento 98.7 oculta los experimentos de los humanos.", thumb, "https://12ns9ceik9.ucarecd.net/0f4d797f-4ec3-46f8-89c3-32a009ac7b0a/MurderDronesEpisodio4Ingles.mp4", "19m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("md_ep5", 5, "Hogar", "Un viaje al pasado revela los orígenes de N, V y J en la mansión.", thumb, "https://12ns9ceik9.ucarecd.net/ff331004-fd55-4773-95b3-74de0831386e/MurderDronesEpisodio5Ingles.mp4", "18m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("md_ep6", 6, "Sin Salida", "Descenso a los laboratorios más profundos.", thumb, "https://12ns9ceik9.ucarecd.net/eb8a3ffb-9809-4c8f-a3b6-68e5812ab11f/MurderDronesEpisodio6Ingles.mp4", "22m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("md_ep7", 7, "La Última Cena", "El colapso del núcleo planetario se aproxima.", thumb, "https://12ns9ceik9.ucarecd.net/b99e73e6-229f-44a5-b19f-fa415be8673f/MurderDronesEpisodio7Ingles.mp4", "21m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("md_ep8", 8, "Final Absoluto", "La confrontación definitiva por el destino de Copper 9.", thumb, "https://12ns9ceik9.ucarecd.net/fc41663b-5fdb-4962-9800-ba93a33141f6/MurderDronesEpisodio8Ingles.mp4", "20m", skipSegments = SkipSegments(0.0, 20.0))
        )
        return Content(
            id = "murder_drones",
            title = "MURDER DRONES",
            description = "Murder Drones es un programa sobre lindos pequeños robots que se asesinan entre sí por razones de programación corporativa extrema.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200&auto=format&fit=crop",
            videoUrl = episodes.first().videoUrl,
            type = "series",
            genre = listOf("Animación", "Sci-Fi", "Humor Negro", "Robots"),
            releaseYear = 2021,
            rating = "PG-13",
            status = "ongoing",
            duration = "1 Season",
            matchPercentage = 96,
            imdbRating = "8.9",
            audioTracks = listOf(
                AudioTrack("md_a1", "Audio Original (Inglés)", "", "en", true),
                AudioTrack("md_a2", "Español Latino (Doblaje)", "", "es-419"),
                AudioTrack("md_a3", "Español Castellano", "", "es-ES"),
                AudioTrack("md_a4", "Japonés (Doblaje)", "", "ja"),
                AudioTrack("md_a5", "Ruso (Русский)", "", "ru"),
                AudioTrack("md_a6", "Francés (Français)", "", "fr"),
                AudioTrack("md_a7", "Alemán (Deutsch)", "", "de")
            ),
            seasons = listOf(Season("md_s1", 1, "Temporada 1 (8 EP)", episodes))
        )
    }

    private fun createHelluvaBossVsMurderDrones(): Content = Content(
        id = "helluva_vs_murder_drones",
        title = "HELLUVA BOSS VS MURDER DRONES",
        description = "Un crossover donde Blitz y el equipo de I.M.P. son contratados para destruir un misterioso búnker subterráneo en el planeta helado Copper 9.",
        thumbnailUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=600&auto=format&fit=crop",
        backdropUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1200&auto=format&fit=crop",
        videoUrl = "https://res.cloudinary.com/dufjeusks/video/upload/v1781648841/YTDown_YouTube_HELLUVA-BOSS-VS-MURDER-DRONES-Short-Cros_Media_F-5gpW4sgHE_002_720p_qrjfxr.mp4",
        type = "movie",
        genre = listOf("Crossover", "Acción", "Humor"),
        releaseYear = 2023,
        rating = "PG-13",
        duration = "45m",
        matchPercentage = 76,
        imdbRating = "8.2",
        audioTracks = listOf(
            AudioTrack("hmd_a1", "Audio Original (Inglés)", "", "en", true),
            AudioTrack("hmd_a2", "Español Latino (Doblaje)", "", "es-419"),
            AudioTrack("hmd_a3", "Español Castellano", "", "es-ES"),
            AudioTrack("hmd_a4", "Japonés (Doblaje)", "", "ja")
        )
    )

    private fun createSolasEnCasa(): Content = Content(
        id = "solas_en_casa",
        title = "SOLAS EN CASA...",
        description = "Dos hermanas quedan solas durante una noche de tormenta eléctrica en una urbanización apartada. Ruidos en el piso de arriba desatan el pánico.",
        thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=600&auto=format&fit=crop",
        backdropUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=1200&auto=format&fit=crop",
        videoUrl = "https://res.cloudinary.com/dfmbbsyfe/video/upload/v1780984411/SOLAS_EN_CASA.._Mini_pel%C3%ADcula_-_GID_Nyna_Studio_Gacha_life__High_g1t8vi.mp4",
        type = "movie",
        genre = listOf("Suspenso", "Drama", "Terror psicológico"),
        releaseYear = 2022,
        rating = "PG-13",
        duration = "52m",
        matchPercentage = 93,
        imdbRating = "7.8",
        audioTracks = listOf(
            AudioTrack("sec_a1", "Audio Original (Español Latino)", "", "es-419", true),
            AudioTrack("sec_a2", "Español Castellano", "", "es-ES"),
            AudioTrack("sec_a3", "Inglés (Doblaje)", "", "en"),
            AudioTrack("sec_a4", "Portugués (Doblaje)", "", "pt-BR")
        )
    )

    private fun createSacrificeIsland(): Content {
        val thumb = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("si_ep1", 1, "UN DIA NORMAL", "El viaje de graduados comienza pacíficamente antes de que todo cambie.", thumb, "https://res.cloudinary.com/dfmbbsyfe/video/upload/v1780977540/YTDown_YouTube_SACRIFICE-ISLAND-UN-DIA-NORMAL-Capitulo-_Media_KRdUG9e909U_002_720p_r9y9wg.mp4", "18m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("si_ep2", 2, "DIAS ANTES DE LA TRAGEDIA", "Las señales que nadie vio en la isla maldita.", thumb, "https://res.cloudinary.com/dfmbbsyfe/video/upload/v1780978282/YTDown_YouTube_SACRIFICE-ISLAND-DIAS-ANTES-DE-LA-TRAGED_Media_lL4L2EBuUII_002_720p_rztzh9.mp4", "22m", skipSegments = SkipSegments(0.0, 15.0))
        )
        return Content(
            id = "sacrifice_island",
            title = "SACRIFICE ISLAND: LOOP MAZE",
            description = "Después de 3 largos años de estudio, el día casi se acerca. Pero... ¡Sorpresa! La academia realizará un viaje para los graduados antes de regresar a sus lugares de origen. Todo sale torcido.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1200&auto=format&fit=crop",
            videoUrl = episodes.first().videoUrl,
            type = "series",
            genre = listOf("Supervivencia", "Misterio", "Bucle Temporal"),
            releaseYear = 2026,
            rating = "PG-13",
            status = "ongoing",
            duration = "1 Season",
            matchPercentage = 62,
            imdbRating = "8.1",
            seasons = listOf(Season("si_s1", 1, "Temporada 1 (2 EP)", episodes))
        )
    }

    private fun createEstamosMuertos(): Content {
        val thumb = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("em_ep1", 1, "Capítulo 1: Brote en el Laboratorio", "Un experimento desata el virus en el laboratorio de ciencias.", thumb, SAMPLE_STREAM_4, "25m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("em_ep2", 2, "Capítulo 2: Atrapados en la Cafetería", "Los sobrevivientes intentan barricarse mientras la horda avanza.", thumb, SAMPLE_STREAM_1, "23m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("em_ep3", 3, "Capítulo 3: Hacia la Azotea", "La última oportunidad de rescate por helicóptero.", thumb, SAMPLE_STREAM_2, "26m", skipSegments = SkipSegments(0.0, 20.0))
        )
        return Content(
            id = "estamos_muertos",
            title = "Estamos muertos",
            description = "Luego de que un virus zombi se propaga por su escuela, un grupo de jóvenes atrapados debe encontrar una salida o acabar infectado.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1200&auto=format&fit=crop",
            videoUrl = episodes.first().videoUrl,
            type = "series",
            genre = listOf("Zombies", "Acción", "Drama Coreano", "Supervivencia"),
            releaseYear = 2022,
            rating = "PG-13",
            status = "completed",
            duration = "1 Season",
            matchPercentage = 79,
            imdbRating = "8.5",
            audioTracks = listOf(
                AudioTrack("em_a1", "Audio Original (Coreano)", "", "ko", true),
                AudioTrack("em_a2", "Español Latino (Doblaje)", "", "es-419"),
                AudioTrack("em_a3", "Español Castellano", "", "es-ES"),
                AudioTrack("em_a4", "Inglés (English)", "", "en"),
                AudioTrack("em_a5", "Japonés (Doblaje)", "", "ja"),
                AudioTrack("em_a6", "Francés (Français)", "", "fr"),
                AudioTrack("em_a7", "Alemán (Deutsch)", "", "de")
            ),
            seasons = listOf(Season("em_s1", 1, "Temporada 1 (3 EP)", episodes))
        )
    }

    private fun createUnaVenganzaApasionada(): Content {
        val thumb = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("va_ep1", 1, "Ep.1", "El inicio del plan de venganza contra la alta sociedad.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1785975352573-8rjj6r-YTDown.com_YouTube_Una-venganza-apasionada-Ep-1-Serie-gacha_Media_ikpiyPCTjW8_001_720p.mp4", "15m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("va_ep2", 2, "Ep.2", "Acercándose peligrosamente al círculo íntimo de la familia.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1785976610753-4jl8qc-YTDown.com_YouTube_Una-venganza-apasionada-Ep-2-Serie-gacha_Media_JwYPHuu1Uiw_001_1080p.mp4", "17m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("va_ep3", 3, "Ep.3", "Secretos oscuros salen a la luz durante la fiesta de gala.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1785977174617-4h0wmo-YTDown.com_YouTube_Una-venganza-apasionada-Ep-3-Serie-gacha_Media_6YEOE4tEZ_0_001_1080p.mp4", "18m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("va_ep4", 4, "Ep.4", "El corazón empieza a traicionar los planes originales.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/media/1785977586867-ii3ht6-YTDown.com_YouTube_Una-venganza-apasionada-Ep-4-Serie-gacha_Media_sagplMVexzY_001_1080p.mp4", "16m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("va_ep5", 5, "Ep.5", "Desenlace de lealtades divididas.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1785977951019-jgbtuc-YTDown.com_YouTube_Una-venganza-apasionada-Ep-5-Serie-gacha_Media_oQq9yEyoMcU_001_1080p.mp4", "19m", skipSegments = SkipSegments(0.0, 15.0))
        )
        return Content(
            id = "una_venganza_apasionada",
            title = "Una venganza apasionada",
            description = "Un joven decide infiltrarse en la alta sociedad para destruir a la familia que arruinó a su padre, pero se enamora de la heredera.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=1200&auto=format&fit=crop",
            videoUrl = episodes.first().videoUrl,
            type = "series",
            genre = listOf("Romance", "Venganza", "Drama"),
            releaseYear = 2024,
            rating = "TV-14",
            status = "ongoing",
            matchPercentage = 88,
            audioTracks = listOf(
                AudioTrack("va_a1", "Audio Original (Español Latino)", "", "es-419", true),
                AudioTrack("va_a2", "Español Castellano", "", "es-ES"),
                AudioTrack("va_a3", "Inglés (Doblaje)", "", "en"),
                AudioTrack("va_a4", "Italiano (Doblaje)", "", "it")
            ),
            seasons = listOf(Season("va_s1", 1, "Temporada 1 (5 EP)", episodes))
        )
    }

    private fun createTodoONada(): Content {
        val thumb = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("ton_ep1", 1, "Capitulo 1", "El inicio del sueño musical en la academia.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1786148908203-r98xd2-YTDown.com_YouTube_Todo-O-Nada-Capitulo-1-Serie-Gacha-BL-ga_Media_uT-7XFJ6q6k_001_1080p.mp4", "14m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("ton_ep2", 2, "Capitulo 2", "Primeros ensayos y rivalidades en la banda.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1786149754263-1wf0nr-_____todo_o_nada__capitulo_2____serie_gacha____bl_.mp4", "16m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("ton_ep3", 3, "PRESENTACIÓN DE CAST", "Conoce a todos los integrantes de la banda.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1786150253934-sg2yq9-_____presentaci_n_de_cast__todo_o_nada____gacha_se.mp4", "12m", skipSegments = SkipSegments(0.0, 15.0)),
            Episode("ton_ep4", 4, "Another Day Of Sun", "Interpretación especial y coreografía en el escenario.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1786150431130-tsrlgx-_____todo_o_nada__another_day_of_sun____gcmv_____g.mp4", "8m", skipSegments = SkipSegments(0.0, 10.0)),
            Episode("ton_ep5", 5, "Waving Through A Window", "El momento más emotivo antes de la competencia nacional.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1786150569061-2ph0xb-_____todo_o_nada__waving_through_a_window____gcmv_.mp4", "7m", skipSegments = SkipSegments(0.0, 10.0)),
            Episode("ton_ep6", 6, "¡Tres Intentos para San Valentín!", "Especial de san valentín en los pasillos de la escuela.", thumb, "https://pub-642e744b66244b29b7b5a6d9bc8925f4.r2.dev/videos/1786150703567-7n94r8-______tres_intentos_para_san_valent_n_____bl_____g.mp4", "10m", skipSegments = SkipSegments(0.0, 10.0))
        )
        return Content(
            id = "todo_o_nada",
            title = "Todo O Nada",
            description = "Una banda escolar de rock gacha compite en el festival nacional mientras sus miembros enfrentan dilemas personales.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1200&auto=format&fit=crop",
            videoUrl = episodes.first().videoUrl,
            type = "series",
            genre = listOf("Música", "Juvenil", "Superación"),
            releaseYear = 2024,
            rating = "TV-PG",
            status = "ongoing",
            matchPercentage = 91,
            audioTracks = listOf(
                AudioTrack("ton_a1", "Audio Original (Español Latino)", "", "es-419", true),
                AudioTrack("ton_a2", "Español Castellano", "", "es-ES"),
                AudioTrack("ton_a3", "Inglés (Doblaje)", "", "en"),
                AudioTrack("ton_a4", "Portugués (Brasil)", "", "pt-BR")
            ),
            seasons = listOf(Season("ton_s1", 1, "Temporada 1 (6 EP)", episodes))
        )
    }

    private fun createEntrelazados(): Content {
        val thumb = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=600&auto=format&fit=crop"
        val episodes = listOf(
            Episode("en_ep1", 1, "Capítulo 1: Lazos Invisibles", "El primer encuentro que une dos destinos bajo las estrellas.", thumb, SAMPLE_STREAM_3, "20m", skipSegments = SkipSegments(0.0, 20.0)),
            Episode("en_ep2", 2, "Capítulo 2: El Eco del Tiempo", "Un misterioso lazo mágico empieza a desafiar las reglas del mundo.", thumb, SAMPLE_STREAM_4, "22m", skipSegments = SkipSegments(0.0, 20.0))
        )
        return Content(
            id = "entrelazados",
            title = "Entrelazados",
            description = "Destinos cruzados bajo las estrellas donde dos almas descubren un misterioso lazo mágico que desafía el tiempo.",
            thumbnailUrl = thumb,
            backdropUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=1200&auto=format&fit=crop",
            videoUrl = episodes.first().videoUrl,
            type = "series",
            genre = listOf("Fantasía", "Magia", "Romance"),
            releaseYear = 2024,
            rating = "TV-PG",
            status = "ongoing",
            matchPercentage = 95,
            audioTracks = listOf(
                AudioTrack("en_a1", "Audio Original (Español Latino)", "", "es-419", true),
                AudioTrack("en_a2", "Español Castellano", "", "es-ES"),
                AudioTrack("en_a3", "Inglés (Doblaje)", "", "en"),
                AudioTrack("en_a4", "Italiano (Doblaje)", "", "it")
            ),
            seasons = listOf(Season("en_s1", 1, "Temporada 1 (2 EP)", episodes))
        )
    }
}
