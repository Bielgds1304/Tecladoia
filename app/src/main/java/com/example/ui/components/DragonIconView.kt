package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.DragonIconStyle

@Composable
fun DragonIconView(
    iconStyle: DragonIconStyle,
    customUri: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF141414))
            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (iconStyle) {
            DragonIconStyle.TATTOO_ART -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_dragon_tattoo),
                    contentDescription = "Ícone Dragão Tatuagem",
                    modifier = Modifier
                        .size(size * 0.85f)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            DragonIconStyle.VECTOR_MINIMAL -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_dragon_vector),
                    contentDescription = "Ícone Dragão Glifo Minimalista",
                    modifier = Modifier
                        .size(size * 0.65f)
                        .padding(2.dp),
                    contentScale = ContentScale.Fit
                )
            }
            DragonIconStyle.CUSTOM_IMAGE -> {
                if (!customUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(Uri.parse(customUri))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Ícone Personalizado do Usuário",
                        modifier = Modifier
                            .size(size)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.ic_dragon_tattoo)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dragon_tattoo),
                        contentDescription = "Ícone Dragão Padrão",
                        modifier = Modifier
                            .size(size * 0.85f)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}
