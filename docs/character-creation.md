# Creación guiada de personajes

El creador prepara una ficha base. Se elige una especie, una clase y un método de atributos antes de ver las reglas y los datos finales.

## Atributos base

- Valores estándar: asignar 15, 14, 13, 12, 10 y 8 a los seis atributos. Cada valor se usa una vez.
- Compra de puntos: repartir 27 puntos. Los valores van de 8 a 15. Los costes respectivos son 0, 1, 2, 3, 4, 5, 7 y 9. El reparto equilibrado propone 13, 13, 13, 12, 12 y 12.
- Tiradas: tirar cuatro dados de seis caras, descartar uno de los menores y sumar los otros tres. Repetir seis veces y asignar los seis resultados. Dos resultados iguales siguen siendo dos valores disponibles distintos.

Estos son valores base. El paso de trasfondo aplica por separado los aumentos de 2024. La especie no agrega aumentos de atributos. La guía de especie y clase explica sus elecciones pendientes. Los beneficios del trasfondo se resuelven en sus pasos específicos cuando la API ofrece esas capacidades.

## Alcance del guardado

El creador conserva los extras que recibe `MainActivity`. Envía nombre, especie, clase, nivel, alineación y seis atributos finales a `POST /characters`. Si se elige trasfondo, agrega la decisión versionada `creation` que valida el backend actualizado; si se difiere, conserva el contrato anterior.

Después de crear el personaje, se completan los valores compatibles en **Completar ficha** y los conjuros en el grimorio. La automatización de los rasgos y decisiones de clase y especie requiere ampliar el motor y la persistencia. Este flujo no certifica una ficha completa conforme al manual.

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

## Información contextual y recomendaciones

Especie y clase usan selectores desplegables. Solo se muestra el panel de la selección actual; los detalles extensos se abren a pedido. El catálogo de clases de `GET /catalog/creation` v1 aporta prioridades, dado de golpe, salvaciones y rasgos disponibles de nivel 1. Su fuente es SRD 2024, no el manual completo ni un motor de progresión.

Los atributos se editan en filas horizontales. Cada resultado de tirada conserva su identidad, aunque varios tengan el mismo valor, y el desplegable indica dónde está usado. La recomendación de clase es opcional y editable: cargar el catálogo o cambiar de clase nunca reemplaza el reparto. En compra propone un arreglo que cuesta 27 puntos; en estándar y tiradas ordena los valores disponibles.

Esta versión requiere el catálogo nuevo para mostrar recomendaciones. Si no está disponible, ofrece reintentar y conserva el flujo base. El endpoint de preview del backend queda preparado para la futura integración de decisiones persistidas; este cliente todavía no lo utiliza.

## Trasfondos de nivel 1

Después de asignar los atributos, el asistente agrega un paso de trasfondo. El catálogo compartido ofrece Acólito, Criminal, Sabio y Soldado. Se elige +2 y +1 en dos atributos permitidos, o +1 en los tres. Cambiar de trasfondo limpia sus aumentos y elecciones de equipo/dote; conserva los valores base y las tiradas.

El resumen distingue base, aumento y total con su modificador. Al guardar, la API valida la decisión y conserva su procedencia. En la ficha, «Origen de los atributos» permite consultar ese desglose y las reducciones temporales sin recalcular bonificaciones sobre el total guardado.

Las capacidades del catálogo determinan qué beneficios se pueden resolver; los que no se eligen quedan pendientes explícitos. El flujo con trasfondo admite nivel 1. «Completar trasfondo más adelante» mantiene el alta anterior sin aumentos ni decisiones de trasfondo. Las fichas existentes no se migran ni reciben bonificaciones nuevas.

Depende del catálogo con `backgroundDecisionsVersion: 1` y del alta con `creation`. Si el servidor no ofrece la capacidad, se puede reintentar o continuar explícitamente sin trasfondo. La API protege las decisiones guardadas frente a cambios directos de atributos/clase/especie/nivel hasta incorporar un flujo de corrección y progresión.

## Competencias concedidas por el trasfondo

Cuando el catálogo anuncia `backgroundSkillsVersion: 1`, el creador informa que las dos competencias de habilidad del trasfondo se aplicarán al guardar. La API determina sus claves y calcula su bono. Las pruebas de habilidad muestran el trasfondo de procedencia.

En la configuración, «Trasfondo (competente)» conserva la competencia concedida sin agregar otra fuente manual; «Competencia adicional» no duplica el bono, y «Pericia» usa el rango superior. Se conserva la presentación anterior cuando el servidor no devuelve los campos nuevos. Las fichas creadas antes de esta capacidad mantienen sus competencias existentes y los avisos de beneficios pendientes.

## Equipo y dote de origen

Con las capacidades `backgroundLoadoutVersion` y `originFeatsVersion` de la API, el asistente muestra dos pasos adicionales después del trasfondo. Primero permite elegir paquete A o 50 po y la herramienta cuando corresponde; después explica la dote y pide únicamente sus decisiones. Iniciado en la magia requiere característica, dos trucos distintos y un conjuro de nivel 1 de su lista. Se conservan al retroceder y al recrear el proceso. Las descripciones largas se expanden por elección.

La ficha permite pruebas de herramientas, administrar monedas e iniciativa. El grimorio ofrece el lanzamiento gratuito de la dote y muestra su disponibilidad; un descanso largo válido lo recupera. Atacante salvaje exige declarar el turno y confirmar el impacto/critico; los objetivos y modificadores se resuelven en la mesa. Los conjuros concedidos quedan protegidos frente al editor genérico.

Sin estas capacidades se conserva el flujo anterior. Las fichas antiguas no reciben concesiones retroactivas. Siguen pendientes elecciones completas de clase/especie, progresión y reemplazos de conjuros de la dote al subir de nivel. Esta entrega usa el catálogo SRD, no todo el Manual del Jugador.

## Habilidades iniciales de clase

Con `classSkillsVersion: 1` y un trasfondo elegido, el asistente agrega un paso después de la guía de clase. Permite seleccionar exactamente el cupo indicado entre las opciones de esa clase, conserva elecciones tras recreación del proceso y las limpia al cambiar de clase. El resumen enumera las habilidades elegidas y la ficha muestra su procedencia. El bono no se duplica con el del trasfondo; la configuración conserva las concesiones.

El contrato anterior se conserva si se difiere el trasfondo o la API no anuncia la capacidad. Pericia, sustituciones de habilidades duplicadas y demás decisiones de clase siguen pendientes.

## Base de clase de nivel 1

Con `classBasicsVersion: 1`, el resumen muestra PV base (máximo del dado + Constitución final, mínimo 1) y salvaciones concedidas. La API inicializa esos PV una sola vez y conserva su fuente. La configuración de salvaciones identifica las concesiones de clase y evita borrarlas o convertirlas en una segunda fuente manual.

Los aumentos por especie, dotes u otras fuentes todavía deben revisarse al completar la ficha. El cálculo mostrado no certifica el máximo definitivo ni habilita automáticamente una ficha incompleta. No modifica personajes anteriores.
