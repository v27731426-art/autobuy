package dev.autobuy;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class AutoBuyScreen extends Screen {
    public AutoBuyScreen() { super(Text.literal("AutoBuy")); }

    @Override
    protected void init() {
        int x = width / 2 - 130;
        int y = 40;

        addDrawableChild(ButtonWidget.builder(label("Автобай", AutoBuyMod.enabled), b -> {
            AutoBuyMod.enabled = !AutoBuyMod.enabled;
            AutoBuyMod.stage = AutoBuyMod.Stage.OPEN;
            b.setMessage(label("Автобай", AutoBuyMod.enabled));
        }).dimensions(x, y - 25, 260, 20).build());

        for (Config.Rule r : Config.I.rules) {
            ButtonWidget btn = ButtonWidget.builder(label(r.name, r.enabled), b -> {
                r.enabled = !r.enabled;
                b.setMessage(label(r.name, r.enabled));
                Config.I.save();
            }).dimensions(x, y, 150, 20).build();
            addDrawableChild(btn);

            TextFieldWidget f = new TextFieldWidget(textRenderer, x + 160, y, 100, 20, Text.literal("max"));
            f.setText(String.valueOf(r.maxPrice));
            f.setChangedListener(s -> {
                try { r.maxPrice = Long.parseLong(s.replaceAll("\\D", "")); } catch (Exception ignored) {}
            });
            addDrawableChild(f);
            y += 24;
        }
    }

    private static Text label(String name, boolean on) {
        return Text.literal(name + ": " + (on ? "§aВКЛ" : "§cВЫКЛ"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Макс. цена справа"), width / 2, 8, 0xAAAAAA);
    }

    @Override
    public void close() {
        Config.I.save();
        super.close();
    }
}
