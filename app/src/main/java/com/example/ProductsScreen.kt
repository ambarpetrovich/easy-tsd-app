package com.example

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BarcodeEntity
import com.example.data.ProductAttributeEntity
import com.example.data.ProductEntity
import com.example.data.ProductWithDetails
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductsScreen(
    onBack: () -> Unit,
    initialAction: String? = null,
    initialGtin: String? = null,
    viewModel: ProductsViewModel = viewModel()
) {
    val groupedProducts by viewModel.filteredProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val existingTypes by viewModel.existingBarcodeTypes.collectAsState()
    val existingAttributeNames by viewModel.existingAttributeNames.collectAsState()

    var showEditDialog by remember { mutableStateOf<ProductWithDetails?>(null) }
    var isAdding by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(initialAction, initialGtin) {
        if (initialGtin != null) {
            if (initialAction == "create") {
                showEditDialog = ProductWithDetails(
                    product = ProductEntity(name = "", gtin = initialGtin),
                    barcodes = emptyList(),
                    attributes = emptyList()
                )
                isAdding = true
            } else if (initialAction == "edit") {
                viewModel.searchQuery.value = initialGtin
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_83), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_68))
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO Import/Export */ }) {
                        Icon(Icons.Default.ImportExport, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_138))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showEditDialog = ProductWithDetails(
                        product = ProductEntity(name = "", gtin = ""),
                        barcodes = emptyList(),
                        attributes = emptyList()
                    )
                    isAdding = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_47))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            // Search and Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_137)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_136))
                            }
                        }
                    },
                    singleLine = true
                )
            }

            // Category filter chips
            if (categories.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory).takeIf { it >= 0 } ?: 0,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp,
                    indicator = {},
                    divider = {}
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.selectedCategory.value = null },
                        label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_135)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.selectedCategory.value = cat },
                            label = { Text(cat.ifEmpty { androidx.compose.ui.res.stringResource(com.example.R.string.str_115) }) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            if (groupedProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_134), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedProducts.forEach { (category, items) ->
                        stickyHeader {
                            Text(
                                text = category.ifEmpty { androidx.compose.ui.res.stringResource(com.example.R.string.str_115) },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                                    .padding(vertical = 8.dp)
                            )
                        }
                        items(items, key = { it.product.id }) { item ->
                            ProductCard(
                                item = item,
                                onEdit = {
                                    showEditDialog = item
                                    isAdding = false
                                },
                                onDelete = { viewModel.deleteProduct(item.product) }
                            )
                        }
                    }
                }
            }
        }
    }

    showEditDialog?.let { currentItem ->
        val saveErrorText = androidx.compose.ui.res.stringResource(com.example.R.string.str_133)
        ProductEditDialog(
            initialItem = currentItem,
            existingTypes = existingTypes,
            existingAttributeNames = existingAttributeNames,
            onDismiss = { showEditDialog = null },
            onSave = { updatedProduct, updatedBarcodes, updatedAttributes ->
                viewModel.addOrUpdateProduct(updatedProduct, updatedBarcodes, updatedAttributes) { result ->
                    result.onSuccess {
                        showEditDialog = null
                    }.onFailure { error ->
                        scope.launch {
                            snackbarHostState.showSnackbar(error.message ?: saveErrorText)
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ProductCard(
    item: ProductWithDetails,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.product.name.ifEmpty { androidx.compose.ui.res.stringResource(com.example.R.string.str_132) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (item.product.gtin.isNotEmpty()) {
                            Text(
                                text = "GTIN: ${item.product.gtin}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        if (item.attributes.isNotEmpty()) {
                            Text(
                                text = item.attributes.joinToString(" | ") { "${it.name}: ${it.value}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_131), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_130), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (item.barcodes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    item.barcodes.forEach { bc ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text("${bc.value} (${bc.type})", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditDialog(
    initialItem: ProductWithDetails,
    existingTypes: List<String>,
    existingAttributeNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, List<BarcodeEntity>, List<ProductAttributeEntity>) -> Unit
) {
    var name by remember { mutableStateOf(initialItem.product.name) }
    var gtin by remember { mutableStateOf(initialItem.product.gtin) }
    var category by remember { mutableStateOf(initialItem.product.category) }
    
    // Mutable state list for barcodes
    val barcodes = remember { mutableStateListOf(*initialItem.barcodes.toTypedArray()) }
    
    // Mutable state list for attributes
    val attributes = remember { mutableStateListOf(*initialItem.attributes.toTypedArray()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .imePadding(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text(
                    text = if (initialItem.product.id == 0) androidx.compose.ui.res.stringResource(com.example.R.string.str_129) else androidx.compose.ui.res.stringResource(com.example.R.string.str_128),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_127)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = gtin,
                    onValueChange = { gtin = it },
                    label = { Text("GTIN") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_126)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_125), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                // Barcode list editor
                barcodes.forEachIndexed { index, bc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = bc.value,
                                onValueChange = { newVal -> barcodes[index] = bc.copy(value = newVal) },
                                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_124)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            // Very simple type selector via TextField (since type can be arbitrary)
                            var expanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = !expanded }
                            ) {
                                OutlinedTextField(
                                    value = bc.type,
                                    onValueChange = { newVal -> barcodes[index] = bc.copy(type = newVal) },
                                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_123)) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                if (existingTypes.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        existingTypes.forEach { typeOption ->
                                            DropdownMenuItem(
                                                text = { Text(typeOption) },
                                                onClick = {
                                                    barcodes[index] = bc.copy(type = typeOption)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { barcodes.removeAt(index) }) {
                            Icon(Icons.Default.Clear, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_122), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                val defaultType = androidx.compose.ui.res.stringResource(com.example.R.string.str_113)
                Button(
                    onClick = {
                        barcodes.add(BarcodeEntity(productId = 0, value = "", type = defaultType))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_121))
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_120), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                attributes.forEachIndexed { index, attr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            var expandedName by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = expandedName,
                                onExpandedChange = { expandedName = !expandedName }
                            ) {
                                OutlinedTextField(
                                    value = attr.name,
                                    onValueChange = { newVal -> attributes[index] = attr.copy(name = newVal) },
                                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_119)) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedName) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                if (existingAttributeNames.isNotEmpty()) {
                                    ExposedDropdownMenu(
                                        expanded = expandedName,
                                        onDismissRequest = { expandedName = false }
                                    ) {
                                        existingAttributeNames.forEach { nameOption ->
                                            DropdownMenuItem(
                                                text = { Text(nameOption) },
                                                onClick = {
                                                    attributes[index] = attr.copy(name = nameOption)
                                                    expandedName = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = attr.value,
                                onValueChange = { newVal -> attributes[index] = attr.copy(value = newVal) },
                                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_118)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        IconButton(onClick = { attributes.removeAt(index) }) {
                            Icon(Icons.Default.Clear, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_117), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                Button(
                    onClick = {
                        attributes.add(ProductAttributeEntity(productId = 0, name = "", value = ""))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_116))
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_13))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updatedProduct = initialItem.product.copy(
                                name = name,
                                gtin = gtin,
                                category = category
                            )
                            onSave(updatedProduct, barcodes.toList(), attributes.toList())
                        },
                        enabled = name.isNotBlank() && 
                                  barcodes.all { it.value.isNotBlank() && it.type.isNotBlank() } &&
                                  attributes.all { it.name.isNotBlank() && it.value.isNotBlank() }
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_114))
                    }
                }
            }
        }
    }
}
