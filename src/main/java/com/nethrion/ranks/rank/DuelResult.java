package com.nethrion.ranks.rank;

import java.util.List;
import java.util.UUID;

/**
 * Duel resolve hone ke baad ka poora result — Phase 2 (duel engine) isko
 * use karega taake players ko sahi messages/titles bhej sake.
 */
public class DuelResult {

    public enum Type {
        PROMOTION,  // same-tier fight — winner promote hua, loser Civillian bana
        FULL_SWAP   // alag-tier fight — dono ki ranks aapas mein swap hui
    }

    private final Type type;
    private final UUID winner;
    private final UUID loser;
    private final RankTier winnerOldTier;
    private final RankTier winnerNewTier;
    private final RankTier loserOldTier;
    private final RankTier loserNewTier;
    private final List<UUID> bumpedPlayers; // jinki jagah khali karayi gayi (kam kills ki wajah se)

    public DuelResult(Type type, UUID winner, UUID loser,
                       RankTier winnerOldTier, RankTier winnerNewTier,
                       RankTier loserOldTier, RankTier loserNewTier,
                       List<UUID> bumpedPlayers) {
        this.type = type;
        this.winner = winner;
        this.loser = loser;
        this.winnerOldTier = winnerOldTier;
        this.winnerNewTier = winnerNewTier;
        this.loserOldTier = loserOldTier;
        this.loserNewTier = loserNewTier;
        this.bumpedPlayers = bumpedPlayers;
    }

    public Type getType() {
        return type;
    }

    public UUID getWinner() {
        return winner;
    }

    public UUID getLoser() {
        return loser;
    }

    public RankTier getWinnerOldTier() {
        return winnerOldTier;
    }

    public RankTier getWinnerNewTier() {
        return winnerNewTier;
    }

    public RankTier getLoserOldTier() {
        return loserOldTier;
    }

    public RankTier getLoserNewTier() {
        return loserNewTier;
    }

    public List<UUID> getBumpedPlayers() {
        return bumpedPlayers;
    }
}
