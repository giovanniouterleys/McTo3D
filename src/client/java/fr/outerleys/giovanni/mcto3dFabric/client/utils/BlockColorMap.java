/*
 * McTo3D
 * Copyright (c) 2026 Giovanni Outerleys
 */

package fr.outerleys.giovanni.mcto3dFabric.client.utils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * V2 du système de couleurs.
 * Introduit des palettes curatées, des catégories de blocs et des stratégies de correspondance multiples.
 */
public class BlockColorMap {

    // ================= CONFIGURATION =================
    // Change cette valeur pour tester différentes approches !
    // Choix possibles : MatchingStrategy.BALANCED_RGB ou MatchingStrategy.VIBRANT_HSB
    public static MatchingStrategy currentStrategy = MatchingStrategy.VIBRANT_HSB;
    // =================================================

    private static final List<BlockColorEntry> PALETTE = new ArrayList<>();

    /**
     * Catégories pour définir la "propreté" d'un bloc.
     * L'algorithme préférera les catégories avec une pénalité faible.
     */
    public enum BlockCategory {
        SMOOTH(0),    // Bétons, Laines (Priorité absolue)
        TERRA(10),     // Terracottas (Unies mais ternes)
        STONE(30),    // Pierres taillées, briques (Texture moyenne)
        NOISY(60),    // Cobble, Diorite, Terre (Texture forte, dernier recours)
        SPECIAL(100); // Blocs très spécifiques

        final double penalty;
        BlockCategory(double penalty) { this.penalty = penalty; }
    }

    /**
     * Structure de données pour stocker un bloc et ses infos de couleur pré-calculées
     */
    private record BlockColorEntry(BlockState state, Color color, float[] hsb, BlockCategory category) {
        BlockColorEntry(Block block, int r, int g, int b, BlockCategory category) {
            this(block.getDefaultState(), new Color(r, g, b), Color.RGBtoHSB(r, g, b, null), category);
        }
    }

