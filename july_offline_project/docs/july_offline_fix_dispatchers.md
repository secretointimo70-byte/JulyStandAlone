# FIX — CoroutineDispatchers open properties
## Aplica antes de FASE 5

### Problema

`TestCoroutineDispatchers` en FASE 4 hereda de `CoroutineDispatchers` pero
las propiedades `val main`, `val io`, `val default`, `val unconfined` están
declaradas sin `open` en la clase base. Kotlin no permite sobrescribir
propiedades `val` sin `open`. Resultado: error de compilación en tests.

```
error: 'main' hides member of supertype 'CoroutineDispatchers' and needs 'override' modifier
error: 'main' overrides nothing
```

### Solución

Dos cambios coordinados:
1. Declarar las propiedades como `open` en `CoroutineDispatchers`
2. Añadir `override` en `TestCoroutineDispatchers`

---

### `core/coroutines/CoroutineDispatchers.kt` (reemplaza FASE 2)

```kotlin
package com.july.offline.core.coroutines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstracción de dispatchers inyectable.
 * Las propiedades son `open` para permitir override en TestCoroutineDispatchers.
 */
@Singleton
open class CoroutineDispatchers @Inject constructor() {
    open val main: CoroutineDispatcher = Dispatchers.Main
    open val io: CoroutineDispatcher = Dispatchers.IO
    open val default: CoroutineDispatcher = Dispatchers.Default
    open val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
}
```

---

### `testutil/TestCoroutineDispatchers.kt` (reemplaza FASE 4)

```kotlin
package com.july.offline.testutil

import com.july.offline.core.coroutines.CoroutineDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler

/**
 * CoroutineDispatchers de test con override correcto.
 * Todos los dispatchers usan el mismo TestDispatcher para control
 * determinístico del tiempo en runTest { }.
 */
class TestCoroutineDispatchers(
    val scheduler: TestCoroutineScheduler = TestCoroutineScheduler()
) : CoroutineDispatchers() {

    private val testDispatcher = StandardTestDispatcher(scheduler)

    override val main: CoroutineDispatcher = testDispatcher
    override val io: CoroutineDispatcher = testDispatcher
    override val default: CoroutineDispatcher = testDispatcher
    override val unconfined: CoroutineDispatcher = testDispatcher
}
```

---

### Impacto

| Archivo | Cambio |
|---|---|
| `core/coroutines/CoroutineDispatchers.kt` | Clase `open` + propiedades `open` |
| `testutil/TestCoroutineDispatchers.kt` | Propiedades con `override` |

Sin otros cambios en el resto del proyecto. Todos los tests de FASE 4
compilan correctamente tras aplicar este fix.
