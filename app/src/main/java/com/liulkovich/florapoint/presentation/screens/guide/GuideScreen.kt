package com.liulkovich.florapoint.presentation.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.liulkovich.florapoint.R
import com.liulkovich.florapoint.domain.FloraCategory
import com.liulkovich.florapoint.domain.Reference
import com.liulkovich.florapoint.domain.localizedName


private enum class ForestType {
    CONIFEROUS,
    DECIDUOUS,
    MIXED
}

private fun matchesForest(
    reference: Reference,
    forest: ForestType?
): Boolean {
    if (forest == null) return true

    if (reference.category != FloraCategory.MUSHROOM.key) {
        return false
    }

    val habitat = reference.habitatRu.lowercase()

    return when (forest) {
        ForestType.CONIFEROUS -> listOf(
            "хвойн", "соснов", "елов", "ельник"
        ).any { it in habitat }

        ForestType.DECIDUOUS -> listOf(
            "лиственн", "берёз", "берез",
            "осинов", "осинник", "дубов", "буков"
        ).any { it in habitat }

        ForestType.MIXED -> "смешанн" in habitat
    }
}


private val dangerousMushroomIds = setOf(
    26,    27,    75,    74,    73,    24,    79,    72
)

fun isDangerousMushroom(id: Int): Boolean {
    return id in dangerousMushroomIds
}


