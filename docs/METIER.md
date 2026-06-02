# Documentation Métier — SkillMap

## Vision
SkillMap donne aux développeurs et recruteurs une vision claire
du marché tech français : quelles compétences sont demandées,
où, combien, et comment les tendances évoluent.

## Utilisateurs cibles
- Développeurs qui veulent savoir quoi apprendre pour trouver un job
- Recruteurs qui veulent comprendre le marché
- Écoles et bootcamps qui veulent adapter leur programme

## Fonctionnalités

### Dashboard principal
- Top 20 compétences du mois (bar chart)
- Comparaison vs mois précédent (flèche + %)
- Filtre par catégorie : Langages / Frameworks / DevOps / Cloud / BDD

### Carte France
- Heatmap des villes qui recrutent le plus
- Filtre par compétence (ex: "Angular en Île-de-France")
- Top 10 villes avec comptage

### Tendances (line chart)
- Évolution mensuelle sur 6 mois
- Comparaison de plusieurs compétences
- Indicateur : "Java stable" / "Rust +45%" / "Flash -100%"

### Insights IA
- Corrélations : "Angular est toujours demandé avec TypeScript (92%)"
- Alertes : "Kubernetes a augmenté de 35% ce mois"
- Opportunités : "Python + AWS = combo le plus demandé en CDI"

### Stats globales (header)
- Nombre d'offres analysées
- Nombre de compétences suivies
- Villes couvertes
- Dernière mise à jour

## Règles métier

### Déduplication des offres
Chaque offre a un externalId unique (ID France Travail ou "ARBEITNOW_" + slug).
Si l'externalId existe déjà en base → on ne sauvegarde pas.

### Calcul des tendances mensuelles
Après chaque collecte, TrendService calcule :
  mentionCount = COUNT offres contenant la compétence ce mois
  growthRate = (mentionCount - mentionCountMoisPrécédent) / mentionCountMoisPrécédent × 100

### Détermination du trend
  growthRate > +10%  → "UP"
  growthRate < -10%  → "DOWN"
  sinon              → "STABLE"

### Extraction des compétences
Le SkillParser analyse : titre + description + compétences de l'offre.
Correspondance par mots-clés (insensible à la casse).
Ex: "spring boot" dans la description → Skill "Spring Boot" ajouté.

## KPIs attendus
- ~700k offres analysées (France Travail)
- ~300 offres/run (Arbeitnow, 3 pages)
- 50+ compétences suivies
- Mise à jour toutes les heures
- Cache insights : 1h (évite de recalculer à chaque requête)
