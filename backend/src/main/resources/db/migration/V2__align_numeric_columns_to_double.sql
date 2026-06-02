-- SkillMap — Alignement des types numériques sur le modèle JPA
-- V2__align_numeric_columns_to_double.sql
--
-- Les entités JPA (JobOffer.salaryMin/salaryMax, SkillTrend.growthRate) sont
-- typées `Double` → Hibernate (ddl-auto: validate) attend `float8` (double
-- precision). La migration initiale les avait déclarées en DECIMAL/NUMERIC,
-- ce qui faisait échouer la validation de schéma au démarrage.

ALTER TABLE job_offers
    ALTER COLUMN salary_min TYPE DOUBLE PRECISION,
    ALTER COLUMN salary_max TYPE DOUBLE PRECISION;

ALTER TABLE skill_trends
    ALTER COLUMN growth_rate TYPE DOUBLE PRECISION;
