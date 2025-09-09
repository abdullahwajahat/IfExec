package me.yourname.ifexec.models;

import org.bukkit.World;

import java.util.List;

public class Trigger {
    private final String name;
    private final World world;
    private final int x1, y1, z1, x2, y2, z2;
    private final List<String> commands;
    private final String role; // "all", "staff", "members"
    private final int cooldown;

    public Trigger(String name, World world, int x1, int y1, int z1, int x2, int y2, int z2,
                   List<String> commands, String role, int cooldown) {
        this.name = name;
        this.world = world;
        this.x1 = x1; this.y1 = y1; this.z1 = z1;
        this.x2 = x2; this.y2 = y2; this.z2 = z2;
        this.commands = commands;
        this.role = role;
        this.cooldown = cooldown;
    }

    public String getName() { return name; }
    public World getWorld() { return world; }
    public List<String> getCommands() { return commands; }
    public String getRole() { return role; }
    public int getCooldown() { return cooldown; }

    public boolean isInside(int x, int y, int z) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        return x >= minX && x <= maxX &&
               y >= minY && y <= maxY &&
               z >= minZ && z <= maxZ;
    }
}
