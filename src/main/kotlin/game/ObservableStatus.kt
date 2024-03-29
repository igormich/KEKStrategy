package game

enum class ObservableStatus {
    /**
     * Территория не открыта, выглядит как чёрный квадрат
     */
    NotInvestigated,

    /**
     * Территория открыта, но в данный момент рядом нет юнитов или строений,
     * всё ещё видно рельеф, но не видно кто  на нём находится
     */
    Investigated,

    /**
     * Видно что находится на клетке
     */
    Observable
}