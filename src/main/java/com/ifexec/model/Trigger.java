package com.ifexec.model;

import org.bukkit.Location;

import java.util.List;

public class Trigger {
    public enum Type { BLOCK, REGION }

    private String name;
    private String role;
    private Type type;
    private boolean enabled;
    private List<String> commands;
    private String world;
    private int x,y,z,x1,y1,z1,x2,y2,z2;

    public boolean matches(Location loc) {
        if (!loc.getWorld().getName().equals(world)) return false;
        if (type == Type.BLOCK) {
            return loc.getBlockX()==x && loc.getBlockY()==y && loc.getBlockZ()==z;
        } else {
            int lx=loc.getBlockX(),ly=loc.getBlockY(),lz=loc.getBlockZ();
            return lx>=Math.min(x1,x2)&&lx<=Math.max(x1,x2)&&
                   ly>=Math.min(y1,y2)&&ly<=Math.max(y1,y2)&&
                   lz>=Math.min(z1,z2)&&lz<=Math.max(z1,z2);
        }
    }

    // getters/setters omitted for brevity
    // ...
}
