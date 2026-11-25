# Backend de VITAIROS

Dos aplicaciones Spring Boot independientes para el MVP Android. Cada servicio tiene una base H2 persistente en su directorio de trabajo. Con Gradle `bootRun`, los archivos quedan en `backend/<servicio>/data/`. El servicio de reportes consulta la sesión en identidad en cada petición.

## Ejecutar en Windows

Use JDK 17 o superior y ejecute los comandos desde la raíz del repositorio. Para compilar, ejecutar la prueba HTTP y generar los JAR:

```powershell
.\gradlew.bat -p backend build
```

En la primera terminal, defina dos claves locales y arranque identidad. Cada cuenta se habilita al configurar su clave.

```powershell
$env:OPERATOR_PASSWORD = Read-Host 'Clave local del operador'
$env:SUPERVISOR_PASSWORD = Read-Host 'Clave local del supervisor'
.\gradlew.bat -p backend :identity-service:bootRun
```

En otra terminal:

```powershell
.\gradlew.bat -p backend :report-service:bootRun
```

Por defecto ambos escuchan en loopback. En cada proceso puede configurar `PORT`, `BIND_ADDRESS` y `DATABASE_URL` (URL JDBC H2). En reportes, `IDENTITY_URL` indica el origen de identidad. Para ejecutar los JAR con `java -jar`, use un directorio de trabajo estable por servicio para conservar sus datos.

## Contrato HTTP v1

Todas las rutas siguientes tienen el prefijo `/api/v1`. Las respuestas y los cuerpos son JSON salvo la carga multipart y la descarga binaria. Tras iniciar sesión use `Authorization: Bearer <accessToken>`.

| Servicio | Método y ruta | Uso |
|---|---|---|
| Identidad · 8081 | POST /sessions | Recibe `email` y `password`. Devuelve 201 con `accessToken`, `expiresAt` y `user` (correo, nombre y rol). La sesión dura ocho horas. |
| Identidad | GET /me · DELETE /sessions/current | Consultar el perfil o cerrar la sesión (204). |
| Reportes · 8082 | GET /catalog | Tipos, procesos, áreas y turnos admitidos por el formulario. |
| Reportes | PUT /reports/{id} | Crear o editar el formulario. Mantener el mismo identificador en los reintentos. Devuelve el detalle. |
| Reportes | GET /reports · GET /reports/{id} | Historial ordenado del más reciente al más antiguo y detalle. El operador ve sus reportes y el supervisor puede consultar todos. |
| Reportes | PUT /reports/{id}/attachments/{fileId} | Subir o reemplazar un archivo mediante el campo multipart `file`. Hasta 5 por reporte, 10 MiB por archivo, JPG, PNG o PDF. Devuelve metadatos. |
| Reportes | GET o DELETE /reports/{id}/attachments/{fileId} | Descargar el contenido o quitar el adjunto. |
| Reportes | PUT /reports/{id}/submission | Enviar o reenviar sin cuerpo. Requiere al menos un adjunto. Repetir el envío de un reporte sincronizado conserva su contador y su historial. |
| Reportes | PUT /reports/{id}/correction | El supervisor devuelve un reporte sincronizado con `{"note":"Revise el turno"}`. El propietario puede editarlo y reenviarlo. |
| Reportes | DELETE /reports/{id} | Eliminar un borrador propio junto con sus archivos y eventos (204). |

Las cuentas del MVP son `operador@vitairos.demo` y `supervisor@vitairos.demo`. Un ejemplo de formulario para `PUT /reports/{id}`:

```json
{
  "type": "Reporte de turno v1",
  "process": "Molienda",
  "area": "Planta",
  "shift": "Día administrativo",
  "notes": "Revisión del molino al cierre de turno",
  "latitude": -23.65,
  "longitude": -70.40
}
```

Las coordenadas son opcionales y se envían juntas. Las observaciones admiten hasta 300 caracteres y son obligatorias en incidentes. Los identificadores de reporte y archivo admiten hasta 80 letras ASCII, dígitos, guiones y guiones bajos. Para varios dispositivos conviene generar un UUID al crear el borrador y conservarlo durante toda la sincronización.

El detalle tiene la forma `{report, attachments, timeline, weather}`. Las fechas usan ISO 8601 con zona. El servidor registra los estados `BORRADOR`, `SINCRONIZADO` y `REQUIERE_CORRECCION`. El teléfono gestiona la cola, los fallos de transporte y sus reintentos. El campo `attempts` del servidor cuenta los envíos aceptados, incluyendo reenvíos después de una corrección. La extracción de campos sigue siendo una simulación local del MVP.

### Clima del reporte

Al crear el reporte en el servidor, `report-service` consulta las condiciones actuales para la latitud y longitud del formulario. Guarda las coordenadas consultadas, la hora UTC del dato del proveedor (`observedAt`), la temperatura en °C (`temperatureC`), la humedad relativa en % (`humidityPercent`), el viento en km/h (`windSpeedKmh`) y el código de condición WMO (`weatherCode`). Los campos `source` y `sourceUrl` permiten mostrar la atribución junto a los datos. Son estimaciones meteorológicas de modelo. [Documentación de Open-Meteo](https://open-meteo.com/en/docs)

El detalle conserva la lectura original incluso si luego se edita la ubicación. Si al crear el reporte faltan coordenadas o el proveedor falla, el guardado continúa con `weather: null`. Los reportes anteriores también devuelven `null`. Para un borrador creado sin conexión en Android, la captura corresponde al momento de su primera creación en el backend.

Para consultar el clima por separado, use `GET /api/v1/weather/current?latitude=-33.45&longitude=-70.66` en el puerto 8082 con el mismo token Bearer. Devuelve el mismo objeto meteorológico, 400 para coordenadas inválidas o 503 si el proveedor está indisponible. Esta consulta es independiente del historial.

La configuración predeterminada usa la API pública de Open-Meteo, gratuita para uso no comercial y sin clave. El servidor necesita salida HTTPS a `api.open-meteo.com`. En pruebas se puede configurar `WEATHER_URL` como el origen de un proveedor compatible. [Condiciones de Open-Meteo](https://open-meteo.com/)

Los errores de formulario devuelven 400, la sesión inválida 401, las acciones reservadas al supervisor 403 y los recursos inexistentes o ajenos al operador 404. Los conflictos de estado o cantidad devuelven 409, las cargas demasiado grandes 413 y la indisponibilidad de identidad 503.
