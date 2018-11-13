package montage;

import javax.imageio.ImageReader;
import javax.imageio.event.IIOReadProgressListener;
import javax.swing.*;

public class ResourceReadProgressListener implements IIOReadProgressListener {

    private ProgressMonitor progMon = null;
    private int numTotalFiles = 0;
    private int fileProgress = 0;

    public ResourceReadProgressListener(int numFiles){
        numTotalFiles = numFiles;
        progMon = new ProgressMonitor(null, "","Loading...",0,100);
    }

    @Override
    public void sequenceStarted(ImageReader source, int minIndex) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public void sequenceComplete(ImageReader source) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void imageStarted(ImageReader source, int imageIndex) {

    }

    @Override
    public void imageProgress(ImageReader source, float percentageDone) {

        int progress = Math.round(( (fileProgress*100)+percentageDone)/numTotalFiles );

        progMon.setProgress(progress);
    }

    public void setFileProgress(int fileNum){
        fileProgress = fileNum;
    }

    @Override
    public void imageComplete(ImageReader source) {
    }

    @Override
    public void thumbnailStarted(ImageReader source, int imageIndex, int thumbnailIndex) { }

    @Override
    public void thumbnailProgress(ImageReader source, float percentageDone) { }

    @Override
    public void thumbnailComplete(ImageReader source) { }

    @Override
    public void readAborted(ImageReader source) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
