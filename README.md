# VITAIROS Reportes — MVP académico (DSY1105)

App Android nativa (Kotlin + Jetpack Compose + Material 3), minSdk 26 (Android 8.0), solo teléfono, orientación vertical.

## Ejecutar
- Abrir la carpeta en Android Studio y ejecutar `app`, o `gradlew.bat assembleDebug` (JDK 17). APK: `app/build/outputs/apk/debug/app-debug.apk`.
- Tests: `gradlew.bat testDebugUnitTest`.
- Login demo: `operador@vitairos.demo` / `demo1234` (o los botones de acceso rápido).

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

## Qué sigue simulado
API de envío (mock con 3 reintentos automáticos de 5/15/30 s y reintento manual), extracción con IA («Simulación»), estados "En revisión/Procesado", rol Supervisor y sesión con usuarios ficticios. No hay backend ni datos reales.

## Estructura
`model/` modelos · `data/local/` Room · `data/SettingsStore` DataStore · `data/MockData` catálogos y datos de ejemplo (se siembran en la primera ejecución) · `ui/ReportsViewModel` estado · `ui/Validators` · `ui/theme` paleta VITAIROS · `ui/components` · `ui/screens` · `ui/navigation` · `util/` imágenes, GPS y conectividad.

## Notas
- Al abrir la app la cola no se sincroniza sola; los pendientes se envían al recuperar la conexión o con "Sincronizar todo".
- Versiones: Room 2.8.5, DataStore 1.2.1, KSP 2.3.12 (todas estables).
