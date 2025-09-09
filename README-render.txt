Render deployment helpers (generated)
------------------------------------
Files added/updated:
- Dockerfile     -> multi-stage Dockerfile (builds jar + includes ffmpeg, yt-dlp, clamav)
- Procfile       -> for native (non-Docker) deploys on Render
- render.yaml    -> example manifest for Render Docker deploy

Quick steps to deploy on Render (Docker):
1. Push your repository to GitHub.
2. In Render: New -> Web Service -> Connect repo -> choose branch.
3. Select "Docker" environment; Dockerfile at repo root will be used.
4. Add environment variables as needed (SPRING_PROFILES_ACTIVE, etc).
5. Deploy.

Spring Boot will bind to the port Render provides ($PORT).
