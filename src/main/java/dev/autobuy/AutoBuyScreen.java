package dev.autobuy;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AutoBuyScreen extends Screen {
    private final List<ClickableWidget> dyn = new ArrayList<>();
    private TextFieldWidget searchField;
    private ButtonWidget prevBtn, nextBtn;
    private String query = "";
    private int page = 0, rows = 5, total = 0;
    private boolean dirty = false;

    public AutoBuyScreen() { super(Text.literal("AutoBuy")); }

    @Override
    protected void init() {
        dyn.clear();
        int x = width / 2 - 130;

        addDrawableChild(ButtonWidget.builder(label("Автобай", AutoBuyMod.enabled), b -> {
            AutoBuyMod.enabled = !AutoBuyMod.enabled;
            AutoBuyMod.stage = AutoBuyMod.Stage.OPEN;
            b.setMessage(label("Автобай", AutoBuyMod.enabled));
        }).dimensions(x, 6, 260, 20).build());

        searchField = new TextFieldWidget(textRenderer, x, 30, 260, 18, Text.literal("Поиск"));
        searchField.setPlaceholder(Text.literal("Поиск предмета, например: тотем"));
        searchField.setText(query);
        searchField.setChangedListener(s -> { query = s; page = 0; dirty = true; });
        addDrawableChild(searchField);

        rows = Math.max(3, Math.min(8, (height - 56 - 30) / 22));

        prevBtn = addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            if (page > 0) { page--; dirty = true; }
        }).dimensions(x, height - 24, 60, 20).build());
        nextBtn = addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            if ((page + 1) * rows < total) { page++; dirty = true; }
        }).dimensions(x + 200, height - 24, 60, 20).build());

        rebuild();
    }

    private void add(ClickableWidget w) {
        addDrawableChild(w);
        dyn.add(w);
    }

    private void rebuild() {
        for (ClickableWidget w : dyn) remove(w);
        dyn.clear();
        int x = width / 2 - 130;
        int y = 56;
        int from = page * rows;

        if (query.isBlank()) {
            List<Config.Rule> rules = Config.I.rules;
            total = rules.size();
            for (int i = from; i < Math.min(total, from + rows); i++) {
                Config.Rule r = rules.get(i);
                add(ButtonWidget.builder(label(r.name, r.enabled), b -> {
                    r.enabled = !r.enabled;
                    b.setMessage(label(r.name, r.enabled));
                    Config.I.save();
                }).dimensions(x, y, 120, 20).build());

                TextFieldWidget f = new TextFieldWidget(textRenderer, x + 124, y, 100, 20, Text.literal("max"));
                f.setText(String.valueOf(r.maxPrice));
                f.setChangedListener(s -> {
                    try { r.maxPrice = Long.parseLong(s.replaceAll("\\D", "")); } catch (Exception ignored) {}
                });
                add(f);

                add(ButtonWidget.builder(Text.literal("X"), b -> {
                    Config.I.rules.remove(r);
                    Config.I.save();
                    dirty = true;
                }).dimensions(x + 228, y, 32, 20).build());
                y += 22;
            }
        } else {
            String q = query.toLowerCase().trim();
            List<Item> found = new ArrayList<>();
            for (Item it : Registries.ITEM) {
                if (it == Items.AIR) continue;
                String id = Registries.ITEM.getId(it).getPath();
                String nm = it.getName().getString().toLowerCase();
                if (id.contains(q) || nm.contains(q)) found.add(it);
            }
            total = found.size();
            for (int i = from; i < Math.min(total, from + rows); i++) {
                Item it = found.get(i);
                add(ButtonWidget.builder(Text.literal("+ " + it.getName().getString()), b -> {
                    addItemRule(it);
                    query = "";
                    page = 0;
                    searchField.setText("");
                    dirty = true;
                }).dimensions(x, y, 260, 20).build());
                y += 22;
            }
        }
        prevBtn.active = page > 0;
        nextBtn.active = (page + 1) * rows < total;
    }

    private static void addItemRule(Item it) {
        String id = Registries.ITEM.getId(it).getPath();
        for (Config.Rule r : Config.I.rules) if (id.equals(r.itemId)) return;
        String name = it.getName().getString();
        Config.Rule r = new Config.Rule(name, true, 100000);
        r.itemId = id;
        r.search = name.toLowerCase();
        Config.I.rules.add(r);
        Config.I.save();
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "§aВКЛ" : "§cВЫКЛ"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (dirty) { dirty = false; rebuild(); }
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Стр. " + (page + 1)), width / 2, height - 18, 0xAAAAAA);
    }

    @Override
    public void close() {
        Config.I.save();
        super.close();
    }
            }
