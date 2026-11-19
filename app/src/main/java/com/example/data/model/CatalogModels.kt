package com.example.data.model

enum class ContentType(val label: String) {
    MOVIE("Película"),
    SERIES("Serie"),
    NOVELA("Novela"),
    DOCUMENTARY("Documental")
}

enum class AgeRating(val code: String, val minAge: Int) {
    TP("TP", 0),
    AGE_7("+7", 7),
    AGE_12("+12", 12),
    AGE_16("+16", 16),
    AGE_18("+18", 18);

    fun isAllowedForKids(): Boolean = minAge <= 7

    companion object {
        fun fromCode(raw: String): AgeRating =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) || it.name.equals(raw, ignoreCase = true) } ?: TP
    }
}

enum class DetflixCategory(val displayName: String, val slug: String, val tagline: String) {
    ACCION("Acción", "accion", "Adrenalina, persecuciones y combate sin tregua"),
    AVENTURA("Aventura", "aventura", "Expediciones épicas y mundos por descubrir"),
    COMEDIA("Comedia", "comedia", "Risas garantizadas y clásicos del humor"),
    TERROR("Terror", "terror", "Tensión nocturna y clásicos del horror"),
    ROMANCE("Romance", "romance", "Historias inolvidables de amor y pasión"),
    NOVELAS("Novelas", "novelas", "Dramas románticos, pasión y capítulos diarios"),
    CIENCIA_FICCION("Ciencia ficción", "ciencia-ficcion", "Futuros distópicos, espacio y tecnología"),
    DRAMA("Drama", "drama", "Relatos intensos y actuaciones memorables"),
    SUSPENSO("Suspenso", "suspenso", "Intriga, misterio y giros inesperados"),
    ANIMACION("Animación", "animacion", "Arte animado independiente y clásicos"),
    DOCUMENTALES("Documentales", "documentales", "Historia real, ciencia, naturaleza y sociedad"),
    INFANTIL("Infantil", "infantil", "Diversión segura para los más pequeños"),
    DEPORTES("Deportes", "deportes", "Hazañas atléticas, historia y competencia");

    companion object {
        fun fromName(name: String): DetflixCategory? =
            entries.firstOrNull {
                it.displayName.equals(name, ignoreCase = true) || it.slug.equals(name, ignoreCase = true)
            }
    }
}

enum class MembershipStatus(val label: String) {
    ACTIVE("Membresía Activa"),
    EXPIRING_SOON("Por Vencer"),
    EXPIRED("Membresía Vencida"),
    SUSPENDED("Acceso Suspendido"),
    NONE("Sin Membresía")
}

data class EpisodeItem(
    val id: String,
    val seriesId: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val description: String,
    val thumbnailUrl: String,
    val streamUrl: String,
    val durationMinutes: Int,
    val subtitleUrl: String? = null
)

data class CatalogItem(
    val id: String,
    val title: String,
    val description: String,
    val posterUrl: String,
    val backdropUrl: String,
    val streamUrl: String,
    val type: ContentType,
    val categories: List<String>,
    val year: Int,
    val durationMinutes: Int,
    val language: String,
    val availableLanguages: List<String>,
    val subtitles: List<String>,
    val ageRating: AgeRating,
    val cast: List<String>,
    val director: String,
    val rating: Double,
    val popularityScore: Int,
    val isFeatured: Boolean,
    val isNewRelease: Boolean,
    val isTrending: Boolean,
    val isNewAddition: Boolean,
    val sourceName: String,
    val seasonsCount: Int = 1,
    val episodes: List<EpisodeItem> = emptyList(),
    val subtitleUrl: String? = null
)

data class ContinueWatchingEntry(
    val item: CatalogItem,
    val episode: EpisodeItem?,
    val positionMs: Long,
    val durationMs: Long,
    val progressPercent: Float,
    val lastWatchedAt: Long
)

data class AvatarOption(
    val id: String,
    val label: String,
    val primaryColorHex: Long,
    val accentColorHex: Long,
    val isKidsAvatar: Boolean = false
)

object DetflixAvatars {
    val all = listOf(
        AvatarOption("crimson_crown", "Corona Roja", 0xFFE50914, 0xFFFFB800),
        AvatarOption("noir_director", "Director Noir", 0xFF1F2937, 0xFFE50914),
        AvatarOption("gold_vip", "VIP Dorado", 0xFFB45309, 0xFFFFD166),
        AvatarOption("cyber_neon", "Sci-Fi Neon", 0xFF0F766E, 0xFF2DD4BF),
        AvatarOption("midnight_cobalt", "Cobalto", 0xFF1D4ED8, 0xFF93C5FD),
        AvatarOption("kids_rocket", "Cohete Kids", 0xFF0284C7, 0xFFFDE047, isKidsAvatar = true),
        AvatarOption("kids_dino", "Aventura Kids", 0xFF16A34A, 0xFFBBF7D0, isKidsAvatar = true),
        AvatarOption("kids_star", "Estrella Kids", 0xFFDB2777, 0xFFFBCFE8, isKidsAvatar = true)
    )

    fun findById(id: String): AvatarOption = all.firstOrNull { it.id == id } ?: all.first()
}
