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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 *
 * @author Robert F Cooper <rfcooper@sas.upenn.edu>
 */
abstract class PsdTreeNode implements MutableTreeNode, Transferable{
        
    protected PsdGroup parent;
    protected String name = "";
    
    PsdTreeNode(String nodeName, PsdGroup parent){
        name = nodeName;
        this.parent = parent;
    }
        
    public String getName(){
        return name;
    }
    
    @Override
    public PsdGroup getParent(){
        return parent;
    }
    
    @Override
    public void removeFromParent(){
        parent.remove(this);
        parent = null;
    }
    
    @Override
    public void setParent(MutableTreeNode newParent){
        if( newParent instanceof PsdGroup){
            parent = (PsdGroup)newParent;
        }else{
            LogManager.getLogger().warn("Unable to set parent of PsdTreeNode " + name+"!");
        }
    }
    
    @Override
    public String toString() {
        if (name.isEmpty()) {
            return "";
        } else {
            return name;
        }
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{new TreeDropFlavor()};
    }


    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return true;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        return this;
    }
    
}
