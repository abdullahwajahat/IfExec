package com.ifexec.model;

import org.bukkit.Location;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Trigger {
    public enum Type { BLOCK, REGION }

    private String name;
    private Type type = Type.BLOCK;
    private String world;

    // block coords
    private int x, y, z;

    // region coords
    private int x1, y1, z1, x2, y2, z2;

    private List<String> commands = new ArrayList<>();
    private boolean enabled = true;
    private int cooldown = -1; // seconds, -1 => use default
    private boolean silent = false;
    private String role = "all"; // staff or all
    private Map<String, String> messages = new HashMap<>(); // keys: staff, all

    // per-player cooldown timestamp
    private transient Map<java.util.UUID, Long> lastTriggered = new ConcurrentHashMap<>();

    public Trigger() {}

    public Trigger cloneTrigger() {
        Trigger t = new Trigger();
        t.name = name;
        t.type = type;
        t.world = world;
        t.x = x; t.y = y; t.z = z;
        t.x1 = x1; t.y1 = y1; t.z1 = z1;
        t.x2 = x2; t.y2 = y2; t.z2 = z2;
        t.commands = new ArrayList<>(commands);
        t.enabled = enabled;
        t.cooldown = cooldown;
        t.silent = silent;
        t.role = role;
        t.messages = new HashMap<>(messages);
        return t;
    }

    // Getters / setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    public int getX() { return x; } public void setX(int x) { this.x = x; }
    public int getY() { return y; } public void setY(int y) { this.y = y; }
    public int getZ() { return z; } public void setZ(int z) { this.z = z; }

    public int getX1() { return x1; } public void setX1(int x1) { this.x1 = x1; }
    public int getY1() { return y1; } public void setY1(int y1) { this.y1 = y1; }
    public int getZ1() { return z1; } public void setZ1(int z1) { this.z1 = z1; }
    public int getX2() { return x2; } public void setX2(int x2) { this.x2 = x2; }
    public int getY2() { return y2; } public void setY2(int y2) { this.y2 = y2; }
    public int getZ2() { return z2; } public void setZ2(int z2) { this.z2 = z2; }

    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> commands) { this.commands = commands; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getCooldown() { return cooldown; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }

    public boolean isSilent() { return silent; }
    public void setSilent(boolean silent) { this.silent = silent; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Map<String,String> getMessages() { return messages; }
    public void setMessages(Map<String,String> messages) { this.messages = messages; }

    // cooldown helpers
    public boolean canTrigger(java.util.UUID playerUuid, int defaultCooldown) {
        long now = System.currentTimeMillis();
        int cd = (cooldown >= 0) ? cooldown : defaultCooldown;
        Long last = lastTriggered.get(playerUuid);
        if (last == null) return true;
        return (now - last) >= (cd * 1000L);
    }

    public void setTriggered(java.util.UUID playerUuid) {
        lastTriggered.put(playerUuid, System.currentTimeMillis());
    }

    public boolean isInBlock(Location loc) {
        if (loc == null) return false;
        if (this.world == null) return false;
        if (!loc.getWorld().getName().equals(this.world)) return false;
        return loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z;
    }

    public boolean isInRegion(Location loc) {
        if (loc == null) return false;
        if (this.world == null) return false;
        if (!loc.getWorld().getName().equals(this.world)) return false;
        int lx = loc.getBlockX(); int ly = loc.getBlockY(); int lz = loc.getBlockZ();
        int minX = Math.min(x1, x2); int maxX = Math.max(x1,x2);
        int minY = Math.min(y1, y2); int maxY = Math.max(y1,y2);
        int minZ = Math.min(z1, z2); int maxZ = Math.max(z1,z2);
        return lx >= minX && lx <= maxX && ly >= minY && ly <= maxY && lz >= minZ && lz <= maxZ;
    }
}
