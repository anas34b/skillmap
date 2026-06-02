-- SkillMap — Migration initiale
-- V1__init_schema.sql

-- ── Skills ────────────────────────────────────────────────────
CREATE TABLE skills (
    id       BIGSERIAL PRIMARY KEY,
    name     VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50)
);

-- ── Job Offers ────────────────────────────────────────────────
CREATE TABLE job_offers (
    id               BIGSERIAL PRIMARY KEY,
    external_id      VARCHAR(255) UNIQUE NOT NULL,
    title            VARCHAR(500) NOT NULL,
    description      TEXT,
    city             VARCHAR(100),
    department       VARCHAR(100),
    contract_type    VARCHAR(50),
    experience_level VARCHAR(50),
    salary_min       DECIMAL(10,2),
    salary_max       DECIMAL(10,2),
    remote           BOOLEAN DEFAULT FALSE,
    source           VARCHAR(50) NOT NULL,  -- FRANCE_TRAVAIL | ARBEITNOW
    company_name     VARCHAR(255),
    apply_url        VARCHAR(1000),
    posted_at        DATE,
    collected_at     TIMESTAMP DEFAULT NOW()
);

-- ── Job Offer ↔ Skills (Many-to-Many) ────────────────────────
CREATE TABLE job_offer_skills (
    job_offer_id BIGINT REFERENCES job_offers(id) ON DELETE CASCADE,
    skill_id     BIGINT REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (job_offer_id, skill_id)
);

-- ── Skill Trends (agrégats mensuels) ─────────────────────────
CREATE TABLE skill_trends (
    id            BIGSERIAL PRIMARY KEY,
    skill_id      BIGINT REFERENCES skills(id) ON DELETE CASCADE,
    year_month    VARCHAR(7) NOT NULL,   -- format: "2026-06"
    mention_count INT DEFAULT 0,
    growth_rate   DECIMAL(8,2),
    top_city      VARCHAR(100),
    UNIQUE (skill_id, year_month)
);

-- ── Index pour les performances ───────────────────────────────
CREATE INDEX idx_job_offers_source     ON job_offers(source);
CREATE INDEX idx_job_offers_city       ON job_offers(city);
CREATE INDEX idx_job_offers_posted_at  ON job_offers(posted_at);
CREATE INDEX idx_job_offers_collected  ON job_offers(collected_at);
CREATE INDEX idx_skill_trends_month    ON skill_trends(year_month);
CREATE INDEX idx_skill_trends_skill    ON skill_trends(skill_id);
CREATE INDEX idx_skills_category       ON skills(category);

-- ── Données initiales : skills de base ────────────────────────
INSERT INTO skills (name, category) VALUES
    ('Java',          'LANGUAGE'),
    ('Python',        'LANGUAGE'),
    ('JavaScript',    'LANGUAGE'),
    ('TypeScript',    'LANGUAGE'),
    ('Kotlin',        'LANGUAGE'),
    ('Go',            'LANGUAGE'),
    ('PHP',           'LANGUAGE'),
    ('C#',            'LANGUAGE'),
    ('Spring Boot',   'FRAMEWORK_BACKEND'),
    ('Angular',       'FRAMEWORK_FRONTEND'),
    ('React',         'FRAMEWORK_FRONTEND'),
    ('Vue.js',        'FRAMEWORK_FRONTEND'),
    ('Docker',        'DEVOPS'),
    ('Kubernetes',    'DEVOPS'),
    ('Terraform',     'DEVOPS'),
    ('Ansible',       'DEVOPS'),
    ('AWS',           'CLOUD'),
    ('GCP',           'CLOUD'),
    ('Azure',         'CLOUD'),
    ('PostgreSQL',    'DATABASE'),
    ('MongoDB',       'DATABASE'),
    ('Redis',         'DATABASE'),
    ('Kafka',         'MESSAGING'),
    ('Microservices', 'ARCHITECTURE'),
    ('REST API',      'ARCHITECTURE')
ON CONFLICT (name) DO NOTHING;
