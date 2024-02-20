/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.mcmod.fabrictools;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;

/**
 *
 * @author gbl
 */
public class ConfigurableModList extends Screen {

    private final Screen parent;
    
    public ConfigurableModList(Screen parent) {
        super(new LiteralText(I18n.translate("mod.options", new Object[0])));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        int y = 10;
        int x = 0;
        int size = this.width / 4 - 20;

        for (String modName: ConfigurationProvider.getRegisteredMods()) {
            this.addButton(new AbstractButtonWidget(x+10, y, size, 20, modName) {
                @Override
                public void onClick(double x, double y) {
                    ModConfigurationHandler handler = ConfigurationProvider.getHandler(this.getMessage());
                    if (handler != null) {
                        MinecraftClient.getInstance().openScreen(
                                new GuiModOptions(parent, this.getMessage(), handler)
                        );
                    }
                }
            });
            x += this.width / 4;
            if (x > this.width) {
                x = this.width;
                y += 24;
            }
        }
    }
    
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        super.render(mouseX, mouseY, partialTicks);
    }
}
