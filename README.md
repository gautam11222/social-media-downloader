# Video Downloader - Spring Boot Application

This is a Maven-based Spring Boot application that allows you to download videos from YouTube and Instagram with audio.

## Features
✔ Download videos by providing the video URL
✔ Supports YouTube and Instagram
✔ Simple and clean UI with Thymeleaf
✔ Uses `yt-dlp` as the underlying download tool

## Requirements
1. Java 17 or higher installed
2. Maven installed
3. `yt-dlp` installed and available in your system path

### Install `yt-dlp`
```bash
pip install yt-dlp
```
or download the binary from [yt-dlp GitHub](https://github.com/yt-dlp/yt-dlp)

## Setup Instructions (Eclipse)
1. Download and extract this zip file.
2. Open Eclipse.
3. Go to File → Import → Maven → Existing Maven Projects.
4. Select the extracted project directory.
5. Finish the import process.
6. Right-click on `DownloaderApplication.java` → Run As → Spring Boot App.
7. Open your browser and visit: `http://localhost:8080/`
8. Enter the video URL and click Download.

## Notes
✔ This project is for educational purposes only.
✔ Make sure to respect the terms of service of video platforms.