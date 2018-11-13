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

import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 *
 * @author Robert F Cooper <rfcooper@sas.upenn.edu>
 */
public class PsdData implements TreeModel{
        
    
    PsdGroup root = new PsdGroup("root");
    ProgressMonitor montageActivityTracker = null;
    // Montage properties
    Rectangle[] layerBounds = null;
    PsdLayer[] layers;
    BufferedImage zBuffer;
    BufferedImage flattenedImage;
    Dimension dimensions;
    boolean[] isVisible;
    
    String montageName="";
    int numLayers   = 0;
    int numbits     = 8;
    ColorSpace montageCS = ColorSpace.getInstance( ColorSpace.CS_sRGB );
    

    final private List<TreeModelListener> montageTML = Collections.synchronizedList( new ArrayList<>() );

    public PsdData(String montageName, Dimension canvasSize, ColorSpace cS, int numBits){
        flattenedImage = new BufferedImage(canvasSize.width, canvasSize.height, BufferedImage.TYPE_INT_ARGB);
        dimensions  = canvasSize;
        montageCS = cS;
        numbits     = numBits;
        zBuffer = new BufferedImage( dimensions.width, dimensions.height, BufferedImage.TYPE_INT_ARGB );
    }
    

    public void addLayer(PsdLayer newLayer){
        root.addNode( new PsdLayerNode( newLayer ) );
    }
    
    public void addGroup(PsdGroup newGroup){
        root.addNode( newGroup );
    }
    
    public void addLayer( String groupName, PsdLayer newLayer ){
        if( !groupName.equals("root") ){                
            addLayer( root, groupName,  new PsdLayerNode( newLayer ) );
        }else{
            addLayer(newLayer);
        }
    }
    
    public void addLayer(PsdGroup group, PsdLayer newLayer ){
        group.addNode( new PsdLayerNode(newLayer) );
    }
    
    public void addGroup( String addToWhichGroup, PsdGroup newGroup ){
        if( !addToWhichGroup.equals("root") ){                
            addLayer( root, addToWhichGroup, newGroup );
        }else{
            addGroup( newGroup );
        }
    }

    void addLayer(PsdGroup currentNode, String groupName, PsdTreeNode newNode ){
        
        int index = currentNode.getIndexOfChild(groupName);
        
        if( index  != -1){ // If we can find the index within this node's children, add it to that child node.
            PsdTreeNode childNode = currentNode.getChild(index);
            
            if( !childNode.isLeaf() ) {
                ((PsdGroup)childNode).addNode(newNode);
            }
            
        }else{ // If it isn't in this node's children, then assume we have to begin checking all of the MontageGroups in our list.
            
            PsdTreeNode mTN;
            
            Enumeration<PsdTreeNode> children = currentNode.children();
            
            while( children.hasMoreElements() ){
                
                mTN = children.nextElement();
            
                if( !mTN.isLeaf() ){
                    addLayer( (PsdGroup)mTN, groupName, newNode );
                }
            }
        }        
    }
    
    public void determineZBuffer(){
        Graphics2D g2d = zBuffer.createGraphics();
        //clear        
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0, 0, dimensions.width, dimensions.height);
        g2d.setComposite( AlphaComposite.DstOver );
        
        revalidateLayers(root, g2d, 0);
        
        g2d.dispose();
        
        Raster r = zBuffer.getData();
        int numBands = r.getNumBands();
        
