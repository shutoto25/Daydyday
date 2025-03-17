package com.gmail.shu10.dev.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gmail.shu10.dev.app.core.CoreDrawable
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme

/**
 * 天気情報エリアのコンポーネント
 *
 * @param weather 天気状態
 * @param temperature 気温
 * @param location 場所
 * @param modifier Modifier
 */
@Composable
fun WeatherInfoSection(
    weather: WeatherType,
    temperature: String,
    location: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 天気アイコン
            Icon(
                painter = painterResource(id = weather.iconRes),
                contentDescription = weather.description,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 天気情報テキスト
            Column {
                Text(
                    text = weather.description,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = temperature,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = "•",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = location,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

/**
 * 天気の種類
 */
enum class WeatherType(val iconRes: Int, val description: String) {
    SUNNY(CoreDrawable.ic_weather_sunny, "晴れ"),
    CLOUDY(CoreDrawable.ic_weather_cloudy, "曇り"),
    RAINY(CoreDrawable.ic_weather_rainy, "雨"),
    SNOWY(CoreDrawable.ic_weather_snowy, "雪"),
    STORMY(CoreDrawable.ic_weather_stormy, "雷雨")
}

@Preview(showBackground = true)
@Composable
fun WeatherInfoSectionPreview() {
    DaydydayTheme {
        Column {
            WeatherInfoSection(
                weather = WeatherType.SUNNY,
                temperature = "25°C",
                location = "東京"
            )

            Spacer(modifier = Modifier.height(8.dp))

            WeatherInfoSection(
                weather = WeatherType.RAINY,
                temperature = "18°C",
                location = "大阪"
            )
        }
    }
}