import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import {
  CityDTO,
  InsightDTO,
  SkillDTO,
  StatsDTO,
  TrendDTO,
} from '../models/skillmap.models';

/**
 * Client HTTP unique pour l'API SkillMap (backend Spring Boot).
 * Base URL configurée dans environment.apiBaseUrl (http://localhost:8081/api).
 */
@Injectable({ providedIn: 'root' })
export class SkillMapService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  /** GET /api/skills?limit=&month=&category= — Top compétences du mois */
  getTopSkills(limit = 20, month?: string, category?: string): Observable<SkillDTO[]> {
    let params = new HttpParams().set('limit', limit);
    if (month) params = params.set('month', month);
    if (category) params = params.set('category', category);
    return this.http.get<SkillDTO[]>(`${this.baseUrl}/skills`, { params });
  }

  /** GET /api/trends?skill=&months= — Évolution mensuelle d'une compétence */
  getTrend(skill: string, months = 6): Observable<TrendDTO[]> {
    const params = new HttpParams().set('skill', skill).set('months', months);
    return this.http.get<TrendDTO[]>(`${this.baseUrl}/trends`, { params });
  }

  /** GET /api/skills/compare?skills=&months= — Comparaison de plusieurs compétences */
  compareSkills(skills: string[], months = 6): Observable<TrendDTO[]> {
    const params = new HttpParams()
      .set('skills', skills.join(','))
      .set('months', months);
    return this.http.get<TrendDTO[]>(`${this.baseUrl}/skills/compare`, { params });
  }

  /** GET /api/cities?skill=&limit= — Répartition géographique */
  getCities(skill?: string, limit = 10): Observable<CityDTO[]> {
    let params = new HttpParams().set('limit', limit);
    if (skill) params = params.set('skill', skill);
    return this.http.get<CityDTO[]>(`${this.baseUrl}/cities`, { params });
  }

  /** GET /api/insights — Analyse IA (mise en cache 1h côté backend) */
  getInsights(): Observable<InsightDTO[]> {
    return this.http.get<InsightDTO[]>(`${this.baseUrl}/insights`);
  }

  /** GET /api/stats — Statistiques globales du header */
  getStats(): Observable<StatsDTO> {
    return this.http.get<StatsDTO>(`${this.baseUrl}/stats`);
  }
}
