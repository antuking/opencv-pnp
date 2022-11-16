package io.kyzu.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum ImageResource { SOURCE, TEMPLATE }

public class Highlighter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private File source;
    private BufferedImage bSource;
    private Rectangle template;
    private int occurence;

    public Highlighter(File source, Rectangle template) {
        this.source = source;
        this.template = template;
        this.occurence = 1;
    }

    public Highlighter(BufferedImage bSource, Rectangle template) {
        this.bSource = bSource;
        this.template = template;
        this.occurence = 2;
    }

    public void drawBorder() {
        BufferedImage source = (BufferedImage) getImageResource().get(ImageResource.SOURCE);
        Rectangle template = (Rectangle) getImageResource().get(ImageResource.TEMPLATE);

        Graphics2D graphics = source.createGraphics();
        // draw rectangle highlight
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(6f));
        graphics.drawRect(template.x, template.y, template.width, template.height);

        try {
            ImageIO.write(source, "png", new File("debug.png"));
        } catch (IOException ex) {
            LOGGER.error("Cannot write highlighted image", ex);
        }
    }

    public void fillArea() {
        BufferedImage source = (BufferedImage) getImageResource().get(ImageResource.SOURCE);
        Rectangle template = (Rectangle) getImageResource().get(ImageResource.TEMPLATE);

        Graphics2D graphics = source.createGraphics();
        int alphaTransparent = 70;
        Color color = new Color(255, 0, 0, alphaTransparent);
        graphics.setColor(color);
        graphics.fillRect(template.x, template.y, template.width, template.height);

        try {
            ImageIO.write(source, "png", new File("debug.png"));
        } catch (IOException ex) {
            LOGGER.error("Cannot write highlighted image", ex);
        }
    }

    private Map<ImageResource, Object> getImageResource() {
        Map<ImageResource, Object> dataImg = new HashMap<>();
        try {
            switch (occurence) {
                case 1:
                    dataImg.put(ImageResource.SOURCE, ImageIO.read(source));
                    dataImg.put(ImageResource.TEMPLATE, template);
                    break;

                case 2:
                    dataImg.put(ImageResource.SOURCE, bSource);
                    dataImg.put(ImageResource.TEMPLATE, template);
                    break;

                default:
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return dataImg;
    }
}
