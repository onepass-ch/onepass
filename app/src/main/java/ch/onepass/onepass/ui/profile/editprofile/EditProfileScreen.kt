package ch.onepass.onepass.ui.profile.editprofile

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.onepass.onepass.ui.theme.Primary
import ch.onepass.onepass.ui.theme.Background
import ch.onepass.onepass.ui.theme.OnBackground
import ch.onepass.onepass.ui.theme.Surface
import ch.onepass.onepass.ui.theme.OnSurface
import ch.onepass.onepass.ui.theme.Error
import coil.compose.SubcomposeAsyncImage
import com.smarttoolfactory.cropper.ImageCropper
import com.smarttoolfactory.cropper.model.AspectRatio
import com.smarttoolfactory.cropper.model.OutlineType
import com.smarttoolfactory.cropper.model.OvalCropShape
import com.smarttoolfactory.cropper.settings.CropDefaults
import com.smarttoolfactory.cropper.settings.CropOutlineProperty
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlinx.coroutines.launch

object EditProfileTestTags {
    const val SCREEN = "edit_profile_screen"
    const val AVATAR = "edit_profile_avatar"
    const val SAVE_BUTTON = "edit_profile_save"
    const val NAME_FIELD = "edit_profile_name"
    const val COUNTRY_FIELD = "edit_profile_country"
    const val PHONE_FIELD = "edit_profile_phone"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val formState by viewModel.formState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val countryList by viewModel.countryList.collectAsState()
    val selectedCountryCode by viewModel.selectedCountryCode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // --- UI STATE ---
    var prefixDropdownExpanded by remember { mutableStateOf(false) }
    var countryDropdownExpanded by remember { mutableStateOf(false) }
    var showAvatarOverlay by remember { mutableStateOf(false) }
    var avatarBounds by remember { mutableStateOf(Rect.Zero) }

    // --- CROPPER STATE ---
    var imageToCrop by remember { mutableStateOf<ImageBitmap?>(null) }
    var showCropper by remember { mutableStateOf(false) }

    // --- HELPER LOGIC ---

    // Filter Logic for Country Search
    val filteredCountries =
        remember(formState.country, countryList) {
            if (formState.country.isBlank()) {
                countryList
            } else {
                countryList.filter { it.first.contains(formState.country, ignoreCase = true) }
            }
        }

    // Identify Flag for Leading Icon
    val currentCountryFlag =
        remember(formState.country) {
            if (formState.country.isNotBlank()) getFlagFromCountryName(formState.country) else ""
        }

    // Image Selection Handler
    fun onImageSelected(uri: Uri) {
        try {
            val bitmap =
                if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION") MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
                }
            imageToCrop = bitmap.asImageBitmap()
            showCropper = true
            showAvatarOverlay = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Launchers
    val galleryLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri?
            ->
            uri?.let { onImageSelected(it) }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success
            ->
            if (success) viewModel.avatarCameraUri?.let { onImageSelected(it) }
        }

    // Blur Animation
    val blurRadius by
    animateDpAsState(
        targetValue = if (showAvatarOverlay || showCropper) 15.dp else 0.dp,
        animationSpec = tween(durationMillis = 400),
        label = "blur")

