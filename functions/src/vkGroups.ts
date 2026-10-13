export type VkGroupScope =
  | "region"
  | "city"
  | "country"
  | "multi-area"
  | "none";

export interface VkKnownGroup {
  ownerId: number;
  screenName: string;
  name: string;
  fallbackLocation?: string;
  scope: VkGroupScope;
}

export const VK_KNOWN_GROUPS: VkKnownGroup[] = [
  {
    ownerId: -129678903,
    screenName: "gribniki37rus",
    name: "Грибы Иваново и Ивановской обл.",
    fallbackLocation: "Ивановская область",
    scope: "region",
  },
  {
    ownerId: -184955770,
    screenName: "gribnievoiskabelarus",
    name: "Грибы: Грибные войска Беларуси",
    scope: "country",
  },
  {
    ownerId: -206506694,
    screenName: "gribi62",
    name: "Грибы и Грибники Рязанской области",
    fallbackLocation: "Рязанская область",
    scope: "region",
  },
  {
    ownerId: -199208851,
    screenName: "club199208851",
    name: "Грибы Пензенского края",
    fallbackLocation: "Пензенская область",
    scope: "region",
  },
  {
    ownerId: -190525425,
    screenName: "samaragrib163",
    name: "Грибы и грибники Самарской области",
    fallbackLocation: "Самарская область",
    scope: "region",
  },
  {
    ownerId: -66128588,
    screenName: "gribyvleningadskoyoblasti",
    name: "Грибы в Ленинградской области 2026",
    fallbackLocation: "Ленинградская область",
    scope: "region",
  },
  {
    ownerId: -228734451,
    screenName: "club228734451",
    name: "ГРИБОИСКАТЕЛЬ",
    scope: "none",
  },
  {
    ownerId: -185354214,
    screenName: "lezpr",
    name: "Определение грибов",
    scope: "none",
  },
  {
    ownerId: -4477835,
    screenName: "lespr",
    name: "Грибы 2026",
    scope: "none",
  },
  {
    ownerId: -221026320,
    screenName: "clubgribnukov",
    name: "Лучшие ГРИБНИКИ Самарской и Ульяновской области",
    scope: "multi-area",
  },
  {
    ownerId: -165503882,
    screenName: "gribnikitula71",
    name: "Грибы и Грибники Тула (Грибы в Туле)",
    fallbackLocation: "Тульская область",
    scope: "region",
  },
  {
    ownerId: -71378810,
    screenName: "gribniki_russia",
    name: "Грибы - моё хобби",
    scope: "none",
  },
  {
    ownerId: -95879985,
    screenName: "grib_tob",
    name: "Грибники Тобольска и Тобольского района",
    fallbackLocation: "Тобольск, Тюменская область",
    scope: "city",
  },
  {
    ownerId: -124646875,
    screenName: "club124646875",
    name: "Грибники Хакасии и Юга Красноярского края.",
    scope: "multi-area",
  },
  {
    ownerId: -149206274,
    screenName: "gribmo",
    name: "Грибы и Грибники Подмосковья",
    fallbackLocation: "Московская область",
    scope: "region",
  },
  {
    ownerId: -129622351,
    screenName: "kurskiegribniki",
    name: "Курские грибники,грибы,ягоды",
    fallbackLocation: "Курская область",
    scope: "region",
  },
  {
    ownerId: -222969360,
    screenName: "club222969360",
    name: "Грибники Рыбаки НСО",
    fallbackLocation: "Новосибирская область",
    scope: "region",
  },
  {
    ownerId: -97470763,
    screenName: "club97470763",
    name: "Грибы в Туле и Тульской области. Тихая охота",
    fallbackLocation: "Тульская область",
    scope: "region",
  },
  {
    ownerId: -165488971,
    screenName: "gribyberries",
    name: "Грибы и Грибники Алтая",
    fallbackLocation: "Алтайский край",
    scope: "region",
  },
  {
    ownerId: -211707051,
    screenName: "gribnik2026",
    name: "Грибники Подмосковья 2026. Идем в лес по грибы",
    fallbackLocation: "Московская область",
    scope: "region",
  },
  {
    ownerId: -183442746,
    screenName: "gribnikimordovii",
    name: "Грибники Мордовии",
    fallbackLocation: "Республика Мордовия",
    scope: "region",
  },
  {
    ownerId: -184852938,
    screenName: "gribmariel",
    name: "Грибы и Ягоды Марий Эл — Йошкин Гриб",
    fallbackLocation: "Республика Марий Эл",
    scope: "region",
  },
  {
    ownerId: -102131464,
    screenName: "cobiraem_gribi",
    name: "Тихая Охота по Грибы",
    scope: "none",
  },
  {
    ownerId: -220156836,
    screenName: "gribniki22rus",
    name: "Грибы и Грибники Алтайского края",
    fallbackLocation: "Алтайский край",
    scope: "region",
  },
  {
    ownerId: -185665482,
    screenName: "gribnik68rus",
    name: "Грибники Тамбова и области \"Грибник 68RUS\"",
    fallbackLocation: "Тамбовская область",
    scope: "region",
  },
  {
    ownerId: -178075269,
    screenName: "grib39",
    name: "Грибы и грибники Калининградской области",
    fallbackLocation: "Калининградская область",
    scope: "region",
  },
  {
    ownerId: -165612799,
    screenName: "mushrooms_spb",
    name: "Грибы и грибники Питера",
    fallbackLocation: "Ленинградская область",
    scope: "region",
  },
  {
    ownerId: -153945292,
    screenName: "club153945292",
    name: "Грибники Альметьевска, Лениногорска и Бугульмы",
    scope: "multi-area",
  },
  {
    ownerId: -153707413,
    screenName: "gribalka66",
    name: "Грибы и грибники Екатеринбурга и Свердловскойобл",
    fallbackLocation: "Свердловская область",
    scope: "region",
  },
  {
    ownerId: -123336188,
    screenName: "omskmushroom",
    name: "Грибы. Грибники Омска и Омской области.",
    fallbackLocation: "Омская область",
    scope: "region",
  },
  {
    ownerId: -102145684,
    screenName: "club102145684",
    name: "Грибы и грибники Ульяновска",
    fallbackLocation: "Ульяновская область",
    scope: "region",
  },
  {
    ownerId: -204211470,
    screenName: "gribniki_tomsk",
    name: "Грибники и Грибы. Томская Область.",
    fallbackLocation: "Томская область",
    scope: "region",
  },
  {
    ownerId: -102329381,
    screenName: "club102329381",
    name: "Грибники Стерлитамака и Республики Башкортостан",
    fallbackLocation: "Республика Башкортостан",
    scope: "region",
  },
  {
    ownerId: -175279427,
    screenName: "gribniki_i_griby",
    name: "Грибы и Грибники Тихая охота",
    scope: "none",
  },
  {
    ownerId: -120911507,
    screenName: "club120911507",
    name: "Грибники Белгородчины",
    fallbackLocation: "Белгородская область",
    scope: "region",
  },
  {
    ownerId: -1391819,
    screenName: "grib_spb",
    name: "Грибы и Грибники СПб",
    fallbackLocation: "Ленинградская область",
    scope: "region",
  },
  {
    ownerId: -129459259,
    screenName: "club129459259",
    name: "Грибники Пензенской области",
    fallbackLocation: "Пензенская область",
    scope: "region",
  },
  {
    ownerId: -125201611,
    screenName: "gribchuvashii",
    name: "Грибники Чувашии, \"Тихая охота\"",
    fallbackLocation: "Чувашская Республика",
    scope: "region",
  },
  {
    ownerId: -5439205,
    screenName: "moscowgrib",
    name: "Грибники Подмосковья",
    fallbackLocation: "Московская область",
    scope: "region",
  },
  {
    ownerId: -98354443,
    screenName: "tyumengrib",
    name: "Грибники Тюмени и Тюменской области",
    fallbackLocation: "Тюменская область",
    scope: "region",
  },
  {
    ownerId: -210136604,
    screenName: "gribniki_rus",
    name: "Грибники всея Руси",
    scope: "none",
  },
  {
    ownerId: -197702065,
    screenName: "club197702065",
    name: "Грибники Самарской Области(Тольятти)",
    fallbackLocation: "Самарская область",
    scope: "region",
  },
  {
    ownerId: -183760997,
    screenName: "club183760997",
    name: "Грибники и ягодники Смоленской области",
    fallbackLocation: "Смоленская область",
    scope: "region",
  },
  {
    ownerId: -131079412,
    screenName: "club131079412",
    name: "ГРИБНИКИ РОССИИ",
    scope: "none",
  },
  {
    ownerId: -171077859,
    screenName: "gribniki_spb",
    name: "Грибники СПб",
    fallbackLocation: "Ленинградская область",
    scope: "region",
  },
];

const VK_KNOWN_GROUPS_BY_OWNER_ID = new Map(
  VK_KNOWN_GROUPS.map((group) => [group.ownerId, group]),
);

export function getVkKnownGroup(
  ownerId: number,
): VkKnownGroup | undefined {
  return VK_KNOWN_GROUPS_BY_OWNER_ID.get(ownerId);
}

export function getVkFallbackLocation(
  ownerId: number,
): string | undefined {
  const group = getVkKnownGroup(ownerId);

  if (!group) {
    return undefined;
  }

  if (
    group.scope !== "region" &&
    group.scope !== "city"
  ) {
    return undefined;
  }

  return group.fallbackLocation;
}