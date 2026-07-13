package com.asaf.whatnext.services.images;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code npx remotion render|still} çağrısını yürüten ortak yardımcı — {@code WeeklyVideoService}
 * (video) ve {@code RemotionStillEngine} (tek kare görsel) aynı process/npx çözümleme mantığını
 * paylaşır (önceden video tarafında tek başına yaşıyordu, kopya çıkmasın diye buraya taşındı).
 */
@Component
public class RemotionRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemotionRunner.class);

    @Value("${whatnext.video.project-path:video}")
    private String videoProjectPath;

    @Value("${whatnext.video.npx-command:}")
    private String npxCommand;

    @Value("${whatnext.video.chrome-path:}")
    private String chromePath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param subcommand    "render" (video) veya "still" (tek kare PNG)
     * @param compositionId {@code video/src/Root.tsx}'te kayıtlı Composition id'si
     * @param props         JSON'a serileştirilip {@code --props} ile geçilir
     * @param outputExtension "mp4" veya "png"
     * @return üretilen dosya — başarısızlıkta {@code null} değil {@link IOException} fırlatılır
     *         (Faz 3.6 kararı: "üretim patladı" sessizce yutulmasın).
     */
    public File run(String subcommand, String compositionId, Map<String, Object> props, String outputExtension)
            throws IOException, InterruptedException {
        Path outputDir = Files.createTempDirectory("whatnext-remotion");
        File outputFile = outputDir.resolve("output." + outputExtension).toFile();
        File propsFile = outputDir.resolve("props.json").toFile();
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(propsFile, props);

        List<String> cmd = new ArrayList<>();
        cmd.add(resolveNpxCommand());
        cmd.add("remotion");
        cmd.add(subcommand);
        cmd.add("src/index.ts");
        cmd.add(compositionId);
        cmd.add(outputFile.getAbsolutePath());
        cmd.add("--props");
        cmd.add(propsFile.getAbsolutePath());
        if (chromePath != null && !chromePath.isEmpty()) {
            cmd.add("--browser-executable");
            cmd.add(chromePath);
        }
        cmd.add("--log");
        cmd.add("verbose");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(videoProjectPath).getAbsoluteFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info("[Remotion {}] {}", subcommand, line);
                output.append(line).append('\n');
            }
        }
        int exitCode = process.waitFor();

        try {
            Files.deleteIfExists(propsFile.toPath());
        } catch (IOException ignored) {
        }

        if (exitCode == 0 && outputFile.exists()) {
            LOGGER.info("Remotion {} başarılı: {}", subcommand, outputFile.getAbsolutePath());
            return outputFile;
        }
        throw new IOException("Remotion " + subcommand + " başarısız (çıkış kodu " + exitCode + "):\n" + output);
    }

    private String resolveNpxCommand() {
        if (npxCommand != null && !npxCommand.isEmpty()) {
            return npxCommand;
        }
        if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
            String programFiles = System.getenv("ProgramFiles");
            if (programFiles != null) {
                File npxInNode = new File(programFiles, "nodejs\\npx.cmd");
                if (npxInNode.exists()) {
                    return npxInNode.getAbsolutePath();
                }
            }
            String programFilesX86 = System.getenv("ProgramFiles(x86)");
            if (programFilesX86 != null) {
                File npxInNode = new File(programFilesX86, "nodejs\\npx.cmd");
                if (npxInNode.exists()) {
                    return npxInNode.getAbsolutePath();
                }
            }
            String appData = System.getenv("APPDATA");
            if (appData != null) {
                File npxInNpm = new File(appData, "npm\\npx.cmd");
                if (npxInNpm.exists()) {
                    return npxInNpm.getAbsolutePath();
                }
            }
            return "npx.cmd";
        }
        return "npx";
    }
}
