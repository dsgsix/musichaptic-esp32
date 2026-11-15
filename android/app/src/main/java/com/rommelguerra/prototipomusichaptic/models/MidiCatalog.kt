package com.rommelguerra.prototipomusichaptic.models

/**
 * Catálogo curado de archivos MIDI de dominio público
 * embarcados en la app (carpeta assets/midi/).
 *
 * Se listan aquí para que el UI del repositorio pueda
 * mostrarlos sin escanear el filesystem en cada apertura.
 */
data class MidiCatalogItem(
    val slug: String,          // nombre de archivo sin .mid
    val titulo: String,
    val compositor: String,
    val duracionAproxSeg: Int  // solo informativo
) {
    val assetPath: String get() = "midi/$slug.mid"
}

object MidiCatalog {

    val items: List<MidiCatalogItem> = listOf(
        MidiCatalogItem(
            slug            = "fur_elise",
            titulo          = "Für Elise",
            compositor      = "Ludwig van Beethoven",
            duracionAproxSeg = 39
        ),
        MidiCatalogItem(
            slug            = "canon_pachelbel",
            titulo          = "Canon en Re",
            compositor      = "Johann Pachelbel",
            duracionAproxSeg = 54
        ),
        MidiCatalogItem(
            slug            = "ave_maria",
            titulo          = "Ave María",
            compositor      = "Franz Schubert",
            duracionAproxSeg = 40
        ),
        MidiCatalogItem(
            slug            = "greensleeves",
            titulo          = "Greensleeves",
            compositor      = "Tradicional inglés",
            duracionAproxSeg = 40
        ),
        MidiCatalogItem(
            slug            = "amazing_grace",
            titulo          = "Amazing Grace",
            compositor      = "John Newton",
            duracionAproxSeg = 59
        ),
        MidiCatalogItem(
            slug            = "oda_alegria",
            titulo          = "Oda a la Alegría",
            compositor      = "Ludwig van Beethoven",
            duracionAproxSeg = 33
        ),
        MidiCatalogItem(
            slug            = "minueto_bach",
            titulo          = "Minué en Sol",
            compositor      = "J. S. Bach (atrib.)",
            duracionAproxSeg = 27
        ),
        MidiCatalogItem(
            slug            = "habanera",
            titulo          = "Habanera",
            compositor      = "Georges Bizet",
            duracionAproxSeg = 24
        ),
        MidiCatalogItem(
            slug            = "auld_lang_syne",
            titulo          = "Auld Lang Syne",
            compositor      = "Robert Burns / Tradicional",
            duracionAproxSeg = 44
        ),
        MidiCatalogItem(
            slug            = "saints_marching",
            titulo          = "Oh When the Saints",
            compositor      = "Tradicional góspel",
            duracionAproxSeg = 43
        )
    )

    fun encontrarPorSlug(slug: String): MidiCatalogItem? =
        items.find { it.slug == slug }
}
