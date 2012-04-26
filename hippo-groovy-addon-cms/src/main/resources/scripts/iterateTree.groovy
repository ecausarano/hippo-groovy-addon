/**
 * Copyright (C) 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package scripts

import org.hippoecm.repository.api.HippoNode

/**
 * Simple script for recursively iterating and printing the path an item in the content tree;
 * @author Jeroen Reijn
 */

def printNode(node) {
  println(node.getPath());
};

def iterateNodeTree(node) {
  printNode(node)
  for(childNode in node.getNodes()) {
    if(!isVirtualNode(childNode)){
      iterateNodeTree(childNode)
    }
  }
}

def isVirtualNode(node) {
  return (node instanceof HippoNode ? !node.isSame(((HippoNode)node).getCanonicalNode()) : false);
}

rootNode = session.getRootNode();
contentNode = rootNode.getNode("content");

iterateNodeTree(contentNode);