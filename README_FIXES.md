Patches applied:
- Added @Service annotation and SLF4J logging to VirusScanService and YtDlpService.
- Improved logging when clamscan is unavailable (VirusScanService).
- Added debug listing before throwing thumbnail-not-found in YtDlpService.

Notes:
- I could not run `mvn package` here because Maven/Java toolchain is unavailable in this environment.
- Please run `mvn -U clean package` locally or in CI to compile and test runtime behavior.
- Ensure Dockerfile installs `yt-dlp` and `clamav`/`clamscan` if you expect scan and download binaries to run inside container.
