# DNDcompanion — Android

Aplicación Java para jugadores de D&D. Esta rama reemplaza Firebase Auth/Firestore por **Supabase Auth + API REST Express**, compartidos con Campaign Hub React. Prisma ORM reside únicamente en el backend.

## Configuración

1. Abrir con Android Studio compatible con el proyecto (AGP 9.1.1, Gradle 9.3.1, JDK 21 y SDK configurado por `app/build.gradle.kts`).
2. La URL y clave pública ya están en `config/supabase.public.json`, igual que en React y el backend. `local.properties` solo necesita el `sdk.dir` administrado por Android Studio. Debug usa `http://10.0.2.2:3001/api` para el emulador.
3. Para otro proyecto, sobrescribir juntas `SUPABASE_URL` y `SUPABASE_PUBLISHABLE_KEY` en `local.properties` o variables de entorno. Para un dispositivo físico/release configurar `API_BASE_URL` con HTTPS. Usar `local.properties.example` como referencia, conservando `sdk.dir`. Nunca incluir `service_role`, claves secretas o credenciales PostgreSQL.
4. Iniciar Express con la migración Prisma aplicada. Emulador: `http://10.0.2.2:3001/api`. Dispositivo físico/release: URL HTTPS accesible. El cliente solo permite HTTP en debug hacia emulador/loopback.
5. Registrar una cuenta nueva. Si Supabase exige confirmación de correo, abrir el enlace en el navegador y luego volver a ingresar. Configurar la Site URL de Auth con la web; no se implementan deep links en esta entrega.

```sh
./gradlew :app:assembleDebug
```

Ya no se requieren Firebase ni `google-services.json`. Los usuarios/datos de prueba anteriores no se importan.

Si aparece `package com.google.firebase.auth does not exist` en `CrearPersonajeActivity`, se está compilando una copia anterior o incompleta: la versión migrada importa `com.miapp.dndcompanion.network.ApiClient`. Actualizar la rama completa, sincronizar Gradle y reconstruir; no volver a agregar Firebase para ocultar el error. Conservar los cambios locales antes de cambiar de rama.

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

## Olvidé mi contraseña

En el login, ingresar el email y tocar **Olvidé mi contraseña**. Se solicita a Supabase el correo de recuperación; el mensaje no revela si la cuenta existe. Abrir el enlace, elegir y confirmar la nueva contraseña en la web y volver a Android para ingresar. No se usan deep links ni se necesita la contraseña anterior.

`PASSWORD_RESET_URL` en `local.properties` o en el entorno debe apuntar a la web actualizada con `/?recovery=1`, y esa URL exacta debe estar autorizada en **Supabase → Authentication → URL Configuration → Redirect URLs**. No incluir secretos.

Debug usa por defecto `http://localhost:5173/?recovery=1`: abrir el correo en la computadora donde corre Vite. Para abrirlo en el navegador del emulador, configurar `http://10.0.2.2:5173/?recovery=1`, autorizarla en Supabase y ejecutar Vite con `npm run dev -- --host 0.0.0.0`. Para un celular físico y release, configurar una web HTTPS accesible; localhost no apunta a la computadora desde un celular. Release exige configurar la URL antes de solicitar recuperación.

Se requiere integrar también el cambio de recuperación del frontend. La solicitud funciona directamente contra Supabase Auth, sin depender de Express. Después del cambio, una sesión antigua de Android puede expirar y solicitar login nuevamente.
