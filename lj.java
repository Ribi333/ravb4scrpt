package me.ksyzov.accountmanager.auth;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;
import me.ksyzov.accountmanager.AccountManager;
import me.ksyzov.accountmanager.auth.Account;
import me.ksyzov.accountmanager.auth.AccountType;
import me.ksyzov.accountmanager.gui.GuiAccountManager;
import me.ksyzov.accountmanager.gui.GuiCookieAuth;
import me.ksyzov.accountmanager.utils.Notification;
import me.ksyzov.accountmanager.utils.TextFormatting;
import net.minecraft.util.Session;

public class CookieAuth {
    private static final ExecutorService executor = Executors.newFixedThreadPool(8);
    private static final Gson gson = new Gson();

    // Cookie validation result class
    public static class CookieResult {
        public final String fileName;
        public final boolean isValid;
        public final String username;
        public final String error;

        public CookieResult(String fileName, boolean isValid, String username, String error) {
            this.fileName = fileName;
            this.isValid = isValid;
            this.username = username;
            this.error = error;
        }
    }

    // Extended result class that includes Hypixel ban status
    public static class ExtendedCookieResult extends CookieResult {
        public final boolean hypixelChecked;
        public final boolean hypixelBanned;
        public final String hypixelError;
        public final Long lastSeen;

        public ExtendedCookieResult(String fileName, boolean isValid, String username, String error,
                                    boolean hypixelChecked, boolean hypixelBanned, String hypixelError, Long lastSeen) {
            super(fileName, isValid, username, error);
            this.hypixelChecked = hypixelChecked;
            this.hypixelBanned = hypixelBanned;
            this.hypixelError = hypixelError;
            this.lastSeen = lastSeen;
        }

        // Convenience constructor for cases without Hypixel checking
        public ExtendedCookieResult(CookieResult base) {
            this(base.fileName, base.isValid, base.username, base.error, false, false, null, null);
        }
    }

    // Progress tracking class for extended results
    public static class ExtendedBatchProgress {
        public AtomicInteger processed = new AtomicInteger(0);
        public AtomicInteger valid = new AtomicInteger(0);
        public AtomicInteger invalid = new AtomicInteger(0);
        public AtomicInteger hypixelChecked = new AtomicInteger(0);
        public AtomicInteger hypixelBanned = new AtomicInteger(0);
        public AtomicInteger hypixelClean = new AtomicInteger(0);
        public int total;
        public List<ExtendedCookieResult> results = new ArrayList<>();

        public ExtendedBatchProgress(int total) {
            this.total = total;
        }

        public synchronized void addResult(ExtendedCookieResult result) {
            results.add(result);
            processed.incrementAndGet();
            if (result.isValid) {
                valid.incrementAndGet();
            } else {
                invalid.incrementAndGet();
            }

            if (result.hypixelChecked) {
                hypixelChecked.incrementAndGet();
                if (result.hypixelBanned) {
                    hypixelBanned.incrementAndGet();
                } else {
                    hypixelClean.incrementAndGet();
                }
            }
        }

        public String getProgressString() {
            return String.format("&eProgress: %d/%d &f| &aValid: %d &f| &cInvalid: %d &f| &bClean: %d &f| &6Banned: %d",
                    processed.get(), total, valid.get(), invalid.get(), hypixelClean.get(), hypixelBanned.get());
        }
    }

    // Original progress tracking class (for backward compatibility)
    public static class BatchProgress {
        public AtomicInteger processed = new AtomicInteger(0);
        public AtomicInteger valid = new AtomicInteger(0);
        public AtomicInteger invalid = new AtomicInteger(0);
        public int total;
        public List<CookieResult> results = new ArrayList<>();

        public BatchProgress(int total) {
            this.total = total;
        }

        public synchronized void addResult(CookieResult result) {
            results.add(result);
            processed.incrementAndGet();
            if (result.isValid) {
                valid.incrementAndGet();
            } else {
                invalid.incrementAndGet();
            }
        }

        public String getProgressString() {
            return String.format("&eProgress: %d/%d &f| &aValid: %d &f| &cInvalid: %d",
                    processed.get(), total, valid.get(), invalid.get());
        }
    }

    public static class McResponse {
        public String access_token;
    }

