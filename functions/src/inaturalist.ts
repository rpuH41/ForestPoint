import { logger } from "firebase-functions";

export interface INaturalistObservation {
  id: string;
  speciesId: string;
  observedAt: string;
  url: string;
  latitude: number;
  longitude: number;
  qualityGrade: "research" | "needs_id" | "casual";
  observerId?: number;
}

type ObservationJson = {
  id?: number;
  observed_on?: string | null;
  time_observed_at?: string | null;
  created_at?: string | null;
  uri?: string;
  quality_grade?: string;
  obscured?: boolean;
  taxon?: { id?: number; name?: string; ancestor_ids?: number[] } | null;
  geojson?: { coordinates?: number[] } | null;
  user?: { id?: number } | null;
};

const OBSERVATIONS_API = "https://api.inaturalist.org/v1/observations";
const RADIUS_KM = 30;
const LOOKBACK_DAYS = 3;
const PAGE_SIZE = 200;
const MAX_PAGES = 5;

// Таксоны iNaturalist: точные виды или узкие роды/группы.
// Широкие категории (например, Boletaceae) намеренно не сопоставляются.
const TAXON_MAP: Record<string, string> = {
  "Boletus edulis": "white_mushroom",
  "Leccinum scabrum": "birch_bolete",
  "Leccinum versipelle": "aspen_bolete",
  "Cantharellus cibarius": "chanterelle",
  "Armillaria mellea": "honey_mushroom",
  "Suillus luteus": "boletus",
  "Lactarius deliciosus": "saffron_milk_cap",
  "Russula": "russula",
  "Lactarius resimus": "milk_cap",
  "Lactarius torminosus": "woolly_milk_cap",
  "Agaricus": "champignon",
  "Morchella": "morel",
  "Lycoperdon": "puffball",
  "Imleria badia": "polish_mushroom",
  "Pleurotus ostreatus": "oyster_mushroom",
  "Macrolepiota procera": "parasol_mushroom",
  "Lactarius necator": "black_milk_cap",
  "Tricholoma": "tricholoma",
  "Hydnum repandum": "hedgehog_mushroom",
  "Tuber": "truffle",
  "Lactarius flexuosus": "milkcap_serushka",
  "Amanita muscaria": "fly_agaric",
  "Gyromitra": "false_morel",
  "Cortinarius": "webcap",
  "Lactifluus vellereus": "fiddlehead",
};

function validCoordinates(lat: number, lon: number): boolean {
  return Number.isFinite(lat) && Number.isFinite(lon) &&
    lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
}

function distanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const toRad = Math.PI / 180;
  const a = Math.sin((lat2 - lat1) * toRad / 2) ** 2 +
    Math.cos(lat1 * toRad) * Math.cos(lat2 * toRad) *
    Math.sin((lon2 - lon1) * toRad / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

function speciesForTaxon(name: string): string | undefined {
  const exact = TAXON_MAP[name];
  if (exact) return exact;
  // Род учитывается только если он явно указан в словаре.
  const genus = name.split(" ")[0];
  return TAXON_MAP[genus];
}

export async function collectINaturalistObservations(
  latitude: number,
  longitude: number,
): Promise<INaturalistObservation[]> {
  if (!validCoordinates(latitude, longitude)) return [];

  const now = Date.now();
  const cutoff = now - LOOKBACK_DAYS * 86400000;
  const observedAfter = new Date(cutoff).toISOString().slice(0, 10);
  const seen = new Set<number>();
  const results: INaturalistObservation[] = [];

  for (let page = 1; page <= MAX_PAGES; page++) {
    const params = new URLSearchParams({
      taxon_id: "47170", // Fungi
      lat: String(latitude),
      lng: String(longitude),
      radius: String(RADIUS_KM),
      d1: observedAfter,
      order_by: "observed_on",
      order: "desc",
      per_page: String(PAGE_SIZE),
      page: String(page),
    });

    try {
      const response = await fetch(`${OBSERVATIONS_API}?${params}`, {
        headers: { Accept: "application/json", "User-Agent": "ForestPoint/1.0 (mushroom forecast)" },
        signal: AbortSignal.timeout(12000),
      });
      if (!response.ok) {
        logger.warn(`[inaturalist] HTTP ${response.status}, page=${page}`);
        break;
      }
      const payload = await response.json() as { results?: ObservationJson[]; total_results?: number };
      const items = payload.results ?? [];
      for (const item of items) {
        if (typeof item.id !== "number" || seen.has(item.id)) continue;
        seen.add(item.id);
        if (item.obscured) continue; // Координаты неточные: не считаем попаданием в 300 км.
        const coords = item.geojson?.coordinates;
        if (!coords || coords.length < 2) continue;
        const [lon, lat] = coords;
        if (!validCoordinates(lat, lon) || distanceKm(latitude, longitude, lat, lon) > RADIUS_KM) continue;
        const date = item.time_observed_at ?? item.observed_on;
        if (!date) continue;
        // observed_on - календарная дата без времени; допускаем её как день наблюдения.
        const timestamp = Date.parse(date);
        if (!Number.isFinite(timestamp) || timestamp > now + 86400000 || timestamp < cutoff - 86400000) continue;
        const speciesId = speciesForTaxon(item.taxon?.name ?? "");
        if (!speciesId) continue;
        const grade = item.quality_grade;
        if (grade !== "research" && grade !== "needs_id") continue;
        results.push({
          id: `inaturalist:${item.id}`,
          speciesId,
          observedAt: date,
          url: item.uri ?? `https://www.inaturalist.org/observations/${item.id}`,
          latitude: lat,
          longitude: lon,
          qualityGrade: grade,
          observerId: item.user?.id,
        });
      }
      if (items.length < PAGE_SIZE || page * PAGE_SIZE >= (payload.total_results ?? 0)) break;
    } catch (error) {
      logger.warn(`[inaturalist] request failed: ${String(error)}`);
      break;
    }
  }

  // Ограничиваем влияние одного наблюдателя: одна запись на вид и календарный день.
  const deduped = new Map<string, INaturalistObservation>();
  for (const obs of results) {
    const key = `${obs.observerId ?? obs.id}:${obs.speciesId}:${obs.observedAt.slice(0, 10)}`;
    const previous = deduped.get(key);
    if (!previous || (obs.qualityGrade === "research" && previous.qualityGrade !== "research")) {
      deduped.set(key, obs);
    }
  }
  logger.info(`[inaturalist] accepted=${deduped.size}, matchedBeforeDedup=${results.length}`);
  return [...deduped.values()];
}
