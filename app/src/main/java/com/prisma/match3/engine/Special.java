package com.prisma.match3.engine;

/**
 * Special powers a gem can carry, produced by matching more than three gems or
 * by matching in an L / T shape. Activating a special triggers a chain of
 * board clears that can detonate further specials (combos).
 */
public enum Special {
    NONE,
    STRIPE_H,   // clears the whole row  (match of 4 in a row)
    STRIPE_V,   // clears the whole column (match of 4 in a column)
    BOMB,       // clears a 5x5 blast (L / T shaped match)
    NOVA        // rainbow: clears every gem of one chosen color (match of 5 in a line)
}
