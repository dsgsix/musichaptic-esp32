# Verificación de MusicHaptic

## Pruebas del núcleo Java

Desde esta carpeta:

```text
python tests/run_core_tests.py
```

El script necesita un JDK con `javac` y `java`. Comprueba protocolo, checksum XOR, colas de órdenes, frecuencias, intensidad, tonos mono y selección temporal de segmentos. Estas pruebas no requieren un teléfono ni compilan la interfaz Android.

## Compilación Android

Con JDK 17 y Android SDK Platform 35 instalados:

```text
./gradlew testDebugUnitTest assembleDebug
```

En Windows utiliza `gradlew.bat`. La compilación completa requiere descargar las dependencias de Gradle y disponer del SDK. Las pruebas de núcleo independientes no sustituyen la compilación del APK ni las pruebas en un dispositivo.

## Referencia Python

El archivo Python de referencia supera la comprobación sintáctica. Su procesamiento de audio con dependencias científicas no se ejecutó y no forma parte de la ruta Android actual.

## Comprobación física

Para verificar BLE, audio, permisos y vibración real, instala la app en un teléfono y sigue la prueba manual de [LEEME_PRIMERO.md](LEEME_PRIMERO.md). Los valores calculados del firmware no son una medición de la frecuencia mecánica del actuador.

## Resultados

El 13 de noviembre de 2026 se generó el APK de desarrollo y se ejecutaron las 13 pruebas Gradle sin fallos. Las suites Java y la interoperabilidad Java/C++ también pasaron; el detalle está en [la validación técnica del repositorio](../docs/VALIDACION.md).
