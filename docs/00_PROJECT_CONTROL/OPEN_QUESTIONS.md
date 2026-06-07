# OPEN_QUESTIONS -- humanOS Native Android

> Preguntas abiertas que requieren input de Felipe o del equipo. Cada pregunta tiene un supuesto operativo que se usa hasta que se confirme o se descarte.

---

## Q-001: Confirmar Firebase Project definitivo

- **Fecha abierta:** 2026-06-06
- **Estado:** Abierta
- **Pregunta:** El proyecto Firebase a usar es `humanos-app`? O se necesita un proyecto Firebase separado para la app Android?
- **Supuesto operativo:** Se usa el GCP project `humanos-app` existente. La app Android se registra como una "app" adicional dentro del mismo proyecto Firebase (junto a la web app si existe).
- **Contexto:** Usar el mismo proyecto Firebase simplifica Auth (mismos usuarios), Firestore (si aplica), y billing. Pero RISK-006 identifica que compartir proyecto podria generar problemas de quota si los servicios existentes ya consumen recursos.
- **Impacto si el supuesto es incorrecto:** Habria que crear un proyecto Firebase nuevo, generar otro `google-services.json`, y potencialmente manejar auth cross-project.
- **Quien decide:** Felipe
- **Refs:** RISK-006

---

## Q-002: Confirmar CI auth strategy

- **Fecha abierta:** 2026-06-06
- **Estado:** Abierta
- **Pregunta:** Para autenticacion de GitHub Actions con GCP/Firebase, se usa Workload Identity Federation (WIF) o una Service Account key?
- **Supuesto operativo:** WIF es el metodo preferido (sin secretos long-lived). Si WIF no es viable por limitaciones del plan GCP, se usa una SA key almacenada como GitHub Secret como excepcion documentada.
- **Contexto:** WIF es la recomendacion de Google para CI/CD porque no requiere exportar claves privadas. Sin embargo, requiere configuracion en GCP IAM que Felipe debe autorizar. Una SA key es mas simple de configurar pero es un secreto long-lived que debe rotarse.
- **Impacto si el supuesto es incorrecto:** Si se usa SA key, hay que documentar politica de rotacion y agregarla a RISKS.
- **Quien decide:** Felipe
- **Refs:** DEC-005

---

## Q-003: Confirmar applicationId / package name definitivo

- **Fecha abierta:** 2026-06-06
- **Estado:** Abierta
- **Pregunta:** Cual es el `applicationId` definitivo para la app? Este ID es **permanente** una vez que se sube el primer build a Google Play o Firebase App Distribution.
- **Supuesto operativo:** `eco.humanos.android` (reverse domain de `humanos.eco` + qualifier `android`).
- **Alternativas consideradas:**
  - `eco.humanos.android` -- alineado con dominio `humanos.eco`, limpio
  - `com.sinapsis.humanos` -- dominio corporativo de Sinapsis SpA
  - `app.humanos.android` -- generico
  - `eco.humanos.app` -- reverse domain con qualifier `app`
- **Contexto:** El applicationId aparece en Google Play Store URL, en el AndroidManifest, en Firebase, y en todos los deep links. Cambiarlo despues del primer upload requiere publicar una app completamente nueva. El Kotlin package name interno puede diferir del applicationId, pero por convencion se alinean.
- **Impacto si el supuesto es incorrecto:** Requiere renombrar packages en todo el proyecto, re-registrar en Firebase, y si ya se subio a Play Store, crear un listing nuevo.
- **Quien decide:** Felipe
- **Refs:** TASK-009, RISK-007

---

## Q-004: Confirmar si existe Google Play Console Sinapsis SpA

- **Fecha abierta:** 2026-06-06
- **Estado:** Abierta
- **Pregunta:** Existe una cuenta de Google Play Console registrada a nombre de Sinapsis SpA? Si no existe, se necesita crear una (requiere pago unico de USD 25 y verificacion de identidad empresarial).
- **Supuesto operativo:** No existe aun, pero no bloquea el skeleton ni Phase 1. Se puede distribuir betas via Firebase App Distribution sin Google Play Console. La cuenta de Play Console se necesita para produccion.
- **Contexto:** Google Play Console para organizaciones requiere verificacion con DUNS number o documentos legales. El proceso puede tomar 1-2 semanas. Si Sinapsis no tiene DUNS, hay que registrar uno primero.
- **Impacto si el supuesto es incorrecto:** Si ya existe una cuenta, se puede saltar el registro. Si no existe y no se inicia pronto, podria retrasar el lanzamiento en Play Store.
- **Quien decide:** Felipe
- **Refs:** DEC-005

---

## Indice rapido

| ID | Pregunta | Estado | Quien decide |
|----|----------|--------|--------------|
| Q-001 | Firebase project definitivo | Abierta | Felipe |
| Q-002 | CI auth strategy (WIF vs SA key) | Abierta | Felipe |
| Q-003 | applicationId definitivo | Abierta | Felipe |
| Q-004 | Google Play Console existe? | Abierta | Felipe |
