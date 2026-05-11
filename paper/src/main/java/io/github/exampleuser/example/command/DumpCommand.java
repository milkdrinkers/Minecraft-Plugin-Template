package io.github.exampleuser.example.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.exampleuser.example.Example;
import io.github.milkdrinkers.colorparser.paper.ColorParser;
import io.github.milkdrinkers.threadutil.Scheduler;
import io.github.milkdrinkers.wordweaver.Translation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.github.exampleuser.example.command.CommandHandler.BASE_PERM;

/**
 * Class containing the code for the dump command.
 * <p>
 * This command gathers server and plugin configurations, logs, and other information, and uploads it to <a href="https://mclo.gs">MCLogs</a>.
 */
final class DumpCommand extends Command {
    private static final boolean INCLUDE_PLUGINS = true; // Whether to include the plugin list in dumps
    private static final Map<String, Path> INCLUDED_SERVER_CONFIGS = Map.ofEntries( // Server-specific configurations to include in dumps
        Map.entry("SERVER PROPERTIES", Paths.get("server.properties")),
        Map.entry("BUKKIT CONFIGURATION", Paths.get("bukkit.yml")),
        Map.entry("SPIGOT CONFIGURATION", Paths.get("spigot.yml")),
        Map.entry("PAPER CONFIGURATION", Paths.get("config", "paper-global.yml")),
        Map.entry("PAPER CONFIGURATION LEGACY", Paths.get("paper.yml")),
        Map.entry("PURPUR CONFIGURATION", Paths.get("purpur.yml")),
        Map.entry("PUFFERFISH CONFIGURATION", Paths.get("pufferfish.yml"))
    );
    private static final Set<Path> INCLUDED_CONFIGS = Set.of( // Plugin-specific configurations to include in dumps
        Paths.get("config.yml"),
        Paths.get("database.yml")
    );
    private static final boolean INCLUDE_LOGS = true; // Whether to include the latest log in dumps

    private static final String DUMP_PERM = BASE_PERM + ".dump";

    public CommandAPICommand command() {
        return new CommandAPICommand("dump")
            .withHelp("Upload server & plugin configs and logs to mclo.gs.", "Upload server & plugin configs and logs to mclo.gs.")
            .withPermission(DUMP_PERM)
            .executes(this::executorDump);
    }

    private void executorDump(CommandSender sender, CommandArguments args) {
        sender.sendMessage(Translation.as("commands.dump.dumping"));

        Scheduler.async(() -> {
                try {
                    return upload(log());
                } catch (Exception e) {
                    sender.sendMessage(
                        ColorParser.of(Translation.of("commands.dump.failure"))
                            .with("error", e.getMessage())
                            .build()
                    );
                    return null;
                }
            })
            .sync(link -> {
                if (link == null)
                    return;

                sender.sendMessage(
                    ColorParser.of(Translation.of("commands.dump.success"))
                        .with("link", link)
                        .build()
                );
            })
            .execute();
    }

    /**
     * Generates a server dump containing various information and configurations.
     *
     * @return the formatted server dump as a string
     */
    private String log() {
        final JavaPlugin plugin = Example.getInstance();
        final StringBuilder dump = new StringBuilder();

        // Server info
        dump.append("=== SERVER INFORMATION ===").append(System.lineSeparator());
        dump.append("Server Version: ").append(Bukkit.getName()).append(" ").append(Bukkit.getVersion()).append(System.lineSeparator());
        dump.append("Minecraft Version: ").append(Bukkit.getMinecraftVersion()).append(System.lineSeparator());
        dump.append("Java Version: ").append(System.getProperty("java.version")).append(System.lineSeparator());
        dump.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(System.lineSeparator());
        dump.append("Available Memory: ").append(Runtime.getRuntime().maxMemory() / 1024 / 1024).append("MB").append(System.lineSeparator());
        dump.append("Used Memory: ").append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024).append("MB").append(System.lineSeparator());
        dump.append("Timestamp: ").append(new Date()).append(System.lineSeparator());
        dump.append(System.lineSeparator());

        // Plugins
        if (INCLUDE_PLUGINS) {
            dump.append("=== INSTALLED PLUGINS ===").append(System.lineSeparator());
            for (final Plugin pl : Arrays.stream(Bukkit.getPluginManager().getPlugins()).sorted(Comparator.comparing(Plugin::getName)).toList())
                dump.append(pl.getName()).append(" v").append(pl.getPluginMeta().getVersion()).append(System.lineSeparator());
            dump.append(System.lineSeparator());
        }

        // Server configuration files
        for (final Map.Entry<String, Path> entry : INCLUDED_SERVER_CONFIGS.entrySet()) {
            try {
                if (!Files.exists(entry.getValue())) {
                    continue;
                }

                final String content = readAndScrub(entry.getValue());
                dump.append("=== %s ===".formatted(entry.getKey())).append(System.lineSeparator());
                dump.append(content).append(System.lineSeparator());
            } catch (Exception ignored) {
            }
        }

