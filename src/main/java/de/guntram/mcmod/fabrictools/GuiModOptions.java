package de.guntram.mcmod.fabrictools;

import com.mojang.blaze3d.platform.GlStateManager;
import de.guntram.mcmod.fabrictools.ConfigChangedEvent.OnConfigChangingEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GuiModOptions extends Screen implements Supplier<Screen> {
    
    private final Screen parent;
    private final String modName;
    private final ModConfigurationHandler handler;
    private final List<String> options;
    private final Logger LOGGER;
    
    private String screenTitle;
    
    private static final int LINEHEIGHT = 25;
    private static final int BUTTONHEIGHT = 20;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiModOptions(Screen parent, String modName, ModConfigurationHandler confHandler) {
        super(new LiteralText(modName));
        this.parent=parent;
        this.modName=modName;
        this.handler=confHandler;
        this.screenTitle=modName+" Configuration";
        this.options=handler.getConfig().getKeys();
        this.LOGGER=LogManager.getLogger();
    }
    
    @Override
    protected void init() {
        this.addButton(new AbstractButtonWidget(this.width / 2 - 100, this.height - 27, 200, 20, I18n.translate("gui.done")) {
            @Override
            public void onClick(double x, double y) {
                for (AbstractButtonWidget button: buttons) {
                    if (button instanceof TextFieldWidget) {
                        if (button.isFocused()) {
                            button.changeFocus(false);
                        }
                    }
                }
                handler.onConfigChanged(new ConfigChangedEvent.OnConfigChangedEvent(modName));
                client.openScreen(parent);
            }
        });
        
        int y=50-LINEHEIGHT;
        for (String text: options) {
            y+=LINEHEIGHT;
            Object value = handler.getConfig().getValue(text);
            AbstractButtonWidget element;
            if (value == null) {
                LogManager.getLogger().warn("value null, adding nothing");
                continue;
            } else if (value instanceof Boolean) {
                element = this.addButton(new AbstractButtonWidget(this.width/2+10, y, 200, BUTTONHEIGHT, ((Boolean) value == true ? "§2true" : "§4false")) {
                    @Override
                    public void onClick(double x, double y) {
                        if ((Boolean)(handler.getConfig().getValue(text))==true) {
                            handler.getConfig().setValue(text, false);
                            handler.onConfigChanging(new OnConfigChangingEvent(modName, text, false));
                        } else {
                            handler.getConfig().setValue(text, true);
                            handler.onConfigChanging(new OnConfigChangingEvent(modName, text, true));
                        }
                        this.changeFocus(true);
                    }
                    @Override
                    public void onFocusedChanged(boolean b) {
                        this.setMessage((Boolean) handler.getConfig().getValue(text) == true ? "§2true" : "§4false");
                        super.onFocusedChanged(b);
                    }
                });
            } else if (value instanceof String) {
                element=this.addButton(new TextFieldWidget(this.textRenderer, this.width/2+10, y, 200, BUTTONHEIGHT, (String) value) {
                    @Override
                    public void onFocusedChanged(boolean b) {
                        if (b) {
                            // LOGGER.info("value to textfield");
                            this.setText((String) handler.getConfig().getValue(text));
                        } else {
                            // LOGGER.info("textfield to value");
                            handler.getConfig().setValue(text, this.getText());
                        }
                        super.onFocusedChanged(b);
                    }
                    @Override
                    public boolean charTyped(char chr, int keyCode) {
                        boolean result = super.charTyped(chr, keyCode);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, text, this.getText()));
                        return result;
                    }

                    @Override
                    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                        boolean result = super.keyPressed(keyCode, scanCode, modifiers);
                        handler.onConfigChanging(new OnConfigChangingEvent(modName, text, this.getText()));
                        return result;
                    }
                    
                });
                element.changeFocus(false);
            } else if (value instanceof Integer || value instanceof Float || value instanceof Double) {
                element=this.addButton(new GuiSlider(this.width/2+10, y, handler.getConfig(), text));
            } else {
                LogManager.getLogger().warn(modName +" has option "+text+" with data type "+value.getClass().getName());
                continue;
            }
            this.addButton(new AbstractButtonWidget(this.width/2+220, y, BUTTONHEIGHT, BUTTONHEIGHT, "") {
                @Override
                public void onClick(double x, double y) {
                    handler.getConfig().setValue(text, handler.getConfig().getDefault(text));
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, text, handler.getConfig().getDefault(text)));
                    element.changeFocus(false);
                }
            });
        }
    }
    
    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        renderBackground();
        drawCenteredString(textRenderer, screenTitle, this.width/2, BUTTONHEIGHT, 0xffffff);
        super.render(mouseX, mouseY, partialTicks);
        
        int y=50;
        for (String text: options) {
            drawString(textRenderer, text, this.width / 2 -155, y+2, 0xffffff);
            y+=LINEHEIGHT;
        }

        y=50;
        for (String text: options) {
            if (mouseX>this.width/2-155 && mouseX<this.width/2 && mouseY>y && mouseY<y+BUTTONHEIGHT) {
                String tooltip=handler.getConfig().getTooltip(text);
                if (tooltip==null)
                    tooltip="missing tooltip";
                if (tooltip.length()<=30) {
                    renderTextHoverEffect(new LiteralText(handler.getConfig().getTooltip(text)), mouseX, mouseY);
                } else {
                    List<String>lines=new ArrayList<>();
                    int pos=0;
                    while (pos<tooltip.length()-30) {
                        int curlen=30;
                        while (tooltip.charAt(pos+curlen)!=' ' && curlen>10) {
                            curlen--;
                        }
                        lines.add(tooltip.substring(pos, pos+curlen));
                        pos+=curlen+1;
                    }
                    lines.add(tooltip.substring(pos));
                    renderTooltip(lines, mouseX, mouseY);
                }
            }
            y+=LINEHEIGHT;
        }
    }

    @Override
    public Screen get() {
        return this;
    }
    
    private enum Type {INT, FLOAT, DOUBLE;}
    
    private class GuiSlider extends AbstractButtonWidget {
        Type type;
        boolean dragging;
        double sliderValue, min, max;
        Configuration config;
        String configOption;
        
        @SuppressWarnings("OverridableMethodCallInConstructor")
        GuiSlider(int x, int y, Configuration config, String option) {
            super(x, y, 200, BUTTONHEIGHT, "?");
            Object value=config.getValue(option);
            if (value instanceof Double) {
                this.setMessage(Double.toString((Double)value));
                this.min=(Double)config.getMin(option);
                this.max=(Double)config.getMax(option);
                sliderValue=((Double)value-min)/(max-min);
                type=Type.DOUBLE;
            }
            else if (value instanceof Float) {
                this.setMessage(Float.toString((Float)value));
                this.min=(Float)config.getMin(option);
                this.max=(Float)config.getMax(option);
                sliderValue=((Float)value-min)/(max-min);
                type=Type.FLOAT;
            } else {
                this.setMessage(Integer.toString((Integer)value));
                this.min=(Integer)config.getMin(option);
                this.max=(Integer)config.getMax(option);
                sliderValue=((Integer)value-min)/(max-min);
                type=Type.INT;
            }

            this.config=config;
            this.configOption=option;
        }
        
        private void updateValue(double value) {
            switch (type) {
                case DOUBLE:
                    double doubleVal=value*(max-min)+min;
                    this.setMessage(Double.toString(doubleVal));
                    this.config.setValue(configOption, (Double) doubleVal);
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, configOption, doubleVal));
                    break;
                case FLOAT:
                    float floatVal=(float) (value*(max-min)+min);
                    this.setMessage(Float.toString(floatVal));
                    this.config.setValue(configOption, (Float) floatVal);
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, configOption, floatVal));
                    break;
                case INT:
                    int intVal=(int) (value*(max-min)+min);
                    this.setMessage(Integer.toString(intVal));
                    this.config.setValue(configOption, (Integer) intVal);
                    handler.onConfigChanging(new OnConfigChangingEvent(modName, configOption, intVal));
                    break;
            }
        }

        @Override
        protected void renderBg(MinecraftClient mc, int mouseX, int mouseY)
        {
            if (this.visible)
            {
                if (this.dragging)
                {
                    this.sliderValue = (double)((float)(mouseX - (this.x + 4)) / (float)(this.width - 8));
                    this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
                    updateValue(this.sliderValue);
                }
                mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                this.drawTexture(this.x + (int)(this.sliderValue * (double)(this.width - 8)), this.y, 0, 66, 4, 20);
                this.drawTexture(this.x + (int)(this.sliderValue * (double)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);
            }
        }

        /**
         * Called when the left mouse button is pressed over this button. This method is specific to AbstractButtonWidget.
         */
        @Override
        public final void onClick(double mouseX, double mouseY)
        {
            this.sliderValue = (mouseX - (double)(this.x + 4)) / (double)(this.width - 8);
            this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
            updateValue(sliderValue);
            this.dragging = true;
        }

        /**
         * Called when the left mouse button is released. This method is specific to AbstractButtonWidget.
         */
        @Override
        public void onRelease(double mouseX, double mouseY)
        {
            this.dragging = false;
        }
        
        @Override
        public void onFocusedChanged(boolean b) {
            Object value=config.getValue(configOption);
            if (value instanceof Double) {
                this.setMessage(Double.toString((Double)value));
                sliderValue=((Double)value-min)/(max-min);
            }
            else if (value instanceof Float) {
                this.setMessage(Float.toString((Float)value));
                sliderValue=((Float)value-min)/(max-min);
            } else {
                this.setMessage(Integer.toString((Integer)value));
                sliderValue=((Integer)value-min)/(max-min);
            }
            super.onFocusedChanged(b);
        }
    }
}
