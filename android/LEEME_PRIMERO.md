# MusicHaptic 2.5.0

Aplicación Android para asociar notas musicales con vibraciones en un brazalete de dos módulos controlado por ESP32. Incluye aprendizaje de notas, catálogo e importación MIDI, análisis de archivos de audio, niveles de intensidad y reproducción de tonos en el teléfono.

## Abrir y ejecutar

1. Abre esta carpeta `android/` en Android Studio. Debe contener `settings.gradle.kts` y `app/`.
2. Configura un JDK 17 completo para Gradle.
3. Instala Android SDK Platform 35 y Build Tools 35.0.0 si Gradle los solicita.
4. Sincroniza Gradle. El proyecto usa Gradle 8.11.1 y Android Gradle Plugin 8.9.2.
5. Conecta un teléfono con Android 7.0 o superior y depuración USB habilitada; selecciona el dispositivo y ejecuta la app.

Para conservar los datos de una instalación anterior, utiliza la misma firma. Desinstalar la app elimina los datos locales que no se hayan respaldado.

## Conectar el brazalete

Carga primero el proyecto de `../firmware/` mediante PlatformIO. La configuración dual utiliza GPIO27 para la señal izquierda y GPIO16 para la derecha. Los GPIO deben conectarse a la entrada de señal de los módulos de control; una bobina no se conecta directamente al GPIO.

En la app, concede los permisos solicitados para descubrir y conectar dispositivos Bluetooth, selecciona **ESP32 Vibrafil** y espera el estado de conexión lista. El firmware debe admitir el protocolo v4.

En Configuración, **Reproducir audio de nota con brazalete** habilita los tonos del teléfono. Su volumen depende del volumen multimedia y de la ruta de audio de Android.

## Intensidad y frecuencia

El selector ofrece Baja (25 %), Media (60 %) y Alta (100 %). La intensidad global viaja separada del reparto izquierdo/derecho. El firmware aplica GAIN, limita el nivel y después aplica el porcentaje seleccionado.

Durante una pista, el cambio de intensidad se incorpora a la siguiente renovación de la orden, nominalmente cada 250 ms, más la latencia BLE. La vibración interna del teléfono aplica el cambio en la siguiente nota.

El modo de inicio del firmware es FREQ. El máximo configurado por defecto es un duty del 75 %, ajustable entre 50 y 90 % mediante `FREQDUTY`. El porcentaje del selector representa la intensidad relativa a ese máximo. Un duty del 100 % eliminaría la alternancia de la señal.

| Nota | Frecuencia háptica solicitada (Hz) | Audio del teléfono (Hz) |
|---|---:|---:|
| DO | 62.15 | 261.63 |
| DO♯ | 75.82 | 277.18 |
| RE | 92.50 | 293.66 |
| RE♯ | 112.85 | 311.13 |
| MI | 137.68 | 329.63 |
| FA | 167.97 | 349.23 |
| FA♯ | 204.92 | 369.99 |
| SOL | 250.00 | 392.00 |
| SOL♯ | 305.00 | 415.30 |
| LA | 372.10 | 440.00 |
| LA♯ | 453.96 | 466.16 |
| SI | 553.83 | 493.88 |

El reparto logarítmico sitúa DO hacia el módulo izquierdo y SI hacia el derecho. Ambos lados comparten un temporizador. Si coinciden notas con distintas frecuencias, se prioriza la que comienza más recientemente; si comienzan juntas, la de frecuencia háptica mayor.

## Prueba manual

1. En el monitor serial de PlatformIO, a 115200 baudios, escribe `TRACE ON`.
2. Conecta la app y reproduce DO con los tres niveles. Repite con SI y con las pruebas izquierda/derecha/ambos.
3. Comprueba los campos de intensidad y frecuencia en las órdenes y en `[APPLIED]`.
4. Reproduce una pista y cambia de intensidad durante la reproducción.
5. Comprueba que Detener o desconectar apagan las salidas.

Para consultar el firmware desde el monitor serial:

```text
STATUS
PLAY DO
LAST
PLAY SI
LAST
STOP
```

`LAST` conserva la última orden aunque el pulso haya terminado. `STATUS` con `running=0` indica que las salidas están apagadas. Los datos de `[APPLIED]` son valores eléctricos calculados, no mediciones de vibración mecánica.

## Análisis de audio y archivos de referencia

La ruta activa de análisis está implementada en Kotlin/Java mediante decodificación PCM y detección de tono YIN. El archivo `app/src/main/python/algoritmo_procesamiento_senales.py` se conserva como referencia del prototipo: no existe una integración Python en la configuración Gradle actual. Su tabla original describe 12 actuadores y multiplexores U4/U5, y su salida JSON textual no equivale al protocolo binario v4.

La app carga de `tabla_triple_codificacion.json` los datos de `escala_cromatica` para inicializar la base SQLite. El bloque `hardware` de ese archivo corresponde a una configuración previa con Voice Coil y DAC; los pines actuales del montaje dual son los declarados en `../firmware/`.

## Verificación y límites

Las pruebas reproducibles se describen en [VERIFICACION.md](VERIFICACION.md).

La frecuencia enviada es la frecuencia eléctrica solicitada al GPIO. La respuesta mecánica depende del actuador, su controlador, alimentación y montaje. El firmware no mide aceleración ni utiliza MPU6050. La comprobación física requiere el brazalete y, para medir su respuesta, instrumentación adecuada.
