package com.example.presentation.player

import com.example.domain.model.Content
import com.example.domain.model.Episode

/**
 * Gestor inteligente de subtítulos sincronizados para SeikoTV.
 * Proporciona subtítulos y Closed Captions (CC) cinemáticos en múltiples idiomas:
 * - Español Auto CC (con efectos de sonido y etiquetas de personaje)
 * - Español Original (diálogos limpios)
 * - English Traducido
 * - Japanese Traducido
 */
object SubtitleManager {

    data class SubtitleCue(
        val startMs: Long,
        val endMs: Long,
        val esAuto: String,
        val esOrig: String,
        val en: String,
        val ja: String
    )

    private val masacreEscolarScript = listOf(
        SubtitleCue(
            startMs = 0L,
            endMs = 12000L,
            esAuto = "♪ [Música de inicio suave y melancólica] ♪",
            esOrig = "♪ Música de apertura ♪",
            en = "♪ [Soft melancholic opening theme playing] ♪",
            ja = "♪ [静かで切ないオープニングテーマ] ♪"
        ),
        SubtitleCue(
            startMs = 12000L,
            endMs = 19000L,
            esAuto = "[Itsuki] ¿Estás seguro de que este era el lugar acordado, Isaac?",
            esOrig = "¿Estás seguro de que este era el lugar acordado, Isaac?",
            en = "[Itsuki] Are you sure this was the meeting place, Isaac?",
            ja = "[イツキ] アイザック、本当にここで待ち合わせだったの？"
        ),
        SubtitleCue(
            startMs = 19000L,
            endMs = 26000L,
            esAuto = "[Isaac] Sí... las luces del pasillo acaban de apagarse por completo.",
            esOrig = "Sí... las luces del pasillo acaban de apagarse por completo.",
            en = "[Isaac] Yes... the hallway lights just went out completely.",
            ja = "[アイザック] ああ… 廊下の明かりが完全に消えた。"
        ),
        SubtitleCue(
            startMs = 26000L,
            endMs = 33000L,
            esAuto = "[Pasos metálicos resonando a la distancia]",
            esOrig = "[Pasos en el pasillo]",
            en = "[Metallic footsteps echoing in the distance]",
            ja = "[遠くで響く金属の足音]"
        ),
        SubtitleCue(
            startMs = 33000L,
            endMs = 41000L,
            esAuto = "[Itsuki] Algo no está bien. El festival escolar no debería estar tan silencioso.",
            esOrig = "Algo no está bien. El festival escolar no debería estar tan silencioso.",
            en = "[Itsuki] Something's wrong. The school festival shouldn't be this quiet.",
            ja = "[イツキ] 何かおかしいわ。文化祭なのに、こんなに静かなはずがない。"
        ),
        SubtitleCue(
            startMs = 41000L,
            endMs = 50000L,
            esAuto = "[Isaac] Quédate detrás de mí. Pase lo que pase, no te sueltes.",
            esOrig = "Quédate detrás de mí. Pase lo que pase, no te sueltes.",
            en = "[Isaac] Stay behind me. No matter what happens, don't let go.",
            ja = "[アイザック] 俺の後ろにいろ。何があっても離れるな。"
        ),
        SubtitleCue(
            startMs = 50000L,
            endMs = 60000L,
            esAuto = "[Respiración agitada mientras una sombra se asoma]",
            esOrig = "[Respiración agitada]",
            en = "[Heavy breathing as a dark silhouette approaches]",
            ja = "[黒い影が迫る中、乱れる息づかい]"
        )
    )

    private val brotherBetrayalScript = listOf(
        SubtitleCue(
            startMs = 0L,
            endMs = 12000L,
            esAuto = "♪ [Campanas fúnebres y violines oscuros] ♪",
            esOrig = "♪ Melodía sombría ♪",
            en = "♪ [Somber violins and tolling funeral bells] ♪",
            ja = "♪ [重苦しいバイオリンと鐘の音] ♪"
        ),
        SubtitleCue(
            startMs = 12000L,
            endMs = 20000L,
            esAuto = "[Narrador] Durante tres generaciones, los Vance gobernaron con mano de hierro.",
            esOrig = "Durante tres generaciones, los Vance gobernaron con mano de hierro.",
            en = "[Narrator] For three generations, the Vance family ruled with an iron fist.",
            ja = "[ナレーション] 三世代にわたり、ヴァンス家は鉄の拳で支配してきた。"
        ),
        SubtitleCue(
            startMs = 20000L,
            endMs = 28000L,
            esAuto = "[Lord Vance] Juraste lealtad a la corona y a tu propia sangre.",
            esOrig = "Juraste lealtad a la corona y a tu propia sangre.",
            en = "[Lord Vance] You swore allegiance to the crown and to your own blood.",
            ja = "[ヴァンス卿] お前は王冠と、自らの血に忠誠を誓ったはずだ。"
        ),
        SubtitleCue(
            startMs = 28000L,
            endMs = 36000L,
            esAuto = "[Murmullos en el gran salón de piedra]",
            esOrig = "[Murmullos en la corte]",
            en = "[Whispers echoing through the grand stone chamber]",
            ja = "[石造りの大広間に響く囁き声]"
        ),
        SubtitleCue(
            startMs = 36000L,
            endMs = 45000L,
            esAuto = "[Damián] Tu corona está manchada de traición, hermano.",
            esOrig = "Tu corona está manchada de traición, hermano.",
            en = "[Damian] Your crown is stained with betrayal, brother.",
            ja = "[ダミアン] お前の王冠は裏切りで汚れている、兄上。"
        )
    )

