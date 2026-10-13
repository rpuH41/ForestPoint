import { VK_KNOWN_GROUPS } from "./vkGroups";

const VK_API_VERSION = "5.199";
const VK_API_URL = "https://api.vk.com/method/newsfeed.search";
const VK_WALL_URL = "https://api.vk.com/method/wall.get";

export interface VkSocialPost {
  id: string;
  ownerId: number;
  text: string;
  locationText?: string;
  url: string;

  // Unix timestamp в секундах - именно так VK возвращает date.
  date: number;

  query: string;
}

interface VkWallPost {
  id: number;
  owner_id: number;
  from_id?: number;
  date: number;
  text?: string;
  marked_as_ads?: number;
  is_pinned?: number;
}

interface VkWallGetResponse {
  response?: {
    count: number;
    items: VkWallPost[];
  };
  error?: {
    error_code: number;
    error_msg: string;
  };
}

export interface FetchVkKnownGroupPostsOptions {
  accessToken: string;
  sinceUnix: number;
  untilUnix?: number;
  countPerGroup?: number;
}

interface VkNewsfeedSearchResponse {
  count?: number;
  items?: VkWallPost[];
  next_from?: string;
  total_count?: number;
}

interface VkApiError {
  error_code: number;
  error_msg: string;
}

interface VkApiResponse {
  response?: VkNewsfeedSearchResponse;
  error?: VkApiError;
}

async function fetchVkGroupWall(
  accessToken: string,
  ownerId: number,
  count: number,
): Promise<VkWallPost[]> {
  const params = new URLSearchParams({
    owner_id: String(ownerId),
    count: String(count),
    filter: "owner",
    v: VK_API_VERSION,
    access_token: accessToken,
  });

  const response = await fetch(
    `${VK_WALL_URL}?${params.toString()}`,
  );

  const data =
    (await response.json()) as VkWallGetResponse;

  if (!response.ok) {
    throw new Error(
      `VK wall HTTP ${response.status}`,
    );
  }

  if (data.error) {
    throw new Error(
      `VK wall error ${data.error.error_code}: ` +
        data.error.error_msg,
    );
  }

  return data.response?.items ?? [];
}
export async function fetchVkKnownGroupPosts(
  options: FetchVkKnownGroupPostsOptions,
): Promise<VkSocialPost[]> {
  const {
    accessToken,
    sinceUnix,
    untilUnix = Math.floor(Date.now() / 1000),
    countPerGroup = 20,
  } = options;

  const postsById =
    new Map<string, VkSocialPost>();

  const seenContent = new Set<string>();

  const groups = VK_KNOWN_GROUPS.filter(
    (group) =>
      group.fallbackLocation != null &&
      (
        group.scope === "region" ||
        group.scope === "city"
      ),
  );

  /*
   * Запрашиваем несколько VK-групп одновременно.
   *
   * 4 - достаточно быстро, но при этом
   * не запускаем десятки запросов сразу.
   */
  const CONCURRENCY = 4;

  for (
    let start = 0;
    start < groups.length;
    start += CONCURRENCY
  ) {
    const batch = groups.slice(
      start,
      start + CONCURRENCY,
    );

    await Promise.all(
      batch.map(async (group) => {
        try {
          const items =
            await fetchVkGroupWall(
              accessToken,
              group.ownerId,
              countPerGroup,
            );

          let accepted = 0;

          for (const item of items) {
            /*
             * wall.get может вернуть старый
             * закреплённый пост первым.
             * Поэтому обязательно проверяем date.
             */
            if (
              item.date < sinceUnix ||
              item.date > untilUnix
            ) {
              continue;
            }

            if (item.marked_as_ads === 1) {
              continue;
            }

            const text = normalizeText(
              item.text ?? "",
            );

            if (!text) {
              continue;
            }

            if (
              !isLikelyVkMushroomFind(text)
            ) {
              continue;
            }

            const uniqueId =
              `${item.owner_id}_${item.id}`;

            if (
              postsById.has(uniqueId)
            ) {
              continue;
            }

            const contentFingerprint =
              text
                .toLowerCase()
                .replace(/ё/g, "е")
                .replace(/\s+/g, " ")
                .slice(0, 500);

            if (
              seenContent.has(
                contentFingerprint,
              )
            ) {
              continue;
            }

            seenContent.add(
              contentFingerprint,
            );

            postsById.set(
              uniqueId,
              {
                id: uniqueId,
                ownerId: item.owner_id,
                text,
                locationText: text,
                url: buildPostUrl(
                  item.owner_id,
                  item.id,
                ),
                date: item.date,
                query:
                  `wall:${group.screenName}`,
              },
            );

            accepted++;
          }

          console.log(
            `[vk-wall] group=${group.screenName}, ` +
              `ownerId=${group.ownerId}, ` +
              `fetched=${items.length}, ` +
              `accepted=${accepted}`,
          );
        } catch (error) {
          console.warn(
            `[vk-wall] failed group=${group.screenName}, ` +
              `ownerId=${group.ownerId}:`,
            error,
          );
        }
      }),
    );

    /*
     * Небольшая пауза между пачками,
     * а не после каждой отдельной группы.
     */
    if (
      start + CONCURRENCY <
      groups.length
    ) {
      await sleep(350);
    }
  }

  return [...postsById.values()].sort(
    (a, b) => b.date - a.date,
  );
}

