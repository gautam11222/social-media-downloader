package com.example.downloader.dto;

public class FormatDTO {
    private String formatId;
    private String ext;
    private String note;
    private String resolution;

    public FormatDTO() {}

    public FormatDTO(String formatId, String ext, String note, String resolution) {
        this.formatId = formatId;
        this.ext = ext;
        this.note = note;
        this.resolution = resolution;
    }

    public String getFormatId() { return formatId; }
    public void setFormatId(String formatId) { this.formatId = formatId; }

    public String getExt() { return ext; }
    public void setExt(String ext) { this.ext = ext; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
