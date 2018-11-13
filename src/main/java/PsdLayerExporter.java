import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import montage.PsdData;
import montage.PsdLayer;
import montage.PsdReader;
import montage.ResourceReadProgressListener;
import org.apache.logging.log4j.LogManager;
import sun.rmi.runtime.Log;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class PsdLayerExporter extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        String suffix = "";
        String fileType = "tif";


        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Photoshop File for Export");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Photoshop files", "*.psd"));
        File psdToLoad = fileChooser.showOpenDialog(primaryStage);

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Select Export Folder");
        dirChooser.setInitialDirectory(psdToLoad.getParentFile());
        File exportFolder = dirChooser.showDialog(primaryStage);


        ResourceReadProgressListener progBar = new ResourceReadProgressListener(2);

        PsdReader psdReader = new PsdReader();

        try {
            PsdData data = psdReader.read(psdToLoad, progBar);

            progBar.setFileProgress(1);
            float progAmt = 0;

            for(PsdLayer layer : data.getMontageLayers()) {

                String layerName = layer.name;
                int extloc = layerName.lastIndexOf(".");
                if( extloc != -1 ) {
                    layerName = layerName.substring(0, extloc);
                }
                LogManager.getLogger().info("Writing " + layerName +"."+ fileType +"("+(100f*progAmt/data.getNumLayers())+"%)");

                File outFile = new File(exportFolder.getAbsolutePath()+"\\"+layerName+"."+fileType );
                ImageTypeSpecifier imSpec = new ImageTypeSpecifier( layer.getLayerImage().getColorModel(),
                        layer.getLayerImage().getSampleModel() );

                try (ImageOutputStream output = ImageIO.createImageOutputStream(outFile)){

                    Iterator<ImageWriter> writerList = ImageIO.getImageWriters(imSpec, fileType);

                    if( !writerList.hasNext() ){
                        LogManager.getLogger().error("Failed to write file: "+layerName+"."+fileType+"! No "+ fileType + " writer detected.");
                        throw new IOException("Failed to load PSD: "+layerName+"."+fileType+"! No "+ fileType +" writer detected.");
                    }

                    ImageWriter psdWriter = writerList.next();

                    psdWriter.setOutput(output);

                    //Add option here.
                    BufferedImage inSitu = imSpec.createBufferedImage(data.getDimensions().width, data.getDimensions().height);
                    LogManager.getLogger().debug(layer.bounds);

                    Rectangle layerRect = new Rectangle(0, 0, layer.bounds.width, layer.bounds.height);
                    if(layer.bounds.x < 0){
                        layerRect.x = -layerRect.x;
                        layer.bounds.x=0;
                    }
                    if(layer.bounds.x+layer.bounds.width > data.getDimensions().width){
                        layerRect.width = data.getDimensions().width-layer.bounds.x-1;
                        layer.bounds.width= data.getDimensions().width-1;
                    }
                    if(layer.bounds.y < 0){
                        layerRect.y = -layerRect.y;
                        layer.bounds.y=0;
                    }
                    if(layer.bounds.y+layer.bounds.height > data.getDimensions().height){
                        layerRect.height= data.getDimensions().height-layer.bounds.y-1;
                        layer.bounds.width= data.getDimensions().height-1;
                    }
                    BufferedImage subim = inSitu.getSubimage(layer.bounds.x, layer.bounds.y, layer.bounds.width, layer.bounds.height);

                    subim.setData(layer.getLayerImage().getSubimage(layerRect.x, layerRect.y, layerRect.width, layerRect.height).getData());

                    psdWriter.write(inSitu);
                    progAmt++;
                    progBar.imageProgress(null, (float) (100*progAmt/data.getNumLayers()) );
                }

            }

            LogManager.getLogger().info("...Done.");

        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.exit();
    }

    public static void main(String[] args){
        Application.launch(args);
    }

}
