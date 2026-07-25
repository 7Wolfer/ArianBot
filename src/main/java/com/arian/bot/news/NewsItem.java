package com.arian.bot.news;

/** Una noticia/paper para el digest. {@code id} es único y estable, se usa para no repetir noticias. */
public record NewsItem(String id, String title, String authors, String source, String url, String category, String published) {
}
