package com.extractor.analyzer;

import com.extractor.model.ApiEndpoint;
import com.extractor.model.ApiSchema;
import com.extractor.model.DependencyGraph;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtTypeReference;

/**
 * Extracts OpenAPI-style contracts from controllers and listeners.
 * Designed to be robust in no-classpath environments.
 */
public class OpenApiExtractor {

    private final DependencyGraph.ApiContracts apiContracts;

    public OpenApiExtractor(DependencyGraph.ApiContracts apiContracts) {
        this.apiContracts = apiContracts;
    }

    public void extractFromType(CtType<?> type) {
        // Spring REST Controllers
        if (hasAnnotation(type, "RestController") || hasAnnotation(type, "Controller")) {
            extractRestEndpoints(type);
        }
        // JAX-RS Resources
        else if (hasAnnotation(type, "Path")) {
            extractJaxRsEndpoints(type);
        }
        // Message Listeners
        extractListeners(type);
    }

    private void extractRestEndpoints(CtType<?> type) {
        String basePath = getRequestMappingValue(type);

        for (CtMethod<?> method : type.getMethods()) {
            CtAnnotation<?> mapping = getMappingAnnotation(method);
            if (mapping != null) {
                ApiEndpoint endpoint = new ApiEndpoint();
                endpoint.setComponentId(type.getQualifiedName());
                endpoint.setId(type.getSimpleName() + "." + method.getSimpleName());

                String path = getMappingValue(mapping);
                endpoint.setPath(cleanPath(basePath + path));
                endpoint.setMethod(getHttpMethod(mapping));

                extractParameters(method, endpoint);
                extractBodyAndResponse(method, endpoint);

                apiContracts.getEndpoints().add(endpoint);
            }
        }
    }

    private void extractJaxRsEndpoints(CtType<?> type) {
        String basePath = getJaxRsPathValue(type);

        for (CtMethod<?> method : type.getMethods()) {
            String httpMethod = getJaxRsHttpMethod(method);
            if (httpMethod != null) {
                ApiEndpoint endpoint = new ApiEndpoint();
                endpoint.setComponentId(type.getQualifiedName());
                endpoint.setId(type.getSimpleName() + "." + method.getSimpleName());

                String methodPath = getJaxRsPathValue(method);
                endpoint.setPath(cleanPath(basePath + methodPath));
                endpoint.setMethod(httpMethod);

                extractJaxRsParameters(method, endpoint);
                extractBodyAndResponse(method, endpoint);

                apiContracts.getEndpoints().add(endpoint);
            }
        }
    }

    private void extractListeners(CtType<?> type) {
        for (CtMethod<?> method : type.getMethods()) {
            CtAnnotation<?> kafka = getAnnotation(method, "KafkaListener");
            CtAnnotation<?> rabbit = getAnnotation(method, "RabbitListener");
            CtAnnotation<?> jms = getAnnotation(method, "JmsListener");

            if (kafka != null || rabbit != null || jms != null) {
                ApiEndpoint endpoint = new ApiEndpoint();
                endpoint.setComponentId(type.getQualifiedName());
                endpoint.setId(type.getSimpleName() + "." + method.getSimpleName());

                if (kafka != null) {
                    endpoint.setMethod("KAFKA_LISTEN");
                    endpoint.setPath(getAnnotationValue(kafka, "topics"));
                } else if (rabbit != null) {
                    endpoint.setMethod("RABBIT_LISTEN");
                    endpoint.setPath(getAnnotationValue(rabbit, "queues"));
                } else {
                    endpoint.setMethod("JMS_LISTEN");
                    endpoint.setPath(getAnnotationValue(jms, "destination"));
                }

                extractBodyAndResponse(method, endpoint);
                apiContracts.getEndpoints().add(endpoint);
            }
        }
    }

    private void extractParameters(CtMethod<?> method, ApiEndpoint endpoint) {
        for (CtParameter<?> param : method.getParameters()) {
            CtAnnotation<?> pathVar = getAnnotation(param, "PathVariable");
            CtAnnotation<?> reqParam = getAnnotation(param, "RequestParam");

            if (pathVar != null || reqParam != null) {
                ApiEndpoint.Parameter p = new ApiEndpoint.Parameter();
                p.setName(getParamName(param, pathVar != null ? pathVar : reqParam));
                p.setIn(pathVar != null ? "path" : "query");
                p.setType(param.getType().getSimpleName());
                p.setRequired(true);
                endpoint.getParameters().add(p);
            }
        }
    }

    private void extractBodyAndResponse(CtMethod<?> method, ApiEndpoint endpoint) {
        // Request Body
        for (CtParameter<?> param : method.getParameters()) {
            if (hasAnnotation(param, "RequestBody") || endpoint.getMethod().endsWith("_LISTEN")) {
                endpoint.setRequestBodySchema(param.getType().getSimpleName());
                registerSchema(param.getType());
                break;
            }
        }

        // Response Body
        CtTypeReference<?> returnType = method.getType();
        if (returnType != null && !returnType.getSimpleName().equals("void")) {
            if (returnType.getSimpleName().equals("ResponseEntity") && !returnType.getActualTypeArguments().isEmpty()) {
                returnType = returnType.getActualTypeArguments().get(0);
            }
            endpoint.setResponseSchema(returnType.getSimpleName());
            registerSchema(returnType);
        }
    }

