package com.pisheblok.app.data.model

/** Which part of the household inventory an entity belongs to. */
enum class Section { FOOD, CHEM }

/** Kind of a location node. */
enum class LocationType { FLOOR, ROOM, PLACE }

/** Storage units, persisted by stable id. */
enum class QuantityUnit(val id: String) {
    PCS("pcs"),
    PACK("pack"),
    BOTTLE("bottle"),
    BOX("box"),
    KG("kg"),
    G("g"),
    L("l"),
    ML("ml");

    companion object {
        fun fromId(id: String): QuantityUnit = entries.firstOrNull { it.id == id } ?: PCS
    }
}

/** Item categories, split by section. */
enum class ItemCategory(val section: Section?, val id: String) {
    VEG(Section.FOOD, "veg"),
    MEAT(Section.FOOD, "meat"),
    DAIRY(Section.FOOD, "dairy"),
    FROZEN(Section.FOOD, "frozen"),
    GRAINS(Section.FOOD, "grains"),
    CANNED(Section.FOOD, "canned"),
    DRINKS(Section.FOOD, "drinks"),
    SPICES(Section.FOOD, "spices"),
    SWEETS(Section.FOOD, "sweets"),
    SNACKS(Section.FOOD, "snacks"),
    DISH(Section.CHEM, "dish"),
    CLEAN(Section.CHEM, "clean"),
    LAUNDRY(Section.CHEM, "laundry"),
    HYGIENE(Section.CHEM, "hygiene"),
    CARE(Section.CHEM, "care"),
    OTHER(null, "other");

    companion object {
        fun fromId(id: String): ItemCategory = entries.firstOrNull { it.id == id } ?: OTHER

        fun forSection(section: Section): List<ItemCategory> =
            entries.filter { it.section == section || it.section == null }
    }
}

/** State of an item expiry date for UI badges. */
sealed interface ExpiryState {
    data object None : ExpiryState
    data class Ok(val daysLeft: Long) : ExpiryState
    data object Soon : ExpiryState
    data object Expired : ExpiryState
}

/** Priority of a shopping-list entry. */
enum class ShoppingPriority {
    URGENT, LATER;

    companion object {
        fun fromId(id: String): ShoppingPriority =
            entries.firstOrNull { it.name == id } ?: URGENT
    }
}

/** Formats a quantity nicely: 3.0 -> "3", 1.5 -> "1.5". */
fun formatQty(qty: Double): String {
    if (qty == qty.toLong().toDouble()) return qty.toLong().toString()
    return qty.toString().trimEnd('0').trimEnd('.')
}