    LaunchedEffect(Unit) { viewModel.loadProfile() }
    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            snackbarHostState.showSnackbar("Profile updated successfully")
            onNavigateBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        // 1. MAIN SCREEN CONTENT
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Profile", color = OnBackground) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Back",
                                tint = OnBackground)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.saveProfile() },
                            enabled = !uiState.isLoading,
                            modifier = Modifier.testTag(EditProfileTestTags.SAVE_BUTTON)) {
                            Text(
                                "Save",
                                color = if (uiState.isLoading) Color.Gray else Primary,
                                fontWeight = FontWeight.Bold)
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Background))
            },
            containerColor = Background,
            snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding).blur(blurRadius)) {
                if (uiState.isLoading && formState.displayName.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    Column(
                        modifier =
                            Modifier.fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                                .testTag(EditProfileTestTags.SCREEN)) {
                        // --- AVATAR ---
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center) {
                            Box(
                                modifier =
                                    Modifier.size(120.dp)
                                        .onGloballyPositioned {
                                            if (!showAvatarOverlay) avatarBounds = it.boundsInRoot()
                                        }
                                        .clip(CircleShape)
                                        .background(Surface)
                                        .clickable { showAvatarOverlay = true }
                                        .testTag(EditProfileTestTags.AVATAR),
                                contentAlignment = Alignment.Center) {
                                val imageToShow = formState.avatarUri ?: formState.avatarUrl
                                if (imageToShow == null) {
                                    Text(
                                        formState.initials,
                                        color = Color.White,
                                        style = MaterialTheme.typography.headlineLarge)
                                } else {
                                    SubcomposeAsyncImage(
                                        model = imageToShow,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize())
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        // --- DISPLAY NAME ---
                        OutlinedTextField(
                            value = formState.displayName,
                            onValueChange = viewModel::updateDisplayName,
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag(EditProfileTestTags.NAME_FIELD),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Primary,
                                    focusedLabelColor = Primary))

                        Spacer(Modifier.height(16.dp))

                        // --- PHONE & CODE ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExposedDropdownMenuBox(
                                expanded = prefixDropdownExpanded,
                                onExpandedChange = {
                                    prefixDropdownExpanded = !prefixDropdownExpanded
                                },
                                modifier = Modifier.weight(0.35f)) {
                                OutlinedTextField(
                                    value = selectedCountryCode,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Code") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = prefixDropdownExpanded)
                                    },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors =
                                        OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Primary,
                                            focusedLabelColor = Primary))
                                ExposedDropdownMenu(
                                    expanded = prefixDropdownExpanded,
                                    onDismissRequest = { prefixDropdownExpanded = false }) {
                                    countryList.forEachIndexed { index, (name, code) ->
                                        DropdownMenuItem(
                                            text = { Text("+$code  $name") },
                                            onClick = {
                                                viewModel.updateCountryIndex(index)
                                                prefixDropdownExpanded = false
                                            })
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = formState.phone,
                                onValueChange = viewModel::updatePhone,
                                label = { Text("Phone") },
                                singleLine = true,
                                modifier =
                                    Modifier.weight(0.65f).testTag(EditProfileTestTags.PHONE_FIELD),
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        focusedLabelColor = Primary))
                        }

                        Spacer(Modifier.height(16.dp))

                        // --- COUNTRY DROPDOWN (SEARCH + FLAGS) ---
                        ExposedDropdownMenuBox(
                            expanded = countryDropdownExpanded,
                            onExpandedChange = { countryDropdownExpanded = !countryDropdownExpanded }) {
                            OutlinedTextField(
                                value = formState.country,
                                onValueChange = {
                                    viewModel.updateCountry(it)
                                    countryDropdownExpanded = true
                                },
                                label = { Text("Country") },
                                singleLine = true,
                                leadingIcon =
                                    if (currentCountryFlag.isNotEmpty()) {
                                        {
                                            Text(
                                                currentCountryFlag,
                                                style = MaterialTheme.typography.titleLarge)
                                        }
                                    } else null,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                        expanded = countryDropdownExpanded)
                                },
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .menuAnchor()
                                        .testTag(EditProfileTestTags.COUNTRY_FIELD),
                                colors =
                                    OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary,
                                        focusedLabelColor = Primary))

                            if (filteredCountries.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = countryDropdownExpanded,
                                    onDismissRequest = { countryDropdownExpanded = false },
                                    modifier = Modifier.heightIn(max = 250.dp)) {
                                    filteredCountries.forEach { (name, _) ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = getFlagFromCountryName(name),
                                                        style = MaterialTheme.typography.titleLarge,
                                                        modifier = Modifier.padding(end = 12.dp))
                                                    Text(text = name)
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateCountry(name)
                                                countryDropdownExpanded = false
                                            },
                                            contentPadding =
                                                ExposedDropdownMenuDefaults.ItemContentPadding)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. ZOOM OVERLAY
        if (showAvatarOverlay) {
            AvatarZoomOverlay(
                startBounds = avatarBounds,
                currentAvatarUrl = formState.avatarUrl,
                currentAvatarUri = formState.avatarUri,
                initials = formState.initials,
                onDismiss = { showAvatarOverlay = false },
                onChooseGallery = { galleryLauncher.launch("image/*") },
                onTakePhoto = { viewModel.createCameraUri()?.let { cameraLauncher.launch(it) } },
                onRemove = {
                    showAvatarOverlay = false
                    viewModel.removeAvatar()
                })
        }

        // 3. CROPPER OVERLAY (CIRCULAR)
        AnimatedVisibility(
            visible = showCropper,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()) {
            imageToCrop?.let { bitmap ->

                // Properties: Circular Outline + Oval Shape
                val cropProperties = remember {
                    CropDefaults.properties(
                        cropOutlineProperty =
                            CropOutlineProperty(OutlineType.Oval, OvalCropShape(0, "Oval")),
                        aspectRatio = AspectRatio(1f),
                        fixedAspectRatio = true,
                        maxZoom = 5f,
                        handleSize = 20f,
                        overlayRatio = 1f,
                    )
                }

                var crop by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    Column(Modifier.fillMaxSize()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { showCropper = false }) {
                                Icon(Icons.Default.Close, "Cancel", tint = Color.White)
                            }
                            Text("Crop Image", color = Color.White, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { crop = true }) {
                                Icon(Icons.Default.Check, "Done", tint = Primary)
                            }
                        }

                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            ImageCropper(
                                imageBitmap = bitmap,
                                contentDescription = "Crop",
                                cropProperties = cropProperties,
                                crop = crop,
                                onCropStart = {},
                                onCropSuccess = { croppedImage ->
                                    val file =
                                        File(
                                            context.cacheDir,
                                            "avatar_cropped_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { out ->
                                        croppedImage
                                            .asAndroidBitmap()
                                            .compress(Bitmap.CompressFormat.JPEG, 90, out)
                                    }
                                    viewModel.selectAvatarImage(Uri.fromFile(file))
                                    showCropper = false
                                    crop = false
                                })
                        }
                    }
                }
            }
        }
    }
}