    static {
        // --- PALETTE CURATÉE V2 ---
        // On privilégie les blocs unis et esthétiques.

        // 1. LES INDISPENSABLES (Monochrome Lisse) - Priorité Maximale
        register(Blocks.WHITE_CONCRETE, 207, 213, 214, BlockCategory.SMOOTH);
        register(Blocks.LIGHT_GRAY_CONCRETE, 125, 125, 115, BlockCategory.SMOOTH);
        register(Blocks.GRAY_CONCRETE, 54, 57, 61, BlockCategory.SMOOTH);
        register(Blocks.BLACK_CONCRETE, 8, 10, 15, BlockCategory.SMOOTH);
        register(Blocks.WHITE_WOOL, 233, 236, 236, BlockCategory.SMOOTH);
        register(Blocks.LIGHT_GRAY_WOOL, 142, 142, 134, BlockCategory.SMOOTH);
        register(Blocks.GRAY_WOOL, 62, 68, 71, BlockCategory.SMOOTH);
        register(Blocks.BLACK_WOOL, 20, 21, 25, BlockCategory.SMOOTH);

        // 2. LES COULEURS VIVES (Lisse)
        register(Blocks.RED_CONCRETE, 142, 32, 32, BlockCategory.SMOOTH);
        register(Blocks.RED_WOOL, 160, 39, 34, BlockCategory.SMOOTH);
        register(Blocks.ORANGE_CONCRETE, 224, 97, 0, BlockCategory.SMOOTH);
        register(Blocks.ORANGE_WOOL, 240, 118, 19, BlockCategory.SMOOTH);
        register(Blocks.YELLOW_CONCRETE, 240, 175, 21, BlockCategory.SMOOTH);
        register(Blocks.YELLOW_WOOL, 248, 197, 39, BlockCategory.SMOOTH);
        register(Blocks.LIME_CONCRETE, 94, 169, 24, BlockCategory.SMOOTH);
        register(Blocks.LIME_WOOL, 112, 185, 25, BlockCategory.SMOOTH);
        register(Blocks.GREEN_CONCRETE, 73, 91, 36, BlockCategory.SMOOTH);
        register(Blocks.GREEN_WOOL, 84, 109, 27, BlockCategory.SMOOTH);
        register(Blocks.CYAN_CONCRETE, 21, 119, 136, BlockCategory.SMOOTH);
        register(Blocks.CYAN_WOOL, 21, 137, 145, BlockCategory.SMOOTH);
        register(Blocks.LIGHT_BLUE_CONCRETE, 35, 137, 198, BlockCategory.SMOOTH);
        register(Blocks.LIGHT_BLUE_WOOL, 58, 175, 217, BlockCategory.SMOOTH);
        register(Blocks.BLUE_CONCRETE, 44, 46, 143, BlockCategory.SMOOTH);
        register(Blocks.BLUE_WOOL, 53, 57, 157, BlockCategory.SMOOTH);
        register(Blocks.PURPLE_CONCRETE, 100, 31, 156, BlockCategory.SMOOTH);
        register(Blocks.PURPLE_WOOL, 121, 42, 172, BlockCategory.SMOOTH);
        register(Blocks.MAGENTA_CONCRETE, 169, 48, 159, BlockCategory.SMOOTH);
        register(Blocks.MAGENTA_WOOL, 189, 68, 179, BlockCategory.SMOOTH);
        register(Blocks.PINK_CONCRETE, 213, 101, 142, BlockCategory.SMOOTH);
        register(Blocks.PINK_WOOL, 237, 141, 172, BlockCategory.SMOOTH);
        register(Blocks.BROWN_CONCRETE, 96, 59, 31, BlockCategory.SMOOTH);
        register(Blocks.BROWN_WOOL, 114, 71, 40, BlockCategory.SMOOTH);

        // 3. TERRACOTTAS (Ternes mais utiles pour les teintes intermédiaires)
        // Note : Cyan Terracotta est excellente pour le gris métal.
        register(Blocks.TERRACOTTA, 152, 94, 67, BlockCategory.TERRA);
        register(Blocks.WHITE_TERRACOTTA, 209, 177, 161, BlockCategory.TERRA);
        register(Blocks.LIGHT_GRAY_TERRACOTTA, 135, 107, 98, BlockCategory.TERRA);
        register(Blocks.GRAY_TERRACOTTA, 57, 41, 35, BlockCategory.TERRA); // Très utile pour le gris foncé chaud
        register(Blocks.BLACK_TERRACOTTA, 37, 22, 16, BlockCategory.TERRA);
        register(Blocks.BROWN_TERRACOTTA, 76, 50, 35, BlockCategory.TERRA);
        register(Blocks.RED_TERRACOTTA, 143, 61, 46, BlockCategory.TERRA);
        register(Blocks.ORANGE_TERRACOTTA, 161, 83, 37, BlockCategory.TERRA);
        register(Blocks.YELLOW_TERRACOTTA, 186, 133, 35, BlockCategory.TERRA);
        register(Blocks.LIME_TERRACOTTA, 103, 117, 53, BlockCategory.TERRA);
        register(Blocks.GREEN_TERRACOTTA, 76, 83, 42, BlockCategory.TERRA);
        register(Blocks.CYAN_TERRACOTTA, 87, 92, 92, BlockCategory.TERRA); // LE bloc métal par excellence
        register(Blocks.LIGHT_BLUE_TERRACOTTA, 113, 108, 137, BlockCategory.TERRA);
        register(Blocks.BLUE_TERRACOTTA, 74, 59, 91, BlockCategory.TERRA);
        register(Blocks.PURPLE_TERRACOTTA, 118, 69, 86, BlockCategory.TERRA);
        register(Blocks.MAGENTA_TERRACOTTA, 149, 88, 108, BlockCategory.TERRA);
        register(Blocks.PINK_TERRACOTTA, 161, 77, 78, BlockCategory.TERRA);

        // 4. PIERRES PROPRES (Stone)
        register(Blocks.SMOOTH_STONE, 158, 158, 158, BlockCategory.STONE);
        register(Blocks.STONE, 125, 125, 125, BlockCategory.STONE);
        register(Blocks.STONE_BRICKS, 120, 120, 120, BlockCategory.STONE);
        register(Blocks.DEEPSLATE_BRICKS, 65, 65, 68, BlockCategory.STONE);
        register(Blocks.POLISHED_DEEPSLATE, 70, 70, 73, BlockCategory.STONE);
        register(Blocks.POLISHED_BLACKSTONE, 50, 45, 50, BlockCategory.STONE);
        register(Blocks.POLISHED_ANDESITE, 135, 136, 136, BlockCategory.STONE);
        register(Blocks.POLISHED_DIORITE, 200, 200, 200, BlockCategory.STONE);
        register(Blocks.POLISHED_GRANITE, 154, 106, 89, BlockCategory.STONE);
        register(Blocks.QUARTZ_BLOCK, 235, 230, 225, BlockCategory.STONE);
        register(Blocks.SMOOTH_SANDSTONE, 216, 203, 155, BlockCategory.STONE);
        register(Blocks.SMOOTH_RED_SANDSTONE, 181, 97, 31, BlockCategory.STONE);
        register(Blocks.NETHER_BRICKS, 44, 21, 26, BlockCategory.STONE);

        // 5. BLOCS "NOISY" (Dernier recours, pénalité élevée)
        // On évite les minerais, on ne garde que les textures utiles
        register(Blocks.COBBLESTONE, 100, 100, 100, BlockCategory.NOISY);
        register(Blocks.ANDESITE, 130, 131, 131, BlockCategory.NOISY);
        register(Blocks.DIORITE, 220, 220, 220, BlockCategory.NOISY);
        register(Blocks.GRANITE, 149, 103, 85, BlockCategory.NOISY);
        register(Blocks.BRICKS, 150, 97, 83, BlockCategory.NOISY);
        register(Blocks.DIRT, 134, 96, 67, BlockCategory.NOISY);
        register(Blocks.OAK_PLANKS, 162, 130, 78, BlockCategory.NOISY);
        register(Blocks.SPRUCE_PLANKS, 114, 84, 56, BlockCategory.NOISY);
        register(Blocks.DARK_OAK_PLANKS, 66, 43, 20, BlockCategory.NOISY);

        // 6. SPÉCIAUX (Couleurs uniques)
        register(Blocks.GOLD_BLOCK, 246, 208, 61, BlockCategory.SPECIAL);
        register(Blocks.IRON_BLOCK, 220, 220, 220, BlockCategory.SPECIAL); // Très brillant
    }

