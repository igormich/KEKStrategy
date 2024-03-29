package game

import game.entities.BaseBuild
import game.entities.BaseEntity
import game.entities.BaseUnit
import utility.*
import java.awt.Color
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyEvent.VK_Q
import java.awt.event.KeyEvent.VK_T
import java.awt.event.MouseEvent
import java.awt.event.MouseEvent.BUTTON1
import java.awt.event.MouseEvent.BUTTON3

class Player(val name: String) {
    var isLoose = false

    /**
     * Список юнитов, принадлежащих игроку
     */
    private val units = mutableListOf<BaseUnit>()


    /**
     * Список зданий, принадлежащих игроку
     */
    private val builds = mutableListOf<BaseBuild>()

    /**
     * Выбранный юнит. Если выбран не юнит или выбрано здание, то возвращает null
     */
    var selectedUnit: BaseUnit?
        set(value) {
            selectedEntity = value
        }
        get() = if (selectedEntity is BaseUnit) selectedEntity as BaseUnit else null

    /**
     * Выбранное здание. Если здание не выбрано или выбран юнит, то возвращает null
     */
    var selectedBuild: BaseBuild?
        set(value) {
            selectedEntity = value
        }
        get() = if (selectedEntity is BaseBuild) selectedEntity as BaseBuild else null

    var selectedEntity: BaseEntity? = if (entities.isNotEmpty()) entities[0] else null

    /**
     * Список сущностей, принадлежащих игроку, объединяет список юнитов и зданий
     */
    val entities: List<BaseEntity>
        get() = List(units.size + builds.size) {
            if (it < units.size) {
                units[it]
            } else {
                builds[it - units.size]
            }
        }

    /**
     * Возвращает список всех сущностей указанного типа
     */
    inline fun <reified T> getEntitiesOf() =
        entities.filter { b -> b is T }.map { it as T }

    var tmpPath: MutableList<Direction>? = null

    var isTechOpen = false
    val technologies = TechnologyTree(this)

    /**
     * Ресурсы игрока
     */
    val resource = mutableMapOf<ResourceType, Int>().apply {
        ResourceType.values().forEach { this[it] = 500 }
    }

    fun changeResource(r: ResourceType, value: Int) {
        resource[r] = resource[r]!! + value
    }

    /**
     * Цвет игрока, отражается на юнитах и зданиях
     */
    var color: Color = Color.RED

    fun mousePressed(ev: MouseEvent) {

    }
    /**
     * Обработка нажатий клавиш мыши
     */
    fun mouseClicked(ev: MouseEvent) {
        var doSomething = false
        when (ev.button) {
            BUTTON1 -> {
                if (isTechOpen) {
                    doSomething = true
                    technologies.mouseClicked(ev)
                }
                val pos = G.map.selectedCellPos
                val u = G.map[pos].unit
                val b = G.map[pos].build
                if (own(u) && own(b)) {
                    if (selectedUnit == u) {
                        selectedBuild = b
                    } else {
                        selectedUnit = u
                    }
                    doSomething = true
                } else if (own(u)) {
                    selectedUnit = u
                    doSomething = true
                } else if (own(b)) {
                    selectedBuild = b
                    doSomething = true
                }
            }
            BUTTON3 -> {
                tmpPath = null
            }
        }
        if (!doSomething) {
            selectedEntity?.mouseClicked(ev)
        }
    }

    fun mouseMoved(ev: MouseEvent) {
        selectedEntity?.mouseMoved(ev)
    }

    fun keyClicked(ev: KeyEvent) {
        when (ev.keyCode) {
            VK_Q -> selectedEntity = null
            VK_T -> isTechOpen = !isTechOpen
        }
        selectedEntity?.keyClicked(ev)
    }

    fun paint(g: Graphics) {
        if (isTechOpen) {
            technologies.paint(g)
        }
    }

    /**
     * Вызывается при завершении игроком хода
     */
    fun endTurn(): Boolean {
        for (e in entities) {
            if (!e.isTurnEnded) {
                e.isTurnEnded = true
                if (e.endTurn()) {
                    G.map.centerOn(e)
                    return true
                }
            }
        }
        return false
    }

    /**
     * Вызывается при начале игроком хода
     */
    fun newTurn() {
        entities.forEach {
            it.isTurnEnded = false
            it.newTurn()
        }
    }

    /**
     * Добавить юнита игроку
     * @param newUnit юнит, который будет добавлен игроку
     */
    fun addUnit(newUnit: BaseUnit) {
        G.map[newUnit.pos].unit = newUnit
        units.add(newUnit)
        //updateInvestigatedArea(newUnit.observableArea)
        newUnit.updateOwnerInvestigatedArea()
        updateObservableArea()
    }

    /**
     * Удалить юнита у игрока
     * @param unit юнит, который будет удалён
     */
    fun removeUnit(unit: BaseUnit) {
        G.map[unit.pos].unit = null
        units.removeIf { it == unit }
        if (selectedUnit == unit) {
            selectedUnit = null
        }
    }

    /**
     * Добавить здание игроку
     * @param newBuild здание, который будет добавлен игроку
     */
    fun addBuild(newBuild: BaseBuild) {
        G.map[newBuild.pos].build = newBuild
        builds.add((newBuild))
        newBuild.updateOwnerInvestigatedArea()
        updateObservableArea()
    }

    /**
     * Удалить здание у игрока
     * @param build здание, которое будет удалено
     */
    fun removeBuild(build: BaseBuild) {
        G.map[build.pos].build = null
        builds.removeIf { it == build }
        if (selectedBuild == build) {
            selectedBuild = null
        }
    }

    /**
     * Проверяет, может ли персонаж заплатить указанную цену
     * @param cost стоимость
     */
    fun canPay(cost: Map<ResourceType, Int>) =
        cost.all { resource[it.key]!! >= it.value }

    /**
     * Проверяет, может ли персонаж заплатить указанную цену, и если да,
     * то отнимает соответствующее количество ресурсов
     * @param cost стоимость
     */
    fun pay(cost: Map<ResourceType, Int>): Boolean {
        if (canPay(cost)) {
            cost.forEach { changeResource(it.key, -it.value) }
            return true
        }
        return false
    }

    /**
     * Проверяет, принадлежит ли данная сущность игроку
     * @param entity сущность, принадлежность которой проверяется, или является null
     * @return true, если принадлежит, иначе false
     */
    infix fun own(entity: BaseEntity?) =
        if (entity == null) false else entity.owner == this

    /**
     * Содержит информацию об исследованной игроком территории, каждая клетка
     * может иметь значение либо game.ObservableStatus.NotInvestigated либо
     * game.ObservableStatus.Investigated
     */
    var investigatedArea =
        makeMatrix(G.map.size) { ObservableStatus.NotInvestigated }

    /**
     * Возвращает данные о том, какие клетки видит игрок
     */
    val observableArea = makeMatrix(G.map.size) { ObservableStatus.NotInvestigated }

    /**
     * Обновить наблюдаемую игроком зону
     */
    fun updateObservableArea() {
        investigatedArea.matrixForEachIndexed { pos, cellStatus ->
            observableArea[pos] = cellStatus
        }
        entities.forEach { entity ->
            entity.updateOwnerObservableArea()
        }
    }

    override operator fun equals(other: Any?) = name == (other as Player).name
    override fun hashCode() = name.hashCode()
}