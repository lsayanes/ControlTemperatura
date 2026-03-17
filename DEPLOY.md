# PyrexLog — Guía de Deploy

## Instalar en teléfono de prueba

### Desde Android Studio
Conectar el teléfono por USB con depuración USB activada y presionar **Run ▶** (`Shift+F10`).

### Desde la terminal
```bash
cd /Users/lsayanes/ControlTemperatura
./gradlew installDebug
```

El APK debug usa el applicationId `com.pyrexlog.debug`, por lo que coexiste con la versión release sin pisarse.

---

## Preparar firma para Google Play

### 1. Generar el Keystore (una sola vez)

```bash
keytool -genkey -v \
  -keystore /Users/lsayanes/ControlTemperatura/pyrexlog.jks \
  -alias pyrexlog \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

> ⚠️ **Importante:** guardá el archivo `pyrexlog.jks` y las contraseñas en un lugar seguro.
> Si perdés el keystore no podés publicar actualizaciones de la app en Play Store.

### 2. Completar keystore.properties

Editar el archivo `keystore.properties` en la raíz del proyecto:

```
storeFile=../pyrexlog.jks
storePassword=TU_STORE_PASSWORD
keyAlias=pyrexlog
keyPassword=TU_KEY_PASSWORD
```

Este archivo está en `.gitignore` y nunca se sube al repositorio.

### 3. Generar el AAB (Android App Bundle)

```bash
./gradlew bundleRelease
```

El archivo resultante queda en:
```
app/build/outputs/bundle/release/app-release.aab
```

Este `.aab` es el que se sube a Google Play Console.

---

## Checklist Google Play

- [ ] **Privacy Policy URL** — Google la exige aunque la app guarde todo localmente. Se puede publicar una página simple en GitHub Pages.
- [ ] **Data Safety** en Play Console → declarar "No se recopilan datos" (almacenamiento 100% local).
- [ ] **Capturas de pantalla** — mínimo 2 para teléfonos.
- [ ] **Descripción corta** (máx. 80 caracteres).
- [ ] **Descripción larga**.
- [ ] **Categoría**: Salud y bienestar.
- [ ] **Clasificación de contenido**: completar el cuestionario (sin contenido sensible → apta para todos).
- [ ] `versionCode` y `versionName` en `app/build.gradle.kts` — actualizar en cada release.

---

## Versioning

| Campo | Valor actual | Descripción |
|---|---|---|
| `versionCode` | 1 | Entero que incrementa en cada release (1, 2, 3…) |
| `versionName` | "1.0" | String visible al usuario ("1.1", "2.0"…) |

Ambos se editan en `app/build.gradle.kts`.
