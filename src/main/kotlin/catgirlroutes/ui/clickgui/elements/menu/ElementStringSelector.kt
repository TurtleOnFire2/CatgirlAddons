package catgirlroutes.ui.clickgui.elements.menu

import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.ui.clickgui.elements.Element
import catgirlroutes.ui.clickgui.elements.ElementType
import catgirlroutes.ui.clickgui.elements.ModuleButton
import catgirlroutes.ui.clickgui.util.ColorUtil
import catgirlroutes.ui.clickgui.util.FontUtil
import net.minecraft.client.gui.Gui
import java.util.*

/**
 * Provides a selector element.
 *
 * @author Aton
 */
class ElementStringSelector(parent: ModuleButton, setting: StringSelectorSetting) :
    Element<StringSelectorSetting>(parent, setting, ElementType.SELECTOR) {

    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        val displayValue = setting.selected

        // Render the text.
        if (FontUtil.getStringWidth(displayValue + "00" + displayName) <= width) {
            FontUtil.drawString(displayName, 1, 2)
            FontUtil.drawString(displayValue, width - FontUtil.getStringWidth(displayValue), 2)
        } else {
            if (isButtonHovered(mouseX, mouseY)) {
                FontUtil.drawCenteredStringWithShadow(displayValue, width / 2.0, 2.0)
            } else {
                FontUtil.drawCenteredString(displayName, width / 2.0, 2.0)
            }
        }

        // Render the tab indicating the drop-down
        Gui.drawRect(0, 13, width, 15, ColorUtil.tabColorBg)
        Gui.drawRect((width * 0.4).toInt(), 12, (width * 0.6).toInt(), 15, ColorUtil.tabColor)


        // Render the dropdown
        if (extended) {
            var ay = DEFAULT_HEIGHT
            val increment = FontUtil.fontHeight + 2
            for (option in setting.options) {
                Gui.drawRect(0, ay, width, ay + increment, ColorUtil.dropDownColor)
                val elementtitle =
                    option.substring(0, 1).uppercase(Locale.getDefault()) + option.substring(1, option.length)
                FontUtil.drawCenteredString(elementtitle, width / 2.0, ay + 2.0)

                /** Highlight the element if it is selected */
                if (setting.isSelected(option)) {
                    Gui.drawRect(0, ay, 2, ay + increment, ColorUtil.clickGUIColor.rgb)
                }
                /** Highlight the element when it is hovered */
                if (mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute + ay && mouseY < yAbsolute + ay + increment) {
                    Gui.drawRect(width - 1, ay, width, ay + increment, ColorUtil.clickGUIColor.rgb)
                }
                ay += increment
            }
        }

        return super.renderElement(mouseX, mouseY, partialTicks)
    }

    /**
     * Handles interaction with this element.
     * Returns true if interacted with the element to cancel further interactions.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (isButtonHovered(mouseX, mouseY)) {
                setting.index += 1
                return true
            }

            if (!extended) return false
            var ay = DEFAULT_HEIGHT
            val increment = FontUtil.fontHeight + 2
            for (option in setting.options) {
                if (mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute + ay && mouseY <= yAbsolute + ay + increment) {
                    setting.selected = option
                    return true
                }
                ay += increment
            }
        } else if( mouseButton == 1) {
            if (isButtonHovered(mouseX, mouseY)) {
                extended = !extended
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Checks whether the mouse is hovering the selector
     */
    private fun isButtonHovered(mouseX: Int, mouseY: Int): Boolean {
        return (mouseX >= xAbsolute && mouseX <= xAbsolute + width && mouseY >= yAbsolute && mouseY <= yAbsolute + DEFAULT_HEIGHT)
    }
}