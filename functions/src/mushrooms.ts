export interface MushroomSpecies {
  id: string;

  ru: string;
  en: string;

  aliases: string[];

  translit: string[];
}

export const MUSHROOM_SPECIES: MushroomSpecies[] = [
      {
id: "white_mushroom",
ru: "белый гриб",
en: "porcini",
aliases: [
  "белые грибы",
  "белого гриба",
  "белых грибов",
  "белыми грибами",
          "белому грибу",

          "боровик",
          "боровики",
          "боровика",
          "боровиков",
          "боровиками",
          "боровичок",
          "боровички",
          "боровичка",
          "боровичков",

          "cep",
          "ceps",
          "porcini mushroom",
          "porcini mushrooms",
        ],
        translit: [
          "belyy grib",
          "belye griby",
          "borovik",
          "boroviki",
          "porcini",
        ],
      },

      {
        id: "birch_bolete",
        ru: "подберёзовик",
        en: "birch bolete",
        aliases: [
          "подберезовик",

          "подберёзовики",
          "подберезовики",

          "подберёзовика",
          "подберезовика",

          "подберёзовиков",
          "подберезовиков",

          "подберёзовиками",
          "подберезовиками",

          "birch bolete",
          "birch boletes",
        ],
        translit: [
          "podberezovik",
          "podberezoviki",
        ],
      },

      {
        id: "aspen_bolete",
        ru: "подосиновик",
        en: "aspen bolete",
        aliases: [
          "подосиновики",
          "подосиновика",
          "подосиновиков",
          "подосиновиками",

          "красноголовик",
          "красноголовики",
          "красноголовика",
          "красноголовиков",
          "красноголовиками",

          "red-capped scaber stalk",
          "red capped scaber stalk",
          "aspen bolete",
          "aspen boletes",
        ],
        translit: [
          "podosinovik",
          "podosinoviki",
          "krasnogolovik",
          "krasnogoloviki",
        ],
      },

      {
        id: "chanterelle",
        ru: "лисичка",
        en: "chanterelle",
        aliases: [
          "лисички",
          "лисичек",
          "лисичками",
          "лисичку",
          "лисичкой",

          "chanterelle",
          "chanterelles",
        ],
        translit: [
          "lisichka",
          "lisichki",
        ],
      },

      {
        id: "honey_mushroom",
        ru: "опёнок",
        en: "honey mushroom",
        aliases: [
          "опенок",

          "опята",
          "опят",
          "опятами",

          "опёнки",
          "опенки",

          "осенний опёнок",
          "осенний опенок",
          "осенние опята",
          "опята осенние",
          "осенних опят",

          "honey mushroom",
          "honey mushrooms",
          "autumn honey mushroom",
          "autumn honey mushrooms",
        ],
        translit: [
          "openok",
          "opyata",
          "osennie opyata",
        ],
      },

      {
        id: "boletus",
        ru: "маслёнок",
        en: "slippery jack",
        aliases: [
          "масленок",

          "маслёнки",
          "масленки",

          "маслята",
          "маслят",
          "маслятами",

          "slippery jack",
          "slippery jacks",
        ],
        translit: [
          "maslenok",
          "maslyata",
        ],
      },

      {
        id: "saffron_milk_cap",
        ru: "рыжик",
        en: "saffron milk cap",
        aliases: [
          "рыжики",
          "рыжика",
          "рыжиков",
          "рыжиками",

          "saffron milk cap",
          "saffron milk caps",
        ],
        translit: [
          "ryzhik",
          "ryzhiki",
        ],
      },

      {
        id: "russula",
        ru: "сыроежка",
        en: "russula",
        aliases: [
          "сыроежки",
          "сыроежек",
          "сыроежками",

          "сыроежка зелёная",
          "сыроежка зеленая",

          "зелёная сыроежка",
          "зеленая сыроежка",

          "зелёные сыроежки",
          "зеленые сыроежки",

          "зелёных сыроежек",
          "зеленых сыроежек",

          "russula",
          "russulas",
          "green russula",
          "green russulas",
        ],
        translit: [
          "syroezhka",
          "syroezhki",
          "zelenaya syroezhka",
        ],
      },

      {
        id: "milk_cap",
        ru: "груздь",
        en: "milk cap",
        aliases: [
          "грузди",
          "груздя",
          "груздей",
          "груздями",

          "груздь настоящий",
          "грузди настоящие",
          "настоящий груздь",
          "настоящие грузди",
          "настоящих груздей",

          "milk mushroom",
          "milk mushrooms",
          "milk cap",
          "milk caps",
        ],
        translit: [
          "gruzd",
          "gruzdi",
          "nastoyashchiy gruzd",
        ],
      },

      {
        id: "woolly_milk_cap",
        ru: "волнушка",
        en: "woolly milk cap",
        aliases: [
          "волнушки",
          "волнушек",
          "волнушками",

          "волнушка розовая",
          "розовая волнушка",
          "розовые волнушки",
          "розовых волнушек",

          "woolly milk cap",
          "woolly milk caps",
          "pink waxy cap",
        ],
        translit: [
          "volnushka",
          "volnushki",
          "rozovaya volnushka",
        ],
      },

      {
        id: "champignon",
        ru: "шампиньон",
        en: "champignon",
        aliases: [
          "шампиньоны",
          "шампиньона",
          "шампиньонов",
          "шампиньонами",

          "шампиньон лесной",
          "лесной шампиньон",
          "лесные шампиньоны",
          "лесных шампиньонов",

          "champignon",
          "champignons",
          "wood mushroom",
          "wood mushrooms",
        ],
        translit: [
          "shampinon",
          "shampinony",
          "lesnoy shampinon",
        ],
      },

      {
        id: "morel",
        ru: "сморчок",
        en: "morel",
        aliases: [
          "сморчки",
          "сморчка",
          "сморчков",
          "сморчками",

          "morel",
          "morels",
        ],
        translit: [
          "smorchok",
          "smorchki",
        ],
      },

      {
        id: "puffball",
        ru: "дождевик",
        en: "puffball",
        aliases: [
          "дождевики",
          "дождевика",
          "дождевиков",
          "дождевиками",

          "puffball",
          "puffballs",
        ],
        translit: [
          "dozhdevik",
          "dozhdeviki",
        ],
      },

      {
        id: "polish_mushroom",
        ru: "польский гриб",
        en: "bay bolete",
        aliases: [
          "польские грибы",
          "польского гриба",
          "польских грибов",

          "польский белый",
          "польские белые",
          "польских белых",

          "bay bolete",
          "bay boletes",
        ],
        translit: [
          "polskiy grib",
          "polskie griby",
          "polskie belye",
        ],
      },

      {
        id: "oyster_mushroom",
        ru: "вешенка",
        en: "oyster mushroom",
        aliases: [
          "вешенки",
          "вешенок",
          "вешенками",

          "oyster mushroom",
          "oyster mushrooms",
        ],
        translit: [
          "veshenka",
          "veshenki",
        ],
      },

      {
        id: "parasol_mushroom",
        ru: "зонтик",
        en: "parasol mushroom",
        aliases: [
          "зонтики",
          "зонтиков",
          "зонтиками",

          "зонтик пёстрый",
          "зонтик пестрый",

          "зонтики пёстрые",
          "зонтики пестрые",

          "зонтиков пёстрых",
          "зонтиков пестрых",

          "пёстрый зонтик",
          "пестрый зонтик",

          "parasol mushroom",
          "parasol mushrooms",
        ],
        translit: [
          "zontik",
          "zontiki",
          "pestryy zontik",
        ],
      },

      {
        id: "black_milk_cap",
        ru: "чернушка",
        en: "black milk cap",
        aliases: [
          "чернушки",
          "чернушек",

            "чёрные груздья",
            "черные груздья",

            "чёрных груздьев",
            "черных груздьев",

            "чёрными груздьями",
            "черными груздьями",

          "чёрный груздь",
          "черный груздь",

          "чёрные грузди",
          "черные грузди",

          "чёрных груздей",
          "черных груздей",

          "груздь чёрный",
          "груздь черный",

          "грузди чёрные",
          "грузди черные",

          "black milk cap",
          "black milk caps",
          "black milkcap",
          "black milkcaps",
        ],
        translit: [
          "chernushka",
          "chernushki",
          "chernyy gruzd",
        ],
      },

      {
        id: "moss_mushroom",
        ru: "моховик",
        en: "mossy bolete",
        aliases: [
          "моховики",
          "моховика",
          "моховиков",
          "моховиками",

          "моховик зелёный",
          "моховик зеленый",

          "зелёный моховик",
          "зеленый моховик",

          "зелёные моховики",
          "зеленые моховики",

          "green bolete",
          "green boletes",
          "mossy bolete",
          "mossy boletes",
        ],
        translit: [
          "mokhovik",
          "mokhoviki",
          "zelenyy mokhovik",
        ],
      },

      {
        id: "tricholoma",
        ru: "рядовка",
        en: "tricholoma",
        aliases: [
          "рядовки",
          "рядовок",
          "рядовками",

          "рядовка фиолетовая",
          "фиолетовая рядовка",

          "фиолетовые рядовки",
          "фиолетовых рядовок",

          "wood blewit",
          "wood blewits",
          "tricholoma",
          "tricholomas",
        ],
        translit: [
          "ryadovka",
          "ryadovki",
          "fioletovaya ryadovka",
        ],
      },

      {
        id: "hedgehog_mushroom",
        ru: "ежовик",
        en: "hedgehog mushroom",
        aliases: [
          "ёжовик",
          "ежовики",
          "ёжовики",
          "ежовиков",
          "ёжовиков",

          "ежовик жёлтый",
          "ёжовик жёлтый",
          "ежовик желтый",
          "ёжовик желтый",

          "жёлтый ежовик",
          "жёлтый ёжовик",
          "желтый ежовик",
          "желтый ёжовик",

          "hedgehog mushroom",
          "hedgehog mushrooms",
        ],
        translit: [
          "ezhovik",
          "ezhoviki",
          "zheltyy ezhovik",
        ],
      },

      {
        id: "truffle",
        ru: "трюфель",
        en: "truffle",
        aliases: [
          "трюфели",
          "трюфеля",
          "трюфелей",
          "трюфелями",

          "трюфель летний",
          "летний трюфель",
          "летние трюфели",
          "летних трюфелей",

          "summer truffle",
          "summer truffles",
          "truffle",
          "truffles",
        ],
        translit: [
          "tryufel",
          "tryufeli",
          "letniy tryufel",
        ],
      },

      {
        id: "milkcap_serushka",
        ru: "серушка",
        en: "grey milk cap",
        aliases: [
          "серушки",
          "серушек",
          "серушками",

          "grey milk cap",
          "grey milk caps",
          "gray milk cap",
          "gray milk caps",
        ],
        translit: [
          "serushka",
          "serushki",
        ],
      },

      {
        id: "talker",
        ru: "говорушка",
        en: "talker",
        aliases: [
          "говорушки",
          "говорушек",
          "говорушками",

          "говорушка дымчатая",
          "дымчатая говорушка",

          "дымчатые говорушки",
          "дымчатых говорушек",

          "clouded agaric",
          "clouded agarics",
          "talker",
          "talkers",
        ],
        translit: [
          "govorushka",
          "govorushki",
          "dymchataya govorushka",
        ],
      },

      {
        id: "mushroom_cap",
        ru: "колпак",
        en: "mushroom cap",
        aliases: [
          "колпаки",
          "колпака",
          "колпаков",
          "колпаками",

          "колпак кольчатый",
          "кольчатый колпак",

          "кольчатые колпаки",
          "кольчатых колпаков",

          "gypsy mushroom",
          "gypsy mushrooms",
        ],
        translit: [
          "kolpak",
          "kolpaki",
          "kolpak kolchatyy",
        ],
      },

      {
        id: "fly_agaric",
        ru: "мухомор",
        en: "fly agaric",
        aliases: [
          "мухоморы",
          "мухомора",
          "мухоморов",
          "мухоморами",

          "мухомор красный",
          "красный мухомор",

          "красные мухоморы",
          "красных мухоморов",

          "fly agaric",
          "fly agarics",
          "red fly agaric",
        ],
        translit: [
          "mukhomor",
          "mukhomory",
          "krasnyy mukhomor",
        ],
      },

      {
        id: "false_morel",
        ru: "строчок",
        en: "false morel",
        aliases: [
          "строчки",
          "строчка",
          "строчков",
          "строчками",

          "false morel",
          "false morels",
        ],
        translit: [
          "strochok",
          "strochki",
        ],
      },

      {
        id: "webcap",
        ru: "паутинник",
        en: "webcap",
        aliases: [
          "паутинники",
          "паутинника",
          "паутинников",
          "паутинниками",

          "паутинник фиолетовый",
          "фиолетовый паутинник",

          "фиолетовые паутинники",
          "фиолетовых паутинников",

          "violet webcap",
          "violet webcaps",
          "webcap",
          "webcaps",
        ],
        translit: [
          "pautinnik",
          "pautinniki",
          "fioletovyy pautinnik",
        ],
      },

      {
        id: "fiddlehead",
        ru: "скрипица",
        en: "fleecy milk-cap",
        aliases: [
          "скрипицы",
          "скрипиц",
          "скрипицами",

          "fleecy milk-cap",
          "fleecy milk-cap mushroom",
        ],
        translit: [
          "skripitsa",
          "skripitsy",
        ],
      },

      {
        id: "bitter_bolete",
        ru: "желчный гриб",
        en: "bitter bolete",
        aliases: [
          "желчный гриб",
          "желчные грибы",
          "желчного гриба",
          "желчных грибов",

          "bitter bolete",
          "bitter boletes",
          "bitter bolete mushroom",
        ],
        translit: [
          "zhelchnyy grib",
          "zhelchnye griby",
          "gorchak",
          "gorchaki",
        ],
      },
    ];

const RUSSIAN_SOCIAL_QUERIES = [
    "mushroomtest2026",
  "грибы",
  "грибники",
  "белый гриб",
  "лисички",
  "опята",
  "подберезовики",
  "подосиновики",
  "рыжики",
  "маслята",
  "грузди",
  "сморчки",
];

const ENGLISH_SOCIAL_QUERIES = [
    "mushroomtest2026",
  "mushrooms",
  "foraging",
  "porcini",
  "chanterelles",
  "honey mushrooms",
  "birch bolete",
  "aspen bolete",
  "morels",
  "russula",
];

export const THREADS_QUERIES = [

  ...new Set([
    ...RUSSIAN_SOCIAL_QUERIES,
    ...ENGLISH_SOCIAL_QUERIES,
  ]),
];

export const REDDIT_QUERIES = [
  ...new Set([
    ...RUSSIAN_SOCIAL_QUERIES,
    ...ENGLISH_SOCIAL_QUERIES,
  ]),
];

export const PIKABU_QUERIES = [
  ...RUSSIAN_SOCIAL_QUERIES,
];

export function getSpeciesById(
  id: string,
): MushroomSpecies | undefined {
  return MUSHROOM_SPECIES.find(
    (species) => species.id === id,
  );
}