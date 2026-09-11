package com.ibem.pedagogo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

data class PedagogicalSpark(
    val quote: String,
    val author: String,
    val isAffirmation: Boolean = false
)

object SparkRepository {
    val sparks = listOf(
        PedagogicalSpark(
            quote = "Teaching is not the filling of a pail, but the lighting of a fire.",
            author = "William Butler Yeats"
        ),
        PedagogicalSpark(
            quote = "The art of teaching is the art of assisting discovery.",
            author = "Mark Van Doren"
        ),
        PedagogicalSpark(
            quote = "Children are not vessels to be filled, but lamps to be lit.",
            author = "Maria Montessori"
        ),
        PedagogicalSpark(
            quote = "Every student can learn, just not on the same day, or in the same way.",
            author = "George Evans"
        ),
        PedagogicalSpark(
            quote = "Take a slow, deep breath before stepping in. Your inner calm becomes your students' sanctuary.",
            author = "Teacher\'s Mindful Practice",
            isAffirmation = true
        ),
        PedagogicalSpark(
            quote = "You don't need to be flawless to be a wonderful teacher. Your care, presence, and listening matter most.",
            author = "Pre-Service Teacher Affirmation",
            isAffirmation = true
        ),
        PedagogicalSpark(
            quote = "Education is the most powerful weapon which you can use to change the world.",
            author = "Nelson Mandela"
        ),
        PedagogicalSpark(
            quote = "To teach is to touch a life forever.",
            author = "Pedagogical Wisdom"
        ),
        PedagogicalSpark(
            quote = "Give yourself grace today. Even the most seasoned educators were once learning how to plan their first lesson.",
            author = "Gentle Encouragement",
            isAffirmation = true
        ),
        PedagogicalSpark(
            quote = "The best teachers show you where to look, but don't tell you what to see.",
            author = "Alexandra K. Trenfor"
        )
    )

    fun getDailySpark(): PedagogicalSpark {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return sparks[dayOfYear % sparks.size]
    }
}

@Composable
fun TeachersSparkCard(
    modifier: Modifier = Modifier
) {
    var currentIndex by remember {
        val initialIndex = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % SparkRepository.sparks.size
        mutableIntStateOf(initialIndex)
    }

    val currentSpark = SparkRepository.sparks[currentIndex]

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (currentSpark.isAffirmation) "TEACHER\'S AFFIRMATION" else "TEACHER\'S SPARK",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        letterSpacing = 1.sp
                    )
                }

                IconButton(
                    onClick = {
                        currentIndex = (currentIndex + 1) % SparkRepository.sparks.size
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "New Spark",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = currentSpark,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "SparkQuote"
            ) { spark ->
                Column {
                    Text(
                        text = "“${spark.quote}”",
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "— ${spark.author}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