        // Server configuration files
        if (!INCLUDED_CONFIGS.isEmpty()) {
            dump.append("=== %s CONFIGURATION ===".formatted(plugin.getName().toUpperCase())).append(System.lineSeparator());

            for (final Path pluginPath : INCLUDED_CONFIGS) {
                try {
                    final Path path = plugin.getDataPath().resolve(pluginPath);

                    if (!Files.exists(path)) {
                        continue;
                    }

                    final String content = readAndScrub(path);
                    dump.append("--- ").append(path).append(" ---").append(System.lineSeparator());
                    dump.append(content).append(System.lineSeparator());
                } catch (Exception ignored) {
                }
            }
        }

        // Latest log
        if (INCLUDE_LOGS) {
            dump.append("=== LATEST LOG ===").append(System.lineSeparator());
            try {
                String latestLog = readAndScrub(Paths.get("logs/latest.log"));
                if (latestLog.length() > 50000) {
                    latestLog = latestLog.substring(latestLog.length() - 50000);
                    dump.append("(Log truncated to last 50,000 characters)").append(System.lineSeparator());
                }
                dump.append(latestLog).append(System.lineSeparator());
            } catch (Exception e) {
                dump.append("Could not read latest.log: ").append(e.getMessage()).append(System.lineSeparator());
            }
        }

