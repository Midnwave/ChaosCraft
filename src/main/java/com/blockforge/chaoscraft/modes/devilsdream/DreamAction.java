package com.blockforge.chaoscraft.modes.devilsdream;

/**
 * Dream Adaptation tracked player actions.
 * Each action maps to a score that escalates nightmare responses.
 */
public enum DreamAction {
    /** Sprinting — fire trails, movement-punishing attacks */
    MOVEMENT,
    /** Mining/breaking blocks — nearby blocks shatter, ground instability */
    DESTRUCTION,
    /** Placing blocks — structures collapse, anti-building attacks */
    CREATION,
    /** Attacking mobs — mobs get stronger, weapon displays target more */
    AGGRESSION,
    /** Standing still — ground effects intensify, shadow hands */
    STILLNESS,
    /** Sneaking — ambient horror escalates, whispers intensify */
    FEAR,
    /** Jumping — gravity attacks, ground crumbles */
    FLIGHT,
    /** Looking around rapidly — eye structures spawn, phantom flickers */
    PARANOIA
}
