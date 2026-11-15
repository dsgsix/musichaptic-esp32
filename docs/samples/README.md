# Muestra de audio para el analizador

`escala-do-re-mi-sol-la.wav` es una señal de prueba monofónica de cinco segundos. Contiene tonos sinusoidales para DO4, RE4, MI4, SOL4 y LA4, en ese orden.

| Parámetro | Valor |
|---|---|
| Formato | WAV PCM, mono, 16 bits |
| Frecuencia de muestreo | 16 000 Hz |
| Frecuencias de los tonos | 261,63; 293,66; 329,63; 392,00; 440,00 Hz |
| Duración por tono | 0,8 s |
| Silencio después de cada tono | 0,2 s |
| Envolvente | Entrada y salida lineales de 15 ms |
| Amplitud máxima | 16 000 sobre 32 767 |

Cada tono sigue `x[n] = 16000 · e[n] · sen(2πfn/16000)`, convertido a entero con signo de 16 bits mediante truncamiento hacia cero. La envolvente `e[n]` limita la entrada y salida para reducir discontinuidades.

En la ejecución documentada con Android 15, el analizador identificó cinco notas y permitió reproducir su secuencia. Esta señal comprueba una entrada tonal controlada; no representa la complejidad de una grabación musical polifónica.