export interface FetchVkPostsOptions {
  accessToken: string;

  /**
   * Ищем записи не старше этого Unix timestamp.
   */
  sinceUnix: number;

  /**
   * Верхняя граница времени.
   * Обычно Date.now() / 1000.
   */
  untilUnix?: number;

  /**
   * Сколько записей просить для каждого поискового запроса.
   */
  countPerQuery?: number;

  /**
   * Максимум страниц на одно ключевое слово.
   * Для начала лучше оставить 1.
   */
  maxPagesPerQuery?: number;

  queries?: string[];
}

const DEFAULT_SEARCH_QUERIES = [
  "грибы",
  "опята",
  "лисички",
  "боровики",
  "белые грибы",
  "подберезовики",
  "подосиновики",
  "маслята",
  "грузди",
  "рыжики",
];

function normalizeText(text: string): string {
  return text
    .replace(/\u00a0/g, " ")
    .replace(/\r/g, "")
    .replace(/[ \t]+/g, " ")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

/**
 * Первый грубый фильтр.
 *
 * Важно: он специально не слишком строгий.
 * Географию и окончательную пригодность записи будем проверять позже
 * вместе с остальными соцсетями.
 */
export function isLikelyVkMushroomFind(text: string): boolean {
  const value = text
    .toLowerCase()
    .replace(/ё/g, "е");

    const beginning = value.slice(0, 100);

    const historicalDateMatch = beginning.match(
      /(?:^|[^а-я0-9])(?:\d{1,2}\s+[а-я]+\s+)?((?:19|20)\d{2})(?:\s*г(?:ода)?\.?|[.,])/u,
    );

    if (historicalDateMatch) {
      const mentionedYear = Number(
        historicalDateMatch[1],
      );

      const currentYear =
        new Date().getUTCFullYear();

      if (mentionedYear < currentYear - 1) {
        return false;
      }
    }

  // Должно присутствовать реальное грибное слово/вид.
  const mushroomPattern =
    /(^|[^а-я])(?:гриб(?:ы|ов|ами)?|опята|опят|опенок|опенки|лисички|лисичек|боровик(?:и|ов)?|белые грибы|подберезовик(?:и|ов)?|подосиновик(?:и|ов)?|маслята|маслят|грузди|груздей|рыжики|рыжиков|моховики|моховиков|волнушки|сыроежки)(?![а-я])/u;

  if (!mushroomPattern.test(value)) {
    return false;
  }

  // Сильные признаки реальной находки / грибного отчёта.
  const findPattern =
    /наш(?:ел|ли|ла)|собрал|собрали|собрала|набрал|набрали|набрала|пошли грибы|пошли опята|тихая охота|за грибами|грибной сезон|находк|урожай|корзин|ведр|в лесу|из леса|растут|лезут|поперли|порадовал|попались|сезон продолжается|сезон в разгаре/u;

  if (!findPattern.test(value)) {
    return false;
  }

  // Явно нерелевантные контексты.
  const noisePattern =
    /куплю|продам|продаю|доставка|заказ|заказать|скидк|акци[яи]|ресторан|кафе|меню|рецепт|блин|пицц|суп|занятие|детский сад|детском саду|дошколь|урок|поделк|пластилин|фотовыставк|написан на|статья|справка|научно-фантастическ|пропал|пропавш|не вернулся|лиза\s*алерт|спасател|разыскива|розыск|кузовщик|кузов|ржавчин|коррози/u;

  if (noisePattern.test(value)) {
    return false;
  }

  return true;
}



function buildPostUrl(ownerId: number, postId: number): string {
  return `https://vk.com/wall${ownerId}_${postId}`;
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function searchVkPage(params: {
  accessToken: string;
  query: string;
  sinceUnix: number;
  untilUnix: number;
  count: number;
  startFrom?: string;
}): Promise<VkNewsfeedSearchResponse> {
  const url = new URL(VK_API_URL);

  url.searchParams.set("access_token", params.accessToken);
  url.searchParams.set("v", VK_API_VERSION);
  url.searchParams.set("q", params.query);
  url.searchParams.set("count", String(params.count));
  url.searchParams.set("start_time", String(params.sinceUnix));
  url.searchParams.set("end_time", String(params.untilUnix));

  if (params.startFrom) {
    url.searchParams.set("start_from", params.startFrom);
  }

  const response = await fetch(url);

  if (!response.ok) {
    throw new Error(
      `VK HTTP ${response.status}: ${response.statusText}`
    );
  }

  const data = (await response.json()) as VkApiResponse;

  if (data.error) {
    throw new Error(
      `VK API error ${data.error.error_code}: ${data.error.error_msg}`
    );
  }

  if (!data.response) {
    throw new Error("VK API returned neither response nor error");
  }

  return data.response;
}

export async function fetchVkPosts(
  options: FetchVkPostsOptions,
): Promise<VkSocialPost[]> {
  const {
    accessToken,
    sinceUnix,
    untilUnix = Math.floor(Date.now() / 1000),
    countPerQuery = 50,
    maxPagesPerQuery = 1,
    queries = DEFAULT_SEARCH_QUERIES,
  } = options;

  if (!accessToken.trim()) {
    throw new Error("VK access token is empty");
  }

  const postsById =
    new Map<string, VkSocialPost>();

  const seenContent =
    new Set<string>();

  /*
   * Одновременно обрабатываем
   * несколько поисковых запросов.
   */
  const CONCURRENCY = 4;

  async function processQuery(
    query: string,
  ): Promise<void> {
    let startFrom: string | undefined;

    for (
      let page = 0;
      page < maxPagesPerQuery;
      page++
    ) {
      try {
        const result =
          await searchVkPage({
            accessToken,
            query,
            sinceUnix,
            untilUnix,
            count: countPerQuery,
            startFrom,
          });

        const items =
          result.items ?? [];

        for (const item of items) {
          if (!item.text) {
            continue;
          }

          if (item.marked_as_ads === 1) {
            continue;
          }

          /*
           * На всякий случай проверяем дату сами,
           * даже несмотря на start_time/end_time.
           */
          if (
            item.date < sinceUnix ||
            item.date > untilUnix
          ) {
            continue;
          }

          const text =
            normalizeText(item.text);

          if (!text) {
            continue;
          }

          if (
            !isLikelyVkMushroomFind(text)
          ) {
            continue;
          }

          const uniqueId =
            `${item.owner_id}_${item.id}`;

          if (
            postsById.has(uniqueId)
          ) {
            continue;
          }

          const contentFingerprint =
            text
              .toLowerCase()
              .replace(/ё/g, "е")
              .replace(/\s+/g, " ")
              .slice(0, 500);

          if (
            seenContent.has(
              contentFingerprint,
            )
          ) {
            continue;
          }

          seenContent.add(
            contentFingerprint,
          );

          postsById.set(
            uniqueId,
            {
              id: uniqueId,
              ownerId: item.owner_id,
              text,
              locationText: text,
              url: buildPostUrl(
                item.owner_id,
                item.id,
              ),
              date: item.date,
              query,
            },
          );
        }

        /*
         * Если следующей страницы нет -
         * этот query закончен.
         */
        if (!result.next_from) {
          break;
        }

        startFrom =
          result.next_from;

        /*
         * Пауза нужна только между
         * страницами одного query.
         */
        if (
          page + 1 <
          maxPagesPerQuery
        ) {
          await sleep(350);
        }
      } catch (error) {
        console.error(
          `[VK] search failed for ` +
            `query="${query}", ` +
            `page=${page + 1}`,
          error,
        );

        /*
         * Ошибка одного query
         * не ломает весь прогноз.
         */
        break;
      }
    }
  }

  /*
   * Обрабатываем queries пачками по 4.
   */
  for (
    let start = 0;
    start < queries.length;
    start += CONCURRENCY
  ) {
    const batch =
      queries.slice(
        start,
        start + CONCURRENCY,
      );

    await Promise.all(
      batch.map(
        (query) =>
          processQuery(query),
      ),
    );

    /*
     * Небольшая пауза между пачками.
     */
    if (
      start + CONCURRENCY <
      queries.length
    ) {
      await sleep(350);
    }
  }

  return Array.from(
    postsById.values(),
  ).sort(
    (a, b) => b.date - a.date,
  );
}