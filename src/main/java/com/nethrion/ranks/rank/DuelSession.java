package com.nethrion.ranks.rank;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Ek active, accepted duel ki live state. Fight khatam hone tak
 * dono players ka weapon-usage (skill ke hisaab se) track karta hai.
 */
public class DuelSession {

    private final UUID playerA;
    private final UUID playerB;
    private final long startTime;
    private final Map<UUID, Map<Skill, Double>> damageBySkill = new HashMap<>();

    public DuelSession(UUID playerA, UUID playerB) {
        this.playerA = playerA;
        this.playerB = playerB;
        this.startTime = System.currentTimeMillis();
        damageBySkill.put(playerA, new EnumMap<>(Skill.class));
        damageBySkill.put(playerB, new EnumMap<>(Skill.class));
    }

    public boolean involves(UUID uuid) {
        return uuid.equals(playerA) || uuid.equals(playerB);
    }

    public UUID getOpponent(UUID uuid) {
        return uuid.equals(playerA) ? playerB : playerA;
    }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - startTime;
    }

    public void recordDamage(UUID attacker, Skill skill, double damage) {
        if (skill == null) return; // khaali haath ya untracked item — count nahi hota
        Map<Skill, Double> map = damageBySkill.get(attacker);
        if (map == null) return;
        map.merge(skill, damage, Double::sum);
    }

    /**
     * Diye gaye player ka is fight mein "clearly dominant" weapon nikalna.
     * Agar koi clear majority (>=60% damage) na ho to null return hota hai
     * (matlab match void hoga — weapon-mixing allowed nahi).
     */
    public Skill getDominantSkill(UUID uuid) {
        Map<Skill, Double> map = damageBySkill.get(uuid);
        if (map == null || map.isEmpty()) return null;

        double total = 0;
        Skill top = null;
        double topValue = -1;

        for (Map.Entry<Skill, Double> entry : map.entrySet()) {
            total += entry.getValue();
            if (entry.getValue() > topValue) {
                topValue = entry.getValue();
                top = entry.getKey();
            }
        }

        if (total <= 0) return null;
        double share = topValue / total;
        return (share >= 0.6) ? top : null;
    }
}
