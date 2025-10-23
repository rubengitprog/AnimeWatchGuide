package com.example.watchguide.utils;

public class TextValidator {

    /**
     * Valida que el texto no esté vacío y no contenga solo caracteres invisibles.
     *
     * Detecta:
     * - Strings vacíos
     * - Espacios en blanco
     * - Tabs
     * - Saltos de línea
     * - Caracteres Unicode invisibles (Zero-Width Space, etc.)
     *
     * @param text El texto a validar
     * @return true si el texto es válido (contiene al menos un carácter visible), false en caso contrario
     */
    public static boolean isValidText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        // Eliminar todos los espacios en blanco (incluyendo tabs, newlines, etc.)
        String trimmed = text.trim();

        // Si después de trim está vacío, no es válido
        if (trimmed.isEmpty()) {
            return false;
        }

        // Verificar si contiene al menos un carácter visible
        // Removemos caracteres de control Unicode y espacios de ancho cero
        String cleanedText = trimmed
                .replaceAll("\\p{C}", "") // Elimina caracteres de control Unicode
                .replaceAll("\\u200B", "") // Zero Width Space
                .replaceAll("\\u200C", "") // Zero Width Non-Joiner
                .replaceAll("\\u200D", "") // Zero Width Joiner
                .replaceAll("\\u200E", "") // Left-to-Right Mark
                .replaceAll("\\u200F", "") // Right-to-Left Mark
                .replaceAll("\\uFEFF", "") // Zero Width No-Break Space (BOM)
                .replaceAll("\\u00A0", "") // Non-Breaking Space
                .replaceAll("\\u2060", "") // Word Joiner
                .replaceAll("\\u180E", "") // Mongolian Vowel Separator
                .trim();

        // Si después de limpiar caracteres invisibles está vacío, no es válido
        return !cleanedText.isEmpty();
    }

    /**
     * Valida y limpia el texto, eliminando caracteres invisibles al inicio y final.
     *
     * @param text El texto a limpiar
     * @return El texto limpio, o null si el texto no es válido
     */
    public static String cleanText(String text) {
        if (!isValidText(text)) {
            return null;
        }

        // Retornar el texto con trim básico (los caracteres invisibles en medio del texto se mantienen)
        return text.trim();
    }
}
