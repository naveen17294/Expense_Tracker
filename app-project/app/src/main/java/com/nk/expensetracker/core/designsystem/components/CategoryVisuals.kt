package com.nk.expensetracker.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material.icons.rounded.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.nk.expensetracker.data.db.entity.CategoryEntity
import kotlin.math.abs

/**
 * Maps a category to an icon and an accent colour.
 *
 * Categories in the database carry a `colorHex`, but nothing stops a
 * user-defined category from having none, so a stable colour is derived from the
 * name instead. That keeps a category the same colour on every screen and after
 * every restart, rather than picking a random one on each composition.
 */
object CategoryVisuals {

    private val keywordIcons = listOf(
        listOf("food", "dining", "restaurant", "eat") to Icons.Rounded.Restaurant,
        listOf("cafe", "coffee", "tea") to Icons.Rounded.LocalCafe,
        listOf("grocery", "groceries", "supermarket", "instamart") to Icons.Rounded.ShoppingCart,
        listOf("fuel", "petrol", "gas", "diesel") to Icons.Rounded.DirectionsCar,
        listOf("taxi", "cab", "auto", "ride", "ola", "uber", "rapido") to Icons.Rounded.DirectionsCar,
        listOf("transport", "travel", "metro", "bus", "rail", "train", "flight") to Icons.Rounded.Train,
        listOf("holiday", "trip") to Icons.Rounded.Flight,
        listOf("entertainment", "movie", "music", "game") to Icons.Rounded.Movie,
        listOf("shopping", "clothes", "apparel", "mall") to Icons.Rounded.ShoppingBag,
        listOf("bill", "utilit", "electric", "water", "maintenance", "recharge", "broadband") to Icons.Rounded.Bolt,
        listOf("health", "medical", "medicine", "hospital", "doctor", "pharmacy") to Icons.Rounded.LocalHospital,
        listOf("education", "school", "course", "book", "stationery") to Icons.Rounded.School,
        listOf("personal", "care", "grooming", "salon", "cosmetic", "pet") to Icons.Rounded.Pets,
        listOf("charity", "gift", "donation") to Icons.Rounded.CardGiftcard,
        listOf("salary", "income", "wage", "revenue", "bonus") to Icons.Rounded.Work,
        listOf("transfer", "self") to Icons.Rounded.SwapHoriz,
        listOf("rent", "housing", "house", "home") to Icons.Rounded.Home,
        listOf("fee", "charge", "interest") to Icons.Rounded.Receipt,
        listOf("bank", "account") to Icons.Rounded.AccountBalance,
        listOf("wallet", "cash") to Icons.Rounded.AccountBalanceWallet,
        listOf("card", "credit") to Icons.Rounded.CreditCard
    )

    private val fallbackColors = listOf(
        Color(0xFF00696D),
        Color(0xFF1B5FA8),
        Color(0xFF8E4E00),
        Color(0xFF7D5260),
        Color(0xFF2E6B33),
        Color(0xFF3949AB),
        Color(0xFF9C2C77)
    )

    /** Parses `#RRGGBB` / `#AARRGGBB`, returning null for anything malformed. */
    fun parseColor(hex: String?): Color? {
        val raw = hex?.trim()?.removePrefix("#") ?: return null
        return runCatching {
            when (raw.length) {
                6 -> Color("FF$raw".toLong(16))
                8 -> Color(raw.toLong(16))
                else -> null
            }
        }.getOrNull()
    }

    private fun stableIndex(key: String): Int =
        if (key.isEmpty()) 0 else abs(key.lowercase().hashCode())

    private fun matchIcon(name: String): ImageVector? =
        keywordIcons.firstOrNull { (keywords, _) -> keywords.any(name::contains) }?.second

    fun iconFor(categoryId: String?, categoryName: String?): ImageVector {
        matchIcon(categoryName?.lowercase().orEmpty())?.let { return it }
        matchIcon(categoryId?.lowercase().orEmpty())?.let { return it }
        return Icons.Rounded.Category
    }

    fun colorFor(categoryId: String?, categoryName: String?, explicitHex: String?): Color {
        parseColor(explicitHex)?.let { return it }
        return fallbackColors[stableIndex(categoryName ?: categoryId.orEmpty()) % fallbackColors.size]
    }

    fun iconFor(category: CategoryEntity): ImageVector = iconFor(category.id, category.name)

    fun colorFor(category: CategoryEntity): Color = colorFor(category.id, category.name, category.colorHex)
}