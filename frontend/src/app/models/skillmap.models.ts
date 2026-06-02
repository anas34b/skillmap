// Modèles TypeScript — miroir des records DTO du backend (com.skillmap.dto)

// Top compétences — GET /api/skills
export interface SkillDTO {
  name: string;
  category: string;
  mentionCount: number;
  growthRate: number;     // % vs mois précédent
  trend: 'UP' | 'DOWN' | 'STABLE';
}

// Évolution mensuelle — GET /api/trends, GET /api/skills/compare
export interface TrendDTO {
  yearMonth: string;      // "2026-06"
  skillName: string;
  mentionCount: number;
  growthRate: number;
}

// Répartition géographique — GET /api/cities
export interface CityDTO {
  city: string;
  jobCount: number;
  percentage: number;
  topSkills: string[];
}

// Insight IA — GET /api/insights
export interface InsightDTO {
  title: string;
  description: string;
  type: 'TREND' | 'CORRELATION' | 'ALERT';
  icon: string;           // emoji
}

// Statistiques globales — GET /api/stats
export interface StatsDTO {
  totalJobsAnalyzed: number;
  totalSkillsTracked: number;
  totalCitiesCovered: number;
  lastUpdated: string;
  newJobsThisMonth: number;
}
