package com.skillmap.parser;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SkillParser {

    // Dictionnaire complet : nom normalisé → mots-clés à détecter
    private static final Map<String, List<String>> TECH_DICTIONARY = new LinkedHashMap<>() {{

        // ── Langages ──────────────────────────
        put("Java",       List.of("java", "jvm", "jdk", "jre"));
        put("Python",     List.of("python", "django", "fastapi", "flask", "pandas", "numpy"));
        put("JavaScript", List.of("javascript", "js", "node.js", "nodejs", "express"));
        put("TypeScript", List.of("typescript", "ts"));
        put("Kotlin",     List.of("kotlin"));
        put("Go",         List.of("golang", " go ", "go lang"));
        put("Rust",       List.of("rust"));
        put("PHP",        List.of("php", "symfony", "laravel"));
        put("C#",         List.of("c#", ".net", "dotnet", "asp.net"));
        put("Scala",      List.of("scala", "akka", "play framework"));
        put("Ruby",       List.of("ruby", "rails", "ruby on rails"));
        put("Swift",      List.of("swift", "swiftui", "xcode"));

        // ── Frameworks Backend ─────────────────
        put("Spring Boot", List.of("spring boot", "spring mvc", "spring framework", "spring cloud"));
        put("Spring",      List.of("spring security", "spring data", "spring batch"));
        put("Quarkus",     List.of("quarkus"));
        put("Micronaut",   List.of("micronaut"));
        put("NestJS",      List.of("nestjs", "nest.js"));
        put("FastAPI",     List.of("fastapi"));
        put("Django",      List.of("django"));

        // ── Frameworks Frontend ────────────────
        put("Angular",    List.of("angular", "angularjs", "angular 2", "ngrx"));
        put("React",      List.of("react", "reactjs", "react.js", "redux", "next.js", "nextjs"));
        put("Vue.js",     List.of("vue", "vuejs", "vue.js", "nuxt"));
        put("Svelte",     List.of("svelte", "sveltekit"));

        // ── DevOps & CI/CD ─────────────────────
        put("Docker",      List.of("docker", "dockerfile", "conteneur", "container"));
        put("Kubernetes",  List.of("kubernetes", "k8s", "kubectl", "helm"));
        put("Terraform",   List.of("terraform", "infrastructure as code", "iac"));
        put("Ansible",     List.of("ansible", "playbook"));
        put("Jenkins",     List.of("jenkins", "jenkinsfile"));
        put("GitHub Actions", List.of("github actions", "github ci", "workflow yaml"));
        put("GitLab CI",   List.of("gitlab ci", "gitlab-ci", ".gitlab-ci"));
        put("ArgoCD",      List.of("argocd", "argo cd", "gitops"));
        put("Prometheus",  List.of("prometheus", "grafana", "alertmanager"));

        // ── Cloud ──────────────────────────────
        put("AWS",         List.of("aws", "amazon web services", "ec2", "s3", "lambda", "eks", "ecs"));
        put("GCP",         List.of("gcp", "google cloud", "cloud run", "bigquery", "gke"));
        put("Azure",       List.of("azure", "microsoft azure", "aks", "azure devops"));
        put("OVH",         List.of("ovh", "ovhcloud"));

        // ── Bases de données ───────────────────
        put("PostgreSQL",  List.of("postgresql", "postgres", "psql"));
        put("MySQL",       List.of("mysql", "mariadb"));
        put("MongoDB",     List.of("mongodb", "mongo"));
        put("Redis",       List.of("redis", "redisson"));
        put("Elasticsearch", List.of("elasticsearch", "elastic search", "elk", "opensearch"));
        put("Cassandra",   List.of("cassandra", "apache cassandra"));
        put("Oracle DB",   List.of("oracle database", "oracle db", "pl/sql"));

        // ── Messaging ─────────────────────────
        put("Kafka",       List.of("kafka", "apache kafka", "confluent"));
        put("RabbitMQ",    List.of("rabbitmq", "amqp"));

        // ── Architecture ───────────────────────
        put("Microservices", List.of("microservices", "micro-services", "architecture microservices"));
        put("REST API",    List.of("rest api", "restful", "api rest", "openapi", "swagger"));
        put("GraphQL",     List.of("graphql"));
        put("gRPC",        List.of("grpc", "protobuf"));

        // ── Méthodes ───────────────────────────
        put("Agile",       List.of("agile", "scrum", "kanban", "sprint"));
        put("TDD",         List.of("tdd", "test driven", "bdd"));
        put("CI/CD",       List.of("ci/cd", "intégration continue", "déploiement continu"));
    }};

    // Catégories par skill
    private static final Map<String, String> CATEGORIES = new HashMap<>() {{
        List.of("Java","Python","JavaScript","TypeScript","Kotlin","Go","Rust","PHP","C#","Scala","Ruby","Swift")
            .forEach(s -> put(s, "LANGUAGE"));
        List.of("Spring Boot","Spring","Quarkus","Micronaut","NestJS","FastAPI","Django")
            .forEach(s -> put(s, "FRAMEWORK_BACKEND"));
        List.of("Angular","React","Vue.js","Svelte")
            .forEach(s -> put(s, "FRAMEWORK_FRONTEND"));
        List.of("Docker","Kubernetes","Terraform","Ansible","Jenkins","GitHub Actions","GitLab CI","ArgoCD","Prometheus")
            .forEach(s -> put(s, "DEVOPS"));
        List.of("AWS","GCP","Azure","OVH")
            .forEach(s -> put(s, "CLOUD"));
        List.of("PostgreSQL","MySQL","MongoDB","Redis","Elasticsearch","Cassandra","Oracle DB")
            .forEach(s -> put(s, "DATABASE"));
        List.of("Kafka","RabbitMQ")
            .forEach(s -> put(s, "MESSAGING"));
        List.of("Microservices","REST API","GraphQL","gRPC")
            .forEach(s -> put(s, "ARCHITECTURE"));
        List.of("Agile","TDD","CI/CD")
            .forEach(s -> put(s, "METHODOLOGY"));
    }};

    /**
     * Extrait les compétences depuis une description d'offre d'emploi
     * @param description texte brut de la description
     * @return liste des noms de compétences détectées
     */
    public List<String> extractSkills(String description) {
        if (description == null || description.isBlank()) return List.of();

        String lower = description.toLowerCase();

        return TECH_DICTIONARY.entrySet().stream()
            .filter(entry -> entry.getValue().stream()
                .anyMatch(keyword -> lower.contains(keyword.toLowerCase())))
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Retourne la catégorie d'une compétence
     */
    public String getCategory(String skillName) {
        return CATEGORIES.getOrDefault(skillName, "OTHER");
    }

    /**
     * Retourne tout le dictionnaire (pour initialisation DB)
     */
    public Map<String, String> getAllSkillsWithCategories() {
        Map<String, String> result = new LinkedHashMap<>();
        TECH_DICTIONARY.keySet().forEach(name -> result.put(name, getCategory(name)));
        return result;
    }
}