    private void registerSchema(CtTypeReference<?> typeRef) {
        if (typeRef == null || typeRef.isPrimitive() || typeRef.getQualifiedName().startsWith("java.lang"))
            return;

        String name = typeRef.getSimpleName();
        if (apiContracts.getSchemas().containsKey(name))
            return;

        try {
            CtType<?> type = typeRef.getTypeDeclaration();
            if (type != null) {
                ApiSchema schema = new ApiSchema(name);
                for (CtField<?> field : type.getFields()) {
                    schema.addProperty(field.getSimpleName(), field.getType().getSimpleName());
                }
                apiContracts.getSchemas().put(name, schema);
            }
        } catch (Exception e) {
            // Ignore resolution errors in no-classpath mode
        }
    }

    private String getRequestMappingValue(CtType<?> type) {
        CtAnnotation<?> mapping = getAnnotation(type, "RequestMapping");
        return getMappingValue(mapping);
    }

    private CtAnnotation<?> getMappingAnnotation(CtMethod<?> method) {
        String[] types = { "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
                "PatchMapping" };
        for (String t : types) {
            CtAnnotation<?> ann = getAnnotation(method, t);
            if (ann != null)
                return ann;
        }
        return null;
    }

    private String getHttpMethod(CtAnnotation<?> mapping) {
        String type = mapping.getAnnotationType().getSimpleName();
        if (type.equals("GetMapping"))
            return "GET";
        if (type.equals("PostMapping"))
            return "POST";
        if (type.equals("PutMapping"))
            return "PUT";
        if (type.equals("DeleteMapping"))
            return "DELETE";
        if (type.equals("PatchMapping"))
            return "PATCH";
        return "UNKNOWN";
    }

    private String getMappingValue(CtAnnotation<?> mapping) {
        if (mapping == null)
            return "";
        String val = getAnnotationValue(mapping, "value");
        if (val.isEmpty())
            val = getAnnotationValue(mapping, "path");
        return val;
    }

    private String getAnnotationValue(CtAnnotation<?> ann, String param) {
        Object val = ann.getValues().get(param);
        if (val == null) {
            // Try to find any value if there's only one
            if (ann.getValues().size() == 1 && (param.equals("value") || param.equals("path"))) {
                val = ann.getValues().values().iterator().next();
            } else {
                return "";
            }
        }
        String s = val.toString().replaceAll("[\"{}\\[\\]]", "");
        if (!s.startsWith("/") && !s.isEmpty() && !ann.getAnnotationType().getSimpleName().contains("Listener"))
            s = "/" + s;
        return s;
    }

    private String getParamName(CtParameter<?> param, CtAnnotation<?> ann) {
        String name = getAnnotationValue(ann, "value");
        if (name.isEmpty())
            name = getAnnotationValue(ann, "name");
        return name.isEmpty() ? param.getSimpleName() : name;
    }

    private boolean hasAnnotation(CtElement element, String name) {
        return getAnnotation(element, name) != null;
    }

    private CtAnnotation<?> getAnnotation(CtElement element, String name) {
        for (CtAnnotation<?> ann : element.getAnnotations()) {
            if (ann.getAnnotationType().getSimpleName().equals(name) ||
                    ann.getAnnotationType().getQualifiedName().endsWith("." + name)) {
                return ann;
            }
        }
        return null;
    }

    private String getJaxRsPathValue(CtElement element) {
        CtAnnotation<?> pathAnn = getAnnotation(element, "Path");
        if (pathAnn == null)
            return "";
        String val = getAnnotationValue(pathAnn, "value");
        if (!val.startsWith("/") && !val.isEmpty())
            val = "/" + val;
        return val;
    }

    private String getJaxRsHttpMethod(CtMethod<?> method) {
        if (hasAnnotation(method, "GET"))
            return "GET";
        if (hasAnnotation(method, "POST"))
            return "POST";
        if (hasAnnotation(method, "PUT"))
            return "PUT";
        if (hasAnnotation(method, "DELETE"))
            return "DELETE";
        if (hasAnnotation(method, "PATCH"))
            return "PATCH";
        return null;
    }

    private void extractJaxRsParameters(CtMethod<?> method, ApiEndpoint endpoint) {
        for (CtParameter<?> param : method.getParameters()) {
            CtAnnotation<?> pathParam = getAnnotation(param, "PathParam");
            CtAnnotation<?> queryParam = getAnnotation(param, "QueryParam");

            if (pathParam != null || queryParam != null) {
                ApiEndpoint.Parameter p = new ApiEndpoint.Parameter();
                CtAnnotation<?> ann = pathParam != null ? pathParam : queryParam;
                // For JAX-RS, get the raw value without path processing
                Object val = ann.getValues().get("value");
                String paramName = val != null ? val.toString().replaceAll("[\"{}\\[\\]]", "") : param.getSimpleName();
                p.setName(paramName);
                p.setIn(pathParam != null ? "path" : "query");
                p.setType(param.getType().getSimpleName());
                p.setRequired(true);
                endpoint.getParameters().add(p);
            }
        }
    }

    private String cleanPath(String path) {
        return path.replaceAll("//+", "/");
    }
}
