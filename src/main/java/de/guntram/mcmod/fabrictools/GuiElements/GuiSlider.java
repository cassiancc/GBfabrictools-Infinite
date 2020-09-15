package de.guntram.mcmod.fabrictools.GuiElements;

import com.mojang.blaze3d.platform.GlStateManager;
import de.guntram.mcmod.fabrictools.Types.SliderValueConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.AbstractButtonWidget;
import static net.minecraft.client.gui.widget.AbstractButtonWidget.WIDGETS_LOCATION;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.MathHelper;
import de.guntram.mcmod.fabrictools.IConfiguration;

public class GuiSlider extends AbstractButtonWidget {
    
    private enum Type {INT, FLOAT, DOUBLE;}
    
    Type type;
    boolean dragging;
    double sliderValue, min, max;
    String configOption;
    SliderValueConsumer parent;
    
    @SuppressWarnings("OverridableMethodCallInConstructor")
    public GuiSlider(SliderValueConsumer optionScreen, int x, int y, int width, int height, IConfiguration config, String option) {
        super(x, y, width, height, new LiteralText("?"));
        Object value=config.getValue(option);
        if (value instanceof Double) {
            this.setMessage(new LiteralText(Double.toString((Double)value)));
            this.min=(Double)config.getMin(option);
            this.max=(Double)config.getMax(option);
            sliderValue=((Double)value-min)/(max-min);
            type=Type.DOUBLE;
        }
        else if (value instanceof Float) {
            this.setMessage(new LiteralText(Float.toString((Float)value)));
            this.min=(Float)config.getMin(option);
            this.max=(Float)config.getMax(option);
            sliderValue=((Float)value-min)/(max-min);
            type=Type.FLOAT;
        } else {
            this.setMessage(new LiteralText(Integer.toString((Integer)value)));
            this.min=(Integer)config.getMin(option);
            this.max=(Integer)config.getMax(option);
            sliderValue=((Integer)value-min)/(max-min);
            type=Type.INT;
        }

        this.configOption=option;
        this.parent = optionScreen;
        optionScreen.setMouseReleased(false);
    }
    
    public GuiSlider(SliderValueConsumer optionScreen, int x, int y, int width, int height, int val, int min, int max, String option) {
        super(x, y, width, height, new LiteralText("?"));
        this.setMessage(new LiteralText(""+val));
        this.min = min;
        this.max = max;
        this.sliderValue=(val-min)/(max-min);
        this.type = Type.INT;
        this.configOption = option;
        this.parent = optionScreen;
    }

    private void updateValue(double value) {
        switch (type) {
            case DOUBLE:
                double doubleVal=value*(max-min)+min;
                this.setMessage(new LiteralText(Double.toString(doubleVal)));
                parent.onConfigChanging(configOption, doubleVal);
                break;
            case FLOAT:
                float floatVal=(float) (value*(max-min)+min);
                this.setMessage(new LiteralText(Float.toString(floatVal)));
                parent.onConfigChanging(configOption, floatVal);
                break;
            case INT:
                int intVal=(int) (value*(max-min)+min);
                this.setMessage(new LiteralText(Integer.toString(intVal)));
                parent.onConfigChanging(configOption, intVal);
                break;
        }
    }

    @Override
    protected void renderBg(MatrixStack stack, MinecraftClient mc, int mouseX, int mouseY)
    {
        if (this.visible)
        {
            if (this.dragging)
            {
                this.sliderValue = (double)((float)(mouseX - (this.x + 4)) / (float)(this.width - 8));
                this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
                updateValue(this.sliderValue);
                if (parent.wasMouseReleased()) {
                    this.dragging = false;
                }
            }
            mc.getTextureManager().bindTexture(WIDGETS_LOCATION);
            GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.drawTexture(stack, this.x + (int)(this.sliderValue * (double)(this.width - 8)), this.y, 0, 66, 4, 20);
            this.drawTexture(stack, this.x + (int)(this.sliderValue * (double)(this.width - 8)) + 4, this.y, 196, 66, 4, 20);
        }
    }

    /*
     * Called when the left mouse button is pressed over this button. This method is specific to AbstractButtonWidget.
     */
    @Override
    public final void onClick(double mouseX, double mouseY)
    {
        this.sliderValue = (mouseX - (double)(this.x + 4)) / (double)(this.width - 8);
        this.sliderValue = MathHelper.clamp(this.sliderValue, 0.0D, 1.0D);
        updateValue(sliderValue);
        this.dragging = true;
        parent.setMouseReleased(false);
    }

    /*
     * Called when the left mouse button is released. This method is specific to AbstractButtonWidget.
     */
    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        this.dragging = false;
    }

    @Override
    public void onFocusedChanged(boolean b) {
        System.out.println("onFocusChanged; do we miss something?");
/*        
        if (type == Type.DOUBLE) {
            this.setMessage(new LiteralText(Double.toString((Double)value)));
            sliderValue=((Double)value-min)/(max-min);
        }
        else if (type == Type.FLOAT) {
            this.setMessage(new LiteralText(Float.toString((Float)value)));
            sliderValue=((Float)value-min)/(max-min);
        } else {
            this.setMessage(new LiteralText(Integer.toString((Integer)value)));
            sliderValue=((Integer)value-min)/(max-min);
        }
*/
        super.onFocusedChanged(b);
    }
}
