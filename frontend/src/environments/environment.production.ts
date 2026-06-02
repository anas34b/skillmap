// Environnement de PRODUCTION (build Firebase Hosting).
// ⚠️ Remplacer l'URL par celle du backend Cloud Run après `terraform apply`
//    (output `backend_url`), ex: https://skillmap-backend-staging-xxxx.a.run.app
export const environment = {
  production: true,
  apiBaseUrl: 'https://REMPLACER-PAR-URL-CLOUD-RUN/api',
};
