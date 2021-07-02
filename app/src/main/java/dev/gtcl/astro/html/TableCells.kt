package dev.gtcl.astro.html

data class Cell(
    val simpleText: SimpleText,
    val alignment: Alignment
) {
    enum class Alignment {
        LEFT,
        CENTER,
        RIGHT
    }
}

data class HeaderCell(
    val simpleText: SimpleText,
    val alignment: Cell.Alignment
)