        for( int i=0;i<layers.length; i++){
            int[] data = new int[ (layers[i].bounds.width) * (layers[i].bounds.height) * numBands];

            r.getPixels( layers[i].bounds.x, layers[i].bounds.y, layers[i].bounds.width, layers[i].bounds.height, data);

            for( int j=0; j<data.length; j+=4){
                int value = (data[j] << 16) | (data[j+1] << 8) | data[j+2];

                isVisible[value] = true;
            }
        }
    }
    
    private int revalidateLayers(PsdGroup group, Graphics2D g2d, int count){
        
        Enumeration groupEnum = group.children();

        while ( groupEnum.hasMoreElements() ){
            PsdTreeNode tN = (PsdTreeNode)groupEnum.nextElement();
            if( !tN.isLeaf() || tN instanceof PsdGroup){ // @TODO: Remove the instanceofs from these recursive methods.
                count = revalidateLayers((PsdGroup)tN, g2d, count);
            }else{
                PsdLayer psdLayer = ((PsdLayerNode)tN).layer;
                Rectangle bounds = psdLayer.bounds;
                    
                g2d.setColor(new Color(count));
                g2d.fill(bounds);
                
                layers[count] = psdLayer;
                layerBounds[count] = bounds;
                
                count++;
                int progcount = count;

                SwingUtilities.invokeLater(() -> {
                    montageActivityTracker.setProgress( progcount );
                    montageActivityTracker.setNote("Layer "+ psdLayer.name);
                });
            }
        }        
        
        return count;
    }
    
    public double getNumLayers(){
        return numLayers;
    }
    
    private int getNumLayers(PsdGroup group, int numlayers){
        
        Enumeration groupEnum = group.children();
        
        while ( groupEnum.hasMoreElements() ){
            PsdTreeNode tN = (PsdTreeNode)groupEnum.nextElement();
            
            if( !tN.isLeaf()){
                numlayers = getNumLayers((PsdGroup)tN, numlayers );
            }else{
                numlayers++;
            }
        }        
        
        return numlayers;
    }
    
    public void revalidate(){
        numLayers = getNumLayers(root, 0);
        layers = new PsdLayer[numLayers];
        layerBounds = new Rectangle[numLayers];
        isVisible = new boolean[numLayers];

        SwingUtilities.invokeLater(() -> {
            montageActivityTracker = new ProgressMonitor(null,"Validating composite...","Nuthin'",0,numLayers);
        });

        redrawMergedImage();
    }
    
    private void redrawMergedImage(){
        
        Graphics2D g2d = flattenedImage.createGraphics();
        //clear        
        g2d.setComposite(AlphaComposite.Clear);
        g2d.fillRect(0,0,dimensions.width,dimensions.height);

        //reset composite
        g2d.setComposite( AlphaComposite.DstOver );
        
        drawAncestorLayers(root, g2d, 0 );
        g2d.setComposite(AlphaComposite.Src);
        
        g2d.dispose();
    }
    
    private int drawAncestorLayers(PsdGroup group, Graphics2D g2d, int count){
        
        Enumeration groupEnum = group.children();

        while ( groupEnum.hasMoreElements() ){
            PsdTreeNode tN = (PsdTreeNode)groupEnum.nextElement();
            if( !tN.isLeaf() || tN instanceof PsdGroup){ // @TODO: Remove the instanceofs from these recursive methods.
                count = drawAncestorLayers((PsdGroup)tN, g2d, count);
            }else{
                
                PsdLayer psdLayer = ((PsdLayerNode)tN).layer;
                Rectangle bounds = psdLayer.bounds;
                    
                g2d.setComposite( AlphaComposite.getInstance( AlphaComposite.DST_OVER, psdLayer.opacity) );
                
                g2d.drawImage( psdLayer.getLayerImage(), bounds.x, bounds.y, null);
                
                layers[count] = psdLayer;
                layerBounds[count] = bounds;
                
                count++;
                int progcount = count;
                
                SwingUtilities.invokeLater(() -> {
                    montageActivityTracker.setProgress( progcount );
                    montageActivityTracker.setNote("Layer "+ psdLayer.name);
                });
            }
        }        
        
        return count;
    }
    
    public PsdLayer[] getMontageLayers(){
        return layers;
    }
    
    public PsdLayer[] revalidateMontageLayers(){
        ArrayList<PsdLayer> layerBounds = revalidateMontageLayers(root, new ArrayList<>());
        
        return layerBounds.toArray(new PsdLayer[0]);
    }
    
    private ArrayList<PsdLayer> revalidateMontageLayers(PsdGroup group, ArrayList<PsdLayer> boundsList){
        
        Enumeration groupEnum = group.children();
        
        while ( groupEnum.hasMoreElements() ){
            PsdTreeNode tN = (PsdTreeNode)groupEnum.nextElement();
            
            if( !tN.isLeaf() || tN instanceof PsdGroup){
                revalidateMontageLayers((PsdGroup)tN, boundsList );
            }else{

                PsdLayer psdLayer = ((PsdLayerNode)tN).layer;
                
                boundsList.add(psdLayer);
            }
        }        
        
        return boundsList;
    }

    public Dimension getDimensions(){
        return dimensions;
    }

    public Rectangle[] getLayerBounds(){
        return layerBounds;
    }
    
    public Rectangle[] revalidateLayerBounds(){
        ArrayList<Rectangle> layerBounds = revalidateLayerBounds(root, new ArrayList<>());
        
        return layerBounds.toArray(new Rectangle[0]);
    }
    
    private ArrayList<Rectangle> revalidateLayerBounds(PsdGroup group, ArrayList<Rectangle> boundsList){
        
        Enumeration groupEnum = group.children();
        
        while ( groupEnum.hasMoreElements() ){
            PsdTreeNode tN = (PsdTreeNode)groupEnum.nextElement();
            
            if( !tN.isLeaf() || tN instanceof PsdGroup){
                revalidateLayerBounds((PsdGroup)tN, boundsList );
            }else{

                PsdLayer psdLayer = ((PsdLayerNode)tN).layer;
                Rectangle layerBounds = psdLayer.bounds;
                boundsList.add(layerBounds);
            }
        }        
        
        return boundsList;
    }    


    public ColorSpace getColorSpace(){
        return montageCS;
    }

    public int getNumBits(){
        return numbits;
    }
    
    @Override
    public Object getRoot() {
        return root;
    }
    
    /*
    * Taken from DefaultTreeModel.
    */
    public TreeNode[] getPathToRoot(TreeNode aNode) {
        return getPathToRoot(aNode, 0);
    }

    /**
     * Taken from DefaultTreeModel.
     */
    protected TreeNode[] getPathToRoot(TreeNode aNode, int depth) {
        TreeNode[]              retNodes;

        if(aNode == null) {
            if(depth == 0)
                return null;
            else
                retNodes = new TreeNode[depth];
        }
        else {
            depth++;
            if(aNode == root)
                retNodes = new TreeNode[depth];
            else
                retNodes = getPathToRoot(aNode.getParent(), depth);
            retNodes[retNodes.length - depth] = aNode;
        }
        return retNodes;
    }
    
    @Override
    public Object getChild(Object parent, int index) {
//        return root.getChild(parent, index);

        if( parent instanceof PsdGroup){
            PsdGroup groupParent = (PsdGroup)parent;
            
            return groupParent.getChild(index);
        }else{
//            LogManager.getLogger().error("Input to getChild not a PsdGroup object!");
            return null;
        }
    }

    @Override
    public int getChildCount(Object parent) {
        
        if( parent instanceof PsdGroup){
            PsdGroup group = (PsdGroup)parent;
                        
            return group.getChildCount();            
            
        }else{            
            return 0;
        }
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((PsdTreeNode)node).isLeaf();
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        LogManager.getLogger().info("*** valueForPathChanged : "
                                    + path + " --> " + newValue);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if( !((PsdTreeNode)parent).isLeaf() ){
            PsdGroup groupParent = (PsdGroup)parent;
            
            return groupParent.getIndexOfChild((PsdTreeNode)child);
        }else{            
            return -1;
        }
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        montageTML.add(l);
    }

    public void insertNodeInto(MutableTreeNode newChild,
                               MutableTreeNode parent, int index){
        
        parent.insert(newChild, index);
        Object[] newChildren = new Object[1];
        newChildren[0] = parent.getChildAt(index);

        for(TreeModelListener TML : montageTML){
            TML.treeNodesInserted(new TreeModelEvent(this, getPathToRoot(newChild), new int[]{index}, newChildren));
        }
    }
    
    /**
    *  Adapted from DefaultTreeModel. 
    */
    public void removeNodeFromParent(MutableTreeNode node){
        MutableTreeNode parent = (MutableTreeNode)node.getParent();
        
        if(parent == null)
            throw new IllegalArgumentException("node does not have a parent.");
        
        int index = parent.getIndex(node);
        parent.remove(index);
        
        for(TreeModelListener TML : montageTML){
            TML.treeNodesRemoved( new TreeModelEvent(this, getPathToRoot(node), new int[]{index}, new Object[]{node} ) );
        }
    }
    
    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        montageTML.remove(l);
    }
    
    void indent(int level) {
        for (int i = 0; i < level; i++){
            System.out.print("    ");
        }
    }
    
    public void printTreeToScreen(PsdTreeNode node, int level){

        // print open tag of element
        indent(level);
        
//        System.out.println(node.name +" has "+node.children().length+" children.");
        System.out.println(node.name +" is a group? "+ !node.isLeaf());
        
        if( !node.isLeaf() ){
            
        
            int newlevel = level+1;
            
            PsdTreeNode mTN;
            
            Enumeration<PsdTreeNode> children = node.children();
            
            while( children.hasMoreElements() ){
                
                mTN = children.nextElement();
            
//                System.out.println(node.name);
                printTreeToScreen(mTN, newlevel);
            }
        
            // print close tag of element
//            indent(level);
        }
        
    }
    
    
    
//    public void addPropertyChangeListener(PropertyChangeListener listener) {
//        montagePCS.addPropertyChangeListener(listener);
//    }
//    
//    public void removePropertyChangeListener(PropertyChangeListener listener) {
//        montagePCS.removePropertyChangeListener(listener);
//    }
//
//    public void removeAllPropertyChangeListeners() {
//        PropertyChangeListener[] listeners = montagePCS.getPropertyChangeListeners();
//        for( int i=0; i<listeners.length; i++){
//            montagePCS.removePropertyChangeListener( listeners[i] );
//        }        
//    }
}
