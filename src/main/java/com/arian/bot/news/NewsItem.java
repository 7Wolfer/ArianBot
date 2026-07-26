package com.arian.bot.news;

/**
 * Una noticia/paper para el digest. {@code id} es único y estable, se usa para no repetir noticias.
 * {@code sourceText} es el abstract/resumen real (si lo hay) que se usa para generar el resumen en
 * español sin inventar datos; puede ser null si la fuente no lo provee (ej. Hacker News).
 */
public record NewsItem(String id, String title, String authors, String source, String url, String category, String published, String sourceText) {
}
