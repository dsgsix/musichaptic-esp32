package com.rommelguerra.prototipomusichaptic.models

import com.rommelguerra.prototipomusichaptic.data.BaseDatosHelper

/**
 * Repositorio centralizado de las 12 notas musicales
 * utilizadas por Haptic Music.
 *
 * Cada nota contiene:
 * - Frecuencia vibrotáctil.
 * - Ubicación espacial.
 * - Amplitud.
 * - Duración.
 */
object NoteRepository {

    // Duración estándar de cada nota
    private const val DURACION_NOTA_MS = 300

    // Amplitud estándar del brazalete (porcentaje)
    private const val AMPLITUD_BRAZALETE = 100

    /**
     * Escala cromática completa de 12 notas.
     *
     * Ubicación:
     * 0.0  = izquierda
     * 5.0  = centro
     * 10.0 = derecha
     */
    val notasMusicales: List<NoteModel> = listOf(

        // 1. DO
        NoteModel(
            nombre = "DO",
            descripcionVisual = "Do - 62.15 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 62.147964530,
            ubicacion = 0.0f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 2. DO#
        NoteModel(
            nombre = "DO#",
            descripcionVisual = "Do# - 75.82 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 75.819517727,
            ubicacion = 0.9f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 3. RE
        NoteModel(
            nombre = "RE",
            descripcionVisual = "Re - 92.50 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 92.499811627,
            ubicacion = 1.8f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 4. RE#
        NoteModel(
            nombre = "RE#",
            descripcionVisual = "Re# - 112.85 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 112.849771185,
            ubicacion = 2.7f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 5. MI
        NoteModel(
            nombre = "MI",
            descripcionVisual = "Mi - 137.68 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 137.676721846,
            ubicacion = 3.6f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 6. FA
        NoteModel(
            nombre = "FA",
            descripcionVisual = "Fa - 167.97 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 167.965600644,
            ubicacion = 4.5f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 7. FA#
        NoteModel(
            nombre = "FA#",
            descripcionVisual = "Fa# - 204.92 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 204.918032787,
            ubicacion = 5.4f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 8. SOL
        NoteModel(
            nombre = "SOL",
            descripcionVisual = "Sol - 250.00 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 250.000000000,
            ubicacion = 6.3f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 9. SOL#
        NoteModel(
            nombre = "SOL#",
            descripcionVisual = "Sol# - 305.00 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 305.000000000,
            ubicacion = 7.2f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 10. LA
        NoteModel(
            nombre = "LA",
            descripcionVisual = "La - 372.10 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 372.100000000,
            ubicacion = 8.1f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 11. LA#
        NoteModel(
            nombre = "LA#",
            descripcionVisual = "La# - 453.96 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 453.962000000,
            ubicacion = 9.0f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        ),

        // 12. SI
        NoteModel(
            nombre = "SI",
            descripcionVisual = "Si - 553.83 Hz",
            patronVibracion = longArrayOf(
                0L,
                DURACION_NOTA_MS.toLong()
            ),
            amplitudes = intArrayOf(
                0,
                255
            ),
            frecuenciaHz = 553.833640000,
            ubicacion = 10.0f,
            amplitudBrazalete = AMPLITUD_BRAZALETE,
            duracionMs = DURACION_NOTA_MS
        )
    )

    /**
     * Busca una nota por nombre.
     *
     * Ejemplos:
     * DO
     * DO#
     * RE
     * SOL#
     */
    fun obtenerNotaPorNombre(
        nombre: String
    ): NoteModel? {

        return notasMusicales.find {
            it.nombre.equals(
                nombre,
                ignoreCase = true
            )
        }
    }

    /**
     * Obtiene las notas correspondientes a una canción,
     * cada una con su duración rítmica.
     *
     * Las canciones se definen como lista de pares
     * (nombreNota, duraciónEnMs). Los valores rítmicos
     * estándar utilizados son:
     *
     *   Corchea             = 250 ms
     *   Negra               = 500 ms
     *   Negra con punto     = 750 ms
     *   Blanca              = 1000 ms
     *   Blanca con punto    = 1500 ms
     *   Redonda             = 2000 ms
     *
     * Cada NoteModel devuelto lleva su propio `duracionMs`
     * y `patronVibracion`, de modo que `reproducirSecuencia`
     * respeta el ritmo al programar las notas.
     */
    fun obtenerSecuenciaCancion(
        nombreCancion: String,
        dbHelper: BaseDatosHelper? = null
    ): List<NoteModel> {

        // Duraciones rítmicas (ms)
        val CORCHEA        = 250
        val NEGRA          = 500
        val NEGRA_PUNTO    = 750
        val BLANCA         = 1000
        val BLANCA_PUNTO   = 1500
        val REDONDA        = 2000

        val notasConRitmo: List<Pair<String, Int>> = when (nombreCancion) {

            // TWINKLE TWINKLE LITTLE STAR (Estrellita)
            // Estructura: A A B B A
            //   A = DO DO SOL SOL LA LA SOL
            //   B = FA FA MI MI RE RE DO
            //   última nota de cada frase = blanca

            "Twinkle Twinkle Little Star" -> listOf(
                // Twinkle twinkle little star
                "DO"  to NEGRA, "DO"  to NEGRA,
                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA"  to NEGRA, "LA"  to NEGRA,
                "SOL" to BLANCA,
                // How I wonder what you are
                "FA"  to NEGRA, "FA"  to NEGRA,
                "MI"  to NEGRA, "MI"  to NEGRA,
                "RE"  to NEGRA, "RE"  to NEGRA,
                "DO"  to BLANCA,
                // Up above the world so high
                "SOL" to NEGRA, "SOL" to NEGRA,
                "FA"  to NEGRA, "FA"  to NEGRA,
                "MI"  to NEGRA, "MI"  to NEGRA,
                "RE"  to BLANCA,
                // Like a diamond in the sky
                "SOL" to NEGRA, "SOL" to NEGRA,
                "FA"  to NEGRA, "FA"  to NEGRA,
                "MI"  to NEGRA, "MI"  to NEGRA,
                "RE"  to BLANCA,
                // Twinkle twinkle little star (reprise)
                "DO"  to NEGRA, "DO"  to NEGRA,
                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA"  to NEGRA, "LA"  to NEGRA,
                "SOL" to BLANCA,
                // How I wonder what you are (final)
                "FA"  to NEGRA, "FA"  to NEGRA,
                "MI"  to NEGRA, "MI"  to NEGRA,
                "RE"  to NEGRA, "RE"  to NEGRA,
                "DO"  to REDONDA
            )

            // CASCABEL (Jingle Bells) — Coro
            // Canción navideña universal.
            // Rango: DO grave a SOL agudo, todo en una octava.

            "Cascabel" -> listOf(
                // Jingle bells, jingle bells
                "MI" to NEGRA, "MI" to NEGRA,
                "MI" to BLANCA,
                "MI" to NEGRA, "MI" to NEGRA,
                "MI" to BLANCA,
                // Jingle all the way
                "MI" to NEGRA, "SOL" to NEGRA,
                "DO" to NEGRA_PUNTO, "RE" to CORCHEA,
                "MI" to BLANCA,
                // Oh what fun it is to ride
                "FA" to NEGRA, "FA" to NEGRA,
                "FA" to NEGRA, "FA" to NEGRA,
                "FA" to NEGRA, "MI" to NEGRA,
                "MI" to NEGRA, "MI" to NEGRA,
                // In a one-horse open sleigh
                "SOL" to NEGRA, "SOL" to NEGRA,
                "FA" to NEGRA, "RE" to NEGRA,
                "DO" to REDONDA
            )

            // OLD MACDONALD (En la granja del tío MacDonald)
            // Canción infantil universalmente conocida.
            // Rango: RE grave a SI agudo, una octava.

            "En la Granja" -> listOf(
                // Old MacDonald had a farm
                "SOL" to NEGRA, "SOL" to NEGRA,
                "SOL" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "MI" to NEGRA,
                "RE" to BLANCA,
                // E-I-E-I-O
                "SI" to NEGRA, "SI" to NEGRA,
                "LA" to NEGRA, "LA" to NEGRA,
                "SOL" to BLANCA,
                // With a moo-moo here
                "RE" to NEGRA, "RE" to NEGRA,
                "SOL" to NEGRA, "SOL" to NEGRA,
                "SOL" to BLANCA,
                // And a moo-moo there
                "RE" to NEGRA, "RE" to NEGRA,
                "SOL" to NEGRA, "SOL" to NEGRA,
                "SOL" to BLANCA,
                // Here a moo, there a moo,
                // everywhere a moo-moo
                "SOL" to NEGRA, "SOL" to NEGRA,
                "SOL" to NEGRA, "SOL" to NEGRA,
                "SOL" to BLANCA,
                // Old MacDonald had a farm
                "SOL" to NEGRA, "SOL" to NEGRA,
                "SOL" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "MI" to NEGRA,
                "RE" to BLANCA,
                // E-I-E-I-O (final)
                "SI" to NEGRA, "SI" to NEGRA,
                "LA" to NEGRA, "LA" to NEGRA,
                "SOL" to REDONDA
            )

            // CUMPLEAÑOS FELIZ (Happy Birthday to You)
            // Mildred J. Hill & Patty Smith Hill, 1893.
            //
            // NOTA: La melodía original abarca una octava
            // completa (12 semitonos). Como el sistema háptico
            // solo llega hasta SI (11 semitonos sobre DO),
            // la nota más aguda de "Happy birthday dear NAME"
            // se comprime a SI (séptima mayor). El resto de la
            // melodía queda intacto y se mantiene reconocible.
            // Original en Fa mayor transpuesto a DO háptico.

            "Cumpleaños Feliz" -> listOf(
                // "Feliz cumpleaños a ti"
                "DO" to CORCHEA, "DO" to CORCHEA,
                "RE" to NEGRA, "DO" to NEGRA,
                "FA" to NEGRA, "MI" to BLANCA,
                // "Feliz cumpleaños a ti"
                "DO" to CORCHEA, "DO" to CORCHEA,
                "RE" to NEGRA, "DO" to NEGRA,
                "SOL" to NEGRA, "FA" to BLANCA,
                // "Feliz cumpleaños querido/a NN"
                // (aquí la original salta a octava; comprimimos a SI)
                "DO" to CORCHEA, "DO" to CORCHEA,
                "SI" to NEGRA, "LA" to NEGRA,
                "FA" to NEGRA, "MI" to CORCHEA,
                "RE" to BLANCA,
                // "Feliz cumpleaños a ti" (cierre)
                "LA#" to CORCHEA, "LA#" to CORCHEA,
                "LA" to NEGRA, "FA" to NEGRA,
                "SOL" to NEGRA, "FA" to REDONDA
            )

            // HIMNO NACIONAL DEL ECUADOR — Versión oficial
            // Música: Antonio Neumane (1866)
            // Letra:  Juan León Mera (1865)
            //
            // Estructura oficial (Coro → Verso II → Coro):
            //   1. CORO   — "¡Salve, oh Patria, mil veces...!"
            //   2. VERSO II — "Los primeros, los hijos del Sol..."
            //   3. CORO   — repetición ceremonial de cierre
            //
            // Duraciones usadas: negras de 500 ms, blancas de 1000 ms
            // y cierres de 1500 o 2000 ms.
            //
            // Contorno melódico ceremonial:
            //   - Apertura solemne sobre tonos medios
            //   - Ascenso al pico de frase en LA/SI
            //   - Descenso resolutivo hasta DO (tónica)
            //
            // Duración total aprox.: 90 segundos

            "Himno del Ecuador" -> listOf(

                // CORO — Primera enunciación
                // "¡Sal—ve, oh Pa—tria, mil ve—ces! ¡Oh Pa—tria!"

                "MI" to BLANCA_PUNTO,    // Sal (larga, majestuosa)
                "FA" to NEGRA,           // ve
                "SOL" to NEGRA,          // oh
                "LA" to NEGRA,           // Pa
                "SOL" to BLANCA,         // tria (punto alto)

                "FA" to NEGRA,           // mil
                "MI" to NEGRA,           // ve
                "RE" to BLANCA,          // ces (descenso)

                "MI" to NEGRA,           // ¡Oh
                "SOL" to NEGRA,          // Pa
                "DO" to BLANCA_PUNTO,    // tria! (tónica resolutoria)

                // "Glo—ria a ti, glo—ria a ti"
                "SOL" to NEGRA,          // Glo
                "LA" to NEGRA,           // ria
                "SOL" to NEGRA,          // a
                "FA" to BLANCA,          // ti

                "MI" to NEGRA,           // Glo
                "FA" to NEGRA,           // ria
                "RE" to NEGRA,           // a
                "DO" to BLANCA_PUNTO,    // ti (cadencia)

                // "Ya tu pe—cho, tu pe—cho re—bo—sa"
                "DO" to NEGRA,           // Ya
                "RE" to NEGRA,           // tu
                "MI" to NEGRA,           // pe
                "FA" to BLANCA,          // cho

                "FA" to NEGRA,           // tu
                "MI" to NEGRA,           // pe
                "RE" to NEGRA,           // cho
                "MI" to NEGRA,           // re
                "FA" to NEGRA,           // bo
                "MI" to BLANCA,          // sa

                // "go—zo y paz"
                "RE" to NEGRA,           // go
                "MI" to NEGRA,           // zo
                "RE" to NEGRA,           // y
                "DO" to BLANCA_PUNTO,    // paz (cadencia frase)

                // "y tu fren—te ra—dio—sa"
                "MI" to NEGRA,           // y
                "SOL" to NEGRA,          // tu
                "LA" to NEGRA,           // fren
                "SI" to BLANCA,          // te (ascenso al clímax)

                "LA" to NEGRA,           // ra
                "SOL" to NEGRA,          // dio
                "FA" to BLANCA,          // sa

                // "más que el sol con—tem—pla—mos lu—cir"
                "SOL" to NEGRA,          // más
                "LA" to NEGRA,           // que
                "SOL" to NEGRA,          // el
                "FA" to BLANCA,          // sol

                "MI" to NEGRA,           // con
                "FA" to NEGRA,           // tem
                "RE" to NEGRA,           // pla
                "MI" to NEGRA,           // mos
                "RE" to NEGRA,           // lu
                "DO" to REDONDA,         // cir (fin de coro)

                // VERSO II — "Los primeros, los hijos del Sol"
                // Segundo verso oficial más conocido.

                "DO" to NEGRA,           // Los
                "RE" to NEGRA,           // pri
                "MI" to NEGRA,           // me
                "FA" to BLANCA,          // ros

                "MI" to NEGRA,           // los
                "RE" to NEGRA,           // hi
                "MI" to NEGRA,           // jos
                "SOL" to BLANCA,         // del

                "FA" to NEGRA,           // Sol
                "MI" to BLANCA_PUNTO,    // (sostenido)

                // "los pri—me—ros los hi—jos del Sol"
                // (repetición ascendente)
                "MI" to NEGRA,           // los
                "SOL" to NEGRA,          // pri
                "LA" to NEGRA,           // me
                "SI" to BLANCA,          // ros (pico)

                "LA" to NEGRA,           // los
                "SOL" to NEGRA,          // hi
                "FA" to NEGRA,           // jos
                "SOL" to BLANCA,         // del

                "MI" to NEGRA,           // Sol
                "RE" to BLANCA_PUNTO,    // (frase)

                // "que su—bli—mes tus lau—ros bor—da—ron"
                "MI" to NEGRA,           // que
                "FA" to NEGRA,           // su
                "SOL" to NEGRA,          // bli
                "LA" to BLANCA,          // mes

                "SOL" to NEGRA,          // tus
                "FA" to NEGRA,           // lau
                "MI" to NEGRA,           // ros
                "SOL" to NEGRA,          // bor
                "FA" to NEGRA,           // da
                "MI" to BLANCA_PUNTO,    // ron

                // "en la sien co—ro—nan—do al E—cua—dor
                //  y su pue—blo con Su—cre in—mor—tal"
                "RE" to NEGRA,           // en
                "MI" to NEGRA,           // la
                "FA" to NEGRA,           // sien
                "SOL" to BLANCA,         // co

                "FA" to NEGRA,           // ro
                "MI" to NEGRA,           // nan
                "RE" to NEGRA,           // do
                "DO" to BLANCA,          // al

                "MI" to NEGRA,           // E
                "SOL" to NEGRA,          // cua
                "DO" to REDONDA,         // dor (cadencia interna)

                "SOL" to NEGRA,          // y
                "FA" to NEGRA,           // su
                "MI" to NEGRA,           // pue
                "RE" to BLANCA,          // blo

                "MI" to NEGRA,           // con
                "SOL" to NEGRA,          // Su
                "FA" to BLANCA,          // cre

                "MI" to NEGRA,           // in
                "RE" to NEGRA,           // mor
                "DO" to REDONDA,         // tal (fin verso)

                // CORO — Repetición ceremonial de cierre
                // (versión levemente más corta, como es tradición)

                "MI" to BLANCA_PUNTO,    // Sal
                "FA" to NEGRA,           // ve
                "SOL" to NEGRA,          // oh
                "LA" to NEGRA,           // Pa
                "SOL" to BLANCA,         // tria

                "FA" to NEGRA,           // mil
                "MI" to NEGRA,           // ve
                "RE" to BLANCA,          // ces

                "MI" to NEGRA,           // Oh
                "SOL" to NEGRA,          // Pa
                "DO" to BLANCA_PUNTO,    // tria

                "SOL" to NEGRA,          // Glo
                "LA" to NEGRA,           // ria
                "SOL" to NEGRA,          // a
                "FA" to BLANCA,          // ti

                "MI" to NEGRA,           // Glo
                "FA" to NEGRA,           // ria
                "RE" to NEGRA,           // a
                "DO" to REDONDA          // ti (final ceremonial)
            )

            // EL POLLITO PÍO — Versión completa acumulativa
            // Pulcino Pio (Morgana Music, 2012).
            //
            // Estructura acumulativa: en cada estrofa se
            // agrega un animal nuevo y se repiten TODOS los
            // sonidos anteriores en orden inverso, cerrando
            // siempre con el pollito.
            //
            // Se incluyen 6 animales: pollito, gallina, gallo,
            // pavo, palomo, gato. Simplificado del original.
            //
            // Duraciones base: negras de 500 ms y blancas de 1000 ms.

            "El Pollito Pío" -> listOf(

                // Introducción: "En la radio hay un pollito"

                "SOL" to NEGRA, "SOL" to NEGRA,   // En la
                "LA" to NEGRA, "LA" to NEGRA,     // ra dio
                "SOL" to NEGRA, "FA" to NEGRA,    // hay un
                "MI" to BLANCA,                    // po llito

                // "Y el pollito Pío"
                "SOL" to NEGRA, "FA" to NEGRA,    // Y el
                "MI" to NEGRA, "RE" to NEGRA,     // po llito
                "MI" to NEGRA, "RE" to BLANCA,    // Pí o

                // "Pío pío pío" (característico)
                "MI" to NEGRA, "RE" to NEGRA,     // Pí o
                "MI" to NEGRA, "RE" to NEGRA,     // Pí o
                "MI" to NEGRA, "DO" to BLANCA,    // Pí o

                // Estrofa 2: agregar gallina

                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "LA" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to BLANCA,                    // ...una gallina

                // "Y la gallina Co Co"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "MI" to NEGRA,
                "RE" to NEGRA, "RE" to BLANCA,

                // "Y el pollito Pío"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to BLANCA,

                // "Pío pío pío"
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "DO" to BLANCA_PUNTO,

                // Estrofa 3: agregar gallo

                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "LA" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to BLANCA,                    // ...un gallo

                // "Y el gallo Cocorocó"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to NEGRA, "FA" to BLANCA,

                // "Y la gallina Co Co"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to BLANCA,

                // "Y el pollito Pío... pío pío pío"
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "DO" to BLANCA_PUNTO,

                // Estrofa 4: agregar pavo

                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "LA" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to BLANCA,                    // ...un pavo

                // "Y el pavo Glu Glu"
                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "LA" to BLANCA,

                // "Y el gallo Cocorocó"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "FA" to BLANCA,

                // "Y la gallina Co Co"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to BLANCA,

                // "Y el pollito Pío... pío pío pío"
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "DO" to BLANCA_PUNTO,

                // Estrofa 5: agregar palomo

                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "LA" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to BLANCA,                    // ...un palomo

                // "Y el palomo Currucucú"
                "MI" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "SOL" to NEGRA,
                "FA" to NEGRA, "MI" to BLANCA,

                // Cadena inversa hasta el pollito
                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA" to BLANCA,                    // pavo
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to BLANCA,                    // gallo
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to BLANCA,     // gallina

                // "pío pío pío"
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "DO" to BLANCA_PUNTO,

                // Estrofa 6 (final): agregar gato

                "SOL" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "LA" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to BLANCA,                    // ...un gato

                // "Y el gato Miau Miau"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to BLANCA,

                // Cadena inversa completa (todos los animales)
                "MI" to NEGRA, "SOL" to NEGRA,
                "LA" to NEGRA, "SOL" to BLANCA,    // palomo
                "SOL" to NEGRA, "LA" to BLANCA,    // pavo
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to BLANCA,                    // gallo
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to BLANCA,     // gallina

                // "Y el pollito Pío... Pío Pío Pío" (final)
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to BLANCA,

                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "DO" to REDONDA     // Pío final
            )

            // MARÍA TENÍA UN CORDERITO (Mary Had a Little Lamb)
            // Sarah Josepha Hale, 1830. Dominio público.
            //
            // Rango DO-SOL. Duraciones base: negras y blancas.

            "María tenía un Corderito" -> listOf(
                // "Ma—rí—a te—ní—a un cor—de—ri—to"
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "MI" to NEGRA,
                "MI" to BLANCA,
                // "cor—de—ri—to"
                "RE" to NEGRA, "RE" to NEGRA,
                "RE" to BLANCA,
                // "cor—de—ri—to"
                "MI" to NEGRA, "SOL" to NEGRA,
                "SOL" to BLANCA,
                // "Ma—rí—a te—ní—a un cor—de—ri—to"
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "MI" to NEGRA,
                "MI" to NEGRA, "MI" to NEGRA,
                // "su ve—llón e—ra blan—co"
                "RE" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to REDONDA
            )

            // EL PUENTE DE LONDRES (London Bridge is Falling Down)
            // Canción infantil tradicional inglesa (s. XVII).
            // Dominio público.
            //
            // Melodía en Do mayor, estructura A-B-A.

            "El Puente de Londres" -> listOf(
                // "Lon—dres el puen—te se va a ca—er, va a ca—er"
                "SOL" to NEGRA, "LA" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA,
                // "va a ca—er"
                "RE" to NEGRA, "MI" to NEGRA,
                "FA" to BLANCA,
                // "va a ca—er"
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA,
                // "Lon—dres el puen—te se va a ca—er"
                "SOL" to NEGRA, "LA" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA,
                // "mi bella dama" (cadencia final)
                "RE" to NEGRA, "SOL" to NEGRA,
                "MI" to NEGRA, "DO" to REDONDA
            )

            // REMA, REMA TU BOTE (Row Row Row Your Boat)
            // Eliphalet Oram Lyte, 1852. Dominio público.
            //
            // Canon universal. La sección "merrily merrily"
            // originalmente salta a la octava alta (DO agudo),
            // pero como el sistema solo llega a SI, comprimimos
            // ese pico a SI (séptima mayor en lugar de octava).

            "Rema tu Bote" -> listOf(
                // "Re—ma re—ma tu bo—te"
                "DO" to NEGRA, "DO" to NEGRA,
                "DO" to NEGRA, "RE" to NEGRA,
                "MI" to BLANCA_PUNTO,
                // "sua—ve—men—te va"
                "MI" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA_PUNTO,
                // "a—le—gre—men—te" (originalmente DO agudo,
                //  aquí SI comprimido para caber en el rango)
                "SI" to NEGRA, "SI" to NEGRA,
                "SI" to BLANCA,
                // "a—le—gre—men—te"
                "SOL" to NEGRA, "SOL" to NEGRA,
                "SOL" to BLANCA,
                // "a—le—gre—men—te"
                "MI" to NEGRA, "MI" to NEGRA,
                "MI" to BLANCA,
                // "a—le—gre—men—te"
                "DO" to NEGRA, "DO" to NEGRA,
                "DO" to BLANCA,
                // "la vi—da es un sue—ño"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to REDONDA
            )

            // LOS POLLITOS DICEN
            // Canción infantil tradicional latinoamericana.
            // Dominio público.
            //
            // Melodía de dos estrofas.

            "Los Pollitos Dicen" -> listOf(
                // "Los po—lli—tos di—cen"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to NEGRA, "FA" to BLANCA,
                // "pí—o pí—o pí—o"
                "MI" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to BLANCA_PUNTO,
                // "cuan—do tie—nen ham—bre"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to NEGRA, "FA" to BLANCA,
                // "cuan—do tie—nen frí—o"
                "MI" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to BLANCA_PUNTO,
                // "La ga—lli—na bus—ca"
                "DO" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA,
                // "el ma—íz y el tri—go"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to BLANCA,
                // "les da la co—mi—da"
                "DO" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA,
                // "y les pres—ta a—bri—go"
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to REDONDA
            )

            // ODA A LA ALEGRÍA (Beethoven, Sinfonía Nº 9)
            // Dos frases simétricas, tema principal en Do M

            "Oda a la Alegría" -> listOf(
                // Frase 1
                "MI" to NEGRA, "MI" to NEGRA,
                "FA" to NEGRA, "SOL" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to NEGRA, "DO" to NEGRA,
                "RE" to NEGRA, "MI" to NEGRA,
                "MI" to NEGRA_PUNTO, "RE" to CORCHEA,
                "RE" to BLANCA,
                // Frase 2 (variación al final)
                "MI" to NEGRA, "MI" to NEGRA,
                "FA" to NEGRA, "SOL" to NEGRA,
                "SOL" to NEGRA, "FA" to NEGRA,
                "MI" to NEGRA, "RE" to NEGRA,
                "DO" to NEGRA, "DO" to NEGRA,
                "RE" to NEGRA, "MI" to NEGRA,
                "RE" to NEGRA_PUNTO, "DO" to CORCHEA,
                "DO" to BLANCA
            )

            // FRAY SANTIAGO (Frère Jacques / Bruder Jakob)
            // Canon en 4 frases, cada una se repite:
            //   A = DO RE MI DO           (negras)
            //   B = MI FA SOL             (neg. neg. blanca)
            //   C = SOL LA SOL FA MI DO   (corch. x4, neg. x2)
            //   D = DO SOL DO             (neg. neg. blanca)

            "Fray Santiago" -> listOf(
                // Fray Santiago, Fray Santiago
                "DO" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "DO" to NEGRA,
                "DO" to NEGRA, "RE" to NEGRA,
                "MI" to NEGRA, "DO" to NEGRA,
                // ¿Duermes tú? ¿Duermes tú?
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA,
                "MI" to NEGRA, "FA" to NEGRA,
                "SOL" to BLANCA,
                // Toca la campana, toca la campana
                "SOL" to CORCHEA, "LA" to CORCHEA,
                "SOL" to CORCHEA, "FA" to CORCHEA,
                "MI" to NEGRA, "DO" to NEGRA,
                "SOL" to CORCHEA, "LA" to CORCHEA,
                "SOL" to CORCHEA, "FA" to CORCHEA,
                "MI" to NEGRA, "DO" to NEGRA,
                // Din don dan, din don dan
                "DO" to NEGRA, "SOL" to NEGRA,
                "DO" to BLANCA,
                "DO" to NEGRA, "SOL" to NEGRA,
                "DO" to BLANCA
            )

            // ESCALA MIXTA — cromática ascendente y descendente
            // Ritmo constante (negra) para uso didáctico.

            "Escala Mixta" -> listOf(
                "DO"   to NEGRA, "DO#"  to NEGRA,
                "RE"   to NEGRA, "RE#"  to NEGRA,
                "MI"   to NEGRA, "FA"   to NEGRA,
                "FA#"  to NEGRA, "SOL"  to NEGRA,
                "SOL#" to NEGRA, "LA"   to NEGRA,
                "LA#"  to NEGRA, "SI"   to BLANCA,

                "SI"   to NEGRA, "LA#"  to NEGRA,
                "LA"   to NEGRA, "SOL#" to NEGRA,
                "SOL"  to NEGRA, "FA#"  to NEGRA,
                "FA"   to NEGRA, "MI"   to NEGRA,
                "RE#"  to NEGRA, "RE"   to NEGRA,
                "DO#"  to NEGRA, "DO"   to BLANCA
            )

            // Escala cromática simple por defecto
            else -> listOf(
                "DO"   to NEGRA, "DO#"  to NEGRA,
                "RE"   to NEGRA, "RE#"  to NEGRA,
                "MI"   to NEGRA, "FA"   to NEGRA,
                "FA#"  to NEGRA, "SOL"  to NEGRA,
                "SOL#" to NEGRA, "LA"   to NEGRA,
                "LA#"  to NEGRA, "SI"   to NEGRA
            )
        }

        /*
         * Aplica la duración específica de cada nota
         * al NoteModel base (obtenido de BD o repositorio).
         * Copiamos la nota base y reemplazamos duracionMs
         * y patronVibracion para que la reproducción respete
         * el ritmo definido arriba.
         */

        // Fuente de las notas base: BD si existe, si no repositorio local
        val notasBD: List<NoteModel>? =
            dbHelper?.obtenerNotasMuestrario()

        val fuenteNotas: (String) -> NoteModel? = { nombre ->
            if (notasBD != null) {
                notasBD.find {
                    it.nombre.equals(
                        nombre,
                        ignoreCase = true
                    )
                }
            } else {
                obtenerNotaPorNombre(nombre)
            }
        }

        return notasConRitmo.mapNotNull { (nombre, duracion) ->

            val notaBase = fuenteNotas(nombre)
                ?: return@mapNotNull null

            notaBase.copy(
                duracionMs = duracion,
                patronVibracion = longArrayOf(
                    0L,
                    duracion.toLong()
                ),
                amplitudes = intArrayOf(
                    0,
                    notaBase.amplitudes
                        .maxOrNull() ?: 255
                )
            )
        }
    }
}