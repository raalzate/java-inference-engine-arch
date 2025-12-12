# Java Dependency Extractor con Motor de Inferencia de Microservicios

Herramienta de análisis estático que extrae dependencias de proyectos Java multi-módulo, genera clusters inteligentes de componentes, y propone arquitecturas de microservicios con nombres de negocio y clasificación de viabilidad.

[![Ver video en YouTube](https://img.youtube.com/vi/m8U0r368jR8/maxresdefault.jpg)](https://www.youtube.com/watch?v=m8U0r368jR8)

## 🚀 Inicio Rápido

### Requisitos

- **Java 11+**
- **Maven 3.6+**

### Instalación

```bash
# Clonar el repositorio
git clone <repository-url>
cd java-dependency-extractor

# Compilar el proyecto
mvn clean compile
```

### Ejecución

```bash
mvn exec:java -Dexec.args="<ruta-proyecto-java> <archivo-salida.json>"
```

**Ejemplo:**
```bash
mvn exec:java -Dexec.args="/home/runner/workspace/spring-boot-monolith output.json"
```

### Archivos Generados

La herramienta genera automáticamente **4 archivos JSON** especializados:

1. **`output.json`** - Grafo completo de dependencias con todos los componentes.
2. **`output_architecture.json`** - Propuesta consolidada de microservicios con clasificación de viabilidad.
3. **`output_layers.json`** - Arquitectura vertical por capas (Controller → Business → Data).
4. **`output_web.json`** - Análisis de capa web (generado solo si existen componentes web).

## 📋 Referencia de Salidas

### 1. Grafo Completo (`output.json`)

Contiene el análisis exhaustivo de todos los componentes y sus relaciones:

```json
{
  "components": [
    {
      "id": "com.example.UserService",
      "layer": "Negocio",
      "tables_used": ["users"],
      "calls_out": ["com.example.UserRepository"],
      "metrics": {
         "cbo": 5,
         "lcom": 1.2
      }
    }
  ]
}
```

### 2. Capas Arquitectónicas (`output_layers.json`)

Organiza los componentes en una estructura de capas limpia:

```json
{
  "microservices": [
    {
      "name": "Cart",
      "controllers": 2,
      "business": 3,
      "persistence": 4,
      "domain": 1,
      "transfer": 2,
      "assessment": "Stack vertical completo"
    }
  ]
}
```

### 3. Arquitectura Consolidada (`output_architecture.json`)

Propuesta final de agrupación lógica:

```json
{
  "proposals": [
    {
      "id": 0,
      "name": "Microservicio de Item y Inventory",
      "viability": "Alta",
      "clusters": [0],
      "components": ["ItemEntity", "ItemRepo", "ItemService"],
      "metrics": {
        "size": 5,
        "cohesion_avg": 0.85,
        "external_coupling": 0.12,
        "data_jaccard": 0.8,
        "tables": ["item"]
      },
      "rationale": [
        "✅ Alta cohesión interna (85%) - componentes bien relacionados",
        "✅ Bajo acoplamiento externo (12%) - buena independencia",
        "✅ Tamaño adecuado (5 componentes)"
      ],
      "recommended_actions": [
        "✅ Diseñar como microservicio independiente",
        "✅ Definir API pública con contratos claros",
        "✅ Asignar base de datos exclusiva",
        "✅ Implementar patrones de resiliencia"
      ]
    }
  ],
  "support_libraries": [...],
  "viability_summary": {
    "alta": 2,
    "media": 1,
    "baja": 0
  }
}
```

**Clasificación de Viabilidad:**
- **Alta (≥0.7)**: Listos para implementar
- **Media (0.5-0.7)**: Requieren refactorización moderada
- **Baja (<0.5)**: Requieren refactorización profunda

**Métricas Consolidadas:**
- `cohesion_avg`: Promedio de cohesión de clusters consolidados
- `external_coupling`: Ratio de llamadas externas vs totales
- `data_jaccard`: Similitud de tablas compartidas (0-1)
- `internal_edge_density`: Densidad de conexiones internas

## 📊 Estructura de Datos del Grafo

El sistema genera la información completa del grafo de arquitectura en el archivo `output_architecture.json`. Esta información puede ser consumida por herramientas de visualización o análisis posteriores.

### Interpretación del Grafo (`output_architecture.json`)

El archivo contiene dos niveles de grafos:

#### 1. Grafo de Dependencias de Paquetes (`project_metadata.package_dependencies`)
Representa la estructura física del proyecto como un grafo dirigido.

- **Nodos**: Claves del mapa (ej: `com.mx.ing.afore.constanciaLiq.bean`)
- **Aristas**: Lista `depends_on_packages`

```json
"com.mx.package.a": {
  "components_count": 5,
  "depends_on_packages": ["com.mx.package.b", "com.mx.package.c"]
}
```

#### 2. Grafo de Microservicios (`proposals`)
Representa la arquitectura lógica propuesta.

- **Nodos**: Objetos en la lista `proposals`
- **Contenido del Nodo**:
  - `id`: Identificador único del nodo
  - `clusters`: IDs de los clusters originales contenidos
  - `components`: Lista completa de clases Java en este nodo
  - `metrics`: Datos de cohesión y acoplamiento del nodo

**Relaciones implícitas**:
La conexión entre nodos de microservicios se infiere de las llamadas entre sus componentes internos (`components`).

### Integración con Herramientas Externas

Para visualizar estos grafos, se recomienda transformar el JSON a formatos estándar como:
- **Mermaid**: Generar diagramas de clases o componentes.
- **Graphviz/DOT**: Para visualización de dependencias complejas.
- **Gephi**: Para análisis de redes grandes.

## 📊 Interpretación de Resultados

### Lectura Rápida de Viabilidad

```
✅ Alta viabilidad (≥0.7)
   → Implementar directamente como microservicio
   → API bien definida, BD independiente, resiliencia

⚠️ Media viabilidad (0.5-0.7)
   → Refactorizar antes de implementar
   → Mejorar cohesión o reducir acoplamiento

❌ Baja viabilidad (<0.5)
   → Requiere rediseño profundo
   → Consolidar con otros clusters o replantear
```

### Ejemplo de Salida en Consola

```
PROPUESTA DE ARQUITECTURA DE MICROSERVICIOS
═══════════════════════════════════════════════

📋 Microservicios Propuestos:
─────────────────────────────
• Microservicio de Item y Inventory → Clusters 0 (5 componentes)
• Microservicio de Order → Clusters 1 (5 componentes)
• Microservicio de Customer → Clusters 2 (5 componentes)

📚 Librerías de Soporte:
────────────────────────
• Microservicio de Aplicación Principal → Clusters 3

📌 Conclusión de Viabilidad:
────────────────────────────
✅ Alta viabilidad: 2 microservicio(s) - Listos para implementar
⚠️ Media viabilidad: 0 microservicio(s) - Requieren refactorización moderada
❌ Baja viabilidad: 1 microservicio(s) - Requieren refactorización profunda
```

## 🎯 Casos de Uso

### 1. Análisis de Monolito para Migración

```bash
mvn exec:java -Dexec.args="/path/to/legacy-monolith migration_analysis.json"
```

Genera propuestas de microservicios con viabilidad clasificada.

### 2. Evaluación de Arquitectura Existente

```bash
mvn exec:java -Dexec.args="/path/to/current-system evaluation.json"
```

Identifica oportunidades de mejora en cohesión y acoplamiento.

### 3. Auditoría de Dependencias

Revisa `output.json` para mapear todas las dependencias entre componentes y librerías externas.

## 🔧 Características Principales

### Motor de Análisis Estático (Spoon)
- **Análisis de AST**: Parsea el código fuente sin necesidad de compilarlo.
- **Soporte Multi-módulo**: Recorre recursivamente árboles de directorios.
- **Procesamiento de Anotaciones**: Soporte completo para Lombok y otros procesadores.

### Modelo de Análisis Basado en Componentes
Cada clase, interfaz o enum se modela como un "componente" con metadatos ricos:
- **Clasificación de Capas**: Asignación automática a capas arquitectónicas.
- **Métricas de Código**: CBO (Acoplamiento) y LCOM (Cohesión).
- **Relaciones**: Llamadas a métodos, herencia e implementación de interfaces.
- **Dependencias Externas**: Mapeo a coordenadas Maven/Gradle.
- **Detección de Sistemas**: Bases de datos, mensajería, datos sensibles.

## 🏗️ Arquitectura y Clasificación

### Sistema de Clasificación de Capas
El `LayerClassifier` categoriza componentes analizando su nombre, anotaciones y dependencias:

#### Capas Core
- **Controlador**: Endpoints REST, Struts Actions, JSF Beans.
- **Negocio**: Servicios, lógica de negocio principal.
- **Compartida**: Utilidades, configuraciones, clientes externos, factories.
- **Web**: Componentes basados en Servlets.

#### Capas de Datos Refinadas
- **Persistencia**: Capa de acceso a datos (`@Repository`, DAOs, JDBC calls).
- **Dominio**: Objetos de dominio o entidades de negocio.
- **Transferencia**: DTOs (Data Transfer Objects).

### Detección de Acceso a Base de Datos
Estrategia multi-nivel para identificar interacciones con BD:

1. **JDBC**: Detección de `java.sql` (`PreparedStatement`, `executeQuery`) y extracción de nombres de tablas de strings SQL.
2. **iBatis/MyBatis**: Detección de `SqlSession`, `SqlSessionFactory` y mappers.
3. **JPA/Hibernate**: Anotaciones `@Entity`, `@Table`, uso de `EntityManager`.
4. **Spring Data**: Interfaces `JpaRepository`, `CrudRepository`.

### Detección de Sistemas de Mensajería
Identifica componentes que actúan como productores o consumidores:
- **Tipos**: JMS, Kafka, RabbitMQ, ActiveMQ.
- **Roles**: Publisher, Consumer.

### Detección de Datos Sensibles (PII & Secretos)
Escaneo basado en patrones para seguridad:
- **Métodos**: Búsqueda de keywords (password, token, secret) en variables y literales.
- **Prevención de Falsos Positivos**: 
  - Exclusión automática de endpoints REST (ej: `/getAuthToken`).
  - Validación de rutas URL vs secretos reales (Base64, UUIDs).

## 📊 Métricas de Calidad de Código

### CBO (Coupling Between Objects)
Mide el número de clases únicas a las que un componente está acoplado.
- **Fuentes**: Llamadas a métodos, constructores, campos, parámetros, herencia.
- **Umbrales de Viabilidad**:
  - **≤ 5**: Bajo acoplamiento (Ideal)
  - **≤ 10**: Acoplamiento moderado (Aceptable)
  - **> 10**: Alto acoplamiento (Problemático)

### LCOM (Lack of Cohesion in Methods)
Usa la fórmula LCOM-HS (Henderson-Sellers) para medir la cohesión basada en uso de campos compartidos.
- **Escala**: 0 = Alta cohesión, 1 = Baja cohesión.
- **Umbrales de Viabilidad**:
  - **≤ 0.3**: Alta cohesión (Ideal - Clases bien enfocadas)
  - **≤ 0.6**: Cohesión moderada (Aceptable)
  - **> 0.6**: Baja cohesión (Problemática - Posible "God Class")

### Cálculo de Viabilidad de Microservicios
Fórmula ponderada para determinar si un clúster es viable como microservicio:

```java
score = 0.5 * adjustedCohesion + 
        0.35 * (1 - externalCoupling) + 
        0.15 * dataCohesion
```

### Motor de Inferencia de Microservicios
- **Clustering automático** por dominio y tablas compartidas
- **Consolidación inteligente** con señales múltiples (tablas, llamadas, tokens, eventos)
- **Generación de nombres** de negocio automática
- **Clasificación de viabilidad** (Alta/Media/Baja)

## 📏 Métricas de Precisión

La herramienta calcula automáticamente dos métricas de precisión que se incluyen en la sección `meta` del archivo `output.json`:

### 1. Dependency Accuracy (Precisión de Dependencias)

Mide la calidad de la detección de dependencias en el código:

```json
"dependency_accuracy": {
  "overall_score": 0.65,
  "coverage_score": 0.75,
  "precision_score": 0.80,
  "depth_score": 0.60,
  "details": {
    "total_components": 50,
    "components_with_calls": 35,
    "components_with_db": 12,
    "components_with_sensitive": 5,
    "total_structural_deps": 120,
    "total_calls": 150
  }
}
```

**Componentes de la métrica:**
- **Coverage Score**: Porcentaje de componentes con llamadas a métodos detectadas
- **Precision Score**: Ratio entre llamadas a métodos detectadas y dependencias estructurales (edges del grafo). Valores altos indican que se detectaron muchas llamadas en relación a las dependencias estructurales
- **Depth Score**: Nivel de análisis profundo (llamadas + BD + datos sensibles) / total componentes
- **Overall Score**: Promedio ponderado de las tres métricas anteriores (40% coverage, 40% precision, 20% depth)

**Interpretación:**
- **≥0.7**: Excelente cobertura de dependencias
- **0.5-0.7**: Buena cobertura, algunas dependencias pueden faltar
- **<0.5**: Cobertura limitada, considerar revisar manualmente

### 2. Decomposition Accuracy (Calidad de Descomposición)

Evalúa la calidad de la agrupación en clusters y microservicios:

```json
"decomposition_accuracy": {
  "overall_score": 0.72,
  "modularity_score": 0.68,
  "cohesion_score": 0.75,
  "coupling_score": 0.25,
  "balance_score": 0.82,
  "details": {
    "cluster_count": 4,
    "avg_cluster_size": 8.5,
    "min_cluster_size": 3,
    "max_cluster_size": 15,
    "clusters_with_high_cohesion": 3,
    "clusters_with_low_coupling": 3
  }
}
```

**Componentes de la métrica:**
- **Modularity Score**: Ratio de conexiones internas vs totales (Q de Newman)
- **Cohesion Score**: Promedio de cohesión de todos los clusters
- **Coupling Score**: Promedio de acoplamiento externo de clusters
- **Balance Score**: Uniformidad en el tamaño de clusters (coeficiente de variación)
- **Overall Score**: Combinación ponderada (30% modularidad, 30% cohesión, 30% bajo acoplamiento, 10% balance)

**Interpretación:**
- **≥0.7**: Excelente descomposición en microservicios
- **0.5-0.7**: Descomposición aceptable, revisar clusters con baja cohesión
- **<0.5**: Descomposición problemática, considerar re-clustering

**Uso práctico:**
- Usar `dependency_accuracy` para validar que el análisis detectó suficientes dependencias
- Usar `decomposition_accuracy` para evaluar si la propuesta de microservicios es viable
- Scores bajos indican necesidad de refactorización antes de migrar a microservicios

## 🧮 Algoritmos del Motor de Inferencia

### 1. InterClusterGraph
Calcula relaciones entre clusters usando 4 señales:

```
evidenceScore = 0.25 * tableJaccard + 
                0.35 * callDensity + 
                0.30 * tokenSimilarity + 
                0.10 * eventLinks
```

- **tableJaccard**: Similitud de tablas compartidas (índice de Jaccard)
- **callDensity**: Densidad normalizada de llamadas entre clusters
- **tokenSimilarity**: Similitud de tokens de dominio (Jaccard)
- **eventLinks**: Acoplamiento por eventos (publisher→listener)

### 2. ClusterConsolidator
Algoritmo greedy que agrupa clusters con:
- **Umbral de evidencia**: ≥0.65 con ≥2 señales fuertes
- **Guardrails de tamaño**: Evita grupos >40 componentes (excepto alta similitud)
- **Separación de soporte**: No mezcla infraestructura (≥80%) con negocio
- **Preservación de candidatos**: No fusiona candidatos fuertes débilmente acoplados

### 3. MicroserviceNameGenerator
Genera nombres automáticos:
- **Infraestructura**: "Microservicio de Seguridad & Configuración"
- **Negocio**: "Microservicio de Item y Inventory"
- **Exclusiones**: 30+ tokens técnicos (api, rest, dto, entity, etc.)

### 4. ViabilityScorer
Clasifica propuestas:

```
viabilityScore = 0.5 * cohesionAdj + 
                 0.35 * (1 - externalCoupling) + 
                 0.15 * dataCohesion
```

- **Alta (≥0.7)**: Implementar directamente
- **Media (0.5-0.7)**: Refactorizar moderadamente
- **Baja (<0.5)**: Rediseño profundo necesario

Ver [ALGORITHMS.md](ALGORITHMS.md) para detalles técnicos completos.

## 🔧 Configuración Avanzada

### Personalización de Consolidación

Los parámetros pueden ajustarse en el código fuente:

**Pesos de señales** (`InterClusterGraph.java`):
```java
private static final double TABLE_WEIGHT = 0.25;
private static final double CALL_WEIGHT = 0.35;
private static final double TOKEN_WEIGHT = 0.30;
private static final double EVENT_WEIGHT = 0.10;
```

**Umbrales de viabilidad** (`ViabilityScorer.java`):
```java
private static final double HIGH_VIABILITY = 0.7;
private static final double MEDIUM_VIABILITY = 0.5;
```

**Tokens excluidos** (`MicroserviceNameGenerator.java`):
```java
private static final Set<String> EXCLUDE_TOKENS = Set.of(
    "entity", "dto", "api", "rest", "service", ...
);
```

Ver [CONFIGURATION.md](CONFIGURATION.md) para guía completa de personalización.

## 🛠️ Troubleshooting

### No se generan componentes

**Causa**: Proyecto sin código Java válido o estructura no reconocida

**Solución**: 
- Verificar carpetas `src/main/java` existan
- Revisar logs de Spoon para errores de parsing

### Nombres genéricos (ej: "Microservicio de Entity")

**Causa**: Componentes solo tienen tokens técnicos excluidos

**Solución**:
- Revisar `MicroserviceNameGenerator.EXCLUDE_TOKENS`
- Asegurar nombres de clases con dominios de negocio claros

### Todos los microservicios con baja viabilidad

**Causa**: Monolito altamente acoplado sin separación clara

**Solución**:
- Revisar recomendaciones de refactorización
- Desacoplar componentes antes de migrar
- Considerar arquitectura hexagonal para separación

### Muchos nano-servicios generados

**Causa**: Componentes muy granulares o desconectados

**Solución**:
- Revisar recomendación de fusionar nano-servicios
- Analizar si componentes pequeños pertenecen a contextos mayores

## 📚 Documentación Adicional

- [ALGORITHMS.md](ALGORITHMS.md) - Deep dive técnico en algoritmos de consolidación
- [CONFIGURATION.md](CONFIGURATION.md) - Guía de configuración y personalización

## 🤝 Contribución

Para extender el sistema:

1. **Añadir nuevas señales** en `InterClusterGraph`
2. **Personalizar reglas** de consolidación en `ClusterConsolidator`
3. **Agregar categorías** de diseño en `MicroserviceRecommendationEngine`
4. **Extender detección** de patrones en `DatabaseDetector` o `SensitiveDataDetector`

