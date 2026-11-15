# Vibrafil Dual 5.1

Basado en la versión Dual 5.0 del proyecto.

- Inicio en FREQ; las notas v3/v4 cambian el temporizador.
- Intensidad v4 separada de niveles base y posterior a GAIN; conserva v1/v2/v3.
- Extensión BLE de capacidades en UUID con prefijo abcdefaf.
- STOP apaga el duty y conserva el último temporizador configurado.
- Logs incluyen intensidad, duty y frecuencia eléctrica calculada.
- `FREQDUTY` configurable entre 50 y 90 %; inicia en 75 % para aumentar amplitud sin cambiar los Hz de la nota. `SAVE` conserva GAIN y FREQDUTY.
- GPIO27/GPIO16; voicecoils y sensores siguen inactivos.

La respuesta mecánica del montaje no se ha medido.
