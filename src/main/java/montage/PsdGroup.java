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
import java.awt.datatransfer.DataFlavor;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author Robert F Cooper <rfcooper@sas.upenn.edu>
 */
public class PsdGroup extends PsdTreeNode {

    // Group backed by a list of nodes
    private final List<PsdTreeNode> montageNodes = new Vector();

    public PsdGroup(String groupName) {
        this(groupName, null);
    }

    public PsdGroup(String groupName, PsdGroup parent) {
        super(groupName, parent);
    }

    void addNode(PsdTreeNode newNode) {
        newNode.setParent(this);
        montageNodes.add(newNode);
    }

    void addNode(PsdTreeNode newNode, int zPos) {
        newNode.setParent(this);
        montageNodes.add(zPos, newNode);
    }

    void removeNode(PsdTreeNode removeNode) {
        removeNode.setParent(null);
        montageNodes.remove(removeNode);
    }

    void changeNodeZPosition(String layerName, int layerZ) {

        for (int i = 0; i < montageNodes.size(); i++) {

            PsdTreeNode mL = montageNodes.get(i);

            if (mL.name.equals(layerName)) {

                montageNodes.remove(mL);
                montageNodes.add(layerZ, mL);
                break;
            }
        }
    }

    void changeNodeZPosition(PsdTreeNode layer, int layerZ) {

        if (montageNodes.contains(layer)) {
            montageNodes.remove(layer);
            montageNodes.add(layerZ, layer);
        }
    }

    @Override
    public boolean isLeaf() {
        return montageNodes.isEmpty();
    }

    @Override
    public int getChildCount() {
        return montageNodes.size();
    }

    @Override
    public Enumeration<PsdTreeNode> children() {
        return ((Vector) montageNodes).elements();
    }

    @Override
    public void remove(int index) {
        montageNodes.remove(index);
    }

    @Override
    public void remove(MutableTreeNode node) {
        montageNodes.remove((PsdTreeNode) node);
    }

    @Override
    public void insert(MutableTreeNode child, int index) {
        child.setParent(this);
        montageNodes.add(index, (PsdTreeNode) child);
    }

    @Override
    public void setUserObject(Object object) {
        LogManager.getLogger().warn("PsdGroup has no user object!");
    }

    @Override
    public TreeNode getChildAt(int childIndex) {
        return montageNodes.get(childIndex);
    }

    @Override
    public int getIndex(TreeNode node) {
        return montageNodes.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren() {
        return true;
    }

    PsdTreeNode getChild(int index) {
        return montageNodes.get(index);

    }

    int getIndexOfChild(PsdTreeNode child) {
        return montageNodes.indexOf(child);
    }

    int getIndexOfChild(String childName) {

        for (int i = 0; i < montageNodes.size(); i++) {
            if (montageNodes.get(i).getName().equals(childName)) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public boolean equals(Object o) {

        // If it is a PsdLayer,
        if (o instanceof PsdGroup) {
            // or the object reference is the same, then return equal.

            return this == o;

        } else {
            return false;
        }
    }
}