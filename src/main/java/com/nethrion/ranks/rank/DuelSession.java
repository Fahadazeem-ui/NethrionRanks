package com.nethrion.ranks.rank;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DuelSession {

    private final UUID playerA;
    private final UUID playerB;
    private final long startTime;
    private final Map<UUID, Map<Skill, Double>> damageBySkill = new HashMap<>();
    private final Map<UUID, Long> lastCombatHit = new HashMap<>();
    private static final long MAX_HIT_GAP_MILLIS = 20_000L;

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

    public UUID getPlayerA() { return playerA; }
    public UUID getPlayerB() { return playerB; }

    public long getElapsedMillis() {
        return System.currentTimeMillis() - startTime;
    }

    public void recordDamage(UUID attacker, Skill skill, double damage) {
        if (skill == null || damage <= 0.0) return;
        Map<Skill, Double> map = damageBySkill.get(attacker);
        if (map != null) {
            long now=System.currentTimeMillis(); Long last=lastCombatHit.get(attacker);
            if(last!=null && now-last>MAX_HIT_GAP_MILLIS) map.clear();
            map.merge(skill, damage, Double::sum); lastCombatHit.put(attacker,now);
        }
    }

    public double getTotalValidDamage(UUID uuid) { Map<Skill,Double> m=damageBySkill.get(uuid); if(m==null)return 0.0; long last=lastCombatHit.getOrDefault(uuid,0L); if(System.currentTimeMillis()-last>MAX_HIT_GAP_MILLIS)return 0.0; return m.values().stream().mapToDouble(Double::doubleValue).sum(); }

    public Map<Skill, Double> getDamage(UUID uuid) {
        return damageBySkill.getOrDefault(uuid, new EnumMap<>(Skill.class));
    }

    public Skill getDominantSkill(UUID uuid) {
        Map<Skill, Double> map = damageBySkill.get(uuid);
        if (map == null || map.isEmpty()) return null;

        double total = 0.0;
        Skill top = null;
        double topValue = -1.0;

        for (Map.Entry<Skill, Double> entry : map.entrySet()) {
            total += entry.getValue();
            if (entry.getValue() > topValue) {
                topValue = entry.getValue();
                top = entry.getKey();
            }
        }

        if (total <= 0.0) return null;
        return (topValue / total) >= 0.60 ? top : null;
    }
}
