/* 
 * Copyright 2016 Robert F Cooper <rfcooper@sas.upenn.edu>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package montage;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 *
 * @author Robert F Cooper <rfcooper@sas.upenn.edu>
 */
public class PsdLayer implements Shape {
    
    // Layer properties
    public String name = null;
    public Rectangle bounds = null;
    public float opacity = 255;
    
    // Layer contents
    private BufferedImage image;
    private Image thumbnail;
    private final int THUMBSIZE = 48;
    
    public PsdLayer(BufferedImage layerImage, String layerName, Rectangle layerBounds ){
        this(layerImage, layerName, layerBounds, 255);
    }
    
    public PsdLayer(BufferedImage layerImage, String layerName, Rectangle layerBounds, float opacity ){
        image = layerImage;
        
        thumbnail = new BufferedImage( THUMBSIZE, THUMBSIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D)thumbnail.getGraphics();
        g2d.drawImage(image, 0, 0, THUMBSIZE, THUMBSIZE, null);
        g2d.dispose();
        
        name = layerName;
        bounds = layerBounds;
        this.opacity = opacity;
    }
    
    public Image getThumbnail(){
        return thumbnail;
    }

    public BufferedImage getLayerImage(){
        return image;
    }
    
    public double getLayerOpacity(){
        return opacity;
    }

    @Override
    public Rectangle getBounds() {
        return bounds.getBounds();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return bounds.getBounds2D();
    }

    @Override
    public boolean contains(double x, double y) {
        return bounds.contains(x, y);
    }

    @Override
    public boolean contains(Point2D p) {
        return bounds.contains(p);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return bounds.intersects(x, y, w, h);
    }

    @Override
    public boolean intersects(Rectangle2D r) {
        return bounds.intersects(r);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return bounds.contains(x, y, w, h);
    }

    @Override
    public boolean contains(Rectangle2D r) {
        return bounds.contains(r);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return bounds.getPathIterator(at);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return bounds.getPathIterator(at,flatness);
    }
    
    @Override
    public boolean equals(Object o){
        
        // If it is a PsdLayer,
        if( o instanceof PsdLayer){
            
            // if the object reference is the same, then return equal.
            if( this.hashCode() == o.hashCode() ){
                return true;
            }
            
            // or if the name is the same,
            return name.equals(o.toString());                
            
        }else{
            return false;
        }
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Objects.hashCode(this.bounds);        
        hash = 23 * hash + Objects.hashCode(this.image);
        return hash;
    }

}