        return dump.toString();
    }

    private static final String MCLO_GS_URL = "https://api.mclo.gs/1/log";
    private static final int MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Reads and scrubs the server log file, then uploads it to MCLogs.
     *
     * @return the URL of the uploaded log
     * @throws IOException if an error occurs during reading or uploading
     */
    private String readAndScrub(final Path path) throws IOException {
        if (!Files.exists(path))
            throw new IOException("File does not exist: %s".formatted(path));

        if (Files.size(path) > MAX_FILE_SIZE)
            throw new IOException("File is too large to include: %s".formatted(path.toString()));

        return Scrubber.scrub(Files.readString(path));
    }

    /**
     * Utility class for scrubbing sensitive information from text.
     */
    private static final class Scrubber {
        private Scrubber() {
        }

        /**
         * Patterns for IP addresses and their replacements.
         * These patterns match IPv4 and IPv6 addresses and replace them with a generic placeholder.
         */
        private static final Map<Pattern, String> IP_PATTERNS = Map.ofEntries(
            Map.entry(Pattern.compile("(?<!([0-9]|-|\\w))(?:[1-2]?[0-9]{1,2}\\.){3}[1-2]?[0-9]{1,2}(?!([0-9]|-|\\w))"), "**.**.**.**"),
            Map.entry(Pattern.compile("(?<!([0-9]|-|\\w))(?:[0-9a-f]{0,4}:){7}[0-9a-f]{0,4}(?!([0-9]|-|\\w))", Pattern.CASE_INSENSITIVE), "****:****:****:****:****:****:****:****")
        );

        /**
         * Patterns for IP addresses that should not be scrubbed.
         * These are localhost or well-known IPs that are safe to leave in logs.
         */
        private static final Pattern[] IP_WHITELIST_PATTERNS = new Pattern[]{
            Pattern.compile("127\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}"),
            Pattern.compile("0\\.0\\.0\\.0"),
            Pattern.compile("1\\.[01]\\.[01]\\.1"),
            Pattern.compile("8\\.8\\.[84]\\.[84]"),
            Pattern.compile("[0:]+1?") // IPv6 localhost
        };

        /**
         * Patterns for sensitive information that should be scrubbed.
         */
        @SuppressWarnings("RegExpUnnecessaryNonCapturingGroup")
        private static final Pattern[] SENSITIVE_PATTERNS = {
            Pattern.compile("^(\\s*(?:password|pass|pwd|auth|token|key|secret|api[\\w-]*key|bearer|authorization)\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE), // YAML
            Pattern.compile("^(\\s*(?:password|pass|pwd|auth|token|key|secret|api[\\w-]*key|bearer|authorization)\\s*=\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE), // Properties
            Pattern.compile("(\\s*['\"](?:password|pass|pwd|auth|token|key|secret|api[\\w-]*key|bearer|authorization)['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE), // JSON
            Pattern.compile("^(\\s*(?:mysql|database|db|mariadb|postgresql|postgres|mongo|redis)[\\w-]*\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:mysql|database|db|mariadb|postgresql|postgres|mongo|redis)[\\w-]*['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:username|user|login)\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:username|user|login)['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:connection-string|conn-string|jdbc-url|database-url|db-url|uri|url)\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:connection-string|conn-string|jdbc-url|database-url|db-url|uri|url)['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:host|hostname|address|server|endpoint|ip|port)\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:host|hostname|address|server|endpoint|ip|port)['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:level-seed|seed|world-seed|seed-[\\w-]+)\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:level-seed|seed|world-seed|seed-[\\w-]+)['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:resource-pack-sha1|sha1|hash|checksum)[\\w-]*\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:resource-pack-sha1|sha1|hash|checksum)[\\w-]*['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:[\\w-]+)\\s*[:=]\\s*)(['\"]?)(https?://[^'\"\\r\\n\\s]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:[\\w-]+)['\"]\\s*:\\s*)(['\"]?)(https?://[^'\"\\r\\n\\s]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:resource-pack|texture-pack|pack-url)[\\w-]*\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:resource-pack|texture-pack|pack-url)[\\w-]*['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:motd|server-description|description)\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:motd|server-description|description)['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:download-url|update-url|plugin-url|mod-url)[\\w-]*\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:download-url|update-url|plugin-url|mod-url)[\\w-]*['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:webhook|discord|slack|notification)[\\w-]*\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:webhook|discord|slack|notification)[\\w-]*['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("^(\\s*(?:server-name|servername|name)\\s*[:=]\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)(\\s*)$", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(\\s*['\"](?:server-name|servername|name)['\"]\\s*:\\s*)(['\"]?)([^'\"\\r\\n]+)(['\"]?)", Pattern.CASE_INSENSITIVE)
        };

        /**
         * Patterns for standalone content that should be replaced with a generic placeholder.
         */
        private static final Map<Pattern, String> STANDALONE_PATTERNS = Map.ofEntries(
            Map.entry(Pattern.compile("\\b(?:https?|ftp)://[^\\s<>\"{}|\\\\^`\\[\\]]+"), "[URL]")
        );

        /**
         * Check if this IP address matches any whitelist filters
         *
         * @param ip the IP address to check
         * @return matches
         */
        private static boolean isWhitelistedIP(final String ip) {
            for (final Pattern filter : IP_WHITELIST_PATTERNS) {
                if (ip.matches(filter.pattern())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Constructs the replacement string for sensitive content.
         *
         * @param matcher the matcher containing the sensitive content
         * @return the replacement string
         */
        private static @NotNull StringBuilder getReplacement(final Matcher matcher) {
            final StringBuilder replacement = new StringBuilder();
            replacement.append(matcher.group(1)); // prefix with key
            if (matcher.group(2) != null) replacement.append(matcher.group(2)); // opening quote
            replacement.append("[REDACTED]"); // replace the value
            if (matcher.group(4) != null) replacement.append(matcher.group(4)); // closing quote
            if (matcher.group(5) != null) replacement.append(matcher.group(5)); // trailing space
            return replacement;
        }

        /**
         * Scrubs sensitive information from the given content.
         *
         * @param content the content to scrub
         * @return the scrubbed content
         */
        public static String scrub(String content) {
            String scrubbedContent = content;

            // Filter out IP addresses
            for (final Map.Entry<Pattern, String> entry : IP_PATTERNS.entrySet()) {
                final Matcher matcher = entry.getKey().matcher(scrubbedContent);
                final StringBuilder sb = new StringBuilder();
                while (matcher.find()) {
                    if (isWhitelistedIP(matcher.group())) {
                        continue;
                    }
                    matcher.appendReplacement(sb, entry.getValue());
                }
                matcher.appendTail(sb);
                scrubbedContent = sb.toString();
            }

            // Filter sensitive content
            for (final Pattern pattern : SENSITIVE_PATTERNS) {
                final Matcher matcher3 = pattern.matcher(scrubbedContent);
                final StringBuilder sb3 = new StringBuilder();
                while (matcher3.find()) {
                    final StringBuilder replacement = getReplacement(matcher3);

                    matcher3.appendReplacement(sb3, replacement.toString());
                }
                matcher3.appendTail(sb3);
                scrubbedContent = sb3.toString();
            }

            // Filter standalone patterns
            for (final Map.Entry<Pattern, String> entry : STANDALONE_PATTERNS.entrySet()) {
                final Matcher matcher = entry.getKey().matcher(scrubbedContent);
                final StringBuilder sb = new StringBuilder();

                while (matcher.find()) {
                    matcher.appendReplacement(sb, entry.getValue());
                }
                matcher.appendTail(sb);
                scrubbedContent = sb.toString();
            }

            return scrubbedContent;
        }
    }

    /**
     * Uploads the passed content to <a href="https://mclo.gs">MCLogs</a>.
     *
     * @param content the content to upload
     * @return the URL of the uploaded content
     * @throws IOException if an error occurs during upload
     */
    private @Nullable String upload(final String content) throws IOException {
        final JavaPlugin plugin = Example.getInstance();
        final URL url = URI.create(MCLO_GS_URL).toURL();

        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("User-Agent", "%s/%s".formatted(plugin.getPluginMeta().getDisplayName(), plugin.getPluginMeta().getVersion()));
        con.setDoOutput(true);

        final String postData = "content=%s".formatted(URLEncoder.encode(content, StandardCharsets.UTF_8));

        try (final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(con.getOutputStream(), StandardCharsets.UTF_8))) {
            bufferedWriter.write(postData);
        }

        final int responseCode = con.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new IOException("HTTP error code: %s".formatted(responseCode));
        }

        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            final StringBuilder response = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                response.append(line);
            }

            final Gson gson = new Gson();
            final JsonObject jsonResponse = gson.fromJson(response.toString(), JsonObject.class);

            if (jsonResponse.has("success") && jsonResponse.get("success").getAsBoolean()) {
                return jsonResponse.get("url").getAsString();
            } else {
                throw new IOException("Upload failed: %s".formatted(jsonResponse.get("error").getAsString()));
            }
        }
    }
}
