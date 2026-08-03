package com.personalblog.service;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Markdown 渲染: commonmark 转 HTML -> jsoup 白名单净化(防 XSS)
 */
@Service
public class MarkdownService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final Safelist safelist;

    public MarkdownService() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder().extensions(extensions).build();
        this.safelist = Safelist.relaxed()
                .addTags("del")
                .addAttributes("a", "href", "title")
                .addAttributes("img", "src", "alt", "title")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https", "data");
    }

    /** 将 Markdown 原文渲染为安全的 HTML(不可信输入) */
    public String renderToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node node = parser.parse(markdown);
        String html = renderer.render(node);
        // commonmark 默认原样透传 raw HTML, 必须经过 jsoup 白名单净化
        return Jsoup.clean(html, safelist);
    }
}
