package com.kevin.dueltime4.network;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Build-time helper.
 * Usage:
 * 0: version
 * 1: packaged jar path
 * 2: download-url template (supports {version})
 * 3: output network/version path
 * 4: output network/update.yml path
 * 5: changelog path
 */
public final class UpdateMetadataGenerator {
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[(.+?)]\\((.+?)\\)");

    private UpdateMetadataGenerator() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 6) {
            throw new IllegalArgumentException("UpdateMetadataGenerator requires 6 arguments.");
        }

        String version = safeTrim(args[0]);
        Path jarPath = Path.of(args[1]);
        String downloadUrlTemplate = safeTrim(args[2]);
        Path versionFilePath = Path.of(args[3]);
        Path updateYamlPath = Path.of(args[4]);
        Path changelogPath = Path.of(args[5]);

        if (version.isEmpty()) {
            throw new IllegalArgumentException("Version must not be empty.");
        }
        if (!Files.exists(jarPath)) {
            throw new IllegalStateException("Packaged jar not found: " + jarPath);
        }

        String downloadUrl = downloadUrlTemplate.replace("{version}", version);
        String sha256 = computeSha256(jarPath);
        List<String> notes = parseNotesFromChangelog(changelogPath, version);

        writeVersionFile(versionFilePath, version, downloadUrl, sha256, notes);
        writeUpdateYaml(updateYamlPath, version, downloadUrl, sha256, notes);

        System.out.println("[UpdateMetadataGenerator] Updated version metadata.");
        System.out.println("[UpdateMetadataGenerator] version=" + version);
        System.out.println("[UpdateMetadataGenerator] sha256=" + sha256);
        System.out.println("[UpdateMetadataGenerator] notes=" + notes.size());
    }

    private static List<String> parseNotesFromChangelog(Path changelogPath, String version) throws Exception {
        List<String> notes = new ArrayList<>();
        if (!Files.exists(changelogPath)) {
            notes.add("No changelog file found.");
            return notes;
        }

        List<String> lines = Files.readAllLines(changelogPath, StandardCharsets.UTF_8);
        boolean inTargetVersion = false;
        String currentSection = "";

        for (String rawLine : lines) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.startsWith("## ")) {
                if (inTargetVersion) {
                    break;
                }
                inTargetVersion = line.equalsIgnoreCase("## " + version);
                currentSection = "";
                continue;
            }
            if (!inTargetVersion) {
                continue;
            }
            if (line.startsWith("### ")) {
                currentSection = line.substring(4).trim();
                continue;
            }
            if (!line.startsWith("- ")) {
                continue;
            }

            String note = sanitizeMarkdown(line.substring(2).trim());
            if (note.isEmpty()) {
                continue;
            }
            if (!currentSection.isEmpty()) {
                note = "[" + currentSection + "] " + note;
            }
            notes.add(note);
        }

        if (notes.isEmpty()) {
            notes.add("No notes provided for version " + version + ".");
        }
        return notes;
    }

    private static String sanitizeMarkdown(String text) {
        String sanitized = text.replace("`", "");
        sanitized = LINK_PATTERN.matcher(sanitized).replaceAll("$1");
        return sanitized.trim();
    }

    private static String computeSha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void writeVersionFile(Path output, String version, String downloadUrl, String sha256, List<String> notes) throws Exception {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(version).append('\n');
        sb.append("download-url: ").append(downloadUrl).append('\n');
        sb.append("sha256: ").append(sha256).append('\n');
        sb.append("notes:").append('\n');
        for (String note : notes) {
            sb.append("- ").append(note).append('\n');
        }
        Files.writeString(output, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void writeUpdateYaml(Path output, String version, String downloadUrl, String sha256, List<String> notes) throws Exception {
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("version: \"").append(escapeYaml(version)).append('"').append('\n');
        sb.append("download-url: \"").append(escapeYaml(downloadUrl)).append('"').append('\n');
        sb.append("sha256: \"").append(escapeYaml(sha256)).append('"').append('\n');
        sb.append("notes:").append('\n');
        for (String note : notes) {
            sb.append("  - \"").append(escapeYaml(note)).append('"').append('\n');
        }
        Files.writeString(output, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String escapeYaml(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }
}
