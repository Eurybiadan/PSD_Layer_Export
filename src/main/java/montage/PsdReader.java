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

import com.twelvemonkeys.imageio.plugins.psd.PSDMetadata;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 *
 * @author Robert F Cooper <rfcooper@sas.upenn.edu>
 */
public class PsdReader {
    public static final String GRAY = "grayscale";
    public static final String RGB = "rgb";
    public static final String CMYK = "cmyk";
    

    public PsdData read(File imageFile) throws IOException{
        return this.read(imageFile, new ResourceReadProgressListener(1));
    }

    public PsdData read(File imageFile, IIOReadProgressListener readProgressListener) throws IOException{
        boolean loadgroups = true;

        try (ImageInputStream input = ImageIO.createImageInputStream(imageFile)){
            
            Iterator<ImageReader> readerList = ImageIO.getImageReaders(input);
            
            if( !readerList.hasNext() ){
                LogManager.getLogger().error("Failed to load PSD: "+imageFile.getName()+"! No PSD reader detected.");
                throw new IOException("Failed to load PSD: "+imageFile.getName()+"! No PSD reader detected.");
            }
            
            ImageReader psdReader = readerList.next();
            
            psdReader.setInput(input);
            
            int numLayers = psdReader.getNumImages(false)-1;
            
            // Don't read the background's metadata, because it will be totally wrong for the other layers!
            PSDMetadata layermetadata = (PSDMetadata)psdReader.getImageMetadata(numLayers);


            IIOMetadataNode root = (IIOMetadataNode) layermetadata.getAsTree(layermetadata.getNativeMetadataFormatName());

            // Get basic information about the file.
            NamedNodeMap rootmap = root.getFirstChild().getAttributes();

            NodeList imResources = root.getElementsByTagName("ImageResources");
            IIOMetadataNode resourceNode = (IIOMetadataNode)imResources.item(0);
            int numResources = resourceNode.getLength();

            // Determine if the psd file has an embedded ICC Profile. If not, attempt to provide one later on.
            IIOMetadataNode iccNode = (IIOMetadataNode)resourceNode.getFirstChild();
            boolean hasProfile = false;
            for (int i = 0; i < numResources; i++) {
                if( iccNode.getNodeName().equals("ICCProfile") ){ // If we have this resource, then the psd was color managed.
                    hasProfile = true;
                    break;
                }else if( iccNode.getAttributes().item(0).getNodeValue().equals("IccUntaggedProfile") ) { // If we have this resource, then that means the psd isn't color managed.
                    resourceNode.removeChild(iccNode);
                    break;
                }else{ // Still haven't found anything yet, carry on...
                    iccNode = (IIOMetadataNode)iccNode.getNextSibling();
                }
            }

            Dimension canvasSize = new Dimension();
            int bits = 0;
            int inttype = 0;

            ComponentColorModel grayColorModel = null;
            ICC_ColorSpace gamma2p2 = null;
            String type = "";
            for (int i = 0; i < rootmap.getLength(); i++) {
                switch( rootmap.item(i).getNodeName() ){
                    case "height":
                        canvasSize.height = Integer.valueOf( rootmap.item(i).getNodeValue() );
                        break;
                    case "width":
                        canvasSize.width = Integer.valueOf( rootmap.item(i).getNodeValue() );
                        break;
                    case "bits":
                        bits = Integer.parseInt(rootmap.item(i).getNodeValue() ); //+ "-bit ";
                        break;
                    case "mode":
                        type = rootmap.item(i).getNodeValue().toLowerCase();
                        
                        switch(type.toLowerCase()){
                            case GRAY:
                                inttype = ColorSpace.CS_GRAY;
                                if(!hasProfile){
                                    try (InputStream inStream = PsdReader.class.getResourceAsStream("/icc_profiles/Gamma22.icc") ){
                                        ICC_Profile gamma2p2GRAY = ICC_Profile.getInstance(inStream);
                                        gamma2p2 = new ICC_ColorSpace(gamma2p2GRAY);
                                        grayColorModel = new ComponentColorModel(gamma2p2,
                                                                                true, false,
                                                                                ComponentColorModel.TRANSLUCENT,
                                                                                DataBuffer.TYPE_BYTE);
                                        Platform.runLater(() -> {
                                            Alert alert = new Alert(Alert.AlertType.WARNING, "This PSD does not have an attached ICC Profile. Defaulting to a Gamma 2.2 profile. If the image does not look correct, please embed the ICC profile you wish to use.");
                                            alert.showAndWait();
                                        });
                                    }
                                }
                                break;
                            case RGB:
                            case CMYK: 
                                inttype = ColorSpace.CS_sRGB;
                                break;
                            default:
                                inttype = ColorSpace.CS_LINEAR_RGB;
                                break;
                        }
                        
                        break;
                        
                }
            }

//            ((ICC_ProfileGray)iccNode.getUserObject()).write("Gamma22.icc");

            PsdData montage = new PsdData( imageFile.getName(), canvasSize, ColorSpace.getInstance(inttype), bits );

            NodeList nL = root.getElementsByTagName("Layers");
                                    
            Node n = nL.item(0);
            
            int imageIndex = numLayers;

            n = n.getLastChild();
            PsdGroup currentGroup = (PsdGroup)montage.getRoot();

            do{
                // Set up the read progress listening if it exists
                if( readProgressListener != null ){
                    readProgressListener.imageProgress(psdReader, 100.0f*(numLayers-imageIndex)/numLayers);
                }
                
                NamedNodeMap map = n.getAttributes();
                String layerName = "";
                
                float opacity = 1.0f;
                short flags = 0;
                Rectangle layerBounds = new Rectangle();
                boolean isImage = true;
                
                for (int i = 0; i < map.getLength(); i++) {
                    switch( map.item(i).getNodeName() ){
                        case "name":
                            layerName = map.item(i).getNodeValue();
                            break;
                        case "top":
                            layerBounds.y = Integer.valueOf( map.item(i).getNodeValue() );
                            break;
                        case "left":
                            layerBounds.x = Integer.valueOf( map.item(i).getNodeValue() );
                            break;
                        case "bottom":
                            layerBounds.height = Integer.valueOf( map.item(i).getNodeValue() );
                            break;
                        case "right":                            
                            layerBounds.width = Integer.valueOf( map.item(i).getNodeValue() );
                            break;
                        case "opacity":
                            opacity = Integer.valueOf( map.item(i).getNodeValue() )/255.0f;
                            break;
                        case "flags":
                            flags = Short.valueOf( map.item(i).getNodeValue() );
                            break;
                        case "pixelDataIrrelevant":
                            isImage = false;
                            break;
                    }

                }


                // If the pixel data isn't irrelevant, then read this in as a layer
                if( isImage ){
                    if( flags != 9 ){ // If the flag is 9, then it has protected transparency, and we don't want their kind here.
                        // First determine the actual bounds (since height holds bottom and width holds right)
                        layerBounds.height = layerBounds.height-layerBounds.y;
                        layerBounds.width  = layerBounds.width-layerBounds.x;

                        BufferedImage copyIm = psdReader.read(imageIndex);
                        if(!hasProfile) {
                            copyIm = new BufferedImage(grayColorModel, copyIm.getRaster(), grayColorModel.isAlphaPremultiplied(), null);
                        }

                        if( loadgroups ){
                            montage.addLayer( currentGroup, new PsdLayer(copyIm, layerName, layerBounds, opacity) );
                        }else{
                            montage.addLayer( (PsdGroup)montage.getRoot(), new PsdLayer(copyIm, layerName, layerBounds, opacity) );
                        }
                    }                    
                }else{ // If the pixel data is irrelevant, then read this in as a group or shape
                    
                    if( layerName.equals("</Layer group>") ){
                        if( loadgroups ){
                            currentGroup = currentGroup.getParent();
                        }
                    }else if( layerBounds.isEmpty() ){
                        if( loadgroups ){
                            PsdGroup tmpGroup = new PsdGroup(layerName);
                            currentGroup.addNode( tmpGroup );
                            currentGroup = tmpGroup;
                        }
                    }else{
                        // Determine the actual bounds of the shape (since height holds bottom and width holds right)
                        layerBounds.height = layerBounds.height-layerBounds.y;
                        layerBounds.width  = layerBounds.width-layerBounds.x;
                        
                        currentGroup.addNode( new PsdLayerNode(new PsdLayer(psdReader.read(imageIndex), layerName, layerBounds, opacity)) );
                    }
                }
                
                imageIndex--;
                n = n.getPreviousSibling();
                
            } while( n != null );

            montage.revalidate();
            System.gc();
            
            if( readProgressListener != null ){
                readProgressListener.imageProgress(psdReader, 100.0f);
            }
            
//            montage.printTreeToScreen(montage.root, 0);
            
            return montage;
            
        } catch (IOException ex) {
            LogManager.getLogger().error("Failed to load PSD: "+imageFile.getName()+"!", ex);
            throw new IOException("Failed to load PSD: "+imageFile.getName()+"!", ex);
        }
    }   
}
