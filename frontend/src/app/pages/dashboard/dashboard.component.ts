import { CommonModule } from '@angular/common';
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { Chart, registerables } from 'chart.js';

import { environment } from '../../../environments/environment';
import { SkillMapService } from '../../services/skillmap.service';
import {
  CityDTO,
  InsightDTO,
  SkillDTO,
  StatsDTO,
} from '../../models/skillmap.models';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent implements OnInit, AfterViewInit {
  private readonly api = inject(SkillMapService);

  @ViewChild('skillsChart') private chartCanvas?: ElementRef<HTMLCanvasElement>;
  private chart?: Chart;
  private viewReady = false;

  readonly stats = signal<StatsDTO | null>(null);
  readonly topSkills = signal<SkillDTO[]>([]);
  readonly insights = signal<InsightDTO[]>([]);
  readonly cities = signal<CityDTO[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.loadAll();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.renderChart();
  }

  /** (Re)charge toutes les données. Le service réessaie tout seul pendant le cold start. */
  loadAll(): void {
    this.loading.set(true);
    this.error.set(null);

    // topSkills = donnée principale : pilote le loader et le graphique
    this.api.getTopSkills(20).subscribe({
      next: (skills) => {
        this.topSkills.set(skills);
        this.loading.set(false);
        // différé : laisse Angular afficher le <canvas> avant de dessiner
        setTimeout(() => this.renderChart());
      },
      error: (e) => {
        this.error.set(this.describe(e));
        this.loading.set(false);
      },
    });

    this.api.getStats().subscribe({ next: (s) => this.stats.set(s), error: () => {} });
    this.api.getInsights().subscribe({ next: (i) => this.insights.set(i), error: () => {} });
    this.api.getCities(undefined, 10).subscribe({ next: (c) => this.cities.set(c), error: () => {} });
  }

  // ── UI helpers ────────────────────────────────────────────
  trendBadgeClass(trend: string): string {
    switch (trend) {
      case 'UP':
        return 'bg-emerald-500/15 text-emerald-400 ring-1 ring-emerald-500/30';
      case 'DOWN':
        return 'bg-rose-500/15 text-rose-400 ring-1 ring-rose-500/30';
      default:
        return 'bg-slate-500/15 text-slate-300 ring-1 ring-slate-500/30';
    }
  }

  trendIcon(trend: string): string {
    return trend === 'UP' ? '▲' : trend === 'DOWN' ? '▼' : '＝';
  }

  insightAccent(type: string): string {
    switch (type) {
      case 'ALERT':
        return 'border-l-4 border-amber-400';
      case 'CORRELATION':
        return 'border-l-4 border-indigo-400';
      default:
        return 'border-l-4 border-sky-400';
    }
  }

  // ── Chart.js ──────────────────────────────────────────────
  private renderChart(): void {
    if (!this.viewReady || !this.chartCanvas) return;
    const skills = this.topSkills();
    if (skills.length === 0) return;

    this.chart?.destroy();

    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: skills.map((s) => s.name),
        datasets: [
          {
            label: 'Mentions',
            data: skills.map((s) => s.mentionCount),
            backgroundColor: 'rgba(56, 189, 248, 0.55)',
            hoverBackgroundColor: 'rgba(56, 189, 248, 0.85)',
            borderColor: 'rgba(56, 189, 248, 1)',
            borderWidth: 1,
            borderRadius: 4,
          },
        ],
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: { backgroundColor: '#0f172a', borderColor: '#334155', borderWidth: 1 },
        },
        scales: {
          x: { grid: { color: 'rgba(148, 163, 184, 0.12)' }, ticks: { color: '#94a3b8' } },
          y: { grid: { display: false }, ticks: { color: '#cbd5e1', font: { size: 12 } } },
        },
      },
    });
  }

  private describe(err: unknown): string {
    const e = err as { status?: number; message?: string };
    return e?.status === 0
      ? `Impossible de joindre le backend (${environment.apiBaseUrl}).`
      : `Erreur ${e?.status ?? ''} : ${e?.message ?? 'inconnue'}`;
  }
}
