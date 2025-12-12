package com.extractor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Root data model for the dependency graph JSON output.
 */
public class DependencyGraph {
    
    @JsonProperty("components")
    private List<Component> components;
    
    @JsonProperty("edges") 
    private List<Edge> edges;
    
    @JsonProperty("meta")
    private Meta meta;
    
    public DependencyGraph() {
        this.meta = new Meta();
    }
    
    public DependencyGraph(List<Component> components, List<Edge> edges) {
        this.components = components;
        this.edges = edges;
        this.meta = new Meta();
    }
    
    // Getters and setters
    public List<Component> getComponents() {
        return components;
    }
    
    public void setComponents(List<Component> components) {
        this.components = components;
    }
    
    public List<Edge> getEdges() {
        return edges;
    }
    
    public void setEdges(List<Edge> edges) {
        this.edges = edges;
    }
    
    public Meta getMeta() {
        return meta;
    }
    
    public void setMeta(Meta meta) {
        this.meta = meta;
    }
    
    /**
     * Metadata for the dependency graph
     */
    public static class Meta {
        @JsonProperty("source")
        private String source = "spoon";
        
        @JsonProperty("collected_at")
        private String collectedAt;
        
        @JsonProperty("microservice_candidates")
        private Map<String, String> microserviceCandidates;
        
        @JsonProperty("dependency_accuracy")
        private Map<String, Object> dependencyAccuracy;
        
        @JsonProperty("decomposition_accuracy")
        private Map<String, Object> decompositionAccuracy;
        
        public Meta() {
            this.collectedAt = Instant.now().toString();
            this.microserviceCandidates = new HashMap<>();
        }
        
        public String getSource() {
            return source;
        }
        
        public void setSource(String source) {
            this.source = source;
        }
        
        public String getCollectedAt() {
            return collectedAt;
        }
        
        public void setCollectedAt(String collectedAt) {
            this.collectedAt = collectedAt;
        }
        
        public Map<String, String> getMicroserviceCandidates() {
            return microserviceCandidates;
        }
        
        public void setMicroserviceCandidates(Map<String, String> microserviceCandidates) {
            this.microserviceCandidates = microserviceCandidates;
        }
        
        public Map<String, Object> getDependencyAccuracy() {
            return dependencyAccuracy;
        }
        
        public void setDependencyAccuracy(Map<String, Object> dependencyAccuracy) {
            this.dependencyAccuracy = dependencyAccuracy;
        }
        
        public Map<String, Object> getDecompositionAccuracy() {
            return decompositionAccuracy;
        }
        
        public void setDecompositionAccuracy(Map<String, Object> decompositionAccuracy) {
            this.decompositionAccuracy = decompositionAccuracy;
        }
    }
}