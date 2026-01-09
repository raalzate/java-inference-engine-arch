package com.extractor;

import com.extractor.analyzer.ProjectAnalyzer;
import com.extractor.inference.InferenceEngine;
import com.extractor.inference.MicroserviceCandidates;
import com.extractor.inference.MicroserviceRecommendationEngine;
import com.extractor.model.DependencyGraph;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main application that analyzes Java projects and generates architecture
 * outputs:
 * 1. output.json - Complete dependency graph with all components and their
 * metadata
 * 2. output_architecture.json - Clusters based on cohesion and coupling
 * analysis
 */
public class MicroserviceInferenceMain {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java MicroserviceInferenceMain <ruta-proyecto> <archivo-salida>");
            System.err.println("Ejemplo: java MicroserviceInferenceMain /path/to/project output.json");
            System.exit(1);
        }

        String projectPath = args[0];
        String outputFile = args[1];

        try {
            System.out.println("🔍 Iniciando análisis del proyecto: " + projectPath);

            // Step 1: Extract dependency graph
            ProjectAnalyzer analyzer = new ProjectAnalyzer();
            Path projectPathObj = Paths.get(projectPath);
            DependencyGraph dependencyGraph = analyzer.analyzeProject(projectPathObj);

            System.out.println("📊 Componentes encontrados: " + dependencyGraph.getComponents().size());
            System.out.println("🔗 Relaciones encontradas: " + dependencyGraph.getEdges().size());

            // Save dependency graph to output.json
            ObjectMapper graphMapper = new ObjectMapper();
            graphMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            graphMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            String graphJson = graphMapper.writeValueAsString(dependencyGraph);
            saveToFile(graphJson, outputFile);
            System.out.println("✅ Grafo de dependencias guardado en: " + outputFile);

            // Step 2: Run inference engine
            System.out.println("\n🧠 Ejecutando motor de inferencias...");
            InferenceEngine inferenceEngine = new InferenceEngine();
            MicroserviceCandidates candidates = inferenceEngine.analyze(dependencyGraph);

            System.out.println("🎯 Clusters generados: " + candidates.getCandidates().size());

            // Step 3: Generate consolidated architecture proposal
            System.out.println("\n🏗️ Generando propuesta de arquitectura consolidada...");
            MicroserviceRecommendationEngine recommendationEngine = new MicroserviceRecommendationEngine();

            // Get project dependencies
            java.util.Map<String, String> projectDeps = analyzer.getDependencyResolver().getAllDependencies();

            com.extractor.inference.ConsolidatedArchitecture architecture = recommendationEngine
                    .analyzeConsolidated(candidates, dependencyGraph.getComponents(), projectDeps);

            String architectureFile = outputFile.replace(".json", "_architecture.json");
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
            mapper.enable(com.fasterxml.jackson.databind.SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            String architectureJson = mapper.writeValueAsString(architecture);
            saveToFile(architectureJson, architectureFile);

            System.out.println("✅ Propuesta de arquitectura guardada en: " + architectureFile);

            // Step 4: Export API entrypoints
            System.out.println("\n🌐 Exportando entrypoints (API Contracts)...");
            String entrypointsFile = outputFile.replace(".json", "_entrypoints.json");
            String entrypointsJson = mapper.writeValueAsString(dependencyGraph.getApiContracts());
            saveToFile(entrypointsJson, entrypointsFile);
            System.out.println("✅ Entrypoints guardados en: " + entrypointsFile);

            // Print summary
            printArchitectureSummary(architecture);

        } catch (Exception e) {
            System.err.println("❌ Error durante el análisis: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Saves the JSON output to a file.
     */
    private static void saveToFile(String content, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(content);
        }
    }

    /**
     * Prints a summary of the consolidated architecture proposal.
     */
    private static void printArchitectureSummary(com.extractor.inference.ConsolidatedArchitecture architecture) {
        System.out.println("\n" + architecture.getSummary());
    }
}
