# DNDcompanion — Android

Aplicación Java para jugadores de D&D. Esta rama reemplaza Firebase Auth/Firestore por **Supabase Auth + API REST Express**, compartidos con Campaign Hub React. Prisma ORM reside únicamente en el backend.

## Configuración

1. Abrir con Android Studio compatible con el proyecto (AGP 9.1.1, Gradle 9.3.1, JDK 21 y SDK configurado por `app/build.gradle.kts`).
2. Agregar los valores de `local.properties.example` a tu `local.properties`, conservando `sdk.dir`. También se pueden usar variables de entorno del mismo nombre.
3. Configurar el mismo proyecto Supabase que usan React y el backend. Solo incluir la clave **publishable**, nunca `service_role`, claves secretas o credenciales PostgreSQL.
4. Iniciar Express con la migración Prisma aplicada. Emulador: `http://10.0.2.2:3001/api`. Dispositivo físico/release: URL HTTPS accesible. El cliente solo permite HTTP en debug hacia emulador/loopback.
5. Registrar una cuenta nueva. Si Supabase exige confirmación de correo, abrir el enlace en el navegador y luego volver a ingresar. Configurar la Site URL de Auth con la web; no se implementan deep links en esta entrega.

```sh
./gradlew :app:assembleDebug
```

Ya no se requieren Firebase ni `google-services.json`. Los usuarios/datos de prueba anteriores no se importan.

## Funciones integradas

- Login/registro/cierre de sesión con Supabase Auth REST, manteniendo Java.
- Access/refresh tokens cifrados con Android Keystore, excluidos de backups y transferencia.
- Renovación de sesión serializada para evitar conflictos de rotación del refresh token; reintento único de una petición que devuelva 401.
- Perfil propio creado por Express tras el primer login.
- Misiones asignadas desde la web: cargar, aceptar por ID y recuperar su estado tras reiniciar. La misión solo cambia en pantalla después de confirmarse la operación del servidor.
- Botón **Actualizar** para consultar cambios del DM. También se recarga al volver a la pantalla principal.
- Personajes: crear por la API y recuperar el más reciente del usuario. Una cuenta puede conservar varios; el selector y vinculación con campaña en la interfaz quedan pendientes (el backend ya admite la relación).
- Se conservan dados, catálogo de hechizos/Open5e, avatar local, notas e inventario de la interfaz existente.

## Límites explícitos

No hay sincronización offline, notificaciones ni realtime. Notas/inventario/avatar no forman parte de la persistencia nueva. XP/oro de misión se muestran; la acreditación automática al personaje y el cálculo de nivel quedan para la siguiente etapa. Crear un personaje no otorga acceso a campañas: debe agregarte el DM.

## Validación y prueba manual

El backend incluye pruebas de integración de persistencia y permisos. Para verificar esta app con servicios reales:

1. Crear cuenta jugador y entrar una vez (crea el perfil).
2. Desde React, el DM agrega ese email a su campaña y asigna una misión.
3. En Android, actualizar y aceptar. Confirmar en React la aceptación.
4. Completar desde React y actualizar Android; reiniciar para verificar que persiste.
5. Crear un personaje y reiniciar: se recupera desde `/characters`.
6. Cerrar sesión y comprobar que vuelve al login.

La compilación completa de Gradle/APK no pudo ejecutarse en el entorno de edición por acceso de red a Gradle. Debe confirmarse en Android Studio antes de integrar la rama.

Ver [contrato REST y configuración del backend](https://github.com/Krayxzlim/dnd-campaign-hub-backend/tree/feat/supabase-prisma-integration/docs/integration.md).
