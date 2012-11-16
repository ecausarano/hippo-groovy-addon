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
package org.onehippo.forge.cms.groovy.plugin.domain;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Session;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.forge.cms.groovy.plugin.ShellOutput;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;

/**
 * Simple domain object representing a Groovy script.
 *
 * @author Jeroen Reijn
 */
public class Script implements Comparable, IClusterable {

    private final static String STORAGE_LOCATION = "content/scripts";

    private String name;
    protected String language;
    private String script;
    private String path;
    private transient Node node;


    private ShellOutput shellOutput;

    public Script(final String name) {
        this.name = name;
    }

    public Script(Node node) throws RepositoryException {
        this.path = node.getPath().substring(1);
        this.name = NodeNameCodec.decode(node.getName());
        this.script = node.getProperty("script").getString();
    }

    public ShellOutput getShellOutput() {
        return shellOutput;
    }

    public void setShellOutput(ShellOutput shellOutput) {
        this.shellOutput = shellOutput;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getScript() {
        return script;
    }

    public void setScript(final String script) {
        this.script = script;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String toString() {
        return "[Script name=" + name + " script=" + script + "]";
    }

    @Override
    public int compareTo(final Object o) {
        return 0;
    }

    //-------------------- persistence helpers ----------//

    /**
     * Create a new groovyscript
     *
     * @throws RepositoryException
     */
    public void create() throws RepositoryException {
        StringBuilder relPath = new StringBuilder(STORAGE_LOCATION);
        relPath.append(NodeNameCodec.encode(getName(), true));
        node = ((UserSession) Session.get()).getRootNode().addNode(relPath.toString(), NodeType.NT_UNSTRUCTURED);
        node.setProperty("script", getScript());
        // save parent when adding a node
        node.getParent().getSession().save();
    }

    public void delete() throws RepositoryException {
        node.remove();
    }

    public Object eval() {

        Object returnVal = null;
        String stringResult = null;
        ScriptEngineManager scriptManager = new ScriptEngineManager();

        ScriptEngine engine = scriptManager.getEngineByName(language);

        SimpleScriptContext ctx = new SimpleScriptContext();

        if (Session.exists()) {
            UserSession userSession = (UserSession) Session.get();
            ctx.setAttribute("session", userSession.getJcrSession(), ScriptContext.ENGINE_SCOPE);
        }

        if (shellOutput != null) {
            ctx.setAttribute("wicket", shellOutput, ScriptContext.ENGINE_SCOPE);
        }

        try {
            engine.setContext(ctx);
            returnVal = engine.eval(script);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnVal;
    }
}

