import { logger } from "firebase-functions";
import iconv from "iconv-lite";
import { PIKABU_QUERIES } from "./mushrooms";

export interface PikabuSocialPost {
  id: string;
  text: string;
  locationText?: string;
  url: string;
  date: string;
}

const LOOKBACK_DAYS = 3;
const REQUEST_TIMEOUT_MS = 12000;

function decodeEntities(value: string): string {
  const named: Record<string, string> = {
    nbsp: " ", quot: '\"', apos: "'", amp: "&", lt: "<", gt: ">",
    laquo: "«", raquo: "»", mdash: "-", ndash: "–", hellip: "…",
  };
  return value.replace(/&(#(?:x[0-9a-f]+|[0-9]+)|[a-z]+);/gi, (original, entity: string) => {
    if (entity.startsWith("#")) {
      const hex = entity[1]?.toLowerCase() === "x";
      const number = parseInt(entity.slice(hex ? 2 : 1), hex ? 16 : 10);
      return Number.isFinite(number) && number > 0 && number <= 0x10ffff
        ? String.fromCodePoint(number) : original;
    }
    return named[entity.toLowerCase()] ?? original;
  });
}

function plainText(html: string): string {
  return decodeEntities(html
    .replace(/<script\b[^>]*>[\s\S]*?<\/script>/gi, " ")
    .replace(/<style\b[^>]*>[\s\S]*?<\/style>/gi, " ")
    .replace(/<[^>]*>/g, " "))
    .replace(/\s+/g, " ").trim();
}