@Composable
fun GuideScreen(
    modifier: Modifier = Modifier,
    viewModel: GuideViewModel = hiltViewModel(),
    onClickDetail: (Reference) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current


    var selectedForest by remember {
        mutableStateOf<ForestType?>(null)
    }

    val filteredSpecies = state.species.filter {
        matchesForest(it, selectedForest)
    }


    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                SearchBar(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    query = state.query,
                    onQueryChange = {
                        viewModel.processCommand(GuideCommand.InputSearchQuery(it))
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                PanelFilter(
                    selectedCategories = state.selectedCategories,
                    onCategoryChange = { categoryName ->
                        selectedForest = null
                        viewModel.processCommand(
                            GuideCommand.CheckCategory(categoryName)
                        )
                    }
                )

                if (
                    state.selectedCategories.size == 1 &&
                    FloraCategory.MUSHROOM.key in state.selectedCategories
                ) {
                    ForestFilter(
                        selectedForest = selectedForest,
                        onForestSelected = { selectedForest = it }
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(contentPadding = innerPadding) {

                if (
                    state.selectedCategories.isEmpty() ||
                    FloraCategory.MUSHROOM.key in state.selectedCategories
                ) {
                    item {
                        GuideSafetyNotice(
                            modifier = Modifier.padding(
                                horizontal = 24.dp,
                                vertical = 10.dp
                            )
                        )
                    }
                }


                item { Spacer(modifier = Modifier.height(5.dp)) }
                items(items = filteredSpecies, key = { it.id }) { speciesItem ->
                    GuideCard(
                        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        imageName = speciesItem.imageName,
                        textName = speciesItem.localizedName(),
                        startMonth = speciesItem.startMonth,
                        endMonth = speciesItem.endMonth,
                        reference = speciesItem,
                        onNotifChange = { isChecked ->
                            viewModel.processCommand(
                                GuideCommand.ToggleNotification(
                                    id = speciesItem.id,
                                    enabled = if (isChecked) 1 else 0
                                )
                            )
                        },
                        onClickDetail = onClickDetail,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                shape = RoundedCornerShape(10.dp)
            ),
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = stringResource(R.string.enter_a_name),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        shape = RoundedCornerShape(10.dp),
        singleLine = true
    )
}

@Composable
fun GuideCard(
    modifier: Modifier = Modifier,
    imageName: String,
    textName: String,
    startMonth: Int,
    endMonth: Int,
    reference: Reference,
    onNotifChange: (Boolean) -> Unit,
    onClickDetail: (Reference) -> Unit
) {
    val context = LocalContext.current
    var selectedNotif by remember(reference.isNotifEnabled) {
        mutableStateOf(reference.isNotifEnabled == 1)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick = { onClickDetail(reference) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val firstImageName = remember(imageName) {
                imageName
                    .split(",")
                    .map { it.trim() }
                    .firstOrNull()
                    ?: ""
            }

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(
                        context.resources.getIdentifier(
                            firstImageName,
                            "drawable",
                            context.packageName
                        ).takeIf { it != 0 } ?: R.drawable.ic_launcher_background
                    )
                    .crossfade(true)
                    .size(144)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = textName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (isDangerousMushroom(reference.id)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(14.dp)
                        )

                        Text(
                            text = stringResource(R.string.dangerous_mushroom),
                            color = Color(0xFFFF9800),
                            fontSize = 12.sp,
                            fontWeight = Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = stringResource(
                        R.string.season,
                        numberInString(startMonth).first,
                        numberInString(endMonth).second
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconToggleButton(
                checked = selectedNotif,
                onCheckedChange = { isChecked ->
                    selectedNotif = isChecked
                    onNotifChange(isChecked)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (selectedNotif) Color(0xFF4CAF50)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
@Composable
fun numberInString(number: Int): Pair<String, String> {
    return when (number) {
        1 -> stringResource(R.string.from_january) to stringResource(R.string.january)
        2 -> stringResource(R.string.from_february) to stringResource(R.string.february)
        3 -> stringResource(R.string.from_march) to stringResource(R.string.march)
        4 -> stringResource(R.string.from_april) to stringResource(R.string.april)
        5 -> stringResource(R.string.from_may) to stringResource(R.string.may)
        6 -> stringResource(R.string.from_june) to stringResource(R.string.june)
        7 -> stringResource(R.string.from_july) to stringResource(R.string.july)
        8 -> stringResource(R.string.from_august) to stringResource(R.string.august)
        9 -> stringResource(R.string.from_september) to stringResource(R.string.september)
        10 -> stringResource(R.string.from_october) to stringResource(R.string.october)
        11 -> stringResource(R.string.from_november) to stringResource(R.string.november)
        12 -> stringResource(R.string.from_december) to stringResource(R.string.december)
        else -> stringResource(R.string.unknown) to stringResource(R.string.unknown)
    }
}

@Composable
fun PanelFilter(
    selectedCategories: Set<String>,
    onCategoryChange: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLargeFont = configuration.fontScale > 1.15f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        FloraCategory.entries
            .filter { it != FloraCategory.OTHER }
            .forEach { category ->
                val isSelected = selectedCategories.contains(category.key)

                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryChange(category.key) },
                    label = {
                        if (isLargeFont) {
                            Text(
                                text = category.emoji,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "${category.emoji} ${stringResource(category.stringRes)}",
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    modifier = Modifier.height(IntrinsicSize.Min)
                )
            }
    }
}


@Composable
private fun GuideSafetyNotice(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(18.dp)
            )

            Text(
                text = stringResource(R.string.safety_notice_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = Bold
            )
        }

        Text(
            text = stringResource(R.string.safety_notice_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun ForestFilter(
    selectedForest: ForestType?,
    onForestSelected: (ForestType?) -> Unit
) {
    Column(
        modifier = Modifier.padding(
            horizontal = 24.dp,
            vertical = 2.dp
        )
    ) {


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val options = listOf(
                null to R.string.forest_all,
                ForestType.CONIFEROUS to R.string.forest_coniferous,
                ForestType.DECIDUOUS to R.string.forest_deciduous,
                ForestType.MIXED to R.string.forest_mixed
            )

            options.forEach { (forest, labelRes) ->
                FilterChip(
                    selected = selectedForest == forest,
                    onClick = { onForestSelected(forest) },
                    label = {
                        Text(stringResource(labelRes))
                    }
                )
            }
        }
    }
}

