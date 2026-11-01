package dev.autobuy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Config {
    public static class Rule {
        public String name;
        public boolean enabled;
        public long maxPrice;
        public List<String> keywords = new ArrayList<>();

        public Rule(String name, boolean enabled, long maxPrice, String... kw) {
            this.name = name;
            this.enabled = enabled;
            this.maxPrice = maxPrice;
            this.keywords.addAll(List.of(kw));
        }
    }

    public List<Rule> rules = new ArrayList<>(List.of(
            new Rule("Талисманы", true, 100000, "талисман"),
            new Rule("ТНТ", false, 5000, "tnt"),
            new Rule("Мечи", false, 50000, "_sword"),
            new Rule("Элитры", false, 100000, "elytra"),
            new Rule("Тотемы", false, 20000, "totem")
    ));

    public int refreshSlot = 49;
    public int minDelayTicks = 4;
    public int maxDelayTicks = 9;
    public String priceRegex = "(?i)цена\\D*([\\d][\\d\\s.,]*)";
    public List<String> confirmWords = new ArrayList<>(List.of("подтверд", "купить", "confirm"));

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("autobuy.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Config I = load();

    public static Config load() {
        try {
            if (Files.exists(FILE)) {
                Config c = GSON.fromJson(Files.readString(FILE), Config.class);
                if (c != null) return c;
            }
        } catch (Exception ignored) {}
        Config c = new Config();
        c.save();
        return c;
    }

    public void save() {
        try { Files.writeString(FILE, GSON.toJson(this)); } catch (Exception ignored) {}
    }
}
