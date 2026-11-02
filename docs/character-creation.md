# Creación guiada de personajes

El creador guía la elección de especie, clase, atributos y beneficios iniciales. Con la API actualizada, termina con protección y un resumen calculado de PV, CA, iniciativa y velocidad; esos valores ya quedan guardados al crear.

## Atributos base

- Valores estándar: asignar 15, 14, 13, 12, 10 y 8 a los seis atributos. Cada valor se usa una vez.
- Compra de puntos: repartir 27 puntos. Los valores van de 8 a 15. Los costes respectivos son 0, 1, 2, 3, 4, 5, 7 y 9. El reparto equilibrado propone 13, 13, 13, 12, 12 y 12.
- Tiradas: tirar cuatro dados de seis caras, descartar uno de los menores y sumar los otros tres. Repetir seis veces y asignar los seis resultados. Dos resultados iguales siguen siendo dos valores disponibles distintos.

Estos son valores base. El paso de trasfondo aplica por separado los aumentos de 2024. La especie no agrega aumentos de atributos. La guía de especie y clase explica sus elecciones pendientes. Los beneficios del trasfondo se resuelven en sus pasos específicos cuando la API ofrece esas capacidades.

## Alcance del guardado

El creador conserva los extras que recibe `MainActivity`. Envía nombre, especie, clase, nivel, alineación y seis atributos finales a `POST /characters`. Agrega la decisión versionada `creation` que valida el backend actualizado. El nuevo recorrido exige trasfondo y sus beneficios antes de calcular la ficha; el contrato anterior sigue disponible con servidores que no anuncian la nueva capacidad.

Los personajes del nuevo recorrido ya no necesitan entrar en **Completar ficha** para cargar PV, CA, iniciativa o velocidad. Siguen pendientes las elecciones completas de conjuros de clase, armas y otros rasgos de clase/especie. El creador informa esos límites en el resumen; no certifica una ficha completa conforme al manual. Las fichas antiguas conservan su estado y configuración.

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

Esta versión requiere el catálogo nuevo para mostrar recomendaciones. Si no está disponible, ofrece reintentar y conserva el flujo base. Con `startingDefenseVersion: 1`, el resumen utiliza el endpoint de preview del backend antes de habilitar el guardado.

## Trasfondos de nivel 1

Después de asignar los atributos, el asistente agrega un paso de trasfondo. El catálogo compartido ofrece Acólito, Criminal, Sabio y Soldado. Se elige +2 y +1 en dos atributos permitidos, o +1 en los tres. Cambiar de trasfondo limpia sus aumentos y elecciones de equipo/dote; conserva los valores base y las tiradas.

El resumen distingue base, aumento y total con su modificador. Al guardar, la API valida la decisión y conserva su procedencia. En la ficha, «Origen de los atributos» permite consultar ese desglose y las reducciones temporales sin recalcular bonificaciones sobre el total guardado.

Las capacidades del catálogo determinan qué beneficios se pueden resolver; los que no se eligen quedan pendientes explícitos. El flujo con trasfondo admite nivel 1. «Completar trasfondo más adelante» mantiene el alta anterior cuando el servidor no ofrece el cálculo de defensa inicial. Con esa capacidad se exige resolverlo para tener atributos y dinero definitivos. Las fichas existentes no se migran ni reciben bonificaciones nuevas.

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

Esta capacidad por sí sola no habilita una ficha incompleta. El recorrido de protección inicial descrito abajo agrega el aumento de PV del enano y configura los valores de juego al guardar. No modifica personajes anteriores.

## Pericia del pícaro

Con `rogueExpertiseVersion: 1`, el pícaro recibe un paso después de elegir habilidades de clase. Ofrece solamente las competencias concedidas por clase o trasfondo y exige dos elecciones distintas. La explicación distingue duplicar competencia de duplicar el modificador completo. El resumen enumera las pericias; la ficha conserva su concesión al configurar habilidades.

Cambiar las habilidades o el trasfondo quita las pericias que dejan de ser elegibles. Cambiar de clase limpia todas las elecciones de clase. Las decisiones válidas sobreviven a la recreación del proceso. Las pericias de otras clases o niveles todavía no están implementadas.

## Protección y ficha calculada

Con `startingDefenseVersion: 1`, después del nombre aparece un paso de protección. Se usa la alternativa de oro inicial de la clase, en lugar de su paquete de equipo, más el dinero del trasfondo. El usuario elige sin armadura o una armadura que pueda pagar y para la que tenga entrenamiento; puede agregar escudo si su clase lo admite. El coste se descuenta una sola vez y la protección queda equipada en el inventario. Las armas y el resto de compras de clase siguen pendientes.

El resumen consulta `POST /catalog/creation/background-preview` con las mismas decisiones que se enviarán al guardar. Muestra PV, CA, iniciativa, velocidad y dinero restante, y permite desplegar la explicación. No duplica las fórmulas del servidor. Cambiar una decisión invalida el resultado; una respuesta atrasada no habilita guardar otro reparto. Si falla la conexión, se conserva el borrador y se ofrece reintentar el cálculo. Restaurar el proceso requiere una nueva consulta.

El backend calcula PV de nivel 1 con Constitución final y el aumento del enano, iniciativa con Alerta cuando corresponda, velocidad de especie y CA según equipo o defensa sin armadura de monje/bárbaro. Una armadura pesada reduce la velocidad si falta Fuerza; su desventaja en Sigilo se informa, pero aún debe indicarse manualmente al tirar. Las elecciones adicionales de clase y especie permanecen señaladas como pendientes.

### Prueba con Android y API de testing

1. Crear un humano guerrero de nivel 1 con valores estándar: Fuerza 15, Destreza 14, Constitución 13, Inteligencia 12, Sabiduría 10 y Carisma 8.
2. Elegir Soldado, +2 Fuerza y +1 Constitución; elegir 50 po en el equipo del trasfondo, naipes y completar las habilidades de clase.
3. En protección, elegir cota de malla y escudo. El resumen debe indicar **12 PV, CA 18, iniciativa +2, velocidad 30 pies y 120 po restantes**.
4. Crear y reabrir la ficha. Debe conservar los valores, tener la armadura y el escudo equipados y permitir registrar daño sin pedir **Completar ficha**. Recibir 3 puntos de daño debe dejar 9 PV y conservarlos al recargar.
5. Antes de crear otro personaje, volver desde el resumen y cambiar de clase: debe limpiar la protección anterior y recalcular. Cortar la conexión durante el cálculo debe impedir guardar hasta un reintento correcto, sin perder las elecciones.

Verificación automatizada local: `assembleDebug`, `testDebugUnitTest` y `lintDebug`; 42 pruebas sin fallos. La suite cubre respuestas atrasadas, reintento, restauración, cambio de clase y ausencia del aviso de configuración para una ficha lista. No sustituye la prueba en un dispositivo físico.