function cleanPikabuBody(html: string): string {
  let cleaned = html;

  // Удаляем блоки, не относящиеся к рассказу автора.
  const unwantedClasses = [
    "story__tags",
    "story__tag",
    "story__footer",
    "story__rating",
    "story__social",
    "story__comments",
    "story__author",
    "story__header",
    "story__controls",
    "story__actions",
    "story__read-more",
    "story__more",
  ];

  // Удаляем служебные элементы по классам.
  // Работает для простых HTML-блоков.
  for (const className of unwantedClasses) {
    const escaped = className.replace(
      /[.*+?^${}()|[\]\\]/g,
      "\\$&",
    );

    const regex = new RegExp(
      `<([a-z][a-z0-9]*)\\b[^>]*class=["'][^"']*\\b${escaped}\\b[^"']*["'][^>]*>[\\s\\S]*?<\\/\\1>`,
      "gi",
    );

    cleaned = cleaned.replace(regex, " ");
  }

  // Удаляем ссылки на теги вместе с текстом.
  cleaned = cleaned.replace(
    /<a\b[^>]*href=["'][^"']*\/tag\/[^"']*["'][^>]*>[\s\S]*?<\/a>/gi,
    " ",
  );

  // Удаляем кнопки, скрипты и стили.
  cleaned = cleaned.replace(
    /<(script|style|button)\b[^>]*>[\s\S]*?<\/\1>/gi,
    " ",
  );

  return plainText(cleaned)
    .replace(/\bПоказать полностью\b/giu, " ")
    .replace(/\bЕщё\b/giu, " ")
    .replace(/\[\s*моё\s*\]/giu, " ")
    .replace(/\s+/g, " ")
    .trim();
}

function findClassBlock(html: string, classPart: string): string | null {
  // Extract a known content container, not the entire <article> (which includes site chrome).
  const escaped = classPart.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const opening = new RegExp(`<([a-z][a-z0-9]*)\\b[^>]*class=["'][^"']*${escaped}[^"']*["'][^>]*>`, "i");
  const match = opening.exec(html);
  if (!match) return null;
  const tag = match[1].toLowerCase();
  const token = new RegExp(`<\\/?${tag}\\b[^>]*>`, "gi");
  token.lastIndex = match.index;
  let depth = 0;
  let part: RegExpExecArray | null;
  while ((part = token.exec(html))) {
    if (/^<\//.test(part[0])) depth--; else depth++;
    if (depth === 0) return html.slice(match.index + match[0].length, part.index);
  }
  return null;
}

function isRecent(date: string): boolean {
  const timestamp = Date.parse(date);
  const age = Date.now() - timestamp;
  return Number.isFinite(timestamp) && age >= 0 && age <= LOOKBACK_DAYS * 86400000;
}

function relevantMushroomText(text: string): boolean {
  const normalized = text.toLowerCase().replace(/ё/g, "е");
  // Avoid ambiguous single words like "рыжик" (cat) and "лисички" (comics).
  return /\b(mushrooms?|foraging|fungi|porcini|chanterelles?)\b/i.test(normalized) ||
    /(гриб|грибник|тихая охота|опят|подберезовик|подосиновик|маслят|боровик|мухомор|сыроежк|шампиньон|сморчк|грузд|волнушк|дождевик|паутинник|рядовк)/i.test(normalized);
}
function isLikelyPikabuMushroomFind(
  text: string,
): boolean {
  const value = text
    .toLowerCase()
    .replace(/ё/g, "е");

  /*
   * Нужен признак реальной текущей находки /
   * сбора грибов, а не просто статья о грибах.
   */
  const findPattern =
    /наш(?:ел|ла|ли)|собрал(?:а|и)?|набрал(?:а|и)?|насобирал(?:а|и)?|попал(?:ся|ась|ись)|попались|попалось|пошли грибы|пошли опята|пошли маслята|выросли грибы|растут грибы|грибов много|полно грибов|полным-полно|полные корзины|полное ведро|полные ведра|тихая охота удалась|урожай грибов|грибная жатва/u;

  if (!findPattern.test(value)) {
    return false;
  }

  /*
   * Явный нерелевантный контент.
   */
  const noisePattern =
    /рецепт|как приготовить|как мариновать|как солить|магазин|продам|продаю|купить|доставка|скидк|акци[яи]|рисуем|рисовали|лепили|аппликаци|занятие|детский сад|викторин|квиз|энциклопед|справочник/u;

  if (noisePattern.test(value)) {
    return false;
  }

  return true;
}

const ALLOWED_MUSHROOM_TAGS = new Set([
  "грибы",
  "грибники",
  "тихая охота",
  "опята",
  "белые грибы",
  "лисички",
  "маслята",
]);

function hasMushroomTag(article: string): boolean {
  const tagLinks =
    article.match(
      /<a\b[^>]*href=["'][^"']*\/tag\/[^"']*["'][^>]*>[\s\S]*?<\/a>/gi,
    ) ?? [];

  return tagLinks.some((link) => {
    const tagName = plainText(link)
      .toLowerCase()
      .replace(/ё/g, "е")
      .replace(/\s+/g, " ")
      .trim();

    return ALLOWED_MUSHROOM_TAGS.has(tagName);
  });
}

function extractLocationTags(article: string): string[] {
  const tagLinks =
    article.match(
      /<a\b[^>]*href=["'][^"']*\/tag\/[^"']*["'][^>]*>[\s\S]*?<\/a>/gi,
    ) ?? [];

  const locationTags = new Set<string>();

  for (const link of tagLinks) {
    const tag = plainText(link).trim();

    if (
      /(?:область|области|край|крае|район|районе|республика)/iu.test(tag)
    ) {
      locationTags.add(tag);
    }
  }

  return [...locationTags];
}

function parseSearchPage(html: string): PikabuSocialPost[] {
  const posts: PikabuSocialPost[] = [];
  const articles = html.match(/<article\b[^>]*>[\s\S]*?<\/article>/gi) ?? [];
  for (const article of articles) {
      if (!hasMushroomTag(article)) {
        continue;
      }
    const link = article.match(/<a\b[^>]*class=["'][^"']*story__title[^"']*["'][^>]*href=["']([^"']+)["']/i) ??
      article.match(/<a\b[^>]*href=["']([^"']+)["'][^>]*class=["'][^"']*story__title[^"']*["']/i);
    const titleHtml = findClassBlock(article, "story__title");
    const date = article.match(/<time\b[^>]*datetime=["']([^"']+)["']/i)?.[1];
    if (!link || !titleHtml || !date || !isRecent(date)) continue;
    let url: string;
    try {
      const parsed = new URL(link[1], "https://pikabu.ru");
      if (parsed.hostname !== "pikabu.ru" && parsed.hostname !== "www.pikabu.ru") continue;
      url = parsed.origin + parsed.pathname;
    } catch { continue; }
    // Only body text; never use the entire article, header, tags, or sidebar.
    const bodyHtml =
      findClassBlock(article, "story__content") ??
      findClassBlock(article, "story__body") ??
      "";

   const title = plainText(titleHtml);
   const body = cleanPikabuBody(bodyHtml);

   const text = `${title}\n${body}`.trim();

   if (body.length < 15) {
     continue;
   }

   if (!relevantMushroomText(text)) {
     continue;
   }

    if (!isLikelyPikabuMushroomFind(text)) {
      logger.info(
        `[pikabu] not a real find: post=${url}`,
      );
      continue;
    }
   // Географические теги используем только для определения места.
   const locationTags = extractLocationTags(article);

   const locationText = [
     text,
     ...locationTags,
   ].join("\n");

   posts.push({
     id: url,
     url,
     text,
     locationText,
     date,
   });
  }
  return posts;
}

async function fetchSearchPage(
  query: string,
  page: number,
): Promise<PikabuSocialPost[]> {
    logger.warn(
        `[pikabu-debug] request ` +
          `query="${query}", page=${page}`,
      );

  const params = new URLSearchParams({
    q: query,
  });

  if (page > 1) {
    params.set("page", String(page));
  }

  const url =
    `https://pikabu.ru/search?${params.toString()}`;
  try {
    const response = await fetch(url, {
      headers: { "User-Agent": "Mozilla/5.0 (compatible; ForestPoint/1.0)", Accept: "text/html" },
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
    if (!response.ok) {
      logger.warn(`[pikabu] HTTP ${response.status} for query=${query}`);
      return [];
    }
    const bytes = Buffer.from(await response.arrayBuffer());
    const charset = response.headers.get("content-type")?.match(/charset=([^;\s]+)/i)?.[1]?.toLowerCase();
    const html = charset === "windows-1251" || charset === "cp1251"
      ? iconv.decode(bytes, "win1251") : bytes.toString("utf8");


      logger.warn(
        `[pikabu-debug] response ` +
          `query="${query}", ` +
          `page=${page}, ` +
          `status=${response.status}, ` +
          `htmlLength=${html.length}`,
      );
    const posts = parseSearchPage(html);

    logger.info(
      `[pikabu] query="${query}", ` +
        `page=${page}, ` +
        `accepted=${posts.length}`,
    );

    return posts;
  } catch (error) {
    logger.warn(`[pikabu] request failed query=${query}: ${String(error)}`);
    return [];
  }
}

export async function collectPikabuPosts():
Promise<PikabuSocialPost[]> {
     logger.warn(
        `[pikabu-debug] collector started, ` +
          `queries=${PIKABU_QUERIES.length}`,
      );
  const unique =
    new Map<string, PikabuSocialPost>();

  const pages = [1, 2, 3];

  const requests: Array<
    Promise<PikabuSocialPost[]>
  > = [];

  for (const query of PIKABU_QUERIES) {
    for (const page of pages) {
      requests.push(
        fetchSearchPage(query, page),
      );
    }
  }

  const results =
    await Promise.all(requests);

  for (const posts of results) {
    for (const post of posts) {
      unique.set(post.id, post);
    }
  }

  logger.info(
    `[pikabu] tagFiltered=${unique.size}, ` +
      `queries=${PIKABU_QUERIES.length}, ` +
      `pages=${pages.length}`,
  );

  return [...unique.values()];
}