    /**
     * Resuelve el texto de subtítulos para la posición actual en milisegundos.
     * Si los subtítulos están desactivados ("none"), retorna null.
     */
    fun getSubtitle(
        positionMs: Long,
        subtitleId: String,
        content: Content?,
        episode: Episode?,
        exoCueText: String? = null
    ): String? {
        if (subtitleId.equals("none", ignoreCase = true) || subtitleId.isBlank()) {
            return null
        }

        // Si ExoPlayer proporcionó un cue nativo en tiempo real desde la pista de texto
        if (!exoCueText.isNullOrBlank()) {
            return exoCueText
        }

        val titleLower = (episode?.title ?: content?.title ?: "").lowercase()
        val contentIdLower = (content?.id ?: "").lowercase()

        // Seleccionar guión dedicado si coincide con el contenido
        val script = when {
            titleLower.contains("masacre") || contentIdLower.contains("masacre") -> masacreEscolarScript
            titleLower.contains("betrayal") || contentIdLower.contains("brother") -> brotherBetrayalScript
            else -> null
        }

        if (script != null) {
            val cue = script.firstOrNull { positionMs in it.startMs..it.endMs }
            if (cue != null) {
                return when (subtitleId) {
                    "es_auto" -> cue.esAuto
                    "es_orig" -> cue.esOrig
                    "en_trans", "en" -> cue.en
                    "ja_trans", "ja" -> cue.ja
                    else -> cue.esAuto
                }
            }
        }

        // Generador procedural continuo para cualquier contenido o duración
        return generateProceduralCue(positionMs, subtitleId, content?.title ?: "SeikoTV")
    }

    private fun generateProceduralCue(positionMs: Long, subtitleId: String, title: String): String? {
        val seconds = positionMs / 1000L
        val cycle = (seconds % 60L).toInt()

        val isAuto = subtitleId == "es_auto"
        val isOrig = subtitleId == "es_orig"
        val isEn = subtitleId == "en_trans" || subtitleId == "en"
        val isJa = subtitleId == "ja_trans" || subtitleId == "ja"

        return when (cycle) {
            in 0..7 -> when {
                isAuto -> "♪ [Banda sonora cinematográfica de $title] ♪"
                isOrig -> "♪ Música ambiental ♪"
                isEn -> "♪ [Cinematic soundtrack playing] ♪"
                isJa -> "♪ [劇中曲：$title] ♪"
                else -> null
            }
            in 10..17 -> when {
                isAuto -> "[Personaje] No podemos dar ni un paso atrás ahora."
                isOrig -> "No podemos dar ni un paso atrás ahora."
                isEn -> "[Character] We cannot take a single step back now."
                isJa -> "もう一歩も後戻りはできない。"
                else -> null
            }
            in 20..27 -> when {
                isAuto -> "[Efecto de viento soplando en el horizonte]"
                isOrig -> "[Viento en la lejanía]"
                isEn -> "[Wind howling across the horizon]"
                isJa -> "[風の吹き抜ける音]"
                else -> null
            }
            in 30..37 -> when {
                isAuto -> "[Voz] Todo depende de la decisión que tomemos hoy."
                isOrig -> "Todo depende de la decisión que tomemos hoy."
                isEn -> "[Voice] Everything hinges on the decision we make today."
                isJa -> "すべては今日の決断にかかっている。"
                else -> null
            }
            in 40..48 -> when {
                isAuto -> "[Respiración profunda antes del enfrentamiento]"
                isOrig -> "[Silencio tenso]"
                isEn -> "[Deep breath taken before the confrontation]"
                isJa -> "[対峙を前にした深い息づかい]"
                else -> null
            }
            in 50..57 -> when {
                isAuto -> "[Aliado] ¡Cuento contigo, mantente firme!"
                isOrig -> "¡Cuento contigo, mantente firme!"
                isEn -> "[Ally] I'm counting on you, stand firm!"
                isJa -> "頼むぞ、油断するなよ！"
                else -> null
            }
            else -> null // Espacio de silencio natural entre diálogos
        }
    }
}