    private static void register(Block block, int r, int g, int b, BlockCategory category) {
        PALETTE.add(new BlockColorEntry(block, r, g, b, category));
    }

    /**
     * Point d'entrée principal. Utilise la stratégie définie en haut du fichier.
     */
    public static BlockState getClosestBlock(int r, int g, int b) {
        return currentStrategy.findBestBlock(r, g, b);
    }


    // ================= STRATÉGIES DE CORRESPONDANCE =================

    public enum MatchingStrategy {
        /**
         * Stratégie 1 : RGB Équilibré avec Pénalités de Texture.
         * Calcule la distance RGB classique, mais ajoute une pénalité si le bloc n'est pas lisse.
         * Bon pour un usage général.
         */
        BALANCED_RGB {
            @Override
            BlockState findBestBlock(int r, int g, int b) {
                BlockState bestBlock = Blocks.WHITE_CONCRETE.getDefaultState();
                double minDistance = Double.MAX_VALUE;

                for (BlockColorEntry entry : PALETTE) {
                    double dR = entry.color.getRed() - r;
                    double dG = entry.color.getGreen() - g;
                    double dB = entry.color.getBlue() - b;

                    // Distance RGB pondérée (l'œil est plus sensible au vert)
                    // + Pénalité de catégorie
                    double distance = (dR * dR * 0.30) + (dG * dG * 0.59) + (dB * dB * 0.11);
                    distance += entry.category.penalty * 50.0; // Pénalité forte pour les blocs bruyants

                    if (distance < minDistance) {
                        minDistance = distance;
                        bestBlock = entry.state;
                    }
                }
                return bestBlock;
            }
        },

        /**
         * Stratégie 2 : HSB Vibrant (Recommandé pour ton Mecha).
         * Priorise la correspondance de la Teinte (Hue) et de la Saturation.
         * Ignore en partie la luminosité pour trouver la couleur la plus "proche" visuellement.
         * Favorise énormément les blocs lisses (Concrete/Wool).
         */
        VIBRANT_HSB {
            @Override
            BlockState findBestBlock(int r, int g, int b) {
                BlockState bestBlock = Blocks.WHITE_CONCRETE.getDefaultState();
                double minDistance = Double.MAX_VALUE;

                float[] inputHsb = Color.RGBtoHSB(r, g, b, null);
                float inH = inputHsb[0]; // Teinte (0.0 - 1.0)
                float inS = inputHsb[1]; // Saturation (0.0 - 1.0)
                float inB = inputHsb[2]; // Luminosité (0.0 - 1.0)

                boolean isGrayscaleInput = inS < 0.1f; // L'entrée est-elle globalement grise ?

                for (BlockColorEntry entry : PALETTE) {
                    float bH = entry.hsb[0];
                    float bS = entry.hsb[1];
                    float bB = entry.hsb[2];

                    // 1. Différence de Teinte (Hue) - C'est un cercle, donc on prend le chemin le plus court
                    float dH = Math.abs(inH - bH);
                    if (dH > 0.5f) dH = 1.0f - dH;

                    // 2. Différence de Saturation et Luminosité
                    float dS = Math.abs(inS - bS);
                    float dB = Math.abs(inB - bB);

                    double distance;

                    if (isGrayscaleInput) {
                        // Si l'entrée est grise, seule la luminosité compte vraiment.
                        // On pénalise fortement les blocs saturés.
                        distance = dB * 2.0 + bS * 5.0;
                    } else {
                        // Si l'entrée est colorée, la teinte est primordiale.
                        // Poids : Teinte x4, Saturation x2, Luminosité x1
                        distance = (dH * 4.0) + (dS * 2.0) + (dB * 1.0);
                    }

                    // Pénalité de catégorie (Concrete/Wool sont avantagés)
                    distance += entry.category.penalty * 0.1;

                    if (distance < minDistance) {
                        minDistance = distance;
                        bestBlock = entry.state;
                    }
                }
                return bestBlock;
            }
        };

        abstract BlockState findBestBlock(int r, int g, int b);
    }
}