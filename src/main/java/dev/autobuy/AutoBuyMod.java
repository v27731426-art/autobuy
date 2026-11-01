package dev.autobuy;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoBuyMod implements ClientModInitializer {
    enum Stage { OPEN, SCAN, CONFIRM }

    static boolean enabled = false;
    static Stage stage = Stage.OPEN;
    static int cooldown = 0;
    static int confirmWait = 0;
    static int ruleIndex = 0;
    static final Random RNG = new Random();
    static KeyBinding toggleKey, menuKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("autobuy", "main"));
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Автобай вкл/выкл", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_BRACKET, cat));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("Меню автобая", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));
        ClientTickEvents.END_CLIENT_TICK.register(AutoBuyMod::tick);
    }

    static void say(MinecraftClient mc, String s) {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[AutoBuy] " + s), true);
    }

    static void delay() {
        Config c = Config.I;
        cooldown = c.minDelayTicks + RNG.nextInt(Math.max(1, c.maxDelayTicks - c.minDelayTicks + 1));
    }

    static List<Config.Rule> active() {
        List<Config.Rule> l = new ArrayList<>();
        for (Config.Rule r : Config.I.rules) if (r.enabled) l.add(r);
        return l;
    }

    static void tick(MinecraftClient mc) {
        while (toggleKey.wasPressed()) {
            enabled = !enabled;
            stage = Stage.OPEN;
            cooldown = 0;
            say(mc, enabled ? "ВКЛ" : "ВЫКЛ");
        }
        while (menuKey.wasPressed()) {
            if (mc.currentScreen == null) mc.setScreen(new AutoBuyScreen());
        }

        if (!enabled || mc.player == null || mc.interactionManager == null) return;
        if (cooldown > 0) { cooldown--; return; }

        var handler = mc.player.currentScreenHandler;

        if (!(handler instanceof GenericContainerScreenHandler h)) {
            if (mc.currentScreen != null) return;
            List<Config.Rule> act = active();
            if (act.isEmpty()) {
                enabled = false;
                say(mc, "Включи хотя бы один предмет");
                return;
            }
            Config.Rule pick = act.get(ruleIndex++ % act.size());
            String cmd = (pick.search != null && !pick.search.isBlank()) ? "ah search " + pick.search : "ah";
            mc.player.networkHandler.sendChatCommand(cmd);
            stage = Stage.SCAN;
            cooldown = 10 + RNG.nextInt(8);
            return;
        }

        int rows = h.getRows();
        int size = rows * 9;

        if (stage == Stage.CONFIRM) {
            int slot = findConfirm(h, size);
            if (slot >= 0) {
                mc.interactionManager.clickSlot(h.syncId, slot, 0, SlotActionType.PICKUP, mc.player);
                stage = Stage.OPEN;
                delay();
                return;
            }
            if (++confirmWait > 20) { stage = Stage.SCAN; confirmWait = 0; }
            return;
        }

        int limit = Math.max(0, size - 9);
        for (int i = 0; i < limit; i++) {
            ItemStack s = h.getSlot(i).getStack();
            if (s.isEmpty()) continue;
            Config.Rule r = match(s);
            if (r == null) continue;
            long p = price(s);
            if (p < 0 || p > r.maxPrice) continue;
            mc.interactionManager.clickSlot(h.syncId, i, 0, SlotActionType.PICKUP, mc.player);
            stage = Stage.CONFIRM;
            confirmWait = 0;
            delay();
            return;
        }

        int rs = Config.I.refreshSlot;
        if (active().size() <= 1 && rs >= 0 && rs < size) {
            mc.interactionManager.clickSlot(h.syncId, rs, 0, SlotActionType.PICKUP, mc.player);
        } else {
            mc.player.closeHandledScreen();
            stage = Stage.OPEN;
        }
        delay();
    }

    static Config.Rule match(ItemStack s) {
        String path = Registries.ITEM.getId(s.getItem()).getPath();
        String name = s.getName().getString().toLowerCase();
        for (Config.Rule r : Config.I.rules) {
            if (!r.enabled) continue;
            if (r.itemId != null && !r.itemId.isEmpty()) {
                if (path.equals(r.itemId)) return r;
                continue;
            }
            for (String k : r.keywords) {
                String kl = k.toLowerCase();
                if (path.contains(kl) || name.contains(kl)) return r;
            }
        }
        return null;
    }

    static long price(ItemStack s) {
        LoreComponent lore = s.get(DataComponentTypes.LORE);
        if (lore == null) return -1;
        Pattern p = Pattern.compile(Config.I.priceRegex);
        for (Text t : lore.lines()) {
            Matcher m = p.matcher(t.getString());
            if (m.find()) {
                String d = m.group(1).replaceAll("\\D", "");
                if (!d.isEmpty() && d.length() < 18) return Long.parseLong(d);
            }
        }
        return -1;
    }

    static int findConfirm(GenericContainerScreenHandler h, int size) {
        for (int i = 0; i < size; i++) {
            ItemStack s = h.getSlot(i).getStack();
            if (s.isEmpty()) continue;
            String n = s.getName().getString().toLowerCase();
            for (String w : Config.I.confirmWords) if (n.contains(w)) return i;
            if (s.isOf(Items.LIME_STAINED_GLASS_PANE) || s.isOf(Items.GREEN_STAINED_GLASS_PANE)) return i;
        }
        return -1;
    }
                                            }
