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

import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Enumeration;

/**
 *
 * @author Robert F Cooper <rfcooper@sas.upenn.edu>
 */
public class PsdLayerNode extends PsdTreeNode {

    public PsdLayer layer = null;
        
    PsdLayerNode(PsdLayer psdLayer, PsdGroup psdGroup){
        super(psdLayer.name, psdGroup);
        layer = psdLayer;
    }
    
    PsdLayerNode(PsdLayer psdLayer){
        super(psdLayer.name, null);
        layer = psdLayer;
    }
    
    
    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public void insert(MutableTreeNode child, int index) { }

    @Override
    public void remove(int index) { }

    @Override
    public void remove(MutableTreeNode node) { }

    @Override
    public void setUserObject(Object object) {
        if( object instanceof PsdLayer){
            layer = (PsdLayer)object;
        }else{
            LogManager.getLogger().warn("PsdLayerNode cannot hold anything but a PsdLayer!");
        }
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return null;
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    @Override
    public int getIndex(TreeNode node) {
        return -1;
    }

    @Override
    public boolean getAllowsChildren() {
        return false;
    }

    @Override
    public Enumeration children() {
        return null;
    }

}
