# VITAIROS Reportes — MVP académico (DSY1105)

App Android nativa (Kotlin + Jetpack Compose + Material 3), minSdk 26 (Android 8.0), solo teléfono, orientación vertical.

## Ejecutar
- Abrir la carpeta en Android Studio y ejecutar `app`, o `gradlew.bat assembleDebug` (JDK 17). APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Tests: `gradlew.bat testDebugUnitTest`.
- Inicie los dos servicios y configure las claves de operador y supervisor según la [guía del backend](backend/README.md). En Android, ingrese con el correo de la cuenta y la clave que configuró.

## Qué funciona de verdad
| Requisito | Implementación |
|---|---|
| Almacenamiento local | **Room** (reportes, adjuntos, trazabilidad) + **DataStore** (sesión y preferencias). La cola y el historial sobreviven al cerrar la app; lo que estaba "Enviando" vuelve a "En cola" al reabrir. |
| Cámara y archivos | Cámara del sistema (`TakePicture` + `FileProvider`), selector de imágenes y de PDF. Los archivos se copian a almacenamiento privado, las imágenes se optimizan (máx. 2000 px, JPEG 80 %) y se pueden rotar. Vista previa real de imagen y de la primera página del PDF. |
| GPS | Ubicación opcional en el reporte (`LocationManager`, permiso en tiempo de ejecución, maneja permiso denegado / GPS apagado / sin señal). |
| Formularios validados | Errores en línea en login, nuevo reporte (listas fijas, observaciones obligatorias en incidentes, máx. 300 caracteres) y datos extraídos (fecha y número). Lógica en `ui/Validators.kt` con tests. |
| Estado | `ReportsViewModel` con flujo unidireccional: Room es la fuente de verdad, escrituras en orden y estado optimista. Conectividad real (`NetworkCallback`) + interruptor demo. |
| Navegación | Transiciones entre pantallas, pestañas con fundido, confirmación al descartar borradores, sesión persistente. |
| Animaciones | Entrada escalonada, chips de estado animados, icono "Enviando" giratorio, banner de conexión, progreso de pasos, contadores, escala de botones, reordenado de listas, deslizar para eliminar. |

## Conexión entre Android y Spring

El APK debug usa `http://10.0.2.2:8081` para identidad y `http://10.0.2.2:8082` para reportes. En el emulador estándar estas direcciones llegan al equipo de desarrollo. Inicie ambos servicios antes de ingresar. Para cambiar los destinos, defina `IDENTITY_URL` y `REPORTS_URL` en las propiedades de Gradle y vuelva a compilar. Use los orígenes sin el sufijo `/api/v1`. En builds de distribución configure servicios HTTPS.

Para un teléfono conectado por USB, ejecute `adb reverse tcp:8081 tcp:8081` y `adb reverse tcp:8082 tcp:8082` y compile debug con `-PIDENTITY_URL=http://127.0.0.1:8081 -PREPORTS_URL=http://127.0.0.1:8082`.

Con ubicación registrada, el formulario permite consultar el clima actual antes del envío. El operador puede guardar borradores, enviar adjuntos y corregir reportes observados. El supervisor puede refrescar el historial, descargar los archivos y solicitar una corrección desde el detalle. El detalle muestra el clima capturado por el servidor cuando hay una lectura disponible. Solo la extracción con IA sigue simulada.

La caché Room se separa por cuenta. Una sesión vigente permite trabajar sin red y los cambios se envían al recuperar conexión o al pulsar **Sincronizar todo**. El historial se puede actualizar manualmente para ver cambios de otros dispositivos.

### Prueba de integración Android

Con un emulador dedicado y ambos servicios apuntando a bases de prueba, ejecute el siguiente comando desde PowerShell. Crea y sincroniza un reporte, descarga su adjunto desde la cuenta del supervisor y verifica la corrección y el reenvío. Use las claves temporales configuradas en identidad.

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.operatorPassword=$env:OPERATOR_PASSWORD" "-Pandroid.testInstrumentationRunnerArguments.supervisorPassword=$env:SUPERVISOR_PASSWORD"
```

## Estructura
`model/` modelos · `data/local/` Room · `data/SettingsStore` DataStore · `data/MockData` catálogo inicial sin conexión y datos de extracción simulada · `ui/ReportsViewModel` estado · `ui/Validators` · `ui/theme` paleta VITAIROS · `ui/components` · `ui/screens` · `ui/navigation` · `util/` imágenes, GPS y conectividad.

## Notas
- Versiones: Room 2.8.5, DataStore 1.2.1, KSP 2.3.12 (todas estables).
