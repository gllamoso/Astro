package dev.gtcl.astro.html

data class Cell(
    val text: SimpleText,
    val alignment: Alignment
) {
    enum class Alignment{
        LEFT,
        CENTER,
        RIGHT
    }
}

data class HeaderCell(
    val text: SimpleText,
    val alignment: Cell.Alignment
)