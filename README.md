# Supletanes App — Gestión de Suplementos y Seguimiento Nutricional

Aplicación móvil diseñada para ayudar a los usuarios a gestionar su inventario de suplementos, consultar información nutricional y realizar un seguimiento de su plan de alimentación.

## Autor

- **Christopher Espinoza**

## Repositorio y estructura del proyecto

- Rama utilizada para el pipeline CI/CD: `main`
- Frontend Android: `frontend/`
- Backend Spring Boot: `backend/`
- Workflow CI/CD: `.github/workflows/main.yml`
- Dockerfile: `backend/Dockerfile`
- Infraestructura AWS ECS: `backend/infra/ecs/`
- Evidencias Evaluación 3: `docs/evidencias/`

## Aplicación y documentación de la API

- **Swagger UI:** [Documentación y pruebas de endpoints](https://supletanesappfinal-production.up.railway.app/swagger-ui/index.html#)
- **Backend público:** [Supletanes App en Railway](https://supletanesappfinal-production.up.railway.app/)

## Arquitectura y tecnologías

### Frontend — Android

- **Lenguaje:** Kotlin.
- **Arquitectura:** MVVM (Model-View-ViewModel).
- **Interfaz:** Jetpack Compose.
- **Concurrencia:** Coroutines y Flow.
- **Comunicación HTTP:** Retrofit.
- **Serialización JSON:** Gson.
- **Pruebas unitarias:** MockK y Coroutine Test.
- **Mapas:** OSMDroid y OpenStreetMap.

Archivos principales:

- `frontend/app/src/main/java/com/example/supletanes/data/api/FoodApi.kt`
- `frontend/app/src/main/java/com/example/supletanes/ui/screens/plan/PlanScreen.kt`
- `frontend/app/src/main/java/com/example/supletanes/ui/screens/plan/viewmodel/PlanViewModel.kt`

### Backend — API REST

- **Lenguaje:** Java 21.
- **Framework:** Spring Boot.
- **Construcción y pruebas:** Maven.
- **Base de datos:** MySQL (desplegada en ECS Fargate con Service Connect) / PostgreSQL 16 (entorno Railway).
- **Documentación de endpoints:** Swagger UI.
- **Verificación de salud:** Spring Boot Actuator, mediante `/actuator/health`.

Código fuente relacionado con alimentos:

`backend/src/main/java/cl/supletanes/supletanes_app/foods`

### API externa

- **Open Food Facts:** consulta de información nutricional y productos mediante su código de barras.

## Endpoints disponibles

### Productos

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/productos` | Obtener todos los productos |
| GET | `/api/productos/{id}` | Obtener un producto por ID |
| GET | `/api/productos/categoria/{categoria}` | Filtrar productos por categoría |
| POST | `/api/productos` | Crear un producto |
| PUT | `/api/productos/{id}` | Actualizar un producto |
| DELETE | `/api/productos/{id}` | Eliminar un producto |

### Recordatorios

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/api/v1/recordatorios` | Crear un recordatorio |
| GET | `/api/v1/recordatorios/usuario/{idUsuario}` | Consultar recordatorios de un usuario |
| PUT | `/api/v1/recordatorios/{id}` | Actualizar un recordatorio |
| DELETE | `/api/v1/recordatorios/{id}` | Eliminar un recordatorio |

### Alimentos

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/food/{barcode}` | Consultar un alimento mediante su código de barras |
| GET | `/api/food/api/food/search` | Buscar alimentos |

Para revisar los parámetros y formatos de solicitud y respuesta, consulta Swagger UI.

---

## 1. Arquitectura e Integración CI/CD

El proyecto utiliza **GitHub Actions** (`.github/workflows/main.yml`) para automatizar las validaciones de calidad, auditoría de seguridad, construcción de contenedores y despliegue en **AWS ECS Fargate** con **Service Connect** y monitoreo en **AWS CloudWatch**.

### Flujo completo del pipeline (Commit a ECS Fargate + Service Connect)

1. **Disparador y control de rama (`push` y `pull_request` hacia `main`):**
   - Cada `pull_request` hacia `main` ejecuta obligatoriamente el job `1. Calidad, Cobertura y Seguridad (Policy Gate)`. Mediante reglas de protección de rama (*Branch Protection Rules*), GitHub bloquea el botón de *Merge* si este job falla.
   - Al integrar cambios en `main` (`push`), se ejecuta primero el *Policy Gate* y, solo si supera todas las validaciones, se habilita el job `2. Build, Auditoría Contenedor y Despliegue en AWS ECS`.
2. **Pruebas unitarias y cálculo de cobertura (Maven + JaCoCo):**
   - Se configura JDK 21 (Temurin) y se ejecuta `mvn test jacoco:report` dentro de `backend/`.
   - Un script extrae el porcentaje de cobertura de instrucciones desde `target/site/jacoco/jacoco.csv` y lo exporta como salida del job (`coverage`).
3. **Análisis estático y Quality Gate (SonarCloud — Bloqueante):**
   - Se ejecuta `mvn sonar:sonar -Dsonar.qualitygate.wait=true`. El pipeline espera el veredicto de SonarCloud sobre código nuevo, bugs, vulnerabilidades y cobertura; si el Quality Gate falla, el flujo se detiene.
4. **Auditoría de vulnerabilidades en código y dependencias (Trivy — Bloqueante):**
   - Se ejecuta `trivy fs --severity CRITICAL,HIGH --exit-code 1 backend`, deteniendo el pipeline ante hallazgos críticos o altos antes de construir la imagen.
5. **Construcción, auditoría de imagen y publicación en Amazon ECR:**
   - En el segundo job, tras autenticar credenciales AWS (`us-east-1`), se construye la imagen Docker de `./backend` etiquetada con `${{ github.sha }}`.
   - Antes de subirla al registro, se audita la imagen compilada mediante `trivy image --severity CRITICAL --ignore-unfixed --exit-code 1` y, si aprueba, se publica en el repositorio **Amazon ECR** (`supletanes-app`).
6. **Orquestación en AWS ECS Fargate + Service Connect:**
   - El script `backend/infra/ecs/render.sh` genera las definiciones de tareas y servicios (`td-mysql.json`, `td-app.json`, `td-client.json` y sus respectivos `svc-*.json`).
   - Se registran las definiciones de tareas y se despliegan/actualizan los servicios en el clúster `supletanes-ecs` bajo el namespace interno `supletanes.local` (**ECS Service Connect**), permitiendo la comunicación por nombre de servicio entre `supletanes-app`, `mysql` y `client`.
   - Se espera la estabilización de cada servicio mediante `aws ecs wait services-stable`.
7. **Observabilidad y métricas personalizadas (AWS CloudWatch):**
   - Se actualiza el dashboard `Dashboard-Mesh-supletanes` (`aws cloudwatch put-dashboard`) y se publican métricas personalizadas bajo el namespace `Supletanes/CICD`: duración del despliegue (`DeploymentDurationSeconds`) y porcentaje de cobertura (`TestCoveragePercent`).

### Herramientas y entornos

| Herramienta | Función |
|---|---|
| GitHub y Branch Protection | Control de versiones y bloqueo de *Merge* hacia `main` |
| GitHub Actions | Orquestación automatizada del pipeline CI/CD |
| Maven y JaCoCo | Compilación, pruebas unitarias y reporte de cobertura |
| SonarCloud | Análisis estático de código y *Quality Gate* bloqueante |
| Trivy | Auditoría de vulnerabilidades en código/dependencias (`fs`) e imagen Docker (`image`) |
| Docker y Amazon ECR | Empaquetado y almacenamiento versionado por SHA de commit |
| AWS ECS Fargate + Service Connect | Orquestación de contenedores (`supletanes-app`, `mysql`, `client`) y descubrimiento de servicios en `supletanes.local` |
| AWS CloudWatch | Logs de contenedores, alarmas (`Supletanes-5XX-Critico`), métricas personalizadas y dashboard (`Dashboard-Mesh-supletanes`) |
| MySQL / PostgreSQL | Persistencia de datos en ECS Fargate / Railway |

### Evidencias de Ejecución, Despliegue y Bloqueo (IE1, IE2, IE3, IE5, IE6)

> *(Ajustar nombres de archivo según las capturas guardadas en `docs/evidencias/`)*

- **Pipeline CI/CD completo en verde en `main`:**
  ![Pipeline CI/CD en verde](docs/evidencias/pipeline-verde.png)
- **Servicios activos en AWS ECS Fargate + Service Connect (`supletanes-ecs`):**
  ![Servicios ECS Fargate](docs/evidencias/ecs-servicios.png)
- **Dashboard de observabilidad en AWS CloudWatch (`Dashboard-Mesh-supletanes`):**
  ![Dashboard CloudWatch](docs/evidencias/cloudwatch-dashboard.png)
- **Alarma `Supletanes-5XX-Critico` activada ante caída simulada de MySQL (`503 Service Unavailable`):**
  ![Alarma 5XX CloudWatch](docs/evidencias/cloudwatch-alarma-5xx.png)
- **Bloqueo automático del Policy Gate en Pull Request ante falla de calidad/seguridad (IE6):**
  ![Bloqueo Policy Gate PR](docs/evidencias/policy-gate-bloqueo.png)

---

## 2. Toma de Decisiones Técnicas (IE4)

La integración de métricas en **AWS CloudWatch** (`Dashboard-Mesh-supletanes` y namespace `Supletanes/CICD`) junto con los reportes del pipeline permite tomar decisiones operativas y de ingeniería basadas en evidencia:

- **Uso de CPU y Memoria en ECS Fargate (Escalamiento de réplicas o recursos):**
  - El monitoreo continuo del consumo de CPU y memoria de las tareas `supletanes-app` y `mysql` permite identificar saturación ante picos de tráfico. Si el uso sostenido supera umbrales críticos (por ejemplo, >75-80% de CPU o memoria), se decide aumentar el `desired-count` de tareas en el servicio ECS para distribuir la carga o ajustar la asignación de vCPU/RAM en la definición de tarea (`td-app.json`).
- **Errores HTTP 5XX y Logs de aplicación (Detección de caídas de base de datos):**
  - Mediante el seguimiento de respuestas `5XX` (como el error `503 Service Unavailable` — `"Base de datos no disponible"` observado al detener el servicio `mysql` a `desired-count 0`) y la alarma `Supletanes-5XX-Critico`, el equipo detecta de inmediato indisponibilidad en la capa de persistencia o fallos de conexión en el pool de Spring Boot. Al cruzar la alerta con los logs centralizados en CloudWatch Logs y las métricas de Service Connect, se diferencia rápidamente si el problema proviene de una caída del contenedor `mysql`, un timeout de red o una excepción no controlada en un endpoint específico.
- **Tiempo de despliegue (`DeploymentDurationSeconds`) (Optimización del ciclo de entrega):**
  - Registrar la duración total desde la generación de manifiestos hasta que `aws ecs wait services-stable` confirma la estabilidad del servicio permite detectar degradaciones en el tiempo de arranque de Spring Boot, demoras en la descarga de imágenes desde ECR o problemas en los *health checks*. Un incremento anómalo en esta métrica indica la necesidad de optimizar el `Dockerfile` (capas y tamaño de imagen) o revisar los tiempos de inicialización de la aplicación.
- **Cobertura JaCoCo (`TestCoveragePercent`) y Quality Gate de SonarCloud (Prevención de regresiones):**
  - Monitorear la evolución del porcentaje de cobertura en cada despliegue y bloquear integraciones mediante SonarCloud y Trivy evita que código sin pruebas unitarias o con vulnerabilidades `HIGH`/`CRITICAL` llegue a producción. Si la cobertura tiende a la baja tras incorporar nuevas funcionalidades, se prioriza la creación de tests antes de continuar agregando características.

---

## 3. Declaración de Uso de Inteligencia Artificial

## 3. Declaración de Uso de Inteligencia Artificial

Se utilizaron herramientas de apoyo basadas en Inteligencia Artificial únicamente para consultar sintaxis puntual de comandos (AWS CLI y expresiones en GitHub Actions) y revisar la redacción técnica de este documento. Todas las configuraciones, pruebas y análisis técnicos fueron desarrollados y validados directamente por el equipo en el entorno real de AWS y GitHub.

---

## 4. Reflexiones Individuales Obligatorias (sin IA)

### Reflexión — Christopher Espinoza

> *[Completar aquí con tu reflexión personal redactada sin apoyo de IA, explicando tu aprendizaje y aporte técnico al proyecto].*

### Reflexión — [Nombre del segundo integrante]

> *[Completar aquí con la reflexión personal del segundo integrante redactada sin apoyo de IA, explicando su aprendizaje y aporte técnico al proyecto].*

---

## Cómo utilizar la aplicación

### Aplicación Android

1. Iniciar la aplicación.
2. Presionar el botón **Continuar**.
3. Iniciar sesión o continuar como invitado.
4. Acceder a las diferentes opciones y pantallas disponibles.

### Pruebas de la API

1. Abrir [Swagger UI](https://supletanesappfinal-production.up.railway.app/swagger-ui/index.html#).
2. Seleccionar el endpoint que se desea probar.
3. Presionar **Try it out**.
4. Completar los parámetros requeridos.
5. Presionar **Execute** y revisar la respuesta.

### Ejecución del pipeline

**Flujo normal en `main`:**

1. Realizar un commit o integrar un Pull Request aprobado en `main`.
2. Abrir la pestaña **Actions** en GitHub.
3. Revisar la ejecución de `1. Calidad, Cobertura y Seguridad (Policy Gate)` (JaCoCo, SonarCloud y Trivy) y `2. Build, Auditoría Contenedor y Despliegue en AWS ECS`.
4. Verificar los servicios activos en AWS ECS y las métricas en el dashboard `Dashboard-Mesh-supletanes` de CloudWatch.

**Demostración de bloqueo en Pull Request:**

1. Crear una rama (por ejemplo, `feat/test-falla-seguridad`) e introducir una falla de calidad, cobertura o seguridad.
2. Abrir un Pull Request hacia `main`.
3. Verificar en GitHub que el job `1. Calidad, Cobertura y Seguridad (Policy Gate)` falla en rojo y bloquea el botón de *Merge* hacia `main`.

## Objetivos del pipeline

- Automatizar la compilación y las pruebas unitarias del backend con reporte de cobertura JaCoCo.
- Incorporar análisis estático bloqueante con SonarCloud y auditoría de vulnerabilidades con Trivy.
- Contenerizar el microservicio y publicar imágenes versionadas por SHA en Amazon ECR.
- Orquestar los contenedores en AWS ECS Fargate con descubrimiento de servicios mediante Service Connect.
- Centralizar logs, alarmas (`Supletanes-5XX-Critico`), dashboard (`Dashboard-Mesh-supletanes`) y métricas CI/CD en AWS CloudWatch.
- Garantizar la protección de la rama `main` deteniendo el flujo automáticamente ante fallas críticas de calidad o seguridad.