// ... AvatarZoomOverlay ...
@Composable
private fun AvatarZoomOverlay(
    startBounds: Rect,
    currentAvatarUrl: String?,
    currentAvatarUri: Uri?,
    initials: String,
    onDismiss: () -> Unit,
    onChooseGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit
) {
    // ... (Keep the exact implementation from before) ...
    val animatable = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var showOptions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        animatable.animateTo(1f, tween(400, easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)))
    }

    fun startDismissSequence() {
        scope.launch {
            showOptions = false
            animatable.animateTo(0f, tween(350, easing = CubicBezierEasing(0.2f, 0f, 0.2f, 1f)))
            onDismiss()
        }
    }

    val animProgress = animatable.value
    // ... Calculations for startX, startY, currentX, currentY ...
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val targetSizePx = with(density) { 300.dp.toPx() }

    val startX = if (startBounds.isEmpty) (screenWidthPx - targetSizePx) / 2f else startBounds.left
    val startY = if (startBounds.isEmpty) (screenHeightPx - targetSizePx) / 2f else startBounds.top
    val startSize = if (startBounds.isEmpty) targetSizePx else startBounds.width

    val currentSizePx = lerp(startSize, targetSizePx, animProgress)
    val currentX = lerp(startX, (screenWidthPx - targetSizePx) / 2f, animProgress)
    val currentY = lerp(startY, (screenHeightPx - targetSizePx) / 2f, animProgress)

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f * animProgress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    startDismissSequence()
                }) {
        Box(
            modifier =
                Modifier.graphicsLayer {
                    translationX = currentX
                    translationY = currentY
                }
                    .size(with(density) { currentSizePx.toDp() })
                    .clip(CircleShape)
                    .background(Surface),
            contentAlignment = Alignment.Center) {
            // ... Image/Text content ...
            val imageToShow = currentAvatarUri ?: currentAvatarUrl
            if (imageToShow == null) {
                val fontSize = lerp(32f, 88f, animProgress)
                Text(
                    initials,
                    color = Color.White,
                    style =
                        MaterialTheme.typography.displayLarge.copy(
                            fontSize = fontSize.sp, fontWeight = FontWeight.Bold))
            } else {
                SubcomposeAsyncImage(
                    model = imageToShow,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize())
            }
        }

        // ... Controls ...
        val controlsAlpha = if (animProgress > 0.8f) (animProgress - 0.8f) * 5f else 0f
        if (controlsAlpha > 0f) {
            // Pencil Icon
            Box(
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = controlsAlpha },
                contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(300.dp)) {
                    Box(
                        modifier =
                            Modifier.align(Alignment.BottomEnd)
                                .offset(x = (-16).dp, y = (-16).dp)
                                .size(50.dp)
                                .background(Color.Black, CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                                .clickable { showOptions = !showOptions },
                        contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp))
                    }
                }
            }

            // Bottom Sheet
            AnimatedVisibility(
                visible = showOptions,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Surface,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.width(300.dp)) {
                        Column {
                            DialogOption("Choose from gallery", onChooseGallery)
                            HorizontalDivider(
                                color = OnSurface.copy(alpha = 0.12f))
                            DialogOption("Take photo", onTakePhoto)
                            if (currentAvatarUrl != null || currentAvatarUri != null) {
                                HorizontalDivider(
                                    color = OnSurface.copy(alpha = 0.12f))
                                DialogOption(
                                    "Remove photo",
                                    onRemove,
                                    Error)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    TextButton(onClick = { startDismissSequence() }) {
                        Text("Cancel", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogOption(text: String, onClick: () -> Unit, textColor: Color = Color.White) {
    Text(
        text,
        color = textColor,
        style = MaterialTheme.typography.bodyLarge,
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 16.dp, horizontal = 20.dp))
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float =
    start + fraction * (stop - start)

fun getFlagFromCountryName(countryName: String): String {
    // optimize: Iterate locales to find the ISO code from the name
    val isoCode =
        Locale.getAvailableLocales()
            .firstOrNull { it.displayCountry.equals(countryName, ignoreCase = true) }
            ?.country ?: return "🌍"

    // Convert ISO code (2 letters) to Flag Emoji
    val firstLetter = Character.codePointAt(isoCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(isoCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}
