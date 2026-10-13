# Creación guiada de personajes

El creador prepara una ficha base. Se elige una especie, una clase y un método de atributos antes de ver las reglas y los datos finales.

## Atributos base

- Valores estándar: asignar 15, 14, 13, 12, 10 y 8 a los seis atributos. Cada valor se usa una vez.
- Compra de puntos: repartir 27 puntos. Los valores van de 8 a 15. Los costes respectivos son 0, 1, 2, 3, 4, 5, 7 y 9. El reparto equilibrado propone 13, 13, 13, 12, 12 y 12.
- Tiradas: tirar cuatro dados de seis caras, descartar uno de los menores y sumar los otros tres. Repetir seis veces y asignar los seis resultados. Dos resultados iguales siguen siendo dos valores disponibles distintos.

Estos valores no incluyen los aumentos del trasfondo de 2024. La especie no agrega aumentos de atributos. La guía de especie y clase explica las elecciones pendientes, pero no registra elecciones de rasgos, dotes, equipo o conjuros.

## Alcance del guardado

El creador conserva el contrato de `POST /characters` y los extras que recibe `MainActivity`. Envía nombre, especie, clase, nivel, alineación y seis atributos. No envía campos adicionales que el backend rechazaría.

Después de crear el personaje, se completan los valores compatibles en **Completar ficha** y los conjuros en el grimorio. La automatización de todos los rasgos y trasfondos requiere ampliar el motor y la persistencia. Este flujo no certifica una ficha completa conforme al manual.

## Verificación

Desde la raíz del proyecto, con JDK 21 y el SDK indicado por Gradle:

```sh
bash gradlew --no-daemon assembleDebug testDebugUnitTest lintDebug
```

Para comprobar el flujo con la API real:

1. Elegir una especie y una clase. Avanzar y volver para comprobar que se conservan.
2. Probar cada método. Completar el reparto y cambiar de método antes de volver al anterior.
3. Rotar la pantalla después de tirar los dados. Comprobar que no cambian los resultados ni las asignaciones.
4. Revisar las guías y completar el nombre. Comparar el resumen con los valores elegidos.
5. Crear el personaje. Comprobar la ficha recibida y volver a abrir la app para comprobar los datos guardados.
6. Probar un fallo de conexión y reintentar sin perder las elecciones.