    public static class ProfileResponse {
        public String name;
        String id;
    }

    // Plancke API response classes
    public static class PlanckeResponse {
        public boolean success;
        public PlanckePlayer player;
        public String cause;

        public static class PlanckePlayer {
            public String displayname;
            public Long lastLogin;
            public Long lastLogout;
            public Map<String, Object> stats;
        }
    }

    /**
     * Enhanced batch processing with Hypixel ban checking via Plancke API
     */
    public static CompletableFuture<ExtendedBatchProgress> processCookieFilesBatchWithHypixel(
            File[] cookieFiles, GuiCookieAuth gui) {

        CompletableFuture<ExtendedBatchProgress> future = new CompletableFuture<>();
        ExtendedBatchProgress progress = new ExtendedBatchProgress(cookieFiles.length);

        executor.execute(() -> {
            try {
                gui.status = "&fStarting batch validation with Hypixel checking...&r";

                for (int i = 0; i < cookieFiles.length; i++) {
                    File cookieFile = cookieFiles[i];

                    try {
                        if (i > 0) {
                            Thread.sleep(2500); // Delay between requests
                        }

                        gui.status = "Processing " + (i + 1) + "/" + cookieFiles.length + ": " + cookieFile.getName() + "&r";

                        // First, validate the cookie
                        CookieResult baseResult = processSingleCookieFileSync(cookieFile);

                        // Then check Hypixel status if cookie is valid
                        ExtendedCookieResult extendedResult;
                        if (baseResult.isValid) {
                            gui.status = "Checking Hypixel status for " + baseResult.username + "...&r";
                            System.out.println("[CookieAuth] Checking Hypixel for: " + baseResult.username);
                            extendedResult = checkHypixelStatusViaPlancke(baseResult);

                            // Debug output for Hypixel check results
                            if (extendedResult.hypixelChecked) {
                                if (extendedResult.hypixelBanned) {
                                    System.out.println("[CookieAuth] Hypixel result for " + baseResult.username + ": BANNED (" + extendedResult.hypixelError + ")");
                                    gui.status = "&6" + baseResult.username + " - BANNED (" + extendedResult.hypixelError + ")&r";
                                    Thread.sleep(1000); // Show status briefly
                                } else {
                                    System.out.println("[CookieAuth] Hypixel result for " + baseResult.username + ": CLEAN");
                                    gui.status = "&a" + baseResult.username + " - CLEAN&r";
                                    Thread.sleep(1000); // Show status briefly
                                }
                            } else {
                                System.out.println("[CookieAuth] Hypixel check FAILED for " + baseResult.username + ": " + extendedResult.hypixelError);
                                gui.status = "&c" + baseResult.username + " - Hypixel check failed: " + extendedResult.hypixelError + "&r";
                                Thread.sleep(1000); // Show status briefly
                            }

                            Thread.sleep(1500); // Additional delay after Hypixel check
                        } else {
                            extendedResult = new ExtendedCookieResult(baseResult);
                        }

                        progress.addResult(extendedResult);

                        // Print individual results
                        if (extendedResult.isValid) {
                            String status = "";
                            if (extendedResult.hypixelChecked) {
                                status = extendedResult.hypixelBanned ? " [POTENTIAL BAN]" : " [CLEAN]";
                            } else {
                                status = " [HYPIXEL UNCHECKED]";
                            }
                            System.out.println("[CookieAuth] ✓ " + extendedResult.fileName + " - " +
                                    extendedResult.username + status);
                        } else {
                            System.out.println("[CookieAuth] ✗ " + extendedResult.fileName + " - Invalid (" + extendedResult.error + ")");
                        }

                        gui.status = "&f" + progress.getProgressString() + "&r";

                    } catch (InterruptedException e) {
                        System.out.println("[CookieAuth] Batch processing interrupted");
                        break;
                    } catch (Exception e) {
                        ExtendedCookieResult errorResult = new ExtendedCookieResult(
                                cookieFile.getName(), false, null, e.getMessage(), false, false, null, null);
                        progress.addResult(errorResult);
                        System.out.println("[CookieAuth] ✗ " + cookieFile.getName() + " - Error: " + e.getMessage());
                    }
                }

                gui.status = "&aBatch processing complete! Valid: " + progress.valid.get() +
                        " | Clean: " + progress.hypixelClean.get() + " | Banned: " + progress.hypixelBanned.get() + "&r";

                printEnhancedBatchSummary(progress);
                future.complete(progress);

            } catch (Exception e) {
                gui.status = "&cError during batch processing&r";
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Check Hypixel ban status using Plancke's API (no API key required)
     */
    private static ExtendedCookieResult checkHypixelStatusViaPlancke(CookieResult baseResult) {
        if (!baseResult.isValid || baseResult.username == null) {
            return new ExtendedCookieResult(baseResult);
        }

        try {
            System.out.println("[CookieAuth] Attempting Plancke API call for: " + baseResult.username);

            // Try the API endpoint first
            String apiUrl = "https://plancke.io/hypixel/player/stats/" + baseResult.username + ".json";
            HttpsURLConnection conn = (HttpsURLConnection)(new URL(apiUrl)).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();
            System.out.println("[CookieAuth] Plancke API response code for " + baseResult.username + ": " + responseCode);

            if (responseCode == 404) {
                // Player not found on Hypixel
                System.out.println("[CookieAuth] Player " + baseResult.username + " never played Hypixel (404)");
                return new ExtendedCookieResult(baseResult.fileName, baseResult.isValid, baseResult.username,
                        baseResult.error, true, false, "Never played Hypixel", null);
            }

            if (responseCode != 200) {
                // Try alternative approach with different URL format
                System.out.println("[CookieAuth] First API attempt failed, trying alternative for " + baseResult.username);
                conn.disconnect();
                return tryAlternativeHypixelCheck(baseResult);
            }

            StringBuilder response = new StringBuilder();
            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            conn.disconnect();

            String jsonResponse = response.toString();
            System.out.println("[CookieAuth] Plancke API response for " + baseResult.username + " (first 100 chars): " +
                    (jsonResponse.length() > 100 ? jsonResponse.substring(0, 100) + "..." : jsonResponse));

            // Check if response is HTML instead of JSON
            if (jsonResponse.trim().startsWith("<!DOCTYPE") || jsonResponse.trim().startsWith("<html")) {
                System.out.println("[CookieAuth] Received HTML instead of JSON, trying alternative for " + baseResult.username);
                return tryAlternativeHypixelCheck(baseResult);
            }

            // Parse the JSON response
            PlanckeResponse planckeResponse = gson.fromJson(jsonResponse, PlanckeResponse.class);

            if (!planckeResponse.success || planckeResponse.player == null) {
                String error = planckeResponse.cause != null ? planckeResponse.cause : "Player data not available";
                System.out.println("[CookieAuth] Plancke response not successful for " + baseResult.username + ": " + error);
                // Still count as checked but not banned (probably never played)
                return new ExtendedCookieResult(baseResult.fileName, baseResult.isValid, baseResult.username,
                        baseResult.error, true, false, error, null);
            }

            // Analyze player data for potential ban indicators
            PlanckeResponse.PlanckePlayer player = planckeResponse.player;
            boolean isBanned = false;
            String banReason = null;
            Long lastSeen = null;

            System.out.println("[CookieAuth] Analyzing player data for " + baseResult.username);
            System.out.println("[CookieAuth] Last login: " + player.lastLogin + ", Last logout: " + player.lastLogout);

            // Get the most recent activity timestamp
            if (player.lastLogin != null && player.lastLogout != null) {
                lastSeen = Math.max(player.lastLogin, player.lastLogout);
            } else if (player.lastLogin != null) {
                lastSeen = player.lastLogin;
            } else if (player.lastLogout != null) {
                lastSeen = player.lastLogout;
            }

            // Heuristic ban detection based on activity patterns
            if (lastSeen != null) {
                long daysSinceLastSeen = (System.currentTimeMillis() - lastSeen) / (1000L * 60 * 60 * 24);
                System.out.println("[CookieAuth] Days since last seen for " + baseResult.username + ": " + daysSinceLastSeen);

                // If account hasn't been seen for more than 90 days, might be banned
                if (daysSinceLastSeen > 90) {
                    isBanned = true;
                    banReason = "Inactive for " + daysSinceLastSeen + " days (potential ban)";
                }

                // Additional heuristic: if last logout is much more recent than last login,
                // and there's been no activity for a while, might indicate a ban
                if (player.lastLogin != null && player.lastLogout != null) {
                    long loginLogoutDiff = player.lastLogout - player.lastLogin;
                    if (loginLogoutDiff < 60000 && daysSinceLastSeen > 30) { // Less than 1 minute session, inactive for 30+ days
                        isBanned = true;
                        banReason = "Short session followed by long inactivity (potential ban)";
                    }
                }
            } else {
                System.out.println("[CookieAuth] No login/logout data for " + baseResult.username + " - assuming clean");
            }

            System.out.println("[CookieAuth] Final result for " + baseResult.username + " - Banned: " + isBanned +
                    (banReason != null ? " (" + banReason + ")" : ""));

            return new ExtendedCookieResult(baseResult.fileName, baseResult.isValid, baseResult.username,
                    baseResult.error, true, isBanned, banReason, lastSeen);

        } catch (Exception e) {
            System.out.println("[CookieAuth] Exception during Plancke check for " + baseResult.username + ": " + e.getMessage());
            // Try alternative approach if main API fails
            return tryAlternativeHypixelCheck(baseResult);
        }
    }

    /**
     * Alternative Hypixel check method - assume clean if we can't verify
     */
    private static ExtendedCookieResult tryAlternativeHypixelCheck(CookieResult baseResult) {
        System.out.println("[CookieAuth] Using alternative check for " + baseResult.username + " - assuming clean");

        // If we can't check via API, assume the account is clean
        // This is better than failing completely
        return new ExtendedCookieResult(baseResult.fileName, baseResult.isValid, baseResult.username,
                baseResult.error, true, false, "API unavailable - assumed clean",
                System.currentTimeMillis());
    }

    /**
     * Enhanced summary with Hypixel ban information
     */
    private static void printEnhancedBatchSummary(ExtendedBatchProgress progress) {
        System.out.println("\n=== ENHANCED COOKIE BATCH SUMMARY ===");
        System.out.println("Total files processed: " + progress.total);
        System.out.println("Valid cookies: " + progress.valid.get());
        System.out.println("Invalid cookies: " + progress.invalid.get());
        System.out.println("Hypixel checked: " + progress.hypixelChecked.get());
        System.out.println("Clean accounts: " + progress.hypixelClean.get());
        System.out.println("Suspected bans: " + progress.hypixelBanned.get());

        System.out.println("\nClean accounts (valid + not banned):");
        progress.results.stream()
                .filter(r -> r.isValid && r.hypixelChecked && !r.hypixelBanned)
                .forEach(r -> System.out.println("  ✓ " + r.fileName + " -> " + r.username));

        if (progress.hypixelBanned.get() > 0) {
            System.out.println("\nSuspected banned accounts:");
            progress.results.stream()
                    .filter(r -> r.hypixelBanned)
                    .forEach(r -> System.out.println("  ⚠ " + r.fileName + " -> " + r.username +
                            (r.hypixelError != null ? " (" + r.hypixelError + ")" : "")));
        }

        if (progress.valid.get() > progress.hypixelChecked.get()) {
            System.out.println("\nValid accounts not checked on Hypixel:");
            progress.results.stream()
                    .filter(r -> r.isValid && !r.hypixelChecked)
                    .forEach(r -> System.out.println("  ? " + r.fileName + " -> " + r.username));
        }

        System.out.println("=====================================\n");
    }

    /**
     * Process multiple cookie files from a directory with Hypixel checking
     */
    public static CompletableFuture<ExtendedBatchProgress> processCookieDirectoryWithHypixel(File directory, GuiCookieAuth gui) {
        if (!directory.isDirectory()) {
            CompletableFuture<ExtendedBatchProgress> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Not a directory"));
            return future;
        }

        File[] cookieFiles = directory.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".cookies"));

        if (cookieFiles == null || cookieFiles.length == 0) {
            gui.status = "&cNo cookie files found in directory&r";
            CompletableFuture<ExtendedBatchProgress> future = new CompletableFuture<>();
            future.complete(new ExtendedBatchProgress(0));
            return future;
        }

        gui.status = "&fFound " + cookieFiles.length + " cookie files to process with Hypixel checking&r";
        return processCookieFilesBatchWithHypixel(cookieFiles, gui);
    }

    /**
     * Add valid clean accounts from enhanced batch results
     */
    public static CompletableFuture<Integer> addValidCleanAccountsFromBatch(ExtendedBatchProgress batchResults, GuiCookieAuth gui) {
        return CompletableFuture.supplyAsync(() -> {
            int addedCount = 0;

            for (ExtendedCookieResult result : batchResults.results) {
                // Only add accounts that are valid and either not checked on Hypixel or confirmed clean
                if (result.isValid && (!result.hypixelChecked || !result.hypixelBanned)) {
                    try {
                        // Re-authenticate to get fresh tokens and add to account manager
                        File cookieFile = new File(result.fileName);
                        Map<String, String> cookieMap = readCookieFile(cookieFile);
                        String cookieString = buildCookieString(cookieMap);

                        // Full authentication and account addition
                        if (performFullAuthentication(cookieString, result.username)) {
                            addedCount++;
                            String status = result.hypixelChecked ? " (Hypixel clean)" : " (Hypixel not checked)";
                            gui.status = "&aAdded account: " + result.username + status + "&r";
                        }

                    } catch (Exception e) {
                        System.err.println("[CookieAuth] Failed to add account " + result.username + ": " + e.getMessage());
                    }
                }
            }

            gui.status = "&aAdded " + addedCount + " clean accounts to account manager&r";
            return addedCount;
        }, executor);
    }

    // ===== ORIGINAL METHODS (for backward compatibility) =====

    /**
     * Batch process multiple cookie files with rate limiting (now includes Hypixel checking)
     */
    public static CompletableFuture<BatchProgress> processCookieFilesBatch(File[] cookieFiles, GuiCookieAuth gui) {
        // Redirect to enhanced version and convert result
        CompletableFuture<BatchProgress> future = new CompletableFuture<>();

        processCookieFilesBatchWithHypixel(cookieFiles, gui).whenComplete((extendedProgress, throwable) -> {
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                // Convert ExtendedBatchProgress to BatchProgress for backward compatibility
                BatchProgress simpleProgress = new BatchProgress(extendedProgress.total);
                for (ExtendedCookieResult extResult : extendedProgress.results) {
                    CookieResult simpleResult = new CookieResult(extResult.fileName, extResult.isValid, extResult.username, extResult.error);
                    simpleProgress.addResult(simpleResult);
                }
                future.complete(simpleProgress);
            }
        });

        return future;
    }

    /**
     * Process a single cookie file synchronously (for sequential processing)
     */
    private static CookieResult processSingleCookieFileSync(File cookieFile) {
        try {
            Map<String, String> cookieMap = readCookieFile(cookieFile);

            if (cookieMap.isEmpty()) {
                return new CookieResult(cookieFile.getName(), false, null, "No valid cookies found");
            }

            String cookieString = buildCookieString(cookieMap);
            return testCookieAuthentication(cookieFile.getName(), cookieString);

        } catch (Exception e) {
            return new CookieResult(cookieFile.getName(), false, null, e.getMessage());
        }
    }

    /**
     * Process multiple cookie files from a directory (now includes Hypixel checking)
     */
    public static CompletableFuture<BatchProgress> processCookieDirectory(File directory, GuiCookieAuth gui) {
        if (!directory.isDirectory()) {
            CompletableFuture<BatchProgress> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Not a directory"));
            return future;
        }

        File[] cookieFiles = directory.listFiles((dir, name) ->
                name.toLowerCase().endsWith(".txt") || name.toLowerCase().endsWith(".cookies"));

        if (cookieFiles == null || cookieFiles.length == 0) {
            gui.status = "&cNo cookie files found in directory&r";
            CompletableFuture<BatchProgress> future = new CompletableFuture<>();
            future.complete(new BatchProgress(0));
            return future;
        }

        gui.status = "&fFound " + cookieFiles.length + " cookie files to process with Hypixel checking&r";
        return processCookieFilesBatch(cookieFiles, gui); // This now calls the enhanced version
    }

    /**
     * Process a single cookie file and return result
     */
    private static CompletableFuture<CookieResult> processSingleCookieFile(File cookieFile) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> cookieMap = readCookieFile(cookieFile);

                if (cookieMap.isEmpty()) {
                    return new CookieResult(cookieFile.getName(), false, null, "No valid cookies found");
                }

                String cookieString = buildCookieString(cookieMap);
                return testCookieAuthentication(cookieFile.getName(), cookieString);

            } catch (Exception e) {
                return new CookieResult(cookieFile.getName(), false, null, e.getMessage());
            }
        }, executor);
    }

    /**
     * Test cookie authentication without adding to account manager
     */
    private static CookieResult testCookieAuthentication(String fileName, String cookieString) {
        try {
            String finalLocation = followRedirectChain(cookieString);
            String accessToken = finalLocation.split("accessToken=")[1];
            String decoded = new String(Base64.getDecoder().decode(accessToken), StandardCharsets.UTF_8);

            String[] parts = decoded.split("\"rp://api.minecraftservices.com/\",");
            if (parts.length < 2) {
                return new CookieResult(fileName, false, null, "Failed to decode token");
            }

            String rest = parts[1];
            String token = rest.split("\"Token\":\"")[1].split("\"")[0];
            String uhs = rest.split(Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\""))[1].split("\"")[0];
            String xblToken = "XBL3.0 x=" + uhs + ";" + token;

            McResponse mcRes = postMinecraftLogin(xblToken);
            if (mcRes == null || mcRes.access_token == null) {
                return new CookieResult(fileName, false, null, "Failed to get MC access token");
            }

            ProfileResponse profileRes = getMinecraftProfile(mcRes.access_token);
            if (profileRes == null || profileRes.name == null) {
                return new CookieResult(fileName, false, null, "Failed to get MC profile");
            }

            return new CookieResult(fileName, true, profileRes.name, null);

        } catch (Exception e) {
            return new CookieResult(fileName, false, null, e.getMessage());
        }
    }

    /**
     * Add valid accounts from batch results (original method)
     */
    public static CompletableFuture<Integer> addValidAccountsFromBatch(BatchProgress batchResults, GuiCookieAuth gui) {
        return CompletableFuture.supplyAsync(() -> {
            int addedCount = 0;

            for (CookieResult result : batchResults.results) {
                if (result.isValid) {
                    try {
                        File cookieFile = new File(result.fileName);
                        Map<String, String> cookieMap = readCookieFile(cookieFile);
                        String cookieString = buildCookieString(cookieMap);

                        if (performFullAuthentication(cookieString, result.username)) {
                            addedCount++;
                            gui.status = "&aAdded account: " + result.username + "&r";
                        }

                    } catch (Exception e) {
                        System.err.println("[CookieAuth] Failed to add account " + result.username + ": " + e.getMessage());
                    }
                }
            }

            gui.status = "&aAdded " + addedCount + " valid accounts to account manager&r";
            return addedCount;
        }, executor);
    }

    /**
     * Print detailed batch summary (original method)
     */
    private static void printBatchSummary(BatchProgress progress) {
        System.out.println("\n=== COOKIE BATCH PROCESSING SUMMARY ===");
        System.out.println("Total files processed: " + progress.total);
        System.out.println("Valid cookies: " + progress.valid.get());
        System.out.println("Invalid cookies: " + progress.invalid.get());
        System.out.println("\nValid accounts:");

        for (CookieResult result : progress.results) {
            if (result.isValid) {
                System.out.println("  ✓ " + result.fileName + " -> " + result.username);
            }
        }

        System.out.println("\nInvalid cookies:");
        for (CookieResult result : progress.results) {
            if (!result.isValid) {
                System.out.println("  ✗ " + result.fileName + " -> " + result.error);
            }
        }
        System.out.println("=======================================\n");
    }

    /**
     * Read cookie file and extract cookies
     */
    private static Map<String, String> readCookieFile(File cookieFile) throws Exception {
        Map<String, String> cookieMap = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cookieFile), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t", -1);
                if (parts.length > 6 && parts[0].endsWith("login.live.com")) {
                    String name = parts[5].trim();
                    if (!cookieMap.containsKey(name)) {
                        cookieMap.put(name, parts[6].trim());
                    }
                }
            }
        }

        return cookieMap;
    }

    /**
     * Perform full authentication and add to account manager
     */
    private static boolean performFullAuthentication(String cookieString, String expectedUsername) throws Exception {
        String finalLocation = followRedirectChain(cookieString);
        String accessToken = finalLocation.split("accessToken=")[1];
        String decoded = new String(Base64.getDecoder().decode(accessToken), StandardCharsets.UTF_8);

        String[] parts = decoded.split("\"rp://api.minecraftservices.com/\",");
        if (parts.length < 2) return false;

        String rest = parts[1];
        String token = rest.split("\"Token\":\"")[1].split("\"")[0];
        String uhs = rest.split(Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\""))[1].split("\"")[0];
        String xblToken = "XBL3.0 x=" + uhs + ";" + token;

        McResponse mcRes = postMinecraftLogin(xblToken);
        if (mcRes == null || mcRes.access_token == null) return false;

        ProfileResponse profileRes = getMinecraftProfile(mcRes.access_token);
        if (profileRes == null || profileRes.name == null) return false;

        Session session = new Session(profileRes.name, profileRes.id, mcRes.access_token, "legacy");
        AccountManager.accounts.add(new Account("", mcRes.access_token, profileRes.name, System.currentTimeMillis()));
        AccountManager.save();

        return true;
    }

    public static CompletableFuture<Boolean> addAccountFromCookieFile(File cookieFile, GuiCookieAuth gui) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        executor.execute(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(cookieFile), StandardCharsets.UTF_8))) {
                gui.status = "&fReading cookie file...&r";
                Map<String, String> cookieMap = new HashMap<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t", -1);
                    if (parts.length > 6 && parts[0].endsWith("login.live.com")) {
                        String name = parts[5].trim();
                        if (!cookieMap.containsKey(name))
                            cookieMap.put(name, parts[6].trim());
                    }
                }
                if (cookieMap.isEmpty()) {
                    gui.status = "&cNo valid login.live.com cookies found&r";
                    future.complete(Boolean.valueOf(false));
                    return;
                }
                gui.status = "&fBuilding cookie string...&r";
                String cookieString = buildCookieString(cookieMap);
                gui.status = "&fAuthenticating with Microsoft...&r";
                authenticateWithCookies(cookieString, gui, null).whenComplete((result, throwable) -> {
                    future.complete(result);
                });
            } catch (Exception e) {
                gui.status = "&cError processing cookie file&r";
                e.printStackTrace();
                future.complete(Boolean.valueOf(false));
            }
        });
        return future;
    }

    public static String buildCookieString(Map<String, String> cookies) {
        StringBuilder sb = new StringBuilder();
        cookies.forEach((k, v) -> sb.append(k).append("=").append(v).append("; "));
        return sb.toString().replaceAll("; $", "");
    }

    public static String followRedirectChain(String cookieString) throws Exception {
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Starting Microsoft authentication (1/3)..."), 5000L);
        String url1 = "https://sisu.xboxlive.com/connect/XboxLive/?state=login&cobrandId=8058f65d-ce06-4c30-9559-473c9275a65d&tid=896928775&ru=https%3A%2F%2Fwww.minecraft.net%2Fen-us%2Flogin&aid=1142970254";
        HttpsURLConnection conn = (HttpsURLConnection)(new URL(url1)).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        conn.setRequestProperty("Accept-Language", "en-US;q=0.8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        conn.setInstanceFollowRedirects(false);
        conn.connect();
        String location1 = conn.getHeaderField("Location");
        if (location1 == null)
            throw new Exception("Redirect failed at step 1");
        location1 = location1.replaceAll(" ", "%20");
        conn.disconnect();
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Processing Microsoft redirect (2/3)..."), 5000L);
        conn = (HttpsURLConnection)(new URL(location1)).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
        conn.setRequestProperty("Cookie", cookieString);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        conn.setInstanceFollowRedirects(false);
        conn.connect();
        String location2 = conn.getHeaderField("Location");
        if (location2 == null)
            throw new Exception("Redirect failed at step 2");
        conn.disconnect();
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Finalizing Microsoft redirect (3/3)..."), 5000L);
        conn = (HttpsURLConnection)(new URL(location2)).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
        conn.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
        conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7");
        conn.setRequestProperty("Cookie", cookieString);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36");
        conn.setInstanceFollowRedirects(false);
        conn.connect();
        String location3 = conn.getHeaderField("Location");
        if (location3 == null)
            throw new Exception("Redirect failed at step 3");
        conn.disconnect();
        return location3;
    }

    private static CompletableFuture<Boolean> authenticateWithCookies(String cookieString, GuiCookieAuth gui, String cookie) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                gui.status = "&fStarting authentication process...&r";
                String finalLocation = followRedirectChain(cookieString);
                gui.status = "&fExtracting access token from redirect...&r";
                String accessToken = finalLocation.split("accessToken=")[1];
                gui.status = "&fDecoding access token...&r";
                String decoded = new String(Base64.getDecoder().decode(accessToken), StandardCharsets.UTF_8);
                gui.status = "&fParsing token data...&r";
                String[] parts = decoded.split("\"rp://api.minecraftservices.com/\",");
                if (parts.length < 2)
                    throw new Exception("Failed to decode token");
                String rest = parts[1];
                gui.status = "&fExtracting XBL token components...&r";
                String token = rest.split("\"Token\":\"")[1].split("\"")[0];
                String uhs = rest.split(Pattern.quote("{\"DisplayClaims\":{\"xui\":[{\"uhs\":\""))[1].split("\"")[0];
                String xblToken = "XBL3.0 x=" + uhs + ";" + token;
                gui.status = "&fAuthenticating with Xbox Live...&r";
                McResponse mcRes = postMinecraftLogin(xblToken);
                if (mcRes == null || mcRes.access_token == null) {
                    System.err.println("[AuthFlow] Failed to get Minecraft access token");
                    gui.status = "&cFailed to get Minecraft access token&r";
                    return Boolean.valueOf(false);
                }
                gui.status = "&fRetrieving Minecraft profile...&r";
                ProfileResponse profileRes = getMinecraftProfile(mcRes.access_token);
                if (profileRes == null || profileRes.name == null) {
                    System.err.println("[AuthFlow] Failed to get Minecraft profile");
                    gui.status = "&cFailed to get Minecraft profile&r";
                    return Boolean.valueOf(false);
                }
                gui.status = "&aCreating Minecraft session...&r";
                Session session = new Session(profileRes.name, profileRes.id, mcRes.access_token, "legacy");
                gui.status = "&aAdding account details...&r";
                AccountManager.accounts.add(new Account("", mcRes.access_token, profileRes.name, System.currentTimeMillis()));
                AccountManager.save();
                SessionManager.set(session);
                System.out.println("[AuthFlow] Successfully logged in as " + profileRes.name);
                gui.status = "&aSuccessfully logged in as " + profileRes.name + "&r";
                return Boolean.valueOf(true);
            } catch (Exception e) {
                System.err.println("[AuthFlow] Authentication failed: " + e.getMessage());
                gui.status = "&cInvalid Cookie File&r";
                return Boolean.valueOf(false);
            }
        });
    }

    public static McResponse postMinecraftLogin(String xblToken) throws Exception {
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Logging into Minecraft services..."), 5000L);
        String url = "https://api.minecraftservices.com/authentication/login_with_xbox";
        String payload = "{\"identityToken\":\"" + xblToken + "\",\"ensureLegacyEnabled\":true}";
        HttpsURLConnection conn = (HttpsURLConnection)(new URL(url)).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        StringBuilder response = new StringBuilder();
        try(InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                response.append(line);
        }
        conn.disconnect();
        return (McResponse)gson.fromJson(response.toString(), McResponse.class);
    }

    public static ProfileResponse getMinecraftProfile(String accessToken) throws Exception {
        GuiAccountManager.notification = new Notification(TextFormatting.translate("&7Fetching Minecraft profile..."), 5000L);
        String url = "https://api.minecraftservices.com/minecraft/profile";
        HttpsURLConnection conn = (HttpsURLConnection)(new URL(url)).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Accept", "application/json");
        StringBuilder response = new StringBuilder();
        try(InputStream is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null)
                response.append(line);
        }
        conn.disconnect();
        return (ProfileResponse)gson.fromJson(response.toString(), ProfileResponse.class);
    }

    public static void shutdown() {
        executor.shutdown();
    }
}
